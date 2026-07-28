-- ============================================================
-- iam_user 表用户数据同步迁移
-- 问题：user 表有 11 个用户，但 iam_user 表只有 6 个用户
-- 导致所有引用 iam_user 外键的表在插入 ID > 6 的用户数据时报错
-- 例如：review_approval 表 submitter_user_id 外键约束失败
-- ============================================================

-- 将 user 表中新增的 5 个用户同步到 iam_user 表
-- 保持 ID 一致，确保外键引用正确

INSERT IGNORE INTO iam_user (id, user_no, display_name, email, dept_id, title, is_active) VALUES
(7, 'test_user', 'test_user', NULL, 1, NULL, 1),
(8, '资讯部执行人', '资讯部执行人', NULL, 4, NULL, 1),
(9, '资讯部负责人', '资讯部负责人', NULL, 4, NULL, 1),
(10, '化学部执行人', '化学部执行人', NULL, 1, NULL, 1),
(11, '化学部负责人', '化学部负责人', NULL, 1, NULL, 1);

-- 验证：执行后 iam_user 表应有 11 个用户
-- SELECT COUNT(*) FROM iam_user; -- 应返回 11