import sqlite3
for path in [r"E:\test_tool\data\agent_platform.db", r"E:\test_tool\platform-app\data\agent_platform.db"]:
    con = sqlite3.connect(path)
    cur = con.cursor()
    cur.execute("SELECT name FROM sqlite_master WHERE type='table'")
    tables = [r[0] for r in cur.fetchall()]
    print("DB:", path)
    print("  tables:", tables)
    if "agent_log" in tables:
        cur.execute("SELECT COUNT(*) FROM agent_log"); print("  agent_log count:", cur.fetchone()[0])
    if "session" in tables:
        cur.execute("SELECT COUNT(*) FROM session"); print("  session count:", cur.fetchone()[0])
    if "user" in tables:
        cur.execute("SELECT id, login_name FROM user"); print("  users:", cur.fetchall())
    con.close()