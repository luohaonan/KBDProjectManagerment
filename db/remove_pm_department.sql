-- ============================================================
-- 移除"项目经理组"部门迁移脚本
-- 说明：项目经理已变为用户属性（由项目管理员在新建项目时指派），
--       不再需要一个独立的"项目经理组"部门。
--       经分析，删除此部门不影响里程碑审批流程（审批流程使用
--       project.pm_user_id 而非 milestone_dept_role 中的 ROLE_PM）。
-- ============================================================
-- 使用说明：
--   mysql -h localhost -u root -p kbd_pm_system < db/remove_pm_department.sql
-- ============================================================

USE `kbd_pm_system`;

-- 1. 将 pm_user (id=2) 的 dept_id 设为 NULL（项目经理组即将被删除）
UPDATE `iam_user` SET `dept_id` = NULL WHERE `id` = 2 AND `dept_id` = 11;

-- 2. 删除 milestone_dept_role 表中所有引用 dept_id=11 的记录
--    （这些记录在代码中未被使用，删除不影响审批流程）
DELETE FROM `milestone_dept_role` WHERE `dept_id` = 11;

-- 3. 删除 org_department 表中的"项目经理组"部门
DELETE FROM `org_department` WHERE `dept_code` = 'PM';

-- 验证结果
SELECT '=== 验证：iam_user 中 pm_user 的部门 ===' AS '';
SELECT id, display_name, dept_id FROM `iam_user` WHERE id = 2;

SELECT '=== 验证：milestone_dept_role 中是否还有 dept_id=11 的记录 ===' AS '';
SELECT COUNT(*) AS remaining_count FROM `milestone_dept_role` WHERE dept_id = 11;

SELECT '=== 验证：org_department 中是否还有项目经理组 ===' AS '';
SELECT COUNT(*) AS remaining_count FROM `org_department` WHERE dept_code = 'PM';

SELECT '=== 删除完成 ===' AS '';