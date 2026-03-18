import psycopg2

conn = psycopg2.connect(host='119.167.165.27', port=5432, dbname='zjylzl',
                        user='postgres', password='zjylzl', options='-c search_path=zjylzl')
cur = conn.cursor()

cur.execute("""
    SELECT person_id, person_name, org_id, parent_org_id, title, position_name, mobile
    FROM hr_person
    WHERE organization_type = '2'
      AND del_flag = 'N'
    ORDER BY org_id
""")
rows = cur.fetchall()
print(f"共 {len(rows)} 条\n")
print(f"{'person_id':<22} {'person_name':<20} {'org_id':<22} {'parent_org_id':<22} {'title':<15} {'position_name'}")
print("-" * 130)
for r in rows:
    print(f"{str(r[0]):<22} {str(r[1]):<20} {str(r[2]):<22} {str(r[3]):<22} {str(r[4]):<15} {r[5]}")

conn.close()
