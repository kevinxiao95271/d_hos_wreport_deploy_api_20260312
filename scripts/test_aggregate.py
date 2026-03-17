import requests, json, sys, psycopg2
sys.stdout.reconfigure(encoding='utf-8')

base = 'http://localhost:8083'
r = requests.post(f'{base}/api/auth/login', json={'account':'wr_admin','password':'Admin@2025'})
token = r.json()['data']['token']
H = {'Authorization': token}

DB = dict(host='119.167.165.27', port=5432, dbname='zjylzl',
          user='postgres', password='zjylzl', options='-c search_path=zjylzl')
conn = psycopg2.connect(**DB)
cur  = conn.cursor()
cur.execute("SELECT id, task_name FROM wr_task WHERE del_flag=0 ORDER BY create_time DESC LIMIT 3")
rows = cur.fetchall()
print("tasks:", rows)

if not rows:
    print("没有任务，跳过")
else:
    for task_id, task_name in rows:
        cur.execute("SELECT COUNT(*) FROM wr_task_org_scope WHERE task_id=%s", (task_id,))
        scope_cnt = cur.fetchone()[0]
        cur.execute("SELECT COUNT(*) FROM wr_record WHERE task_id=%s AND del_flag=0", (task_id,))
        record_cnt = cur.fetchone()[0]

        agg = requests.get(f'{base}/wr/record/admin/aggregate?taskId={task_id}', headers=H).json()
        data = agg.get('data', {})
        total_api = data.get('total')

        print(f"\ntask: {task_name} (id={task_id})")
        print(f"  wr_task_org_scope 数量: {scope_cnt}")
        print(f"  wr_record 数量:         {record_cnt}")
        print(f"  API total:              {total_api}")
        print(f"  draft={data.get('draft')} submitted={data.get('submitted')} approved={data.get('approved')} rejected={data.get('rejected')}")

        assert total_api == scope_cnt, f"✗ total={total_api} 与 scope_cnt={scope_cnt} 不符"
        print("  ✓ total == wr_task_org_scope 数量")

conn.close()
print("\n全部验证通过")
