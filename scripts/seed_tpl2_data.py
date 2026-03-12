"""
seed_tpl2_data.py
为附件2任务批量写入测试记录（多机构、随机数据）
"""
import sys, io, random, requests
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

BASE      = "http://localhost:8083"
TASK2_ID  = "3000000000000001"

# 测试账号列表：(account, password)
ACCOUNTS = [
    ("wr_org_a",  "OrgA@2025"),
    ("wr_org_b",  "OrgB@2025"),
    ("qc_001",    "Test@2025"),
    ("qc_002",    "Test@2025"),
    ("qc_003",    "Test@2025"),
    ("qc_004",    "Test@2025"),
    ("qc_005",    "Test@2025"),
]

def login(account, password):
    r = requests.post(f"{BASE}/api/auth/login",
                      json={"account": account, "password": password})
    data = r.json().get("data", {})
    return data.get("token"), data.get("orgName", account)

def get_leaf_items(token):
    """获取附件2的所有叶子列 itemId 及 valueType"""
    items = requests.get(f"{BASE}/wr/template/items/2000000000000001",
                         headers={"Authorization": token}).json().get("data", [])
    return [(str(it["id"]), it.get("valueType","text")) for it in items if it.get("isLeaf") == 1]

def rand_value(vtype):
    if vtype == "number":
        return str(random.randint(1, 29))
    # text 字段也填随机数字，方便展示
    return str(random.randint(1, 29))

def seed_one(account, password, leaf_items):
    token, org_name = login(account, password)
    if not token:
        print(f"  [{account}] 登录失败，跳过")
        return

    cells = [{"itemId": iid, "value": rand_value(vtype)} for iid, vtype in leaf_items]
    payload = {
        "taskId": TASK2_ID,
        "rows": [{"rowIndex": 1, "cells": cells}]
    }
    r = requests.post(f"{BASE}/wr/record/save",
                      headers={"Authorization": token}, json=payload)
    resp = r.json()
    code = resp.get("code")
    rid  = resp.get("data")
    if code == 200:
        print(f"  [{account}] {org_name}  recordId={rid}  ✅ 草稿保存成功")
    else:
        print(f"  [{account}] ❌ 失败: {resp.get('message')}")

def main():
    requests.post(f"{BASE}/api/auth/dev-reset-pwd")

    # 用 admin token 拿列定义（不限角色）
    admin_token, _ = login("wr_admin", "Admin@2025")
    leaf_items = get_leaf_items(admin_token)
    print(f"附件2 叶子列数: {len(leaf_items)}\n")

    print("=== 批量写入草稿记录 ===\n")
    for acc, pwd in ACCOUNTS:
        seed_one(acc, pwd, leaf_items)

    print("\n=== 验证：admin 查看附件2任务的上报列表 ===\n")
    records = requests.get(f"{BASE}/wr/record/admin/page",
        headers={"Authorization": admin_token},
        params={"taskId": TASK2_ID, "pageNum": 1, "pageSize": 20}).json()
    rows = records.get("data", {}).get("records", [])
    print(f"共 {len(rows)} 条记录：")
    for r in rows:
        print(f"  recordId={r.get('id')}  orgName={r.get('orgName')}  status={r.get('status')}")

if __name__ == "__main__":
    main()
