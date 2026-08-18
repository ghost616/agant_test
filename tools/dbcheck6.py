import sqlite3
con = sqlite3.connect(r"E:\test_tool\platform-app\data\agent_platform.db")
cur = con.cursor()
cur.execute("""
SELECT s.id, s.is_child, s.deleted, s.is_evaluation,
  (SELECT COUNT(*) FROM session c WHERE c.parent_session_id = s.id AND c.is_child = 1) AS nchild,
  (SELECT COUNT(*) FROM agent_log l WHERE l.session_id = s.id) AS nlogs
FROM session s
WHERE s.user_id = 1 AND s.is_child = 0 AND s.deleted = 0
  AND (SELECT COUNT(*) FROM agent_log l WHERE l.session_id = s.id) > 0
  AND (SELECT COUNT(*) FROM session c WHERE c.parent_session_id = s.id AND c.is_child = 1) = 0
LIMIT 5
""")
print("valid roots (no children, has logs, not deleted):")
for r in cur.fetchall(): print(" ", r)
con.close()