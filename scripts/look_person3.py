import psycopg2

conn = psycopg2.connect(host='119.167.165.27', port=5432, dbname='zjylzl',
                        user='postgres', password='zjylzl', options='-c search_path=zjylzl')
cur = conn.cursor()

org_ids = [1990670394004348930, 1990606397926305794, 2033466019170775041, 2031900517168349186]

print("=== hr_organization 里对应机构 ===")
for oid in org_ids:
    cur.execute("SELECT org_id, org_name, org_type FROM hr_organization WHERE org_id = %s", (oid,))
    r = cur.fetchone()
    print(f"  {oid} -> {r}")

print("\n=== sys_user 里 org_id 对应的用户 ===")
for oid in org_ids:
    cur.execute("SELECT user_id, account, real_name FROM sys_user WHERE org_id = %s AND del_flag='N'", (oid,))
    rows = cur.fetchall()
    print(f"  org_id={oid}: {rows}")

conn.close()
