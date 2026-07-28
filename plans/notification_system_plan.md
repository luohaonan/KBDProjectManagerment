# 通知系统设计与实施计划

## 一、需求背景

### 当前问题
1. **项目经理完善项目信息后**：系统没有通知资讯部执行人上传交付物
2. **执行人上传交付物后**：同部门的其他执行人没有收到"XX项目XX阶段的交付物已由XX用户上传"的通知
3. **每个阶段的流程流转**：G0-G9每个里程碑都需要通知对应部门的执行人

### 目标
构建一个完整的通知系统，在关键业务节点自动向相关用户发送通知，用户可以通过顶部导航栏的铃铛图标查看未读通知。

---

## 二、后端设计

### 2.1 数据库表

```sql
CREATE TABLE notification (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  recipient_user_id BIGINT NOT NULL COMMENT '接收人ID（对应 iam_user.id）',
  type VARCHAR(32) NOT NULL COMMENT '通知类型: PROJECT_CREATED / DELIVERABLE_UPLOADED / REVIEW_SUBMITTED / REVIEW_DECIDED / MILESTONE_APPROVED / PROJECT_COMPLETION',
  title VARCHAR(256) NOT NULL COMMENT '通知标题',
  content TEXT COMMENT '通知内容',
  project_id BIGINT COMMENT '关联项目ID',
  milestone_code VARCHAR(8) COMMENT '关联里程碑代码',
  related_user_id BIGINT COMMENT '关联操作人ID',
  is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  INDEX idx_recipient_read (recipient_user_id, is_read),
  INDEX idx_recipient_created (recipient_user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';
```

### 2.2 后端组件

#### 新增文件清单

| 文件 | 说明 |
|---|---|
| `NotificationEntity.java` | 通知实体类 |
| `NotificationRepository.java` | 通知数据访问层 |
| `NotificationService.java` | 通知业务逻辑 |
| `NotificationController.java` | 通知API |
| `NotificationDto.java` | 通知响应DTO |

#### 通知服务 NotificationService

```java
@Service
public class NotificationService {

    // 发送通知（单个用户）
    public void sendNotification(Long recipientUserId, String type, String title, 
                                 String content, Long projectId, String milestoneCode);
    
    // 发送通知（批量 - 同一部门的多个执行人）
    public void sendNotificationToDeptExecutors(Long deptId, String type, String title,
                                                 String content, Long projectId, String milestoneCode,
                                                 Long excludeUserId);
    
    // 获取用户未读通知列表
    public List<NotificationDto> getUnreadNotifications(Long userId);
    
    // 获取用户所有通知（分页）
    public Page<NotificationDto> getUserNotifications(Long userId, Pageable pageable);
    
    // 标记通知为已读
    public void markAsRead(Long notificationId, Long userId);
    
    // 标记全部已读
    public void markAllAsRead(Long userId);
    
    // 获取未读通知数量
    public long countUnread(Long userId);
}
```

### 2.3 通知触发点（需插入通知代码的位置）

#### 触发器1：项目经理完善项目信息 → 通知部门执行人上传交付物

**位置**：`ProjectService.updateProject()` 方法中，当项目状态从 DRAFT 转为 ACTIVE 时

```java
// 在项目状态变为 ACTIVE 后添加：
// 1. 查询当前里程碑（G0）流程引擎配置中的上传部门
// 2. 向这些部门的所有执行人发送通知
// 通知内容："项目 [项目名称] ([项目编号]) 已完成初始化，请上传 G0 阶段核心交付物"
```

#### 触发器2：部门执行人上传交付物 → 通知同部门其他执行人

**位置**：`ReviewService.uploadDeliverable()` 方法中，文档保存后

```java
// 在文档保存成功后添加：
// 1. 获取上传者的部门ID
// 2. 查询同部门其他执行人
// 3. 发送通知："XX项目XX阶段的[交付物名称]已由[上传者]上传"
```

#### 触发器3：评审提交 → 通知审批人

**位置**：`ReviewService.initiateReview()` 方法中，审批任务创建后

```java
// 在 createMultiStepTasks 后添加：
// 1. 获取第一步审批的所有审批人
// 2. 发送通知："XX项目XX阶段评审已提交，请审批"
```

#### 触发器4：审批决策 → 通知下一节点审批人/执行人

**位置**：各审批handle方法中（如 `handleDeptHeadApprove`）

#### 其他可能触发器：
- 里程碑评审通过 → 通知下一阶段执行人
- 评审退回 → 通知执行人重新上传
- 项目创建 → 通知项目经理完善信息（现有 toast 改为系统通知）

---

## 三、前端设计

### 3.1 新增/修改文件

| 文件 | 说明 |
|---|---|
| `components/NotificationBell.tsx` | 顶部导航栏通知铃铛组件（新增） |
| `pages/NotificationCenter.tsx` | 通知中心页面（新增） |
| `App.tsx` | 添加通知铃铛到导航栏 |
| `lib/api.ts` | 添加通知相关 API 方法 |

### 3.2 通知铃铛组件 NotificationBell

```tsx
// 功能：
// - 显示铃铛图标 + 未读数量角标
// - 点击展开通知下拉面板（显示最近5条未读通知）
// - 点击"查看全部"跳转到通知中心页面
// - 点击单条通知跳转到对应项目
// - 每30秒轮询后端获取未读数量
```

### 3.3 通知中心页面 NotificationCenter

```tsx
// 功能：
// - 分页显示所有通知（未读优先）
// - 支持标记单条已读 / 全部已读
// - 通知类型用不同图标标识
// - 点击通知跳转到相关页面
```

### 3.4 App.tsx 修改

当前 `App.tsx` 中需要找到顶部导航栏的位置，添加 `NotificationBell` 组件。

---

## 四、实施步骤

### 第一阶段：后端基础（约3-4小时）

| 步骤 | 内容 | 工作量 |
|---|---|---|
| 1 | 创建 `notification` 表（运行SQL迁移脚本） | 0.5h |
| 2 | 创建 `NotificationEntity`、`NotificationRepository` | 0.5h |
| 3 | 创建 `NotificationService`（发送、查询、标记已读） | 1h |
| 4 | 创建 `NotificationController`（API端点） | 0.5h |
| 5 | 集成触发器到 `ProjectService`（项目完善后通知） | 0.5h |
| 6 | 集成触发器到 `ReviewService`（上传、提交、审批通知） | 1h |

### 第二阶段：前端基础（约2-3小时）

| 步骤 | 内容 | 工作量 |
|---|---|---|
| 7 | 创建 `NotificationBell` 组件 | 1h |
| 8 | 创建 `NotificationCenter` 页面 | 1h |
| 9 | 集成到 `App.tsx` 导航栏 | 0.5h |
| 10 | 添加30秒轮询 | 0.5h |

### 第三阶段：测试与联调（约1-2小时）

| 步骤 | 内容 | 工作量 |
|---|---|---|
| 11 | 端到端测试通知流程 | 1h |
| 12 | 修复问题 | 1h |

### 总计工作量：约 6-9 小时

---

## 五、API 接口设计

```
GET  /api/notifications/unread          → 获取未读通知列表
GET  /api/notifications?page=0&size=20  → 分页获取所有通知
GET  /api/notifications/count-unread    → 获取未读数量
PUT  /api/notifications/{id}/read       → 标记单条已读
PUT  /api/notifications/read-all        → 标记全部已读
```

---

## 六、通知类型与内容模板

| 类型 | 触发时机 | 标题 | 内容模板 |
|---|---|---|---|
| `PROJECT_COMPLETION` | PM完善项目信息后 | 项目已完善 | "[项目名称] 项目信息已完善，请上传 [里程碑] 阶段交付物" |
| `DELIVERABLE_UPLOADED` | 执行人上传后 | 交付物已上传 | "[项目名称][里程碑] 的 [交付物名称] 已由 [上传人] 上传" |
| `REVIEW_SUBMITTED` | 评审提交后 | 评审已提交 | "[项目名称][里程碑] 评审已提交，请您审批" |
| `REVIEW_DECIDED` | 审批决策后 | 评审结果 | "[项目名称][里程碑] 评审已 [通过/有条件通过/不通过]，意见：[审批意见]" |
| `MILESTONE_APPROVED` | 里程碑通过后 | 里程碑通过 | "[项目名称][里程碑] 已通过，请开始下一阶段工作" |
| `REVIEW_REJECTED` | 评审退回后 | 评审退回 | "[项目名称][里程碑] 评审退回，原因：[退回原因]，请重新上传" |

---

## 七、邮件提醒

### 7.1 技术可行性

完全可行。Spring Boot 提供 `spring-boot-starter-mail` 依赖，底层使用 Jakarta Mail（原 JavaMail），支持通过 SMTP 协议连接任意邮件服务器发送邮件。网易企业邮箱对外提供标准的 SMTP 服务。

### 7.2 网易企业邮箱配置

| 配置项 | 值 |
|---|---|
| SMTP 服务器 | `smtp.qiye.163.com` |
| 端口（SSL） | `465` |
| 端口（TLS） | `587` |
| 端口（明文） | `25` |
| 需要认证 | 是 |
| 账号 | `系统邮箱账号@企业域名`（如 `noreply@xxx.com`） |
| 密码/授权码 | 企业邮箱的登录密码或客户端专用授权码 |

### 7.3 配置方式：数据库存储 + 系统界面管理（而非硬编码）

发信邮箱配置**不硬编码在 application.yml 中**，而是存储在数据库表中，管理员可通过系统界面实时修改配置，无需重启服务。

#### 7.3.1 系统配置表

```sql
CREATE TABLE system_config (
  config_key VARCHAR(64) PRIMARY KEY COMMENT '配置键',
  config_value TEXT COMMENT '配置值',
  description VARCHAR(256) COMMENT '配置说明',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 邮件配置初始数据
INSERT INTO system_config (config_key, config_value, description) VALUES
('mail.smtp.host', 'smtp.qiye.163.com', 'SMTP 服务器地址'),
('mail.smtp.port', '465', 'SMTP 端口'),
('mail.smtp.username', '', '发信邮箱账号'),
('mail.smtp.password', '', '发信邮箱密码/授权码'),
('mail.smtp.ssl', 'true', '是否启用 SSL'),
('mail.from.address', '', '发信人地址（通常与 username 相同）'),
('mail.enabled', 'true', '是否启用邮件发送');
```

#### 7.3.2 SystemConfig 后端组件

| 文件 | 说明 |
|---|---|
| `SystemConfigEntity.java` | 配置实体 |
| `SystemConfigRepository.java` | 配置数据访问层 |
| `SystemConfigService.java` | 读取/更新配置 |
| `SystemConfigController.java` | 配置管理 API（仅 ADMIN 角色可访问） |

```java
@RestController
@RequestMapping("/api/system/config")
@PreAuthorize("hasRole('ADMIN')")  // 仅管理员可操作
public class SystemConfigController {
    
    // PUT /api/system/config/mail  → 更新邮件配置（批量）
    // GET  /api/system/config/mail  → 获取邮件配置（值脱敏，密码回显****）
    // POST /api/system/config/mail/test → 发送测试邮件验证配置是否正确
}
```

#### 7.3.3 EmailService 从数据库读取配置

```java
@Service
public class EmailService {

    private final SystemConfigService configService;
    
    @Async
    public void sendNotificationEmail(String to, String subject, String content) {
        // 1. 检查是否启用了邮件发送
        if (!"true".equals(configService.getConfig("mail.enabled"))) {
            return;  // 邮件发送已关闭
        }
        
        // 2. 从数据库读取配置，动态创建 JavaMailSender
        String host = configService.getConfig("mail.smtp.host");
        String port = configService.getConfig("mail.smtp.port");
        String username = configService.getConfig("mail.smtp.username");
        String password = configService.getConfig("mail.smtp.password");
        String ssl = configService.getConfig("mail.smtp.ssl");
        String from = configService.getConfig("mail.from.address");
        
        if (host == null || username == null || password == null) {
            log.warn("邮件配置不完整，跳过邮件发送");
            return;
        }
        
        try {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(host);
            mailSender.setPort(Integer.parseInt(port));
            mailSender.setUsername(username);
            mailSender.setPassword(password);
            
            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.ssl.enable", ssl);
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "10000");
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from != null ? from : username);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("发送邮件失败: to={}, subject={}", to, subject, e);
        }
    }
}
```

### 7.4 前端：邮件配置管理页面

#### 7.4.1 页面位置

在 `AccountManagement.tsx`（账号管理页面）中新增"邮件配置"选项卡，或新建独立的 `EmailConfig.tsx` 页面。

#### 7.4.2 页面功能

| 功能 | 说明 |
|---|---|
| 配置表单 | SMTP 服务器、端口、账号、密码、发信人地址 |
| 测试连接 | 点击"发送测试邮件"输入框输入测试邮箱地址，发送测试邮件验证配置 |
| 密码脱敏 | 获取配置时密码字段回显为 `****`，修改时留空表示不修改密码 |
| 启用/关闭 | 邮件发送总开关，紧急情况下可一键关闭 |

#### 7.4.3 页面位置

建议放在"系统管理"导航菜单下，新增一个"邮件配置"页面入口，仅 `ROLE_ADMIN` 角色可见。

### 7.5 邮件内容模板

| 通知类型 | 邮件主题 | 邮件正文（HTML） |
|---|---|---|
| `PROJECT_COMPLETION` | [PMS通知] 项目「{项目名称}」已完善，请上传交付物 | 项目 **{项目名称}**（{项目编号}）已完成初始化，**{里程碑}** 阶段已开放交付物上传，请登录系统上传核心交付物。 |
| `DELIVERABLE_UPLOADED` | [PMS通知] {项目名称} 交付物已由 {上传人} 上传 | 项目 **{项目名称}** 的 **{里程碑}** 阶段交付物「{交付物名称}」已由 **{上传人}** 上传。 |
| `REVIEW_SUBMITTED` | [PMS通知] {项目名称} 评审已提交，请审批 | 项目 **{项目名称}** 的 **{里程碑}** 阶段评审已提交，请您登录系统进行审批。 |
| `REVIEW_DECIDED` | [PMS通知] {项目名称} 评审结果通知 | 项目 **{项目名称}** 的 **{里程碑}** 阶段评审结果：**{通过/有条件通过/不通过}**，意见：{审批意见} |
| `MILESTONE_APPROVED` | [PMS通知] {项目名称} {里程碑} 已通过 | 项目 **{项目名称}** 的 **{里程碑}** 阶段已评审通过，请开始下一阶段工作。 |

### 7.6 集成方式

在 `NotificationService.sendNotification()` 方法发送系统通知后，同时调用 `EmailService.sendNotificationEmail()` 发送邮件：

```java
@Service
public class NotificationService {
    
    private final EmailService emailService;
    private final IamUserRepository iamUserRepository;
    
    public void sendNotification(Long recipientUserId, String type, String title, 
                                  String content, Long projectId, String milestoneCode) {
        // 1. 写入数据库通知记录
        NotificationEntity entity = saveToDb(...);
        
        // 2. 异步发送邮件
        //    查询收件人的邮箱地址
        iamUserRepository.findById(recipientUserId).ifPresent(user -> {
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                String emailSubject = buildEmailSubject(type, ...);
                String emailContent = buildEmailContent(type, ...);
                emailService.sendNotificationEmail(user.getEmail(), emailSubject, emailContent);
            }
        });
    }
}
```

### 7.7 邮件模板渲染

邮件正文建议使用 HTML 模板引擎（如 Thymeleaf 或简单的字符串模板）：

```xml
<!-- resources/templates/email/notification.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
    <div style="max-width:600px;margin:0 auto;font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto;">
        <div style="background:#1e293b;padding:20px;text-align:center;">
            <h2 style="color:#fff;margin:0;">PMS 项目管理系统</h2>
        </div>
        <div style="padding:20px;background:#f8fafc;">
            <p th:utext="${content}"></p>
            <p style="color:#64748b;font-size:12px;margin-top:20px;">
                请登录系统查看详情：<a th:href="${loginUrl}" th:text="${loginUrl}"></a>
            </p>
        </div>
        <div style="background:#1e293b;padding:10px;text-align:center;">
            <p style="color:#94a3b8;font-size:12px;margin:0;">
                此邮件由 PMS 系统自动发送，请勿回复
            </p>
        </div>
    </div>
</body>
</html>
```

### 7.8 注意事项

1. **授权码**：网易企业邮箱建议使用"客户端授权码"而非登录密码，需要在邮箱设置中生成
2. **异步发送**：邮件发送使用 `@Async` 异步执行，避免阻塞主业务逻辑
3. **发送限制**：网易企业邮箱通常有每日发送上限（约500封/天），建议监控发送量
4. **错误处理**：发送失败不应影响主业务流程，失败日志记录即可
5. **HTML兼容性**：邮件 HTML 需兼容 Outlook /网易邮箱客户端，避免使用复杂 CSS
6. **退信处理**：如果邮件地址无效被退回，应记录日志但不阻塞流程
7. **配置变更实时生效**：配置存储在数据库，每次发送时读取最新值，修改后无需重启
8. **密码脱敏**：API 返回配置时密码字段显示 `****`，只有非 `****` 值才视为新密码
9. **测试功能**：提供"发送测试邮件"功能，管理员可验证配置是否正确

---

## 八、注意事项

1. **批量通知**：通知部门执行人时，需要从 `iam_user` 表中根据 `dept_id` 和角色查询用户列表
2. **排除操作人**：上传通知中应排除上传者本人（不需要通知自己）
3. **通知去重**：同一事件不需要重复发送
4. **性能考虑**：通知写入使用异步方式（`@Async` 或消息队列），避免阻塞主业务逻辑
5. **邮件异步**：邮件发送使用独立的线程池，不影响系统通知的写入速度
6. **历史清理**：可考虑定期清理3个月前的已读通知
7. **邮箱有效性**：使用 `iam_user.email` 字段作为收件地址，用户管理时需确保邮箱填写正确
