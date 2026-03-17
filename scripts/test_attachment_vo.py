import requests, json, sys
sys.stdout.reconfigure(encoding='utf-8')

base = 'http://localhost:8083'
r = requests.post(f'{base}/api/auth/login', json={'account':'wr_admin','password':'Admin@2025'})
token = r.json()['data']['token']

# 找一个有附件的 record
import psycopg2
conn = psycopg2.connect(host='119.167.165.27', port=5432, dbname='zjylzl',
    user='postgres', password='zjylzl', options='-c search_path=zjylzl')
cur = conn.cursor()
cur.execute("""
    SELECT a.record_id, a.item_id, a.attach_name
    FROM wr_attachment a
    WHERE a.del_flag = 0
    LIMIT 5
""")
rows = cur.fetchall()
conn.close()

if not rows:
    print("没有附件数据")
else:
    record_id = rows[0][0]
    print(f"使用 record_id={record_id}")
    resp = requests.get(f'{base}/wr/attachment/list/{record_id}', headers={'Authorization': token})
    data = resp.json()
    print(f"code={data.get('code')}")
    for a in (data.get('data') or []):
        print(f"  [{a.get('attachName')}]")
        print(f"    itemId     : {a.get('itemId')}")
        print(f"    itemName   : {a.get('itemName')}")
        print(f"    headerPath : {a.get('headerPath')}")
