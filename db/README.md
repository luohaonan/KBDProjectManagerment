# 数据库脚本管理指南

## 📋 脚本概览

本文件夹包含所有数据库相关的脚本，分为九个演变阶段。

| 阶段 | 脚本文件 | 类型 | 说明 |
|------|---------|------|------|
| 1️⃣ | `ddl_mysql8.sql` | DDL | 初始化所有基础表（初始部署必需） |
| 2️⃣ | `alter_project_add_process_oversight_dept.sql` | DDL | 添加流程监管部门字段 |
| 3️⃣ | `alter_milestone_review_core.sql` | DDL | 添加里程碑评审相关表和字段 |
| 4️⃣ | `rbac_ddl.sql` | DDL | 创建权限管理（RBAC）系统表 |
| 4️⃣ | `rbac_init_data.sql` | DML | 初始化 RBAC 系统数据（角色、权限、用户） |
| 5️⃣ | `create_document_management.sql` | DDL+DML | 创建文档管理表和权限 |
| 6️⃣ | `alter_project_change_request_fields.sql` | DDL | 添加变更申请和终止任务表字段 |
| 7️⃣ | `alter_add_budget_limit_table.sql` | DDL | 添加缺失的budget_limit表 |
| 8️⃣ | `alter_lob_text_columns_to_tinytext.sql` | DDL | 修复 Hibernate @Lob 与 MySQL 列类型映射 |
| 9️⃣ | `alter_project_add_initiation_fields.sql` | DDL | 扩展 project 表，增加立项表单完整字段 |
| 🔟 | `alter_project_add_date_fields_and_risk.sql` | DDL | 扩展 project 表，增加日期字段、阶段预算、风险评估、建议与所需支持 |
| 🔍 | `verify_migration.sql` | 验证 | 检查所有脚本是否正确应用 |

## 🚀 快速开始

### 新部署（从零开始）

建议按顺序执行以确保完整性：

```bash
cd db

# 依次执行脚本
mysql -u root -p kbd_pm_system < ddl_mysql8.sql
mysql -u root -p kbd_pm_system < alter_project_add_process_oversight_dept.sql
mysql -u root -p kbd_pm_system < alter_milestone_review_core.sql
mysql -u root -p kbd_pm_system < rbac_ddl.sql
mysql -u root -p kbd_pm_system < rbac_init_data.sql
mysql -u root -p kbd_pm_system < create_document_management.sql
mysql -u root -p kbd_pm_system < alter_project_change_request_fields.sql
mysql -u root -p kbd_pm_system < alter_add_budget_limit_table.sql
mysql -u root -p kbd_pm_system < alter_lob_text_columns_to_tinytext.sql
mysql -u root -p kbd_pm_system < alter_project_budget_snapshot_snapshot_month_varchar.sql
mysql -u root -p kbd_pm_system < alter_project_add_initiation_fields.sql
mysql -u root -p kbd_pm_system < alter_project_add_date_fields_and_risk.sql

# 验证迁移
mysql -u root -p kbd_pm_system < verify_migration.sql
```

### 已有数据库，仅添加权限管理

```bash
cd db

# 只需执行 RBAC 脚本
mysql -u root -p kbd_pm_system < rbac_ddl.sql
mysql -u root -p kbd_pm_system < rbac_init_data.sql

# 验证迁移
mysql -u root -p kbd_pm_system < verify_migration.sql
```

## 📚 详细文档

| 文档 | 内容 | 适用场景 |
|------|------|--------|
| `DATABASE_APPLICATION_ORDER.md` | 脚本应用顺序详解 | 理解整个演变过程 |
| `RBAC_MIGRATION_GUIDE.md` | RBAC 系统迁移指南 | 部署权限管理系统 |
| `DOCUMENT_MANAGEMENT_MIGRATION_GUIDE.md` | 文档管理迁移指南 | 部署文档管理系统 |
| `DATABASE_SCHEMA.md` | 数据库架构和设计 | 深入了解表结构 |

## ✅ 关键问题解答

### Q1: 开发阶段是否需要严格按顺序执行?

**不需要！** 在开发阶段，所有脚本都已使用 `IF NOT EXISTS` 和 `INSERT IGNORE`：
- ✓ 可以单独执行最新的脚本
- ✓ 支持脚本重复运行
- ✓ 无需按顺序执行前置脚本
- ✓ 自动忽略已存在的对象

### Q2: 生产环境是否需要按顺序执行?

**建议按顺序执行！** 虽然脚本相对独立，但为确保数据完整性：
- ✓ 按阶段顺序执行确保逻辑清晰
- ✓ 避免遗漏任何依赖的前置配置
- ✓ 便于问题排查和演变追踪

### Q3: 如何验证迁移成功？

运行验证脚本：
```bash
mysql -u root -p kbd_pm_system < verify_migration.sql
```

应该看到：
- 基础表 ≥ 15 个
- RBAC 表 = 5 个
- 文档管理表 = 2 个
- 角色 = 6 个
- 权限 ≥ 26 个（包含文档管理权限）
- 用户 = 6 个

### Q3: 如果需要回滚 RBAC？

```sql
-- 安全的回滚（只删除 RBAC 表，保留其他表）
DROP TABLE IF EXISTS role_permissions;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS permission;
DROP TABLE IF EXISTS role;
DROP TABLE IF EXISTS user;
```

## 🔄 数据库演变流程

```
第1阶段 → 第2阶段 → 第3阶段 → 第4阶段 → 第5阶段
   ↓          ↓          ↓          ↓          ↓
基础表    流程部门    里程碑系统   权限管理   文档管理
  15+        2+         2+         5个        2个
 个表      字段       新表         新表       新表

开发阶段：可单独执行第5阶段脚本（前置脚本已应用）
生产部署：建议按顺序执行所有阶段
```

## 🎯 每个脚本的作用

### 1. `ddl_mysql8.sql` 
**初始化核心数据库结构**
- 部门、员工、委员会管理
- 项目分级、里程碑字典
- 项目、预算、审批等核心表

### 2. `alter_project_add_process_oversight_dept.sql`
**扩展：流程监管部门追踪**
- 给 `project` 表添加 `process_oversight_dept_id` 字段
- 用于追踪项目的流程监管部门

### 3. `alter_milestone_review_core.sql`
**扩展：里程碑评审功能**
- 给 `project` 表添加 `terminated_reason` 字段
- 给 `project_milestone` 表添加 `conditional_deadline` 字段
- 创建 `project_document` 表（交付物管理）
- 创建 `milestone_history` 表（评审历史追踪）

### 4. `rbac_ddl.sql`
**新增：权限管理系统**
- 创建 `user` 表（系统登录）
- 创建 `role` 表（角色定义）
- 创建 `permission` 表（权限定义）
- 创建 `user_roles` 表（用户-角色映射）
- 创建 `role_permissions` 表（角色-权限映射）

### 5. `rbac_init_data.sql`
**初始化：权限系统数据**
- 6 个预定义角色：PMC、PM、部门负责人、效率部、合规部、管理员
- 21 个权限：涵盖里程碑、预算、项目、交付物、变更、系统等
- 6 个测试用户，密码均为 `test123`（**生产环境必改**）

### 6. `create_document_management.sql`
**新增：文档管理系统**
- 创建 `document` 表（文档信息存储）
- 创建 `audit_log` 表（操作审计日志）
- 添加 5 个文档管理权限
- 为相关角色自动分配文档权限

### 7. `verify_migration.sql`
**验证：检查迁移完整性**
- 检查所有表是否创建
- 验证关键字段是否存在
- 统计数据完整性
- 生成最终报告

### 9. `alter_project_add_initiation_fields.sql`
**扩展：项目立项表单完整字段**
- 给 `project` 表添加 12 个新字段
- 项目描述（description）
- 生物学机制（mechanism）
- 未满足的临床需求（unmet_needs）
- 科学依据（scientific_basis）
- 预期适应症（expected_indication）
- 给药途径（administration_route）
- 剂型（dosage_form）
- 剂量频率（dosage_frequency）
- 预期疗效指标（efficacy_target）
- 安全性优势（safety_advantage）
- 差异化优势（differentiation）
- 总预算（budget_total）

### 10. `alter_project_add_date_fields_and_risk.sql`
**扩展：项目日期、阶段预算、风险评估、建议与所需支持**
- 给 `project` 表添加 8 个新字段
- 预估PCC提名日期（planned_pcc_date）- 对应里程碑G0的计划日期
- 预估IND获批日期（planned_ind_date）- 对应里程碑G5的计划日期
- 预估NDA获批日期（planned_nda_date）- 对应里程碑G9的计划日期
- 阶段预算至PCC（budget_to_pcc）
- 科学风险（risk_scientific）- 靶点有效性风险、成药性风险、安全性风险
- 竞争风险（risk_competitive）- 主要竞品进展
- 注册风险（risk_regulatory）- 法规路径不确定性
- 建议与所需支持（suggestion_and_support）

## ⚠️ 重要提醒

### 生产环境

1. **修改初始密码**
   - 所有测试用户密码均为 `test123`
   - 必须修改为强密码

2. **配置 JWT**
   ```bash
   export JWT_SECRET="your-secret-key-at-least-32-characters"
   export JWT_EXPIRATION=86400000
   ```

3. **审查权限**
   - 确保每个角色的权限分配正确
   - 定期审查用户权限

### 备份

执行任何修改脚本前，建议备份数据库：
```bash
mysqldump -u root -p kbd_pm_system > kbd_pm_system_backup_$(date +%Y%m%d).sql
```

## 📞 故障排查

### 问题：表已存在错误
```
Error: Table 'user' already exists
```
✓ 解决：脚本已使用 `IF NOT EXISTS`，可安全重试

### 问题：外键约束错误
```
Error: Cannot add or update a child row
```
✓ 解决：确认所有前置阶段脚本都已应用

### 问题：字符编码问题
```
Error: Incorrect string value
```
✓ 解决：确保 MySQL 使用 `utf8mb4` 编码

## 🔍 常用查询

### 查看所有表
```sql
SHOW TABLES;
```

### 查看表结构
```sql
DESCRIBE user;
DESC project;
```

### 查看用户权限
```sql
SELECT DISTINCT p.name, p.description
FROM user u
JOIN user_roles ur ON u.id = ur.user_id
JOIN role r ON ur.role_id = r.id
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permission p ON rp.permission_id = p.id
WHERE u.username = 'pm_user';
```

### 查看外键关系
```sql
SELECT CONSTRAINT_NAME, TABLE_NAME, REFERENCED_TABLE_NAME
FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = 'kbd_pm_system';
```

## 📊 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.3 | 2026-05-12 | 扩展 project 表，增加日期字段、阶段预算、风险评估、建议与所需支持（8个新字段） |
| 1.2 | 2026-05-07 | 扩展 project 表，增加立项表单完整字段（12个新字段） |
| 1.1 | 2026-04-27 | 添加文档管理模块，支持开发阶段灵活应用 |
| 1.0 | 2026-04-27 | 初始版本，包含 RBAC 系统 |

## 🎓 学习资源

- [MySQL 官方文档](https://dev.mysql.com/doc/)
- [Spring Security 指南](https://spring.io/projects/spring-security)
- [JWT 标准](https://jwt.io)

---

**最后更新**: 2026-04-27
**维护人**: 系统开发团队