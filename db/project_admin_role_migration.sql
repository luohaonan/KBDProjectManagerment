-- ============================================================
-- 新增"项目管理员"角色与权限迁移脚本
-- ============================================================
-- 使用说明：
--   mysql -h localhost -u root -p kbd_pm_system < db/project_admin_role_migration.sql
-- ============================================================

-- 1. 新增"项目管理员"角色 (ROLE_PROJECT_ADMIN)
INSERT IGNORE INTO `role` (`name`, `description`, `created_at`, `updated_at`)
VALUES ('ROLE_PROJECT_ADMIN', '项目管理员 - 可查看所有项目，可创建新项目', NOW(), NOW());

-- 2. 新增权限 (如果不存在)
-- 创建项目权限（给项目管理员使用，PM的创建权限保留）
INSERT IGNORE INTO `permission` (`name`, `description`, `created_at`, `updated_at`)
VALUES ('PERMISSION_CREATE_PROJECT', '创建新项目', NOW(), NOW());

-- 3. 为 ROLE_PROJECT_ADMIN 分配权限
-- 3a. 创建项目权限
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `role` r, `permission` p
WHERE r.name = 'ROLE_PROJECT_ADMIN' AND p.name = 'PERMISSION_CREATE_PROJECT';

-- 3b. 查看所有项目权限 (复用 PERMISSION_VIEW_ALL_PROJECTS 如果存在，否则在代码中通过角色判断)
-- 如果 PERMISSION_VIEW_ALL_PROJECTS 权限存在，则分配给 ROLE_PROJECT_ADMIN 和 ROLE_PM
INSERT IGNORE INTO `permission` (`name`, `description`, `created_at`, `updated_at`)
VALUES ('PERMISSION_VIEW_ALL_PROJECTS', '查看所有项目', NOW(), NOW());

INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `role` r, `permission` p
WHERE r.name = 'ROLE_PROJECT_ADMIN' AND p.name = 'PERMISSION_VIEW_ALL_PROJECTS';

-- 同时把查看全部项目权限给 ROLE_PM 和 ROLE_PMC (如果尚未分配)
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `role` r, `permission` p
WHERE r.name = 'ROLE_PM' AND p.name = 'PERMISSION_VIEW_ALL_PROJECTS';

INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `role` r, `permission` p
WHERE r.name = 'ROLE_PMC' AND p.name = 'PERMISSION_VIEW_ALL_PROJECTS';

-- 4. 如果 admin_user 需要项目管理员角色，取消注释以下行：
-- INSERT IGNORE INTO `user_roles` (`user_id`, `role_id`)
-- SELECT u.id, r.id
-- FROM `user` u, `role` r
-- WHERE u.username = 'admin_user' AND r.name = 'ROLE_PROJECT_ADMIN';