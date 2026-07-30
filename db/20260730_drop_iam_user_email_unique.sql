-- 允许测试阶段多个账号共用同一个邮箱
-- 问题现象：修改用户邮箱时，同步 iam_user 会触发唯一索引 uk_iam_user_email 冲突
-- 处理方式：删除 iam_user.email 上的唯一索引，仅保留普通字段

SET @index_exists = (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'iam_user'
    AND index_name = 'uk_iam_user_email'
);

SET @drop_idx_sql = IF(
  @index_exists > 0,
  'ALTER TABLE `iam_user` DROP INDEX `uk_iam_user_email`',
  'SELECT ''uk_iam_user_email not exists'''
);

PREPARE stmt FROM @drop_idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 可选校验
-- SHOW INDEX FROM `iam_user`;