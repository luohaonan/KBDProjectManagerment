-- ============================================================
-- 评审中心 & 角色权限增强 数据库变更脚本
-- 版本: 1.4
-- 日期: 2026-05-20
-- 说明:
--   1. 新增 ROLE_DEPT_EXECUTOR (部门执行人) 角色
--   2. 新增 milestone_dept_role 表，映射里程碑阶段与部门角色
--   3. 新增 review_approval_task 增加 conditional_attachment_required 字段
--   4. 新增 project 表增加 planned_end_date 字段
--   5. 为 ROLE_DEPT_EXECUTOR 分配权限
-- ============================================================

-- ============================================================
-- 辅助存储过程：安全地添加列（兼容 MySQL 8.0，不支持 IF NOT EXISTS）
-- ============================================================
DROP PROCEDURE IF EXISTS sp_add_column_if_not_exists;
DELIMITER $$
CREATE PROCEDURE sp_add_column_if_not_exists(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_column_definition TEXT
)
BEGIN
    DECLARE column_count INT;
    SET @db_name = DATABASE();
    SELECT COUNT(*) INTO column_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = p_table_name
      AND COLUMN_NAME = p_column_name;

    IF column_count = 0 THEN
        SET @sql = CONCAT('ALTER TABLE ', p_table_name, ' ADD COLUMN ', p_column_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- ============================================================
-- 1. 新增 planned_end_date 字段到 project 表
-- ============================================================
CALL sp_add_column_if_not_exists('project', 'planned_end_date',
    'planned_end_date DATE DEFAULT NULL COMMENT ''预估项目结束日期''');

-- ============================================================
-- 2. 新增 ROLE_DEPT_EXECUTOR 角色
-- ============================================================
INSERT IGNORE INTO role (name, description, created_at, updated_at)
VALUES ('ROLE_DEPT_EXECUTOR', '部门执行人 - 可上传交付物并发起评审', NOW(), NOW());

-- ============================================================
-- 3. 为 ROLE_DEPT_EXECUTOR 分配权限
--    部门执行人可以: 查看项目、上传交付物、提交评审、查看评审
-- ============================================================
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.name = 'ROLE_DEPT_EXECUTOR'
  AND p.name IN (
    'PERMISSION_VIEW_PROJECT',
    'PERMISSION_UPLOAD_DOCUMENT',
    'PERMISSION_SUBMIT_REVIEW',
    'PERMISSION_VIEW_REVIEW'
  );

-- ============================================================
-- 4. 为 ROLE_DEPT_HEAD 补充评审权限（如果尚未分配）
-- ============================================================
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.name = 'ROLE_DEPT_HEAD'
  AND p.name IN (
    'PERMISSION_VIEW_REVIEW',
    'PERMISSION_APPROVE_REVIEW'
  );

-- ============================================================
-- 5. 创建里程碑-部门角色映射表
-- ============================================================
CREATE TABLE IF NOT EXISTS milestone_dept_role (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  milestone_def_id BIGINT UNSIGNED NOT NULL COMMENT '里程碑定义ID',
  dept_id BIGINT UNSIGNED NOT NULL COMMENT '部门ID',
  role_type VARCHAR(50) NOT NULL COMMENT '角色类型: DEPT_EXECUTOR(部门执行人) / DEPT_HEAD(部门负责人)',
  is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_milestone_dept_role (milestone_def_id, dept_id, role_type),
  KEY idx_milestone_def (milestone_def_id),
  KEY idx_dept (dept_id),
  CONSTRAINT fk_milestone_dept_role_milestone_def FOREIGN KEY (milestone_def_id) REFERENCES milestone_def(id),
  CONSTRAINT fk_milestone_dept_role_dept FOREIGN KEY (dept_id) REFERENCES org_department(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='里程碑阶段-部门角色映射表';

-- ============================================================
-- 6. 为 review_approval_task 增加 conditional_attachment_required 字段
-- ============================================================
CALL sp_add_column_if_not_exists('review_approval_task', 'conditional_attachment_required',
    'conditional_attachment_required TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否需要条件附件(Conditional Go时)''');

-- ============================================================
-- 7. 为 review_approval 增加 review_type 字段（区分立项评审和里程碑评审）
-- ============================================================
CALL sp_add_column_if_not_exists('review_approval', 'review_type',
    'review_type VARCHAR(50) DEFAULT ''MILESTONE'' COMMENT ''评审类型: INITIATION(立项评审) / MILESTONE(里程碑评审)''');

-- ============================================================
-- 8. 为 review_record 增加 review_type 字段
-- ============================================================
CALL sp_add_column_if_not_exists('review_record', 'review_type',
    'review_type VARCHAR(50) DEFAULT ''MILESTONE'' COMMENT ''评审类型: INITIATION(立项评审) / MILESTONE(里程碑评审)''');

-- ============================================================
-- 9. 为 project_milestone 增加 conditional_attachments 字段（存储条件附件信息）
-- ============================================================
CALL sp_add_column_if_not_exists('project_milestone', 'conditional_attachments',
    'conditional_attachments JSON DEFAULT NULL COMMENT ''Conditional Go条件附件列表''');

-- ============================================================
-- 10. 创建评审中心视图（方便查询待办评审）
-- ============================================================
CREATE OR REPLACE VIEW v_pending_review_tasks AS
SELECT
  t.id AS task_id,
  t.review_approval_id,
  t.approver_user_id,
  t.approver_role,
  t.status AS task_status,
  a.project_id,
  a.project_milestone_id,
  a.submitter_user_id,
  a.submit_comment,
  a.status AS approval_status,
  a.submitted_at,
  a.review_type,
  p.project_code,
  p.project_name,
  p.current_milestone_id,
  md.milestone_code,
  md.milestone_name
FROM review_approval_task t
JOIN review_approval a ON t.review_approval_id = a.id
JOIN project p ON a.project_id = p.id
LEFT JOIN project_milestone pm ON a.project_milestone_id = pm.id
LEFT JOIN milestone_def md ON pm.milestone_id = md.id
WHERE t.status = 'PENDING'
  AND a.status = 'SUBMITTED'
  AND p.status IN ('ACTIVE', 'DRAFT')
ORDER BY a.submitted_at DESC;

-- ============================================================
-- 11. 清理辅助存储过程
-- ============================================================
DROP PROCEDURE IF EXISTS sp_add_column_if_not_exists;

-- ============================================================
-- 12. 更新 db/README.md 中记录的版本号
-- ============================================================
-- 手动更新 README.md 中的版本信息
