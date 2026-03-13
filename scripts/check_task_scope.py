import psycopg2

conn = psycopg2.connect(
    host='119.167.165.27', port=5432, dbname='zjylzl',
    user='postgres', password='zjylzl', options='-c search_path=zjylzl'
)
c = conn.cursor()

c.execute("SELECT account, org_id FROM sys_user WHERE account IN ('wr_org_a','wr_org_b','wr_admin')")
for row in c.fetchall():
    print('User:', row)

c.execute("SELECT id, task_name, status, del_flag FROM wr_task ORDER BY create_time DESC LIMIT 10")
tasks = c.fetchall()
print('\nTasks:')
for t in tasks:
    print(' ', t)

c.execute("SELECT task_id, org_id FROM wr_task_org_scope LIMIT 20")
scopes = c.fetchall()
print('\nTask scopes:')
for s in scopes:
    print(' ', s)

conn.close()
