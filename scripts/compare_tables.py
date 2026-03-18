import psycopg2

conn = psycopg2.connect(host='119.167.165.27', port=5432, dbname='zjylzl',
                        user='postgres', password='zjylzl', options='-c search_path=zjylzl')
cur = conn.cursor()

for tbl in ['sys_user', 'hr_person']:
    cur.execute("""
        SELECT column_name, data_type, is_nullable
        FROM information_schema.columns
        WHERE table_schema='zjylzl' AND table_name=%s
        ORDER BY ordinal_position
    """, (tbl,))
    cols = cur.fetchall()
    print(f"\n=== {tbl} ===")
    for c in cols:
        print(f"  {c[0]:<35} {c[1]:<25} {'NOT NULL' if c[2]=='NO' else ''}")

conn.close()
