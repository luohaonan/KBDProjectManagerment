package com.kbd.pms.repository;

import com.kbd.pms.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

  /** 获取用户未读通知（最近5条） */
  List<NotificationEntity> findTop5ByRecipientUserIdAndIsReadFalseOrderByCreatedAtDesc(Long recipientUserId);

  /** 获取用户待办通知（最新优先） */
  List<NotificationEntity> findByRecipientUserIdAndIsTodoTrueAndIsDoneFalseOrderByCreatedAtDesc(Long recipientUserId);

  /** 获取用户所有通知（分页，未读优先，按创建时间倒序） */
  @Query("SELECT n FROM NotificationEntity n WHERE n.recipientUserId = :recipientUserId ORDER BY n.isRead ASC, n.createdAt DESC")
  Page<NotificationEntity> findByRecipientUserIdOrderByIsReadAscCreatedAtDesc(Long recipientUserId, Pageable pageable);

  /** 获取用户未读通知数量 */
  long countByRecipientUserIdAndIsReadFalse(Long recipientUserId);

  /** 标记单条通知为已读 */
  @Modifying
  @Query("UPDATE NotificationEntity n SET n.isRead = true WHERE n.id = :notificationId AND n.recipientUserId = :userId")
  int markAsRead(Long notificationId, Long userId);

  /** 标记用户所有通知为已读 */
  @Modifying
  @Query("UPDATE NotificationEntity n SET n.isRead = true WHERE n.recipientUserId = :userId")
  int markAllAsRead(Long userId);

  /** 按项目/里程碑关闭待办 */
  @Modifying
  @Query("UPDATE NotificationEntity n SET n.isDone = true WHERE n.projectId = :projectId AND n.milestoneCode = :milestoneCode AND n.type = :type AND n.isTodo = true AND n.isDone = false")
  int markTodoDoneByProjectAndMilestoneAndType(Long projectId, String milestoneCode, String type);
}