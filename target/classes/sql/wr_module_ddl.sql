-- =====================================================
-- 日常工作管理模块 - 业务表 DDL
-- Schema: zjylzl
-- DB: PostgreSQL
-- 注意: 不修改任何已有表结构
-- =====================================================

SET search_path TO zjylzl;

-- ---------------------------------------------------
-- 1. 上报模板表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS wr_template (
    id               BIGINT          NOT NULL,
    template_name    VARCHAR(200)    NOT NULL,
    description      TEXT,
    status           SMALLINT        NOT NULL DEFAULT 0,  -- 0=停用 1=启用
    create_user      BIGINT,
    create_time      TIMESTAMP,
    update_user      BIGINT,
    update_time      TIMESTAMP,
    del_flag         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_wr_template PRIMARY KEY (id)
);

COMMENT ON TABLE  wr_template            IS '上报模板';
COMMENT ON COLUMN wr_template.status     IS '状态: 0=停用 1=启用';
COMMENT ON COLUMN wr_template.del_flag   IS '删除标记: 0=正常 1=已删除';

-- ---------------------------------------------------
-- 2. 模板字段/表头节点表（支持多级表头树）
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS wr_template_item (
    id                       BIGINT          NOT NULL,
    template_id              BIGINT          NOT NULL,
    parent_id                BIGINT,                       -- NULL = 顶级节点
    item_name                VARCHAR(200)    NOT NULL,
    header_row               SMALLINT        NOT NULL DEFAULT 1,   -- 表头所在行 (1或2)
    col_index                INT             NOT NULL DEFAULT 1,   -- 起始列序号
    row_span                 SMALLINT        NOT NULL DEFAULT 1,
    col_span                 SMALLINT        NOT NULL DEFAULT 1,
    is_leaf                  SMALLINT        NOT NULL DEFAULT 1,   -- 0=分组 1=叶子(填报)
    value_type               VARCHAR(30)     NOT NULL DEFAULT 'text',
                                                                   -- text/number/date/select/attachment
    unit                     VARCHAR(50),
    placeholder              VARCHAR(500),
    require_attachment       SMALLINT        NOT NULL DEFAULT 0,   -- 0=不需要 1=必须 2=建议
    format_template_file_id  BIGINT,                               -- 格式模板标记（非空=已上传）
    format_template_url      VARCHAR(1000),                        -- MinIO 格式模板访问 URL
    format_template_name     VARCHAR(500),                         -- 格式模板原始文件名
    sort_num                 INT             NOT NULL DEFAULT 0,
    create_user              BIGINT,
    create_time              TIMESTAMP,
    update_user              BIGINT,
    update_time              TIMESTAMP,
    del_flag                 SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_wr_template_item PRIMARY KEY (id)
);

COMMENT ON TABLE  wr_template_item                      IS '模板字段/表头节点';
COMMENT ON COLUMN wr_template_item.parent_id            IS 'NULL=顶级, 非NULL=子节点';
COMMENT ON COLUMN wr_template_item.is_leaf              IS '0=分组节点(不填报) 1=叶子节点(实际填报)';
COMMENT ON COLUMN wr_template_item.require_attachment   IS '0=不需要 1=必须上传 2=建议上传';
COMMENT ON COLUMN wr_template_item.value_type           IS 'text/number/date/select/attachment';

CREATE INDEX IF NOT EXISTS idx_wr_template_item_tid ON wr_template_item(template_id);
CREATE INDEX IF NOT EXISTS idx_wr_template_item_pid ON wr_template_item(parent_id);

-- ---------------------------------------------------
-- 3. 上报任务表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS wr_task (
    id              BIGINT          NOT NULL,
    task_name       VARCHAR(300)    NOT NULL,
    template_id     BIGINT          NOT NULL,
    stat_year       VARCHAR(10),
    deadline        TIMESTAMP,
    status          SMALLINT        NOT NULL DEFAULT 0,  -- 0=草稿 1=进行中 2=已结束
    remark          TEXT,
    create_user     BIGINT,
    create_time     TIMESTAMP,
    update_user     BIGINT,
    update_time     TIMESTAMP,
    del_flag        SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_wr_task PRIMARY KEY (id)
);

COMMENT ON TABLE  wr_task          IS '上报任务';
COMMENT ON COLUMN wr_task.status   IS '状态: 0=草稿 1=进行中 2=已结束';

CREATE INDEX IF NOT EXISTS idx_wr_task_status ON wr_task(status);

-- ---------------------------------------------------
-- 4. 上报记录表（每个机构×每个任务 = 1条记录）
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS wr_record (
    id              BIGINT          NOT NULL,
    task_id         BIGINT          NOT NULL,
    template_id     BIGINT          NOT NULL,
    org_id          BIGINT          NOT NULL,             -- 机构ID (来自LoginUser.organizationId)
    org_name        VARCHAR(200),                         -- 机构名称快照
    status          SMALLINT        NOT NULL DEFAULT 1,   -- 1=草稿 2=待审核 3=审核通过 4=退回
    submit_user     BIGINT,
    submit_time     TIMESTAMP,
    audit_user      BIGINT,
    audit_time      TIMESTAMP,
    audit_result    SMALLINT,                             -- 3=通过 4=退回 (与status保持一致)
    audit_remark    TEXT,
    create_user     BIGINT,
    create_time     TIMESTAMP,
    update_user     BIGINT,
    update_time     TIMESTAMP,
    del_flag        SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_wr_record PRIMARY KEY (id)
);

COMMENT ON TABLE  wr_record             IS '上报记录';
COMMENT ON COLUMN wr_record.status      IS '1=草稿 2=待审核 3=审核通过 4=退回';
COMMENT ON COLUMN wr_record.org_id      IS '机构ID, 来自LoginUser.organizationId';

-- 同一机构在同一任务下只有一条有效记录
CREATE UNIQUE INDEX IF NOT EXISTS uq_wr_record_task_org
    ON wr_record(task_id, org_id)
    WHERE del_flag = 0;

CREATE INDEX IF NOT EXISTS idx_wr_record_task ON wr_record(task_id);
CREATE INDEX IF NOT EXISTS idx_wr_record_org  ON wr_record(org_id);

-- ---------------------------------------------------
-- 5. 上报数据值表（EAV / KV 模型）
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS wr_record_value (
    id              BIGINT          NOT NULL,
    record_id       BIGINT          NOT NULL,
    template_id     BIGINT          NOT NULL,
    item_id         BIGINT          NOT NULL,             -- 对应 wr_template_item.id
    row_index       INT             NOT NULL DEFAULT 1,   -- 表格行序号 (表单型固定为1)
    cell_value      TEXT,
    create_user     BIGINT,
    create_time     TIMESTAMP,
    update_time     TIMESTAMP,
    CONSTRAINT pk_wr_record_value PRIMARY KEY (id),
    -- 唯一键: 同一记录×同一字段×同一行 只有一个值
    CONSTRAINT uq_wr_record_value UNIQUE (record_id, item_id, row_index)
);

COMMENT ON TABLE  wr_record_value            IS '上报数据值 (EAV)';
COMMENT ON COLUMN wr_record_value.row_index  IS '行序号: 表单型固定=1, 表格型>=1';

CREATE INDEX IF NOT EXISTS idx_wr_record_value_rid ON wr_record_value(record_id);

-- ---------------------------------------------------
-- 6. 佐证附件表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS wr_attachment (
    id              BIGINT          NOT NULL,
    record_id       BIGINT          NOT NULL,
    item_id         BIGINT,                               -- NULL=整体佐证, 非NULL=挂接到具体节点
    file_id         BIGINT,                               -- Roses框架 sys_file_info.id
    attach_name     VARCHAR(500),
    attach_path     VARCHAR(1000),
    attach_size     BIGINT,
    attach_type     VARCHAR(50),
    create_user     BIGINT,
    create_time     TIMESTAMP,
    del_flag        SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_wr_attachment PRIMARY KEY (id)
);

COMMENT ON TABLE  wr_attachment          IS '佐证附件';
COMMENT ON COLUMN wr_attachment.item_id  IS 'NULL=整体佐证, 非NULL=挂接到对应节点';
COMMENT ON COLUMN wr_attachment.file_id  IS 'Roses文件框架 sys_file_info.id';

CREATE INDEX IF NOT EXISTS idx_wr_attachment_rid ON wr_attachment(record_id);
