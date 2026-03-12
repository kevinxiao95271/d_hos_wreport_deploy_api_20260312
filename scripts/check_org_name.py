import psycopg2

DSN = "host=119.167.165.27 port=5432 dbname=zjylzl user=postgres password=zjylzl options='-c search_path=zjylzl'"
conn = psycopg2.connect(DSN)
cur = conn.cursor()

# 确认 wr_org_a/b 对应的机构名
cur.execute("""
    SELECT u.account, u.real_name, u.org_id, ho.org_name
    FROM sys_user u
    LEFT JOIN hr_organization ho ON ho.org_id = u.org_id
    WHERE u.account IN ('wr_org_a', 'wr_org_b', 'wr_admin')
""")
for row in cur.fetchall():
    print(dict(zip([d[0] for d in cur.description], row)))

# 同时确认正确的 JOIN 路径：sys_user.org_id -> hr_organization.org_id
cur.execute("""
    SELECT u.account, u.org_id, ho.org_id AS hr_org_id, ho.org_name
    FROM sys_user u
    JOIN sys_user_role ur ON ur.user_id = u.user_id
    JOIN sys_role r ON r.role_id = ur.role_id
    LEFT JOIN hr_organization ho ON ho.org_id = u.org_id
    WHERE r.role_code IN ('qcUser', 'deptAdmin') AND u.del_flag = 'N'
    LIMIT 15
""")
print("\n=== qcUser/deptAdmin + org_name ===")
for row in cur.fetchall():
    print(dict(zip([d[0] for d in cur.description], row)))

conn.close()
