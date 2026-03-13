"""
fix_tpl3_and_boolean_fields.py
--------------------------------
1. 删除附件3中误入的28条多余列（IDs 2032...，value_type=text）
   这些是之前 API 测试写入的垃圾数据
2. 附件2中 is/否 字段（col 1, 44, 45, 46）value_type 改为 boolean
3. 更新附件2测试数据：所有机构的这些字段值改为 "0" 或 "1"
"""
import sys, io, random
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
import psycopg2

DB = dict(host="119.167.165.27", port=5432, dbname="zjylzl",
          user="postgres", password="zjylzl", options="-c search_path=zjylzl")

TPL2_ID = 2_000_000_000_000_001
TPL3_ID = 2_000_000_000_000_002

# 附件2 的布尔字段 col_index
BOOLEAN_COLS_TPL2 = [1, 44, 45, 46]

def run():
    conn = psycopg2.connect(**DB)
    cur  = conn.cursor()

    # ── 1. 清除附件3中的多余text列（IDs 2032...） ──────────────────────────
    # 这些列 ID 不是用 next_id() 生成的，是通过 API 误写入的
    cur.execute("""
        SELECT id, item_name, col_index, value_type FROM wr_template_item
        WHERE template_id = %s AND value_type != 'checkbox' AND del_flag = 0
        ORDER BY col_index
    """, (TPL3_ID,))
    spurious = cur.fetchall()
    print(f"[附件3] 发现多余列 {len(spurious)} 条：")
    for r in spurious:
        print(f"  id={r[0]} col={r[2]} name={r[1]} type={r[3]}")

    if spurious:
        spurious_ids = [r[0] for r in spurious]
        # 先清理这些列的 values（如有）
        cur.execute("DELETE FROM wr_record_value WHERE item_id = ANY(%s)", (spurious_ids,))
        deleted_vals = cur.rowcount
        # 硬删除这些列（非业务数据，直接删）
        cur.execute("DELETE FROM wr_template_item WHERE id = ANY(%s)", (spurious_ids,))
        print(f"  已删除 {cur.rowcount} 条多余列，清理关联值 {deleted_vals} 条")

    # ── 2. 附件2 是/否字段改为 boolean ──────────────────────────────────────
    cur.execute("""
        UPDATE wr_template_item
        SET value_type = 'boolean'
        WHERE template_id = %s AND col_index = ANY(%s) AND is_leaf = 1
    """, (TPL2_ID, BOOLEAN_COLS_TPL2))
    print(f"\n[附件2] 已将 {cur.rowcount} 个字段的 value_type 改为 boolean")

    # 确认字段名
    cur.execute("""
        SELECT col_index, item_name, value_type FROM wr_template_item
        WHERE template_id = %s AND col_index = ANY(%s) AND is_leaf = 1
        ORDER BY col_index
    """, (TPL2_ID, BOOLEAN_COLS_TPL2))
    print("  布尔字段：")
    for r in cur.fetchall():
        print(f"  col={r[0]} [{r[1]}] type={r[2]}")

    # ── 3. 更新附件2测试数据：是/否字段改为 "0" 或 "1" ──────────────────────
    # 找出所有附件2记录的 boolean 列 item_ids
    cur.execute("""
        SELECT id, item_name FROM wr_template_item
        WHERE template_id = %s AND col_index = ANY(%s) AND is_leaf = 1
    """, (TPL2_ID, BOOLEAN_COLS_TPL2))
    bool_items = cur.fetchall()  # [(id, name), ...]
    print(f"\n[附件2] 更新测试数据中的布尔字段（共 {len(bool_items)} 个列）：")

    # 特殊值设定：是否为国家级质控中心(col=1)各机构不同，其余字段合理填值
    col_val_map = {
        1:  None,   # 各机构随机 0/1（是否为国家级）
        44: "1",    # 是否有专职人员 -> 是
        45: "1",    # 是否有专用场所 -> 是
        46: "0",    # 是否需要跨专业协同 -> 否
    }
    cur.execute("""
        SELECT col_index, id FROM wr_template_item
        WHERE template_id = %s AND col_index = ANY(%s) AND is_leaf = 1
    """, (TPL2_ID, BOOLEAN_COLS_TPL2))
    col_to_item = {r[0]: r[1] for r in cur.fetchall()}

    # 找所有附件2的记录
    cur.execute("""
        SELECT id FROM wr_record WHERE template_id = %s AND del_flag = 0
    """, (TPL2_ID,))
    records = [r[0] for r in cur.fetchall()]
    print(f"  涉及记录数：{len(records)}")

    updated = 0
    for rec_id in records:
        for col_idx, item_id in col_to_item.items():
            val = col_val_map.get(col_idx)
            if val is None:
                val = str(random.randint(0, 1))  # col=1 各记录随机
            cur.execute("""
                UPDATE wr_record_value SET cell_value = %s
                WHERE record_id = %s AND item_id = %s
            """, (val, rec_id, item_id))
            if cur.rowcount == 0:
                # 该记录没有该字段的值，不插入（保持空值）
                pass
            updated += cur.rowcount

    print(f"  已更新 {updated} 条值为 0/1")

    conn.commit()
    conn.close()
    print("\n全部完成！")

if __name__ == "__main__":
    run()
