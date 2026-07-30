# 账号管理 / 权限组管理权限体检报告

体检时间：2026-07-31  
体检范围：`backend/src/main/java`、`frontend/src`、`db/*.sql`  
体检方式：静态扫描前后端权限判断、角色判断、数据库权限种子脚本、账号管理页面权限配置逻辑。  

> 重要结论：当前系统能正常运行，不建议立即删除、合并或改名已有权限。当前权限体系存在“角色判断、权限编码判断、流程审批角色、业务身份判断”四套控制口径并存的情况，已经形成脆弱平衡。建议先观测和标记，再做展示层分类，最后分模块灰度收敛。

---

## 1. 总体结论

### 1.1 当前权限体系的主要问题

当前系统中权限控制不是单一 RBAC，而是混合模式：

1. **Spring Security 方法级角色控制**  
   例如：`@PreAuthorize("hasRole('ADMIN')")`、`@PreAuthorize("hasRole('ROLE_PM')")`。

2. **业务代码内角色判断**  
   例如：`roles.contains("ROLE_PM")`、`roles.contains("ROLE_PROJECT_ADMIN")`。

3. **业务代码内权限编码判断**  
   例如：`permissions.contains("PERMISSION_DELETE_PROJECT")`、`PERMISSION_BUDGET_MANAGE`。

4. **流程/部门/项目身份判断**  
   例如：项目 PM、项目团队 PM、部门负责人、部门执行人、当前审批节点处理人、PMC 委员会成员。

这导致账号管理里的“权限组设置”并不能完整表达真实授权逻辑：

- 有些按钮由前端权限控制；
- 有些接口由后端角色注解决定；
- 有些操作由 Service 里的业务条件决定；
- 有些流程由工作流节点、部门、审批任务决定；
- 有些权限只是数据库中存在，但当前代码几乎不用。

### 1.2 最高风险发现

发现 Spring Security `hasRole` 使用方式不一致：

- `UserService.loadUserByUsername()` 中使用：

```java
.roles(user.getRoles().stream().map(role -> role.getName().replace("ROLE_", "")).toArray(String[]::new))
```

这会把数据库中的 `ROLE_PM`、`ROLE_ADMIN` 转换成 Spring Security authority：

```text
ROLE_PM
ROLE_ADMIN
```

在 Spring Security 中：

```java
hasRole("ADMIN")
```

会检查：

```text
ROLE_ADMIN
```

这是正确写法。

但项目中存在：

```java
@PreAuthorize("hasRole('ROLE_PM')")
@PreAuthorize("hasRole('ROLE_PMC')")
@PreAuthorize("hasRole('ROLE_COMPLIANCE')")
```

这类写法在 Spring Security 语义下通常会变成检查：

```text
ROLE_ROLE_PM
ROLE_ROLE_PMC
ROLE_ROLE_COMPLIANCE
```

很可能实际不生效或总是拒绝。

涉及文件包括：

- `backend/src/main/java/com/kbd/pms/web/DocumentController.java`
- `backend/src/main/java/com/kbd/pms/service/MilestoneService.java`

这属于高风险问题。由于当前系统功能基本正常，说明很多核心操作可能没有依赖这些注解，而是依赖 Controller 无注解 + Service 业务判断来维持运行。

---

## 2. 当前识别到的角色

### 2.1 系统角色

| 角色 | 当前用途 |
|---|---|
| `ROLE_ADMIN` | 系统管理员，账号管理、部门管理、部分全局操作 |
| `ROLE_PROJECT_ADMIN` | 项目管理员，查看所有项目、创建项目、交付物导入等 |
| `ROLE_PM` | 项目经理，项目编辑、里程碑提交、流程审批、项目变更等 |
| `ROLE_PMC` | PMC 成员，决策审批、查看项目、查看交付物等 |
| `ROLE_DEPT_HEAD` | 部门负责人，审批、上传/查看部分文档、处理通知待办 |
| `ROLE_DEPT_EXECUTOR` | 部门执行人，上传交付物、处理部门执行任务 |
| `ROLE_COMPLIANCE` | 药政合规，文档审核、流程意见 |

### 2.2 非角色但形似角色的流程/节点编码

| 编码 | 含义 | 风险 |
|---|---|---|
| `ROLE_APPROVE` | 工作流节点类型，不是系统角色 | 容易和真实角色混淆 |
| `ROLE_PM_DECISION` | 出现在旧 `milestone_dept_role` 脚本中 | 可能是历史遗留流程角色 |
| `DEPT_HEAD` | 审批人规则，不是 Spring Security 角色 | 与 `ROLE_DEPT_HEAD` 并存，需区分 |
| `DEPT_EXECUTOR` | 部门执行人规则，不是 Spring Security 角色 | 与 `ROLE_DEPT_EXECUTOR` 并存，需区分 |

建议：在权限组管理 UI 中不要把流程节点编码和系统角色混在一起展示。

---

## 3. 当前识别到的权限编码

### 3.1 权限清单

| 权限编码 | 来源 | 当前代码使用情况 | 初步判断 |
|---|---|---|---|
| `PERMISSION_DELETE_PROJECT` | 代码中使用，种子来源未在本次扫描中明确发现 | 前端 Dashboard 控制删除按钮；后端 ProjectService 删除项目校验 | **有效权限，高风险** |
| `PERMISSION_APPROVE_INITIATION` | `InitiationService` | 用于查找立项审批人 | **有效权限，高风险/流程关键** |
| `PERMISSION_BUDGET_MANAGE` | `db/20260730_budget_management.sql` | BudgetService 管理预算、预算调整、支出管理校验 | **有效权限，高风险** |
| `PERMISSION_BUDGET_VIEW` | `db/20260730_budget_management.sql` | 本次扫描未发现后端/前端直接判断 | **疑似展示权限/弱使用权限** |
| `PERMISSION_DELIVERABLE_IMPORT` | `db/20260730_deliverable_permissions.sql` | 前端 DocumentList 控制导入按钮；后端 MilestoneDeliverableService 导入校验 | **有效权限，高风险** |
| `PERMISSION_DELIVERABLE_VIEW_ALL` | `db/20260730_deliverable_permissions.sql` | MilestoneDeliverableService 全量查看交付物判断 | **有效权限，中高风险** |
| `PERMISSION_DELIVERABLE_VIEW` | `db/20260730_deliverable_permissions.sql` | 本次扫描未发现直接校验 | **疑似基础展示/预留权限** |
| `PERMISSION_CREATE_PROJECT` | `db/project_admin_role_migration.sql` | 本次扫描未发现直接校验，创建按钮主要看角色 | **疑似无实际后端约束** |
| `PERMISSION_VIEW_ALL_PROJECTS` | `db/project_admin_role_migration.sql` | 本次扫描未发现直接校验，项目可见性主要看角色 | **疑似无实际后端约束** |

---

## 4. 前端权限体检

### 4.1 前端权限入口

文件：`frontend/src/contexts/AuthContext.tsx`

前端权限来自 JWT 和 `/api/users/me`：

```ts
roles: data.roles || [],
permissions: data.permissions || [],
```

判断函数：

```ts
const hasRole = (role: string) => user?.roles.includes(role) || false;
const hasPermission = (permission: string) => user?.permissions?.includes(permission) || false;
```

前端判断是纯字符串匹配，使用的是完整 `ROLE_ADMIN` / `PERMISSION_xxx`。

### 4.2 前端角色控制点

| 文件 | 控制点 | 使用角色/权限 |
|---|---|---|
| `frontend/src/App.tsx` | 工作流管理入口、PMC 页面路由 | `ROLE_ADMIN`、`ROLE_PMC` |
| `frontend/src/pages/AccountManagement.tsx` | 账号与权限管理页面 | `ROLE_ADMIN` |
| `frontend/src/pages/Dashboard.tsx` | 删除按钮、创建项目按钮、PM 待办显示 | `PERMISSION_DELETE_PROJECT`、`ROLE_ADMIN`、`ROLE_PROJECT_ADMIN`、`ROLE_PM` |
| `frontend/src/pages/CreateProject.tsx` | 创建/编辑模式逻辑 | `ROLE_ADMIN`、`ROLE_PROJECT_ADMIN` |
| `frontend/src/pages/ProjectDetail.tsx` | 编辑项目信息、项目变更、上传交付物入口 | `ROLE_PM`、`ROLE_ADMIN`、`ROLE_PROJECT_ADMIN`、`ROLE_DEPT_HEAD`、`ROLE_DEPT_EXECUTOR` |
| `frontend/src/components/DocumentList.tsx` | 历史交付物导入按钮 | `PERMISSION_DELIVERABLE_IMPORT`、`ROLE_ADMIN`、`ROLE_PROJECT_ADMIN` |
| `frontend/src/components/ProtectedRoute.tsx` | 路由级角色保护 | `roles` 参数 |
| `frontend/src/components/HasPermission.tsx` | 条件渲染组件 | 实际按角色判断，命名容易误导 |

### 4.3 前端发现的问题

#### 问题 1：`HasPermission` 命名不准确

文件：`frontend/src/components/HasPermission.tsx`

组件名叫 `HasPermission`，但参数是：

```ts
roles: string[]
```

内部实际判断：

```ts
roles.some(role => hasRole(role))
```

它本质是 `HasRole`，不是 `HasPermission`。这会误导后续开发者，以为它支持权限编码。

建议：短期保留兼容，新增 `HasRole` 和真正的 `HasPermission`，逐步替换。

#### 问题 2：前端“可见”不代表后端“可执行”

例如创建项目按钮主要由：

```ts
hasRole('ROLE_ADMIN') || hasRole('ROLE_PROJECT_ADMIN')
```

控制，但数据库存在 `PERMISSION_CREATE_PROJECT`，本次扫描未发现后端直接校验该权限。

这意味着账号管理中修改 `PERMISSION_CREATE_PROJECT` 可能不会影响真实创建能力。

---

## 5. 后端权限体检

### 5.1 后端安全入口

文件：`backend/src/main/java/com/kbd/pms/security/SecurityConfig.java`

当前配置：

```java
.requestMatchers("/api/auth/**").permitAll()
.anyRequest().authenticated()
```

含义：

- 除登录接口外，所有 API 都要求登录；
- 具体角色/权限控制依赖 `@PreAuthorize` 或业务代码手动判断。

### 5.2 Spring Security 角色注解问题

正确写法应统一为：

```java
@PreAuthorize("hasRole('ADMIN')")
@PreAuthorize("hasRole('PM')")
@PreAuthorize("hasRole('PMC')")
@PreAuthorize("hasRole('COMPLIANCE')")
```

或者统一改用：

```java
@PreAuthorize("hasAuthority('ROLE_PM')")
```

当前项目混用：

```java
@PreAuthorize("hasRole('ADMIN')")       // 正常
@PreAuthorize("hasRole('ROLE_PM')")     // 高风险
@PreAuthorize("hasRole('ROLE_PMC')")    // 高风险
```

建议不要马上全局替换，应先加测试或手动验证这些接口是否当前被前端调用。

### 5.3 Controller 层保护情况

| Controller | 路径 | Controller 层权限 | 风险 |
|---|---|---|---|
| `UserController` | `/api/users` | 管理类接口多数 `hasRole('ADMIN')` | 较安全 |
| `DepartmentController` | `/api/departments` | 多数 `hasRole('ADMIN')` | 较安全 |
| `WfProcessController` | 工作流配置 | `hasRole('ADMIN')` | 较安全 |
| `DocumentController` | `/api/documents` | 使用 `hasRole('ROLE_PM')` 等 | **注解写法高风险** |
| `ProjectController` | `/api/projects` | 多数无注解，依赖 Service | 需确认 Service 覆盖完整性 |
| `MilestoneDeliverableController` | `/api/milestone-deliverables` | 无注解，依赖 Service | 需重点审计 |
| `BudgetController` | `/api/budgets` | 无注解，依赖 Service | 需重点审计 |
| `ReviewController` | `/api/reviews` | 无注解，依赖 Service/任务归属 | 高风险但符合流程型业务 |
| `InitiationController` | `/api/initiations` | 无注解，依赖 Service/任务归属 | 高风险但符合流程型业务 |
| `NotificationController` | `/api/notifications` | 无注解，依赖当前用户 | 中风险 |
| `SystemConfigController` | `/api/system/config` | 本次扫描未见 `@PreAuthorize` | **高风险：系统配置应仅管理员** |

---

## 6. 隐式业务限制清单

这些限制不完全由账号管理中的权限控制，而是在业务代码中动态判断。

### 6.1 项目管理

文件：`backend/src/main/java/com/kbd/pms/service/ProjectService.java`

- 删除项目：依赖 `PERMISSION_DELETE_PROJECT`；
- 项目列表：`ROLE_PMC`、`ROLE_PM`、`ROLE_PROJECT_ADMIN` 可看全部，但 else 分支当前也返回全部项目；
- 项目详情：对非 `ROLE_PMC` / `ROLE_PM` 暂未强限制，仅注释说明未来应检查参与关系。

风险：项目查看权限和 `PERMISSION_VIEW_ALL_PROJECTS` 当前不一致，可能存在“权限组配置了但代码不用”的问题。

### 6.2 预算管理

文件：`backend/src/main/java/com/kbd/pms/service/BudgetService.java`

预算查看条件：

- 项目 PM；
- 项目管理员；
- 当前预算审批人；
- 预算申请相关人；
- `ROLE_ADMIN`。

预算管理条件：

- 拥有 `PERMISSION_BUDGET_MANAGE` 或 `ROLE_ADMIN`；
- 并且是项目 PM / 项目管理员 / `ROLE_ADMIN`。

说明：这是“权限 + 业务身份”组合模型。账号管理里只勾选 `PERMISSION_BUDGET_MANAGE` 并不一定能管理所有预算。

### 6.3 交付物管理

文件：`backend/src/main/java/com/kbd/pms/service/MilestoneDeliverableService.java`

核心规则：

- 上传：`ROLE_ADMIN`、项目 PM、执行部门的 `ROLE_DEPT_EXECUTOR`；
- 导入：`PERMISSION_DELIVERABLE_IMPORT` 或 `ROLE_ADMIN` / `ROLE_PROJECT_ADMIN`；
- 全量查看：`PERMISSION_DELIVERABLE_VIEW_ALL`，或兼容 `ROLE_ADMIN` / `ROLE_PROJECT_ADMIN` / `ROLE_PMC`；
- 删除：`ROLE_ADMIN`、项目 PM、上传者本人、执行部门执行人。

说明：这是典型的混合权限，不能简单在权限组里取消某个权限后期待行为完全变化。

### 6.4 文档管理

文件：`backend/src/main/java/com/kbd/pms/service/DocumentService.java`

规则包括：

- `ROLE_ADMIN` 全量访问；
- 项目 PM 全量访问；
- PMC 委员会成员全量访问；
- `ROLE_DEPT_EXECUTOR` 只能看自己上传；
- `ROLE_COMPLIANCE` / `ROLE_ADMIN` 查看待审核文档。

风险：Controller 注解中存在 `hasRole('ROLE_PM')` 写法，可能与 Service 规则产生冲突。

### 6.5 流程审批

涉及文件：

- `backend/src/main/java/com/kbd/pms/service/ReviewService.java`
- `backend/src/main/java/com/kbd/pms/workflow/WfProcessService.java`
- `db/workflow_engine_migration.sql`

审批权限不是单纯角色，而是：

- 当前任务是否分配给该用户；
- 审批节点类型；
- 审批人规则：`DEPT_HEAD`、`ROLE_PM`、`ROLE_PMC`、`ROLE_COMPLIANCE`、`SPECIFIC_USER`；
- 项目 PM、PMC 委员会、部门负责人配置。

这类权限属于最高风险，不建议纳入普通权限合并。

---

## 7. 疑似无效或弱生效权限

以下权限不建议删除，只建议标记。

| 权限 | 问题 | 建议 |
|---|---|---|
| `PERMISSION_CREATE_PROJECT` | 数据库脚本存在，但前后端主要看 `ROLE_ADMIN` / `ROLE_PROJECT_ADMIN` | 标记为“弱生效/待接入” |
| `PERMISSION_VIEW_ALL_PROJECTS` | 数据库脚本存在，但项目可见性主要看角色，且 else 分支也返回全部 | 标记为“弱生效/待重构” |
| `PERMISSION_BUDGET_VIEW` | 数据库脚本存在，但预算查看逻辑主要看业务身份 | 标记为“展示权限/待接入” |
| `PERMISSION_DELIVERABLE_VIEW` | 数据库脚本存在，但本次未发现直接校验 | 标记为“基础权限/待接入” |

---

## 8. 高风险权限和模块

### 8.1 高风险权限

| 权限/角色 | 风险原因 |
|---|---|
| `ROLE_ADMIN` | 系统级管理权限，影响账号、部门、工作流、系统配置 |
| `PERMISSION_DELETE_PROJECT` | 删除项目会级联删除大量业务数据 |
| `PERMISSION_APPROVE_INITIATION` | 影响立项审批人分配 |
| `PERMISSION_BUDGET_MANAGE` | 影响预算调整、支出管理 |
| `PERMISSION_DELIVERABLE_IMPORT` | 可导入历史交付物，影响项目资料完整性 |
| `ROLE_PMC` | 决策权限，影响关键里程碑和项目状态 |
| `ROLE_PM` | 项目管理、流程提交、预算、变更等多处使用 |
| `ROLE_DEPT_HEAD` / `DEPT_HEAD` | 部门审批逻辑，同时存在角色和部门负责人身份判断 |

### 8.2 高风险模块

| 模块 | 风险等级 | 原因 |
|---|---|---|
| 账号与权限管理 | 高 | 可直接修改角色和权限绑定 |
| 项目删除 | 高 | 级联删除数据，强依赖 `PERMISSION_DELETE_PROJECT` |
| 工作流配置 | 高 | 改节点会影响审批任务流转 |
| 里程碑/评审流程 | 高 | 多角色、多节点、多业务状态耦合 |
| 预算管理 | 高 | 金额、审批、项目身份组合控制 |
| 交付物管理 | 中高 | 上传、导入、删除、全量查看规则复杂 |
| 系统配置 | 高 | 当前 Controller 未见管理员注解，应尽快确认 |

---

## 9. 权限组管理页面的问题

文件：`frontend/src/pages/AccountManagement.tsx`

当前页面问题：

1. 权限列表平铺展示，未分类；
2. 没有区分“真实生效权限”“弱生效权限”“流程关键权限”“高危权限”；
3. 编辑角色权限时会整体覆盖：

```ts
api.put(`/api/users/roles/${showPermissionModal.id}/permissions`, { permissions: selectedPermissions })
```

后端：`UserController.updateRolePermissions()` 会执行：

```java
role.setPermissions(permissions);
```

这意味着一次保存会覆盖该角色所有权限。对高风险角色非常危险。

建议至少增加：

- 高危权限确认弹窗；
- 修改前后 diff 展示；
- 权限变更审计日志；
- 禁止删除内置角色；
- 禁止普通管理员修改 `ROLE_ADMIN`；
- 对流程关键权限加“影响说明”。

---

## 10. 治理建议

### 10.1 立即做，但不改变业务行为

1. **冻结现有权限语义**  
   不删除、不改名、不合并底层权限。

2. **给权限加分类和状态字段**  
   建议分类：
   - 项目管理；
   - 预算管理；
   - 交付物管理；
   - 评审/审批流程；
   - 账号与组织；
   - 系统配置。

   建议状态：
   - 有效；
   - 弱生效；
   - 预留；
   - 疑似废弃；
   - 高风险；
   - 流程关键。

3. **账号管理 UI 先做展示层分类**  
   不改变数据库权限编码，不改变 Service 判断逻辑。

4. **给高风险权限加提示**  
   例如删除项目、预算管理、审批权限、交付物导入。

### 10.2 优先修复但需要测试的点

1. **统一 Spring Security 注解写法**

   二选一：

   方案 A：统一 `hasRole('PM')`：

   ```java
   @PreAuthorize("hasRole('PM')")
   ```

   方案 B：统一 `hasAuthority('ROLE_PM')`：

   ```java
   @PreAuthorize("hasAuthority('ROLE_PM')")
   ```

   推荐方案 B，因为数据库和前端都是完整 `ROLE_` 字符串，认知一致。

2. **检查 `SystemConfigController` 是否需要管理员限制**

   `/api/system/config/mail` 更新邮件配置、测试邮件等应至少限制 `ROLE_ADMIN`。

3. **补充权限审计日志**

   在关键 Service 判断失败/成功时记录：用户、角色、权限、业务对象、判断结果。

### 10.3 中期治理

1. 建立权限矩阵：接口 × 前端入口 × 后端校验 × 业务条件；
2. 将弱生效权限标记出来，不再让管理员误以为“勾选就生效”；
3. 对项目查看、创建、预算查看等权限逐步接入统一权限服务；
4. 对流程审批保持业务条件模型，不要强行简化成普通权限。

---

## 11. 建议的权限分类草案

### 项目管理

- `PERMISSION_CREATE_PROJECT`：创建项目，当前疑似弱生效；
- `PERMISSION_VIEW_ALL_PROJECTS`：查看全部项目，当前疑似弱生效；
- `PERMISSION_DELETE_PROJECT`：删除项目，高风险有效权限。

### 预算管理

- `PERMISSION_BUDGET_VIEW`：预算查看，当前疑似弱生效；
- `PERMISSION_BUDGET_MANAGE`：预算管理，有效，高风险。

### 交付物管理

- `PERMISSION_DELIVERABLE_VIEW`：基础查看，当前疑似弱生效；
- `PERMISSION_DELIVERABLE_VIEW_ALL`：全量查看，有效；
- `PERMISSION_DELIVERABLE_IMPORT`：历史导入，有效，高风险。

### 审批流程

- `PERMISSION_APPROVE_INITIATION`：立项审批人候选权限，有效，流程关键。

### 系统角色

- `ROLE_ADMIN`
- `ROLE_PROJECT_ADMIN`
- `ROLE_PM`
- `ROLE_PMC`
- `ROLE_DEPT_HEAD`
- `ROLE_DEPT_EXECUTOR`
- `ROLE_COMPLIANCE`

---

## 12. 推荐下一步执行顺序

### 第一步：只做可视化和标记

在权限组管理页面中：

- 按模块分组展示权限；
- 标记高风险/弱生效/流程关键；
- 增加权限说明；
- 保存前展示变更 diff。

这一步不会影响现有功能，最安全。

### 第二步：补权限审计日志

优先加在：

- 项目删除；
- 预算管理；
- 交付物导入/删除；
- 工作流审批；
- 系统配置更新。

### 第三步：修复 `hasRole('ROLE_xxx')` 问题

先写回归测试或手动验证以下接口，再统一改为 `hasAuthority('ROLE_xxx')`：

- `DocumentController.uploadDocument`
- `DocumentController.reviewDocument`
- `DocumentController.getPendingReviews`
- `MilestoneService.submitReview`
- `MilestoneService.rescheduleMilestone`
- `MilestoneService.executeDecision`

### 第四步：逐步接入真正权限编码

例如：

- 创建项目接入 `PERMISSION_CREATE_PROJECT`；
- 查看所有项目接入 `PERMISSION_VIEW_ALL_PROJECTS`；
- 预算查看接入 `PERMISSION_BUDGET_VIEW`；
- 交付物基础查看接入 `PERMISSION_DELIVERABLE_VIEW`。

### 第五步：废弃无效权限

至少观察 1-2 个版本后，再隐藏或废弃长期未使用权限。

---

## 13. 最终建议

当前不要直接“大清洗权限”。更稳妥的目标是：

> 先让权限可理解、可分类、可观测、可回滚，再逐步让权限真正生效。

本次体检建议优先处理：

1. 权限组页面分类和风险标记；
2. `hasRole('ROLE_xxx')` 注解风险验证；
3. 系统配置接口管理员保护确认；
4. 高风险操作审计日志；
5. 弱生效权限逐步接入真实后端校验。
