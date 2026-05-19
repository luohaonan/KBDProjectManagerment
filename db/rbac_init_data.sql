-- Initial data for RBAC tables
-- 角色、权限和初始用户数据

-- ===== Insert Roles =====
INSERT INTO `role` (`name`, `description`) VALUES
  ('ROLE_PMC', 'PMC（项目管理委员会）- 拥有最高决策权，负责Go/No Go决策、重大变更审批及预算追加审批'),
  ('ROLE_PM', 'PM（项目经理）- 负责横向贯通，拥有制定计划、监控预算、组织评审及提交变更申请的权限'),
  ('ROLE_DEPT_HEAD', '职能部门负责人 - 负责所属领域的交付物提交'),
  ('ROLE_EFFICIENCY', '效率管理部 - 负责系统维护、月度预算核算、预警监控及项目考核'),
  ('ROLE_COMPLIANCE', '药政合规部 - 负责合规性意见出具及申报文档审查'),
  ('ROLE_ADMIN', '系统管理员 - 拥有系统管理和配置权限');

-- ===== Insert Permissions =====
INSERT INTO `permission` (`name`, `description`) VALUES
  -- Milestone operations
  ('PERMISSION_SUBMIT_REVIEW', '提交里程碑评审'),
  ('PERMISSION_APPROVE_MILESTONE', '批准里程碑（PMC Go/No Go决策）'),
  ('PERMISSION_VIEW_MILESTONE', '查看里程碑信息'),
  
  -- Budget operations
  ('PERMISSION_VIEW_BUDGET', '查看预算信息'),
  ('PERMISSION_APPROVE_BUDGET', '批准预算调整'),
  ('PERMISSION_MANAGE_BUDGET', '管理预算计划'),
  
  -- Project operations
  ('PERMISSION_CREATE_PROJECT', '创建项目'),
  ('PERMISSION_VIEW_PROJECT', '查看项目信息'),
  ('PERMISSION_EDIT_PROJECT', '编辑项目'),
  ('PERMISSION_DELETE_PROJECT', '删除项目'),
  ('PERMISSION_TERMINATE_PROJECT', '终止项目'),
  
  -- Document operations
  ('PERMISSION_UPLOAD_DOCUMENT', '上传交付物'),
  ('PERMISSION_VIEW_DOCUMENT', '查看交付物'),
  ('PERMISSION_REVIEW_DOCUMENT', '审查交付物'),
  
  -- Change request operations
  ('PERMISSION_SUBMIT_CHANGE_REQUEST', '提交变更申请'),
  ('PERMISSION_APPROVE_CHANGE_REQUEST', '批准变更申请'),
  ('PERMISSION_VIEW_CHANGE_REQUEST', '查看变更申请'),
  
  -- System operations
  ('PERMISSION_MANAGE_USERS', '管理用户'),
  ('PERMISSION_MANAGE_ROLES', '管理角色'),
  ('PERMISSION_VIEW_REPORTS', '查看报表'),
  ('PERMISSION_SYSTEM_MAINTENANCE', '系统维护');

-- ===== Assign Permissions to Roles =====

-- PMC 权限
INSERT INTO `role_permissions` (`role_id`, `permission_id`) 
SELECT r.id, p.id FROM `role` r, `permission` p 
WHERE r.name = 'ROLE_PMC' AND p.name IN (
  'PERMISSION_APPROVE_MILESTONE',
  'PERMISSION_VIEW_PROJECT',
  'PERMISSION_VIEW_BUDGET',
  'PERMISSION_APPROVE_BUDGET',
  'PERMISSION_VIEW_DOCUMENT',
  'PERMISSION_APPROVE_CHANGE_REQUEST',
  'PERMISSION_VIEW_CHANGE_REQUEST'
);

-- PM 权限
INSERT INTO `role_permissions` (`role_id`, `permission_id`) 
SELECT r.id, p.id FROM `role` r, `permission` p 
WHERE r.name = 'ROLE_PM' AND p.name IN (
  'PERMISSION_CREATE_PROJECT',
  'PERMISSION_VIEW_PROJECT',
  'PERMISSION_EDIT_PROJECT',
  'PERMISSION_SUBMIT_REVIEW',
  'PERMISSION_VIEW_BUDGET',
  'PERMISSION_MANAGE_BUDGET',
  'PERMISSION_UPLOAD_DOCUMENT',
  'PERMISSION_VIEW_DOCUMENT',
  'PERMISSION_SUBMIT_CHANGE_REQUEST',
  'PERMISSION_VIEW_CHANGE_REQUEST'
);

-- 职能部门负责人权限
INSERT INTO `role_permissions` (`role_id`, `permission_id`) 
SELECT r.id, p.id FROM `role` r, `permission` p 
WHERE r.name = 'ROLE_DEPT_HEAD' AND p.name IN (
  'PERMISSION_VIEW_PROJECT',
  'PERMISSION_UPLOAD_DOCUMENT',
  'PERMISSION_VIEW_DOCUMENT',
  'PERMISSION_VIEW_BUDGET'
);

-- 效率管理部权限
INSERT INTO `role_permissions` (`role_id`, `permission_id`) 
SELECT r.id, p.id FROM `role` r, `permission` p 
WHERE r.name = 'ROLE_EFFICIENCY' AND p.name IN (
  'PERMISSION_VIEW_PROJECT',
  'PERMISSION_VIEW_BUDGET',
  'PERMISSION_VIEW_CHANGE_REQUEST',
  'PERMISSION_VIEW_REPORTS',
  'PERMISSION_SYSTEM_MAINTENANCE'
);

-- 药政合规部权限
INSERT INTO `role_permissions` (`role_id`, `permission_id`) 
SELECT r.id, p.id FROM `role` r, `permission` p 
WHERE r.name = 'ROLE_COMPLIANCE' AND p.name IN (
  'PERMISSION_VIEW_PROJECT',
  'PERMISSION_VIEW_DOCUMENT',
  'PERMISSION_REVIEW_DOCUMENT'
);

-- 系统管理员权限
INSERT INTO `role_permissions` (`role_id`, `permission_id`) 
SELECT r.id, p.id FROM `role` r, `permission` p 
WHERE r.name = 'ROLE_ADMIN' AND p.name IN (
  'PERMISSION_MANAGE_USERS',
  'PERMISSION_MANAGE_ROLES',
  'PERMISSION_VIEW_PROJECT',
  'PERMISSION_EDIT_PROJECT',
  'PERMISSION_DELETE_PROJECT',
  'PERMISSION_VIEW_BUDGET',
  'PERMISSION_VIEW_REPORTS',
  'PERMISSION_SYSTEM_MAINTENANCE'
);

-- ===== Create Initial Test Users =====
-- 密码均为 test123（需使用 BCryptPasswordEncoder 加密）
-- BCrypt 加密后的 "test123" 示例：$2a$10$RYvEYVQjMqFR8aw3x.Ev4uYuP.bVJzLy8lHvtjw1.I1o8xPEBsHnC

INSERT INTO `user` (`username`, `password`, `email`, `is_active`) VALUES
  ('pmc_user', '$2a$10$RYvEYVQjMqFR8aw3x.Ev4uYuP.bVJzLy8lHvtjw1.I1o8xPEBsHnC', 'pmc@example.com', 1),
  ('pm_user', '$2a$10$RYvEYVQjMqFR8aw3x.Ev4uYuP.bVJzLy8lHvtjw1.I1o8xPEBsHnC', 'pm@example.com', 1),
  ('dept_head', '$2a$10$RYvEYVQjMqFR8aw3x.Ev4uYuP.bVJzLy8lHvtjw1.I1o8xPEBsHnC', 'dept@example.com', 1),
  ('efficiency_user', '$2a$10$RYvEYVQjMqFR8aw3x.Ev4uYuP.bVJzLy8lHvtjw1.I1o8xPEBsHnC', 'efficiency@example.com', 1),
  ('compliance_user', '$2a$10$RYvEYVQjMqFR8aw3x.Ev4uYuP.bVJzLy8lHvtjw1.I1o8xPEBsHnC', 'compliance@example.com', 1),
  ('admin_user', '$2a$10$RYvEYVQjMqFR8aw3x.Ev4uYuP.bVJzLy8lHvtjw1.I1o8xPEBsHnC', 'admin@example.com', 1);

-- ===== Assign Roles to Users =====
INSERT INTO `user_roles` (`user_id`, `role_id`) 
SELECT u.id, r.id FROM `user` u, `role` r 
WHERE u.username = 'pmc_user' AND r.name = 'ROLE_PMC';

INSERT INTO `user_roles` (`user_id`, `role_id`) 
SELECT u.id, r.id FROM `user` u, `role` r 
WHERE u.username = 'pm_user' AND r.name = 'ROLE_PM';

INSERT INTO `user_roles` (`user_id`, `role_id`) 
SELECT u.id, r.id FROM `user` u, `role` r 
WHERE u.username = 'dept_head' AND r.name = 'ROLE_DEPT_HEAD';

INSERT INTO `user_roles` (`user_id`, `role_id`) 
SELECT u.id, r.id FROM `user` u, `role` r 
WHERE u.username = 'efficiency_user' AND r.name = 'ROLE_EFFICIENCY';

INSERT INTO `user_roles` (`user_id`, `role_id`) 
SELECT u.id, r.id FROM `user` u, `role` r 
WHERE u.username = 'compliance_user' AND r.name = 'ROLE_COMPLIANCE';

INSERT INTO `user_roles` (`user_id`, `role_id`) 
SELECT u.id, r.id FROM `user` u, `role` r 
WHERE u.username = 'admin_user' AND r.name = 'ROLE_ADMIN';
