import requests, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

BASE = "http://localhost:8083"
requests.post(f"{BASE}/api/auth/dev-reset-pwd")

for acc, pwd in [("wr_org_a", "OrgA@2025"), ("wr_org_b", "OrgB@2025"), ("wr_admin", "Admin@2025")]:
    resp = requests.post(f"{BASE}/api/auth/login", json={"account": acc, "password": pwd}).json()
    d = resp.get("data", {})
    org_id   = d.get("orgId", "N/A")
    org_name = d.get("orgName", "N/A")
    real     = d.get("realName", "N/A")
    role     = d.get("roleCode", "N/A")
    same = " ⚠️ 相同orgId!" if org_id == "1986677412049780738" else " ✅"
    print(f"[{acc}] realName={real}  orgId={org_id}  orgName={org_name}  role={role}{same}")

# /api/auth/users 确认列表也正确
print()
token = requests.post(f"{BASE}/api/auth/login",
    json={"account": "wr_admin", "password": "Admin@2025"}).json()["data"]["token"]
users = requests.get(f"{BASE}/api/auth/users",
    headers={"Authorization": token}).json().get("data", [])
print(f"/api/auth/users 返回 {len(users)} 条，前5条：")
for u in users[:5]:
    print(f"  account={u.get('account')}  realName={u.get('realName')}  orgName={u.get('orgName')}")
