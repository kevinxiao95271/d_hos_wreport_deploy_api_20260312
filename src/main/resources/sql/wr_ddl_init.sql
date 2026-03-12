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
CREATE INDEX IF NOT EXISTS idx_wr_task_status ON wr_task(status);
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
CREATE INDEX IF NOT EXISTS idx_wr_record_value_rid ON wr_record_value(record_id);
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
