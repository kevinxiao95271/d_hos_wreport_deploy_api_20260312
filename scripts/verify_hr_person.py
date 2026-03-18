import psycopg2

conn = psycopg2.connect(host='119.167.165.27', port=5432, dbname='zjylzl',
                        user='postgres', password='zjylzl', options='-c search_path=zjylzl')
cur = conn.cursor()

cur.execute("""
    SELECT p.person_name, p.org_id, o.org_name,
           CASE WHEN p.person_name = o.org_name THEN 'OK' ELSE 'MISMATCH' END AS match
    FROM hr_person p
    LEFT JOIN hr_organization o ON o.org_id = p.org_id
    WHERE p.organization_type = '2' AND p.del_flag = 'N'
    ORDER BY p.create_time
""")
rows = cur.fetchall()
print(f"hr_person type=2 del_flag=N 共 {len(rows)} 条\n")
print(f"{'person_name':<30} {'org_name(hr_org)':<30} match")
print("-" * 75)
for r in rows:
    print(f"{str(r[0]):<30} {str(r[1]):<30} {r[3]}")

conn.close()
