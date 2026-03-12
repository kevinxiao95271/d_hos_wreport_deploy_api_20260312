import psycopg2, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

conn = psycopg2.connect(
    host="119.167.165.27", port=5432, dbname="zjylzl",
    user="postgres", password="zjylzl",
    options="-c search_path=zjylzl"
)
cur = conn.cursor()

print("=== qcUser 账号 + 对应机构 ===\n")
cur.execute("""
    SELECT u.account, u.real_name, u.org_id, ho.org_name
    FROM sys_user u
    LEFT JOIN hr_organization ho ON ho.org_id = u.org_id
    JOIN sys_user_role ur ON ur.user_id = u.user_id
    JOIN sys_role r ON r.role_id = ur.role_id AND r.role_code = 'qcUser'
    WHERE u.del_flag = 'N'
    ORDER BY u.account
""")
for r in cur.fetchall():
    print(f"  {r[0]:<12} real={r[1]:<25} org_id={r[2]}  org_name={r[3]}")

print("\n=== 含'超声'或'产科'的 hr_organization 记录 ===\n")
cur.execute("""
    SELECT org_id, org_name FROM hr_organization
    WHERE org_name LIKE '%超声%' OR org_name LIKE '%产科%'
""")
for r in cur.fetchall():
    print(f"  org_id={r[0]}  org_name={r[1]}")

conn.close()
