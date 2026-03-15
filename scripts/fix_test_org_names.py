"""
让测试账号的 orgName 匹配附件3的列名，方便本地验证过滤逻辑。
wr_org_a -> 超声质控中心（附件3无对应列，改为防盲技术指导中心）
wr_org_b -> 日间手术技术指导中心（附件3 col=20）
"""
import sys, io, psycopg2
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

conn = psycopg2.connect(host="119.167.165.27", port=5432, dbname="zjylzl",
                        user="postgres", password="zjylzl", options="-c search_path=zjylzl")
c = conn.cursor()

# 查现有组织 ID
c.execute("SELECT org_id, org_name FROM hr_organization WHERE org_name LIKE '%日间手术%' LIMIT 3")
rows = c.fetchall()
print("现有含'日间手术'的组织:", rows)

c.execute("SELECT org_id, org_name FROM hr_organization WHERE org_name LIKE '%防盲%' LIMIT 3")
rows = c.fetchall()
print("现有含'防盲'的组织:", rows)

# 直接更新 hr_organization 中 wr_org_b 当前所属组织的名字为 "日间手术技术指导中心"
c.execute("SELECT u.org_id FROM sys_user u WHERE u.account='wr_org_b'")
org_id_b = c.fetchone()[0]
c.execute("UPDATE hr_organization SET org_name='日间手术技术指导中心' WHERE org_id=%s", (org_id_b,))
print(f"wr_org_b org({org_id_b}) 更新为: 日间手术技术指导中心，影响行数={c.rowcount}")

# 同时更新 wr_record 里该 org 的 org_name 字段
c.execute("UPDATE wr_record SET org_name='日间手术技术指导中心' WHERE org_id=%s", (org_id_b,))
print(f"  wr_record 更新 {c.rowcount} 条")

conn.commit()
conn.close()
print("完成！重启后端后生效（JWT 中的 orgName 在下次登录时刷新）")
