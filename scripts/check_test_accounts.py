import psycopg2, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

conn = psycopg2.connect(
    host="119.167.165.27", port=5432, dbname="zjylzl",
    user="postgres", password="zjylzl",
    options="-c search_path=zjylzl"
)
cur = conn.cursor()

print("=== wr_admin / wr_org_a / wr_org_b 详细信息 ===\n")
cur.execute("""
    SELECT u.account, u.real_name, u.org_id,
           ho.org_name,
           r.role_code, r.role_name
    FROM sys_user u
    LEFT JOIN hr_organization ho ON ho.org_id = u.org_id
    LEFT JOIN sys_user_role ur ON ur.user_id = u.user_id
    LEFT JOIN sys_role r ON r.role_id = ur.role_id
    WHERE u.account IN ('wr_admin','wr_org_a','wr_org_b')
    AND u.del_flag = 'N'
    ORDER BY u.account
""")
rows = cur.fetchall()
print(f"{'account':<12} {'real_name':<20} {'org_id':<22} {'org_name':<20} {'role_code':<15} {'role_name'}")
print("-" * 110)
for r in rows:
    print(f"{str(r[0]):<12} {str(r[1]):<20} {str(r[2]):<22} {str(r[3]):<20} {str(r[4]):<15} {r[5]}")

print("\n=== sys_user 原始字段（wr_org_a / wr_org_b）===\n")
cur.execute("""
    SELECT user_id, account, real_name, org_id, status_flag, del_flag
    FROM sys_user
    WHERE account IN ('wr_org_a','wr_org_b')
    AND del_flag = 'N'
""")
for r in cur.fetchall():
    print(r)

print("\n=== hr_organization 里 org_id=1986677412049780738 对应什么 ===\n")
cur.execute("SELECT * FROM hr_organization WHERE org_id = 1986677412049780738")
for r in cur.fetchall():
    print(r)

conn.close()
