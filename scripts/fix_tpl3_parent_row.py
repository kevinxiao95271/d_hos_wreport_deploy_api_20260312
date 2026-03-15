"""
fix_tpl3_parent_row.py
-----------------------
修复附件3行定义的 parent_row_index：
  - 行 4-14（市级别汇总行）→ parent = 3（市级成立个数）
  - 行 16,30,41,54,60,68,75,85,92,97,107（各市行）→ parent = 15（县级成立个数）
  - 行 17+ 的县区级已正确，不动
"""
import sys, io, psycopg2
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

DB = dict(host="119.167.165.27", port=5432, dbname="zjylzl",
          user="postgres", password="zjylzl", options="-c search_path=zjylzl")
TPL3_ID = 2_000_000_000_000_002

# 市级别汇总行：row_index 4-14，隶属于 row 3（市级成立个数）
CITY_SUMMARY_ROWS = list(range(4, 15))   # 4,5,...,14

# 各市行（下辖县区），隶属于 row 15（县级成立个数）
CITY_ROWS = [16, 30, 41, 54, 60, 68, 75, 85, 92, 97, 107]

def run():
    conn = psycopg2.connect(**DB)
    cur  = conn.cursor()

    cur.execute("""
        UPDATE wr_template_row
           SET parent_row_index = 3
         WHERE template_id = %s
           AND row_index = ANY(%s)
           AND del_flag = 0
    """, (TPL3_ID, CITY_SUMMARY_ROWS))
    print(f"市级别汇总行 parent→3：更新 {cur.rowcount} 行")

    cur.execute("""
        UPDATE wr_template_row
           SET parent_row_index = 15
         WHERE template_id = %s
           AND row_index = ANY(%s)
           AND del_flag = 0
    """, (TPL3_ID, CITY_ROWS))
    print(f"各市行 parent→15：更新 {cur.rowcount} 行")

    conn.commit()

    # 验证：打印前几个看看
    cur.execute("""
        SELECT row_index, row_label, row_level, parent_row_index
          FROM wr_template_row
         WHERE template_id = %s AND del_flag = 0
         ORDER BY sort_num
         LIMIT 30
    """, (TPL3_ID,))
    print("\n验证（前30行）：")
    for r in cur.fetchall():
        indent = "  " * ((r[2] or 1) - 1)
        print(f"{indent}[{r[0]}] {r[1]}  level={r[2]} parent={r[3]}")

    conn.close()
    print("\n完成！")

if __name__ == "__main__":
    run()
