# 文档管理数据库迁移指南

## 概述
本指南说明如何应用新增的文档管理数据库表和权限配置。

## 文件说明

### 1. `create_document_management.sql` - 表结构和权限配置
包含以下 2 个新表的 DDL 和相关权限初始化：

| 表名 | 说明 |
|------|------|
| `document` | 文档信息表，存储文件元数据和合规状态 |
| `audit_log` | 文档操作审计日志表 |

#### 文档表字段说明
- `file_name`: 文件名
- `storage_path`: 物理存储路径
- `file_type`: 文件类型（用于唯一性校验）
- `project_id`: 所属项目ID
- `milestone_phase`: 里程碑阶段（G0-G9）
- `uploader`: 上传人ID
- `compliance_status`: 合规审核状态（PENDING/APPROVED/REJECTED）
- `is_locked`: 是否锁定（归档后锁定）
- `uploaded_at`: 上传时间
- `created_at`: 创建时间

#### 审计日志表字段说明
- `user_id`: 操作人ID
- `action`: 操作动作（UPLOAD/DOWNLOAD/DELETE/REVIEW）
- `document_id`: 文档ID
- `timestamp`: 操作时间
- `details`: 操作详情

## 新增权限

脚本会自动添加以下 5 个文档管理权限：

| 权限名 | 说明 | 适用角色 |
|--------|------|----------|
| `DOCUMENT_UPLOAD` | 上传文档 | PM, 部门负责人 |
| `DOCUMENT_DOWNLOAD` | 下载文档 | PMC, PM, 合规部, 管理员 |
| `DOCUMENT_DELETE` | 删除文档 | PM |
| `DOCUMENT_REVIEW` | 审核文档合规性 | 合规部 |
| `DOCUMENT_VIEW_AUDIT` | 查看文档审计日志 | 所有相关角色 |

## 应用步骤

### 方式一：开发阶段（快速应用）
前置脚本已在之前的开发中应用过，可直接执行最新改动：

### 方式二：生产部署（完整流程）
确保所有前置条件都已满足：
- 已应用 `ddl_mysql8.sql`
- 已应用 `rbac_ddl.sql` 和 `rbac_init_data.sql`（权限系统必需）

### 执行文档管理脚本
```bash
# 开发环境：直接执行
mysql -u root -p kbd_pm_system < create_document_management.sql

# 生产环境：需确保前置脚本已应用
mysql -u root -p kbd_pm_system < create_document_management.sql
```

## 重要注意事项

### 1. 外键依赖
文档管理表依赖以下现有表：
- `project` 表（项目信息）
- `iam_user` 表（用户信息）
- `role`、`permission`、`role_permissions` 表（权限系统）

### 2. 权限自动分配
脚本会自动为以下角色分配文档权限：
- **PMC**: 下载、查看审计日志
- **PM**: 上传、下载、删除、查看审计日志
- **合规部**: 下载、审核、查看审计日志
- **管理员**: 所有文档权限

### 3. 唯一性约束
- 同一项目、同一里程碑阶段下，同类型的核心交付物不能重复
- 通过 `file_type` 字段实现业务层面的唯一性校验

### 4. 合规性权重
- 文档合规状态直接影响里程碑 GO 决策
- 只有所有核心交付物通过合规审核，PMC 才能批准 GO

### 5. 归档锁定机制
- 里程碑状态变为 GO 时，系统自动锁定该阶段所有文档
- 锁定后禁止删除或覆盖操作

## 与现有系统的关系

### 与 RBAC 系统的集成
- 文档操作权限通过 RBAC 系统控制
- 审计日志记录操作人的用户ID

### 与里程碑系统的集成
- 文档绑定到具体的里程碑阶段
- 合规审核影响里程碑评审结果
- GO 决策触发文档锁定

### 与项目系统的集成
- 文档按项目组织存储
- 项目成员可访问项目内文档

## 后续扩展

### 添加新文档类型
```sql
-- 在 document 表中添加新的 file_type
INSERT INTO `document` (file_name, storage_path, file_type, ...)
VALUES ('new_file.pdf', '/path/to/file', 'NEW_DOCUMENT_TYPE', ...);
```

### 查看文档审计日志
```sql
SELECT
  al.timestamp,
  u.username,
  al.action,
  d.file_name,
  al.details
FROM `audit_log` al
JOIN `iam_user` u ON al.user_id = u.id
JOIN `document` d ON al.document_id = d.id
WHERE d.project_id = ?
ORDER BY al.timestamp DESC;
```

### 合规审核统计
```sql
SELECT
  compliance_status,
  COUNT(*) as count
FROM `document`
WHERE project_id = ? AND milestone_phase = ?
GROUP BY compliance_status;
```

## 测试验证

应用脚本后，可验证数据完整性：

```sql
-- 检查文档管理表是否创建成功
SELECT COUNT(*) as document_tables_count
FROM information_schema.tables
WHERE table_schema = 'kbd_pm_system'
  AND table_name IN ('document', 'audit_log');
-- 期望结果：2

-- 检查文档管理权限是否添加成功
SELECT COUNT(*) as document_permissions_count
FROM `permission`
WHERE name LIKE 'DOCUMENT_%';
-- 期望结果：5

-- 检查权限分配是否正确
SELECT r.name, COUNT(rp.permission_id) as doc_permissions
FROM `role` r
JOIN `role_permissions` rp ON r.id = rp.role_id
JOIN `permission` p ON rp.permission_id = p.id
WHERE p.name LIKE 'DOCUMENT_%'
GROUP BY r.id, r.name;
-- 验证各角色分配的文档权限数量
```

## 回滚

如需回滚（谨慎使用）：
```sql
-- 删除文档管理权限分配
DELETE FROM `role_permissions`
WHERE permission_id IN (
  SELECT id FROM `permission` WHERE name LIKE 'DOCUMENT_%'
);

-- 删除文档管理权限
DELETE FROM `permission` WHERE name LIKE 'DOCUMENT_%';

-- 删除文档管理表（会级联删除审计日志）
DROP TABLE IF EXISTS `audit_log`;
DROP TABLE IF EXISTS `document`;
```