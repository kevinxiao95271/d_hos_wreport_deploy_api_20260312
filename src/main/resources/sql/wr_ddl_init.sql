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
    meeting_start_date DATE          NOT NULL,
    meeting_start_half VARCHAR(2), -- AM/PM
    meeting_end_date   DATE          NOT NULL,
    meeting_end_half   VARCHAR(2), -- AM/PM
    meeting_form     VARCHAR(20)   NOT NULL,  -- 'online'=线上 'offline'=线下
    meeting_content  TEXT          NOT NULL,
    attendee_count   INT           NOT NULL DEFAULT 0,
    attendance_rate  NUMERIC(5,2)  NOT NULL DEFAULT 0, -- 参会率（%）
    self_score       NUMERIC(5,2),                     -- 填报者自评分（可为空）
    del_flag         SMALLINT      NOT NULL DEFAULT 0,
    create_user      BIGINT,
    create_time      TIMESTAMP     NOT NULL DEFAULT NOW(),
    update_time      TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_dw_meeting PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_dw_meeting_rid ON dw_meeting(record_id);
ALTER TABLE dw_meeting ADD COLUMN IF NOT EXISTS self_score NUMERIC(5,2);
ALTER TABLE dw_meeting ADD COLUMN IF NOT EXISTS meeting_start_date DATE;
ALTER TABLE dw_meeting ADD COLUMN IF NOT EXISTS meeting_start_half VARCHAR(2);
ALTER TABLE dw_meeting ADD COLUMN IF NOT EXISTS meeting_end_date DATE;
ALTER TABLE dw_meeting ADD COLUMN IF NOT EXISTS meeting_end_half VARCHAR(2);
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'dw_meeting'
          AND column_name = 'meeting_time'
    ) THEN
        UPDATE dw_meeting
           SET meeting_start_date = COALESCE(meeting_start_date, meeting_time),
               meeting_end_date   = COALESCE(meeting_end_date, meeting_time),
               meeting_start_half = COALESCE(meeting_start_half, 'AM'),
               meeting_end_half   = COALESCE(meeting_end_half, 'PM');
        ALTER TABLE dw_meeting DROP COLUMN IF EXISTS meeting_time;
    END IF;
END
$$;

-- ② 质控培训（多条记录）
CREATE TABLE IF NOT EXISTS dw_training (
    id               BIGINT        NOT NULL,
    record_id        BIGINT        NOT NULL,
    training_name    VARCHAR(200)  NOT NULL,
    training_start_date DATE          NOT NULL,
    training_start_half VARCHAR(2), -- AM/PM
    training_end_date   DATE          NOT NULL,
    training_end_half   VARCHAR(2), -- AM/PM
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
ALTER TABLE dw_training ADD COLUMN IF NOT EXISTS training_start_date DATE;
ALTER TABLE dw_training ADD COLUMN IF NOT EXISTS training_start_half VARCHAR(2);
ALTER TABLE dw_training ADD COLUMN IF NOT EXISTS training_end_date DATE;
ALTER TABLE dw_training ADD COLUMN IF NOT EXISTS training_end_half VARCHAR(2);
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'dw_training'
          AND column_name = 'training_time'
    ) THEN
        UPDATE dw_training
           SET training_start_date = COALESCE(training_start_date, training_time),
               training_end_date   = COALESCE(training_end_date, training_time),
               training_start_half = COALESCE(training_start_half, 'AM'),
               training_end_half   = COALESCE(training_end_half, 'PM');
        ALTER TABLE dw_training DROP COLUMN IF EXISTS training_time;
    END IF;
END
$$;

-- ③ 质控指导（多条记录）
--    市级质控中心：省→市 两级树勾选，末级节点数自动统计
--    县级质控中心：省→市→县 三级树勾选，末级节点数自动统计
--    医疗机构：手动填入数量
--    三项数量之和必须 > 0
CREATE TABLE IF NOT EXISTS dw_guidance (
    id                   BIGINT        NOT NULL,
    record_id            BIGINT        NOT NULL,
    guidance_start_date  DATE          NOT NULL,
    guidance_start_half  VARCHAR(2), -- AM/PM
    guidance_end_date    DATE          NOT NULL,
    guidance_end_half    VARCHAR(2), -- AM/PM
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
ALTER TABLE dw_guidance ADD COLUMN IF NOT EXISTS guidance_start_date DATE;
ALTER TABLE dw_guidance ADD COLUMN IF NOT EXISTS guidance_start_half VARCHAR(2);
ALTER TABLE dw_guidance ADD COLUMN IF NOT EXISTS guidance_end_date DATE;
ALTER TABLE dw_guidance ADD COLUMN IF NOT EXISTS guidance_end_half VARCHAR(2);
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'dw_guidance'
          AND column_name = 'guidance_time'
    ) THEN
        UPDATE dw_guidance
           SET guidance_start_date = COALESCE(guidance_start_date, guidance_time),
               guidance_end_date   = COALESCE(guidance_end_date, guidance_time),
               guidance_start_half = COALESCE(guidance_start_half, 'AM'),
               guidance_end_half   = COALESCE(guidance_end_half, 'PM');
        ALTER TABLE dw_guidance DROP COLUMN IF EXISTS guidance_time;
    END IF;
END
$$;

-- ④ 质控调研（多条记录）
CREATE TABLE IF NOT EXISTS dw_survey (
    id               BIGINT        NOT NULL,
    record_id        BIGINT        NOT NULL,
    survey_start_date DATE          NOT NULL,
    survey_start_half VARCHAR(2), -- AM/PM
    survey_end_date   DATE          NOT NULL,
    survey_end_half   VARCHAR(2), -- AM/PM
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
ALTER TABLE dw_survey ADD COLUMN IF NOT EXISTS survey_start_date DATE;
ALTER TABLE dw_survey ADD COLUMN IF NOT EXISTS survey_start_half VARCHAR(2);
ALTER TABLE dw_survey ADD COLUMN IF NOT EXISTS survey_end_date DATE;
ALTER TABLE dw_survey ADD COLUMN IF NOT EXISTS survey_end_half VARCHAR(2);
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'dw_survey'
          AND column_name = 'survey_time'
    ) THEN
        UPDATE dw_survey
           SET survey_start_date = COALESCE(survey_start_date, survey_time),
               survey_end_date   = COALESCE(survey_end_date, survey_time),
               survey_start_half = COALESCE(survey_start_half, 'AM'),
               survey_end_half   = COALESCE(survey_end_half, 'PM');
        ALTER TABLE dw_survey DROP COLUMN IF EXISTS survey_time;
    END IF;
END
$$;

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
    comp_start_date DATE,
    comp_start_half VARCHAR(2), -- AM/PM
    comp_end_date   DATE,
    comp_end_half   VARCHAR(2), -- AM/PM
    del_flag      SMALLINT      NOT NULL DEFAULT 0,
    create_user   BIGINT,
    create_time   TIMESTAMP     NOT NULL DEFAULT NOW(),
    update_time   TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_dw_bonus      PRIMARY KEY (id),
    CONSTRAINT uq_dw_bonus_type UNIQUE (record_id, bonus_type)
);
CREATE INDEX IF NOT EXISTS idx_dw_bonus_rid ON dw_bonus(record_id);
ALTER TABLE dw_bonus ADD COLUMN IF NOT EXISTS comp_start_date DATE;
ALTER TABLE dw_bonus ADD COLUMN IF NOT EXISTS comp_start_half VARCHAR(2);
ALTER TABLE dw_bonus ADD COLUMN IF NOT EXISTS comp_end_date DATE;
ALTER TABLE dw_bonus ADD COLUMN IF NOT EXISTS comp_end_half VARCHAR(2);
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'dw_bonus'
          AND column_name = 'comp_date'
    ) THEN
        UPDATE dw_bonus
           SET comp_start_date = COALESCE(comp_start_date, comp_date),
               comp_end_date   = COALESCE(comp_end_date, comp_date),
               comp_start_half = COALESCE(comp_start_half, 'AM'),
               comp_end_half   = COALESCE(comp_end_half, 'PM')
         WHERE bonus_type = 'competition';
        ALTER TABLE dw_bonus DROP COLUMN IF EXISTS comp_date;
    END IF;
END
$$;

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

-- score_desc：公开版考核说明（机构可见），来自评分表"得分标准"列，去掉具体分值
ALTER TABLE dw_module_config ADD COLUMN IF NOT EXISTS score_desc TEXT;

-- ===========================================================
-- dw_module_config 初始数据（幂等 UPSERT）
-- 分值来源：附件-2025年度质控工作的日常工作评分表
-- score_rule 仅管理员可见，机构端不展示
-- ===========================================================
INSERT INTO dw_module_config (id, module_key, module_name, score_max, score_rule, is_enabled, sort_order, upload_hint) VALUES
(9000000000000001, 'meeting',         '质控会议',         10, '每次质控会议得2分，上限10分', TRUE,  1, '请上传会议纪要（PDF/DOCX）、现场照片（图片/PDF）及签到表（图片/PDF/DOCX/XLSX）'),
(9000000000000002, 'training',        '质控培训',         10, '每次培训得2分，上限10分', TRUE,  2, '请上传培训材料（PDF/DOCX）及现场照片（图片/PDF）'),
(9000000000000003, 'guidance',        '质控指导',         10, '每次指导得2分，上限10分', TRUE,  3, '请上传佐证材料（PDF/DOCX）'),
(9000000000000004, 'survey',          '质控调研',        10, '每次调研得2分，上限10分', TRUE,  4, '请上传调研报告（PDF/DOCX）及现场照片（图片/PDF）'),
(9000000000000005, 'annual_work',     '年度工作落实推进', 20, '完成得20分，未完成得0分', TRUE,  5, '请上传佐证材料（PDF/DOCX），材料须加盖公章'),
(9000000000000006, 'it_construction', '信息化建设',       10, '完成得10分，未完成得0分', TRUE,  6, '请上传佐证材料（PDF/DOCX），材料须加盖公章'),
(9000000000000007, 'work_plan',       '工作计划总结',     10, '年度计划5分+年度总结5分', TRUE,  7, '请分别上传年度工作计划及年度工作总结（PDF/DOCX），材料须加盖公章'),
(9000000000000008, 'admin_response',  '行政指令响应与传达', 10, '完成得10分，未完成得0分', TRUE,  8, '请上传响应与传达的佐证材料（PDF/DOCX），材料须加盖公章'),
(9000000000000009, 'activity_report', '质控活动报备',     10, '完成得10分，未完成得0分', TRUE,  9, '请上传活动报备事前截图及事后截图（图片/PDF）'),
(9000000000000010, 'funding',         '经费执行',         10, '经费执行率及规范性', TRUE, 10, '第四季度填写，请如实填报财政专项及医院配套经费执行情况'),
(9000000000000011, 'bonus_pub',       '加分项-丛书/指南',  5, '近两年制定丛书、指南、规范、共识等情况', TRUE, 11, '请上传出版证明文件（PDF/DOCX）'),
(9000000000000012, 'bonus_comp',      '加分项-技能竞赛',   5, '省总工会+省卫健委联合举办得5分；其他形式得2分；2024-2025年内举办', TRUE, 12, '请上传竞赛证明文件（PDF/DOCX）')
ON CONFLICT (module_key) DO UPDATE SET
    module_name = EXCLUDED.module_name,
    score_max   = EXCLUDED.score_max,
    score_rule  = EXCLUDED.score_rule,
    sort_order  = EXCLUDED.sort_order,
    upload_hint = EXCLUDED.upload_hint,
    update_time = NOW();

-- ===========================================================
-- score_desc 初始数据（公开版考核说明，去掉具体分值保留"不得分"）
-- 来源：附件-2025年度质控工作的日常工作评分表"得分标准"列原文
-- ===========================================================
INSERT INTO dw_field_config (
    id, module_key, field_key, field_name, field_type, field_options,
    is_required, sort_order, placeholder, is_enabled
) VALUES
    (9000000000001001, 'meeting',         'module_self_score', '项目自评分', 'number', NULL, FALSE, 1, '请填写该项目自评分（0-满分）', TRUE),
    (9000000000001002, 'training',        'module_self_score', '项目自评分', 'number', NULL, FALSE, 1, '请填写该项目自评分（0-满分）', TRUE),
    (9000000000001003, 'guidance',        'module_self_score', '项目自评分', 'number', NULL, FALSE, 1, '请填写该项目自评分（0-满分）', TRUE),
    (9000000000001004, 'survey',          'module_self_score', '项目自评分', 'number', NULL, FALSE, 1, '请填写该项目自评分（0-满分）', TRUE),
    (9000000000001005, 'annual_work',     'module_self_score', '项目自评分', 'number', NULL, FALSE, 1, '请填写该项目自评分（0-满分）', TRUE),
    (9000000000001006, 'it_construction', 'module_self_score', '项目自评分', 'number', NULL, FALSE, 1, '请填写该项目自评分（0-满分）', TRUE),
    (9000000000001007, 'work_plan',       'module_self_score', '项目自评分', 'number', NULL, FALSE, 1, '请填写该项目自评分（0-满分）', TRUE),
    (9000000000001008, 'admin_response',  'module_self_score', '项目自评分', 'number', NULL, FALSE, 1, '请填写该项目自评分（0-满分）', TRUE),
    (9000000000001009, 'activity_report', 'module_self_score', '项目自评分', 'number', NULL, FALSE, 1, '请填写该项目自评分（0-满分）', TRUE),
    (9000000000001010, 'funding',         'module_self_score', '项目自评分', 'number', NULL, FALSE, 1, '请填写该项目自评分（0-满分）', TRUE),
    (9000000000001011, 'bonus_pub',       'module_self_score', '项目自评分', 'number', NULL, FALSE, 1, '请填写该项目自评分（0-满分）', TRUE),
    (9000000000001012, 'bonus_comp',      'module_self_score', '项目自评分', 'number', NULL, FALSE, 1, '请填写该项目自评分（0-满分）', TRUE)
ON CONFLICT (module_key, field_key) DO UPDATE SET
    field_name   = EXCLUDED.field_name,
    field_type   = EXCLUDED.field_type,
    sort_order   = EXCLUDED.sort_order,
    placeholder  = EXCLUDED.placeholder,
    is_enabled   = TRUE,
    update_time  = NOW();

UPDATE dw_module_config SET score_desc =
'（1）未开展（不得分）；
（2）有开展工作并提交相关佐证材料；
    提供每次会议内容（如通讯稿或会议纪要是否规范，包括但不限于会议名称、内容、时间、人数、形式等）、会议现场照片；
    参会人员的覆盖面与层级是否合理；提供会议签到单。'
WHERE module_key = 'meeting';

UPDATE dw_module_config SET score_desc =
'（1）未开展（不得分）；
（2）有开展工作并提交相关佐证材料；
    提供每次培训内容（包括但不限于培训名称、内容、时间、人数、形式、培训覆盖率等）、会议现场照片。'
WHERE module_key = 'training';

UPDATE dw_module_config SET score_desc =
'（1）未开展（不得分）；
（2）有开展工作并提交相关佐证材料；
    对市、县级质控中心开展质控指导；
    对全省医疗机构开展技术指导。'
WHERE module_key = 'guidance';

UPDATE dw_module_config SET score_desc =
'（1）未开展（不得分）；
（2）有开展工作并提交相关佐证材料；
    开展实地或线上调研（包括但不限于调研名称、内容、时间、形式等）、调研照片；
    调研报告是否按时提交。'
WHERE module_key = 'survey';

UPDATE dw_module_config SET score_desc =
'（1）未制定年度工作要点、重点指标（不得分）；
（2）制定年度工作要点、重点指标；
    是否按时间节点推进；
    对滞后任务是否及时分析原因并采取有效措施；
    重点工作的阶段性成果是否达到预期；
    是否存在因主观原因导致重大任务未完成的情况。'
WHERE module_key = 'annual_work';

UPDATE dw_module_config SET score_desc =
'信息化建设相关工作情况，请上传佐证材料（需加盖公章）。'
WHERE module_key = 'it_construction';

UPDATE dw_module_config SET score_desc =
'（1）无计划、总结（不得分）；
（2）有计划、总结；
    年度工作计划是否目标清晰、责任明确、措施可行；
    年度工作总结完成情况；
    计划与总结是否按规定时限报送。'
WHERE module_key = 'work_plan';

UPDATE dw_module_config SET score_desc =
'（1）对行政部门组织的重要会议、紧急通知是否及时响应；
（2）参会人员是否符合要求；
（3）会议精神是否在中心内部或相关单位间有效传达与落实；
（4）相关反馈材料是否按时、规范报送。'
WHERE module_key = 'admin_response';

UPDATE dw_module_config SET score_desc =
'（1）钉钉平台是否按规定事前报备；
（2）钉钉平台是否按规定事后报备；
（3）钉钉平台事前事后报备内容与实际执行是否一致。'
WHERE module_key = 'activity_report';

UPDATE dw_module_config SET score_desc =
'（1）财政专项经费：有拨款的中心纳入考核；
    经费执行率≥90%；执行率＜90%（不得分）。
（2）挂靠医院配套经费：所有中心纳入考核；
    医院无配套经费（不得分）；有配套经费：
    执行率≥90%；执行率≥60%；执行率≥20%；执行率＜20%（不得分）。'
WHERE module_key = 'funding';

UPDATE dw_module_config SET score_desc =
'第一署名为质控中心或技术指导中心（2024-2025年期间）：
    丛书/指南/共识；
    标准/规范。'
WHERE module_key = 'bonus_pub';

UPDATE dw_module_config SET score_desc =
'以质控中心或技术指导中心名义开展技能竞赛（2024-2025年期间）：
    省总工会、省卫健委联合主办；
    其他形式。'
WHERE module_key = 'bonus_comp';

-- ===========================================================
-- ⑪ 质控指导地区树表（浙江省 市/县 两棵树，启动时一次性加载内存）
--    tree_type: 'city'   = 省→市级质控中心（共 11 个叶节点，level=2）
--               'county' = 省→市→区县（市节点 level=2，区县节点 level=3）
--    ID 规划：
--      city   树叶节点 101~111
--      county 树市节点 201~211，区县节点 2xx01~2xx13
-- ===========================================================
CREATE TABLE IF NOT EXISTS dw_region (
    id          INT         NOT NULL,
    parent_id   INT,
    name        VARCHAR(50) NOT NULL,
    level       SMALLINT    NOT NULL,   -- 2=市, 3=区县
    tree_type   VARCHAR(10) NOT NULL,   -- 'city' | 'county'
    sort_order  INT         NOT NULL DEFAULT 0,
    CONSTRAINT pk_dw_region PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_dw_region_tree ON dw_region(tree_type, parent_id);

-- 全量数据（幂等插入，已存在则跳过）
INSERT INTO dw_region (id, parent_id, name, level, tree_type, sort_order) VALUES
-- ── city 树：省→市级质控中心（11 个叶节点，parent_id=NULL）─────────────────────
(101, NULL, '杭州市级', 2, 'city',   1),
(102, NULL, '宁波市级', 2, 'city',   2),
(103, NULL, '温州市级', 2, 'city',   3),
(104, NULL, '湖州市级', 2, 'city',   4),
(105, NULL, '嘉兴市级', 2, 'city',   5),
(106, NULL, '绍兴市级', 2, 'city',   6),
(107, NULL, '金华市级', 2, 'city',   7),
(108, NULL, '衢州市级', 2, 'city',   8),
(109, NULL, '舟山市级', 2, 'city',   9),
(110, NULL, '台州市级', 2, 'city',  10),
(111, NULL, '丽水市级', 2, 'city',  11),
-- ── county 树：市级节点（parent_id=NULL）───────────────────────────────────────
(201, NULL, '杭州市', 2, 'county',  1),
(202, NULL, '宁波市', 2, 'county',  2),
(203, NULL, '温州市', 2, 'county',  3),
(204, NULL, '湖州市', 2, 'county',  4),
(205, NULL, '嘉兴市', 2, 'county',  5),
(206, NULL, '绍兴市', 2, 'county',  6),
(207, NULL, '金华市', 2, 'county',  7),
(208, NULL, '衢州市', 2, 'county',  8),
(209, NULL, '舟山市', 2, 'county',  9),
(210, NULL, '台州市', 2, 'county', 10),
(211, NULL, '丽水市', 2, 'county', 11),
-- ── county 树：杭州市区县（parent_id=201）──────────────────────────────────────
(20101, 201, '上城区', 3, 'county',  1),
(20102, 201, '拱墅区', 3, 'county',  2),
(20103, 201, '西湖区', 3, 'county',  3),
(20104, 201, '滨江区', 3, 'county',  4),
(20105, 201, '萧山区', 3, 'county',  5),
(20106, 201, '余杭区', 3, 'county',  6),
(20107, 201, '临平区', 3, 'county',  7),
(20108, 201, '钱塘区', 3, 'county',  8),
(20109, 201, '富阳区', 3, 'county',  9),
(20110, 201, '临安区', 3, 'county', 10),
(20111, 201, '桐庐县', 3, 'county', 11),
(20112, 201, '淳安县', 3, 'county', 12),
(20113, 201, '建德市', 3, 'county', 13),
-- ── county 树：宁波市区县（parent_id=202）──────────────────────────────────────
(20201, 202, '海曙区', 3, 'county',  1),
(20202, 202, '江北区', 3, 'county',  2),
(20203, 202, '镇海区', 3, 'county',  3),
(20204, 202, '北仑区', 3, 'county',  4),
(20205, 202, '鄞州区', 3, 'county',  5),
(20206, 202, '奉化区', 3, 'county',  6),
(20207, 202, '余姚市', 3, 'county',  7),
(20208, 202, '慈溪市', 3, 'county',  8),
(20209, 202, '宁海县', 3, 'county',  9),
(20210, 202, '象山县', 3, 'county', 10),
-- ── county 树：温州市区县（parent_id=203）──────────────────────────────────────
(20301, 203, '鹿城区', 3, 'county',  1),
(20302, 203, '龙湾区', 3, 'county',  2),
(20303, 203, '瓯海区', 3, 'county',  3),
(20304, 203, '洞头区', 3, 'county',  4),
(20305, 203, '乐清市', 3, 'county',  5),
(20306, 203, '瑞安市', 3, 'county',  6),
(20307, 203, '永嘉县', 3, 'county',  7),
(20308, 203, '文成县', 3, 'county',  8),
(20309, 203, '平阳县', 3, 'county',  9),
(20310, 203, '泰顺县', 3, 'county', 10),
(20311, 203, '苍南县', 3, 'county', 11),
(20312, 203, '龙港市', 3, 'county', 12),
-- ── county 树：湖州市区县（parent_id=204）──────────────────────────────────────
(20401, 204, '吴兴区', 3, 'county',  1),
(20402, 204, '南浔区', 3, 'county',  2),
(20403, 204, '德清县', 3, 'county',  3),
(20404, 204, '长兴县', 3, 'county',  4),
(20405, 204, '安吉县', 3, 'county',  5),
-- ── county 树：嘉兴市区县（parent_id=205）──────────────────────────────────────
(20501, 205, '南湖区', 3, 'county',  1),
(20502, 205, '秀洲区', 3, 'county',  2),
(20503, 205, '嘉善县', 3, 'county',  3),
(20504, 205, '平湖市', 3, 'county',  4),
(20505, 205, '海盐县', 3, 'county',  5),
(20506, 205, '海宁市', 3, 'county',  6),
(20507, 205, '桐乡市', 3, 'county',  7),
-- ── county 树：绍兴市区县（parent_id=206）──────────────────────────────────────
(20601, 206, '越城区', 3, 'county',  1),
(20602, 206, '柯桥区', 3, 'county',  2),
(20603, 206, '上虞区', 3, 'county',  3),
(20604, 206, '诸暨市', 3, 'county',  4),
(20605, 206, '嵊州市', 3, 'county',  5),
(20606, 206, '新昌县', 3, 'county',  6),
-- ── county 树：金华市区县（parent_id=207）──────────────────────────────────────
(20701, 207, '婺城区', 3, 'county',  1),
(20702, 207, '金东区', 3, 'county',  2),
(20703, 207, '兰溪市', 3, 'county',  3),
(20704, 207, '东阳市', 3, 'county',  4),
(20705, 207, '义乌市', 3, 'county',  5),
(20706, 207, '永康市', 3, 'county',  6),
(20707, 207, '浦江县', 3, 'county',  7),
(20708, 207, '武义县', 3, 'county',  8),
(20709, 207, '磐安县', 3, 'county',  9),
-- ── county 树：衢州市区县（parent_id=208）──────────────────────────────────────
(20801, 208, '柯城区', 3, 'county',  1),
(20802, 208, '衢江区', 3, 'county',  2),
(20803, 208, '龙游县', 3, 'county',  3),
(20804, 208, '江山市', 3, 'county',  4),
(20805, 208, '常山县', 3, 'county',  5),
(20806, 208, '开化县', 3, 'county',  6),
-- ── county 树：舟山市区县（parent_id=209）──────────────────────────────────────
(20901, 209, '定海区', 3, 'county',  1),
(20902, 209, '普陀区', 3, 'county',  2),
(20903, 209, '岱山县', 3, 'county',  3),
(20904, 209, '嵊泗县', 3, 'county',  4),
-- ── county 树：台州市区县（parent_id=210）──────────────────────────────────────
(21001, 210, '椒江区', 3, 'county',  1),
(21002, 210, '黄岩区', 3, 'county',  2),
(21003, 210, '路桥区', 3, 'county',  3),
(21004, 210, '临海市', 3, 'county',  4),
(21005, 210, '温岭市', 3, 'county',  5),
(21006, 210, '玉环市', 3, 'county',  6),
(21007, 210, '天台县', 3, 'county',  7),
(21008, 210, '仙居县', 3, 'county',  8),
(21009, 210, '三门县', 3, 'county',  9),
-- ── county 树：丽水市区县（parent_id=211）──────────────────────────────────────
(21101, 211, '莲都区', 3, 'county',  1),
(21102, 211, '龙泉市', 3, 'county',  2),
(21103, 211, '青田县', 3, 'county',  3),
(21104, 211, '云和县', 3, 'county',  4),
(21105, 211, '庆元县', 3, 'county',  5),
(21106, 211, '缙云县', 3, 'county',  6),
(21107, 211, '遂昌县', 3, 'county',  7),
(21108, 211, '松阳县', 3, 'county',  8),
(21109, 211, '景宁县', 3, 'county',  9)
ON CONFLICT (id) DO NOTHING;
