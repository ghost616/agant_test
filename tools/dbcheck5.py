import sqlite3
con = sqlite3.connect(r"E:\test_tool\platform-app\data\agent_platform.db")
cur = con.cursor()
cur.execute("SELECT id, user_id, is_child, deleted FROM session WHERE id IN (2087004655850450946, 2087020758475472897, 2077246397967323137, 2086369644872814594)")
print("sessions + deleted:", cur.fetchall())
cur.execute("SELECT deleted, COUNT(*) FROM agent_log WHERE session_id = 2087004655850450946 GROUP BY deleted")
print("agent_log deleted flags:", cur.fetchall())
con.close()