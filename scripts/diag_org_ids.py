import psycopg2, sys
sys.stdout.reconfigure(encoding='utf-8')

DB = dict(host='119.167.165.27', port=5432, dbname='zjylzl',
          user='postgres', password='zjylzl', options='-c search_path=zjylzl')
conn = psycopg2.connect(**DB)
cur  = conn.cursor()

# 1. scope 里的 org_id 在 sys_user 里存不存在
cur.execute("SELECT org_id FROM wr_task_org_scope WHERE task_id=3000000000000001 LIMIT 5")
scope_ids = [r[0] for r in cur.fetchall()]
print(f"scope org_ids: {scope_ids}")

for oid in scope_ids[:3]:
    cur.execute("SELECT user_id, account, real_name, org_id FROM sys_user WHERE org_id=%s LIMIT 2", (oid,))
    rows = cur.fetchall()
    print(f"  sys_user WHERE org_id={oid}: {rows}")

# 2. wr_record 里的 org_id 样本
cur.execute("SELECT org_id, org_name FROM wr_record WHERE task_id=3000000000000001 AND del_flag=0 LIMIT 5")
rec_rows = cur.fetchall()
print(f"\nwr_record org_ids for task1: {rec_rows}")

# 3. 检查 wr_record org_id 是否在 wr_task_org_scope 里
if rec_rows:
    rec_org_ids = [r[0] for r in rec_rows]
    cur.execute("SELECT org_id FROM wr_task_org_scope WHERE task_id=3000000000000001 AND org_id = ANY(%s)", (rec_org_ids,))
    matched = cur.fetchall()
    print(f"wr_record org_ids 在 scope 里的交集: {matched}")

# 4. hr_organization 样本
cur.execute("SELECT org_id, org_name FROM hr_organization LIMIT 5")
print(f"\nhr_organization samples: {cur.fetchall()}")

# 5. sys_user org_id 样本（真实用户的 org_id 是什么范围）
cur.execute("SELECT DISTINCT org_id FROM sys_user WHERE del_flag='N' LIMIT 5")
print(f"sys_user distinct org_ids: {[r[0] for r in cur.fetchall()]}")

conn.close()
