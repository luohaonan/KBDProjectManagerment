-- 数据库迁移验证脚本
-- 运行此脚本以检查所有必要的表和字段是否已正确应用

-- ===== 检查第1阶段：基础表 (ddl_mysql8.sql) =====
SELECT '=== 第1阶段：基础表检查 ===' as stage;

SELECT 
  COUNT(*) as table_count,
  GROUP_CONCAT(table_name) as tables
FROM information_schema.tables 
WHERE table_schema = 'kbd_pm_system' 
AND table_name IN ('org_department', 'iam_user', 'governance_committee', 
                   'project_level', 'milestone_def', 'project', 
                   'project_team_member', 'project_milestone',
                   'project_budget_policy', 'project_budget_plan');

-- ===== 检查第2阶段：ALTER - process_oversight_dept =====
SELECT '=== 第2阶段：process_oversight_dept 字段检查 ===' as stage;

SELECT 
  COLUMN_NAME,
  COLUMN_TYPE,
  IS_NULLABLE,
  COLUMN_KEY
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'kbd_pm_system' 
AND TABLE_NAME = 'project' 
AND COLUMN_NAME = 'process_oversight_dept_id';

-- ===== 检查第3阶段：ALTER - milestone review core =====
SELECT '=== 第3阶段：milestone review 字段检查 ===' as stage;

-- 检查 conditional_deadline 字段
SELECT 
  'conditional_deadline' as field_name,
  COLUMN_TYPE,
  IS_NULLABLE
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'kbd_pm_system' 
AND TABLE_NAME = 'project_milestone' 
AND COLUMN_NAME = 'conditional_deadline';

-- 检查 terminated_reason 字段
SELECT 
  'terminated_reason' as field_name,
  COLUMN_TYPE,
  IS_NULLABLE
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'kbd_pm_system' 
AND TABLE_NAME = 'project' 
AND COLUMN_NAME = 'terminated_reason';

-- 检查新表
SELECT 
  TABLE_NAME,
  TABLE_ROWS
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'kbd_pm_system' 
AND TABLE_NAME IN ('project_document', 'milestone_history')
ORDER BY TABLE_NAME;

-- ===== 检查第4阶段：RBAC 表 (rbac_ddl.sql) =====
SELECT '=== 第4阶段：RBAC 表检查 ===' as stage;

SELECT 
  TABLE_NAME,
  TABLE_ROWS as row_count,
  ROUND(DATA_LENGTH/1024, 2) as size_kb
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'kbd_pm_system' 
AND TABLE_NAME IN ('user', 'role', 'permission', 'user_roles', 'role_permissions')
ORDER BY TABLE_NAME;

-- ===== 检查第5阶段：文档管理表 (create_document_management.sql) =====
SELECT '=== 第5阶段：文档管理表检查 ===' as stage;

SELECT 
  TABLE_NAME,
  TABLE_ROWS as row_count,
  ROUND(DATA_LENGTH/1024, 2) as size_kb
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'kbd_pm_system' 
AND TABLE_NAME IN ('document', 'audit_log')
ORDER BY TABLE_NAME;

-- ===== 完整性检查 =====
SELECT '=== 完整性检查 ===' as stage;

SELECT 
  CASE 
    WHEN table_count >= 15 THEN '✓ 基础表完整'
    ELSE '✗ 基础表不完整'
  END as base_tables_status,
  
  CASE 
    WHEN process_oversight_count > 0 THEN '✓ 第2阶段字段已应用'
    ELSE '✗ 第2阶段字段未应用'
  END as phase2_status,
  
  CASE 
    WHEN milestone_review_count > 0 THEN '✓ 第3阶段表已应用'
    ELSE '✗ 第3阶段表未应用'
  END as phase3_status,
  
  CASE 
    WHEN rbac_table_count = 5 THEN '✓ RBAC 表完整'
    WHEN rbac_table_count > 0 THEN '⚠ RBAC 表部分应用'
    ELSE '✗ RBAC 表未应用'
  END as rbac_status,
  
  CASE 
    WHEN document_table_count = 2 THEN '✓ 文档管理表完整'
    WHEN document_table_count > 0 THEN '⚠ 文档管理表部分应用'
    ELSE '✗ 文档管理表未应用'
  END as document_status

FROM (
  SELECT 
    COUNT(DISTINCT t.TABLE_NAME) as table_count,
    SUM(CASE WHEN COLUMN_NAME = 'process_oversight_dept_id' THEN 1 ELSE 0 END) as process_oversight_count,
    SUM(CASE WHEN t.TABLE_NAME IN ('project_document', 'milestone_history') THEN 1 ELSE 0 END) as milestone_review_count,
    SUM(CASE WHEN t.TABLE_NAME IN ('user', 'role', 'permission', 'user_roles', 'role_permissions') THEN 1 ELSE 0 END) as rbac_table_count,
    SUM(CASE WHEN t.TABLE_NAME IN ('document', 'audit_log') THEN 1 ELSE 0 END) as document_table_count
  FROM information_schema.TABLES t
  LEFT JOIN information_schema.COLUMNS c ON t.TABLE_SCHEMA = c.TABLE_SCHEMA AND t.TABLE_NAME = c.TABLE_NAME
  WHERE t.TABLE_SCHEMA = 'kbd_pm_system'
) as status_check;

-- ===== 数据统计 =====
SELECT '=== 数据统计 ===' as section;

SELECT 
  'roles' as entity_type,
  COUNT(*) as count
FROM `role`
UNION ALL
SELECT 
  'permissions' as entity_type,
  COUNT(*) as count
FROM `permission`
UNION ALL
SELECT 
  'users' as entity_type,
  COUNT(*) as count
FROM `user`
UNION ALL
SELECT 
  'user_roles' as entity_type,
  COUNT(*) as count
FROM `user_roles`
UNION ALL
SELECT 
  'role_permissions' as entity_type,
  COUNT(*) as count
FROM `role_permissions`
UNION ALL
SELECT 
  'documents' as entity_type,
  COUNT(*) as count
FROM `document`
UNION ALL
SELECT 
  'audit_logs' as entity_type,
  COUNT(*) as count
FROM `audit_log`;

-- ===== 权限分配完整性检查 =====
SELECT '=== 权限分配检查 ===' as section;

SELECT 
  r.name as role_name,
  COUNT(DISTINCT rp.permission_id) as assigned_permissions,
  GROUP_CONCAT(DISTINCT p.name ORDER BY p.name SEPARATOR ', ') as permission_names
FROM `role` r
LEFT JOIN `role_permissions` rp ON r.id = rp.role_id
LEFT JOIN `permission` p ON rp.permission_id = p.id
GROUP BY r.id, r.name
ORDER BY r.name;

-- ===== 用户角色分配检查 =====
SELECT '=== 用户角色检查 ===' as section;

SELECT 
  u.username,
  u.is_active,
  GROUP_CONCAT(r.name SEPARATOR ', ') as roles
FROM `user` u
LEFT JOIN `user_roles` ur ON u.id = ur.user_id
LEFT JOIN `role` r ON ur.role_id = r.id
GROUP BY u.id, u.username, u.is_active
ORDER BY u.username;

-- ===== 最终状态报告 =====
SELECT '=== 最终状态报告 ===' as final_report;

SELECT 
  '✓ 数据库迁移验证完成' as status,
  CURDATE() as verification_date,
  VERSION() as mysql_version,
  DATABASE() as current_database;