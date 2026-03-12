import psycopg2, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

conn = psycopg2.connect(
    host="119.167.165.27", port=5432, dbname="zjylzl",
    user="postgres", password="zjylzl",
    options="-c search_path=zjylzl"
)
cur = conn.cursor()
cur.execute("""
    SELECT tablename, indexname
    FROM pg_indexes
    WHERE schemaname = 'zjylzl' AND tablename LIKE 'wr_%'
    ORDER BY tablename, indexname
""")
rows = cur.fetchall()
cur_table = None
for tbl, idx in rows:
    if tbl != cur_table:
        print(f"\n{tbl}")
        cur_table = tbl
    print(f"    {idx}")
conn.close()
print(f"\n共 {len(rows)} 个索引")
