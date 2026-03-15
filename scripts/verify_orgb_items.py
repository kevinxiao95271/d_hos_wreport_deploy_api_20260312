import sys, io, requests
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
BASE = "http://localhost:8083"
requests.post(f"{BASE}/api/auth/dev-reset-pwd")
r = requests.post(f"{BASE}/api/auth/login", json={"account": "wr_org_b", "password": "OrgB@2025"})
d = r.json()["data"]
print("orgName:", d.get("orgName"))
H = {"Authorization": d["token"]}
r2 = requests.get(f"{BASE}/wr/template/items/2000000000000002", headers=H)
items = r2.json().get("data", [])
print("items count:", len(items))
for it in items:
    col  = it.get("colIndex")
    name = it.get("itemName")
    print(f"  col={col} name={name}")
