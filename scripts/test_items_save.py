"""
自测 POST /wr/template/items/save/{templateId}
覆盖4个场景：
  S1 正常保存（含新节点+已有节点混合）
  S2 校验：id 重复
  S3 校验：parentId 引用不存在的 id
  S4 校验：环形引用
  S5 验证附件2 items/save 后树结构不崩
"""
import requests, json, sys
sys.stdout.reconfigure(encoding='utf-8')

base = 'http://localhost:8083'
r    = requests.post(f'{base}/api/auth/login', json={'account':'wr_admin','password':'Admin@2025'})
token = r.json()['data']['token']
H = {'Authorization': token}

TPL2 = '2000000000000001'

def save(tpl_id, payload):
    return requests.post(f'{base}/wr/template/items/save/{tpl_id}', json=payload, headers=H).json()

def items(tpl_id):
    return requests.get(f'{base}/wr/template/items/{tpl_id}', headers=H).json().get('data', [])

# ── 场景 S1：正常保存小型树（1父2子，含1个新节点） ────────────────────────────
print("=== S1: 正常保存（已有节点+新节点） ===")
# 先创建一个测试模板
tmpl = requests.post(f'{base}/wr/template/add', json={'templateName':'自测模板_clientId','items':[]}, headers=H).json()
test_tpl = str(tmpl.get('data'))
print(f"  创建测试模板 id={test_tpl}")

payload_s1 = [
    {"id": "root",  "parentId": None,   "itemName": "L1根节点", "headerRow": 1, "colIndex": 1, "rowSpan": 1, "colSpan": 2, "isLeaf": 0, "valueType": "text", "sortNum": 1},
    {"id": "c1",    "parentId": "root", "itemName": "L2子A",   "headerRow": 2, "colIndex": 1, "rowSpan": 1, "colSpan": 1, "isLeaf": 1, "valueType": "text", "sortNum": 2},
    {"id": "c2",    "parentId": "root", "itemName": "L2子B",   "headerRow": 2, "colIndex": 2, "rowSpan": 1, "colSpan": 1, "isLeaf": 1, "valueType": "number","sortNum": 3},
]
res = save(test_tpl, payload_s1)
print(f"  save resp: code={res.get('code')}")
saved = items(test_tpl)
print(f"  保存后节点数: {len(saved)}")
for i in sorted(saved, key=lambda x: x.get('sortNum', 0)):
    print(f"    id={i['id']}  parentId={i.get('parentId')}  name={i['itemName']}  path={i.get('headerPath')}")

# ── 场景 S2：id 重复 ─────────────────────────────────────────────────────────
print("\n=== S2: id 重复（期望 400/500） ===")
res2 = save(test_tpl, [
    {"id": "dup", "parentId": None,  "itemName": "A", "headerRow": 1, "colIndex": 1, "rowSpan": 1, "colSpan": 1, "isLeaf": 1, "valueType": "text", "sortNum": 1},
    {"id": "dup", "parentId": None,  "itemName": "B", "headerRow": 1, "colIndex": 2, "rowSpan": 1, "colSpan": 1, "isLeaf": 1, "valueType": "text", "sortNum": 2},
])
print(f"  resp: code={res2.get('code')}  msg={res2.get('message','')[:60]}")
assert res2.get('code') != 200, "✗ 应该拒绝但通过了"
print(f"  ✓ 正确拒绝")

# ── 场景 S3：parentId 悬空 ───────────────────────────────────────────────────
print("\n=== S3: parentId 引用不存在的 id（期望拒绝） ===")
res3 = save(test_tpl, [
    {"id": "x1", "parentId": "ghost", "itemName": "孤儿", "headerRow": 2, "colIndex": 1, "rowSpan": 1, "colSpan": 1, "isLeaf": 1, "valueType": "text", "sortNum": 1},
])
print(f"  resp: code={res3.get('code')}  msg={res3.get('message','')[:80]}")
assert res3.get('code') != 200, "✗ 应该拒绝但通过了"
print(f"  ✓ 正确拒绝")

# ── 场景 S4：环形引用 ────────────────────────────────────────────────────────
print("\n=== S4: 环形引用（A→B→A，期望拒绝） ===")
res4 = save(test_tpl, [
    {"id": "A", "parentId": "B", "itemName": "节点A", "headerRow": 1, "colIndex": 1, "rowSpan": 1, "colSpan": 1, "isLeaf": 0, "valueType": "text", "sortNum": 1},
    {"id": "B", "parentId": "A", "itemName": "节点B", "headerRow": 2, "colIndex": 1, "rowSpan": 1, "colSpan": 1, "isLeaf": 1, "valueType": "text", "sortNum": 2},
])
print(f"  resp: code={res4.get('code')}  msg={res4.get('message','')[:80]}")
assert res4.get('code') != 200, "✗ 应该拒绝但通过了"
print(f"  ✓ 正确拒绝")

# ── 场景 S5：附件2 全量读回再保存，验证树不崩 ─────────────────────────────────
print("\n=== S5: 附件2 全量读回再保存，验证树结构不崩 ===")
orig = items(TPL2)
print(f"  读回节点: {len(orig)}  叶子: {sum(1 for i in orig if i.get('isLeaf')==1)}")

# 构造 payload：原样带回 id 和 parentId
payload_s5 = []
for i in orig:
    payload_s5.append({
        "id":       str(i['id']),
        "parentId": str(i['parentId']) if i.get('parentId') else None,
        "itemName": i['itemName'],
        "headerRow":i.get('headerRow'), "colIndex": i.get('colIndex'),
        "rowSpan":  i.get('rowSpan'),   "colSpan":  i.get('colSpan'),
        "isLeaf":   i.get('isLeaf'),    "valueType":i.get('valueType'),
        "sortNum":  i.get('sortNum'),   "dictCode": i.get('dictCode'),
    })
res5 = save(TPL2, payload_s5)
print(f"  save resp: code={res5.get('code')}")

after = items(TPL2)
null_parents = [i for i in after if i.get('parentId') is None]
has_parents  = [i for i in after if i.get('parentId') is not None]
print(f"  保存后 parent=null: {len(null_parents)}（应=15）  有父: {len(has_parents)}（应=58）")
assert len(null_parents) == 15, f"✗ 树崩了！null_parent={len(null_parents)}"
assert len(has_parents)  == 58, f"✗ 树崩了！has_parent={len(has_parents)}"

# 验证一条三级路径
sample = next((i for i in after if i.get('colIndex') == 4 and i.get('isLeaf') == 1), None)
print(f"  col=4 (质控核心委员会议/线上次数) headerPath={sample.get('headerPath') if sample else 'not found'}")
assert sample and len(sample.get('headerPath', [])) == 3, "✗ 三级路径丢失"
print(f"  ✓ 全量保存后树结构完整")

print("\n🎉 全部5个场景通过")
