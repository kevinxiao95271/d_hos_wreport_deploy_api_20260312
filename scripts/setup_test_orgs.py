"""
1. 检查这批机构名是否存在于 hr_organization
2. 不存在的创建机构记录
3. 检查每个机构是否有对应 qcUser 账号（sys_user + sys_user_role）
4. 不存在的创建账号（密码统一 Test@2025，BCrypt hash）
5. 最终打印可用账号表
"""
import psycopg2, bcrypt, time

DSN = "host=119.167.165.27 port=5432 dbname=zjylzl user=postgres password=zjylzl options='-c search_path=zjylzl'"

ORG_NAMES = [
    "省神经外科技术指导中心",
    "省骨科技术指导中心",
    "省口腔正畸中心",
    "分娩镇痛技术指导中心",
    "全科医学技术指导中心",
    "甲状腺病诊治技术指导中心",
    "产科医疗质控中心",
    "人类辅助生殖技术质控中心",
    "产前筛查质控中心",
    "儿童生长发育质控中心",
    "新生儿疾病筛查质控中心",
    "超声质控中心",
]

PWD_PLAIN  = "Test@2025"
PWD_HASH   = bcrypt.hashpw(PWD_PLAIN.encode(), bcrypt.gensalt(rounds=10)).decode()

# 雪花ID简版：毫秒时间戳左移22位 + 序号
_seq = 0
def next_id():
    global _seq
    _seq += 1
    return (int(time.time() * 1000) << 22) | (_seq & 0x3FF)

conn = psycopg2.connect(DSN)
conn.autocommit = False
cur = conn.cursor()

# 查 qcUser 的 role_id
cur.execute("SELECT role_id FROM sys_role WHERE role_code = 'qcUser' LIMIT 1")
row = cur.fetchone()
if not row:
    print("ERROR: qcUser 角色不存在")
    exit(1)
qc_role_id = row[0]
print(f"qcUser role_id = {qc_role_id}")

# 查根机构（作为 parent_id）
cur.execute("SELECT org_id FROM hr_organization WHERE org_parent_id IS NULL OR org_parent_id = -1 LIMIT 1")
row = cur.fetchone()
root_org_id = row[0] if row else None
print(f"root org_id = {root_org_id}")

print("\n{'机构名':<20} {'org_id':<22} {'account':<25} {'状态'}")
print("-" * 80)

for org_name in ORG_NAMES:
    # 1. 查机构
    cur.execute("SELECT org_id FROM hr_organization WHERE org_name = %s AND del_flag = 'N' LIMIT 1", (org_name,))
    row = cur.fetchone()
    if row:
        org_id = row[0]
        created_org = False
    else:
        org_id = next_id()
        org_pids = f"[-1],[{root_org_id}],"
        org_code = f"WK{org_id % 100000:05d}"
        cur.execute("""
            INSERT INTO hr_organization(org_id, org_parent_id, org_pids, org_name, org_code, del_flag, status_flag, org_type)
            VALUES (%s, %s, %s, %s, %s, 'N', 1, 1)
        """, (org_id, root_org_id, org_pids, org_name, org_code))
        created_org = True

    # 2. 查该机构是否已有 qcUser 账号
    cur.execute("""
        SELECT u.user_id, u.account FROM sys_user u
        JOIN sys_user_role ur ON ur.user_id = u.user_id
        WHERE u.org_id = %s AND ur.role_id = %s AND u.del_flag = 'N' LIMIT 1
    """, (org_id, qc_role_id))
    row = cur.fetchone()

    if row:
        user_id, account = row
        created_user = False
    else:
        # 生成账号：取机构名首个汉字拼音缩写 → 简单用 org + 序号
        cur.execute("SELECT COUNT(*) FROM sys_user WHERE account LIKE 'qc_%'")
        cnt = cur.fetchone()[0]
        account = f"qc_{cnt+1:03d}"
        user_id = next_id()

        cur.execute("""
            INSERT INTO sys_user(user_id, account, password, real_name, org_id,
                                 sex, super_admin_flag, status_flag, del_flag)
            VALUES (%s, %s, %s, %s, %s, 'M', 'N', 1, 'N')
        """, (user_id, account, PWD_HASH, org_name, org_id))

        # 绑定角色
        user_role_id = next_id()
        cur.execute("""
            INSERT INTO sys_user_role(user_role_id, user_id, role_id) VALUES (%s, %s, %s)
        """, (user_role_id, user_id, qc_role_id))
        created_user = True

    tag = ("新建机构+账号" if (created_org and created_user)
           else "新建账号" if created_user
           else "已存在")
    print(f"{org_name:<20} {str(org_id):<22} {account:<25} {tag}")

conn.commit()
print(f"\n统一密码: {PWD_PLAIN}")
print("完成。")
cur.close()
conn.close()
