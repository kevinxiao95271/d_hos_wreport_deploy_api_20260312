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
