"""
通过 org_id 为媒介，将 hr_person.person_id 回填到 sys_user.person_id
关联逻辑：sys_user.org_id = hr_person.org_id（organization_type='2', del_flag='N'）
"""
import psycopg2

conn = psycopg2.connect(host='119.167.165.27', port=5432, dbname='zjylzl',
                        user='postgres', password='zjylzl', options='-c search_path=zjylzl')
cur = conn.cursor()

cur.execute("""
    UPDATE sys_user u
    SET person_id = p.person_id
    FROM hr_person p
    WHERE p.org_id = u.org_id
      AND p.organization_type = '2'
      AND p.del_flag = 'N'
      AND u.account LIKE 'qc_%'
      AND u.del_flag = 'N'
""")
print(f"更新行数：{cur.rowcount}")
conn.commit()

# 验证
print("\n=== 验证结果 ===")
cur.execute("""
    SELECT u.account, u.user_id, u.person_id, p.person_name
    FROM sys_user u
    LEFT JOIN hr_person p ON p.person_id = u.person_id
    WHERE u.account LIKE 'qc_%' AND u.del_flag = 'N'
    ORDER BY u.account
""")
for r in cur.fetchall():
    status = "OK" if r[2] else "MISSING"
    print(f"  [{status}]  {r[0]:<10}  user_id={r[1]}  person_id={r[2]}  person_name={r[3]}")

conn.close()
