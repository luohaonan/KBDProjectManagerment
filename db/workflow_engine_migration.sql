-- ============================================================
-- KBD PMS - 可视化流程引擎迁移脚本
-- 基于 DAG（有向无环图）节点+连线模型
-- ============================================================
-- 使用说明：
--   mysql -h localhost -u root -p kbd_pm_system < db/workflow_engine_migration.sql
-- ============================================================

USE `kbd_pm_system`;

-- ============================================================
-- 0. 先清理外键约束（如果表已存在），再按子→父顺序 DROP
-- ============================================================
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `wf_process_edge`;
DROP TABLE IF EXISTS `wf_process_node`;
DROP TABLE IF EXISTS `wf_process_definition`;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 1. 流程定义表
-- ============================================================
CREATE TABLE `wf_process_definition` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `process_type` varchar(32) NOT NULL COMMENT '流程类型: MILESTONE/CHANGE/TERMINATION',
  `milestone_code` varchar(4) DEFAULT NULL COMMENT '里程碑代码(G0-G9)，仅MILESTONE类型使用',
  `description` varchar(255) DEFAULT NULL COMMENT '流程说明',
  `is_active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_process_type_milestone` (`process_type`, `milestone_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程定义表';

-- ============================================================
-- 2. 流程节点表
-- ============================================================
CREATE TABLE `wf_process_node` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `process_definition_id` bigint unsigned NOT NULL COMMENT '所属流程定义',
  `node_code` varchar(64) NOT NULL COMMENT '节点代码',
  `node_name` varchar(128) NOT NULL COMMENT '节点显示名',
  `node_type` varchar(32) NOT NULL COMMENT '节点类型: UPLOAD/DEPT_HEAD_APPROVE/ROLE_APPROVE/DECISION',
  `approver_rule` varchar(64) DEFAULT NULL COMMENT '审批人规则: DEPT_HEAD/ROLE_PM/ROLE_PMC/ROLE_COMPLIANCE/SPECIFIC_USER',
  `approver_value` varchar(128) DEFAULT NULL COMMENT '规则参数(dept_id/role_name/user_id逗号分隔)',
  `decision_type` varchar(32) DEFAULT NULL COMMENT '决策类型: GO_NO_GO/APPROVE_REJECT/NONE',
  `is_uploader` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否为上传节点(Step1)',
  `deliverable_slot_code` varchar(64) DEFAULT NULL COMMENT '交付物槽位代码',
  `position_x` int NOT NULL DEFAULT '0' COMMENT '画布X坐标',
  `position_y` int NOT NULL DEFAULT '0' COMMENT '画布Y坐标',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_process_def` (`process_definition_id`),
  CONSTRAINT `fk_node_process_def` FOREIGN KEY (`process_definition_id`) REFERENCES `wf_process_definition` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程节点表';

-- ============================================================
-- 3. 流程连线表
-- ============================================================
CREATE TABLE `wf_process_edge` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `process_definition_id` bigint unsigned NOT NULL COMMENT '所属流程定义',
  `from_node_id` bigint unsigned NOT NULL COMMENT '源节点',
  `to_node_id` bigint unsigned NOT NULL COMMENT '目标节点',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_from_node` (`from_node_id`),
  KEY `idx_to_node` (`to_node_id`),
  CONSTRAINT `fk_edge_process_def` FOREIGN KEY (`process_definition_id`) REFERENCES `wf_process_definition` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_edge_from_node` FOREIGN KEY (`from_node_id`) REFERENCES `wf_process_node` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_edge_to_node` FOREIGN KEY (`to_node_id`) REFERENCES `wf_process_node` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程连线表';

-- ============================================================
-- 4. 初始化默认里程碑审批流程 (G0)
-- ============================================================
INSERT INTO `wf_process_definition` (`process_type`, `milestone_code`, `description`) VALUES
('MILESTONE', 'G0', 'G0 项目立项里程碑审批流程'),
('MILESTONE', 'G1', 'G1 先导化合物确认里程碑审批流程'),
('MILESTONE', 'G2', 'G2 优选化合物里程碑审批流程'),
('MILESTONE', 'G3', 'G3 候选化合物提名(PCC)里程碑审批流程'),
('MILESTONE', 'G4', 'G4 临床前开发完成(GLP)里程碑审批流程'),
('MILESTONE', 'G5', 'G5 临床试验申请获批(IND)里程碑审批流程'),
('MILESTONE', 'G6', 'G6 临床Ⅰ期里程碑审批流程'),
('MILESTONE', 'G7', 'G7 临床Ⅱ期里程碑审批流程'),
('MILESTONE', 'G8', 'G8 临床Ⅲ期里程碑审批流程'),
('MILESTONE', 'G9', 'G9 新药上市申请获批(NDA)里程碑审批流程'),
('CHANGE', NULL, '项目变更审批流程'),
('TERMINATION', NULL, '项目终止审批流程');

-- ============================================================
-- 5. 初始化 G0 节点和连线
-- ============================================================
SET @g0_def_id = (SELECT id FROM wf_process_definition WHERE process_type='MILESTONE' AND milestone_code='G0');

INSERT INTO `wf_process_node` (`process_definition_id`, `node_code`, `node_name`, `node_type`, `approver_rule`, `approver_value`, `decision_type`, `is_uploader`, `position_x`, `position_y`, `sort_order`) VALUES
(@g0_def_id, 'UPLOAD_G0_INFO', '资讯部执行人上传', 'UPLOAD', 'DEPT_HEAD', '4', 'NONE', 1, 50, 80, 1),
(@g0_def_id, 'DEPT_HEAD_G0_INFO', '资讯部负责人审批', 'DEPT_HEAD_APPROVE', 'DEPT_HEAD', '4', 'APPROVE_REJECT', 0, 200, 80, 2),
(@g0_def_id, 'PM_TECH_G0', 'PM技术初评', 'ROLE_APPROVE', 'ROLE_PM', NULL, 'APPROVE_REJECT', 0, 350, 80, 3),
(@g0_def_id, 'COMPLIANCE_G0', '药政合规部意见', 'ROLE_APPROVE', 'ROLE_COMPLIANCE', '7', 'APPROVE_REJECT', 0, 500, 80, 4),
(@g0_def_id, 'PMC_DECISION_G0_1', 'PMC决策-1', 'DECISION', 'ROLE_PMC', NULL, 'GO_NO_GO', 0, 650, 60, 5),
(@g0_def_id, 'PMC_DECISION_G0_2', 'PMC决策-2', 'DECISION', 'ROLE_PMC', NULL, 'GO_NO_GO', 0, 650, 100, 5),
(@g0_def_id, 'PMC_DECISION_G0_3', 'PMC决策-3', 'DECISION', 'ROLE_PMC', NULL, 'GO_NO_GO', 0, 650, 140, 5);

SET @n1 = (SELECT id FROM wf_process_node WHERE process_definition_id=@g0_def_id AND node_code='UPLOAD_G0_INFO');
SET @n2 = (SELECT id FROM wf_process_node WHERE process_definition_id=@g0_def_id AND node_code='DEPT_HEAD_G0_INFO');
SET @n3 = (SELECT id FROM wf_process_node WHERE process_definition_id=@g0_def_id AND node_code='PM_TECH_G0');
SET @n4 = (SELECT id FROM wf_process_node WHERE process_definition_id=@g0_def_id AND node_code='COMPLIANCE_G0');
SET @n5 = (SELECT id FROM wf_process_node WHERE process_definition_id=@g0_def_id AND node_code='PMC_DECISION_G0_1');
SET @n6 = (SELECT id FROM wf_process_node WHERE process_definition_id=@g0_def_id AND node_code='PMC_DECISION_G0_2');
SET @n7 = (SELECT id FROM wf_process_node WHERE process_definition_id=@g0_def_id AND node_code='PMC_DECISION_G0_3');

INSERT INTO `wf_process_edge` (`process_definition_id`, `from_node_id`, `to_node_id`) VALUES
(@g0_def_id, @n1, @n2), (@g0_def_id, @n2, @n3), (@g0_def_id, @n3, @n4),
(@g0_def_id, @n4, @n5), (@g0_def_id, @n4, @n6), (@g0_def_id, @n4, @n7);

-- ============================================================
-- 6. 初始化 G1 节点和连线 (化学部+资讯部并行上传, 两负责人并行审批, PM内部评审)
-- ============================================================
SET @g1_def_id = (SELECT id FROM wf_process_definition WHERE process_type='MILESTONE' AND milestone_code='G1');

INSERT INTO `wf_process_node` (`process_definition_id`, `node_code`, `node_name`, `node_type`, `approver_rule`, `approver_value`, `decision_type`, `is_uploader`, `position_x`, `position_y`, `sort_order`) VALUES
(@g1_def_id, 'UPLOAD_G1_CHEM', '化学部执行人上传', 'UPLOAD', 'DEPT_HEAD', '1', 'NONE', 1, 50, 60, 1),
(@g1_def_id, 'UPLOAD_G1_INFO', '资讯部执行人上传', 'UPLOAD', 'DEPT_HEAD', '4', 'NONE', 1, 50, 120, 1),
(@g1_def_id, 'DEPT_HEAD_G1_CHEM', '化学部负责人审批', 'DEPT_HEAD_APPROVE', 'DEPT_HEAD', '1', 'APPROVE_REJECT', 0, 200, 60, 2),
(@g1_def_id, 'DEPT_HEAD_G1_INFO', '资讯部负责人审批', 'DEPT_HEAD_APPROVE', 'DEPT_HEAD', '4', 'APPROVE_REJECT', 0, 200, 120, 2),
(@g1_def_id, 'PM_TECH_G1', 'PM技术初评', 'ROLE_APPROVE', 'ROLE_PM', NULL, 'APPROVE_REJECT', 0, 350, 90, 3),
(@g1_def_id, 'COMPLIANCE_G1', '药政合规部意见', 'ROLE_APPROVE', 'ROLE_COMPLIANCE', '7', 'APPROVE_REJECT', 0, 500, 90, 4),
(@g1_def_id, 'PM_INTERNAL_G1', 'PM项目组内部评审', 'DECISION', 'ROLE_PM', NULL, 'GO_NO_GO', 0, 650, 90, 5);

SET @g1n1 = (SELECT id FROM wf_process_node WHERE process_definition_id=@g1_def_id AND node_code='UPLOAD_G1_CHEM');
SET @g1n2 = (SELECT id FROM wf_process_node WHERE process_definition_id=@g1_def_id AND node_code='UPLOAD_G1_INFO');
SET @g1n3 = (SELECT id FROM wf_process_node WHERE process_definition_id=@g1_def_id AND node_code='DEPT_HEAD_G1_CHEM');
SET @g1n4 = (SELECT id FROM wf_process_node WHERE process_definition_id=@g1_def_id AND node_code='DEPT_HEAD_G1_INFO');
SET @g1n5 = (SELECT id FROM wf_process_node WHERE process_definition_id=@g1_def_id AND node_code='PM_TECH_G1');
SET @g1n6 = (SELECT id FROM wf_process_node WHERE process_definition_id=@g1_def_id AND node_code='COMPLIANCE_G1');
SET @g1n7 = (SELECT id FROM wf_process_node WHERE process_definition_id=@g1_def_id AND node_code='PM_INTERNAL_G1');

INSERT INTO `wf_process_edge` (`process_definition_id`, `from_node_id`, `to_node_id`) VALUES
(@g1_def_id, @g1n1, @g1n3), (@g1_def_id, @g1n2, @g1n4),
(@g1_def_id, @g1n3, @g1n5), (@g1_def_id, @g1n4, @g1n5),
(@g1_def_id, @g1n5, @g1n6), (@g1_def_id, @g1n6, @g1n7);

-- ============================================================
-- 7. G2~G4, G6~G8 使用与G0/G1类似结构，此处用存储过程批量创建
-- ============================================================
DROP PROCEDURE IF EXISTS `create_simple_milestone_flow`;

DELIMITER //
CREATE PROCEDURE `create_simple_milestone_flow`(
    IN p_milestone_code VARCHAR(4),
    IN p_dept_name VARCHAR(64),
    IN p_dept_id VARCHAR(16),
    IN p_has_compliance BOOLEAN,
    IN p_use_pmc BOOLEAN
)
BEGIN
    DECLARE def_id BIGINT;
    DECLARE n1 BIGINT; DECLARE n2 BIGINT; DECLARE n3 BIGINT; DECLARE n4 BIGINT;
    DECLARE n5a BIGINT; DECLARE n5b BIGINT; DECLARE n5c BIGINT;
    SET def_id = (SELECT id FROM wf_process_definition WHERE process_type='MILESTONE' AND milestone_code=p_milestone_code);

    -- Step1: 上传
    INSERT INTO `wf_process_node` (`process_definition_id`, `node_code`, `node_name`, `node_type`, `approver_rule`, `approver_value`, `decision_type`, `is_uploader`, `position_x`, `position_y`, `sort_order`)
    VALUES (def_id, CONCAT('UPLOAD_', p_milestone_code), CONCAT(p_dept_name, '执行人上传'), 'UPLOAD', 'DEPT_HEAD', p_dept_id, 'NONE', 1, 50, 80, 1);
    SET n1 = LAST_INSERT_ID();

    -- Step2: 部门负责人
    INSERT INTO `wf_process_node` (`process_definition_id`, `node_code`, `node_name`, `node_type`, `approver_rule`, `approver_value`, `decision_type`, `is_uploader`, `position_x`, `position_y`, `sort_order`)
    VALUES (def_id, CONCAT('DEPT_HEAD_', p_milestone_code), CONCAT(p_dept_name, '负责人审批'), 'DEPT_HEAD_APPROVE', 'DEPT_HEAD', p_dept_id, 'APPROVE_REJECT', 0, 200, 80, 2);
    SET n2 = LAST_INSERT_ID();

    -- Step3: PM技术初评
    INSERT INTO `wf_process_node` (`process_definition_id`, `node_code`, `node_name`, `node_type`, `approver_rule`, `approver_value`, `decision_type`, `is_uploader`, `position_x`, `position_y`, `sort_order`)
    VALUES (def_id, CONCAT('PM_TECH_', p_milestone_code), 'PM技术初评', 'ROLE_APPROVE', 'ROLE_PM', NULL, 'APPROVE_REJECT', 0, 350, 80, 3);
    SET n3 = LAST_INSERT_ID();

    INSERT INTO `wf_process_edge` (`process_definition_id`, `from_node_id`, `to_node_id`) VALUES
    (def_id, n1, n2), (def_id, n2, n3);

    IF p_has_compliance THEN
        -- Step4: 合规意见
        INSERT INTO `wf_process_node` (`process_definition_id`, `node_code`, `node_name`, `node_type`, `approver_rule`, `approver_value`, `decision_type`, `is_uploader`, `position_x`, `position_y`, `sort_order`)
        VALUES (def_id, CONCAT('COMPLIANCE_', p_milestone_code), '药政合规部意见', 'ROLE_APPROVE', 'ROLE_COMPLIANCE', '7', 'APPROVE_REJECT', 0, 500, 80, 4);
        SET n4 = LAST_INSERT_ID();
        INSERT INTO `wf_process_edge` (`process_definition_id`, `from_node_id`, `to_node_id`) VALUES (def_id, n3, n4);
    ELSE
        SET n4 = n3;
    END IF;

    IF p_use_pmc THEN
        -- Step5: PMC并行决策 (3个节点)
        INSERT INTO `wf_process_node` (`process_definition_id`, `node_code`, `node_name`, `node_type`, `approver_rule`, `approver_value`, `decision_type`, `is_uploader`, `position_x`, `position_y`, `sort_order`) VALUES
        (def_id, CONCAT('PMC_DEC_', p_milestone_code, '_1'), 'PMC决策-1', 'DECISION', 'ROLE_PMC', NULL, 'GO_NO_GO', 0, 650, 60, 5),
        (def_id, CONCAT('PMC_DEC_', p_milestone_code, '_2'), 'PMC决策-2', 'DECISION', 'ROLE_PMC', NULL, 'GO_NO_GO', 0, 650, 100, 5),
        (def_id, CONCAT('PMC_DEC_', p_milestone_code, '_3'), 'PMC决策-3', 'DECISION', 'ROLE_PMC', NULL, 'GO_NO_GO', 0, 650, 140, 5);
        SET n5a = (SELECT id FROM wf_process_node WHERE process_definition_id=def_id AND node_code=CONCAT('PMC_DEC_', p_milestone_code, '_1'));
        SET n5b = (SELECT id FROM wf_process_node WHERE process_definition_id=def_id AND node_code=CONCAT('PMC_DEC_', p_milestone_code, '_2'));
        SET n5c = (SELECT id FROM wf_process_node WHERE process_definition_id=def_id AND node_code=CONCAT('PMC_DEC_', p_milestone_code, '_3'));
        INSERT INTO `wf_process_edge` (`process_definition_id`, `from_node_id`, `to_node_id`) VALUES
        (def_id, n4, n5a), (def_id, n4, n5b), (def_id, n4, n5c);
    ELSE
        -- PM内部评审 (G1/G2)
        INSERT INTO `wf_process_node` (`process_definition_id`, `node_code`, `node_name`, `node_type`, `approver_rule`, `approver_value`, `decision_type`, `is_uploader`, `position_x`, `position_y`, `sort_order`)
        VALUES (def_id, CONCAT('PM_INTERNAL_', p_milestone_code), 'PM项目组内部评审', 'DECISION', 'ROLE_PM', NULL, 'GO_NO_GO', 0, 650, 90, 5);
        SET n5a = LAST_INSERT_ID();
        INSERT INTO `wf_process_edge` (`process_definition_id`, `from_node_id`, `to_node_id`) VALUES (def_id, n4, n5a);
    END IF;
END //
DELIMITER ;

-- G2: 化学部 + 合规 + PM内部评审
CALL create_simple_milestone_flow('G2', '新药化学部', '1', TRUE, FALSE);

-- G3: 化学部 + 合规 + PMC
CALL create_simple_milestone_flow('G3', '新药化学部', '1', TRUE, TRUE);

-- G4: 新药化学部 + 合规 + PMC (简化，实际多部门并行上传在流程管理界面按需添加)
CALL create_simple_milestone_flow('G4', '新药化学部', '1', TRUE, TRUE);

-- G5: 药政合规部 + 跳过合规 + PMC
CALL create_simple_milestone_flow('G5', '药政合规部', '7', FALSE, TRUE);

-- G6: 新药临床部 + 合规 + PMC
CALL create_simple_milestone_flow('G6', '新药临床部', '3', TRUE, TRUE);

-- G7: 新药临床部 + 合规 + PMC
CALL create_simple_milestone_flow('G7', '新药临床部', '3', TRUE, TRUE);

-- G8: 新药临床部 + 合规 + PMC
CALL create_simple_milestone_flow('G8', '新药临床部', '3', TRUE, TRUE);

-- G9: 药政合规部 + 跳过合规 + PMC
CALL create_simple_milestone_flow('G9', '药政合规部', '7', FALSE, TRUE);

DROP PROCEDURE IF EXISTS `create_simple_milestone_flow`;

-- ============================================================
-- 8. 初始化项目变更审批流程 (CHANGE)
-- ============================================================
SET @chg_def_id = (SELECT id FROM wf_process_definition WHERE process_type='CHANGE');

INSERT INTO `wf_process_node` (`process_definition_id`, `node_code`, `node_name`, `node_type`, `approver_rule`, `approver_value`, `decision_type`, `is_uploader`, `position_x`, `position_y`, `sort_order`) VALUES
(@chg_def_id, 'CHANGE_EFF_APPROVE', '效率管理部审批', 'DEPT_HEAD_APPROVE', 'DEPT_HEAD', '6', 'APPROVE_REJECT', 0, 200, 80, 1),
(@chg_def_id, 'CHANGE_PMC_APPROVE', 'PMC审批', 'DECISION', 'ROLE_PMC', NULL, 'APPROVE_REJECT', 0, 400, 80, 2);

SET @chgn1 = (SELECT id FROM wf_process_node WHERE process_definition_id=@chg_def_id AND node_code='CHANGE_EFF_APPROVE');
SET @chgn2 = (SELECT id FROM wf_process_node WHERE process_definition_id=@chg_def_id AND node_code='CHANGE_PMC_APPROVE');

INSERT INTO `wf_process_edge` (`process_definition_id`, `from_node_id`, `to_node_id`) VALUES
(@chg_def_id, @chgn1, @chgn2);

-- ============================================================
-- 9. 初始化项目终止审批流程 (TERMINATION)
-- ============================================================
SET @term_def_id = (SELECT id FROM wf_process_definition WHERE process_type='TERMINATION');

INSERT INTO `wf_process_node` (`process_definition_id`, `node_code`, `node_name`, `node_type`, `approver_rule`, `approver_value`, `decision_type`, `is_uploader`, `position_x`, `position_y`, `sort_order`) VALUES
(@term_def_id, 'TERM_EFF_APPROVE', '效率管理部审批', 'DEPT_HEAD_APPROVE', 'DEPT_HEAD', '6', 'APPROVE_REJECT', 0, 200, 80, 1),
(@term_def_id, 'TERM_PMC_APPROVE', 'PMC审批', 'DECISION', 'ROLE_PMC', NULL, 'APPROVE_REJECT', 0, 400, 80, 2),
(@term_def_id, 'TERM_PM_COMPLETE', 'PM完成终止任务', 'ROLE_APPROVE', 'ROLE_PM', NULL, 'NONE', 0, 600, 80, 3);

SET @termn1 = (SELECT id FROM wf_process_node WHERE process_definition_id=@term_def_id AND node_code='TERM_EFF_APPROVE');
SET @termn2 = (SELECT id FROM wf_process_node WHERE process_definition_id=@term_def_id AND node_code='TERM_PMC_APPROVE');
SET @termn3 = (SELECT id FROM wf_process_node WHERE process_definition_id=@term_def_id AND node_code='TERM_PM_COMPLETE');

INSERT INTO `wf_process_edge` (`process_definition_id`, `from_node_id`, `to_node_id`) VALUES
(@term_def_id, @termn1, @termn2), (@term_def_id, @termn2, @termn3);

-- ============================================================
-- 完成！
-- ============================================================
SELECT 'Workflow engine migration completed!' AS status;
SELECT COUNT(*) AS process_count FROM wf_process_definition;
SELECT COUNT(*) AS node_count FROM wf_process_node;
SELECT COUNT(*) AS edge_count FROM wf_process_edge;