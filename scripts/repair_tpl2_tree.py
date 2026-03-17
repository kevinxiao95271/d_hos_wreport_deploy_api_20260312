"""
repair_tpl2_tree.py
-------------------
仅修复 wr_template_item 中 附件2 (template_id=2000000000000001) 的 parent_id。
不删除、不重建节点，不修改任何其他字段（dictCode、itemName、valueType 等均不变）。
原有 wr_record_value 所引用的 itemId 全部保留。
"""
import psycopg2, sys
sys.stdout.reconfigure(encoding='utf-8')

DB = dict(host='119.167.165.27', port=5432, dbname='zjylzl',
          user='postgres', password='zjylzl', options='-c search_path=zjylzl')
TPL2_ID = 2_000_000_000_000_001

# ── 附件2 的完整三级结构定义（与 init_templates.py 一致）──────────────────────
items_tpl2 = [
    ("国家质控", "是否为国家级质控中心",          None,                           1, "text"),
    ("国家质控", "国家考核情况",                  "2024年",                        2, "text"),
    ("国家质控", "国家考核情况",                  "2025年",                        3, "text"),
    ("质控工作会议", "质控核心委员会议",           "线上次数",                      4, "number"),
    ("质控工作会议", "质控核心委员会议",           "参会人数(线上)",                 5, "number"),
    ("质控工作会议", "质控核心委员会议",           "线下次数",                      6, "number"),
    ("质控工作会议", "质控核心委员会议",           "参会人数(线下)",                 7, "number"),
    ("质控工作会议", "质控委员扩大会议",           "线上次数",                      8, "number"),
    ("质控工作会议", "质控委员扩大会议",           "参会人数(线上)",                 9, "number"),
    ("质控工作会议", "质控委员扩大会议",           "线下次数",                     10, "number"),
    ("质控工作会议", "质控委员扩大会议",           "参会人数(线下)",                11, "number"),
    ("质控培训", "线上培训",                      "次数",                         12, "number"),
    ("质控培训", "线上培训",                      "参会人数",                     13, "number"),
    ("质控培训", "线下培训",                      "次数",                         14, "number"),
    ("质控培训", "线下培训",                      "参会人数",                     15, "number"),
    ("质控指导（技术指导）", "对市级质控中心指导", "指导时间（月份）",              16, "text"),
    ("质控指导（技术指导）", "对市级质控中心指导", "线上指导次数",                 17, "number"),
    ("质控指导（技术指导）", "对市级质控中心指导", "线下指导次数",                 18, "number"),
    ("质控指导（技术指导）", "对市级质控中心指导", "指导内容",                     19, "text"),
    ("质控指导（技术指导）", "对医疗机构指导",     "指导时间（月份）",              20, "text"),
    ("质控指导（技术指导）", "对医疗机构指导",     "线上指导次数",                 21, "number"),
    ("质控指导（技术指导）", "对医疗机构指导",     "线下指导次数",                 22, "number"),
    ("质控指导（技术指导）", "对医疗机构指导",     "指导内容",                     23, "text"),
    ("质控调研（请附调研报告）", "专项调研",       "开展内容",                     24, "text"),
    ("质控调研（请附调研报告）", "专项调研",       "开展形式（线上/线下）",         25, "text"),
    ("质控调研（请附调研报告）", "行政委托全省调研","开展内容",                     26, "text"),
    ("技能竞赛", "注明赛事级别（省总工会）",       None,                           27, "text"),
    ("中心内部管理制度修订情况", "中心已有的制度目录",        None,               28, "text"),
    ("中心内部管理制度修订情况", "2025年修订完善的制度目录",  None,               29, "text"),
    ("2025年主编/参与编写", "规范",               None,                           30, "text"),
    ("2025年主编/参与编写", "丛书",               None,                           31, "text"),
    ("2025年主编/参与编写", "指南",               None,                           32, "text"),
    ("2025年主编/参与编写", "共识",               None,                           33, "text"),
    ("2025年主编/参与编写", "标准",               None,                           34, "text"),
    ("撰写年度数据分析报告", None,                None,                           35, "text"),
    ("以质控名义申报课题/论文", "申报课题（注明课题信息）",    None,              36, "text"),
    ("以质控名义申报课题/论文", "撰写论文（注明题目及状态）",  None,              37, "text"),
    ("政府指令性任务情况", "项目名称",             None,                           38, "text"),
    ("政府指令性任务情况", "完成情况",             None,                           39, "text"),
    ("经费执行情况", "财政拨付经费（万元）",       "拨付金额（万元）",              40, "number"),
    ("经费执行情况", "财政拨付经费（万元）",       "执行率（%）",                  41, "number"),
    ("经费执行情况", "挂靠单位配套经费",           "配套金额（万元）",              42, "number"),
    ("经费执行情况", "挂靠单位配套经费",           "执行率（%）",                  43, "number"),
    ("专职人员及专用场所", "是否有专职人员",       None,                           44, "text"),
    ("专职人员及专用场所", "是否有专用场所",       None,                           45, "text"),
    ("质控工作需跨专业协同", "是否需要跨专业协同（请简要说明）", None,            46, "text"),
    ("质控工作需跨专业协同", "所需配合的相关专业",  None,                          47, "text"),
    ("其他", "对质控管理工作的意见和建议",         None,                           48, "text"),
]

# ── 根据 col_index 范围推算父子关系 ─────────────────────────────────────────
l1_cols = {}  # l1_name → [col, ...]
l2_cols = {}  # (l1_name, l2_name) → [col, ...]

for (l1, l2, l3, col, _) in items_tpl2:
    l1_cols.setdefault(l1, []).append(col)
    if l2:
        l2_cols.setdefault((l1, l2), []).append(col)

l1_range = {k: (min(v), max(v)) for k, v in l1_cols.items()}
l2_range = {k: (min(v), max(v)) for k, v in l2_cols.items()}

def find_l1(col):
    """根据 col_index 找所属 L1 名称"""
    for name, (mn, mx) in l1_range.items():
        if mn <= col <= mx:
            return name
    return None

def find_l2(col):
    """根据 col_index 找所属 L2 (l1_name, l2_name) key"""
    for key, (mn, mx) in l2_range.items():
        if mn <= col <= mx:
            return key
    return None

# ── 读取当前节点 ──────────────────────────────────────────────────────────────
conn = psycopg2.connect(**DB)
conn.autocommit = False
cur = conn.cursor()

cur.execute("""
    SELECT id, item_name, col_index, header_row, is_leaf, row_span, parent_id
    FROM wr_template_item
    WHERE template_id = %s AND del_flag = 0
    ORDER BY header_row, col_index
""", (TPL2_ID,))
nodes = cur.fetchall()
print(f"当前节点总数: {len(nodes)}")

# id 索引
by_id   = {r[0]: r for r in nodes}
# (header_row, item_name, col_index) → id  —— 用于精确定位
by_key  = {}
for r in nodes:
    key = (r[3], r[1], r[2])  # (header_row, item_name, col_index)
    by_key[key] = r[0]

# L1 name → node id
l1_name_to_id = {}
for r in nodes:
    if r[3] == 1:  # header_row=1
        l1_name_to_id[r[1]] = r[0]

# (L1_name, L2_name) → node id （通过 L2 节点的 col_index 匹配）
l2_key_to_id = {}
for r in nodes:
    if r[3] == 2:  # header_row=2
        col = r[2]
        l1_name = find_l1(col)
        if l1_name:
            l2_key_to_id[(l1_name, r[1])] = r[0]

# ── 计算每个节点应有的 parent_id ─────────────────────────────────────────────
updates = []  # (new_parent_id, node_id)
errors  = []

for r in nodes:
    node_id, item_name, col_index, header_row, is_leaf, row_span, cur_parent = r

    if header_row == 1:
        expected_parent = None
    elif header_row == 2:
        l1_name = find_l1(col_index)
        if l1_name is None:
            errors.append(f"L2 节点找不到 L1: id={node_id} name={item_name} col={col_index}")
            continue
        expected_parent = l1_name_to_id.get(l1_name)
        if expected_parent is None:
            errors.append(f"L2 找不到 L1 节点: l1_name={l1_name} id={node_id}")
            continue
    elif header_row == 3:
        l2_key = find_l2(col_index)
        if l2_key is None:
            errors.append(f"L3 节点找不到 L2: id={node_id} name={item_name} col={col_index}")
            continue
        expected_parent = l2_key_to_id.get(l2_key)
        if expected_parent is None:
            errors.append(f"L3 找不到 L2 节点: l2_key={l2_key} id={node_id}")
            continue
    else:
        errors.append(f"未知 header_row={header_row} id={node_id}")
        continue

    if cur_parent != expected_parent:
        updates.append((expected_parent, node_id, item_name, header_row, cur_parent))

print(f"\n需要修复的节点: {len(updates)} 个")
print(f"已正确的节点:   {len(nodes) - len(updates) - len(errors)} 个")

if errors:
    print(f"\n⚠ 错误 ({len(errors)} 个):")
    for e in errors:
        print(f"  {e}")
    print("中止，不修改数据库")
    conn.rollback()
    conn.close()
    sys.exit(1)

# ── 预览前10条变更 ─────────────────────────────────────────────────────────
print("\n前10条变更预览:")
for (new_p, nid, name, hr, old_p) in updates[:10]:
    print(f"  [{name}] header_row={hr}  old_parent={old_p} → new_parent={new_p}")

# ── 执行 UPDATE ───────────────────────────────────────────────────────────────
print(f"\n执行 UPDATE parent_id ...")
for (new_p, nid, name, hr, old_p) in updates:
    cur.execute("UPDATE wr_template_item SET parent_id=%s WHERE id=%s", (new_p, nid))

conn.commit()
print(f"✅ 修复完成，共更新 {len(updates)} 条记录")

# ── 验证 ─────────────────────────────────────────────────────────────────────
cur.execute("""
    SELECT COUNT(*) FROM wr_template_item
    WHERE template_id=%s AND del_flag=0 AND parent_id IS NULL
""", (TPL2_ID,))
null_count = cur.fetchone()[0]
print(f"\n验证: parent_id=null 的节点数 = {null_count}（应为 L1 节点数）")

cur.execute("""
    SELECT COUNT(*) FROM wr_template_item
    WHERE template_id=%s AND del_flag=0 AND header_row=1
""", (TPL2_ID,))
l1_count = cur.fetchone()[0]
print(f"        L1节点数(header_row=1)       = {l1_count}")
print("✓ 一致" if null_count == l1_count else "✗ 不一致，请检查")

conn.close()
