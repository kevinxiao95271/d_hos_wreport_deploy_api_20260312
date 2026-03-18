"""
将 sys_user 中 qc_001~qc_012 迁移到 hr_person（organization_type='2'）
- person_id  : 基于时间戳生成唯一 bigint（模拟 Snowflake 风格）
- person_name: sys_user.real_name
- org_id     : sys_user.org_id
- organization_type: '2'
- del_flag   : 'N'
- 其余字段    : NULL
已存在（org_id 已在 hr_person type=2 中）则跳过，保证幂等。
"""
import psycopg2
import time

def gen_id(seq):
    """简单时间戳 + 序号，生成唯一 bigint"""
    return int(time.time() * 1000) * 10000 + seq

conn = psycopg2.connect(host='119.167.165.27', port=5432, dbname='zjylzl',
                        user='postgres', password='zjylzl', options='-c search_path=zjylzl')
cur = conn.cursor()

# 1. 读取 qc_ 账号
cur.execute("""
    SELECT account, real_name, org_id
    FROM sys_user
    WHERE account LIKE 'qc_%' AND del_flag = 'N'
    ORDER BY account
""")
qc_users = cur.fetchall()
print(f"待迁移账号数：{len(qc_users)}")

# 2. 读取 hr_person 中 type=2 已有 org_id（去重用）
cur.execute("SELECT org_id FROM hr_person WHERE organization_type='2' AND del_flag='N'")
existing_org_ids = {r[0] for r in cur.fetchall()}
print(f"hr_person type=2 已有 org_id：{existing_org_ids}\n")

# 3. 逐条插入
inserted = 0
skipped  = 0
for i, (account, real_name, org_id) in enumerate(qc_users):
    if org_id in existing_org_ids:
        print(f"  [跳过] {account} / {real_name}（org_id={org_id} 已存在）")
        skipped += 1
        continue

    person_id = gen_id(i)
    cur.execute("""
        INSERT INTO hr_person
            (person_id, person_name, org_id, organization_type,
             del_flag, create_time, create_user,
             parent_org_id, gender, id_card, title, mobile, label, position_name, remark)
        VALUES
            (%s, %s, %s, '2',
             'N', NOW(), 1339550467939639299,
             NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
    """, (person_id, real_name, org_id))
    print(f"  [插入] {account} / {real_name}  org_id={org_id}  person_id={person_id}")
    inserted += 1

conn.commit()
print(f"\n完成：插入 {inserted} 条，跳过 {skipped} 条")

# 4. 验证
print("\n=== 验证结果 ===")
cur.execute("""
    SELECT p.person_name, p.org_id, o.org_name
    FROM hr_person p
    LEFT JOIN hr_organization o ON o.org_id = p.org_id
    WHERE p.organization_type = '2' AND p.del_flag = 'N'
    ORDER BY p.create_time
""")
for r in cur.fetchall():
    match = "✅" if r[0] == r[2] else "⚠️"
    print(f"  {match}  person_name={r[0]}  org_name={r[2]}")

conn.close()
