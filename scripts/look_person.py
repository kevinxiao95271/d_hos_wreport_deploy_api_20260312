import psycopg2

conn = psycopg2.connect(host='119.167.165.27', port=5432, dbname='zjylzl',
                        user='postgres', password='zjylzl', options='-c search_path=zjylzl')
cur = conn.cursor()

# 找 person 相关表
cur.execute("""
    SELECT table_name FROM information_schema.tables
    WHERE table_schema = current_schema()
      AND table_name ILIKE '%person%'
    ORDER BY table_name
""")
print("=== person 相关表 ===")
for r in cur.fetchall():
    print(" ", r[0])

# 找 org / dept / user 相关表
cur.execute("""
    SELECT table_name FROM information_schema.tables
    WHERE table_schema = current_schema()
      AND (table_name ILIKE '%org%' OR table_name ILIKE '%dept%' OR table_name ILIKE '%user%')
    ORDER BY table_name
""")
print("\n=== org / dept / user 相关表 ===")
for r in cur.fetchall():
    print(" ", r[0])

# 找所有 hr_ 开头的表
cur.execute("""
    SELECT table_name FROM information_schema.tables
    WHERE table_schema = current_schema()
      AND table_name ILIKE 'hr_%'
    ORDER BY table_name
""")
print("\n=== hr_ 开头的表 ===")
hr_tables = [r[0] for r in cur.fetchall()]
for t in hr_tables:
    print(" ", t)

# 对每个 hr_ 表，打印列名和前3条数据
for t in hr_tables:
    cur.execute(f"SELECT column_name FROM information_schema.columns WHERE table_name='{t}' ORDER BY ordinal_position")
    cols = [r[0] for r in cur.fetchall()]
    print(f"\n--- {t} 列: {cols} ---")
    cur.execute(f"SELECT * FROM {t} LIMIT 3")
    for row in cur.fetchall():
        print(" ", row)

conn.close()
