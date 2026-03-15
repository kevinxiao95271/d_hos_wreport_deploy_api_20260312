"""
init_templates.py
-----------------
初始化附件2、附件3两个模板的全量数据：
  - wr_template       (模板基本信息)
  - wr_template_item  (列定义，含三级表头)
  - wr_template_row   (行定义，附件3专用)
  - wr_task           (两个任务，deadline=2026-05-31)

运行前确保后端已启动（DDL 已建表），或直接连数据库执行。
用法：python scripts/init_templates.py
"""
import sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

import psycopg2, time, random

DB = dict(
    host="119.167.165.27", port=5432, dbname="zjylzl",
    user="postgres", password="zjylzl",
    options="-c search_path=zjylzl"
)

# ── Snowflake-like ID ──────────────────────────────────────────────────────────
_seq = 0
def next_id():
    global _seq
    ts = int(time.time() * 1000) & 0x1FFFFFFFFFF   # 41-bit ms
    mid = 1                                          # machine
    _seq = (_seq + 1) & 0xFFF
    return (ts << 22) | (mid << 12) | _seq

# ── IDs（固定，方便重复运行幂等判断）──────────────────────────────────────────
TPL2_ID   = 2_000_000_000_000_001
TPL3_ID   = 2_000_000_000_000_002
TASK2_ID  = 3_000_000_000_000_001
TASK3_ID  = 3_000_000_000_000_002

def run():
    conn = psycopg2.connect(**DB)
    cur  = conn.cursor()

    # ── 清理旧数据（幂等）────────────────────────────────────────────────────
    for tbl_id in [TPL2_ID, TPL3_ID]:
        cur.execute("DELETE FROM wr_template_item WHERE template_id=%s", (tbl_id,))
        cur.execute("DELETE FROM wr_template_row  WHERE template_id=%s", (tbl_id,))
    for t_id in [TASK2_ID, TASK3_ID]:
        cur.execute("DELETE FROM wr_task WHERE id=%s", (t_id,))
    cur.execute("DELETE FROM wr_template WHERE id IN (%s,%s)", (TPL2_ID, TPL3_ID))

    # ══════════════════════════════════════════════════════════════════════════
    # 附件2：质控工作开展情况统计表（三级表头，48列，每机构填一行）
    # ══════════════════════════════════════════════════════════════════════════
    cur.execute("""
        INSERT INTO wr_template(id,template_name,description,status,del_flag)
        VALUES(%s,%s,%s,1,0)
    """, (TPL2_ID,
          "附件2：年度质控工作开展情况统计表",
          "2025年度省级质控中心/技术指导中心工作开展情况统计表，三级表头，每机构填一行"))

    # (L1_name, L2_name, L3_name, col_index, value_type)
    # None 表示该层不存在（直接是叶子）
    # L1 col_span 和 row_span 会在后面按 group 计算
    items_tpl2 = [
        # L1=国家质控
        ("国家质控", "是否为国家级质控中心", None,           1,  "text"),
        ("国家质控", "国家考核情况",         "2024年",       2,  "text"),
        ("国家质控", "国家考核情况",         "2025年",       3,  "text"),
        # L1=质控工作会议
        ("质控工作会议", "质控核心委员会议", "线上次数",     4,  "number"),
        ("质控工作会议", "质控核心委员会议", "参会人数(线上)",5, "number"),
        ("质控工作会议", "质控核心委员会议", "线下次数",     6,  "number"),
        ("质控工作会议", "质控核心委员会议", "参会人数(线下)",7, "number"),
        ("质控工作会议", "质控委员扩大会议", "线上次数",     8,  "number"),
        ("质控工作会议", "质控委员扩大会议", "参会人数(线上)",9, "number"),
        ("质控工作会议", "质控委员扩大会议", "线下次数",    10,  "number"),
        ("质控工作会议", "质控委员扩大会议", "参会人数(线下)",11,"number"),
        # L1=质控培训
        ("质控培训", "线上培训", "次数",      12, "number"),
        ("质控培训", "线上培训", "参会人数",   13, "number"),
        ("质控培训", "线下培训", "次数",      14, "number"),
        ("质控培训", "线下培训", "参会人数",   15, "number"),
        # L1=质控指导（技术指导）
        ("质控指导（技术指导）", "对市级质控中心指导", "指导时间（月份）", 16, "text"),
        ("质控指导（技术指导）", "对市级质控中心指导", "线上指导次数",    17, "number"),
        ("质控指导（技术指导）", "对市级质控中心指导", "线下指导次数",    18, "number"),
        ("质控指导（技术指导）", "对市级质控中心指导", "指导内容",       19, "text"),
        ("质控指导（技术指导）", "对医疗机构指导",    "指导时间（月份）", 20, "text"),
        ("质控指导（技术指导）", "对医疗机构指导",    "线上指导次数",    21, "number"),
        ("质控指导（技术指导）", "对医疗机构指导",    "线下指导次数",    22, "number"),
        ("质控指导（技术指导）", "对医疗机构指导",    "指导内容",       23, "text"),
        # L1=质控调研
        ("质控调研（请附调研报告）", "专项调研",   "开展内容",          24, "text"),
        ("质控调研（请附调研报告）", "专项调研",   "开展形式（线上/线下）",25,"text"),
        ("质控调研（请附调研报告）", "行政委托全省调研", "开展内容",    26, "text"),
        # L1=技能竞赛
        ("技能竞赛", "注明赛事级别（省总工会）", None,       27, "text"),
        # L1=制度修订
        ("中心内部管理制度修订情况", "中心已有的制度目录",       None, 28, "text"),
        ("中心内部管理制度修订情况", "2025年修订完善的制度目录", None, 29, "text"),
        # L1=主编/参与
        ("2025年主编/参与编写", "规范",  None, 30, "text"),
        ("2025年主编/参与编写", "丛书",  None, 31, "text"),
        ("2025年主编/参与编写", "指南",  None, 32, "text"),
        ("2025年主编/参与编写", "共识",  None, 33, "text"),
        ("2025年主编/参与编写", "标准",  None, 34, "text"),
        # L1=数据分析报告
        ("撰写年度数据分析报告", None, None, 35, "text"),
        # L1=课题论文
        ("以质控名义申报课题/论文", "申报课题（注明课题信息）", None, 36, "text"),
        ("以质控名义申报课题/论文", "撰写论文（注明题目及状态）", None, 37, "text"),
        # L1=政府指令任务
        ("政府指令性任务情况", "项目名称",  None, 38, "text"),
        ("政府指令性任务情况", "完成情况",  None, 39, "text"),
        # L1=经费执行
        ("经费执行情况", "财政拨付经费（万元）", "拨付金额（万元）", 40, "number"),
        ("经费执行情况", "财政拨付经费（万元）", "执行率（%）",      41, "number"),
        ("经费执行情况", "挂靠单位配套经费",     "配套金额（万元）", 42, "number"),
        ("经费执行情况", "挂靠单位配套经费",     "执行率（%）",      43, "number"),
        # L1=专职人员及场所
        ("专职人员及专用场所", "是否有专职人员", None, 44, "text"),
        ("专职人员及专用场所", "是否有专用场所", None, 45, "text"),
        # L1=跨专业协同
        ("质控工作需跨专业协同", "是否需要跨专业协同（请简要说明）", None, 46, "text"),
        ("质控工作需跨专业协同", "所需配合的相关专业",             None, 47, "text"),
        # L1=其他
        ("其他", "对质控管理工作的意见和建议", None, 48, "text"),
    ]

    # 建立 L1 → id 映射
    l1_ids = {}
    l2_ids = {}  # (l1_name, l2_name) → id

    def get_or_create_l1(cur, l1_name, col_start):
        if l1_name not in l1_ids:
            nid = next_id()
            l1_ids[l1_name] = (nid, col_start)
        return l1_ids[l1_name][0]

    def get_or_create_l2(cur, l1_id, l1_name, l2_name, col_start):
        key = (l1_name, l2_name)
        if key not in l2_ids:
            nid = next_id()
            l2_ids[key] = (nid, col_start)
        return l2_ids[key][0]

    # 先扫一遍确定 L1/L2 col_start
    for (l1, l2, l3, col, vtype) in items_tpl2:
        get_or_create_l1(cur, l1, col)
        if l2:
            get_or_create_l2(cur, l1_ids.get(l1, (None,None))[0], l1, l2, col)

    # 计算 col_span
    from collections import Counter
    l1_colspans = Counter()
    l2_colspans = Counter()
    for (l1, l2, l3, col, vtype) in items_tpl2:
        l1_colspans[l1] += 1
        if l2:
            l2_colspans[(l1, l2)] += 1

    # 插入 L1 节点
    for l1_name, (nid, col_start) in l1_ids.items():
        # 是否该L1只有一个叶子且L2=None → row_span=3(占满三行)
        all_leaves = [x for x in items_tpl2 if x[0] == l1_name]
        has_l2 = any(x[1] for x in all_leaves)
        has_l3 = any(x[2] for x in all_leaves)
        if not has_l2:
            row_span = 3; is_leaf = 1
        else:
            row_span = 1; is_leaf = 0
        cur.execute("""
            INSERT INTO wr_template_item
              (id,template_id,parent_id,item_name,header_row,col_index,row_span,col_span,is_leaf,value_type,sort_num,del_flag)
            VALUES(%s,%s,NULL,%s,1,%s,%s,%s,%s,'text',%s,0)
        """, (nid, TPL2_ID, l1_name, col_start, row_span, l1_colspans[l1_name], is_leaf, col_start))

    # 插入 L2 节点
    for (l1_name, l2_name), (nid, col_start) in l2_ids.items():
        l1_id = l1_ids[l1_name][0]
        all_l2_leaves = [x for x in items_tpl2 if x[0]==l1_name and x[1]==l2_name]
        has_l3 = any(x[2] for x in all_l2_leaves)
        if not has_l3:
            row_span = 2; is_leaf = 1
        else:
            row_span = 1; is_leaf = 0
        cur.execute("""
            INSERT INTO wr_template_item
              (id,template_id,parent_id,item_name,header_row,col_index,row_span,col_span,is_leaf,value_type,sort_num,del_flag)
            VALUES(%s,%s,%s,%s,2,%s,%s,%s,%s,'text',%s,0)
        """, (nid, TPL2_ID, l1_id, l2_name, col_start, row_span, l2_colspans[(l1_name,l2_name)], is_leaf, col_start))

    # 插入 L3 叶子节点
    for (l1_name, l2_name, l3_name, col, vtype) in items_tpl2:
        if l3_name is None:
            continue  # L1或L2直接是叶子，已处理
        l2_id = l2_ids.get((l1_name, l2_name), (None,None))[0]
        if l2_id is None:
            continue
        cur.execute("""
            INSERT INTO wr_template_item
              (id,template_id,parent_id,item_name,header_row,col_index,row_span,col_span,is_leaf,value_type,sort_num,del_flag)
            VALUES(%s,%s,%s,%s,3,%s,1,1,1,%s,%s,0)
        """, (next_id(), TPL2_ID, l2_id, l3_name, col, vtype, col))

    print(f"[附件2] 模板 id={TPL2_ID} 写入完成，共 {len(items_tpl2)} 列")

    # ══════════════════════════════════════════════════════════════════════════
    # 附件3：省市县质控中心设立情况统计表（checkbox矩阵，1列，~120行）
    # 列只有1个"已成立"——模板只定义结构，不硬编码机构列表。
    # 每个机构提交自己的1条 record，用 wr_record.org_name 区分是谁填的。
    # ══════════════════════════════════════════════════════════════════════════
    TPL3_ITEM_ID    = 2_000_000_000_100_001   # 已成立（checkbox）
    TPL3_ITEM_CITY  = 2_000_000_000_100_002   # 市级成立个数（number）
    TPL3_ITEM_CTY   = 2_000_000_000_100_003   # 县级成立个数（number）

    cur.execute("""
        INSERT INTO wr_template(id,template_name,description,status,del_flag)
        VALUES(%s,%s,%s,1,0)
    """, (TPL3_ID,
          "附件3：省市县质控中心设立情况统计表",
          "省市县质控中心/技术指导中心设立情况统计，每格为勾选（已成立=1）"))

    # 3 个独立的 item：
    #   已成立     - checkbox，矩阵勾选列
    #   市级成立个数 - number，独立数字填报项（不参与树联动）
    #   县级成立个数 - number，独立数字填报项（不参与树联动）
    for iid, name, vtype, col, srt in [
        (TPL3_ITEM_ID,   "已成立",     "checkbox", 1, 1),
        (TPL3_ITEM_CITY, "市级成立个数","number",   2, 2),
        (TPL3_ITEM_CTY,  "县级成立个数","number",   3, 3),
    ]:
        cur.execute("""
            INSERT INTO wr_template_item
              (id,template_id,parent_id,item_name,header_row,col_index,row_span,col_span,is_leaf,value_type,sort_num,del_flag)
            VALUES(%s,%s,NULL,%s,1,%s,1,1,1,%s,%s,0)
        """, (iid, TPL3_ID, name, col, vtype, srt))

    print(f"[附件3] 模板 id={TPL3_ID} 列定义写入完成，共3项（已成立/市级成立个数/县级成立个数）")

    # 行定义说明：
    #   - 行 1/2 为省级汇总勾选行（无父节点，无子节点，独立勾选）
    #   - 行 4-14 为各市"市级"勾选行（level=2，无父行，前端可按 level 分组展示）
    #   - 行 16/30/... 为市级行，其下县区为 level=3 子节点（参与树联动）
    #   - 原"市级成立个数"(row3) 和"县级成立个数"(row15) 已移为独立 item，此处不再定义为行
    rows_tpl3 = [
        (1,  "省市县全部成立",  1, None),
        (2,  "市级全部成立",    1, None),
        (4,  "杭州市级",       2, None),
        (5,  "宁波市级",       2, None),
        (6,  "温州市级",       2, None),
        (7,  "湖州市级",       2, None),
        (8,  "嘉兴市级",       2, None),
        (9,  "绍兴市级",       2, None),
        (10, "金华市级",       2, None),
        (11, "衢州市级",       2, None),
        (12, "舟山市级",       2, None),
        (13, "台州市级",       2, None),
        (14, "丽水市级",       2, None),
        # 杭州市
        (16, "杭州市",        2, None),
        (17, "上城区",        3, 16),
        (18, "拱墅区",        3, 16),
        (19, "西湖区",        3, 16),
        (20, "滨江区",        3, 16),
        (21, "萧山区",        3, 16),
        (22, "余杭区",        3, 16),
        (23, "临平区",        3, 16),
        (24, "钱塘区",        3, 16),
        (25, "富阳区",        3, 16),
        (26, "临安区",        3, 16),
        (27, "桐庐区",        3, 16),
        (28, "淳安县",        3, 16),
        (29, "建德市",        3, 16),
        # 宁波市
        (30, "宁波市",        2, None),
        (31, "海曙区",        3, 30),
        (32, "江北区",        3, 30),
        (33, "镇海区",        3, 30),
        (34, "北仑区",        3, 30),
        (35, "鄞州区",        3, 30),
        (36, "奉化区",        3, 30),
        (37, "余姚市",        3, 30),
        (38, "慈溪市",        3, 30),
        (39, "宁海县",        3, 30),
        (40, "象山县",        3, 30),
        # 温州市
        (41, "温州市",        2, None),
        (42, "鹿城区",        3, 41),
        (43, "龙湾区",        3, 41),
        (44, "瓯海区",        3, 41),
        (45, "洞头区",        3, 41),
        (46, "乐清市",        3, 41),
        (47, "瑞安市",        3, 41),
        (48, "永嘉县",        3, 41),
        (49, "文成县",        3, 41),
        (50, "平阳县",        3, 41),
        (51, "泰顺县",        3, 41),
        (52, "苍南县",        3, 41),
        (53, "龙港市",        3, 41),
        # 湖州市
        (54, "湖州市",        2, None),
        (55, "吴兴区",        3, 54),
        (56, "南浔区",        3, 54),
        (57, "德清县",        3, 54),
        (58, "长兴县",        3, 54),
        (59, "安吉县",        3, 54),
        # 嘉兴市
        (60, "嘉兴市",        2, None),
        (61, "南湖区",        3, 60),
        (62, "秀洲区",        3, 60),
        (63, "嘉善县",        3, 60),
        (64, "平湖市",        3, 60),
        (65, "海盐县",        3, 60),
        (66, "海宁市",        3, 60),
        (67, "桐乡市",        3, 60),
        # 绍兴市
        (68, "绍兴市",        2, None),
        (69, "越城区",        3, 68),
        (70, "柯桥区",        3, 68),
        (71, "上虞区",        3, 68),
        (72, "诸暨市",        3, 68),
        (73, "嵊州市",        3, 68),
        (74, "新昌县",        3, 68),
        # 金华市
        (75, "金华市",        2, None),
        (76, "婺城区",        3, 75),
        (77, "金东区",        3, 75),
        (78, "兰溪市",        3, 75),
        (79, "东阳市",        3, 75),
        (80, "义乌市",        3, 75),
        (81, "永康市",        3, 75),
        (82, "浦江县",        3, 75),
        (83, "武义县",        3, 75),
        (84, "磐安县",        3, 75),
        # 衢州市
        (85, "衢州市",        2, None),
        (86, "柯城区",        3, 85),
        (87, "衢江区",        3, 85),
        (88, "龙游县",        3, 85),
        (89, "江山市",        3, 85),
        (90, "常山县",        3, 85),
        (91, "开化县",        3, 85),
        # 舟山市
        (92, "舟山市",        2, None),
        (93, "定海区",        3, 92),
        (94, "普陀区",        3, 92),
        (95, "岱山县",        3, 92),
        (96, "嵊泗县",        3, 92),
        # 台州市
        (97,  "台州市",       2, None),
        (98,  "椒江区",       3, 97),
        (99,  "黄岩区",       3, 97),
        (100, "路桥区",       3, 97),
        (101, "临海市",       3, 97),
        (102, "温岭市",       3, 97),
        (103, "玉环市",       3, 97),
        (104, "天台县",       3, 97),
        (105, "仙居县",       3, 97),
        (106, "三门县",       3, 97),
        # 丽水市
        (107, "丽水市",       2, None),
        (108, "莲都区",       3, 107),
        (109, "龙泉市",       3, 107),
        (110, "青田县",       3, 107),
        (111, "云和县",       3, 107),
        (112, "庆元县",       3, 107),
        (113, "缙云县",       3, 107),
        (114, "遂昌县",       3, 107),
        (115, "松阳县",       3, 107),
        (116, "景宁县",       3, 107),
    ]

    for sort_num, (row_index, row_label, row_level, parent_row_index) in enumerate(rows_tpl3):
        cur.execute("""
            INSERT INTO wr_template_row
              (id,template_id,row_index,row_label,row_level,parent_row_index,sort_num,del_flag)
            VALUES(%s,%s,%s,%s,%s,%s,%s,0)
        """, (next_id(), TPL3_ID, row_index, row_label, row_level, parent_row_index, sort_num))

    print(f"[附件3] 行定义写入完成，共 {len(rows_tpl3)} 行")

    # ══════════════════════════════════════════════════════════════════════════
    # 创建任务（deadline 2026-05-31）
    # ══════════════════════════════════════════════════════════════════════════
    cur.execute("""
        INSERT INTO wr_task(id,task_name,template_id,stat_year,deadline,status,remark,del_flag)
        VALUES(%s,%s,%s,'2025','2026-05-31 23:59:59',1,'附件2测试任务',0)
    """, (TASK2_ID, "2025年度质控工作开展情况填报", TPL2_ID))

    cur.execute("""
        INSERT INTO wr_task(id,task_name,template_id,stat_year,deadline,status,remark,del_flag)
        VALUES(%s,%s,%s,'2025','2026-05-31 23:59:59',1,'附件3测试任务',0)
    """, (TASK3_ID, "2025年省市县质控中心设立情况填报", TPL3_ID))

    print(f"[任务] task_id={TASK2_ID} (附件2), task_id={TASK3_ID} (附件3) 写入完成")

    conn.commit()
    cur.close()
    conn.close()
    print("\n✅ 全部初始化完成！")

if __name__ == "__main__":
    run()
