import requests, sys
sys.stdout.reconfigure(encoding='utf-8')

base  = 'http://localhost:8083'
r     = requests.post(f'{base}/api/auth/login', json={'account':'wr_admin','password':'Admin@2025'})
token = r.json()['data']['token']
H     = {'Authorization': token}

items = requests.get(f'{base}/wr/template/items/2000000000000001', headers=H).json().get('data', [])
leaves = [i for i in items if i.get('isLeaf') == 1]
print(f"全部节点: {len(items)}  叶子节点: {len(leaves)}")
print()
for i in leaves:
    path = i.get('headerPath') or []
    print(f"  col={i.get('colIndex'):2}  depth={len(path)}  {' / '.join(path)}")
