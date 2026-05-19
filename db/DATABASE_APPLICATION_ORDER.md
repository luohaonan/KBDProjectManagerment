# 数据库脚本应用顺序指南

## 概述

您的数据库演变过程分为五个阶段。在**开发阶段**，所有脚本相对独立，可灵活应用；在**生产部署**时，建议按顺序执行以确保数据完整性。

```
阶段1 (初始化)
    ↓
ddl_mysql8.sql
(创建所有基础表)
    ↓
阶段2 (第一次修改)
    ↓
alter_project_add_process_oversight_dept.sql
(给 project 表添加 process_oversight_dept_id 字段)
    ↓
阶段3 (第二次修改)
    ↓
alter_milestone_review_core.sql
(给 project_milestone 表添加 conditional_deadline 字段)
(创建 project_document 表)
(创建 milestone_history 表)
    ↓
阶段4 (权限管理系统)
    ↓
rbac_ddl.sql
(创建 5 个 RBAC 相关的新表)
    ↓
rbac_init_data.sql
(初始化角色、权限、用户数据)
    ↓
阶段5 (文档管理系统 - 现在新增)
    ↓
create_document_management.sql
(创建 document 表和 audit_log 表)
(添加文档管理权限)
```

## 答案：完全独立应用

**是的，`rbac_ddl.sql`、`rbac_init_data.sql` 和 `create_document_management.sql` 都可以直接在现有数据库基础上运行。**

### 原因

1. **完全独立的新表** - 这些脚本创建的都是全新的表，与现有的 project、milestone 等表无关联
2. **无字段冲突** - 新增的表（RBAC 5个 + 文档管理2个）不会修改现有表结构
3. **外键独立** - 新表的外键只关联到现有表，不影响现有数据

新表列表：
- `user` ✓ 新创建（RBAC）
- `role` ✓ 新创建（RBAC）
- `permission` ✓ 新创建（RBAC）
- `user_roles` ✓ 新创建（RBAC）
- `role_permissions` ✓ 新创建（RBAC）
- `document` ✓ 新创建（文档管理）
- `audit_log` ✓ 新创建（文档管理）

## 标准应用流程

### 如果是从零开始的新部署

```bash
# 1. 应用初始 DDL
mysql -u root -p kbd_pm_system < ddl_mysql8.sql

# 2. 应用第一次修改
mysql -u root -p kbd_pm_system < alter_project_add_process_oversight_dept.sql

# 3. 应用第二次修改
mysql -u root -p kbd_pm_system < alter_milestone_review_core.sql

# 4. 应用 RBAC 脚本（新增）
mysql -u root -p kbd_pm_system < rbac_ddl.sql
mysql -u root -p kbd_pm_system < rbac_init_data.sql
```

### 如果现有数据库已经应用了前三个脚本

```bash
# 只需要应用 RBAC 脚本
mysql -u root -p kbd_pm_system < rbac_ddl.sql
mysql -u root -p kbd_pm_system < rbac_init_data.sql
```

## 验证步骤

应用完 RBAC 脚本后，验证所有表是否存在：

```sql
-- 检查 RBAC 新增的表
SHOW TABLES LIKE 'user%';
SHOW TABLES LIKE 'role%';
SHOW TABLES LIKE 'permission%';

-- 应该显示：
-- user
-- user_roles
-- role
-- role_permissions
-- permission

-- 检查数据
SELECT COUNT(*) as role_count FROM `role`;        -- 应为 6
SELECT COUNT(*) as permission_count FROM `permission`;  -- 应为 21
SELECT COUNT(*) as user_count FROM `user`;        -- 应为 6
```

## RBAC 表与现有表的关系

### 与 `iam_user` 表的区别

```
┌─────────────────────────┐
│      iam_user           │ （现有表）
├─────────────────────────┤
│ user_no                 │ 员工编号
│ display_name            │ 显示名称
│ email                   │ 邮箱
│ dept_id ─────┐          │ 部门 ID
│ title        │          │ 职位
│              │          │
│   用途：组织关系 + 个人信息
└─────────────────────────┘
                 │
                 │ (可选) 关联
                 │
┌─────────────────────────┐
│        user             │ （新表）
├─────────────────────────┤
│ username                │ 登录用户名
│ password                │ 登录密码
│ email                   │ 邮箱
│ is_active               │ 激活状态
│                         │
│   用途：系统认证 + 权限控制
└─────────────────────────┘
```

**关系说明：**
- `iam_user` 表用于组织结构和人员档案管理
- `user` 表用于系统登录和权限控制
- 两个表可以通过 username 字段进行应用级关联

## 后续维护

### 如果需要回滚 RBAC 脚本

```sql
DROP TABLE IF EXISTS role_permissions;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS permission;
DROP TABLE IF EXISTS role;
DROP TABLE IF EXISTS user;
```

### 如果需要添加新用户（应用后）

```sql
INSERT INTO `user` (`username`, `password`, `email`, `is_active`) VALUES
  ('new_user', '<bcrypt_hash_of_password>', 'new@example.com', 1);

-- 分配角色
INSERT INTO `user_roles` (`user_id`, `role_id`) 
SELECT u.id, r.id FROM `user` u, `role` r 
WHERE u.username = 'new_user' AND r.name = 'ROLE_PM';
```

## 注意事项

### ✅ 安全的操作
- ✓ 直接应用 RBAC 脚本到现有数据库
- ✓ RBAC 脚本可以重复运行（使用 `IF NOT EXISTS` 和 `CREATE TABLE IF NOT EXISTS`）
- ✓ 无需修改任何现有的脚本

### ⚠️ 需要注意
- 初始密码为 `test123`，**生产环境必须修改**
- 确保 JWT_SECRET 足够长（至少 32 个字符）
- 定期审查用户权限

## 总结

| 脚本 | 表数量 | 新/改 | 应用顺序 | 依赖 |
|------|-----:|:---:|:------:|:---:|
| ddl_mysql8.sql | 10+ | 新 | 1️⃣ | 无 |
| alter_project_add_process_oversight_dept.sql | - | 改 | 2️⃣ | #1 |
| alter_milestone_review_core.sql | +2 | 改+新 | 3️⃣ | #2 |
| rbac_ddl.sql | 5 | 新 | 4️⃣ | 无 |
| rbac_init_data.sql | - | 数据 | 5️⃣ | #4 |

**`rbac_ddl.sql` 和 `rbac_init_data.sql` 完全独立应用，无需修改任何内容！**