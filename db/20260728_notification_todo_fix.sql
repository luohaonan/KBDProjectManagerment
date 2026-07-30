-- 2026-07-28
-- 修复 notification 表缺失的待办相关字段与索引
-- 适用于开发库/部署前整理最终库结构时手动执行

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