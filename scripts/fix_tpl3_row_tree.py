"""
fix_tpl3_row_tree.py
---------------------
构建附件3完整行树结构：
  Row 1 (省市县全部成立) = 全局根，parent=null
    Row 2 (市级全部成立)  parent=1
      Row 4-14 (各市级)   parent=2
    Row 16,30,41,54,60,68,75,85,92,97,107 (各市) parent=1
      Row 17+... (各县区) parent=对应市行  ← 已正确，不动
"""
import sys, io, psycopg2
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

DB = dict(host="119.167.165.27", port=5432, dbname="zjylzl",
          user="postgres", password="zjylzl", options="-c search_path=zjylzl")
TPL3_ID = 2_000_000_000_000_002

CITY_LEVEL_ROWS = list(range(4, 15))          # 4-14：各市级
CITY_ROWS       = [16,30,41,54,60,68,75,85,92,97,107]  # 各市（下辖县区）

def run():
    conn = psycopg2.connect(**DB)
    cur  = conn.cursor()

    # Row 2 → parent=1
    cur.execute("""
        UPDATE wr_template_row SET parent_row_index=1
         WHERE template_id=%s AND row_index=2 AND del_flag=0
    """, (TPL3_ID,))
    print(f"Row2 parent→1：{cur.rowcount} 行")

    # Rows 4-14 → parent=2
    cur.execute("""
        UPDATE wr_template_row SET parent_row_index=2
         WHERE template_id=%s AND row_index=ANY(%s) AND del_flag=0
    """, (TPL3_ID, CITY_LEVEL_ROWS))
    print(f"Rows4-14 parent→2：{cur.rowcount} 行")

    # 各市行 → parent=1
    cur.execute("""
        UPDATE wr_template_row SET parent_row_index=1
         WHERE template_id=%s AND row_index=ANY(%s) AND del_flag=0
    """, (TPL3_ID, CITY_ROWS))
    print(f"各市行 parent→1：{cur.rowcount} 行")

    conn.commit()

    # 验证前30行
    cur.execute("""
        SELECT row_index, row_label, row_level, parent_row_index
          FROM wr_template_row
         WHERE template_id=%s AND del_flag=0
         ORDER BY sort_num LIMIT 30
    """, (TPL3_ID,))
    print("\n验证（前30行）：")
    rows = cur.fetchall()
    idx_map = {r[0]: r[1] for r in rows}
    for r in rows:
        indent = "  " * ((r[2] or 1) - 1)
        parent_label = f"→ {idx_map.get(r[3], '?')}" if r[3] else ""
        print(f"  {indent}[{r[0]}] {r[1]}  parent={r[3]} {parent_label}")

    conn.close()
    print("\n完成！")

if __name__ == "__main__":
    run()
