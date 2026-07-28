-- 通知系统迁移脚本
-- 创建 notification 表

CREATE TABLE IF NOT EXISTS notification (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  recipient_user_id BIGINT NOT NULL COMMENT '接收人ID（对应 iam_user.id）',
  type VARCHAR(32) NOT NULL COMMENT '通知类型: PROJECT_COMPLETION / DELIVERABLE_UPLOADED / REVIEW_SUBMITTED / REVIEW_DECIDED / MILESTONE_APPROVED / REVIEW_REJECTED',
  title VARCHAR(256) NOT NULL COMMENT '通知标题',
  content TEXT COMMENT '通知内容',
  project_id BIGINT COMMENT '关联项目ID',
  milestone_code VARCHAR(8) COMMENT '关联里程碑代码',
  related_user_id BIGINT COMMENT '关联操作人ID',
  is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读',
  is_todo TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否作为待办持久展示',
  is_done TINYINT(1) NOT NULL DEFAULT 0 COMMENT '待办是否已完成',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  INDEX idx_recipient_read (recipient_user_id, is_read),
  INDEX idx_recipient_todo (recipient_user_id, is_todo, is_done),
  INDEX idx_recipient_created (recipient_user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

ALTER TABLE notification
  ADD COLUMN IF NOT EXISTS is_todo TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否作为待办持久展示';

ALTER TABLE notification
  ADD COLUMN IF NOT EXISTS is_done TINYINT(1) NOT NULL DEFAULT 0 COMMENT '待办是否已完成';

CREATE INDEX idx_recipient_todo ON notification (recipient_user_id, is_todo, is_done);