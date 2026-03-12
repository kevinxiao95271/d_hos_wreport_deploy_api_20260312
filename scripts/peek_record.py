"""查看一条记录的前几个字段值，确认数据正确写入"""
import requests, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

BASE = "http://localhost:8083"
requests.post(f"{BASE}/api/auth/dev-reset-pwd")

token = requests.post(f"{BASE}/api/auth/login",
    json={"account": "wr_org_a", "password": "OrgA@2025"}).json()["data"]["token"]
h = {"Authorization": token}

# 找自己的附件2记录
records = requests.get(f"{BASE}/wr/record/my/page", headers=h,
    params={"pageNum": 1, "pageSize": 10, "taskId": "3000000000000001"}).json()
recs = records.get("data", {}).get("records", [])
if not recs:
    print("未找到记录")
    exit()

rec_id = recs[0]["id"]
detail = requests.get(f"{BASE}/wr/record/detail/{rec_id}", headers=h).json()["data"]

record  = detail.get("record", {})
values  = detail.get("values", [])
rows_d  = detail.get("rows", [])

print(f"recordId={rec_id}  orgName={record.get('orgName')}  status={detail.get('statusLabel')}")
print(f"values 共 {len(values)} 条  rows 共 {len(rows_d)} 条（附件2应为0）")
print()
print("前8个填报值：")
for v in values[:8]:
    print(f"  itemId={v.get('itemId')}  rowIndex={v.get('rowIndex')}  value={v.get('cellValue')}")
