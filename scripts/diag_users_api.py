import psycopg2, requests, time, sys, json
sys.stdout.reconfigure(encoding='utf-8')

# ── 1. 实测接口耗时 ─────────────────────────────────────────────
base = 'http://localhost:8083'
r = requests.post(f'{base}/api/auth/login', json={'account':'wr_admin','password':'Admin@2025'})
token = r.json()['data']['token']

times = []
for i in range(3):
    t0 = time.time()
    resp = requests.get(f'{base}/api/auth/users', headers={'Authorization': token})
    elapsed = (time.time()-t0)*1000
    times.append(elapsed)
    if i == 0:
        d = resp.json()
        rows = d.get('data') or []
        print(f"返回行数: {len(rows)}")

print(f"接口耗时(3次): {[f'{t:.0f}ms' for t in times]}  avg={sum(times)/len(times):.0f}ms")

# ── 2. 直连 DB 跑 EXPLAIN ANALYZE ─────────────────────────────
conn = psycopg2.connect(
    host='119.167.165.27', port=5432, dbname='zjylzl',
    user='postgres', password='zjylzl',
    options='-c search_path=zjylzl'
)
cur = conn.cursor()

sql = """
EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
SELECT u.user_id AS "userId", u.account,
       u.real_name AS "realName", u.org_id AS "orgId",
       ho.org_name AS "orgName", u.status_flag AS "statusFlag"
FROM sys_user u
LEFT JOIN hr_organization ho ON ho.org_id = u.org_id
WHERE u.del_flag = 'N'
ORDER BY u.create_time DESC LIMIT 20
"""
t0 = time.time()
cur.execute(sql)
db_elapsed = (time.time()-t0)*1000
rows = cur.fetchall()
print(f"\nDB EXPLAIN ANALYZE 耗时: {db_elapsed:.0f}ms\n")
print("执行计划:")
for row in rows:
    print(" ", row[0])

# ── 3. 表规模 ──────────────────────────────────────────────────
cur.execute("SELECT COUNT(*) FROM sys_user WHERE del_flag='N'")
cnt_user = cur.fetchone()[0]
cur.execute("SELECT COUNT(*) FROM hr_organization")
cnt_org = cur.fetchone()[0]
print(f"\nsys_user(del_flag='N') 行数: {cnt_user}")
print(f"hr_organization 行数: {cnt_org}")

# ── 4. 检查 create_time 及现有索引 ────────────────────────────
cur.execute("""
SELECT indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'zjylzl'
  AND tablename IN ('sys_user','hr_organization')
ORDER BY tablename, indexname
""")
idxs = cur.fetchall()
print(f"\n现有索引 ({len(idxs)} 条):")
for idx in idxs:
    print(f"  [{idx[0]}] {idx[1]}")

cur.execute("""
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_schema='zjylzl' AND table_name='sys_user'
  AND column_name IN ('create_time','del_flag','org_id')
ORDER BY ordinal_position
""")
print("\nsys_user 关键列类型:")
for row in cur.fetchall():
    print(f"  {row[0]}: {row[1]}")

conn.close()
