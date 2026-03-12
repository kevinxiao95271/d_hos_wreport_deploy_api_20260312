import psycopg2
DSN = "host=119.167.165.27 port=5432 dbname=zjylzl user=postgres password=zjylzl options='-c search_path=zjylzl'"
conn = psycopg2.connect(DSN)
cur = conn.cursor()
cur.execute("""
    SELECT u.account, u.real_name, ho.org_name
    FROM sys_user u
    LEFT JOIN hr_organization ho ON ho.org_id = u.org_id
    WHERE u.account LIKE 'qc_%'
    ORDER BY u.account
""")
print(f"{'account':<10} {'real_name':<25} {'org_name'}")
print("-"*70)
for row in cur.fetchall():
    print(f"{row[0]:<10} {str(row[1]):<25} {row[2]}")
conn.close()
