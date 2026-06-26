# 数据库管理指南

## 📦 概述

本文件夹包含 KBD 项目管理系统（`kbd_pm_system`）的完整数据库 Schema 和部署文档。数据库基于 **MySQL 8.0**，字符集为 `utf8mb4`。

## 🚀 快速部署

> **新部署**：从旧完整 dump 导入后执行迁移脚本；或直接执行迁移脚本（含完整种子数据）。

### 方式一：在现有库上执行迁移脚本

```bash
mysql -u root -p kbd_pm_system < milestone_review_flow_migration.sql
```

> 迁移脚本包含所有 DDL 变更（ALTER TABLE 增加字段、新建表）和种子数据（G0-G9 的 milestone_dept_role 数据）。

### 方式二：导出/导入完整数据库

```bash
# 导出
mysqldump -u root -p --databases kbd_pm_system \
  --add-drop-database --add-drop-table \
  --routines --triggers --single-transaction \
  --default-character-set=utf8mb4 \
  --result-file="kbd_pm_system_full.sql"

# 导入
mysql -u root -p kbd_pm_system < kbd_pm_system_full.sql
```

## 📊 数据库概览

| 指标 | 数值 |
|------|------|
| 数据库名 | `kbd_pm_system` |
| 数据库引擎 | MySQL 8.0 |
| 字符集 | utf8mb4 |
| 表数量 | 37 个（含 1 个视图） |

### 表清单

| 类别 | 表名 | 说明 |
|------|------|------|
| **组织架构** | `org_department` | 部门信息 |
| | `iam_user` | 员工信息（组织关系） |
| | `governance_committee` | 治理委员会 |
| | `governance_committee_member` | 委员会成员 |
| **项目管理** | `project` | 项目主表 |
| | `project_level` | 项目分级定义 |
| | `project_milestone` | 项目里程碑 |
| | `project_team_member` | 项目团队成员 |
| | `project_document` | 项目交付物 |
| | `project_change_request` | 变更申请 |
| | `project_termination_request` | **新增** - 项目终止申请 |
| | `project_termination_task` | 终止任务清单 |
| **预算管理** | `project_budget_plan` | 预算计划 |
| | `project_budget_ledger` | 预算明细 |
| | `project_budget_snapshot` | 预算快照 |
| | `project_budget_policy` | 预算策略 |
| | `budget_limit` | 预算上限 |
| **里程碑** | `milestone_def` | 里程碑定义字典（G0-G9） |
| | `milestone_dept_role` | 里程碑部门角色映射（含 G0-G9 种子数据） |
| | `milestone_history` | 里程碑变更历史 |
| **评审审批** | `review_record` | 评审操作记录 |
| | `review_approval` | 评审审批记录 |
| | `review_approval_task` | 评审审批任务（含 `step_code` 多步流程字段） |
| | `initiation_approval` | 立项审批 |
| | `initiation_approval_task` | 立项审批任务 |
| **权限管理** | `user` | 系统登录用户 |
| | `role` | 角色定义 |
| | `permission` | 权限定义 |
| | `user_roles` | 用户-角色关联 |
| | `role_permissions` | 角色-权限关联 |
| **文档管理** | `document` | 文档存储（含 `deliverable_slot_code` 字段） |
| | `audit_log` | 审计日志 |
| **工作流** | `wf_template` | 工作流模板 |
| | `wf_template_node` | 工作流节点 |
| | `wf_instance` | 工作流实例 |
| | `wf_task` | 工作流任务 |
| | `wf_action_log` | 工作流操作日志 |
| **视图** | `v_pending_review_tasks` | 待评审任务视图 |

## 👥 初始用户与角色

### 预定义角色

| 角色 | 标识 | 说明 |
|------|------|------|
| PMC（项目管理委员会） | `ROLE_PMC` | 最高决策权，Go/No Go 决策、重大变更审批 |
| PM（项目经理） | `ROLE_PM` | 制定计划、监控预算、组织评审、提交变更 |
| 职能部门负责人 | `ROLE_DEPT_HEAD` | 所属领域交付物提交与审批 |
| 部门执行人 | `ROLE_DEPT_EXECUTOR` | 上传交付物并发起评审 |
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

## 🗂️ 里程碑评审流程种子数据

迁移脚本 `milestone_review_flow_migration.sql` 包含 G0-G9 完整的里程碑部门角色种子数据：

| 里程碑 | 上传部门 | 部门负责人审批 | PM技术初评 | 合规意见 | 最终决策 |
|--------|----------|---------------|-----------|---------|---------|
| G0 | 新药资讯部 | 新药资讯部负责人 | ✅ | 药政合规部 | PMC并行 |
| G1 | 新药化学部+新药资讯部(并行) | 两部门负责人并行 | ✅ | 药政合规部 | **PM内部评审** |
| G2 | 新药化学部+新药资讯部(并行) | 两部门负责人并行 | ✅ | 药政合规部 | **PM内部评审** |
| G3 | 新药化学部 | 新药化学部负责人 | ✅ | 药政合规部 | PMC并行 |
| G4 | 三部门并行 | 三部门负责人并行 | ✅ | 药政合规部 | PMC并行 |
| G5 | 药政合规部 | 药政合规部负责人 | ✅ | **跳过** | PMC并行 |
| G6-G8 | 新药临床部 | 新药临床部负责人 | ✅ | 药政合规部 | PMC并行 |
| G9 | 药政合规部 | 药政合规部负责人 | ✅ | **跳过** | PMC结项 |

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

### 查看里程碑审批流程配置
```sql
SELECT md.milestone_code, md.milestone_name, d.dept_name, mdr.role_type
FROM milestone_dept_role mdr
JOIN milestone_def md ON md.id = mdr.milestone_def_id
JOIN org_department d ON d.id = mdr.dept_id
WHERE mdr.is_active = 1
ORDER BY md.sort_no, mdr.role_type, d.dept_name;
```

### 查看项目及其里程碑
```sql
SELECT p.project_name, md.milestone_code, md.milestone_name, pm.status, pm.planned_date
FROM project p
JOIN project_milestone pm ON pm.project_id = p.id
JOIN milestone_def md ON md.id = pm.milestone_id
ORDER BY p.id, md.sort_no;
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
mysql -u root -p kbd_pm_system < [backup_file].sql
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
6. **迁移脚本**：`milestone_review_flow_migration.sql` 可在现有库上直接执行，包含所有新字段和新表

---

**最后更新**: 2026-06-24
**SQL 文件**: `milestone_review_flow_migration.sql`（DDL 变更 + 种子数据）