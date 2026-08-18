# -*- coding: utf-8 -*-
"""会话日志相关后端改造 - 接口测试脚本 v2（修正 isEvaluation 字符串序列化、软删除根会话、动态子会话集）"""
import json, sys, urllib.request, urllib.error, http.cookiejar, sqlite3

BASE = "http://127.0.0.1:8080"
DB = r"E:\test_tool\platform-app\data\agent_platform.db"
results = []

def req(method, path, cookie=None, body=None):
    url = BASE + path
    headers = {"Content-Type": "application/json"}
    data = None
    if cookie:
        headers["Cookie"] = "SESSION_ID=" + cookie
    if body is not None:
        data = json.dumps(body).encode("utf-8")
    r = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        resp = urllib.request.urlopen(r, timeout=20)
        return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode("utf-8"))
        except Exception:
            return e.code, {}

def check(name, cond, detail=""):
    results.append((name, bool(cond), detail))
    print(("PASS" if cond else "FAIL") + " | " + name + (" | " + detail if detail else ""))

def is_true(v):
    return v is True or str(v).lower() == "true"

def children_of(root_id):
    con = sqlite3.connect(DB)
    cur = con.cursor()
    cur.execute("SELECT id FROM session WHERE parent_session_id = ? AND is_child = 1", (int(root_id),))
    ids = [str(r[0]) for r in cur.fetchall()]
    con.close()
    return ids

# 登录
cj = http.cookiejar.CookieJar()
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj))
r = urllib.request.Request(BASE + "/api/auth/login",
                           data=json.dumps({"loginName": "admin", "password": "123456"}).encode(),
                           headers={"Content-Type": "application/json"}, method="POST")
opener.open(r, timeout=20)
cookie = None
for c in cj:
    if c.name == "SESSION_ID":
        cookie = c.value
if not cookie:
    print("FATAL: no SESSION_ID cookie")
    sys.exit(2)
print("LOGIN OK")

def reqc(path, cookie=cookie):
    return req("GET", path, cookie=cookie)

# ========== 1. log-sessions ==========
code, body = reqc("/api/sessions/log-sessions")
sessions = body.get("data") or []
check("log-sessions 返回 200 且 success", code == 200 and body.get("success"), "code=" + str(code))
check("log-sessions 数据为列表", isinstance(sessions, list), "type=" + str(type(sessions).__name__))
main_only = all((s.get("isChild") in (None, False, "false", "False")) for s in sessions)
check("log-sessions 仅含主会话（isChild null/false）", main_only)
has_eval = any(is_true(s.get("isEvaluation")) for s in sessions)
check("log-sessions 含评估会话(isEvaluation=true)", has_eval, "total=" + str(len(sessions)))
times = [s.get("createTime") or "" for s in sessions]
check("log-sessions 按创建时间倒序", times == sorted(times, reverse=True), "first=" + (times[0] if times else "N/A"))
if sessions:
    s0 = sessions[0]
    fields_ok = all(k in s0 for k in ("id", "isChild", "parentSessionId", "isEvaluation", "title"))
    check("log-sessions DTO 含 isChild/parentSessionId/isEvaluation", fields_ok, str(list(s0.keys())))
    check("log-sessions id 序列化为字符串", isinstance(s0.get("id"), str), "id=" + repr(s0.get("id")))

# ========== 2. /api/sessions 行为不变 ==========
code, body = reqc("/api/sessions")
sessions2 = body.get("data") or []
check("sessions 返回 200 且 success", code == 200 and body.get("success"))
no_eval = all(not is_true(s.get("isEvaluation")) for s in sessions2)
check("sessions 不含评估会话(isEvaluation=true)", no_eval, "total=" + str(len(sessions2)))
only_main2 = all((s.get("isChild") in (None, False, "false", "False")) for s in sessions2)
check("sessions 仅含主会话", only_main2)
check("log-sessions 数量 >= sessions 数量", len(sessions) >= len(sessions2), "log=" + str(len(sessions)) + " plain=" + str(len(sessions2)))
eval_ids = {s.get("id") for s in sessions if is_true(s.get("isEvaluation"))}
plain_ids = {s.get("id") for s in sessions2}
check("log-sessions 独有的评估会话不在 sessions 中", eval_ids.isdisjoint(plain_ids), "eval_count=" + str(len(eval_ids)))

# ========== 3. agent-logs rootSessionId 过滤 ==========
ROOT = "2077246397967323137"   # 未删除主会话，有多个子会话且有日志
CHILD = "2086369644872814594"
code, body = reqc("/api/agent-logs?rootSessionId=" + ROOT + "&page=1&size=500")
logs = (body.get("data") or {}).get("list") or []
check("agent-logs?rootSessionId 返回 200 且 success", code == 200 and body.get("success"))
total_r = (body.get("data") or {}).get("total")
check("rootSessionId 过滤有结果", len(logs) > 0, "total=" + str(total_r))
exp_ids = {ROOT} | set(children_of(ROOT))
sid_set = {str(l.get("sessionId")) for l in logs}
check("rootSessionId 过滤日志均属于主会话或其全部子会话", sid_set.issubset(exp_ids), "sids=" + str(sid_set))
check("rootSessionId 结果含主会话日志", ROOT in sid_set)
check("rootSessionId 结果含子会话日志", CHILD in sid_set)

code2, body2 = reqc("/api/agent-logs?page=1&size=500")
total_all = (body2.get("data") or {}).get("total")
check("无 rootSessionId 查询正常且 total>= 过滤值", code2 == 200 and total_all >= (total_r or 0), "all=" + str(total_all) + " filtered=" + str(total_r))

# 主会话无子会话（未删除、有日志）：IN 集合仅含主会话
SINGLE = "2085634076849287170"
code3, body3 = reqc("/api/agent-logs?rootSessionId=" + SINGLE + "&page=1&size=500")
d3 = body3.get("data") or {}
sids3 = {str(l.get("sessionId")) for l in d3.get("list") or []}
check("rootSessionId 无子会话时返回该主会话日志", (d3.get("total") or 0) > 0, "total=" + str(d3.get("total")))
check("rootSessionId 无子会话时仅含主会话日志", sids3.issubset({SINGLE}), "sids=" + str(sids3))

# 软删除的主会话（deleted=1）→ 视为不存在 → 空分页
code4, body4 = reqc("/api/agent-logs?rootSessionId=2087004655850450946&page=1&size=20")
d4 = body4.get("data") or {}
check("rootSessionId 为软删除会话 → 空列表", code4 == 200 and d4.get("list") == [] and d4.get("total") == 0, str(d4))

# rootSessionId 不存在 → 空分页
code4b, body4b = reqc("/api/agent-logs?rootSessionId=999999999999999999&page=1&size=20")
d4b = body4b.get("data") or {}
check("rootSessionId 不存在 → 空列表", code4b == 200 and d4b.get("list") == [] and d4b.get("total") == 0, str(d4b))

# rootSessionId 属于其他用户 → 空分页（ghost 用户 id=2088677402640994306 的会话，先查一个）
con = sqlite3.connect(DB); cur = con.cursor()
cur.execute("SELECT id FROM session WHERE user_id = 2088677402640994306 AND is_child = 0 AND deleted = 0 LIMIT 1")
other_root = cur.fetchone()
con.close()
if other_root:
    code5, body5 = reqc("/api/agent-logs?rootSessionId=" + str(other_root[0]) + "&page=1&size=20")
    d5 = body5.get("data") or {}
    check("rootSessionId 属于其他用户 → 空列表", code5 == 200 and d5.get("list") == [] and d5.get("total") == 0, str(d5))
else:
    print("SKIP | 其他用户会话不存在，跳过该用例")

# ========== 4. isChild 序列化 ==========
code6, body6 = reqc("/api/agent-logs?sessionId=" + CHILD + "&page=1&size=5")
logs6 = (body6.get("data") or {}).get("list") or []
check("子会话日志 isChild=true", len(logs6) > 0 and all(l.get("isChild") is True for l in logs6), "isChild=" + str([l.get("isChild") for l in logs6]))
code7, body7 = reqc("/api/agent-logs?sessionId=" + ROOT + "&page=1&size=5")
logs7 = (body7.get("data") or {}).get("list") or []
check("主会话日志 isChild=false", len(logs7) > 0 and all(l.get("isChild") is False for l in logs7), "isChild=" + str([l.get("isChild") for l in logs7]))
check("agent-logs 响应含 isChild 字段", "isChild" in json.dumps(logs7), "")

# ========== 5. 未登录 ==========
code8, body8 = req("GET", "/api/sessions/log-sessions")
check("未登录 log-sessions → USER_NOT_LOGIN", body8.get("code") == "USER-NOT-LOGIN", str(body8.get("code")))
code9, body9 = req("GET", "/api/agent-logs")
check("未登录 agent-logs → USER_NOT_LOGIN", body9.get("code") == "USER-NOT-LOGIN", str(body9.get("code")))

passed = sum(1 for _, ok, _ in results if ok)
failed = sum(1 for _, ok, _ in results if not ok)
print()
print("=" * 60)
print("TOTAL: %d, PASS: %d, FAIL: %d" % (len(results), passed, failed))
for name, ok, detail in results:
    if not ok:
        print("FAILED DETAIL: " + name + " | " + detail)
sys.exit(0 if failed == 0 else 1)