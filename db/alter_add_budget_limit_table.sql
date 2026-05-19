-- 添加缺失的budget_limit表
-- 执行顺序：第7阶段，在所有基础表创建后执行

USE kbd_pm_system;

-- 阶段预算限额（G0-G9）
CREATE TABLE IF NOT EXISTS budget_limit (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_id BIGINT UNSIGNED NOT NULL,
  milestone_code VARCHAR(4) NOT NULL,       -- G0, G1, ..., G9
  approved_budget DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  created_by BIGINT UNSIGNED NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_by BIGINT UNSIGNED NULL,
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_budget_limit_project_milestone (project_id, milestone_code),
  KEY idx_budget_limit_project (project_id),
  CONSTRAINT fk_budget_limit_project FOREIGN KEY (project_id) REFERENCES project(id),
  CONSTRAINT ck_budget_limit_budget CHECK (approved_budget >= 0),
  CONSTRAINT ck_budget_limit_milestone CHECK (milestone_code REGEXP '^G[0-9]$')
) ENGINE=InnoDB;