-- 机构申请驳回 + 管理员处理
ALTER TABLE wr_record ADD COLUMN IF NOT EXISTS reject_apply_status SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE wr_record ADD COLUMN IF NOT EXISTS reject_apply_reason TEXT;
ALTER TABLE wr_record ADD COLUMN IF NOT EXISTS reject_apply_time TIMESTAMP;
ALTER TABLE wr_record ADD COLUMN IF NOT EXISTS reject_apply_handle_remark TEXT;
ALTER TABLE wr_record ADD COLUMN IF NOT EXISTS reject_apply_handle_time TIMESTAMP;

COMMENT ON COLUMN wr_record.reject_apply_status IS '驳回申请状态：0=无 1=待处理 2=已同意驳回 3=已拒绝申请';
COMMENT ON COLUMN wr_record.reject_apply_reason IS '机构申请驳回原因';
COMMENT ON COLUMN wr_record.reject_apply_time IS '机构申请驳回时间';
COMMENT ON COLUMN wr_record.reject_apply_handle_remark IS '管理员处理意见';
COMMENT ON COLUMN wr_record.reject_apply_handle_time IS '管理员处理时间';
