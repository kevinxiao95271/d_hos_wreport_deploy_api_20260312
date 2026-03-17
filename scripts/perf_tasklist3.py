import requests, time, json, sys
sys.stdout.reconfigure(encoding='utf-8')

base = 'http://localhost:8083'

r = requests.post(f'{base}/api/auth/login', json={'account':'wr_admin','password':'Admin@2025'})
token = r.json()['data']['token']
headers_admin = {'Authorization': token}

ro = requests.post(f'{base}/api/auth/login', json={'account':'wr_org_a','password':'OrgA@2025'})
token_org = ro.json()['data']['token']
headers_org = {'Authorization': token_org}

print("--- admin /wr/task/page ---")
times = []
for _ in range(5):
    t0 = time.time()
    resp = requests.get(f'{base}/wr/task/page?pageNum=1&pageSize=20', headers=headers_admin)
    elapsed = (time.time()-t0)*1000
    times.append(elapsed)
d = resp.json()
print(f"  runs: {[f'{t:.0f}ms' for t in times]}  avg={sum(times)/len(times):.0f}ms")
print(f"  resp code={d.get('code')}  data keys={list(d.get('data',{}).keys()) if isinstance(d.get('data'),dict) else type(d.get('data'))}")
if d.get('code') == 200:
    pg = d['data']
    print(f"  total={pg.get('total')}  pages={pg.get('pages')}  list_len={len(pg.get('list',[]))}")

print()
print("--- org /wr/task/active ---")
times2 = []
for _ in range(5):
    t0 = time.time()
    resp2 = requests.get(f'{base}/wr/task/active', headers=headers_org)
    elapsed = (time.time()-t0)*1000
    times2.append(elapsed)
d2 = resp2.json()
print(f"  runs: {[f'{t:.0f}ms' for t in times2]}  avg={sum(times2)/len(times2):.0f}ms")
print(f"  resp code={d2.get('code')}  count={len(d2.get('data',[]) or [])}")
if d2.get('message') and d2.get('code') != 200:
    print(f"  error: {d2.get('message')}")
