"""
fix_test_account_orgs.py
修正 wr_org_a / wr_org_b 的 org_id 指向正确的质控中心机构
"""
import psycopg2, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

conn = psycopg2.connect(
    host="119.167.165.27", port=5432, dbname="zjylzl",
    user="postgres", password="zjylzl",
    options="-c search_path=zjylzl"
)
cur = conn.cursor()

updates = [
    # (account, real_name, new_org_id, org_name_for_verify)
    ("wr_org_a", "超声质控中心",   7437930044913090594, "超声质控中心"),
    ("wr_org_b", "产科医疗质控中心", 7437930041691865107, "产科医疗质控中心"),
]

for account, real_name, org_id, _ in updates:
    cur.execute(
        "UPDATE sys_user SET org_id = %s, real_name = %s WHERE account = %s AND del_flag = 'N'",
        (org_id, real_name, account)
    )
    print(f"  updated {account}: real_name='{real_name}', org_id={org_id}")

conn.commit()

# 验证
print("\n=== 验证结果 ===\n")
cur.execute("""
    SELECT u.account, u.real_name, u.org_id, ho.org_name, r.role_code
    FROM sys_user u
    LEFT JOIN hr_organization ho ON ho.org_id = u.org_id
    JOIN sys_user_role ur ON ur.user_id = u.user_id
    JOIN sys_role r ON r.role_id = ur.role_id
    WHERE u.account IN ('wr_admin','wr_org_a','wr_org_b') AND u.del_flag = 'N'
    ORDER BY u.account
""")
print(f"{'account':<12} {'real_name':<20} {'org_name':<20} {'role_code'}")
print("-" * 75)
for r in cur.fetchall():
    print(f"{r[0]:<12} {str(r[1]):<20} {str(r[3]):<20} {r[4]}")

cur.close()
conn.close()
print("\n✅ 修正完成")
