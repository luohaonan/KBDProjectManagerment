-- ============================================================
-- 第🔟阶段：评审审批工作流 - 添加评审审批相关表和字段
-- 用于支持：立项评审、里程碑评审的审批流程
-- ============================================================

USE `kbd_pm_system`;

-- 1. project 表添加评审相关字段
ALTER TABLE project
  ADD COLUMN initiator_user_id BIGINT UNSIGNED NULL COMMENT '发起人（立项申请人）' AFTER pm_user_id,
  ADD COLUMN review_status VARCHAR(32) NULL COMMENT '评审状态：PENDING_REVIEW/IN_REVIEW/APPROVED/REJECTED' AFTER status,
  ADD COLUMN review_submitted_at DATETIME(3) NULL COMMENT '评审提交时间' AFTER review_status;

-- 2. 创建评审审批记录表（用于存储每次评审的审批流程）
CREATE TABLE IF NOT EXISTS review_approval (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_id BIGINT UNSIGNED NOT NULL COMMENT '项目ID',
  project_milestone_id BIGINT UNSIGNED NOT NULL COMMENT '项目里程碑ID',
  wf_instance_id BIGINT UNSIGNED NULL COMMENT '工作流实例ID',
  submitter_user_id BIGINT UNSIGNED NULL COMMENT '提交人（发起人）',
  submit_comment TEXT NULL COMMENT '提交备注',
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/SUBMITTED/APPROVED/REJECTED',
  submitted_at DATETIME(3) NULL COMMENT '提交时间',
  finished_at DATETIME(3) NULL COMMENT '完成时间',
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_ra_project (project_id),
  KEY idx_ra_milestone (project_milestone_id),
  KEY idx_ra_status (status),
  CONSTRAINT fk_ra_project FOREIGN KEY (project_id) REFERENCES project(id),
  CONSTRAINT fk_ra_milestone FOREIGN KEY (project_milestone_id) REFERENCES project_milestone(id),
  CONSTRAINT fk_ra_wf_instance FOREIGN KEY (wf_instance_id) REFERENCES wf_instance(id),
  CONSTRAINT fk_ra_submitter FOREIGN KEY (submitter_user_id) REFERENCES iam_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 3. 创建评审审批人任务表（每个审批人的审批任务）
CREATE TABLE IF NOT EXISTS review_approval_task (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  review_approval_id BIGINT UNSIGNED NOT NULL COMMENT '评审审批记录ID',
  approver_user_id BIGINT UNSIGNED NOT NULL COMMENT '审批人用户ID',
  approver_role VARCHAR(64) NULL COMMENT '审批人角色',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '审批顺序',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED',
  decision VARCHAR(32) NULL COMMENT '决策：APPROVED/REJECTED',
  opinion TEXT NULL COMMENT '审批意见',
  decided_at DATETIME(3) NULL COMMENT '决策时间',
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_rat_approval (review_approval_id),
  KEY idx_rat_approver (approver_user_id, status),
  CONSTRAINT fk_rat_approval FOREIGN KEY (review_approval_id) REFERENCES review_approval(id),
  CONSTRAINT fk_rat_approver FOREIGN KEY (approver_user_id) REFERENCES iam_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 4. 创建评审记录视图表（用于展示评审历史）
CREATE TABLE IF NOT EXISTS review_record (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_id BIGINT UNSIGNED NOT NULL COMMENT '项目ID',
  project_milestone_id BIGINT UNSIGNED NOT NULL COMMENT '项目里程碑ID',
  review_approval_id BIGINT UNSIGNED NULL COMMENT '关联的审批记录ID',
  action VARCHAR(32) NOT NULL COMMENT '操作类型：SUBMIT/APPROVE/REJECT/SAVE_DRAFT',
  actor_user_id BIGINT UNSIGNED NULL COMMENT '操作人',
  actor_role VARCHAR(64) NULL COMMENT '操作人角色',
  result VARCHAR(32) NULL COMMENT '结果：PASS/FAIL/SUBMITTED/DRAFT_SAVED',
  opinion TEXT NULL COMMENT '意见',
  action_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '操作时间',
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_rr_project (project_id, action_at),
  KEY idx_rr_milestone (project_milestone_id),
  KEY idx_rr_actor (actor_user_id),
  CONSTRAINT fk_rr_project FOREIGN KEY (project_id) REFERENCES project(id),
  CONSTRAINT fk_rr_milestone FOREIGN KEY (project_milestone_id) REFERENCES project_milestone(id),
  CONSTRAINT fk_rr_approval FOREIGN KEY (review_approval_id) REFERENCES review_approval(id),
  CONSTRAINT fk_rr_actor FOREIGN KEY (actor_user_id) REFERENCES iam_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 5. 添加评审相关权限
INSERT IGNORE INTO `permission` (`name`, `description`) VALUES
  ('PERMISSION_REVIEW_INITIATION', '评审立项申请'),
  ('PERMISSION_APPROVE_INITIATION', '审批立项申请'),
  ('PERMISSION_VIEW_REVIEW_RECORD', '查看评审记录');

-- 6. 为角色分配评审权限
-- PMC 拥有审批立项和查看评审记录权限
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `role` r, `permission` p
WHERE r.name = 'ROLE_PMC' AND p.name IN (
  'PERMISSION_APPROVE_INITIATION',
  'PERMISSION_VIEW_REVIEW_RECORD'
);

-- PM 拥有提交评审和查看评审记录权限
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `role` r, `permission` p
WHERE r.name = 'ROLE_PM' AND p.name IN (
  'PERMISSION_REVIEW_INITIATION',
  'PERMISSION_VIEW_REVIEW_RECORD'
);

-- 效率部门和部门负责人拥有查看评审记录权限
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `role` r, `permission` p
WHERE r.name IN ('ROLE_EFFICIENCY', 'ROLE_DEPT_HEAD') AND p.name = 'PERMISSION_VIEW_REVIEW_RECORD';
