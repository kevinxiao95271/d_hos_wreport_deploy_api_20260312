import psycopg2

conn = psycopg2.connect(host='119.167.165.27', port=5432, dbname='zjylzl',
                        user='postgres', password='zjylzl', options='-c search_path=zjylzl')
cur = conn.cursor()

target = 1986677623363010563

# 查这个 org 本身
cur.execute("""
    SELECT org_id, org_name, org_parent_id, org_pids, org_type, org_level
    FROM hr_organization WHERE org_id = %s
""", (target,))
r = cur.fetchone()
print(f"=== org_id={target} ===")
print(f"  org_name   : {r[1]}")
print(f"  org_parent_id : {r[2]}")
print(f"  org_pids   : {r[3]}")
print(f"  org_type   : {r[4]}")
print(f"  org_level  : {r[5]}")

# 查它的父节点
if r[2]:
    cur.execute("SELECT org_id, org_name, org_level FROM hr_organization WHERE org_id = %s", (r[2],))
    parent = cur.fetchone()
    print(f"\n  父节点: {parent}")

# 查它的直接子节点（了解层级结构）
cur.execute("""
    SELECT org_id, org_name, org_level
    FROM hr_organization WHERE org_parent_id = %s
    ORDER BY org_sort
    LIMIT 10
""", (target,))
children = cur.fetchall()
print(f"\n  直接子节点（{len(children)} 个）:")
for c in children:
    print(f"    {c[0]}  {c[1]}  level={c[2]}")

conn.close()
