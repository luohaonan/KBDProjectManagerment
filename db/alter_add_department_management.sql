-- ============================================================
-- 部门管理功能 数据库变更脚本
-- 版本: 1.0
-- 日期: 2026-05-20
-- 说明:
--   1. org_department 表添加 head_user_id 字段（部门负责人）
--   2. user 表添加 department_id 字段（所属部门）
--   3. 插入10个预定义部门数据
-- ============================================================

-- ============================================================
-- 辅助存储过程：安全地添加列（兼容 MySQL 8.0）
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
-- 1. org_department 表添加 head_user_id 字段
-- ============================================================
CALL sp_add_column_if_not_exists('org_department', 'head_user_id',
    'head_user_id BIGINT UNSIGNED DEFAULT NULL COMMENT ''部门负责人用户ID（关联user表）''');

-- ============================================================
-- 2. user 表添加 department_id 字段
-- ============================================================
CALL sp_add_column_if_not_exists('user', 'department_id',
    'department_id BIGINT UNSIGNED DEFAULT NULL COMMENT ''所属部门ID（关联org_department表）''');

-- ============================================================
-- 3. 插入10个预定义部门
-- ============================================================
INSERT IGNORE INTO org_department (dept_code, dept_name, dept_type, parent_id, is_active, created_at, updated_at)
VALUES
('SYSTEM',     'System',       'OTHER', NULL, 1, NOW(), NOW()),
('PMC',        '项目管理委员会', 'OTHER', NULL, 1, NOW(), NOW()),
('PM',         '项目经理组',    'OTHER', NULL, 1, NOW(), NOW()),
('CHEMISTRY',  '新药化学部',    'PDT',   NULL, 1, NOW(), NOW()),
('BIOLOGY',    '新药生物部',    'PDT',   NULL, 1, NOW(), NOW()),
('CLINICAL',   '新药临床部',    'PDT',   NULL, 1, NOW(), NOW()),
('INFO',       '新药资讯部',    'PDT',   NULL, 1, NOW(), NOW()),
('BD',         '商务拓展部',    'ROSS',  NULL, 1, NOW(), NOW()),
('EFFICIENCY', '效率管理部',    'ROSS',  NULL, 1, NOW(), NOW()),
('REGULATORY', '药政合规部',    'ROSS',  NULL, 1, NOW(), NOW());

-- ============================================================
-- 4. 清理辅助存储过程
-- ============================================================
DROP PROCEDURE IF EXISTS sp_add_column_if_not_exists;
