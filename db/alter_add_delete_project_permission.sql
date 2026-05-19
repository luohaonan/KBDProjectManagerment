-- 添加"删除项目"权限并分配给系统管理员角色
-- 用于已有数据库的增量迁移

-- 插入新权限
INSERT INTO `permission` (`name`, `description`, `created_at`, `updated_at`)
SELECT 'PERMISSION_DELETE_PROJECT', '删除项目', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `name` = 'PERMISSION_DELETE_PROJECT');

-- 将权限分配给系统管理员角色 (ROLE_ADMIN)
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `role` r, `permission` p
WHERE r.name = 'ROLE_ADMIN' AND p.name = 'PERMISSION_DELETE_PROJECT'
AND NOT EXISTS (
  SELECT 1 FROM `role_permissions` rp
  WHERE rp.role_id = r.id AND rp.permission_id = p.id
);
