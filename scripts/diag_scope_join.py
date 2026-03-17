"""
诊断 selectScopeWithStatus 两个 LEFT JOIN 失效的根因
"""
import psycopg2, sys
sys.stdout.reconfigure(encoding='utf-8')

DB = dict(host='119.167.165.27', port=5432, dbname='zjylzl',
          user='postgres', password='zjylzl', options='-c search_path=zjylzl')
conn = psycopg2.connect(**DB)
cur  = conn.cursor()

TASK1 = 3000000000000001
TASK2 = 3000000000000002

for task_id in [TASK1, TASK2]:
    print(f"\n{'='*60}")
    print(f"taskId = {task_id}")

    # 1. scope orgs
    cur.execute("SELECT org_id FROM wr_task_org_scope WHERE task_id=%s ORDER BY org_id", (task_id,))
    scope_orgs = [r[0] for r in cur.fetchall()]
    print(f"  scope org_ids ({len(scope_orgs)}): {scope_orgs[:5]}{'...' if len(scope_orgs)>5 else ''}")

    if not scope_orgs:
        print("  ⚠️ scope 为空，跳过")
        continue

    sample_org = scope_orgs[0]

    # 2. hr_organization JOIN 能否命中
    cur.execute("SELECT org_id, org_name FROM hr_organization WHERE org_id = %s LIMIT 1", (sample_org,))
    hr_row = cur.fetchone()
    print(f"\n  [orgName JOIN] hr_organization WHERE org_id={sample_org}")
    print(f"  → {hr_row if hr_row else '❌ 无匹配行'}")

    # 3. 检查 hr_organization 表的 schema 和 org_id 样本
    cur.execute("SELECT table_schema FROM information_schema.tables WHERE table_name='hr_organization' LIMIT 3")
    schemas = [r[0] for r in cur.fetchall()]
    print(f"  hr_organization 所在 schema: {schemas}")

    cur.execute("SELECT org_id, org_name FROM hr_organization LIMIT 3")
    sample_hr = cur.fetchall()
    print(f"  hr_organization 样本 org_id: {[(r[0], r[1]) for r in sample_hr]}")

    # 4. wr_record JOIN 能否命中
    cur.execute("""
        SELECT org_id, status FROM wr_record
        WHERE task_id=%s AND org_id=%s AND del_flag=0 LIMIT 1
    """, (task_id, sample_org))
    rec_row = cur.fetchone()
    print(f"\n  [recordStatus JOIN] wr_record WHERE task_id={task_id} AND org_id={sample_org}")
    print(f"  → {rec_row if rec_row else '❌ 无匹配行'}")

    # 5. 查该任务有无任何记录
    cur.execute("SELECT COUNT(*) FROM wr_record WHERE task_id=%s AND del_flag=0", (task_id,))
    rec_cnt = cur.fetchone()[0]
    print(f"  该任务 wr_record 总记录数: {rec_cnt}")

    # 6. 手动执行 selectScopeWithStatus 的完整 SQL，看结果
    cur.execute("""
        SELECT s.org_id, ho.org_name, r.status
        FROM wr_task_org_scope s
        LEFT JOIN hr_organization ho ON ho.org_id = s.org_id
        LEFT JOIN wr_record r ON r.task_id = s.task_id AND r.org_id = s.org_id AND r.del_flag = 0
        WHERE s.task_id = %s
        LIMIT 5
    """, (task_id,))
    rows = cur.fetchall()
    print(f"\n  直接执行 JOIN SQL 结果 (前5行):")
    for row in rows:
        print(f"    org_id={row[0]}  org_name={row[1]}  status={row[2]}")

conn.close()
