import psycopg2
DSN = "host=119.167.165.27 port=5432 dbname=zjylzl user=postgres password=zjylzl options='-c search_path=zjylzl'"
conn = psycopg2.connect(DSN)
cur = conn.cursor()
cur.execute("""
    SELECT column_name, data_type, is_nullable, column_default
    FROM information_schema.columns
    WHERE table_schema = current_schema() AND table_name = 'sys_user'
    ORDER BY ordinal_position
""")
print(f"{'列名':<30} {'类型':<25} {'可为NULL':<10} {'默认值'}")
print("-"*80)
for col, dtype, nullable, default in cur.fetchall():
    print(f"{col:<30} {dtype:<25} {nullable:<10} {default or ''}")
conn.close()
