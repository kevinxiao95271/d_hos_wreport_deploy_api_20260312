"""
fix_tpl3_count_items.py
------------------------
将"市级成立个数"和"县级成立个数"从 wr_template_row 移出，
改为独立的 wr_template_item（valueType='number'）。

具体操作：
1. 软删除 rows 3、15（市级成立个数、县级成立个数 section header 行）
2. 清除行 4-14 和各市行（16,30,41...）的 parent_row_index（它们原来指向3或15）
3. 新增2个 template_item：市级成立个数、县级成立个数（valueType='number'）
"""
import sys, io, psycopg2
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

DB = dict(host="119.167.165.27", port=5432, dbname="zjylzl",
          user="postgres", password="zjylzl", options="-c search_path=zjylzl")
TPL3_ID = 2_000_000_000_000_002

# 新 item ID（固定，幂等）
ITEM_CITY_COUNT   = 2_000_000_000_100_002   # 市级成立个数
ITEM_COUNTY_COUNT = 2_000_000_000_100_003   # 县级成立个数

# 原来 parent 指向 row3 的行（市级别：4-14）
ROWS_UNDER_3  = list(range(4, 15))

# 原来 parent 指向 row15 的行（各市：杭州/宁波/...）
ROWS_UNDER_15 = [16, 30, 41, 54, 60, 68, 75, 85, 92, 97, 107]

def run():
    conn = psycopg2.connect(**DB)
    cur  = conn.cursor()

    # 1. 软删除行 3 和行 15
    cur.execute("""
        UPDATE wr_template_row SET del_flag=1
         WHERE template_id=%s AND row_index IN (3, 15)
    """, (TPL3_ID,))
    print(f"软删除 rows 3/15：{cur.rowcount} 行")

    # 2. 清除行 4-14 的 parent_row_index（原指向3）
    cur.execute("""
        UPDATE wr_template_row SET parent_row_index=NULL
         WHERE template_id=%s AND row_index=ANY(%s) AND del_flag=0
    """, (TPL3_ID, ROWS_UNDER_3))
    print(f"清除行4-14 parent：{cur.rowcount} 行")

    # 3. 清除各市行的 parent_row_index（原指向15）
    cur.execute("""
        UPDATE wr_template_row SET parent_row_index=NULL
         WHERE template_id=%s AND row_index=ANY(%s) AND del_flag=0
    """, (TPL3_ID, ROWS_UNDER_15))
    print(f"清除市行 parent：{cur.rowcount} 行")

    # 4. 新增 item：市级成立个数
    cur.execute("SELECT id FROM wr_template_item WHERE id=%s", (ITEM_CITY_COUNT,))
    if not cur.fetchone():
        cur.execute("""
            INSERT INTO wr_template_item
              (id,template_id,parent_id,item_name,header_row,col_index,
               row_span,col_span,is_leaf,value_type,sort_num,del_flag)
            VALUES (%s,%s,NULL,'市级成立个数',1,2,1,1,1,'number',2,0)
        """, (ITEM_CITY_COUNT, TPL3_ID))
        print(f"新增 item '市级成立个数' id={ITEM_CITY_COUNT}")
    else:
        print(f"item '市级成立个数' 已存在，跳过")

    # 5. 新增 item：县级成立个数
    cur.execute("SELECT id FROM wr_template_item WHERE id=%s", (ITEM_COUNTY_COUNT,))
    if not cur.fetchone():
        cur.execute("""
            INSERT INTO wr_template_item
              (id,template_id,parent_id,item_name,header_row,col_index,
               row_span,col_span,is_leaf,value_type,sort_num,del_flag)
            VALUES (%s,%s,NULL,'县级成立个数',1,3,1,1,1,'number',3,0)
        """, (ITEM_COUNTY_COUNT, TPL3_ID))
        print(f"新增 item '县级成立个数' id={ITEM_COUNTY_COUNT}")
    else:
        print(f"item '县级成立个数' 已存在，跳过")

    conn.commit()

    # 验证
    cur.execute("""
        SELECT id, item_name, value_type, sort_num
          FROM wr_template_item WHERE template_id=%s AND del_flag=0 ORDER BY sort_num
    """, (TPL3_ID,))
    print("\n当前 template_item：")
    for r in cur.fetchall():
        print(f"  id={r[0]} name={r[1]} type={r[2]} sort={r[3]}")

    cur.execute("""
        SELECT row_index, row_label, row_level, parent_row_index
          FROM wr_template_row WHERE template_id=%s AND del_flag=0
         ORDER BY sort_num LIMIT 20
    """, (TPL3_ID,))
    print("\n当前 template_row（前20）：")
    for r in cur.fetchall():
        indent = "  " * ((r[2] or 1) - 1)
        print(f"  {indent}[{r[0]}] {r[1]}  level={r[2]} parent={r[3]}")

    conn.close()
    print("\n完成！")

if __name__ == "__main__":
    run()
