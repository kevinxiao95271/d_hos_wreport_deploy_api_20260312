"""验证 /wr/record/detail 返回中已包含 rows 字段"""
import requests, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

BASE = "http://localhost:8083"
requests.post(f"{BASE}/api/auth/dev-reset-pwd")

# 用 wr_org_a 登录，先保存一条附件3的草稿记录
token_a = requests.post(f"{BASE}/api/auth/login",
    json={"account": "wr_org_a", "password": "OrgA@2025"}).json()["data"]["token"]
ah = {"Authorization": token_a}

# 保存草稿（附件3任务，勾选几个格）
save_resp = requests.post(f"{BASE}/wr/record/save", headers=ah, json={
    "taskId": "3000000000000002",
    "rows": [
        {"rowIndex": 1, "cells": [{"itemId": None, "value": "1"}]},
    ]
}).json()

# 先获取附件3的列定义，拿到第一个itemId
items = requests.get(f"{BASE}/wr/template/items/2000000000000002", headers=ah).json().get("data", [])
if not items:
    print("❌ 无法获取附件3列定义")
    exit(1)
item_id = items[0]["id"]

# 重新保存带真实itemId的记录
requests.post(f"{BASE}/wr/record/save", headers=ah, json={
    "taskId": "3000000000000002",
    "rows": [
        {"rowIndex": 1, "cells": [{"itemId": item_id, "value": "1"}]},
        {"rowIndex": 4, "cells": [{"itemId": item_id, "value": "1"}]},
    ]
})

# 查找我的记录
my_records = requests.get(f"{BASE}/wr/record/my/page",
    headers=ah, params={"pageNum": 1, "pageSize": 10}).json().get("data", {}).get("records", [])

record_id = None
for r in my_records:
    if str(r.get("taskId")) == "3000000000000002":
        record_id = r.get("id")
        break

if not record_id:
    print("❌ 未找到附件3的记录，请检查任务配置")
    exit(1)

# 获取详情
detail = requests.get(f"{BASE}/wr/record/detail/{record_id}", headers=ah).json()
data = detail.get("data", {})

fields = list(data.keys())
rows   = data.get("rows", None)
values = data.get("values", [])

print(f"record detail 返回字段: {fields}")
print()

if rows is None:
    print("❌ rows 字段不存在")
elif not isinstance(rows, list):
    print(f"❌ rows 类型错误: {type(rows)}")
else:
    print(f"✅ rows 字段存在，共 {len(rows)} 行")
    if rows:
        first = rows[0]
        print(f"   前3行: {[r['rowLabel'] for r in rows[:3]]}")

print(f"✅ values 字段: {len(values)} 条")
print(f"✅ statusLabel: {data.get('statusLabel')}")
