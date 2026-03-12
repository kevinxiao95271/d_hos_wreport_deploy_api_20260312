"""
自测：登录 -> 创建模板 -> 发布任务 -> 机构填报提交 -> 管理员驳回 -> 重提 -> 审核通过
"""
import requests, json, sys, datetime

BASE = "http://localhost:8083"

def ok(label, r):
    try:
        d = r.json()
    except Exception:
        print(f"[FAIL] {label}  status={r.status_code}  body={r.text[:200]}")
        sys.exit(1)
    status = "OK" if d.get("code") == 200 else "FAIL"
    print(f"[{status}] {label}  code={d.get('code')}  msg={d.get('message','')}")
    if status == "FAIL":
        print("       detail:", json.dumps(d, ensure_ascii=False)[:300])
        sys.exit(1)
    return d.get("data")

# ── 0. 重置密码 ─────────────────────────────────────────────
ok("dev-reset-pwd", requests.post(f"{BASE}/api/auth/dev-reset-pwd"))

# ── 1. 三账号登录 ────────────────────────────────────────────
def login(account, pwd):
    data = ok(f"login({account})", requests.post(
        f"{BASE}/api/auth/login", json={"account": account, "password": pwd}))
    print(f"       orgId={data['orgId']}  role={data['roleCode']}")
    return {"Authorization": data["token"]}

admin_h = login("wr_admin", "Admin@2025")
org_a_h = login("wr_org_a", "OrgA@2025")
org_b_h = login("wr_org_b", "OrgB@2025")

# ── 2. 创建模板（一层表头） ───────────────────────────────────
tpl_id = ok("创建模板", requests.post(f"{BASE}/wr/template/add", headers=admin_h, json={
    "templateName": "自测模板_单层",
    "description":  "自动化测试用",
    "items": [
        {"itemName": "科室",   "headerRow": 1, "colIndex": 1, "isLeaf": 1, "valueType": "text"},
        {"itemName": "人数",   "headerRow": 1, "colIndex": 2, "isLeaf": 1, "valueType": "number"},
        {"itemName": "完成率", "headerRow": 1, "colIndex": 3, "isLeaf": 1, "valueType": "text"},
    ]
}))
print(f"       templateId={tpl_id}")

# ── 3. 获取模板表头，拿 itemId ────────────────────────────────
items = ok("获取模板表头", requests.get(f"{BASE}/wr/template/items/{tpl_id}", headers=admin_h))
item_map = {it["itemName"]: it["id"] for it in items}
print(f"       items={item_map}")

# ── 4. 发布任务 ──────────────────────────────────────────────
deadline = (datetime.datetime.now() + datetime.timedelta(days=7)).strftime("%Y-%m-%d %H:%M:%S")
task_id = ok("发布任务", requests.post(f"{BASE}/wr/task/add", headers=admin_h, json={
    "taskName":   "2026年Q1自测任务",
    "templateId": tpl_id,
    "deadline":   deadline,
}))
print(f"       taskId={task_id}")

# ── 5. 机构A 保存草稿 ─────────────────────────────────────────
record_id = ok("org_a 保存草稿", requests.post(f"{BASE}/wr/record/save", headers=org_a_h, json={
    "taskId": task_id,
    "rows": [
        {"rowIndex": 1, "cells": [
            {"itemId": item_map["科室"],   "value": "内科"},
            {"itemId": item_map["人数"],   "value": "20"},
            {"itemId": item_map["完成率"], "value": "95%"},
        ]},
        {"rowIndex": 2, "cells": [
            {"itemId": item_map["科室"],   "value": "外科"},
            {"itemId": item_map["人数"],   "value": "15"},
            {"itemId": item_map["完成率"], "value": "88%"},
        ]},
    ]
}))
print(f"       recordId={record_id}")

# ── 6. 机构A 提交 ─────────────────────────────────────────────
ok("org_a 提交", requests.post(f"{BASE}/wr/record/submit", headers=org_a_h,
   json={"recordId": record_id}))

# ── 7. 管理员查列表 ───────────────────────────────────────────
page = ok("admin 查列表", requests.get(f"{BASE}/wr/record/admin/page", headers=admin_h,
          params={"taskId": task_id, "pageNum": 1, "pageSize": 10}))
recs = page.get("records", [])
print(f"       记录数={len(recs)}  status={[r['status'] for r in recs]}")

# ── 8. 管理员驳回（默认 +7天 resubmitDeadline） ──────────────
ok("admin 驳回", requests.post(f"{BASE}/wr/record/audit", headers=admin_h, json={
    "recordId":    record_id,
    "auditResult": 2,
    "auditRemark": "数据有误，请修改后重提",
}))

# ── 9. 机构A 修改后重提 ───────────────────────────────────────
ok("org_a 修改草稿", requests.post(f"{BASE}/wr/record/save", headers=org_a_h, json={
    "taskId": task_id,
    "rows": [{"rowIndex": 1, "cells": [
        {"itemId": item_map["科室"],   "value": "内科(修)"},
        {"itemId": item_map["人数"],   "value": "22"},
        {"itemId": item_map["完成率"], "value": "97%"},
    ]}]
}))
ok("org_a 重提", requests.post(f"{BASE}/wr/record/submit", headers=org_a_h,
   json={"recordId": record_id}))

# ── 10. 管理员审核通过 ────────────────────────────────────────
ok("admin 审核通过", requests.post(f"{BASE}/wr/record/audit", headers=admin_h, json={
    "recordId":    record_id,
    "auditResult": 1,
    "auditRemark": "数据核实无误，通过",
}))

print("\n====== 全部通过 ======")
