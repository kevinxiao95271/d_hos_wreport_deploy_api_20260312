"""
fix_tpl3_seed_data.py
----------------------
清理附件3测试数据中错误的多列情况：
每条 wr_record 只属于一个机构，只应有一个 item_id 的数据。
如果一条记录存了多个 item_id，按 col_index 最小的那个保留，其余删除。

同时尝试通过 org_name 匹配正确的 item（优先匹配，无匹配时保留 col_index 最小的）。
"""
import sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
import psycopg2

DB = dict(host="119.167.165.27", port=5432, dbname="zjylzl",
          user="postgres", password="zjylzl", options="-c search_path=zjylzl")
TPL3_ID = 2_000_000_000_000_002

def run():
    conn = psycopg2.connect(**DB)
    cur  = conn.cursor()

    # 拿附件3所有列定义 (item_id -> item_name, col_index)
    cur.execute("""
        SELECT id, item_name, col_index FROM wr_template_item
        WHERE template_id = %s AND is_leaf = 1 AND del_flag = 0
        ORDER BY col_index
    """, (TPL3_ID,))
    all_items = cur.fetchall()   # [(id, name, col_index), ...]
    name_to_item = {r[1]: r[0] for r in all_items}
    print(f"附件3列定义共 {len(all_items)} 列")

    # 拿所有附件3记录及其 item_id 分布
    cur.execute("""
        SELECT r.id, r.org_name,
               ARRAY_AGG(DISTINCT rv.item_id ORDER BY rv.item_id) as item_ids
        FROM wr_record r
        JOIN wr_record_value rv ON rv.record_id = r.id
        WHERE r.template_id = %s AND r.del_flag = 0
        GROUP BY r.id, r.org_name
    """, (TPL3_ID,))
    records = cur.fetchall()
    print(f"附件3记录共 {len(records)} 条\n")

    for rec_id, org_name, item_ids in records:
        if len(item_ids) <= 1:
            print(f"  record={rec_id} org={org_name} -> 正常（1列）")
            continue

        print(f"  record={rec_id} org={org_name} -> 有 {len(item_ids)} 列，需修复")

        # 优先：org_name 与 item_name 匹配（如"超声质控中心" 匹配 "超声质控中心技术指导中心"）
        keep_item_id = None
        for name, iid in name_to_item.items():
            if org_name and (org_name in name or name in org_name):
                keep_item_id = iid
                print(f"    按名称匹配到: {name} (id={iid})")
                break

        if keep_item_id is None:
            # 无匹配时保留 col_index 最小的那一列
            cur.execute("""
                SELECT rv.item_id, ti.col_index, ti.item_name
                FROM wr_record_value rv
                JOIN wr_template_item ti ON ti.id = rv.item_id
                WHERE rv.record_id = %s
                ORDER BY ti.col_index
                LIMIT 1
            """, (rec_id,))
            row = cur.fetchone()
            keep_item_id = row[0]
            print(f"    无名称匹配，保留 col={row[1]} {row[2]} (id={keep_item_id})")

        # 删除其他 item_id 的 values
        cur.execute("""
            DELETE FROM wr_record_value
            WHERE record_id = %s AND item_id != %s
        """, (rec_id, keep_item_id))
        print(f"    删除 {cur.rowcount} 条错误values，保留 item_id={keep_item_id}")

    conn.commit()

    # 验证
    print("\n修复后验证：")
    cur.execute("""
        SELECT r.id, r.org_name, COUNT(DISTINCT rv.item_id) as item_count
        FROM wr_record r
        JOIN wr_record_value rv ON rv.record_id = r.id
        WHERE r.template_id = %s AND r.del_flag = 0
        GROUP BY r.id, r.org_name
        ORDER BY r.id
    """, (TPL3_ID,))
    for r in cur.fetchall():
        status = "✓" if r[2] == 1 else "✗"
        print(f"  {status} record={r[0]} org={r[1]} item_count={r[2]}")

    conn.close()
    print("\n完成！")

if __name__ == "__main__":
    run()
