-- KBD PMS dynamic deliverables migration
-- Safe to run repeatedly. This migration does not delete document records,
-- deliverable definitions, or uploaded files.

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

UPDATE wf_process_node n
JOIN wf_process_definition p
  ON p.id = n.process_definition_id
JOIN milestone_deliverable_def d
  ON d.milestone_code = p.milestone_code
 AND d.slot_code = n.deliverable_slot_code
SET n.deliverable_name = d.slot_name
WHERE (n.is_uploader = 1 OR n.node_type = 'UPLOAD')
  AND n.deliverable_name IS NULL;

SHOW COLUMNS FROM wf_process_node LIKE 'deliverable_name';

SELECT
  id,
  node_code,
  node_name,
  approver_value,
  deliverable_slot_code,
  deliverable_name
FROM wf_process_node
WHERE is_uploader = 1 OR node_type = 'UPLOAD'
ORDER BY process_definition_id, sort_order, id;