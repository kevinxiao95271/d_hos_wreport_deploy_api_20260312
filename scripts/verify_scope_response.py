import requests, json, sys
sys.stdout.reconfigure(encoding='utf-8')

base = 'http://localhost:8083'
r = requests.post(f'{base}/api/auth/login', json={'account':'wr_admin','password':'Admin@2025'})
token = r.json()['data']['token']
H = {'Authorization': token}

for task_id in ['3000000000000001', '3000000000000002']:
    resp = requests.get(f'{base}/wr/task/scope/{task_id}', headers=H).json()
    orgs = resp.get('data', {}).get('orgs', [])
    print(f"\n=== task {task_id} (orgs={len(orgs)}) ===")
    for o in orgs:
        print(f"  orgId={o.get('orgId')}  orgName={o.get('orgName')}  recordStatus={o.get('recordStatus')}")

    null_name = sum(1 for o in orgs if not o.get('orgName'))
    has_status = sum(1 for o in orgs if o.get('recordStatus') is not None)
    print(f"  → orgName有值: {len(orgs)-null_name}/{len(orgs)}  recordStatus有值: {has_status}/{len(orgs)}")
