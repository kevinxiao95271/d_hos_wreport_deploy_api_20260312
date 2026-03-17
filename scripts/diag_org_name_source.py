import psycopg2, sys
sys.stdout.reconfigure(encoding='utf-8')

DB = dict(host='119.167.165.27', port=5432, dbname='zjylzl',
          user='postgres', password='zjylzl', options='-c search_path=zjylzl')
conn = psycopg2.connect(**DB)
cur  = conn.cursor()

# wr_record 里有没有这两个任务下的所有机构的 org_name？
for tid in [3000000000000001, 3000000000000002]:
    cur.execute("""
        SELECT DISTINCT org_id, org_name
        FROM wr_record WHERE task_id=%s AND del_flag=0
        ORDER BY org_id
    """, (tid,))
    rows = cur.fetchall()
    print(f"\ntask {tid} wr_record org列表({len(rows)}个):")
    for r in rows:
        print(f"  org_id={r[0]}  org_name={r[1]}")

# scope 里有哪些机构？
print("\n--- scope vs wr_record 的 org_id 对比 ---")
for tid in [3000000000000001, 3000000000000002]:
    cur.execute("SELECT org_id FROM wr_task_org_scope WHERE task_id=%s ORDER BY org_id", (tid,))
    scope_ids = set(r[0] for r in cur.fetchall())
    cur.execute("SELECT DISTINCT org_id FROM wr_record WHERE task_id=%s AND del_flag=0", (tid,))
    rec_ids   = set(r[0] for r in cur.fetchall())
    in_both   = scope_ids & rec_ids
    only_scope = scope_ids - rec_ids
    only_rec   = rec_ids - scope_ids
    print(f"\ntask {tid}:")
    print(f"  scope({len(scope_ids)}): {sorted(scope_ids)[:3]}...")
    print(f"  wr_record({len(rec_ids)}): {sorted(rec_ids)[:3]}...")
    print(f"  交集: {in_both}")
    print(f"  仅在scope(未填报): {only_scope}")
    print(f"  仅在wr_record(scope外有记录): {only_rec}")

conn.close()
