"""
演示前端如何拉取 /wr/record/detail/{recordId}
只返回本机构自己的数据，附件3矩阵模板只有1列
"""
import sys, io, json, requests
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

BASE      = "http://localhost:8083"
RECORD_ID = "2032186935832743937"

# ── Step 1: 登录（前端已有 token，此处模拟）────────────────────────────────
requests.post(f"{BASE}/api/auth/dev-reset-pwd")
r = requests.post(f"{BASE}/api/auth/login",
                  json={"account": "wr_org_a", "password": "OrgA@2025"})
login_data = r.json()["data"]
token      = login_data["token"]
org_name   = login_data.get("orgName", "")
print("=" * 60)
print(f"登录账号: wr_org_a")
print(f"orgName : {org_name}  ← 即附件3中该机构对应的列名")
print(f"token   : {token[:40]}...")
print()

H = {"Authorization": token}

# ── Step 2: 拉取记录详情（唯一需要调用的接口）─────────────────────────────
print(f"GET {BASE}/wr/record/detail/{RECORD_ID}")
r2 = requests.get(f"{BASE}/wr/record/detail/{RECORD_ID}", headers=H)
resp = r2.json()
print(f"HTTP {r2.status_code}  code={resp['code']}")
print()

data = resp.get("data", {})

# ── record 基本信息 ──────────────────────────────────────────────────────────
rec = data.get("record", {})
print("── record ──")
print(f"  id         : {rec.get('id')}")
print(f"  taskId     : {rec.get('taskId')}")
print(f"  templateId : {rec.get('templateId')}")
print(f"  orgName    : {rec.get('orgName')}")
print(f"  status     : {rec.get('status')}  ({data.get('statusLabel')})")
print()

# ── items（列定义，矩阵模板只有1列）─────────────────────────────────────────
items = data.get("items", [])
leaf_items = [it for it in items if it.get("isLeaf") == 1]
print(f"── items（列定义）共 {len(items)} 个，其中叶子列 {len(leaf_items)} 个 ──")
for it in leaf_items:
    print(f"  itemId={it['id']}  colIndex={it['colIndex']}  name={it['itemName']}  valueType={it['valueType']}")
print()

# ── rows（行定义，省市县层级）──────────────────────────────────────────────
rows = data.get("rows", [])
print(f"── rows（行定义）共 {len(rows)} 行 ──")
if rows:
    for row in rows[:5]:
        indent = "  " * (row.get("rowLevel", 1) - 1)
        print(f"  {indent}rowIndex={row['rowIndex']}  level={row['rowLevel']}  label={row['rowLabel']}")
    if len(rows) > 5:
        print(f"  ... 共 {len(rows)} 行，此处只展示前5行")
print()

# ── values（勾选值）─────────────────────────────────────────────────────────
values = data.get("values", [])
print(f"── values（勾选数据）共 {len(values)} 条 ──")
if values:
    # 按 rowIndex 建索引
    val_map = {v["rowIndex"]: v["cellValue"] for v in values}
    for v in values[:10]:
        row_label = next((rw["rowLabel"] for rw in rows if rw["rowIndex"] == v["rowIndex"]), "?")
        print(f"  rowIndex={v['rowIndex']}  [{row_label}]  value={v['cellValue']}")
    if len(values) > 10:
        print(f"  ... 共 {len(values)} 条")
print()

# ── 前端渲染逻辑说明 ─────────────────────────────────────────────────────────
print("=" * 60)
print("【前端渲染附件3矩阵的逻辑】")
print()
if leaf_items:
    col = leaf_items[0]
    print(f"1. 列头 = items[0].itemName = '{col['itemName']}'")
    print(f"   （也等于登录态的 orgName='{org_name}'，可从任意一处取）")
else:
    print("1. items 为空，用登录态 orgName 作列头")
print()
print("2. 行 = rows，按 rowLevel 缩进渲染（1=省汇总, 2=市, 3=县区）")
print()
print("3. 单元格值 = values.find(v => v.rowIndex === row.rowIndex)?.cellValue")
print("   '1' → ✓ 已成立，'0'/null → 空/未填")
print()
print("4. 只有一列，不需要再调 /wr/template/items 或 /wr/template/detail/full")
