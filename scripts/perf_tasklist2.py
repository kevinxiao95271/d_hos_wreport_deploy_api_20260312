import requests, time, json, sys
sys.stdout.reconfigure(encoding='utf-8')

base = 'http://localhost:8083'

r = requests.post(f'{base}/api/auth/login', json={'account':'wr_admin','password':'Admin@2025'})
token = r.json()['data']['token']
headers_admin = {'Authorization': token}

ro = requests.post(f'{base}/api/auth/login', json={'account':'wr_org_a','password':'OrgA@2025'})
token_org = ro.json()['data']['token']
headers_org = {'Authorization': token_org}

print("--- admin page raw ---")
resp = requests.get(f'{base}/wr/task/admin/page?pageNum=1&pageSize=20', headers=headers_admin)
print(json.dumps(resp.json(), ensure_ascii=False, indent=2)[:1500])

print()
print("--- org my/list raw ---")
resp2 = requests.get(f'{base}/wr/task/my/list', headers=headers_org)
print(json.dumps(resp2.json(), ensure_ascii=False, indent=2)[:1500])
