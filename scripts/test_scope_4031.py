"""
专门测试 4031 返回码：
找一个有 status>=1 记录的任务，尝试移除该机构
"""
import requests, json, sys, psycopg2
sys.stdout.reconfigure(encoding='utf-8')

base = 'http://localhost:8083'
r    = requests.post(f'{base}/api/auth/login', json={'account':'wr_admin','password':'Admin@2025'})
token = r.json()['data']['token']
H = {'Authorization': token}

DB = dict(host='119.167.165.27', port=5432, dbname='zjylzl',
          user='postgres', password='zjylzl', options='-c search_path=zjylzl')
conn = psycopg2.connect(**DB)
cur  = conn.cursor()

# 找 status>=1 的记录
cur.execute("""
    SELECT r.task_id, r.org_id, r.org_name, r.status
    FROM wr_record r
    JOIN wr_task_org_scope s ON s.task_id=r.task_id AND s.org_id=r.org_id
    WHERE r.del_flag=0 AND r.status >= 1
    LIMIT 1
""")
row = cur.fetchone()
conn.close()

if not row:
    print("数据库中没有 status>=1 的记录，跳过")
else:
    task_id, org_id, org_name, status = row
    print(f"找到: task_id={task_id}  org={org_name}(id={org_id})  status={status}")

    scope_resp = requests.get(f'{base}/wr/task/scope/{task_id}', headers=H).json()
    # orgId 在 JSON 里可能是字符串（Java Long 序列化），统一转 int 再比较
    all_ids = [int(o['orgId']) for o in scope_resp.get('data', {}).get('orgs', [])]
    print(f"当前 scope orgIds: {all_ids}")

    # 尝试移除这个已提交的机构
    new_ids = [x for x in all_ids if x != org_id]
    res = requests.post(f'{base}/wr/task/scope/{task_id}', json={'orgIds': new_ids}, headers=H).json()
    print(f"POST scope => code={res.get('code')}  msg={res.get('message','')}")
    assert res.get('code') == 4031, f"✗ 期望4031，得到{res.get('code')}"
    print("✓ 正确返回 4031，移除被阻断")
