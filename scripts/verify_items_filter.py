import requests, json

BASE = 'http://localhost:8083'

requests.post(f'{BASE}/api/auth/dev-reset-pwd')
r = requests.post(f'{BASE}/api/auth/login', json={'account': 'wr_org_a', 'password': 'OrgA@2025'})
token = r.json()['data']['token']
H = {'Authorization': token}
print('[OK] Logged in as wr_org_a')

# Get active tasks (no scope => all orgs see them)
r = requests.get(f'{BASE}/wr/task/active', headers=H)
tasks = r.json().get('data', [])
print(f'[OK] Active tasks ({len(tasks)}):')
for t in tasks:
    print(f'  - id={t["id"]} | {t["taskName"]} | templateId={t["templateId"]}')

# template IDs: 2000000000000001 = 附件2, 2000000000000002 = 附件3
TPL3_TASK = next((t for t in tasks if t['templateId'] == '2000000000000002'), None)
TPL2_TASK = next((t for t in tasks if t['templateId'] == '2000000000000001'), None)

for label, task in [('附件3 (checkbox matrix)', TPL3_TASK), ('附件2 (standard 3-level)', TPL2_TASK)]:
    if not task:
        print(f'[SKIP] No task found for {label}')
        continue

    tid = task['id']
    print(f'\n=== {label} (taskId={tid}) ===')

    # Get my records
    r = requests.get(f'{BASE}/wr/record/my/page', params={'taskId': tid}, headers=H)
    recs = r.json().get('data', {}).get('records', [])
    print(f'My records: {len(recs)}')

    if not recs:
        print('[SKIP] No records, creating one to test...')
        # Try to save a minimal record
        r2 = requests.post(f'{BASE}/wr/record/save', headers=H, json={
            'taskId': int(tid),
            'templateId': int(task['templateId']),
            'rows': []
        })
        print('  save:', r2.status_code, r2.text[:200])
        r = requests.get(f'{BASE}/wr/record/my/page', params={'taskId': tid}, headers=H)
        recs = r.json().get('data', {}).get('records', [])

    if recs:
        rec_id = recs[0]['id']
        print(f'Using record id={rec_id}')
        r2 = requests.get(f'{BASE}/wr/record/detail', params={'id': rec_id}, headers=H)
        detail = r2.json().get('data', {})

        items = detail.get('items', None)
        rows  = detail.get('rows',  [])
        values = detail.get('values', [])

        if items is None:
            print('[FAIL] items field not present in response!')
        else:
            leaf_items = [it for it in items if it.get('isLeaf') == 1]
            non_leaf   = [it for it in items if it.get('isLeaf') != 1]
            print(f'items total={len(items)}, leaf={len(leaf_items)}, non-leaf={len(non_leaf)}')
            print('Leaf items:')
            for it in leaf_items:
                print(f'  id={it["id"]} name={it["itemName"]} parentId={it.get("parentId")}')

        print(f'rows count: {len(rows)}, values count: {len(values)}')
