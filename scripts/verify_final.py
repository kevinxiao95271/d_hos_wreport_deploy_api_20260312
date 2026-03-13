import requests

BASE = 'http://localhost:8083'
requests.post(f'{BASE}/api/auth/dev-reset-pwd')
r = requests.post(f'{BASE}/api/auth/login', json={'account': 'wr_org_a', 'password': 'OrgA@2025'})
token = r.json()['data']['token']
H = {'Authorization': token}

tests = [
    ('附件3 (矩阵)', '2032186648493559809'),
    ('附件2 (三级)', '2032189692832649218'),
]

for label, rid in tests:
    r2 = requests.get(f'{BASE}/wr/record/detail/{rid}', headers=H)
    d = r2.json().get('data', {})
    items = d.get('items', 'MISSING')
    rows  = d.get('rows', [])
    vals  = d.get('values', [])
    if isinstance(items, list):
        leaf = [it for it in items if it.get('isLeaf') == 1]
        print(f'{label}: items={len(items)}(leaf={len(leaf)}), rows={len(rows)}, values={len(vals)}')
        for it in leaf:
            col = it.get('colIndex')
            name = it.get('itemName')
            print(f'  -> col={col} name={name}')
    else:
        print(f'{label}: items=MISSING')
