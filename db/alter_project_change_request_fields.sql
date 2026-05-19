-- 添加项目变更申请相关字段
-- 执行顺序：第6阶段，在所有基础表创建后执行

USE kbd_pm_system;

-- 为 project_change_request 表添加缺失的字段
ALTER TABLE project_change_request
  ADD COLUMN target_milestone_id BIGINT UNSIGNED NULL COMMENT '目标里程碑ID（里程碑调整时使用）',
  ADD COLUMN target_milestone_planned_date DATE NULL COMMENT '目标里程碑新计划日期',
  ADD COLUMN previous_budget_amount DECIMAL(18,2) NULL COMMENT '变更前预算金额',
  ADD COLUMN requested_budget_amount DECIMAL(18,2) NULL COMMENT '申请预算金额',
  ADD COLUMN new_pm_user_id BIGINT UNSIGNED NULL COMMENT '新PM用户ID（负责人变更时使用）',
  ADD COLUMN asset_disposal_confirmed BOOLEAN DEFAULT FALSE COMMENT '资产处置确认（终止时使用）',
  ADD COLUMN archive_confirmed BOOLEAN DEFAULT FALSE COMMENT '归档确认（终止时使用）',
  ADD CONSTRAINT fk_change_target_milestone FOREIGN KEY (target_milestone_id) REFERENCES milestone_def(id),
  ADD CONSTRAINT fk_change_new_pm FOREIGN KEY (new_pm_user_id) REFERENCES iam_user(id);

-- 创建项目终止任务表
CREATE TABLE IF NOT EXISTS project_termination_task (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_id BIGINT UNSIGNED NOT NULL,
  change_request_id BIGINT UNSIGNED NULL,
  task_code VARCHAR(64) NOT NULL COMMENT '任务代码，如 ASSET_DISPOSAL, DOCUMENT_ARCHIVE',
  task_description TEXT NOT NULL COMMENT '任务详细描述',
  status ENUM('OPEN','COMPLETED','OVERDUE') NOT NULL DEFAULT 'OPEN' COMMENT '任务状态',
  due_date DATE NULL COMMENT '截止日期',
  completed_at DATETIME(3) NULL COMMENT '完成时间',
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_termination_project (project_id),
  KEY idx_termination_status (status),
  CONSTRAINT fk_termination_project FOREIGN KEY (project_id) REFERENCES project(id),
  CONSTRAINT fk_termination_change_request FOREIGN KEY (change_request_id) REFERENCES project_change_request(id)
) ENGINE=InnoDB COMMENT='项目终止任务清单';

-- 为 project_budget_plan 表添加版本查询方法支持
ALTER TABLE project_budget_plan
  ADD KEY idx_budget_plan_project_version (project_id, version_no);