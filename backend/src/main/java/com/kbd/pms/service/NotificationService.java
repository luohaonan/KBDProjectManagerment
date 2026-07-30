package com.kbd.pms.service;

import com.kbd.pms.dto.NotificationDto;
import com.kbd.pms.entity.IamUserEntity;
import com.kbd.pms.entity.NotificationEntity;
import com.kbd.pms.entity.OrgDepartmentEntity;
import com.kbd.pms.entity.Role;
import com.kbd.pms.entity.User;
import com.kbd.pms.repository.IamUserRepository;
import com.kbd.pms.repository.NotificationRepository;
import com.kbd.pms.repository.OrgDepartmentRepository;
import com.kbd.pms.repository.UserRepository;
import com.kbd.pms.workflow.WfProcessDefinition;
import com.kbd.pms.workflow.WfProcessNode;
import com.kbd.pms.workflow.WfProcessRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NotificationService {

  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

  private final NotificationRepository notificationRepository;
  private final IamUserRepository iamUserRepository;
  private final SecurityHelper securityHelper;
  private final EmailService emailService;
  private final UserRepository userRepository;
  private final OrgDepartmentRepository orgDepartmentRepository;
  private final WfProcessRepository wfProcessRepository;

  public NotificationService(
      NotificationRepository notificationRepository,
      IamUserRepository iamUserRepository,
      SecurityHelper securityHelper,
      EmailService emailService,
      UserRepository userRepository,
      OrgDepartmentRepository orgDepartmentRepository,
      WfProcessRepository wfProcessRepository) {
    this.notificationRepository = notificationRepository;
    this.iamUserRepository = iamUserRepository;
    this.securityHelper = securityHelper;
    this.emailService = emailService;
    this.userRepository = userRepository;
    this.orgDepartmentRepository = orgDepartmentRepository;
    this.wfProcessRepository = wfProcessRepository;
  }

  // ==================== 发送通知 ====================

  /**
   * 发送通知给单个用户
   */
  @Transactional
  public void sendNotification(Long recipientUserId, String type, String title,
                               String content, Long projectId, String milestoneCode) {
    sendNotification(recipientUserId, type, title, content, projectId, milestoneCode, null);
  }

  /**
   * 发送通知给单个用户（含关联操作人ID）
   */
  @Transactional
  public void sendNotification(Long recipientUserId, String type, String title,
                               String content, Long projectId, String milestoneCode,
                               Long relatedUserId) {
    sendNotification(recipientUserId, type, title, content, projectId, milestoneCode, relatedUserId, false);
  }

  /**
   * 发送通知给单个用户（可指定是否同时作为待办）
   */
  @Transactional
  public void sendNotification(Long recipientUserId, String type, String title,
                               String content, Long projectId, String milestoneCode,
                               Long relatedUserId, boolean isTodo) {
    NotificationEntity entity = new NotificationEntity();
    entity.setRecipientUserId(recipientUserId);
    entity.setType(type);
    entity.setTitle(title);
    entity.setContent(content);
    entity.setProjectId(projectId);
    entity.setMilestoneCode(milestoneCode);
    entity.setRelatedUserId(relatedUserId);
    entity.setIsRead(false);
    entity.setIsTodo(isTodo);
    entity.setIsDone(false);
    entity.setCreatedAt(Instant.now());
    notificationRepository.save(entity);

    // 异步发送邮件通知
    trySendEmail(recipientUserId, type, title, content);
  }

  /**
   * 发送通知给同一部门的多个执行人（排除指定用户）
   */
  @Transactional
  public void sendNotificationToDeptExecutors(Long deptId, String type, String title,
                                              String content, Long projectId,
                                              String milestoneCode, Long excludeUserId) {
    sendNotificationToDeptExecutors(deptId, type, title, content, projectId, milestoneCode, excludeUserId, false);
  }

  /**
   * 发送通知给同一部门的多个执行人（可指定是否同时作为待办）
   * 排除该部门的负责人（部门负责人负责审批而非上传交付物），但不影响其他部门
   */
  @Transactional
  public void sendNotificationToDeptExecutors(Long deptId, String type, String title,
                                              String content, Long projectId,
                                              String milestoneCode, Long excludeUserId,
                                              boolean isTodo) {
    // 查找该部门的负责人ID，仅排除本部门负责人（而非全局ROLE_DEPT_HEAD）
    Long headUserId = orgDepartmentRepository.findById(deptId)
        .map(OrgDepartmentEntity::getHeadUserId)
        .orElse(null);

    List<User> users = userRepository.findByDepartmentId(deptId);
    for (User user : users) {
      if (!Boolean.TRUE.equals(user.getIsActive())) {
        continue;
      }
      boolean isDeptExecutor = user.getRoles() != null
          && user.getRoles().stream().map(Role::getName).anyMatch("ROLE_DEPT_EXECUTOR"::equals);
      if (!isDeptExecutor) {
        continue;
      }
      // 跳过部门负责人角色（负责审批，不负责上传交付物）
      boolean isDeptHead = user.getRoles() != null
          && user.getRoles().stream().map(Role::getName).anyMatch("ROLE_DEPT_HEAD"::equals);
      if (isDeptHead) {
        continue;
      }
      // 跳过 PM 角色（PM 负责审批，不负责上传交付物）
      boolean isPm = user.getRoles() != null
          && user.getRoles().stream().map(Role::getName).anyMatch("ROLE_PM"::equals);
      if (isPm) {
        continue;
      }
      // 跳过该部门的负责人（他们负责审批而非上传）
      if (headUserId != null && user.getId().equals(headUserId)) {
        continue;
      }
      if (excludeUserId != null && user.getId().equals(excludeUserId)) {
        continue;
      }
      sendNotification(user.getId(), type, title, content, projectId, milestoneCode, null, isTodo);
    }
  }

  /**
   * 批量发送通知给指定用户ID列表
   */
  @Transactional
  public void sendNotificationToUsers(List<Long> userIds, String type, String title,
                                      String content, Long projectId, String milestoneCode,
                                      Long relatedUserId) {
    for (Long userId : userIds) {
      sendNotification(userId, type, title, content, projectId, milestoneCode, relatedUserId);
    }
  }

  // ==================== 查询通知 ====================

  /**
   * 获取当前用户未读通知（最近5条，供铃铛下拉面板使用）
   */
  @Transactional(readOnly = true)
  public List<NotificationDto> getUnreadNotifications() {
    Long userId = securityHelper.getCurrentUserId();
    return notificationRepository
        .findTop5ByRecipientUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
        .stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  /**
   * 获取当前用户所有通知（分页，未读优先）
   */
  @Transactional(readOnly = true)
  public Page<NotificationDto> getUserNotifications(Pageable pageable) {
    Long userId = securityHelper.getCurrentUserId();
    return notificationRepository
        .findByRecipientUserIdOrderByIsReadAscCreatedAtDesc(userId, pageable)
        .map(this::toDto);
  }

  /**
   * 获取当前用户未读通知数量
   */
  @Transactional(readOnly = true)
  public long countUnread() {
    Long userId = securityHelper.getCurrentUserId();
    return notificationRepository.countByRecipientUserIdAndIsReadFalse(userId);
  }

  /**
   * 获取当前用户待办通知（同时自动清理不应存在的错误待办）
   */
  @Transactional
  public List<NotificationDto> getPendingTodos() {
    Long userId = securityHelper.getCurrentUserId();
    User currentUser = userRepository.findById(userId).orElse(null);
    List<String> roles = currentUser == null || currentUser.getRoles() == null
        ? List.of()
        : currentUser.getRoles().stream().map(Role::getName).toList();
    Set<Long> userDeptIds = currentUser == null || currentUser.getDepartments() == null
        ? Set.of()
        : currentUser.getDepartments().stream()
            .map(OrgDepartmentEntity::getId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
    boolean isDepartmentHead = roles.contains("ROLE_DEPT_HEAD") || isDepartmentHeadByAssignment(userId);

    List<NotificationEntity> allPending = notificationRepository
        .findByRecipientUserIdAndIsTodoTrueAndIsDoneFalseOrderByCreatedAtDesc(userId);

    List<NotificationDto> visible = new java.util.ArrayList<>();
    for (NotificationEntity entity : allPending) {
      if (isTodoVisibleForRoles(entity, roles, userDeptIds, isDepartmentHead)) {
        visible.add(toDto(entity));
      } else {
        // 自动清理不应存在的待办（角色变更或规则更新后自动消除）
        entity.setIsDone(true);
        notificationRepository.save(entity);
        log.info("自动清理无效待办: id={}, type={}, recipientUserId={}, roles={}, deptIds={}, isDepartmentHead={}",
            entity.getId(), entity.getType(), entity.getRecipientUserId(), roles, userDeptIds, isDepartmentHead);
      }
    }
    return visible;
  }

  // ==================== 标记已读 ====================

  /**
   * 标记单条通知为已读
   */
  @Transactional
  public void markAsRead(Long notificationId) {
    Long userId = securityHelper.getCurrentUserId();
    notificationRepository.markAsRead(notificationId, userId);
  }

  /**
   * 标记全部已读
   */
  @Transactional
  public void markAllAsRead() {
    Long userId = securityHelper.getCurrentUserId();
    notificationRepository.markAllAsRead(userId);
  }

  /**
   * 完成某类待办（按项目+里程碑统一关闭）
   */
  @Transactional
  public void completeTodoByProjectAndMilestone(Long projectId, String milestoneCode, String type) {
    notificationRepository.markTodoDoneByProjectAndMilestoneAndType(projectId, milestoneCode, type);
  }

  // ==================== 辅助方法 ====================

  private NotificationDto toDto(NotificationEntity entity) {
    String relatedUserName = null;
    if (entity.getRelatedUserId() != null) {
      relatedUserName = iamUserRepository.findById(entity.getRelatedUserId())
          .map(IamUserEntity::getDisplayName)
          .orElse(null);
    }
    return new NotificationDto(
        entity.getId(),
        entity.getRecipientUserId(),
        entity.getType(),
        entity.getTitle(),
        entity.getContent(),
        entity.getProjectId(),
        entity.getMilestoneCode(),
        entity.getRelatedUserId(),
        relatedUserName,
        entity.getIsRead(),
        entity.getIsTodo(),
        entity.getIsDone(),
        entity.getCreatedAt()
    );
  }

  private boolean isTodoVisibleForRoles(NotificationEntity entity, List<String> roles,
                                        Set<Long> userDeptIds, boolean isDepartmentHead) {
    if (entity == null || entity.getType() == null) {
      return true;
    }
    String normalizedType = entity.getType().trim().toUpperCase(Locale.ROOT);
    boolean isPm = roles.contains("ROLE_PM");
    boolean isDeptExecutor = roles.contains("ROLE_DEPT_EXECUTOR");
    return switch (normalizedType) {
      // 完善项目信息是 PM 的职责，部门负责人不应看到此待办
      case "PROJECT_COMPLETION" -> isPm && !isDepartmentHead;
      // 上传交付物的可见性需要与流程管理页中配置的上传部门一致
      case "DELIVERABLE" -> isDeliverableTodoVisible(entity, isDeptExecutor, isPm, isDepartmentHead, userDeptIds);
      // REVIEW_APPROVAL 属于评审批办体系，不应再作为通知待办返回；由 ReviewService 统一提供
      case "REVIEW_APPROVAL" -> false;
      default -> true;
    };
  }

  private boolean isDeliverableTodoVisible(NotificationEntity entity, boolean isDeptExecutor,
                                           boolean isPm, boolean isDepartmentHead,
                                           Set<Long> userDeptIds) {
    if (!isDeptExecutor || isPm || isDepartmentHead) {
      return false;
    }
    if (entity.getMilestoneCode() == null || entity.getProjectId() == null) {
      return true;
    }
    Set<Long> uploaderDeptIds = resolveUploaderDeptIds(entity.getMilestoneCode());
    if (uploaderDeptIds.isEmpty()) {
      log.warn("DELIVERABLE待办未匹配到流程上传部门，暂保留待办: notificationId={}, projectId={}, milestoneCode={}",
          entity.getId(), entity.getProjectId(), entity.getMilestoneCode());
      return true;
    }
    return userDeptIds.stream().anyMatch(uploaderDeptIds::contains);
  }

  private Set<Long> resolveUploaderDeptIds(String milestoneCode) {
    if (milestoneCode == null || milestoneCode.isBlank()) {
      return Set.of();
    }
    WfProcessDefinition def = wfProcessRepository
        .findByProcessTypeAndMilestoneCodeAndIsActiveTrue("MILESTONE", milestoneCode)
        .orElse(null);
    if (def == null || def.getNodes() == null) {
      return Set.of();
    }
    Set<Long> deptIds = new HashSet<>();
    for (WfProcessNode node : def.getNodes()) {
      if (!Boolean.TRUE.equals(node.getIsUploader()) || node.getApproverValue() == null) {
        continue;
      }
      for (String deptIdStr : node.getApproverValue().split(",")) {
        try {
          deptIds.add(Long.parseLong(deptIdStr.trim()));
        } catch (NumberFormatException e) {
          log.warn("流程上传节点存在无效部门ID: milestoneCode={}, nodeCode={}, approverValue={}",
              milestoneCode, node.getNodeCode(), node.getApproverValue());
        }
      }
    }
    return deptIds;
  }

  private boolean isDepartmentHeadByAssignment(Long userId) {
    return userId != null && !orgDepartmentRepository.findByHeadUserId(userId).isEmpty();
  }

  /**
   * 尝试异步发送邮件通知
   */
  private void trySendEmail(Long recipientUserId, String type, String title, String content) {
    try {
      iamUserRepository.findById(recipientUserId).ifPresent(user -> {
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
          String subject = "[PMS通知] " + title;
          String htmlContent = content != null ? content.replace("\n", "<br/>") : "";
          emailService.sendNotificationEmail(user.getEmail(), subject, htmlContent);
        }
      });
    } catch (Exception e) {
      log.warn("邮件发送失败: recipientUserId={}, title={}", recipientUserId, title, e);
    }
  }
}
