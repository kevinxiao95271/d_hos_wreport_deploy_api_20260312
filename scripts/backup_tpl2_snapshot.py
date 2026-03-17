"""
backup_tpl2_snapshot.py
-----------------------
将附件2当前 wr_template_item 全量导出为可重放的 SQL INSERT 底稿。
万一后续操作出错，直接执行 restore_tpl2.sql 即可还原。
"""
import psycopg2, sys, datetime
sys.stdout.reconfigure(encoding='utf-8')

DB = dict(host='119.167.165.27', port=5432, dbname='zjylzl',
          user='postgres', password='zjylzl', options='-c search_path=zjylzl')
TPL2_ID = 2_000_000_000_000_001

conn = psycopg2.connect(**DB)
cur  = conn.cursor()

cur.execute("""
    SELECT id, template_id, parent_id, item_name, header_row, col_index,
           row_span, col_span, is_leaf, value_type, unit, placeholder,
           require_attachment, sort_num, dict_code, del_flag
    FROM wr_template_item
    WHERE template_id = %s
    ORDER BY header_row, col_index, sort_num
""", (TPL2_ID,))
rows = cur.fetchall()

ts   = datetime.datetime.now().strftime('%Y%m%d_%H%M%S')
fname = f'scripts/restore_tpl2_{ts}.sql'

with open(fname, 'w', encoding='utf-8') as f:
    f.write(f"-- 附件2 wr_template_item 底稿，生成时间: {ts}\n")
    f.write(f"-- 共 {len(rows)} 行\n\n")
    f.write(f"DELETE FROM wr_template_item WHERE template_id = {TPL2_ID};\n\n")
    for r in rows:
        def v(x):
            if x is None: return 'NULL'
            if isinstance(x, str): return "'" + x.replace("'","''") + "'"
            return str(x)
        cols = ('id','template_id','parent_id','item_name','header_row','col_index',
                'row_span','col_span','is_leaf','value_type','unit','placeholder',
                'require_attachment','sort_num','dict_code','del_flag')
        vals = ','.join(v(x) for x in r)
        f.write(f"INSERT INTO wr_template_item ({','.join(cols)}) VALUES ({vals});\n")

print(f"✅ 底稿已保存: {fname}  ({len(rows)} 行)")
conn.close()
