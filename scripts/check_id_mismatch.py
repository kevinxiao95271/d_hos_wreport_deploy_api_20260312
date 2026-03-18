import psycopg2

conn = psycopg2.connect(host='119.167.165.27', port=5432, dbname='zjylzl',
                        user='postgres', password='zjylzl', options='-c search_path=zjylzl')
cur = conn.cursor()

# 1. 总体数量
cur.execute("""
    SELECT 
        COUNT(*) AS total,
        COUNT(person_id) AS has_person_id,
        COUNT(CASE WHEN user_id = person_id THEN 1 END) AS user_eq_person,
        COUNT(CASE WHEN user_id != person_id AND person_id IS NOT NULL THEN 1 END) AS user_ne_person
    FROM sys_user
    WHERE del_flag = 'N'
""")
r = cur.fetchone()
print(f"总有效用户数      : {r[0]}")
print(f"有 person_id      : {r[1]}")
print(f"user_id = person_id : {r[2]}")
print(f"user_id != person_id (且 person_id 不为空) : {r[3]}")

# 2. 不相等的具体记录
print("\n=== user_id != person_id 的详情 ===")
cur.execute("""
    SELECT u.account, u.real_name, u.user_id, u.person_id,
           p.person_name, p.organization_type
    FROM sys_user u
    LEFT JOIN hr_person p ON p.person_id = u.person_id
    WHERE u.del_flag = 'N'
      AND u.person_id IS NOT NULL
      AND u.user_id != u.person_id
    ORDER BY u.account
    LIMIT 30
""")
rows = cur.fetchall()
print(f"共 {len(rows)} 条")
print(f"{'account':<12} {'real_name':<25} {'user_id':<22} {'person_id':<22} {'hr_person.name':<25} {'org_type'}")
print("-" * 130)
for r in rows:
    print(f"{str(r[0]):<12} {str(r[1]):<25} {str(r[2]):<22} {str(r[3]):<22} {str(r[4]):<25} {r[5]}")

conn.close()
