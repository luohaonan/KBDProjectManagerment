SET @is_todo_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'notification'
    AND column_name = 'is_todo'
);

SET @alter_is_todo_sql := IF(
  @is_todo_exists = 0,
  'ALTER TABLE notification ADD COLUMN is_todo TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否作为待办持久展示''',
  'SELECT 1'
);

PREPARE stmt FROM @alter_is_todo_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @is_done_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'notification'
    AND column_name = 'is_done'
);

SET @alter_is_done_sql := IF(
  @is_done_exists = 0,
  'ALTER TABLE notification ADD COLUMN is_done TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''待办是否已完成''',
  'SELECT 1'
);

PREPARE stmt FROM @alter_is_done_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'notification'
    AND index_name = 'idx_recipient_todo'
);

SET @create_idx_sql := IF(
  @idx_exists = 0,
  'CREATE INDEX idx_recipient_todo ON notification (recipient_user_id, is_todo, is_done)',
  'SELECT 1'
);

PREPARE stmt FROM @create_idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @wf_budget_warning_threshold_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'wf_process_definition'
    AND column_name = 'budget_warning_threshold'
);

SET @alter_wf_budget_warning_threshold_sql := IF(
  @wf_budget_warning_threshold_exists = 0,
  'ALTER TABLE wf_process_definition ADD COLUMN budget_warning_threshold DECIMAL(5,2) NULL COMMENT ''预算预警阈值(%)''',
  'SELECT 1'
);

PREPARE stmt FROM @alter_wf_budget_warning_threshold_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @project_change_adjustment_amount_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'project_change_request'
    AND column_name = 'adjustment_amount'
);

SET @alter_project_change_adjustment_amount_sql := IF(
  @project_change_adjustment_amount_exists = 0,
  'ALTER TABLE project_change_request ADD COLUMN adjustment_amount DECIMAL(18,2) NULL COMMENT ''本次预算调整增减金额''',
  'SELECT 1'
);

PREPARE stmt FROM @alter_project_change_adjustment_amount_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @notification_milestone_code_len := (
  SELECT CHARACTER_MAXIMUM_LENGTH
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'notification'
    AND column_name = 'milestone_code'
  LIMIT 1
);

SET @alter_notification_milestone_code_sql := IF(
  @notification_milestone_code_len IS NOT NULL AND @notification_milestone_code_len < 64,
  'ALTER TABLE notification MODIFY COLUMN milestone_code VARCHAR(64) NULL COMMENT ''关联里程碑代码/流程节点编码''',
  'SELECT 1'
);

PREPARE stmt FROM @alter_notification_milestone_code_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE wf_process_definition
SET budget_warning_threshold = COALESCE(budget_warning_threshold, 80.00)
WHERE process_type = 'BUDGET';

UPDATE project_change_request
SET adjustment_amount = requested_budget_amount - COALESCE(previous_budget_amount, 0)
WHERE change_type = 'BUDGET'
  AND requested_budget_amount IS NOT NULL
  AND adjustment_amount IS NULL;

SET @project_budget_ledger_expense_category_type := (
  SELECT COLUMN_TYPE
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'project_budget_ledger'
    AND column_name = 'expense_category'
  LIMIT 1
);

SET @project_budget_ledger_expense_category_len := (
  SELECT CHARACTER_MAXIMUM_LENGTH
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'project_budget_ledger'
    AND column_name = 'expense_category'
  LIMIT 1
);

SET @alter_project_budget_ledger_expense_category_sql := IF(
  @project_budget_ledger_expense_category_type IS NOT NULL
  AND (
    LOWER(@project_budget_ledger_expense_category_type) LIKE 'enum(%'
    OR COALESCE(@project_budget_ledger_expense_category_len, 0) < 32
  ),
  'ALTER TABLE project_budget_ledger MODIFY COLUMN expense_category VARCHAR(32) NOT NULL COMMENT ''支出分类''',
  'SELECT 1'
);

PREPARE stmt FROM @alter_project_budget_ledger_expense_category_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 动态上传节点附件名称。幂等迁移，不删除或重命名任何既有槽位/历史文档。
SET @wf_node_deliverable_name_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'wf_process_node'
    AND column_name = 'deliverable_name'
);

SET @alter_wf_node_deliverable_name_sql := IF(
  @wf_node_deliverable_name_exists = 0,
  'ALTER TABLE wf_process_node ADD COLUMN deliverable_name VARCHAR(256) NULL COMMENT ''上传节点配置的附件名称'' AFTER deliverable_slot_code',
  'SELECT 1'
);

PREPARE stmt FROM @alter_wf_node_deliverable_name_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 用现有槽位名称回填上传节点，保持部署前审批及上传配置不变。
UPDATE wf_process_node n
JOIN wf_process_definition p ON p.id = n.process_definition_id
JOIN milestone_deliverable_def d
  ON d.milestone_code = p.milestone_code
 AND d.slot_code = n.deliverable_slot_code
SET n.deliverable_name = d.slot_name
WHERE (n.is_uploader = 1 OR n.node_type = 'UPLOAD')
  AND n.deliverable_name IS NULL;
