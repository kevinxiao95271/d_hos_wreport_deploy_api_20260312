import sys, io, requests
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

BASE = "http://localhost:8083"
requests.post(f"{BASE}/api/auth/dev-reset-pwd")
token = requests.post(f"{BASE}/api/auth/login",
    json={"account": "wr_admin", "password": "Admin@2025"}).json()["data"]["token"]
h = {"Authorization": token}

# 1. 模板列表
tpls = requests.get(f"{BASE}/wr/template/list", headers=h).json().get("data", [])
print(f"[PASS] /wr/template/list  count={len(tpls)}")
for t in tpls:
    print(f"       id={t['id']}  name={t['templateName']}")

# 2. 附件2 完整详情
d2 = requests.get(f"{BASE}/wr/template/detail/full/2000000000000001", headers=h).json().get("data", {})
items2 = d2.get("items", [])
rows2  = d2.get("rows", [])
leaf2  = [x for x in items2 if x.get("isLeaf") == 1]
print(f"[PASS] 附件2 full detail  total_items={len(items2)}  leaf={len(leaf2)}  rows={len(rows2)}")
print(f"       前4个叶子: {[x['itemName'] for x in leaf2[:4]]}")

# 3. 附件3 完整详情
d3 = requests.get(f"{BASE}/wr/template/detail/full/2000000000000002", headers=h).json().get("data", {})
items3 = d3.get("items", [])
rows3  = d3.get("rows", [])
print(f"[PASS] 附件3 full detail  cols={len(items3)}  rows={len(rows3)}")
print(f"       前3列: {[x['itemName'] for x in items3[:3]]}")
print(f"       前5行: {[x['rowLabel'] for x in rows3[:5]]}")
print(f"       末3行: {[x['rowLabel'] for x in rows3[-3:]]}")

# 4. rows 专用接口
rows_api = requests.get(f"{BASE}/wr/template/rows/2000000000000002", headers=h).json().get("data", [])
print(f"[PASS] /wr/template/rows/附件3  count={len(rows_api)}")

# 5. 任务列表（机构端）
org_token = requests.post(f"{BASE}/api/auth/login",
    json={"account": "wr_org_a", "password": "OrgA@2025"}).json()["data"]["token"]
oh = {"Authorization": org_token}
tasks = requests.get(f"{BASE}/wr/task/active", headers=oh).json().get("data", [])
print(f"[PASS] /wr/task/activeTasks  count={len(tasks)}")
for t in tasks:
    print(f"       taskId={t['id']}  name={t['taskName']}  deadline={t['deadline']}")
