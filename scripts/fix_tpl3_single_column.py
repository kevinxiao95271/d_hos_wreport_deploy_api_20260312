"""
fix_tpl3_single_column.py
--------------------------
附件3模板重构：
- 删除原来26个机构名称列（错误设计，将"谁填"硬编码进了模板）
- 新增1个通用列 "已成立"（valueType=checkbox），所有机构共用此itemId
- 更新 wr_record_value 中的 item_id 指向新列
"""
import sys, io, psycopg2, time
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

DB = dict(host="119.167.165.27", port=5432, dbname="zjylzl",
          user="postgres", password="zjylzl", options="-c search_path=zjylzl")
TPL3_ID       = 2_000_000_000_000_002
NEW_ITEM_ID   = 2_000_000_000_100_001   # 固定ID，方便幂等

def run():
    conn = psycopg2.connect(**DB)
    cur  = conn.cursor()

    # 1. 查当前附件3所有列
    cur.execute("SELECT id, item_name FROM wr_template_item WHERE template_id=%s AND del_flag=0", (TPL3_ID,))
    old_items = cur.fetchall()
    print(f"当前附件3列数: {len(old_items)}")
    old_ids = [r[0] for r in old_items]

    # 2. 确保新列存在（幂等）
    cur.execute("SELECT id FROM wr_template_item WHERE id=%s", (NEW_ITEM_ID,))
    if not cur.fetchone():
        cur.execute("""
            INSERT INTO wr_template_item
              (id, template_id, parent_id, item_name, header_row, col_index,
               row_span, col_span, is_leaf, value_type, sort_num, del_flag)
            VALUES (%s,%s,NULL,'已成立',1,1,1,1,1,'checkbox',1,0)
        """, (NEW_ITEM_ID, TPL3_ID))
        print(f"新建通用列 '已成立' id={NEW_ITEM_ID}")
    else:
        print(f"通用列已存在 id={NEW_ITEM_ID}")

    # 3. 把 wr_record_value 里所有旧 item_id 都改指新列
    if old_ids:
        cur.execute("""
            UPDATE wr_record_value SET item_id = %s
            WHERE item_id = ANY(%s)
        """, (NEW_ITEM_ID, old_ids))
        print(f"wr_record_value 更新 {cur.rowcount} 条 -> item_id={NEW_ITEM_ID}")

    # 4. 删除旧的26个机构列（硬删，非业务数据）
    if old_ids:
        # 排除新列本身（如果它恰好在 old_ids 里）
        to_delete = [i for i in old_ids if i != NEW_ITEM_ID]
        if to_delete:
            cur.execute("DELETE FROM wr_template_item WHERE id = ANY(%s)", (to_delete,))
            print(f"删除旧机构列 {cur.rowcount} 条")

    conn.commit()

    # 5. 验证
    cur.execute("SELECT id, item_name, value_type FROM wr_template_item WHERE template_id=%s AND del_flag=0", (TPL3_ID,))
    final = cur.fetchall()
    print(f"\n修复后附件3列数: {len(final)}")
    for r in final:
        print(f"  id={r[0]} name={r[1]} type={r[2]}")

    cur.execute("""
        SELECT COUNT(DISTINCT item_id) FROM wr_record_value rv
        JOIN wr_record r ON r.id=rv.record_id
        WHERE r.template_id=%s
    """, (TPL3_ID,))
    print(f"wr_record_value 中 distinct item_id 数: {cur.fetchone()[0]}（应为1）")

    conn.close()
    print("\n完成！")

if __name__ == "__main__":
    run()
