"""
探察机构名称字段来源：
  - sys_user_org 表结构与样本
  - hr_organization 表结构与样本
  - qcUser 角色用户 + org_id 一览
  - org_id 在 hr_organization / sys_user_org 中的关联结果
"""
import psycopg2, json

DSN = "host=119.167.165.27 port=5432 dbname=zjylzl user=postgres password=zjylzl options='-c search_path=zjylzl'"

def run(conn, sql, label):
    cur = conn.cursor()
    cur.execute(sql)
    rows = cur.fetchall()
    cols = [d[0] for d in cur.description]
    print(f"\n=== {label} ({len(rows)} rows) ===")
    for r in rows:
        print(dict(zip(cols, r)))
    cur.close()
    return cols, rows

conn = psycopg2.connect(DSN)

# 1. sys_user_org 列结构
run(conn, """
    SELECT column_name, data_type
    FROM information_schema.columns
    WHERE table_schema = current_schema() AND table_name = 'sys_user_org'
    ORDER BY ordinal_position
""", "sys_user_org columns")

# 2. sys_user_org 样本数据
run(conn, "SELECT * FROM sys_user_org LIMIT 5", "sys_user_org sample")

# 3. hr_organization 列结构
run(conn, """
    SELECT column_name, data_type
    FROM information_schema.columns
    WHERE table_schema = current_schema() AND table_name = 'hr_organization'
    ORDER BY ordinal_position
""", "hr_organization columns")

# 4. hr_organization 样本数据
run(conn, "SELECT * FROM hr_organization LIMIT 5", "hr_organization sample")

# 5. qcUser 用户 + org_id
run(conn, """
    SELECT u.user_id, u.account, u.real_name, u.org_id, r.role_code
    FROM sys_user u
    JOIN sys_user_role ur ON ur.user_id = u.user_id
    JOIN sys_role r ON r.role_id = ur.role_id
    WHERE r.role_code = 'qcUser' AND u.del_flag = 'N'
    LIMIT 20
""", "qcUser 用户")

# 6. 尝试用 sys_user_org 关联 hr_organization 查机构名
run(conn, """
    SELECT u.account, u.real_name, u.org_id,
           suo.org_id AS suo_org_id,
           ho.id AS hr_org_id, ho.name AS hr_org_name
    FROM sys_user u
    JOIN sys_user_role ur ON ur.user_id = u.user_id
    JOIN sys_role r ON r.role_id = ur.role_id
    LEFT JOIN sys_user_org suo ON suo.user_id = u.user_id
    LEFT JOIN hr_organization ho ON ho.id::text = suo.org_id::text
    WHERE r.role_code = 'qcUser' AND u.del_flag = 'N'
    LIMIT 20
""", "qcUser + hr_organization JOIN 尝试")

conn.close()
