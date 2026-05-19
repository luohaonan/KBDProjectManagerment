# RBAC 数据库迁移指南

## 概述
本指南说明如何应用新增的权限管理（RBAC）数据库表和初始数据。

## 文件说明

### 1. `rbac_ddl.sql` - 表结构定义
包含以下 5 个新表的 DDL 脚本：

| 表名 | 说明 |
|------|------|
| `user` | 系统认证用户表（用于登录） |
| `role` | 角色表 |
| `permission` | 权限表 |
| `user_roles` | 用户-角色关联表（多对多） |
| `role_permissions` | 角色-权限关联表（多对多） |

### 2. `rbac_init_data.sql` - 初始数据脚本
包含以下初始化内容：

#### 预定义角色（6 个）
- `ROLE_PMC` - 项目管理委员会
- `ROLE_PM` - 项目经理
- `ROLE_DEPT_HEAD` - 职能部门负责人
- `ROLE_EFFICIENCY` - 效率管理部
- `ROLE_COMPLIANCE` - 药政合规部
- `ROLE_ADMIN` - 系统管理员

#### 预定义权限（21 个）
涵盖：里程碑、预算、项目、交付物、变更、系统管理等操作

#### 测试用户（6 个）
| 用户名 | 角色 | 密码 | 邮箱 |
|--------|------|------|------|
| `pmc_user` | ROLE_PMC | test123 | pmc@example.com |
| `pm_user` | ROLE_PM | test123 | pm@example.com |
| `dept_head` | ROLE_DEPT_HEAD | test123 | dept@example.com |
| `efficiency_user` | ROLE_EFFICIENCY | test123 | efficiency@example.com |
| `compliance_user` | ROLE_COMPLIANCE | test123 | compliance@example.com |
| `admin_user` | ROLE_ADMIN | test123 | admin@example.com |

## 应用步骤

### ✅ 确认前置条件
- 已应用 `ddl_mysql8.sql`
- 已应用 `alter_project_add_process_oversight_dept.sql`
- 已应用 `alter_milestone_review_core.sql`

### 第一步：执行 DDL 脚本
```bash
mysql -h localhost -u root -p kbd_pm_system < rbac_ddl.sql
```

### 第二步：执行初始数据脚本
```bash
mysql -h localhost -u root -p kbd_pm_system < rbac_init_data.sql
```

或者在 MySQL 客户端中：
```sql
source rbac_ddl.sql;
source rbac_init_data.sql;
```

## 重要注意事项

### 1. 密码安全
初始化脚本中的密码为示例 BCrypt 加密后的 "test123"：
```
$2a$10$RYvEYVQjMqFR8aw3x.Ev4uYuP.bVJzLy8lHvtjw1.I1o8xPEBsHnC
```

**必须**在生产环境中：
- 修改所有初始用户的密码
- 使用强密码
- 禁用不需要的测试账户

### 2. 与现有表的关系
RBAC 脚本创建的表完全独立，**无需修改现有脚本**：
- ✓ 新增 5 个 RBAC 专用表
- ✓ 不修改任何现有表结构
- ✓ 无外键依赖到现有表
- ✓ 可以直接在现有数据库基础上运行

### 3. 字符编码
所有表都使用 `utf8mb4` 编码，支持 emoji 和其他扩展字符。

### 4. 外键约束
表之间使用 ON DELETE CASCADE，删除角色或权限时会自动清理关联数据。

### 5. 与现有系统的关系
- 新增的 `user` 表用于**认证**（登录）
- 现有的 `iam_user` 表用于**组织信息**（部门、岗位等）
- 两个表可以通过 username/user_no 字段关联

## 后续扩展

### 添加新用户
```sql
INSERT INTO `user` (`username`, `password`, `email`) VALUES
  ('new_user', '<bcrypt_password>', 'new@example.com');

INSERT INTO `user_roles` (`user_id`, `role_id`) 
SELECT u.id, r.id FROM `user` u, `role` r 
WHERE u.username = 'new_user' AND r.name = 'ROLE_PM';
```

### 修改用户角色
```sql
DELETE FROM `user_roles` WHERE user_id = (SELECT id FROM `user` WHERE username = 'some_user');

INSERT INTO `user_roles` (`user_id`, `role_id`) 
SELECT u.id, r.id FROM `user` u, `role` r 
WHERE u.username = 'some_user' AND r.name = 'ROLE_DEPT_HEAD';
```

### 查看用户权限
```sql
SELECT DISTINCT p.name, p.description
FROM `user` u
JOIN `user_roles` ur ON u.id = ur.user_id
JOIN `role` r ON ur.role_id = r.id
JOIN `role_permissions` rp ON r.id = rp.role_id
JOIN `permission` p ON rp.permission_id = p.id
WHERE u.username = 'pm_user'
ORDER BY p.name;
```

## 测试验证

应用脚本后，可验证数据完整性：

```sql
-- 检查角色是否创建成功
SELECT COUNT(*) as role_count FROM `role`;
-- 期望结果：6

-- 检查权限是否创建成功
SELECT COUNT(*) as permission_count FROM `permission`;
-- 期望结果：21

-- 检查测试用户是否创建成功
SELECT COUNT(*) as user_count FROM `user`;
-- 期望结果：6

-- 检查角色-权限关联是否正确
SELECT r.name, COUNT(p.id) as permission_count
FROM `role` r
LEFT JOIN `role_permissions` rp ON r.id = rp.role_id
LEFT JOIN `permission` p ON rp.permission_id = p.id
GROUP BY r.id, r.name;
```

## 回滚

如需回滚（谨慎使用）：
```sql
DROP TABLE IF EXISTS `role_permissions`;
DROP TABLE IF EXISTS `user_roles`;
DROP TABLE IF EXISTS `permission`;
DROP TABLE IF EXISTS `role`;
DROP TABLE IF EXISTS `user`;
```