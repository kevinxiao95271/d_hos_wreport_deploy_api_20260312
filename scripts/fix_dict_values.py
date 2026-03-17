"""
fix_dict_values.py
-------------------
将附件2测试数据中绑定了字典的字段值更新到合法值域内：
  yes_no       → "0" 或 "1"（随机）
  meeting_form → "online" / "offline" / "both"（随机）
  task_status  → "done" / "in_progress" / "not_done"（随机）
"""
import sys, io, psycopg2, random
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

DB = dict(host="119.167.165.27", port=5432, dbname="zjylzl",
          user="postgres", password="zjylzl", options="-c search_path=zjylzl")
TPL2_ID = 2_000_000_000_000_001

DICT_VALUES = {
    "yes_no":       ["0", "1"],
    "meeting_form": ["online", "offline", "both"],
    "task_status":  ["done", "in_progress", "not_done"],
}

def run():
    conn = psycopg2.connect(**DB)
    cur  = conn.cursor()

    # 查出所有绑定了字典的叶子节点
    cur.execute("""
        SELECT id, item_name, dict_code
          FROM wr_template_item
         WHERE template_id=%s AND is_leaf=1 AND del_flag=0
           AND dict_code IS NOT NULL
    """, (TPL2_ID,))
    dict_items = cur.fetchall()
    print(f"绑定字典的叶子节点: {len(dict_items)}")

    total = 0
    for item_id, item_name, dict_code in dict_items:
        valid_values = DICT_VALUES.get(dict_code, [])
        if not valid_values:
            print(f"  [{item_name}] 未知字典 {dict_code}，跳过")
            continue

        # 查出该字段的所有测试记录值
        cur.execute("""
            SELECT id, cell_value FROM wr_record_value
             WHERE item_id=%s AND template_id=%s
        """, (item_id, TPL2_ID))
        rows = cur.fetchall()

        for rv_id, cell_value in rows:
            if cell_value not in valid_values:
                new_val = random.choice(valid_values)
                cur.execute("UPDATE wr_record_value SET cell_value=%s WHERE id=%s",
                            (new_val, rv_id))
                total += 1

        print(f"  [{item_name}] dict={dict_code}  更新 {len(rows)} 条  "
              f"→ 合法值: {valid_values}")

    conn.commit()
    print(f"\n共更新 {total} 条记录值")

    # 验证
    print("\n验证（各字典字段的当前值分布）：")
    for item_id, item_name, dict_code in dict_items:
        cur.execute("""
            SELECT cell_value, COUNT(*) FROM wr_record_value
             WHERE item_id=%s AND template_id=%s
             GROUP BY cell_value ORDER BY cell_value
        """, (item_id, TPL2_ID))
        dist = {r[0]: r[1] for r in cur.fetchall()}
        print(f"  [{item_name}] {dist}")

    conn.close()
    print("\n完成！")

if __name__ == "__main__":
    run()
