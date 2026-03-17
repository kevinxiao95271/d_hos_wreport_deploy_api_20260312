"""
init_dict_tables.py
--------------------
1. 创建 wr_dict_type / wr_dict_item 表
2. ALTER wr_template_item 加 dict_code 字段
3. 初始化3个字典：yes_no / meeting_form / task_status
4. 绑定附件2叶子节点的 dict_code
"""
import sys, io, psycopg2
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

DB = dict(host="119.167.165.27", port=5432, dbname="zjylzl",
          user="postgres", password="zjylzl", options="-c search_path=zjylzl")
TPL2_ID = 2_000_000_000_000_001

def run():
    conn = psycopg2.connect(**DB)
    cur  = conn.cursor()

    # ── 1. 建表 wr_dict_type ──────────────────────────────────────────────────
    cur.execute("""
        CREATE TABLE IF NOT EXISTS wr_dict_type (
            id          BIGINT       PRIMARY KEY,
            dict_code   VARCHAR(50)  NOT NULL UNIQUE,
            dict_name   VARCHAR(100) NOT NULL,
            remark      VARCHAR(255),
            status      SMALLINT     NOT NULL DEFAULT 1,
            create_time TIMESTAMP    NOT NULL DEFAULT NOW(),
            update_time TIMESTAMP    NOT NULL DEFAULT NOW(),
            del_flag    SMALLINT     NOT NULL DEFAULT 0
        )
    """)
    print("wr_dict_type 表 OK")

    # ── 2. 建表 wr_dict_item ──────────────────────────────────────────────────
    cur.execute("""
        CREATE TABLE IF NOT EXISTS wr_dict_item (
            id           BIGINT       PRIMARY KEY,
            dict_type_id BIGINT       NOT NULL,
            item_label   VARCHAR(100) NOT NULL,
            item_value   VARCHAR(100) NOT NULL,
            sort_num     INT          NOT NULL DEFAULT 0,
            status       SMALLINT     NOT NULL DEFAULT 1,
            create_time  TIMESTAMP    NOT NULL DEFAULT NOW(),
            update_time  TIMESTAMP    NOT NULL DEFAULT NOW(),
            del_flag     SMALLINT     NOT NULL DEFAULT 0
        )
    """)
    print("wr_dict_item 表 OK")

    # ── 3. ALTER wr_template_item 加 dict_code ────────────────────────────────
    cur.execute("""
        SELECT 1 FROM information_schema.columns
        WHERE table_schema='zjylzl' AND table_name='wr_template_item' AND column_name='dict_code'
    """)
    if not cur.fetchone():
        cur.execute("ALTER TABLE wr_template_item ADD COLUMN dict_code VARCHAR(50)")
        print("wr_template_item.dict_code 字段 新增 OK")
    else:
        print("wr_template_item.dict_code 字段 已存在")

    # ── 4. 初始化字典种子数据 ──────────────────────────────────────────────────
    dicts = [
        # (type_id, dict_code, dict_name, remark, items[(item_id, label, value, sort)])
        (3_000_000_000_000_001, "yes_no", "是/否", "通用是否选项", [
            (3_000_000_000_001_001, "是", "1", 1),
            (3_000_000_000_001_002, "否", "0", 2),
        ]),
        (3_000_000_000_000_002, "meeting_form", "开展形式", "线上/线下/混合", [
            (3_000_000_000_002_001, "线上",     "online",  1),
            (3_000_000_000_002_002, "线下",     "offline", 2),
            (3_000_000_000_002_003, "线上+线下", "both",    3),
        ]),
        (3_000_000_000_000_003, "task_status", "完成情况", "政府指令性任务完成状态", [
            (3_000_000_000_003_001, "已完成",  "done",        1),
            (3_000_000_000_003_002, "进行中",  "in_progress", 2),
            (3_000_000_000_003_003, "未完成",  "not_done",    3),
        ]),
    ]

    for tid, code, name, remark, items in dicts:
        cur.execute("SELECT id FROM wr_dict_type WHERE dict_code=%s", (code,))
        if not cur.fetchone():
            cur.execute("""
                INSERT INTO wr_dict_type(id,dict_code,dict_name,remark,status,del_flag)
                VALUES(%s,%s,%s,%s,1,0)
            """, (tid, code, name, remark))
            print(f"新增字典类型: {code} ({name})")
            for iid, label, value, sort in items:
                cur.execute("""
                    INSERT INTO wr_dict_item(id,dict_type_id,item_label,item_value,sort_num,status,del_flag)
                    VALUES(%s,%s,%s,%s,%s,1,0)
                """, (iid, tid, label, value, sort))
            print(f"  ↳ 写入 {len(items)} 条选项")
        else:
            print(f"字典 {code} 已存在，跳过")

    # ── 5. 绑定附件2叶子节点的 dict_code ──────────────────────────────────────
    # yes_no 字段：是否为国家级质控中心 / 是否有专职人员 / 是否有专用场所 / 是否需要跨专业协同
    yes_no_names = [
        "是否为国家级质控中心",
        "是否有专职人员",
        "是否有专用场所",
        "是否需要跨专业协同（请简要说明）",
    ]
    cur.execute("""
        UPDATE wr_template_item SET dict_code='yes_no'
        WHERE template_id=%s AND is_leaf=1 AND del_flag=0
          AND item_name = ANY(%s)
    """, (TPL2_ID, yes_no_names))
    print(f"\n绑定 yes_no：{cur.rowcount} 个字段")

    # meeting_form：开展形式（线上/线下）
    cur.execute("""
        UPDATE wr_template_item SET dict_code='meeting_form'
        WHERE template_id=%s AND is_leaf=1 AND del_flag=0
          AND item_name='开展形式（线上/线下）'
    """, (TPL2_ID,))
    print(f"绑定 meeting_form：{cur.rowcount} 个字段")

    # task_status：完成情况
    cur.execute("""
        UPDATE wr_template_item SET dict_code='task_status'
        WHERE template_id=%s AND is_leaf=1 AND del_flag=0
          AND item_name='完成情况'
    """, (TPL2_ID,))
    print(f"绑定 task_status：{cur.rowcount} 个字段")

    conn.commit()

    # ── 验证 ──────────────────────────────────────────────────────────────────
    cur.execute("""
        SELECT item_name, dict_code FROM wr_template_item
        WHERE template_id=%s AND is_leaf=1 AND del_flag=0 AND dict_code IS NOT NULL
        ORDER BY sort_num
    """, (TPL2_ID,))
    print("\n附件2 已绑定字典的叶子节点：")
    for r in cur.fetchall():
        print(f"  [{r[0]}] → {r[1]}")

    conn.close()
    print("\n完成！")

if __name__ == "__main__":
    run()
