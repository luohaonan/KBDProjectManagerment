-- ============================================================
-- 立项申请审批表 - 用于项目立项申请审批流程
-- ============================================================

USE `kbd_pm_system`;

-- 1. project 表添加立项申请相关字段
ALTER TABLE project
  ADD COLUMN initiation_status VARCHAR(32) NULL COMMENT '立项状态：null(未申请)/SUBMITTED(已提交)/APPROVED(已通过)/REJECTED(已驳回)' AFTER review_submitted_at,
  ADD COLUMN initiation_submitted_at DATETIME(3) NULL COMMENT '立项申请提交时间' AFTER initiation_status,
  ADD COLUMN initiation_application TEXT NULL COMMENT '立项申请信息（项目经理填写的申请内容）' AFTER initiation_submitted_at;

-- 2. 创建立项申请审批记录表
CREATE TABLE IF NOT EXISTS initiation_approval (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_id BIGINT UNSIGNED NOT NULL COMMENT '项目ID',
  submitter_user_id BIGINT UNSIGNED NULL COMMENT '提交人（项目经理）',
  application_content TEXT NULL COMMENT '立项申请内容',
  status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED' COMMENT '状态：SUBMITTED/APPROVED/REJECTED',
  submitted_at DATETIME(3) NULL COMMENT '提交时间',
  finished_at DATETIME(3) NULL COMMENT '完成时间（全部审批完成）',
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_ia_project (project_id),
  KEY idx_ia_status (status),
  CONSTRAINT fk_ia_project FOREIGN KEY (project_id) REFERENCES project(id),
  CONSTRAINT fk_ia_submitter FOREIGN KEY (submitter_user_id) REFERENCES iam_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 3. 创建立项申请审批人任务表
CREATE TABLE IF NOT EXISTS initiation_approval_task (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  initiation_approval_id BIGINT UNSIGNED NOT NULL COMMENT '立项申请审批记录ID',
  approver_user_id BIGINT UNSIGNED NOT NULL COMMENT '审批人用户ID（PMC成员）',
  approver_role VARCHAR(64) NULL COMMENT '审批人角色',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '审批顺序',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED',
  decision VARCHAR(32) NULL COMMENT '决策：APPROVED/REJECTED',
  opinion TEXT NULL COMMENT '审批意见',
  decided_at DATETIME(3) NULL COMMENT '决策时间',
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_iat_approval (initiation_approval_id),
  KEY idx_iat_approver (approver_user_id, status),
  CONSTRAINT fk_iat_approval FOREIGN KEY (initiation_approval_id) REFERENCES initiation_approval(id),
  CONSTRAINT fk_iat_approver FOREIGN KEY (approver_user_id) REFERENCES iam_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
