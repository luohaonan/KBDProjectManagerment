# RBAC 实现总结与部署指南

## 概述
本文档总结了基于角色的访问控制（RBAC）系统的实现，包括后端权限控制和前端路由保护。

## 数据库变更

### 新增表（5 个）
已在 `db/rbac_ddl.sql` 中定义：

```
user ──────────┐
               ├──> user_roles ──────────┐
role ──────────┤                         │
               ├──> role_permissions ────┼──> permission
               └──────────────────────────
```

### 初始化数据
已在 `db/rbac_init_data.sql` 中定义：
- 6 个预定义角色
- 21 个权限
- 6 个测试用户

详见：`db/RBAC_MIGRATION_GUIDE.md`

## 后端实现

### 1. 核心类

#### 实体类
- `com.kbd.pms.entity.User` - 认证用户
- `com.kbd.pms.entity.Role` - 角色
- `com.kbd.pms.entity.Permission` - 权限

#### Repository
- `UserRepository` - 用户查询
- `RoleRepository` - 角色查询
- `PermissionRepository` - 权限查询

#### 服务类
- `UserService` - 实现 `UserDetailsService`，用于加载用户信息和权限
- `ProjectService` - 实现数据隔离，根据用户角色查询可见项目

#### 安全配置
- `SecurityConfig` - Spring Security 核心配置
- `JwtUtils` - JWT 令牌生成和验证
- `AuthTokenFilter` - JWT 过滤器
- `AuthEntryPointJwt` - 异常处理

#### 控制器
- `AuthController` - 登录接口

### 2. 权限检查注解

#### 方法级权限控制
```java
@PreAuthorize("hasRole('ROLE_PM')")
public void submitReview(long projectMilestoneId, long actorUserId)

@PreAuthorize("hasRole('ROLE_PMC')")
public void executeDecision(long projectMilestoneId, MilestoneApproveRequest req)
```

### 3. 数据隔离实现

在 `ProjectService` 中实现：
```java
public List<ProjectDetailResponse> getVisibleProjects(String username)
```

根据用户角色：
- `ROLE_PMC` 和 `ROLE_PM` - 查看全部项目
- 其他角色 - 仅查看参与项目

## 前端实现

### 1. 上下文和 Hook

#### AuthContext（`src/contexts/AuthContext.tsx`）
提供认证状态管理：
```typescript
const { user, login, logout, isAuthenticated, hasRole } = useAuth();
```

### 2. 权限组件

#### HasPermission（`src/components/HasPermission.tsx`）
条件渲染包装器：
```jsx
<HasPermission roles={['ROLE_PMC']}>
  <Button>PMC 审批按钮</Button>
</HasPermission>
```

#### ProtectedRoute（`src/components/ProtectedRoute.tsx`）
路由级权限保护：
```jsx
<ProtectedRoute roles={['ROLE_PMC']}>
  <PmcApproval />
</ProtectedRoute>
```

### 3. UI 增强

- 使用 `sonner` 库显示 Toast 提示
- 权限不足时显示友好的错误提示
- 动态隐藏/显示操作按钮

## 工作流程

### 登录流程
1. 用户输入用户名和密码
2. 调用 `/api/auth/login` 接口
3. 后端验证凭证，返回 JWT token
4. 前端存储 token 到 `localStorage`
5. 后续请求在 Authorization header 中携带 token

### 请求验证流程
1. `AuthTokenFilter` 从请求 header 提取 JWT token
2. `JwtUtils` 验证 token 有效性
3. 从 token 中提取 username
4. `UserService.loadUserByUsername()` 加载用户和权限
5. Spring Security 设置认证上下文

### 权限检查流程
1. 前端：`hasRole()` 检查用户角色
2. 前端：`ProtectedRoute` 检查路由访问权限
3. 前端：`HasPermission` 条件渲染组件
4. 后端：`@PreAuthorize` 检查方法级权限
5. 后端：`ProjectService` 实现数据级隔离

## 部署步骤

### 第一步：应用数据库脚本

```bash
# 1. 建立新表
mysql -h localhost -u root -p kbd_pm_system < db/rbac_ddl.sql

# 2. 初始化数据
mysql -h localhost -u root -p kbd_pm_system < db/rbac_init_data.sql
```

### 第二步：环境配置

配置以下环境变量（或修改 `application.yml`）：

```bash
# JWT 配置
export JWT_SECRET="your-secret-key-at-least-32-characters"
export JWT_EXPIRATION=86400000  # 24 小时

# 数据库配置
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=kbd_pm_system
export DB_USER=root
export DB_PASSWORD=your_password
```

### 第三步：编译后端

```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

### 第四步：编译前端

```bash
cd frontend
npm install
npm run dev
```

## 测试

### 后端测试

```bash
# 1. 测试登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"pmc_user","password":"test123"}'

# 响应示例：
# {"token":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."}

# 2. 使用 token 访问受保护的资源
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/projects
```

### 前端测试

1. 打开 http://localhost:5173
2. 登录为 `pmc_user` / `test123`
3. 验证 PMC 审批按钮显示
4. 登出，再以 `dept_head` / `test123` 登录
5. 验证 PMC 审批按钮隐藏

## 默认测试账户

| 用户名 | 密码 | 角色 |
|--------|------|------|
| pmc_user | test123 | ROLE_PMC |
| pm_user | test123 | ROLE_PM |
| dept_head | test123 | ROLE_DEPT_HEAD |
| efficiency_user | test123 | ROLE_EFFICIENCY |
| compliance_user | test123 | ROLE_COMPLIANCE |
| admin_user | test123 | ROLE_ADMIN |

## 安全建议

1. **生产环境**
   - 修改所有初始密码
   - 设置强 JWT_SECRET（至少 32 个字符）
   - 使用 HTTPS
   - 配置 CORS 白名单

2. **权限管理**
   - 定期审查用户权限
   - 删除不活跃用户
   - 使用角色而不是直接分配权限

3. **日志和监控**
   - 启用审计日志
   - 监控失败的登录尝试
   - 记录权限变更

## 故障排查

### JWT 过期
若前端报 401 错误，说明 token 过期：
- 调用登录接口获取新 token
- 检查 JWT_EXPIRATION 配置

### 权限拒绝
若前端报 403 错误，说明权限不足：
- 检查用户角色分配
- 查看 `role_permissions` 表
- 验证 @PreAuthorize 注解配置

### CORS 错误
若前端无法调用后端 API：
- 检查 SecurityConfig 中的 CORS 配置
- 添加前端域名到允许列表

## 参考文档

- Spring Security 官方文档：https://spring.io/projects/spring-security
- JWT 令牌标准：https://jwt.io
- MySQL 文档：https://dev.mysql.com/doc