"""
双修脚本：
  1. 修复 wr_task_org_scope：从 wr_record + sys_user 重建两个任务的正确机构列表
  2. 验证结果（通过 API）
"""
import psycopg2, sys
sys.stdout.reconfigure(encoding='utf-8')

DB = dict(host='119.167.165.27', port=5432, dbname='zjylzl',
          user='postgres', password='zjylzl', options='-c search_path=zjylzl')

conn = psycopg2.connect(**DB)
cur  = conn.cursor()

TASKS = [3000000000000001, 3000000000000002]

# ── 所有真实机构：sys_user 里 role=org 的机构
cur.execute("""
    SELECT DISTINCT u.org_id, ho.org_name
    FROM sys_user u
    LEFT JOIN hr_organization ho ON ho.org_id = u.org_id
    JOIN sys_user_role ur ON ur.user_id = u.user_id
    JOIN sys_role r ON r.role_id = ur.role_id AND r.role_code = 'org'
    WHERE u.del_flag = 'N'
    ORDER BY u.org_id
""")
org_users = cur.fetchall()
print(f"sys_user 中 role=org 的机构 ({len(org_users)} 个):")
for o in org_users:
    print(f"  org_id={o[0]}  hr_org_name={o[1]}")

# ── 从 wr_record 拿各任务的机构列表（含 org_name）
for task_id in TASKS:
    cur.execute("""
        SELECT DISTINCT org_id, org_name
        FROM wr_record
        WHERE task_id=%s AND del_flag=0
        ORDER BY org_id
    """, (task_id,))
    rec_orgs = cur.fetchall()
    print(f"\n── task {task_id} wr_record 机构 ({len(rec_orgs)} 个) ──")
    for o in rec_orgs:
        print(f"  org_id={o[0]}  org_name={o[1]}")

# ── 修复：用 sys_user 里所有 org 角色机构作为 scope（全量）
# 如果 wr_record 里有更多机构也加进来
print("\n\n=== 开始修复 scope ===")

# 合并：sys_user org角色 + wr_record 里出现的机构（去重）
all_org_ids = set(o[0] for o in org_users)
for task_id in TASKS:
    cur.execute("SELECT DISTINCT org_id FROM wr_record WHERE task_id=%s AND del_flag=0", (task_id,))
    rec_org_ids = set(r[0] for r in cur.fetchall())
    all_org_ids |= rec_org_ids

print(f"合并后机构总数: {len(all_org_ids)}")
print(f"org_ids: {sorted(all_org_ids)}")

# 修复两个任务的 scope
for task_id in TASKS:
    # 删旧 scope
    cur.execute("DELETE FROM wr_task_org_scope WHERE task_id = %s", (task_id,))
    deleted = cur.rowcount
    # 插新 scope（admin user_id 用 177330561143451283，或任意存在的用户）
    cur.execute("SELECT user_id FROM sys_user WHERE del_flag='N' LIMIT 1")
    admin_id = cur.fetchone()[0]
    inserted = 0
    for org_id in sorted(all_org_ids):
        # 生成简单自增 id（用 sequence 或 max+1）
        cur.execute("SELECT COALESCE(MAX(id),0)+1 FROM wr_task_org_scope")
        new_id = cur.fetchone()[0]
        cur.execute("""
            INSERT INTO wr_task_org_scope(id, task_id, org_id, create_user, create_time)
            VALUES (%s, %s, %s, %s, NOW())
        """, (new_id + inserted, task_id, org_id, admin_id))
        inserted += 1
    print(f"  task {task_id}: 删除 {deleted} 条，插入 {inserted} 条")

conn.commit()
print("\n✅ scope 修复完成")

# ── 验证
for task_id in TASKS:
    cur.execute("SELECT COUNT(*) FROM wr_task_org_scope WHERE task_id=%s", (task_id,))
    cnt = cur.fetchone()[0]
    cur.execute("""
        SELECT s.org_id, r.org_name, r.status
        FROM wr_task_org_scope s
        LEFT JOIN wr_record r ON r.task_id=s.task_id AND r.org_id=s.org_id AND r.del_flag=0
        WHERE s.task_id=%s LIMIT 5
    """, (task_id,))
    rows = cur.fetchall()
    print(f"\ntask {task_id} scope({cnt})，JOIN 结果(前5):")
    for row in rows:
        print(f"  org_id={row[0]}  org_name={row[1]}  status={row[2]}")

conn.close()
