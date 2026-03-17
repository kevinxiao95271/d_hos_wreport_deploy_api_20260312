import psycopg2, sys
sys.stdout.reconfigure(encoding='utf-8')

conn = psycopg2.connect(
    host='119.167.165.27', port=5432, dbname='zjylzl',
    user='postgres', password='zjylzl',
    options='-c search_path=zjylzl'
)
cur = conn.cursor()

TPL2 = 2000000000000001

cur.execute("""
SELECT id, parent_id, item_name, header_row, col_index, is_leaf, value_type, dict_code, sort_num
FROM wr_template_item
WHERE template_id = %s AND del_flag = 0
ORDER BY header_row, col_index, sort_num
""", (TPL2,))
rows = cur.fetchall()
print(f"总节点数: {len(rows)}")

null_parent = [r for r in rows if r[1] is None]
has_parent  = [r for r in rows if r[1] is not None]
print(f"  parent_id=null: {len(null_parent)} 个")
print(f"  parent_id非null: {len(has_parent)} 个")

print("\n--- parent_id=null 的节点（应当只有根节点）---")
for r in null_parent:
    print(f"  id={r[0]}  header_row={r[2]}  isLeaf={r[5]}  name={r[1+1]}")

if len(null_parent) > 5:
    print("\n⚠ parent_id=null 节点超过5个，树结构已崩塌（全变平了）")

# 检查是否存在孤儿节点（parentId 指向不存在的 id）
all_ids = {r[0] for r in rows}
orphans = [r for r in rows if r[1] is not None and r[1] not in all_ids]
print(f"\n孤儿节点（parentId 指向不存在id）: {len(orphans)} 个")
for r in orphans:
    print(f"  id={r[0]}  parent_id={r[1]}  name={r[2]}")

conn.close()
