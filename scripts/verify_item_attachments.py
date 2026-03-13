import requests, json

BASE = 'http://localhost:8083'
RECORD_ID = '2032189692832649218'  # wr_org_a 附件2 record

requests.post(f'{BASE}/api/auth/dev-reset-pwd')
r = requests.post(f'{BASE}/api/auth/login', json={'account': 'wr_org_a', 'password': 'OrgA@2025'})
token = r.json()['data']['token']
H = {'Authorization': token}

r2 = requests.get(f'{BASE}/wr/record/detail/{RECORD_ID}', headers=H)
data = r2.json()['data']

# Items with requireAttachment=1
items = data.get('items', [])
req_items = [it for it in items if it.get('requireAttachment') == 1]
print(f'=== requireAttachment=1 items: {len(req_items)} ===')
for it in req_items:
    print(f'  itemId={it["id"]}  col={it["colIndex"]}  name={it["itemName"]}')

# Attachments grouped
attachments = data.get('attachments', [])
no_item_att   = [a for a in attachments if not a.get('itemId')]
item_bound_att = [a for a in attachments if a.get('itemId')]

print(f'\n=== attachments total={len(attachments)} ===')
print(f'  全局附件 (itemId=null): {len(no_item_att)}')
print(f'  绑定到具体列 (itemId!=null): {len(item_bound_att)}')
for a in item_bound_att:
    # Find matching item name
    match = next((it for it in items if str(it['id']) == str(a.get('itemId'))), None)
    item_name = match['itemName'] if match else '(unknown)'
    print(f'  -> itemId={a["itemId"]}  [{item_name}]  file={a["attachName"]}  size={a["attachSize"]}B')

print('\n=== How frontend should render ===')
print('For each requireAttachment=1 item, look up attachments where attachment.itemId == item.id')
for it in req_items:
    related = [a for a in attachments if str(a.get('itemId')) == str(it['id'])]
    print(f'  col={it["colIndex"]} [{it["itemName"]}] => {len(related)} attachment(s):')
    for a in related:
        print(f'    - {a["attachName"]} ({a["attachSize"]}B)')
