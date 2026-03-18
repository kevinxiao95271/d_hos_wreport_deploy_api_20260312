import psycopg2

conn = psycopg2.connect(host='119.167.165.27', port=5432, dbname='zjylzl',
                        user='postgres', password='zjylzl', options='-c search_path=zjylzl')
cur = conn.cursor()

# 当前 qc_ 账号在 sys_user 里的 person_id 是什么
print("=== sys_user qc_ 账号当前 person_id ===")
cur.execute("""
    SELECT u.account, u.user_id, u.person_id, u.org_id
    FROM sys_user u
    WHERE u.account LIKE 'qc_%' AND u.del_flag = 'N'
    ORDER BY u.account
""")
sys_rows = cur.fetchall()
for r in sys_rows:
    print(f"  {r[0]:<10} user_id={r[1]}  person_id={r[2]}  org_id={r[3]}")

# hr_person 里刚插入的记录
print("\n=== hr_person type=2 新插入记录（按 org_id 对应）===")
cur.execute("""
    SELECT p.person_id, p.person_name, p.org_id
    FROM hr_person p
    WHERE p.organization_type = '2' AND p.del_flag = 'N'
      AND p.org_id IN (
          SELECT org_id FROM sys_user WHERE account LIKE 'qc_%' AND del_flag='N'
      )
    ORDER BY p.person_name
""")
person_rows = cur.fetchall()
for r in person_rows:
    print(f"  person_id={r[0]}  person_name={r[1]}  org_id={r[2]}")

conn.close()
