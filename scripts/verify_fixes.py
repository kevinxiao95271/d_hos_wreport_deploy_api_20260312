"""验证三个 bug 修复：orgName / users接口 / ISO 8601 日期"""
import requests, json

BASE = "http://localhost:8083"

def check(label, ok, detail=""):
    tag = "OK" if ok else "FAIL"
    print(f"[{tag}] {label}" + (f"  => {detail}" if detail else ""))

# 重置密码
requests.post(f"{BASE}/api/auth/dev-reset-pwd")

# Fix 1: orgName 不再为空
r = requests.post(f"{BASE}/api/auth/login", json={"account": "wr_org_a", "password": "OrgA@2025"})
d = r.json()["data"]
org_name = d.get("orgName", "")
check("Fix1: wr_org_a.orgName 非空", bool(org_name), org_name)
admin_token = requests.post(f"{BASE}/api/auth/login", json={"account": "wr_admin", "password": "Admin@2025"}).json()["data"]["token"]
admin_h = {"Authorization": admin_token}

# Fix 1b: 新建机构账号 qc_001 登录后 orgName 也正确
r2 = requests.post(f"{BASE}/api/auth/login", json={"account": "qc_001", "password": "Test@2025"})
d2 = r2.json().get("data", {})
check("Fix1: qc_001.orgName 非空", bool(d2.get("orgName")), d2.get("orgName","<空>"))

# Fix 2: /api/auth/users 有 real_name/org_name，无 password
r3 = requests.get(f"{BASE}/api/auth/users")
users = r3.json().get("data", [])
if users:
    sample = users[0]
    check("Fix2: users有real_name字段", "real_name" in sample)
    check("Fix2: users有org_name字段",  "org_name"  in sample)
    check("Fix2: users无password字段",  "password"  not in sample)
else:
    print("[FAIL] Fix2: users 接口返回空")

# Fix 3: resubmitDeadline 支持 ISO 8601
# 先创建一条记录并提交，再驳回时传 ISO 格式日期
tpl_id = requests.post(f"{BASE}/wr/template/add", headers=admin_h, json={
    "templateName": "ISO日期测试模板",
    "items": [{"itemName": "字段A", "headerRow": 1, "colIndex": 1, "isLeaf": 1, "valueType": "text"}]
}).json()["data"]

import datetime
deadline = (datetime.datetime.now() + datetime.timedelta(days=7)).strftime("%Y-%m-%d %H:%M:%S")
task_id = requests.post(f"{BASE}/wr/task/add", headers=admin_h, json={
    "taskName": "ISO测试任务", "templateId": tpl_id, "deadline": deadline
}).json()["data"]

org_h = {"Authorization": d["token"]}
items = requests.get(f"{BASE}/wr/template/items/{tpl_id}", headers=admin_h).json()["data"]
item_id = items[0]["id"]

record_id = requests.post(f"{BASE}/wr/record/save", headers=org_h, json={
    "taskId": task_id,
    "rows": [{"rowIndex": 1, "cells": [{"itemId": item_id, "value": "测试值"}]}]
}).json()["data"]
requests.post(f"{BASE}/wr/record/submit", headers=org_h, json={"recordId": record_id})

# 驳回时用 ISO 8601 格式
r4 = requests.post(f"{BASE}/wr/record/audit", headers=admin_h, json={
    "recordId":        record_id,
    "auditResult":     2,
    "auditRemark":     "ISO日期测试",
    "resubmitDeadline": "2026-05-31T00:00:00"
})
check("Fix3: resubmitDeadline 接受 ISO 8601", r4.json().get("code") == 200, r4.json().get("message",""))
