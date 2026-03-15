import sys, io, requests
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

BASE = "http://localhost:8083"
r = requests.post(f"{BASE}/api/auth/login", json={"account": "wr_org_a", "password": "OrgA@2025"})
token = r.json()["data"]["token"]
org_name = r.json()["data"]["orgName"]
print(f"Login OK, orgName={org_name}")

# 附件2 (templateId=2000000000000001)
resp = requests.get(f"{BASE}/wr/template/items/2000000000000001", headers={"Authorization": token})
items = resp.json()["data"]
leaves = [i for i in items if i.get("isLeaf") == 1]
print(f"\n附件2 叶子节点数: {len(items)} 总, {len(leaves)} 叶子")
print("前10条叶子 headerPath:")
for it in leaves[:10]:
    path = " / ".join(it.get("headerPath") or [])
    print(f"  [{it['itemName']}] => {path}")

# 附件3 (templateId=2000000000000002)
resp3 = requests.get(f"{BASE}/wr/template/items/2000000000000002", headers={"Authorization": token})
items3 = resp3.json()["data"]
print(f"\n附件3 列数: {len(items3)}")
for it in items3:
    path = " / ".join(it.get("headerPath") or [])
    print(f"  [{it['itemName']}] isLeaf={it.get('isLeaf')} => {path}")
