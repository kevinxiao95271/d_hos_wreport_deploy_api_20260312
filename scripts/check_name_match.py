import sys, io, psycopg2
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
conn = psycopg2.connect(host="119.167.165.27", port=5432, dbname="zjylzl",
                        user="postgres", password="zjylzl", options="-c search_path=zjylzl")
c = conn.cursor()

c.execute("""SELECT u.account, o.org_name
             FROM sys_user u JOIN hr_organization o ON o.org_id=u.org_id
             WHERE u.account IN ('wr_org_a','wr_org_b','qc_001','qc_002','qc_003','qc_004','qc_005')""")
orgs = c.fetchall()
print("=== 测试账号 orgName ===")
for r in orgs:
    print(f"  {r[0]}: [{r[1]}]")

c.execute("""SELECT item_name FROM wr_template_item
             WHERE template_id=2000000000000002 AND del_flag=0 AND is_leaf=1
             ORDER BY col_index""")
item_names = [r[0] for r in c.fetchall()]
print(f"\n=== 附件3 item names ({len(item_names)} 列) ===")
for n in item_names:
    print(f"  [{n}]")

print("\n=== 名称匹配检查 ===")
for account, org_name in orgs:
    matched = [n for n in item_names
               if org_name and (org_name in n or n in org_name)]
    print(f"  {account} [{org_name}] => 匹配: {matched if matched else '无匹配 (兜底返回全量)'}")

conn.close()
