import psycopg2

conn = psycopg2.connect(host='119.167.165.27', port=5432, dbname='zjylzl',
                        user='postgres', password='zjylzl', options='-c search_path=zjylzl')
cur = conn.cursor()

cur.execute("""
    SELECT u.account, u.real_name, u.org_id,
           o.org_name, o.org_type, o.org_level, o.org_pids
    FROM sys_user u
    LEFT JOIN hr_organization o ON o.org_id = u.org_id
    WHERE u.account LIKE 'qc_%' AND u.del_flag = 'N'
    ORDER BY u.account
""")
rows = cur.fetchall()
print(f"{'account':<10} {'real_name':<25} {'org_id':<22} {'org_name(hr_org)':<30} {'org_type'} {'org_pids'}")
print("-" * 140)
for r in rows:
    print(f"{r[0]:<10} {str(r[1]):<25} {str(r[2]):<22} {str(r[3]):<30} {str(r[4]):<9} {r[6]}")

conn.close()
