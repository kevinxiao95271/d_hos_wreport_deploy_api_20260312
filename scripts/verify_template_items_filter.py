import sys, io, requests
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

BASE    = "http://localhost:8083"
TPL3_ID = "2000000000000002"

requests.post(f"{BASE}/api/auth/dev-reset-pwd")

for account, pwd, role in [
    ("wr_org_a",  "OrgA@2025",  "qcUser"),
    ("wr_admin",  "Admin@2025", "deptAdmin"),
]:
    r = requests.post(f"{BASE}/api/auth/login", json={"account": account, "password": pwd})
    d = r.json()["data"]
    token   = d["token"]
    orgName = d.get("orgName", "")
    H = {"Authorization": token}

    r2 = requests.get(f"{BASE}/wr/template/items/{TPL3_ID}", headers=H)
    items = r2.json().get("data", [])
    leaf  = [it for it in items if it.get("isLeaf") == 1]

    print(f"[{role}] {account} (orgName={orgName})")
    print(f"  /wr/template/items  => {len(items)} items, {len(leaf)} leaf")
    for it in leaf:
        print(f"    col={it['colIndex']} name={it['itemName']}")

    r3 = requests.get(f"{BASE}/wr/template/detail/full/{TPL3_ID}", headers=H)
    vo    = r3.json().get("data", {})
    items2 = vo.get("items", [])
    leaf2  = [it for it in items2 if it.get("isLeaf") == 1]
    print(f"  /wr/template/detail/full => {len(items2)} items, {len(leaf2)} leaf")
    for it in leaf2:
        print(f"    col={it['colIndex']} name={it['itemName']}")
    print()
