"""
seed_attachments.py
为附件2已存在的草稿记录上传测试附件文件
- 查找 require_attachment=1 的列（如有）
- 若无强制列，则为每条记录上传1~2个说明性附件（调研报告/证明文件）
"""
import sys, io, requests, random, os, tempfile
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

BASE     = "http://localhost:8083"
TASK2_ID = "3000000000000001"

ACCOUNTS = [
    ("wr_org_a", "OrgA@2025"),
    ("wr_org_b", "OrgB@2025"),
    ("qc_001",   "Test@2025"),
    ("qc_002",   "Test@2025"),
    ("qc_003",   "Test@2025"),
]

# 生成简单的文本文件内容作为"附件"
def make_dummy_file(org_name, doc_type, idx):
    content = f"""【{doc_type}】
机构名称：{org_name}
填报年度：2025年
文件序号：第{idx}份

本文件为系统测试用途，内容仅供演示。
填报人员已确认上述数据的真实性和准确性。

---
生成时间：2026-03-13
"""
    return content.encode("utf-8")

def login(account, password):
    r = requests.post(f"{BASE}/api/auth/login",
                      json={"account": account, "password": password})
    d = r.json().get("data", {})
    return d.get("token"), d.get("orgName", account)

def get_my_record(token, task_id):
    r = requests.get(f"{BASE}/wr/record/my/page", headers={"Authorization": token},
                     params={"pageNum": 1, "pageSize": 10, "taskId": task_id})
    recs = r.json().get("data", {}).get("records", [])
    return recs[0] if recs else None

def upload_file(token, record_id, item_id, filename, content_bytes):
    files = {"file": (filename, content_bytes, "text/plain")}
    params = {"recordId": record_id}
    if item_id:
        params["itemId"] = item_id
    r = requests.post(f"{BASE}/wr/attachment/upload",
                      headers={"Authorization": token},
                      params=params, files=files)
    return r.json()

def main():
    requests.post(f"{BASE}/api/auth/dev-reset-pwd")

    # 查询附件2 require_attachment=1 的列
    admin_token, _ = login("wr_admin", "Admin@2025")
    items = requests.get(f"{BASE}/wr/template/items/2000000000000001",
                         headers={"Authorization": admin_token}).json().get("data", [])
    req_items = [(str(it["id"]), it["itemName"]) for it in items
                 if it.get("requireAttachment") == 1 and it.get("isLeaf") == 1]

    if req_items:
        print(f"发现 {len(req_items)} 个需要附件的栏位：")
        for iid, name in req_items:
            print(f"  itemId={iid}  name={name}")
    else:
        print("附件2模板中无强制附件栏位，将为每条记录上传通用佐证材料（调研报告 + 证明文件）")

    print()

    doc_types = [
        ("年度工作总结报告", "年度工作总结_{org}_{n}.txt"),
        ("专项调研报告",    "调研报告_{org}_{n}.txt"),
    ]

    for account, password in ACCOUNTS:
        token, org_name = login(account, password)
        if not token:
            print(f"  [{account}] 登录失败")
            continue

        record = get_my_record(token, TASK2_ID)
        if not record:
            print(f"  [{account}] 未找到附件2草稿记录，跳过")
            continue

        rec_id = str(record["id"])
        uploaded = []

        if req_items:
            # 上传到指定需附件的列
            for idx, (item_id, item_name) in enumerate(req_items[:3], 1):
                fname    = f"{item_name}_{org_name}_{idx}.txt"
                content  = make_dummy_file(org_name, item_name, idx)
                resp     = upload_file(token, rec_id, item_id, fname, content)
                if resp.get("code") == 200:
                    uploaded.append(fname)
        else:
            # 上传通用附件（不绑定 itemId）
            for idx, (doc_type, fname_tpl) in enumerate(doc_types, 1):
                short_org = org_name[:6]
                fname     = fname_tpl.format(org=short_org, n=idx)
                content   = make_dummy_file(org_name, doc_type, idx)
                resp      = upload_file(token, rec_id, None, fname, content)
                if resp.get("code") == 200:
                    uploaded.append(fname)
                else:
                    print(f"    ⚠️  上传失败: {resp.get('message')}")

        print(f"  [{account}] {org_name}  recordId={rec_id}  上传 {len(uploaded)} 个附件: {uploaded}")

    print()
    print("=== 验证：admin 查看第1条记录附件列表 ===")
    # 用 admin 拿第一条记录的附件
    page = requests.get(f"{BASE}/wr/record/admin/page",
        headers={"Authorization": admin_token},
        params={"taskId": TASK2_ID, "pageNum": 1, "pageSize": 5}).json()
    recs = page.get("data", {}).get("records", [])
    if recs:
        rid = str(recs[0]["id"])
        attachments = requests.get(f"{BASE}/wr/attachment/list/{rid}",
            headers={"Authorization": admin_token}).json().get("data", [])
        print(f"  recordId={rid}  附件数={len(attachments)}")
        for a in attachments:
            print(f"    {a.get('attachName')}  size={a.get('attachSize')}B")

if __name__ == "__main__":
    main()
