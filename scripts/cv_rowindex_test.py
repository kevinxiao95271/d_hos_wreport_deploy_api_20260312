import requests, json, sys
sys.stdout.reconfigure(encoding='utf-8')

r = requests.post('http://localhost:8083/api/auth/login', json={'account':'wr_admin','password':'Admin@2025'})
token = r.json()['data']['token']  # already has 'Bearer ' prefix

# use init task for 附件3: taskId=3000000000000002
task_id = '3000000000000002'

# no filter
cv = requests.get('http://localhost:8083/wr/record/admin/crossview',
    params={'taskId': task_id},
    headers={'Authorization': token})
d = cv.json()
data = d.get('data', {})
print('=== No filter ===')
print('templateType:', data.get('templateType'))
rows = data.get('rows') or []
print('rows count:', len(rows))
for row in rows:
    print(f"  row {row.get('rowIndex'):3} {row.get('rowLabel')}")

print()
# filter: only rows 1, 16, 17, 18 (省市县全部成立 + 杭州市下面的几个)
cv2 = requests.get('http://localhost:8083/wr/record/admin/crossview',
    params={'taskId': task_id, 'rowIndexes': '1,16,17,18'},
    headers={'Authorization': token})
d2 = cv2.json()
data2 = d2.get('data', {})
print('=== rowIndexes=1,16,17,18 (auto-include ancestors) ===')
rows2 = data2.get('rows') or []
print('rows count:', len(rows2))
for row in rows2:
    print(f"  row {row.get('rowIndex'):3} {row.get('rowLabel')}")

print()
print('matrixValues count:', len(data2.get('matrixValues') or []))
