import requests, time, json, sys
sys.stdout.reconfigure(encoding='utf-8')

base = 'http://localhost:8083'

# login
r = requests.post(f'{base}/api/auth/login', json={'account':'wr_admin','password':'Admin@2025'})
token = r.json()['data']['token']

apis = [
    ('admin task page p1',  f'{base}/wr/task/admin/page?pageNum=1&pageSize=20'),
    ('admin task page p2',  f'{base}/wr/task/admin/page?pageNum=2&pageSize=20'),
    ('org task list (wr_org_a)', None),   # will use org token
]

headers_admin = {'Authorization': token}

# org login
ro = requests.post(f'{base}/api/auth/login', json={'account':'wr_org_a','password':'OrgA@2025'})
token_org = ro.json()['data']['token']
headers_org = {'Authorization': token_org}

print("=== 管理员端 - 任务列表 ===")
for label, url in [
    ('admin /wr/task/admin/page p1 (size=20)', f'{base}/wr/task/admin/page?pageNum=1&pageSize=20'),
    ('admin /wr/task/admin/page p1 (size=50)', f'{base}/wr/task/admin/page?pageNum=1&pageSize=50'),
]:
    times = []
    for _ in range(3):
        t0 = time.time()
        resp = requests.get(url, headers=headers_admin)
        elapsed = (time.time() - t0) * 1000
        times.append(elapsed)
        d = resp.json().get('data', {})
        total = d.get('total', '?') if isinstance(d, dict) else '?'
    avg = sum(times)/len(times)
    print(f"  {label}")
    print(f"    runs: {[f'{t:.0f}ms' for t in times]}  avg={avg:.0f}ms  total_records={total}")

print()
print("=== 机构用户端 - 任务列表 ===")
for label, url in [
    ('org  /wr/task/my/list', f'{base}/wr/task/my/list'),
]:
    times = []
    total = '?'
    for _ in range(3):
        t0 = time.time()
        resp = requests.get(url, headers=headers_org)
        elapsed = (time.time() - t0) * 1000
        times.append(elapsed)
        d = resp.json().get('data', [])
        total = len(d) if isinstance(d, list) else '?'
    avg = sum(times)/len(times)
    print(f"  {label}")
    print(f"    runs: {[f'{t:.0f}ms' for t in times]}  avg={avg:.0f}ms  returned_count={total}")
