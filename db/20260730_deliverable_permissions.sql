-- 交付物管理权限初始化
-- 可重复执行：使用 INSERT IGNORE，且仅给目标权限组追加权限，不覆盖已有权限。

INSERT IGNORE INTO `permission` (`name`, `description`, `created_at`, `updated_at`)
VALUES
  ('PERMISSION_DELIVERABLE_VIEW', '查看本人上传或本人评审过的交付物', NOW(), NOW()),
  ('PERMISSION_DELIVERABLE_VIEW_ALL', '查看项目全部里程碑交付物', NOW(), NOW()),
  ('PERMISSION_DELIVERABLE_IMPORT', '将历史文件导入指定 G0-G9 交付物槽位', NOW(), NOW());

-- 用户可查看本人上传或评审过的文件：基础查看能力默认给所有业务角色。
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `role` r CROSS JOIN `permission` p
WHERE p.name = 'PERMISSION_DELIVERABLE_VIEW'
  AND r.name IN ('ROLE_ADMIN', 'ROLE_PROJECT_ADMIN', 'ROLE_PM', 'ROLE_PMC',
                 'ROLE_DEPT_HEAD', 'ROLE_DEPT_EXECUTOR', 'ROLE_COMPLIANCE');

-- 全量查看：系统管理员、项目管理员、PMC；效率管理部成员按部门识别规则获得。
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `role` r CROSS JOIN `permission` p
WHERE p.name = 'PERMISSION_DELIVERABLE_VIEW_ALL'
  AND r.name IN ('ROLE_ADMIN', 'ROLE_PROJECT_ADMIN', 'ROLE_PMC');

-- 历史项目导入：仅系统管理员、项目管理员。
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `role` r CROSS JOIN `permission` p
WHERE p.name = 'PERMISSION_DELIVERABLE_IMPORT'
  AND r.name IN ('ROLE_ADMIN', 'ROLE_PROJECT_ADMIN');