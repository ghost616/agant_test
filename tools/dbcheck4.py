import sqlite3
con = sqlite3.connect(r"E:\test_tool\platform-app\data\agent_platform.db")
cur = con.cursor()
cur.execute("""SELECT parent_session_id, COUNT(*) FROM session WHERE is_child = 1 GROUP BY parent_session_id HAVING COUNT(*) IN (1, 2) LIMIT 10""")
print("roots with 1 or 2 children:", cur.fetchall())
con.close()