CREATE TABLE IF NOT EXISTS wr_template (
    id               BIGINT       NOT NULL,
    template_name    VARCHAR(200) NOT NULL,
    description      TEXT,
    status           SMALLINT     NOT NULL DEFAULT 0,
    create_user      BIGINT,
    create_time      TIMESTAMP,
    update_user      BIGINT,
    update_time      TIMESTAMP,
    del_flag         SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT pk_wr_template PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS wr_template_item (
    id                      BIGINT       NOT NULL,
    template_id             BIGINT       NOT NULL,
    parent_id               BIGINT,
    item_name               VARCHAR(200) NOT NULL,
    header_row              SMALLINT     NOT NULL DEFAULT 1,
    col_index               INT          NOT NULL DEFAULT 1,
    row_span                SMALLINT     NOT NULL DEFAULT 1,
    col_span                SMALLINT     NOT NULL DEFAULT 1,
    is_leaf                 SMALLINT     NOT NULL DEFAULT 1,
    value_type              VARCHAR(30)  NOT NULL DEFAULT 'text',
    unit                    VARCHAR(50),
    placeholder             VARCHAR(500),
    require_attachment      SMALLINT     NOT NULL DEFAULT 0,
    format_template_file_id BIGINT,
    format_template_url     VARCHAR(1000),
    format_template_name    VARCHAR(500),
    sort_num                INT          NOT NULL DEFAULT 0,
    create_user             BIGINT,
    create_time             TIMESTAMP,
    update_user             BIGINT,
    update_time             TIMESTAMP,
    del_flag                SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT pk_wr_template_item PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_wr_template_item_tid ON wr_template_item(template_id);
CREATE INDEX IF NOT EXISTS idx_wr_template_item_pid ON wr_template_item(parent_id);
CREATE TABLE IF NOT EXISTS wr_task (
    id          BIGINT       NOT NULL,
    task_name   VARCHAR(300) NOT NULL,
    template_id BIGINT       NOT NULL,
    stat_year   VARCHAR(10),
    deadline    TIMESTAMP,
    status      SMALLINT     NOT NULL DEFAULT 0,
    remark      TEXT,
    create_user BIGINT,
    create_time TIMESTAMP,
    update_user BIGINT,
    update_time TIMESTAMP,
    del_flag    SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT pk_wr_task PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_wr_task_status   ON wr_task(status);
CREATE INDEX IF NOT EXISTS idx_wr_task_deadline ON wr_task(deadline) WHERE del_flag = 0;
CREATE TABLE IF NOT EXISTS wr_task_org_scope (
    id          BIGINT NOT NULL,
    task_id     BIGINT NOT NULL,
    org_id      BIGINT NOT NULL,
    create_user BIGINT,
    create_time TIMESTAMP,
    CONSTRAINT pk_wr_task_org_scope PRIMARY KEY (id),
    CONSTRAINT uq_wr_task_org_scope UNIQUE (task_id, org_id)
);
-- task_id 单独索引：NOT EXISTS / LEFT JOIN 过滤任务是否有范围限制
CREATE INDEX IF NOT EXISTS idx_wr_task_org_scope_tid     ON wr_task_org_scope(task_id);
-- 复合索引：isTaskInScope / selectActiveTasksByOrg 的 (task_id, org_id) 查询
CREATE INDEX IF NOT EXISTS idx_wr_task_org_scope_tid_oid ON wr_task_org_scope(task_id, org_id);
CREATE TABLE IF NOT EXISTS wr_record (
    id           BIGINT       NOT NULL,
    task_id      BIGINT       NOT NULL,
    template_id  BIGINT       NOT NULL,
    org_id       BIGINT       NOT NULL,
    org_name     VARCHAR(200),
    status       SMALLINT     NOT NULL DEFAULT 0,
    submit_user  BIGINT,
    submit_time  TIMESTAMP,
    audit_user   BIGINT,
    audit_time   TIMESTAMP,
    audit_result      SMALLINT,
    audit_remark      TEXT,
    resubmit_deadline TIMESTAMP,
    create_user  BIGINT,
    create_time  TIMESTAMP,
    update_user  BIGINT,
    update_time  TIMESTAMP,
    del_flag     SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT pk_wr_record PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_wr_record_task_org ON wr_record(task_id, org_id) WHERE del_flag = 0;
CREATE INDEX IF NOT EXISTS idx_wr_record_task ON wr_record(task_id);
CREATE INDEX IF NOT EXISTS idx_wr_record_org  ON wr_record(org_id);
CREATE TABLE IF NOT EXISTS wr_record_value (
    id          BIGINT   NOT NULL,
    record_id   BIGINT   NOT NULL,
    template_id BIGINT   NOT NULL,
    item_id     BIGINT   NOT NULL,
    row_index   INT      NOT NULL DEFAULT 1,
    cell_value  TEXT,
    create_user BIGINT,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    CONSTRAINT pk_wr_record_value PRIMARY KEY (id),
    CONSTRAINT uq_wr_record_value UNIQUE (record_id, item_id, row_index)
);
CREATE INDEX IF NOT EXISTS idx_wr_record_value_rid    ON wr_record_value(record_id);
-- item_id 索引：aggregate 按列聚合时按 item_id 过滤
CREATE INDEX IF NOT EXISTS idx_wr_record_value_iid    ON wr_record_value(item_id);
-- 复合索引：按 (record_id, item_id) 快速回填单条记录的所有值
CREATE INDEX IF NOT EXISTS idx_wr_record_value_rid_iid ON wr_record_value(record_id, item_id);
CREATE TABLE IF NOT EXISTS wr_attachment (
    id          BIGINT       NOT NULL,
    record_id   BIGINT       NOT NULL,
    item_id     BIGINT,
    file_id     BIGINT,
    attach_name VARCHAR(500),
    attach_path VARCHAR(1000),
    attach_size BIGINT,
    attach_type VARCHAR(50),
    create_user BIGINT,
    create_time TIMESTAMP,
    del_flag    SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT pk_wr_attachment PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_wr_attachment_rid ON wr_attachment(record_id);

-- 补充 wr_record.resubmit_deadline 字段（幂等，列已存在时会报错被 DatabaseInitializer 忽略）
ALTER TABLE wr_record ADD COLUMN IF NOT EXISTS resubmit_deadline TIMESTAMP;

-- 模板总字数限制字段：
--   NULL 或 0 → 不限制（功能未启用）
--   正整数    → 启用，填报提交时所有 cell_value 字符总数不得超过此值
--   由管理员在模板配置页设置，0 代表"未启用"；前端用 checkbox+数字框表达
ALTER TABLE wr_template ADD COLUMN IF NOT EXISTS max_total_chars INT NOT NULL DEFAULT 0;
-- 补充 wr_template_item.dict_code 字段（字典绑定，幂等）
ALTER TABLE wr_template_item ADD COLUMN IF NOT EXISTS dict_code VARCHAR(100);

CREATE TABLE IF NOT EXISTS wr_template_row (
    id               BIGINT       NOT NULL,
    template_id      BIGINT       NOT NULL,
    row_index        INT          NOT NULL,
    row_label        VARCHAR(200) NOT NULL,
    row_level        SMALLINT     NOT NULL DEFAULT 1,
    parent_row_index INT,
    sort_num         INT          NOT NULL DEFAULT 0,
    create_user      BIGINT,
    create_time      TIMESTAMP,
    update_user      BIGINT,
    update_time      TIMESTAMP,
    del_flag         SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT pk_wr_template_row PRIMARY KEY (id),
    CONSTRAINT uq_wr_template_row UNIQUE (template_id, row_index)
);
CREATE INDEX IF NOT EXISTS idx_wr_template_row_tid ON wr_template_row(template_id);

-- ============================================================
-- 评分细则模板（score 类型）扩展字段
-- ============================================================

-- wr_template.template_type：模板类型
--   'form'  → 原有表单录入模板（附件2/附件3，默认值，存量数据保持不变）
--   'score' → 评分细则模板（纯文件上传 + 分值，本次新增）
ALTER TABLE wr_template
    ADD COLUMN IF NOT EXISTS template_type VARCHAR(20) NOT NULL DEFAULT 'form';

-- wr_template_item.min_attachments：该指标最少上传文件数
--   0 → 不强制校验；正整数 → 提交时文件不足则拒绝（error 4033）
ALTER TABLE wr_template_item
    ADD COLUMN IF NOT EXISTS min_attachments INT NOT NULL DEFAULT 0;

-- wr_template_item.max_attachments：该指标最多上传文件数
--   0 → 不限；正整数 → 上传时超出则直接拒绝
ALTER TABLE wr_template_item
    ADD COLUMN IF NOT EXISTS max_attachments INT NOT NULL DEFAULT 0;

-- wr_template_item.score_value：该指标固定分值（仅 score 类模板使用）
--   form 类模板默认 0 即可；导出时按指标文件达标情况折算得分
ALTER TABLE wr_template_item
    ADD COLUMN IF NOT EXISTS score_value NUMERIC(6,1) NOT NULL DEFAULT 0;

-- wr_template_item.allowed_formats：允许上传的文件格式（逗号分隔扩展名，不含点）
--   NULL 或空串 → 不限制任何格式
--   示例："pdf"              → 仅 PDF
--         "pdf,doc,docx"     → PDF 或 Word
--         "pdf,jpg,jpeg,png,gif" → PDF 或图片
--   上传时后端取扩展名与此列表匹配，不符合则拒绝（error 4035）
ALTER TABLE wr_template_item
    ADD COLUMN IF NOT EXISTS allowed_formats VARCHAR(200);

-- ===========================================================
-- 日常工作模块（Daily Work）DDL
-- 附件文件实体存储于 MinIO，数据库只存 URL 及元数据
-- 任务/审批/驳回流程复用现有 wr_task + wr_record 体系
-- ===========================================================

-- wr_task 新增 task_type 区分普通模板任务与日常工作任务
--   'normal'      普通模板填报任务（现有）
--   'daily_work'  日常工作模块任务（新增）
ALTER TABLE wr_task
    ADD COLUMN IF NOT EXISTS task_type VARCHAR(20) NOT NULL DEFAULT 'normal';
-- 日常工作任务无模板，template_id 改为可空
ALTER TABLE wr_task ALTER COLUMN template_id DROP NOT NULL;
-- wr_record 同步
ALTER TABLE wr_record ALTER COLUMN template_id DROP NOT NULL;

-- ① 质控会议（多条记录，每条对应一次会议）
CREATE TABLE IF NOT EXISTS dw_meeting (
    id               BIGINT        NOT NULL,
    record_id        BIGINT        NOT NULL,  -- 关联 wr_record.id
    meeting_name     VARCHAR(200)  NOT NULL,
    meeting_time     DATE          NOT NULL,
    meeting_form     VARCHAR(20)   NOT NULL,  -- 'online'=线上 'offline'=线下
    meeting_content  TEXT          NOT NULL,
    attendee_count   INT           NOT NULL DEFAULT 0,
    attendance_rate  NUMERIC(5,2)  NOT NULL DEFAULT 0, -- 参会率（%）
    del_flag         SMALLINT      NOT NULL DEFAULT 0,
    create_user      BIGINT,
    create_time      TIMESTAMP     NOT NULL DEFAULT NOW(),
    update_time      TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_dw_meeting PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_dw_meeting_rid ON dw_meeting(record_id);

-- ② 质控培训（多条记录）
CREATE TABLE IF NOT EXISTS dw_training (
    id               BIGINT        NOT NULL,
    record_id        BIGINT        NOT NULL,
    training_name    VARCHAR(200)  NOT NULL,
    training_time    DATE          NOT NULL,
    training_form    VARCHAR(20)   NOT NULL,  -- 'online'=线上 'offline'=线下
    training_content TEXT          NOT NULL,
    attendee_count   INT           NOT NULL DEFAULT 0,
    coverage_rate    NUMERIC(5,2)  NOT NULL DEFAULT 0, -- 培训覆盖率（%）
    del_flag         SMALLINT      NOT NULL DEFAULT 0,
    create_user      BIGINT,
    create_time      TIMESTAMP     NOT NULL DEFAULT NOW(),
    update_time      TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_dw_training PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_dw_training_rid ON dw_training(record_id);

-- ③ 质控指导（多条记录）
--    市级质控中心：省→市 两级树勾选，末级节点数自动统计
--    县级质控中心：省→市→县 三级树勾选，末级节点数自动统计
--    医疗机构：手动填入数量
--    三项数量之和必须 > 0
CREATE TABLE IF NOT EXISTS dw_guidance (
    id                   BIGINT        NOT NULL,
    record_id            BIGINT        NOT NULL,
    guidance_time        DATE          NOT NULL,
    guidance_form        VARCHAR(20)   NOT NULL,  -- 'online'=线上 'onsite'=现场
    guidance_content     TEXT          NOT NULL,
    city_center_count    INT           NOT NULL DEFAULT 0, -- 市级质控中心数量
    city_center_ids      TEXT,                            -- 市级节点ID列表（JSON数组，供前端回显）
    county_center_count  INT           NOT NULL DEFAULT 0, -- 县级质控中心数量
    county_center_ids    TEXT,                            -- 县级节点ID列表（JSON数组，供前端回显）
    hospital_count       INT           NOT NULL DEFAULT 0, -- 医疗机构数量（手动填入）
    del_flag             SMALLINT      NOT NULL DEFAULT 0,
    create_user          BIGINT,
    create_time          TIMESTAMP     NOT NULL DEFAULT NOW(),
    update_time          TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_dw_guidance    PRIMARY KEY (id),
    CONSTRAINT ck_dw_guidance_nz CHECK (city_center_count + county_center_count + hospital_count > 0)
);
CREATE INDEX IF NOT EXISTS idx_dw_guidance_rid ON dw_guidance(record_id);

-- ④ 质控调研（多条记录）
CREATE TABLE IF NOT EXISTS dw_survey (
    id               BIGINT        NOT NULL,
    record_id        BIGINT        NOT NULL,
    survey_time      DATE          NOT NULL,
    survey_target    VARCHAR(300)  NOT NULL,  -- 调研对象
    survey_type      VARCHAR(20)   NOT NULL,  -- 'baseline'=基线调研 'special'=专项调研
    survey_form      VARCHAR(20)   NOT NULL,  -- 'online'=线上 'offline'=线下
    survey_content   TEXT          NOT NULL,
    del_flag         SMALLINT      NOT NULL DEFAULT 0,
    create_user      BIGINT,
    create_time      TIMESTAMP     NOT NULL DEFAULT NOW(),
    update_time      TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_dw_survey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_dw_survey_rid ON dw_survey(record_id);

-- ⑤ 经费执行（每个 record 只有一条，第四季度填写）
--    fiscal_execution_rate：≥90%→3分；<90%→0分
--    hospital_execution_rate：≥90%→3分；≥60%→2分；≥20%→1分；<20%→0分
CREATE TABLE IF NOT EXISTS dw_funding (
    id                       BIGINT        NOT NULL,
    record_id                BIGINT        NOT NULL,
    fiscal_has_fund          BOOLEAN       NOT NULL DEFAULT FALSE, -- 财政专项是否有拨款
    fiscal_execution_rate    NUMERIC(5,2),                        -- 财政专项执行率（%）
    hospital_has_fund        BOOLEAN       NOT NULL DEFAULT FALSE, -- 医院配套是否有拨款
    hospital_execution_rate  NUMERIC(5,2),                        -- 医院配套执行率（%）
    del_flag                 SMALLINT      NOT NULL DEFAULT 0,
    create_user              BIGINT,
    create_time              TIMESTAMP     NOT NULL DEFAULT NOW(),
    update_time              TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_dw_funding     PRIMARY KEY (id),
    CONSTRAINT uq_dw_funding_rid UNIQUE (record_id)
);

-- ⑥ 加分项（publication / competition 各最多一条）
--    pub_category: 'book_guide_consensus'=丛书/指南/共识(3分) 'standard_norm'=标准/规范(2分)
--    comp_sponsor:  'provincial_joint'=省总工会+省卫健委联合(5分) 'other'=其他形式(2分)
CREATE TABLE IF NOT EXISTS dw_bonus (
    id            BIGINT        NOT NULL,
    record_id     BIGINT        NOT NULL,
    bonus_type    VARCHAR(20)   NOT NULL, -- 'publication'=丛书/指南  'competition'=技能竞赛
    pub_name      VARCHAR(300),
    pub_category  VARCHAR(30),
    pub_date      DATE,                  -- 限 2024-2025 年
    comp_name     VARCHAR(300),
    comp_sponsor  VARCHAR(30),
    comp_date     DATE,                  -- 限 2024-2025 年
    del_flag      SMALLINT      NOT NULL DEFAULT 0,
    create_user   BIGINT,
    create_time   TIMESTAMP     NOT NULL DEFAULT NOW(),
    update_time   TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_dw_bonus      PRIMARY KEY (id),
    CONSTRAINT uq_dw_bonus_type UNIQUE (record_id, bonus_type)
);
CREATE INDEX IF NOT EXISTS idx_dw_bonus_rid ON dw_bonus(record_id);

-- ⑦ 日常工作附件表
--    文件实体存 MinIO，此处只存 URL + 元数据
--
--    module_type 枚举：
--      'meeting'          质控会议
--      'training'         质控培训
--      'guidance'         质控指导
--      'survey'           质控调研
--      'annual_work'      年度工作落实推进（模块5）
--      'it_construction'  信息化建设（模块6）
--      'work_plan'        工作计划总结（模块7）
--      'admin_response'   行政指令响应与传达（模块8）
--      'activity_report'  质控活动报备（模块9）
--      'bonus'            加分项
--
--    slot 枚举（区分同一子记录下的不同附件类型）：
--      meeting        → minutes(纪要/通讻稿) | photo(现场照片) | signin(签到表)
--      training       → material(培训材料)   | photo(现场照片)
--      guidance       → evidence(佐证材料)
--      survey         → report(调研报告)     | photo(现场照片)
--      annual_work    → evidence
--      it_construction→ evidence
--      work_plan      → plan(年度计划)       | summary(年度总结)
--      admin_response → evidence
--      activity_report→ pre_report(事前截图) | post_report(事后截图)
--      bonus          → evidence(证明文件)
CREATE TABLE IF NOT EXISTS dw_attachment (
    id             BIGINT        NOT NULL,
    record_id      BIGINT        NOT NULL,   -- 关联 wr_record.id
    module_type    VARCHAR(30)   NOT NULL,   -- 所属模块
    sub_record_id  BIGINT,                  -- 子记录ID（dw_meeting/training/guidance/survey/bonus 的 id）
    slot           VARCHAR(30)   NOT NULL,   -- 附件槽位
    file_name      VARCHAR(500)  NOT NULL,   -- 原始文件名
    file_url       VARCHAR(1000) NOT NULL,   -- MinIO 直链 URL
    file_size      BIGINT,                  -- 文件大小（字节）
    file_mime      VARCHAR(100),            -- MIME 类型
    del_flag       SMALLINT      NOT NULL DEFAULT 0,
    create_user    BIGINT,
    create_time    TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_dw_attachment PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_dw_attach_rid ON dw_attachment(record_id);
CREATE INDEX IF NOT EXISTS idx_dw_attach_sub ON dw_attachment(sub_record_id)
    WHERE sub_record_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_dw_attach_mod ON dw_attachment(record_id, module_type);

-- ===========================================================
-- 日常工作模块配置化（Route A + 局部配置）
-- ===========================================================

-- ⑧ 模块配置表（每行对应一个评分模块）
--    管理员可调整：模块名、满分、提示语、启用状态、排序
CREATE TABLE IF NOT EXISTS dw_module_config (
    id           BIGINT        NOT NULL,
    module_key   VARCHAR(30)   NOT NULL,   -- 与 dw_attachment.module_type 一致
    module_name  VARCHAR(100)  NOT NULL,   -- 前端展示名
    score_max    NUMERIC(6,2)  NOT NULL DEFAULT 0, -- 满分（含加分项）
    score_rule   TEXT,                    -- 评分规则说明（JSON/自由文本），仅管理员可见
    is_enabled   BOOLEAN       NOT NULL DEFAULT TRUE,
    sort_order   INT           NOT NULL DEFAULT 0,
    upload_hint  TEXT,                    -- 附件上传提示语，前端展示给机构用户
    create_time  TIMESTAMP     NOT NULL DEFAULT NOW(),
    update_time  TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_dw_module_config    PRIMARY KEY (id),
    CONSTRAINT uq_dw_module_key       UNIQUE (module_key)
);

-- ⑨ 模块扩展字段定义表（运营人员在后台新增字段，无需改代码）
--    field_type: 'text' | 'number' | 'enum' | 'checkbox'
--    field_options: enum/checkbox 时存 JSON 数组，如 ["线上","线下"]
CREATE TABLE IF NOT EXISTS dw_field_config (
    id             BIGINT        NOT NULL,
    module_key     VARCHAR(30)   NOT NULL,
    field_key      VARCHAR(50)   NOT NULL,  -- 字段唯一标识（英文小写下划线）
    field_name     VARCHAR(100)  NOT NULL,  -- 字段显示名
    field_type     VARCHAR(20)   NOT NULL,  -- 'text'|'number'|'enum'|'checkbox'
    field_options  TEXT,                   -- enum/checkbox 选项（JSON字符串数组）
    is_required    BOOLEAN       NOT NULL DEFAULT FALSE,
    sort_order     INT           NOT NULL DEFAULT 0,
    placeholder    VARCHAR(200),           -- 输入提示占位符
    is_enabled     BOOLEAN       NOT NULL DEFAULT TRUE,
    create_time    TIMESTAMP     NOT NULL DEFAULT NOW(),
    update_time    TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_dw_field_config PRIMARY KEY (id),
    CONSTRAINT uq_dw_field_key    UNIQUE (module_key, field_key)
);
CREATE INDEX IF NOT EXISTS idx_dw_field_cfg_mod ON dw_field_config(module_key);

-- ⑩ 模块扩展字段值表（统一用 TEXT 存储，前端按 field_type 解析）
--    sub_record_id：多条记录型模块（meeting/training/guidance/survey/bonus）的子记录ID
--                  纯上传型/单条型模块（annual_work/funding 等）时为 NULL
CREATE TABLE IF NOT EXISTS dw_field_value (
    id             BIGINT        NOT NULL,
    record_id      BIGINT        NOT NULL,
    sub_record_id  BIGINT,
    module_key     VARCHAR(30)   NOT NULL,
    field_key      VARCHAR(50)   NOT NULL,
    field_value    TEXT,                   -- 所有类型统一 TEXT；checkbox 存 JSON 数组
    create_time    TIMESTAMP     NOT NULL DEFAULT NOW(),
    update_time    TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_dw_field_value  PRIMARY KEY (id)
);
-- sub_record_id 可为 NULL，用两个部分唯一索引分别保证去重
CREATE UNIQUE INDEX IF NOT EXISTS uq_dw_fv_with_sub    ON dw_field_value (record_id, sub_record_id, module_key, field_key)
    WHERE sub_record_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_dw_fv_without_sub ON dw_field_value (record_id, module_key, field_key)
    WHERE sub_record_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_dw_fv_record ON dw_field_value(record_id);
CREATE INDEX IF NOT EXISTS idx_dw_fv_sub    ON dw_field_value(sub_record_id)
    WHERE sub_record_id IS NOT NULL;

-- ===========================================================
-- dw_module_config 初始数据（幂等 UPSERT）
-- 分值来源：附件-2025年度质控工作的日常工作评分表
-- score_rule 仅管理员可见，机构端不展示
-- ===========================================================
INSERT INTO dw_module_config (id, module_key, module_name, score_max, score_rule, is_enabled, sort_order, upload_hint) VALUES
(9000000000000001, 'meeting',         '质控会议',         10, '每次质控会议得2分，上限10分', TRUE,  1, '请上传会议纪要（PDF/DOCX）、现场照片（图片/PDF）及签到表（图片/PDF/DOCX/XLSX）'),
(9000000000000002, 'training',        '质控培训',         10, '每次培训得2分，上限10分', TRUE,  2, '请上传培训材料（PDF/DOCX）及现场照片（图片/PDF）'),
(9000000000000003, 'guidance',        '质控指导',         10, '每次指导得2分，上限10分', TRUE,  3, '请上传佐证材料（PDF/DOCX）'),
(9000000000000004, 'survey',          '质控调研（检查）', 10, '每次调研得2分，上限10分', TRUE,  4, '请上传调研报告（PDF/DOCX）及现场照片（图片/PDF）'),
(9000000000000005, 'annual_work',     '年度工作落实推进', 15, '完成得15分，未完成得0分', TRUE,  5, '请上传佐证材料（PDF/DOCX），材料须加盖公章'),
(9000000000000006, 'it_construction', '信息化建设',       10, '完成得10分，未完成得0分', TRUE,  6, '请上传佐证材料（PDF/DOCX），材料须加盖公章'),
(9000000000000007, 'work_plan',       '工作计划总结',     15, '年度计划8分+年度总结7分', TRUE,  7, '请分别上传年度工作计划及年度工作总结（PDF/DOCX），材料须加盖公章'),
(9000000000000008, 'admin_response',  '行政指令响应与传达', 10, '完成得10分，未完成得0分', TRUE,  8, '请上传响应与传达的佐证材料（PDF/DOCX），材料须加盖公章'),
(9000000000000009, 'activity_report', '质控活动报备',     10, '完成得10分，未完成得0分', TRUE,  9, '请上传活动报备事前截图及事后截图（图片/PDF）'),
(9000000000000010, 'funding',         '经费执行',          6, '财政专项≥90%得3分；医院配套≥90%得3分，≥60%得2分，≥20%得1分', TRUE, 10, '第四季度填写，请如实填报财政专项及医院配套经费执行情况'),
(9000000000000011, 'bonus_pub',       '加分项-丛书/指南',  3, '丛书/指南/共识得3分；标准/规范得2分；2024-2025年内出版', TRUE, 11, '请上传出版证明文件（PDF/DOCX）'),
(9000000000000012, 'bonus_comp',      '加分项-技能竞赛',   5, '省总工会+省卫健委联合举办得5分；其他形式得2分；2024-2025年内举办', TRUE, 12, '请上传竞赛证明文件（PDF/DOCX）')
ON CONFLICT (module_key) DO UPDATE SET
    module_name = EXCLUDED.module_name,
    score_max   = EXCLUDED.score_max,
    score_rule  = EXCLUDED.score_rule,
    sort_order  = EXCLUDED.sort_order,
    upload_hint = EXCLUDED.upload_hint,
    update_time = NOW();
