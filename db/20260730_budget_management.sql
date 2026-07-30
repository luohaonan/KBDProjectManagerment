USE `kbd_pm_system`;

START TRANSACTION;

INSERT INTO permission (`name`, `description`, `created_at`, `updated_at`)
SELECT 'PERMISSION_BUDGET_VIEW', '预算管理查看权限', NOW(3), NOW(3)
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE `name` = 'PERMISSION_BUDGET_VIEW');

INSERT INTO permission (`name`, `description`, `created_at`, `updated_at`)
SELECT 'PERMISSION_BUDGET_MANAGE', '预算管理操作与预算调整发起权限', NOW(3), NOW(3)
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE `name` = 'PERMISSION_BUDGET_MANAGE');

INSERT INTO wf_process_definition (`process_type`, `milestone_code`, `description`, `is_active`, `created_at`, `updated_at`)
SELECT 'BUDGET', NULL, '预算变更审批流程', 1, NOW(3), NOW(3)
WHERE NOT EXISTS (
  SELECT 1 FROM wf_process_definition WHERE process_type = 'BUDGET' AND milestone_code IS NULL
);

SET @budget_def_id := (
  SELECT id FROM wf_process_definition WHERE process_type = 'BUDGET' AND milestone_code IS NULL LIMIT 1
);

INSERT INTO wf_process_node (
  process_definition_id, node_code, node_name, node_type, approver_rule, approver_value,
  decision_type, is_uploader, deliverable_slot_code, position_x, position_y, sort_order,
  created_at, updated_at
)
SELECT @budget_def_id, 'BUDGET_EFF_APPROVE', '效率管理部负责人审批', 'DEPT_HEAD_APPROVE', 'DEPT_HEAD', '6',
       'APPROVE_REJECT', 0, NULL, 220, 80, 1, NOW(3), NOW(3)
WHERE NOT EXISTS (
  SELECT 1 FROM wf_process_node WHERE process_definition_id = @budget_def_id AND node_code = 'BUDGET_EFF_APPROVE'
);

INSERT INTO wf_process_node (
  process_definition_id, node_code, node_name, node_type, approver_rule, approver_value,
  decision_type, is_uploader, deliverable_slot_code, position_x, position_y, sort_order,
  created_at, updated_at
)
SELECT @budget_def_id, 'BUDGET_PMC_APPROVE', 'PMC审批', 'DECISION', 'ROLE_PMC', NULL,
       'APPROVE_REJECT', 0, NULL, 420, 80, 2, NOW(3), NOW(3)
WHERE NOT EXISTS (
  SELECT 1 FROM wf_process_node WHERE process_definition_id = @budget_def_id AND node_code = 'BUDGET_PMC_APPROVE'
);

SET @budget_n1 := (SELECT id FROM wf_process_node WHERE process_definition_id = @budget_def_id AND node_code = 'BUDGET_EFF_APPROVE' LIMIT 1);
SET @budget_n2 := (SELECT id FROM wf_process_node WHERE process_definition_id = @budget_def_id AND node_code = 'BUDGET_PMC_APPROVE' LIMIT 1);

INSERT INTO wf_process_edge (process_definition_id, from_node_id, to_node_id, created_at)
SELECT @budget_def_id, @budget_n1, @budget_n2, NOW(3)
WHERE @budget_n1 IS NOT NULL AND @budget_n2 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM wf_process_edge WHERE process_definition_id = @budget_def_id AND from_node_id = @budget_n1 AND to_node_id = @budget_n2
  );

COMMIT;