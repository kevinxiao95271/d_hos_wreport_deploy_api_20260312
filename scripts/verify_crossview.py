import requests, sys, io, json
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

BASE  = "http://localhost:8083"
token = requests.post(f"{BASE}/api/auth/login",
                      json={"account":"wr_admin","password":"Admin@2025"}).json()["data"]["token"]
hdrs  = {"Authorization": token}

# ── 附件2 crossview ──────────────────────────────────────────────────────────
r2 = requests.get(f"{BASE}/wr/record/admin/crossview",
                  params={"taskId": "3000000000000001"}, headers=hdrs)
d2 = r2.json()["data"]
print(f"附件2 templateType={d2['templateType']}")
print(f"  items 数: {len(d2.get('items') or [])}")
print(f"  orgRows 数: {len(d2.get('orgRows') or [])}")
if d2.get("orgRows"):
    row = d2["orgRows"][0]
    print(f"  第1行 orgName={row['orgName']} status={row['statusLabel']} cells数={len(row.get('cells') or [])}")
    for c in (row.get("cells") or [])[:3]:
        print(f"    itemId={c['itemId']} value={c['cellValue']} label={c['cellLabel']}")

print()

# ── 附件3 crossview ──────────────────────────────────────────────────────────
r3 = requests.get(f"{BASE}/wr/record/admin/crossview",
                  params={"taskId": "3000000000000002"}, headers=hdrs)
d3 = r3.json()["data"]
print(f"附件3 templateType={d3['templateType']}")
print(f"  rows 数: {len(d3.get('rows') or [])}")
print(f"  numberItems: {[i['itemName'] for i in (d3.get('numberItems') or [])]}")
print(f"  orgCols 数: {len(d3.get('orgCols') or [])}")
print(f"  matrixValues 数: {len(d3.get('matrixValues') or [])}")
print(f"  numberValues 数: {len(d3.get('numberValues') or [])}")
if d3.get("orgCols"):
    for c in d3["orgCols"][:3]:
        print(f"    orgName={c['orgName']} status={c['statusLabel']}")
