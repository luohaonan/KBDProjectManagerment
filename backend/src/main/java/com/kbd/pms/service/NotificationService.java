package com.kbd.pms.service;

import com.kbd.pms.dto.NotificationDto;
import com.kbd.pms.entity.IamUserEntity;
import com.kbd.pms.entity.NotificationEntity;
import com.kbd.pms.repository.IamUserRepository;
import com.kbd.pms.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

  private final NotificationRepository notificationRepository;
  private final IamUserRepository iamUserRepository;
  private final SecurityHelper securityHelper;
  private final EmailService emailService;

  public NotificationService(
      NotificationRepository notificationRepository,
      IamUserRepository iamUserRepository,
      SecurityHelper securityHelper,
      EmailService emailService) {
    this.notificationRepository = notificationRepository;
    this.iamUserRepository = iamUserRepository;
    this.securityHelper = securityHelper;
    this.emailService = emailService;
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
   */
  @Transactional
  public void sendNotificationToDeptExecutors(Long deptId, String type, String title,
                                              String content, Long projectId,
                                              String milestoneCode, Long excludeUserId,
                                              boolean isTodo) {
    List<IamUserEntity> users = iamUserRepository.findByDeptIdAndIsActiveTrue(deptId);
    for (IamUserEntity user : users) {
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
   * 获取当前用户待办通知
   */
  @Transactional(readOnly = true)
  public List<NotificationDto> getPendingTodos() {
    Long userId = securityHelper.getCurrentUserId();
    return notificationRepository
        .findByRecipientUserIdAndIsTodoTrueAndIsDoneFalseOrderByCreatedAtDesc(userId)
        .stream()
        .map(this::toDto)
        .collect(Collectors.toList());
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
