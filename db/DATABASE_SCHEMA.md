# 数据库设计文档 - RBAC 权限系统

## 表关系图

```
┌──────────────┐
│    user      │ 认证用户表
├──────────────┤
│ id (PK)      │
│ username     │──── 用于登录
│ password     │──── BCrypt 加密
│ email        │
│ is_active    │
└──┬───────────┘
   │ (1:n)
   │ user_roles 关联表
   │
   ▼
┌──────────────┐
│    role      │ 角色表
├──────────────┤
│ id (PK)      │
│ name         │──── 角色标识 (ROLE_PMC, ROLE_PM等)
│ description  │
└──┬───────────┘
   │ (1:n)
   │ role_permissions 关联表
   │
   ▼
┌──────────────────┐
│   permission     │ 权限表
├──────────────────┤
│ id (PK)          │
│ name             │──── 权限标识
│ description      │
└──────────────────┘
```

## 表详细定义

### 1. user 表
认证用户表，用于系统登录。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 用户 ID |
| username | VARCHAR(64) | NOT NULL, UNIQUE | 用户名（登录凭证） |
| password | VARCHAR(255) | NOT NULL | 加密密码（BCrypt） |
| email | VARCHAR(128) | | 邮箱 |
| is_active | TINYINT(1) | NOT NULL, DEFAULT 1 | 激活状态 |
| created_at | TIMESTAMP(3) | NOT NULL | 创建时间 |
| updated_at | TIMESTAMP(3) | NOT NULL | 更新时间 |

**索引：**
- `uk_user_username` - UNIQUE(username)
- `idx_user_email` - (email)
- `idx_user_is_active` - (is_active)

**备注：**
- 与 `iam_user` 表无直接关联，可通过 username 字段手工映射
- 密码使用 BCryptPasswordEncoder 加密

### 2. role 表
角色表，定义系统中的各种角色。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 角色 ID |
| name | VARCHAR(64) | NOT NULL, UNIQUE | 角色名称（ROLE_* 格式） |
| description | VARCHAR(255) | | 角色描述 |
| created_at | TIMESTAMP(3) | NOT NULL | 创建时间 |
| updated_at | TIMESTAMP(3) | NOT NULL | 更新时间 |

**索引：**
- `uk_role_name` - UNIQUE(name)

**预定义角色：**
```
ROLE_PMC              项目管理委员会 - 最高决策权
ROLE_PM               项目经理 - 项目协调权
ROLE_DEPT_HEAD        职能部门负责人 - 部门交付权
ROLE_EFFICIENCY       效率管理部 - 系统维护权
ROLE_COMPLIANCE       药政合规部 - 合规审查权
ROLE_ADMIN            系统管理员 - 管理员权限
```

### 3. permission 表
权限表，定义系统中的各种权限。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 权限 ID |
| name | VARCHAR(64) | NOT NULL, UNIQUE | 权限名称 |
| description | VARCHAR(255) | | 权限描述 |
| created_at | TIMESTAMP(3) | NOT NULL | 创建时间 |
| updated_at | TIMESTAMP(3) | NOT NULL | 更新时间 |

**索引：**
- `uk_permission_name` - UNIQUE(name)

**权限分类：**
- 里程碑操作 (MILESTONE_*)
- 预算操作 (BUDGET_*)
- 项目操作 (PROJECT_*)
- 交付物操作 (DOCUMENT_*)
- 变更操作 (CHANGE_REQUEST_*)
- 系统操作 (SYSTEM_*)

### 4. user_roles 表
用户-角色关联表（多对多）。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| user_id | BIGINT UNSIGNED | PK, FK | 用户 ID |
| role_id | BIGINT UNSIGNED | PK, FK | 角色 ID |
| created_at | TIMESTAMP(3) | NOT NULL | 创建时间 |

**外键：**
- `fk_user_roles_user` → user(id) ON DELETE CASCADE
- `fk_user_roles_role` → role(id) ON DELETE CASCADE

**索引：**
- PRIMARY KEY (user_id, role_id)
- `idx_user_roles_role` - (role_id)

**特点：**
- 一个用户可以有多个角色
- 删除用户或角色时自动清理关联数据

### 5. role_permissions 表
角色-权限关联表（多对多）。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| role_id | BIGINT UNSIGNED | PK, FK | 角色 ID |
| permission_id | BIGINT UNSIGNED | PK, FK | 权限 ID |
| created_at | TIMESTAMP(3) | NOT NULL | 创建时间 |

**外键：**
- `fk_role_permissions_role` → role(id) ON DELETE CASCADE
- `fk_role_permissions_permission` → permission(id) ON DELETE CASCADE

**索引：**
- PRIMARY KEY (role_id, permission_id)
- `idx_role_permissions_permission` - (permission_id)

**特点：**
- 一个角色可以有多个权限
- 一个权限可以被多个角色拥有
- 删除角色或权限时自动清理关联数据

## 权限矩阵

### 角色权限分配表

| 权限 | PMC | PM | DEPT_HEAD | EFFICIENCY | COMPLIANCE | ADMIN |
|------|-----|----|-----------:|:----------:|:----------:|:-----:|
| SUBMIT_REVIEW | ✓ | ✓ | | | | |
| APPROVE_MILESTONE | ✓ | | | | | |
| VIEW_MILESTONE | ✓ | ✓ | | | | ✓ |
| VIEW_BUDGET | ✓ | ✓ | ✓ | ✓ | | ✓ |
| APPROVE_BUDGET | ✓ | | | | | |
| MANAGE_BUDGET | | ✓ | | | | |
| CREATE_PROJECT | | ✓ | | | | |
| VIEW_PROJECT | ✓ | ✓ | ✓ | ✓ | | ✓ |
| EDIT_PROJECT | | ✓ | | | | ✓ |
| TERMINATE_PROJECT | | ✓ | | | | |
| UPLOAD_DOCUMENT | | ✓ | ✓ | | | |
| VIEW_DOCUMENT | ✓ | ✓ | ✓ | | ✓ | |
| REVIEW_DOCUMENT | | | | | ✓ | |
| SUBMIT_CHANGE_REQUEST | | ✓ | | | | |
| APPROVE_CHANGE_REQUEST | ✓ | | | | | |
| VIEW_CHANGE_REQUEST | ✓ | ✓ | | ✓ | | |
| MANAGE_USERS | | | | | | ✓ |
| MANAGE_ROLES | | | | | | ✓ |
| VIEW_REPORTS | | | | ✓ | | ✓ |
| SYSTEM_MAINTENANCE | | | | ✓ | | ✓ |

## 数据隔离策略

### 项目可见性

| 用户角色 | 可见项目范围 |
|---------|------------|
| ROLE_PMC | 全部项目 |
| ROLE_PM | 全部项目 + 作为 PM 的项目 |
| ROLE_DEPT_HEAD | 参与的项目 |
| ROLE_EFFICIENCY | 全部项目（只读） |
| ROLE_COMPLIANCE | 参与评审的项目 |
| ROLE_ADMIN | 全部项目 |

### 实现方式

在 `ProjectService.getVisibleProjects(username)` 中：
```java
if (roles.contains("ROLE_PMC") || roles.contains("ROLE_PM")) {
    // 查询全部项目
    projects = projectRepository.findAll();
} else {
    // 查询用户参与的项目
    projects = projectRepository.findByTeamMembers(userId);
}
```

## 查询示例

### 1. 获取用户的所有权限
```sql
SELECT DISTINCT p.id, p.name, p.description
FROM user u
JOIN user_roles ur ON u.id = ur.user_id
JOIN role r ON ur.role_id = r.id
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permission p ON rp.permission_id = p.id
WHERE u.username = 'pm_user'
ORDER BY p.name;
```

### 2. 获取角色的所有权限
```sql
SELECT p.id, p.name, p.description
FROM role r
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permission p ON rp.permission_id = p.id
WHERE r.name = 'ROLE_PM'
ORDER BY p.name;
```

### 3. 获取具有某权限的用户
```sql
SELECT DISTINCT u.id, u.username, u.email
FROM user u
JOIN user_roles ur ON u.id = ur.user_id
JOIN role r ON ur.role_id = r.id
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permission p ON rp.permission_id = p.id
WHERE p.name = 'PERMISSION_APPROVE_MILESTONE'
ORDER BY u.username;
```

### 4. 获取用户的所有角色
```sql
SELECT r.id, r.name, r.description
FROM user u
JOIN user_roles ur ON u.id = ur.user_id
JOIN role r ON ur.role_id = r.id
WHERE u.username = 'pmc_user'
ORDER BY r.name;
```

## 性能考虑

### 索引策略
- user_roles 和 role_permissions 采用 PRIMARY KEY (user_id/role_id, role_id/permission_id)，支持高效的多对多查询
- 额外索引用于反向查询（如找出具有某权限的用户）

### 查询优化建议
- 使用 JOIN 而非子查询
- 对权限检查结果进行缓存
- 在应用层实现权限缓存策略

## 扩展考虑

### 1. 权限条件化
未来可以扩展 `role_permissions` 表添加条件列，实现更细粒度的权限控制：
```sql
ALTER TABLE role_permissions ADD COLUMN condition_json JSON;
-- 例如：{"resource":"project","action":"view","constraint":"own_projects"}
```

### 2. 审计日志
添加权限变更审计表：
```sql
CREATE TABLE role_permissions_audit (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT UNSIGNED,
  permission_id BIGINT UNSIGNED,
  action ENUM('GRANT','REVOKE'),
  modified_by BIGINT UNSIGNED,
  modified_at TIMESTAMP(3),
  reason TEXT
);
```

### 3. 时间限制
在 `user_roles` 表添加生效期限制：
```sql
ALTER TABLE user_roles 
ADD COLUMN effective_from DATE,
ADD COLUMN effective_to DATE;
```