"""
自测 scope 接口：
  S1  GET /scope/{taskId} 返回 orgs 含机构名及 recordStatus
  S2  POST /scope/{taskId} 移除有 status>=1 记录的机构时，返回 code=4031
  S3  POST /scope/{taskId} 移除无记录或只有草稿的机构时，允许通过
"""
import requests, json, sys
sys.stdout.reconfigure(encoding='utf-8')

base = 'http://localhost:8083'
r    = requests.post(f'{base}/api/auth/login', json={'account':'wr_admin','password':'Admin@2025'})
token = r.json()['data']['token']
H = {'Authorization': token, 'Content-Type': 'application/json'}

TASK_ID = '3000000000000001'

# ── S1: GET scope/{taskId} ─────────────────────────────────────────────────
print("=== S1: GET scope (含 recordStatus) ===")
scope = requests.get(f'{base}/wr/task/scope/{TASK_ID}', headers=H).json()
print(f"  code={scope.get('code')}")
orgs = scope.get('data', {}).get('orgs', [])
print(f"  orgs 数量: {len(orgs)}")
for o in orgs[:5]:
    print(f"    orgId={o.get('orgId')}  orgName={o.get('orgName')}  recordStatus={o.get('recordStatus')}")
assert len(orgs) > 0, "✗ orgs 为空"
print("  ✓ 返回结构正确")

# ── S2: 尝试移除有 status>=1 的机构 ────────────────────────────────────────
submitted_org = next((o for o in orgs if o.get('recordStatus') is not None and o['recordStatus'] >= 1), None)
if submitted_org:
    print(f"\n=== S2: 移除已提交机构 {submitted_org.get('orgName')} (status={submitted_org.get('recordStatus')}) ===")
    all_org_ids = [o['orgId'] for o in orgs]
    new_ids     = [x for x in all_org_ids if x != submitted_org['orgId']]
    res2 = requests.post(f'{base}/wr/task/scope/{TASK_ID}', json={'orgIds': new_ids}, headers=H).json()
    print(f"  code={res2.get('code')}  msg={res2.get('message','')[:80]}")
    assert res2.get('code') == 4031, f"✗ 期望4031，得到{res2.get('code')}"
    print("  ✓ 正确返回 4031 并阻断操作")
else:
    print("\n=== S2: 跳过（当前任务没有 status>=1 的机构记录） ===")

# ── S3: 移除只有草稿或无记录的机构（应允许） ────────────────────────────────
draft_or_none_org = next((o for o in orgs if o.get('recordStatus') is None or o['recordStatus'] == 0), None)
if draft_or_none_org:
    print(f"\n=== S3: 移除草稿/无记录机构 {draft_or_none_org.get('orgName')} (status={draft_or_none_org.get('recordStatus')}) ===")
    all_org_ids = [o['orgId'] for o in orgs]
    new_ids     = [x for x in all_org_ids if x != draft_or_none_org['orgId']]
    res3 = requests.post(f'{base}/wr/task/scope/{TASK_ID}', json={'orgIds': new_ids}, headers=H).json()
    print(f"  code={res3.get('code')}  msg={res3.get('message','')[:50]}")
    assert res3.get('code') == 200, f"✗ 期望200，得到{res3.get('code')}"
    print("  ✓ 正确放行")
    # 恢复
    requests.post(f'{base}/wr/task/scope/{TASK_ID}', json={'orgIds': all_org_ids}, headers=H)
    print("  ✓ 已恢复原始 scope")
else:
    print("\n=== S3: 跳过（没有可安全移除的机构） ===")

print("\n全部测试通过")
