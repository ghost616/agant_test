import sqlite3
con = sqlite3.connect(r"E:\test_tool\platform-app\data\agent_platform.db")
cur = con.cursor()
cur.execute("SELECT sql FROM sqlite_master WHERE type='table' AND name='agent_log'")
print(cur.fetchone()[0])
# check session_id storage type for the failing session
cur.execute("SELECT typeof(session_id), session_id, COUNT(*) FROM agent_log WHERE session_id = 2087004655850450946 GROUP BY typeof(session_id), session_id")
print("typeof:", cur.fetchall())
# is there any root with exactly one child with logs?
cur.execute("SELECT s.parent_session_id, COUNT(*) FROM session s JOIN agent_log l ON l.session_id = s.id WHERE s.is_child = 1 AND l.user_id = 1 GROUP BY s.parent_session_id HAVING COUNT(DISTINCT s.id) = 1")
print("roots with exactly 1 child (has logs):", cur.fetchall())
con.close()