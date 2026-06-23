# 数据库管理指南

## 📦 概述

本文件夹包含 KBD 项目管理系统（`kbd_pm_system`）的完整数据库导出文件和部署文档。数据库基于 **MySQL 8.0**，字符集为 `utf8mb4`。

## 🚀 快速部署

### 一键导入（推荐）

```bash
# 1. 创建数据库（如尚未创建）
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS kbd_pm_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. 导入完整数据库（含表结构 + 种子数据）
mysql -u root -p kbd_pm_system < kbd_pm_system_full.sql
```

> 导入后即获得与当前开发环境完全一致的数据库，包含所有表、索引、外键、视图和种子数据。

## 📊 数据库概览

| 指标 | 数值 |
|------|------|
| 数据库名 | `kbd_pm_system` |
| 数据库引擎 | MySQL 8.0 |
| 字符集 | utf8mb4 |
| 表数量 | 36 个（含 1 个视图） |
| 总行数 | ~240 行（含种子数据） |

### 表清单

| 类别 | 表名 | 行数 | 说明 |
|------|------|:---:|------|
| **组织架构** | `org_department` | 10 | 部门信息 |
| | `iam_user` | 6 | 员工信息（组织关系） |
| | `governance_committee` | 0 | 治理委员会 |
| | `governance_committee_member` | 0 | 委员会成员 |
| **项目管理** | `project` | 6 | 项目主表 |
| | `project_level` | 7 | 项目分级定义 |
| | `project_milestone` | 60 | 项目里程碑 |
| | `project_team_member` | 0 | 项目团队成员 |
| | `project_document` | 0 | 项目交付物 |
| | `project_change_request` | 0 | 变更申请 |
| | `project_termination_task` | 0 | 终止任务 |
| **预算管理** | `project_budget_plan` | 0 | 预算计划 |
| | `project_budget_ledger` | 0 | 预算明细 |
| | `project_budget_snapshot` | 6 | 预算快照 |
| | `project_budget_policy` | 6 | 预算策略 |
| | `budget_limit` | 0 | 预算上限 |
| **里程碑** | `milestone_def` | 10 | 里程碑定义字典 |
| | `milestone_dept_role` | 0 | 里程碑部门角色 |
| | `milestone_history` | 0 | 里程碑变更历史 |
| **评审审批** | `review_record` | 0 | 评审记录 |
| | `review_approval` | 0 | 评审审批 |
| | `review_approval_task` | 0 | 评审审批任务 |
| | `initiation_approval` | 2 | 立项审批 |
| | `initiation_approval_task` | 3 | 立项审批任务 |
| **权限管理** | `user` | 6 | 系统登录用户 |
| | `role` | 6 | 角色定义 |
| | `permission` | 29 | 权限定义 |
| | `user_roles` | 12 | 用户-角色关联 |
| | `role_permissions` | 73 | 角色-权限关联 |
| **文档管理** | `document` | 0 | 文档存储 |
| | `audit_log` | 0 | 审计日志 |
| **工作流** | `wf_template` | 0 | 工作流模板 |
| | `wf_template_node` | 0 | 工作流节点 |
| | `wf_instance` | 0 | 工作流实例 |
| | `wf_task` | 0 | 工作流任务 |
| | `wf_action_log` | 0 | 工作流操作日志 |
| **视图** | `v_pending_review_tasks` | - | 待评审任务视图 |

## 👥 初始用户与角色

### 预定义角色

| 角色 | 标识 | 说明 |
|------|------|------|
| PMC（项目管理委员会） | `ROLE_PMC` | 最高决策权，Go/No Go 决策、重大变更审批 |
| PM（项目经理） | `ROLE_PM` | 制定计划、监控预算、组织评审、提交变更 |
| 职能部门负责人 | `ROLE_DEPT_HEAD` | 所属领域交付物提交 |
| 效率管理部 | `ROLE_EFFICIENCY` | 系统维护、预算核算、预警监控 |
| 药政合规部 | `ROLE_COMPLIANCE` | 合规性意见出具、申报文档审查 |
| 系统管理员 | `ROLE_ADMIN` | 系统管理和配置 |

### 初始用户

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| `admin_user` | `test123` | ADMIN（全角色） | 系统管理员 |
| `pmc_user` | `test123` | PMC | 项目管理委员会 |
| `pm_user` | `test123` | PM | 项目经理 |
| `dept_head` | `test123` | DEPT_HEAD | 部门负责人 |
| `efficiency_user` | `test123` | EFFICIENCY | 效率管理部 |
| `compliance_user` | `test123` | COMPLIANCE | 药政合规部 |

> ⚠️ **生产环境必须修改所有初始密码！** 密码使用 BCrypt 加密存储。

## 🗂️ 种子数据说明

导入后数据库包含以下示例数据，用于开发和测试：

- **6 个示例项目**：涵盖不同阶段的新药研发项目
- **60 个里程碑**：每个项目包含 G0~G9 共 10 个里程碑
- **10 个里程碑定义**：G0（靶点确认）到 G9（NDA 获批）
- **10 个部门**：研发各部门组织架构
- **6 个员工**：对应初始用户的组织人员信息
- **7 个项目等级**：S/A/B/C/D 等级及预算策略
- **2 个立项审批**：示例审批流程数据

## 🔍 常用查询

### 查看用户权限
```sql
SELECT DISTINCT pe.name, pe.description
FROM user u
JOIN user_roles ur ON u.id = ur.user_id
JOIN role r ON ur.role_id = r.id
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permission pe ON rp.permission_id = pe.id
WHERE u.username = 'pm_user'
ORDER BY pe.name;
```

### 查看项目及其里程碑
```sql
SELECT p.id, p.name AS project, m.name AS milestone, pm.status, pm.planned_date
FROM project p
JOIN project_milestone pm ON pm.project_id = p.id
JOIN milestone_def m ON m.id = pm.milestone_def_id
ORDER BY p.id, pm.milestone_def_id;
```

### 查看外键关系
```sql
SELECT CONSTRAINT_NAME, TABLE_NAME, REFERENCED_TABLE_NAME
FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = 'kbd_pm_system';
```

## 💾 备份与恢复

### 导出当前数据库
```bash
mysqldump -u root -p --databases kbd_pm_system \
 --add-drop-database --add-drop-table \
 --routines --triggers --single-transaction \
 --default-character-set=utf8mb4 \
 --result-file="kbd_pm_system_$(date +%Y%m%d_%H%M%S).sql"
```

### 恢复数据库
```bash
mysql -u root -p kbd_pm_system < kbd_pm_system_full.sql
```

## ⚠️ 注意事项

1. **密码安全**：初始密码为 `test123`，生产环境必须修改
2. **JWT 配置**：确保 `JWT_SECRET` 至少 32 个字符（见 `application.yml`）
3. **字符编码**：必须使用 `utf8mb4`，否则中文数据可能乱码
4. **MySQL 版本**：需要 MySQL 8.0+（使用了 CTE、窗口函数等特性）
5. **外键约束**：导入时如遇外键错误，可临时关闭外键检查：
  ```sql
  SET FOREIGN_KEY_CHECKS = 0;
  -- 执行导入
  SET FOREIGN_KEY_CHECKS = 1;
  ```

---

**最后更新**: 2026-06-23
**导出文件**: `kbd_pm_system_full.sql`（包含完整的 DDL + DML）