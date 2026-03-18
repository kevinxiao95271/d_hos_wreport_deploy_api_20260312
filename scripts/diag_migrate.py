import psycopg2

conn = psycopg2.connect(host='119.167.165.27', port=5432, dbname='zjylzl',
                        user='postgres', password='zjylzl', options='-c search_path=zjylzl')
cur = conn.cursor()

# 1. 现有 qc_ 账号
print("=== sys_user 中 qc_ 账号 ===")
cur.execute("""
    SELECT user_id, account, real_name, org_id
    FROM sys_user
    WHERE account LIKE 'qc_%' AND del_flag = 'N'
    ORDER BY account
""")
qc_users = cur.fetchall()
for r in qc_users:
    print(f"  user_id={r[0]}  account={r[1]}  real_name={r[2]}  org_id={r[3]}")

# 2. 这些 org_id 在 hr_organization 里有没有
print("\n=== 对应 org_id 在 hr_organization 的情况 ===")
org_ids = list({r[3] for r in qc_users if r[3]})
for oid in org_ids:
    cur.execute("SELECT org_id, org_name FROM hr_organization WHERE org_id = %s", (oid,))
    r = cur.fetchone()
    print(f"  org_id={oid} -> {r}")

# 3. hr_person 表的完整字段（NOT NULL 约束）
print("\n=== hr_person 字段及 NOT NULL 约束 ===")
cur.execute("""
    SELECT column_name, data_type, is_nullable, column_default
    FROM information_schema.columns
    WHERE table_schema = 'zjylzl' AND table_name = 'hr_person'
    ORDER BY ordinal_position
""")
for r in cur.fetchall():
    nn = "NOT NULL" if r[2] == 'NO' else ""
    print(f"  {r[0]:<30} {r[1]:<20} {nn}  default={r[3]}")

# 4. organization_type='2' 的现有样本（字段参照）
print("\n=== organization_type='2' 现有样本 ===")
cur.execute("SELECT * FROM hr_person WHERE organization_type='2' LIMIT 2")
cols = [d[0] for d in cur.description]
print("  cols:", cols)
for r in cur.fetchall():
    print("  ", dict(zip(cols, r)))

# 5. sys_user_role 里 qc_ 账号的角色
print("\n=== qc_ 账号角色 ===")
cur.execute("""
    SELECT u.account, r.role_code, r.role_name
    FROM sys_user u
    JOIN sys_user_role ur ON ur.user_id = u.user_id
    JOIN sys_role r ON r.role_id = ur.role_id
    WHERE u.account LIKE 'qc_%' AND u.del_flag='N'
""")
for r in cur.fetchall():
    print(f"  {r}")

conn.close()
