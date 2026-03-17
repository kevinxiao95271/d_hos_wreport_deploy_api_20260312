import psycopg2, sys
sys.stdout.reconfigure(encoding='utf-8')

conn = psycopg2.connect(
    host='119.167.165.27', port=5432, dbname='zjylzl',
    user='postgres', password='zjylzl',
    options='-c search_path=zjylzl'
)
conn.autocommit = False
cur = conn.cursor()

# 要清除的任务 ID
task_ids = (2032074032907608065, 2032093179871776769)

# 查下受影响的 record
cur.execute("SELECT id, org_name FROM wr_record WHERE task_id = ANY(%s) AND del_flag=0", (list(task_ids),))
records = cur.fetchall()
record_ids = [r[0] for r in records]
print(f"找到 record 数: {len(records)}")
for r in records:
    print(f"  record {r[0]}  org={r[1]}")

if record_ids:
    # 删 record_value
    cur.execute("DELETE FROM wr_record_value WHERE record_id = ANY(%s)", (record_ids,))
    print(f"deleted wr_record_value: {cur.rowcount} 行")

    # 软删 record
    cur.execute("UPDATE wr_record SET del_flag=1 WHERE id = ANY(%s)", (record_ids,))
    print(f"soft-deleted wr_record: {cur.rowcount} 行")

# 删 task_org_scope
cur.execute("DELETE FROM wr_task_org_scope WHERE task_id = ANY(%s)", (list(task_ids),))
print(f"deleted wr_task_org_scope: {cur.rowcount} 行")

# 软删 task
cur.execute("UPDATE wr_task SET del_flag=1 WHERE id = ANY(%s)", (list(task_ids),))
print(f"soft-deleted wr_task: {cur.rowcount} 行")

conn.commit()
print("✓ 清除完成")
conn.close()
