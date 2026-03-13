"""
seed_item_attachments.py
-----------------------
1. 更新 附件2 中需要佐证材料的列 require_attachment=1
2. 为 wr_org_a 的 附件2 记录上传带 item_id 的测试附件到 MinIO
3. 插入 wr_attachment 记录

需要附件的列（按业务逻辑）：
  col 24 - 质控调研/专项调研/开展内容  (L1="质控调研（请附调研报告）")
  col 26 - 质控调研/行政委托全省调研/开展内容
  col 35 - 撰写年度数据分析报告
  col 36 - 申报课题（注明课题信息）
  col 37 - 撰写论文（注明题目及状态）
"""
import sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

import psycopg2, time, random, io as bio
from minio import Minio

DB = dict(host="119.167.165.27", port=5432, dbname="zjylzl",
          user="postgres", password="zjylzl", options="-c search_path=zjylzl")
MINIO_ENDPOINT   = "119.167.165.27:58010"
MINIO_ACCESS     = "minioadmin"
MINIO_SECRET     = "Ygcx2025"
BUCKET_EVIDENCE  = "wk-registration-files"
TPL2_ID          = 2_000_000_000_000_001
REQUIRE_ATT_COLS = [24, 26, 35, 36, 37]

# Snowflake-like id
_seq = 0
def next_id():
    global _seq
    ts = int(time.time() * 1000) & 0x1FFFFFFFFFF
    _seq = (_seq + 1) & 0xFFF
    return (ts << 22) | (1 << 12) | _seq

def run():
    conn = psycopg2.connect(**DB)
    cur  = conn.cursor()

    # ── 1. 找出需要附件的 item_id ────────────────────────────────────────────
    cur.execute("""
        SELECT id, col_index, item_name FROM wr_template_item
        WHERE template_id=%s AND del_flag=0 AND is_leaf=1
          AND col_index = ANY(%s)
        ORDER BY col_index
    """, (TPL2_ID, REQUIRE_ATT_COLS))
    req_items = cur.fetchall()
    print("需要附件的列：")
    for row in req_items:
        print(f"  col={row[1]} id={row[0]} name={row[2]}")

    # ── 2. 更新 require_attachment=1 ────────────────────────────────────────
    req_item_ids = [r[0] for r in req_items]
    cur.execute("""
        UPDATE wr_template_item SET require_attachment=1
        WHERE template_id=%s AND col_index = ANY(%s) AND is_leaf=1
    """, (TPL2_ID, REQUIRE_ATT_COLS))
    print(f"\n已更新 {cur.rowcount} 列 require_attachment=1")

    # ── 3. 找 wr_org_a 的附件2记录 ──────────────────────────────────────────
    cur.execute("""
        SELECT r.id FROM wr_record r
        JOIN sys_user u ON u.org_id = r.org_id
        WHERE u.account='wr_org_a' AND r.template_id=%s AND r.del_flag=0
        ORDER BY r.create_time DESC LIMIT 1
    """, (TPL2_ID,))
    row = cur.fetchone()
    if not row:
        print("未找到 wr_org_a 的附件2记录，请先运行 seed_tpl2_data.py")
        conn.rollback()
        conn.close()
        return
    record_id = row[0]
    print(f"\nwr_org_a 附件2 record_id = {record_id}")

    # 获取 wr_org_a 的 user_id
    cur.execute("SELECT user_id FROM sys_user WHERE account='wr_org_a'")
    user_row = cur.fetchone()
    user_id  = user_row[0] if user_row else None

    # ── 4. 清除旧的 item 级附件（如有） ────────────────────────────────────
    cur.execute("""
        DELETE FROM wr_attachment
        WHERE record_id=%s AND item_id IS NOT NULL
    """, (record_id,))
    print(f"清除旧 item 级附件 {cur.rowcount} 条")

    # ── 5. 上传 MinIO + 写 wr_attachment ──────────────────────────────────
    minio = Minio(MINIO_ENDPOINT, access_key=MINIO_ACCESS,
                  secret_key=MINIO_SECRET, secure=False)

    attachment_meta = {
        24: ("质控调研_专项调研报告.txt",     "本年度专项调研开展情况报告（示例文件）"),
        26: ("质控调研_行政委托调研报告.txt", "行政委托全省调研结果汇总（示例文件）"),
        35: ("年度数据分析报告.txt",          "2025年度质控数据分析报告（示例文件）"),
        36: ("课题申报材料.txt",             "申报课题立项书及研究方案（示例文件）"),
        37: ("论文清单.txt",                 "本年度以质控名义发表论文目录（示例文件）"),
    }

    for item_id, col_idx, item_name in [(r[0], r[1], r[2]) for r in req_items]:
        meta = attachment_meta.get(col_idx, (f"col{col_idx}_attachment.txt", "示例附件内容"))
        fname, content = meta
        file_content = f"{content}\n生成时间：{time.strftime('%Y-%m-%d %H:%M:%S')}\n".encode("utf-8")
        obj_name = f"record/{record_id}/item_{item_id}/{fname}"
        minio.put_object(BUCKET_EVIDENCE, obj_name,
                         bio.BytesIO(file_content), len(file_content),
                         content_type="text/plain")
        url = f"/{BUCKET_EVIDENCE}/{obj_name}"
        att_id = next_id()
        time.sleep(0.01)
        cur.execute("""
            INSERT INTO wr_attachment(id, record_id, item_id, attach_name, attach_path,
                                      attach_size, attach_type, create_user, create_time, del_flag)
            VALUES(%s,%s,%s,%s,%s,%s,'text/plain',%s,NOW(),0)
        """, (att_id, record_id, item_id, fname, url, len(file_content), user_id))
        print(f"  上传 col={col_idx} item_id={item_id} -> {fname} ({len(file_content)} bytes)")

    conn.commit()
    conn.close()
    print("\n全部完成！")

if __name__ == "__main__":
    run()
