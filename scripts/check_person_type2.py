import psycopg2

conn = psycopg2.connect(host='119.167.165.27', port=5432, dbname='zjylzl',
                        user='postgres', password='zjylzl', options='-c search_path=zjylzl')
cur = conn.cursor()

# 查 organization_type='2' 的所有 org_id 及其在 hr_organization 的含义
cur.execute("""
    SELECT p.person_id, p.person_name, p.org_id, p.del_flag,
           o.org_name, o.org_type
    FROM hr_person p
    LEFT JOIN hr_organization o ON o.org_id = p.org_id
    WHERE p.organization_type = '2'
    ORDER BY p.create_time
""")
rows = cur.fetchall()
print(f"共 {len(rows)} 条\n")
print(f"{'person_name':<15} {'org_id':<22} {'org_name(hr_org)':<30} {'org_type':<10} {'del_flag'}")
print("-" * 100)
for r in rows:
    print(f"{str(r[1]):<15} {str(r[2]):<22} {str(r[4]):<30} {str(r[5]):<10} {r[3]}")

conn.close()
