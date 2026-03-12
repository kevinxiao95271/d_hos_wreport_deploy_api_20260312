-- =====================================================
-- 日常工作管理模块 - 测试数据
-- 包含: 模板(含两层表头) + 任务 + 上报记录 + 数据值
-- 前提: wr_module_ddl.sql 已执行, 测试账号已创建
-- =====================================================

SET search_path TO zjylzl;

-- =====================================================
-- 一、模板
-- =====================================================
INSERT INTO wr_template (id, template_name, description, status, create_user, create_time)
VALUES (
    1000000000000000001,
    '2025年度省级质控中心/技术指导中心工作开展情况统计表',
    '填报范围：省级质控中心及省级技术指导中心；请如实填报本年度工作开展情况。',
    1,
    8000000000000010001,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 二、模板表头节点（两层表头示例）
--
-- 可视化表格结构:
-- ┌────────────────────────┬──────────────────────────────┬──────────────────────────────┐
-- │     国家质控 (跨2列)    │    质控工作会议 (跨2列)        │   质控调研（请附调研报告）(跨2列)│
-- ├────────────┬───────────┼──────────────┬───────────────┼──────────────┬───────────────┤
-- │是否为国家级 │ 质量评级  │  召开次数     │   参与人数     │   调研次数   │  调研覆盖机构数 │
-- └────────────┴───────────┴──────────────┴───────────────┴──────────────┴───────────────┘
-- 还有单独两列: 质控人员配置 / 质控工作年度亮点 (单层，跨两行)
-- =====================================================

-- ---- 父节点（is_leaf=0，不填报，只作分组展示）----

-- 父节点1: 国家质控 (跨2列)
INSERT INTO wr_template_item
    (id, template_id, parent_id, item_name, header_row, col_index, row_span, col_span, is_leaf, sort_num, create_time)
VALUES (1000000000000000010, 1000000000000000001, NULL, '国家质控', 1, 1, 1, 2, 0, 1, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 父节点2: 质控工作会议 (跨2列)
INSERT INTO wr_template_item
    (id, template_id, parent_id, item_name, header_row, col_index, row_span, col_span, is_leaf, sort_num, create_time)
VALUES (1000000000000000020, 1000000000000000001, NULL, '质控工作会议', 1, 3, 1, 2, 0, 2, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 父节点3: 质控指导（技术指导）(跨2列)
INSERT INTO wr_template_item
    (id, template_id, parent_id, item_name, header_row, col_index, row_span, col_span, is_leaf, sort_num, create_time)
VALUES (1000000000000000030, 1000000000000000001, NULL, '质控指导（技术指导）', 1, 5, 1, 2, 0, 3, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 父节点4: 质控培训 (跨2列)
INSERT INTO wr_template_item
    (id, template_id, parent_id, item_name, header_row, col_index, row_span, col_span, is_leaf, sort_num, create_time)
VALUES (1000000000000000040, 1000000000000000001, NULL, '质控培训', 1, 7, 1, 2, 0, 4, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 父节点5: 质控调研（请附调研报告）(跨2列) - requireAttachment=1(必须上传)
INSERT INTO wr_template_item
    (id, template_id, parent_id, item_name, header_row, col_index, row_span, col_span, is_leaf,
     require_attachment, placeholder, sort_num, create_time)
VALUES (1000000000000000050, 1000000000000000001, NULL,
        '质控调研（请附调研报告）', 1, 9, 1, 2, 0,
        1, '请在填写后，在附件区上传调研报告（见格式模板）', 5, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- ---- 叶子节点（is_leaf=1，实际填报）----

-- 国家质控 子节点
INSERT INTO wr_template_item
    (id, template_id, parent_id, item_name, header_row, col_index, row_span, col_span, is_leaf, value_type, sort_num, create_time)
VALUES
(1000000000000000011, 1000000000000000001, 1000000000000000010, '是否为国家级质控中心', 2, 1, 1, 1, 1, 'select', 11, CURRENT_TIMESTAMP),
(1000000000000000012, 1000000000000000001, 1000000000000000010, '质量评级',             2, 2, 1, 1, 1, 'select', 12, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 质控工作会议 子节点
INSERT INTO wr_template_item
    (id, template_id, parent_id, item_name, header_row, col_index, row_span, col_span, is_leaf, value_type, unit, sort_num, create_time)
VALUES
(1000000000000000021, 1000000000000000001, 1000000000000000020, '召开次数', 2, 3, 1, 1, 1, 'number', '次', 21, CURRENT_TIMESTAMP),
(1000000000000000022, 1000000000000000001, 1000000000000000020, '参与人数', 2, 4, 1, 1, 1, 'number', '人', 22, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 质控指导（技术指导）子节点
INSERT INTO wr_template_item
    (id, template_id, parent_id, item_name, header_row, col_index, row_span, col_span, is_leaf, value_type, unit, sort_num, create_time)
VALUES
(1000000000000000031, 1000000000000000001, 1000000000000000030, '指导次数', 2, 5, 1, 1, 1, 'number', '次', 31, CURRENT_TIMESTAMP),
(1000000000000000032, 1000000000000000001, 1000000000000000030, '参与人数', 2, 6, 1, 1, 1, 'number', '人', 32, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 质控培训 子节点
INSERT INTO wr_template_item
    (id, template_id, parent_id, item_name, header_row, col_index, row_span, col_span, is_leaf, value_type, unit, sort_num, create_time)
VALUES
(1000000000000000041, 1000000000000000001, 1000000000000000040, '培训次数', 2, 7, 1, 1, 1, 'number', '次', 41, CURRENT_TIMESTAMP),
(1000000000000000042, 1000000000000000001, 1000000000000000040, '参训人次', 2, 8, 1, 1, 1, 'number', '人次', 42, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 质控调研 子节点
INSERT INTO wr_template_item
    (id, template_id, parent_id, item_name, header_row, col_index, row_span, col_span, is_leaf, value_type, unit, sort_num, create_time)
VALUES
(1000000000000000051, 1000000000000000001, 1000000000000000050, '调研次数',       2, 9,  1, 1, 1, 'number', '次',  51, CURRENT_TIMESTAMP),
(1000000000000000052, 1000000000000000001, 1000000000000000050, '调研覆盖机构数', 2, 10, 1, 1, 1, 'number', '家',  52, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 单层叶子节点（跨两行，col_span=1, row_span=2, is_leaf=1）
-- 质控人员配置
INSERT INTO wr_template_item
    (id, template_id, parent_id, item_name, header_row, col_index, row_span, col_span, is_leaf,
     value_type, placeholder, sort_num, create_time)
VALUES (1000000000000000121, 1000000000000000001, NULL,
        '质控人员配置', 1, 11, 2, 1, 1,
        'text', '请填写专职人数及办公场地情况', 121, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 质控工作年度亮点
INSERT INTO wr_template_item
    (id, template_id, parent_id, item_name, header_row, col_index, row_span, col_span, is_leaf,
     value_type, placeholder, sort_num, create_time)
VALUES (1000000000000000131, 1000000000000000001, NULL,
        '质控工作年度亮点', 1, 12, 2, 1, 1,
        'text', '请简要描述本年度最重要的质控工作亮点（不超过200字）', 131, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 三、上报任务
-- =====================================================
INSERT INTO wr_task (id, task_name, template_id, stat_year, deadline, status, remark, create_user, create_time)
VALUES (
    2000000000000000001,
    '2025年度省级质控中心工作情况上报',
    1000000000000000001,
    '2025',
    '2025-06-30 23:59:59',
    1,   -- 1=进行中
    '请各省级质控中心于6月30日前完成上报，对于质控调研请附调研报告。',
    8000000000000010001,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 四、上报记录（两个机构的示例数据）
-- org_a: 已提交审核通过
-- org_b: 草稿中
-- =====================================================

-- org_a_user 所在机构: organizationId = 8000000000000000002 (超声质控中心)
INSERT INTO wr_record (id, task_id, template_id, org_id, org_name, status,
                       submit_user, submit_time, audit_user, audit_time,
                       audit_result, audit_remark, create_user, create_time)
VALUES (
    3000000000000000001,
    2000000000000000001,
    1000000000000000001,
    8000000000000000002,
    '超声质控中心',
    3,   -- 3=审核通过
    8000000000000010002,
    '2025-03-15 10:00:00',
    8000000000000010001,
    '2025-03-20 14:30:00',
    3,
    '材料齐全，数据真实，审核通过',
    8000000000000010002,
    '2025-03-10 09:00:00'
)
ON CONFLICT (id) DO NOTHING;

-- org_b_user 所在机构: organizationId = 8000000000000000003 (日间手术技术指导中心)
INSERT INTO wr_record (id, task_id, template_id, org_id, org_name, status,
                       create_user, create_time)
VALUES (
    3000000000000000002,
    2000000000000000001,
    1000000000000000001,
    8000000000000000003,
    '日间手术技术指导中心',
    1,   -- 1=草稿
    8000000000000010003,
    '2025-03-18 11:00:00'
)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 五、上报数据值（org_a 的完整数据）
-- =====================================================
INSERT INTO wr_record_value (id, record_id, template_id, item_id, row_index, cell_value, create_time)
VALUES
-- 国家质控
(4000000000000000001, 3000000000000000001, 1000000000000000001, 1000000000000000011, 1, '是',           CURRENT_TIMESTAMP),
(4000000000000000002, 3000000000000000001, 1000000000000000001, 1000000000000000012, 1, '优秀',         CURRENT_TIMESTAMP),
-- 质控工作会议
(4000000000000000011, 3000000000000000001, 1000000000000000001, 1000000000000000021, 1, '6',            CURRENT_TIMESTAMP),
(4000000000000000012, 3000000000000000001, 1000000000000000001, 1000000000000000022, 1, '280',          CURRENT_TIMESTAMP),
-- 质控指导
(4000000000000000021, 3000000000000000001, 1000000000000000001, 1000000000000000031, 1, '4',            CURRENT_TIMESTAMP),
(4000000000000000022, 3000000000000000001, 1000000000000000001, 1000000000000000032, 1, '200',          CURRENT_TIMESTAMP),
-- 质控培训
(4000000000000000031, 3000000000000000001, 1000000000000000001, 1000000000000000041, 1, '3',            CURRENT_TIMESTAMP),
(4000000000000000032, 3000000000000000001, 1000000000000000001, 1000000000000000042, 1, '150',          CURRENT_TIMESTAMP),
-- 质控调研
(4000000000000000041, 3000000000000000001, 1000000000000000001, 1000000000000000051, 1, '5',            CURRENT_TIMESTAMP),
(4000000000000000042, 3000000000000000001, 1000000000000000001, 1000000000000000052, 1, '30',           CURRENT_TIMESTAMP),
-- 单层字段
(4000000000000000051, 3000000000000000001, 1000000000000000001, 1000000000000000121, 1, '专职3人，场地200㎡', CURRENT_TIMESTAMP),
(4000000000000000052, 3000000000000000001, 1000000000000000001, 1000000000000000131, 1, '承办了全省质控工作现场经验交流会，参会200余人', CURRENT_TIMESTAMP)
ON CONFLICT (record_id, item_id, row_index) DO NOTHING;

-- org_b 草稿（部分数据）
INSERT INTO wr_record_value (id, record_id, template_id, item_id, row_index, cell_value, create_time)
VALUES
(4000000000000000101, 3000000000000000002, 1000000000000000001, 1000000000000000011, 1, '否',  CURRENT_TIMESTAMP),
(4000000000000000102, 3000000000000000002, 1000000000000000001, 1000000000000000021, 1, '2',   CURRENT_TIMESTAMP),
(4000000000000000103, 3000000000000000002, 1000000000000000001, 1000000000000000022, 1, '60',  CURRENT_TIMESTAMP)
ON CONFLICT (record_id, item_id, row_index) DO NOTHING;
