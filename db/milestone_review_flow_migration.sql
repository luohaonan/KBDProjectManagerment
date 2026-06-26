-- ============================================================
-- KBD PMS - 里程碑评审流程迁移脚本（幂等版本 - 可安全重复执行）
-- 基于 policy_extracted.md 第74-124行流程逻辑
-- ============================================================

USE `kbd_pm_system`;

-- ============================================================
-- 辅助：创建"添加列（如果不存在）"的存储过程
-- ============================================================
DROP PROCEDURE IF EXISTS `add_column_if_not_exists`;

DELIMITER //

CREATE PROCEDURE `add_column_if_not_exists`(
    IN table_name_param VARCHAR(128),
    IN column_name_param VARCHAR(128),
    IN column_definition VARCHAR(1024)
)
BEGIN
    DECLARE col_count INT;
    SELECT COUNT(*) INTO col_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = table_name_param
      AND COLUMN_NAME = column_name_param;
    IF col_count = 0 THEN
        SET @ddl = CONCAT('ALTER TABLE `', table_name_param, '` ADD COLUMN `', column_name_param, '` ', column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //

DELIMITER ;

-- ============================================================
-- 1. 为 review_approval_task 添加 step_code 字段
-- ============================================================
CALL add_column_if_not_exists('review_approval_task', 'step_code',
    "varchar(32) DEFAULT NULL COMMENT '审批步骤代码' AFTER `approver_role`");

CALL add_column_if_not_exists('review_approval_task', 'deliverable_slot_code',
    "varchar(64) DEFAULT NULL COMMENT '交付物槽位代码' AFTER `step_code`");

-- ============================================================
-- 2. 为 document 表添加 deliverable_slot_code 字段
-- ============================================================
CALL add_column_if_not_exists('document', 'deliverable_slot_code',
    "varchar(64) DEFAULT NULL COMMENT '交付物槽位代码' AFTER `milestone_phase`");

-- ============================================================
-- 3. 为 project_change_request 表添加效率管理部审批字段
-- ============================================================
CALL add_column_if_not_exists('project_change_request', 'attachment_uri',
    "varchar(1024) DEFAULT NULL COMMENT '变更附件路径' AFTER `reason_text`");

CALL add_column_if_not_exists('project_change_request', 'efficiency_approver_id',
    "bigint unsigned DEFAULT NULL COMMENT '效率管理部审批人' AFTER `status`");

CALL add_column_if_not_exists('project_change_request', 'efficiency_opinion',
    "text COMMENT '效率管理部审批意见' AFTER `efficiency_approver_id`");

CALL add_column_if_not_exists('project_change_request', 'efficiency_decided_at',
    "datetime(3) DEFAULT NULL COMMENT '效率管理部审批时间' AFTER `efficiency_opinion`");

-- 添加效率管理部审批人的外键（如果不存在）
SET @fk_exists = (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project_change_request'
    AND CONSTRAINT_NAME = 'fk_change_efficiency_approver');
SET @add_fk = IF(@fk_exists = 0,
    'ALTER TABLE `project_change_request` ADD KEY `fk_change_efficiency_approver` (`efficiency_approver_id`), ADD CONSTRAINT `fk_change_efficiency_approver` FOREIGN KEY (`efficiency_approver_id`) REFERENCES `iam_user` (`id`)',
    'SELECT "fk_change_efficiency_approver already exists"');
PREPARE stmt FROM @add_fk; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 更新status枚举（使用 MODIFY COLUMN 覆盖）  
ALTER TABLE `project_change_request`
  MODIFY COLUMN `status` enum('DRAFT','SUBMITTED','EFFICIENCY_APPROVED','EFFICIENCY_REJECTED','PMC_APPROVED','PMC_REJECTED','APPROVED','REJECTED','CANCELLED') NOT NULL DEFAULT 'DRAFT';

-- ============================================================
-- 4. 更新 project_termination_task 表的 status 枚举
-- ============================================================
ALTER TABLE `project_termination_task`
  MODIFY COLUMN `status` enum('OPEN','COMPLETED','OVERDUE','CANCELLED') NOT NULL DEFAULT 'OPEN' COMMENT '任务状态';

-- ============================================================
-- 5. 新增 project_termination_request 表（幂等：先删后建）
-- ============================================================
DROP TABLE IF EXISTS `project_termination_request`;
CREATE TABLE `project_termination_request` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `project_id` bigint unsigned NOT NULL COMMENT '项目ID',
  `requested_by` bigint unsigned DEFAULT NULL COMMENT '发起人（项目经理）',
  `termination_reason` text COMMENT '终止原因',
  `attachment_uri` varchar(1024) DEFAULT NULL COMMENT '终止建议附件路径',
  `status` enum('DRAFT','SUBMITTED','EFFICIENCY_APPROVED','EFFICIENCY_REJECTED','PMC_APPROVED','PMC_REJECTED','COMPLETED') NOT NULL DEFAULT 'DRAFT' COMMENT '终止流程状态',
  `efficiency_approver_id` bigint unsigned DEFAULT NULL COMMENT '效率管理部审批人',
  `efficiency_opinion` text COMMENT '效率管理部审批意见',
  `efficiency_decided_at` datetime(3) DEFAULT NULL COMMENT '效率管理部审批时间',
  `pmc_approver_id` bigint unsigned DEFAULT NULL COMMENT 'PMC审批人',
  `pmc_opinion` text COMMENT 'PMC审批意见',
  `pmc_decided_at` datetime(3) DEFAULT NULL COMMENT 'PMC审批时间',
  `summary_report_uri` varchar(1024) DEFAULT NULL COMMENT '项目总结报告附件路径',
  `asset_disposal_confirmed` tinyint(1) NOT NULL DEFAULT '0' COMMENT '资产处置确认',
  `archive_confirmed` tinyint(1) NOT NULL DEFAULT '0' COMMENT '归档确认',
  `submitted_at` datetime(3) DEFAULT NULL COMMENT '提交时间',
  `finished_at` datetime(3) DEFAULT NULL COMMENT '完成时间',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_termination_project` (`project_id`),
  KEY `idx_termination_status` (`status`),
  KEY `fk_termination_requested_by` (`requested_by`),
  CONSTRAINT `fk_termination_req_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `fk_termination_requested_by` FOREIGN KEY (`requested_by`) REFERENCES `iam_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目终止申请表';

-- ============================================================
-- 6. 填充 milestone_dept_role 种子数据（幂等：先清空）
-- ============================================================
TRUNCATE TABLE `milestone_dept_role`;

INSERT INTO `milestone_dept_role` (`milestone_def_id`, `dept_id`, `role_type`, `is_active`) VALUES
((SELECT id FROM milestone_def WHERE milestone_code='G0'), 4, 'DEPT_EXECUTOR', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G0'), 4, 'DEPT_HEAD', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G0'), 11, 'ROLE_PM', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G0'), 7, 'DEPT_HEAD', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G0'), 10, 'ROLE_PMC', 1);

INSERT INTO `milestone_dept_role` (`milestone_def_id`, `dept_id`, `role_type`, `is_active`) VALUES
((SELECT id FROM milestone_def WHERE milestone_code='G1'), 1, 'DEPT_EXECUTOR', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G1'), 4, 'DEPT_EXECUTOR', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G1'), 1, 'DEPT_HEAD', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G1'), 4, 'DEPT_HEAD', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G1'), 11, 'ROLE_PM', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G1'), 7, 'DEPT_HEAD', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G1'), 11, 'ROLE_PM_DECISION', 1);

INSERT INTO `milestone_dept_role` (`milestone_def_id`, `dept_id`, `role_type`, `is_active`) VALUES
((SELECT id FROM milestone_def WHERE milestone_code='G2'), 1, 'DEPT_EXECUTOR', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G2'), 4, 'DEPT_EXECUTOR', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G2'), 1, 'DEPT_HEAD', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G2'), 4, 'DEPT_HEAD', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G2'), 11, 'ROLE_PM', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G2'), 7, 'DEPT_HEAD', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G2'), 11, 'ROLE_PM_DECISION', 1);

INSERT INTO `milestone_dept_role` (`milestone_def_id`, `dept_id`, `role_type`, `is_active`) VALUES
((SELECT id FROM milestone_def WHERE milestone_code='G3'), 1, 'DEPT_EXECUTOR', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G3'), 1, 'DEPT_HEAD', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G3'), 11, 'ROLE_PM', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G3'), 7, 'DEPT_HEAD', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G3'), 10, 'ROLE_PMC', 1);

INSERT INTO `milestone_dept_role` (`milestone_def_id`, `dept_id`, `role_type`, `is_active`) VALUES
((SELECT id FROM milestone_def WHERE milestone_code='G4'), 1, 'DEPT_EXECUTOR', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G4'), 4, 'DEPT_EXECUTOR', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G4'), 2, 'DEPT_EXECUTOR', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G4'), 1, 'DEPT_HEAD', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G4'), 4, 'DEPT_HEAD', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G4'), 2, 'DEPT_HEAD', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G4'), 11, 'ROLE_PM', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G4'), 7, 'DEPT_HEAD', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G4'), 10, 'ROLE_PMC', 1);

INSERT INTO `milestone_dept_role` (`milestone_def_id`, `dept_id`, `role_type`, `is_active`) VALUES
((SELECT id FROM milestone_def WHERE milestone_code='G5'), 7, 'DEPT_EXECUTOR', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G5'), 7, 'DEPT_HEAD', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G5'), 11, 'ROLE_PM', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G5'), 10, 'ROLE_PMC', 1);

INSERT INTO `milestone_dept_role` (`milestone_def_id`, `dept_id`, `role_type`, `is_active`) VALUES
((SELECT id FROM milestone_def WHERE milestone_code='G6'), 3, 'DEPT_EXECUTOR', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G6'), 3, 'DEPT_HEAD', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G6'), 11, 'ROLE_PM', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G6'), 7, 'DEPT_HEAD', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G6'), 10, 'ROLE_PMC', 1);

INSERT INTO `milestone_dept_role` (`milestone_def_id`, `dept_id`, `role_type`, `is_active`) VALUES
((SELECT id FROM milestone_def WHERE milestone_code='G7'), 3, 'DEPT_EXECUTOR', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G7'), 3, 'DEPT_HEAD', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G7'), 11, 'ROLE_PM', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G7'), 7, 'DEPT_HEAD', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G7'), 10, 'ROLE_PMC', 1);

INSERT INTO `milestone_dept_role` (`milestone_def_id`, `dept_id`, `role_type`, `is_active`) VALUES
((SELECT id FROM milestone_def WHERE milestone_code='G8'), 3, 'DEPT_EXECUTOR', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G8'), 3, 'DEPT_HEAD', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G8'), 11, 'ROLE_PM', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G8'), 7, 'DEPT_HEAD', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G8'), 10, 'ROLE_PMC', 1);

INSERT INTO `milestone_dept_role` (`milestone_def_id`, `dept_id`, `role_type`, `is_active`) VALUES
((SELECT id FROM milestone_def WHERE milestone_code='G9'), 7, 'DEPT_EXECUTOR', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G9'), 7, 'DEPT_HEAD', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G9'), 11, 'ROLE_PM', 1),
((SELECT id FROM milestone_def WHERE milestone_code='G9'), 10, 'ROLE_PMC', 1);

-- ============================================================
-- 清理存储过程
-- ============================================================
DROP PROCEDURE IF EXISTS `add_column_if_not_exists`;

-- ============================================================
-- 完成!
-- ============================================================