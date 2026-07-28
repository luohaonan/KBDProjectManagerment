package com.kbd.pms.web;

import com.kbd.pms.dto.NotificationDto;
import com.kbd.pms.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  /** 获取未读通知列表（最近5条，供铃铛下拉面板） */
  @GetMapping("/unread")
  public Result<List<NotificationDto>> getUnreadNotifications() {
    return Result.ok(notificationService.getUnreadNotifications());
  }

  /** 获取当前用户待办通知列表 */
  @GetMapping("/pending-todos")
  public Result<List<NotificationDto>> getPendingTodos() {
    return Result.ok(notificationService.getPendingTodos());
  }

  /** 分页获取所有通知（未读优先） */
  @GetMapping
  public Result<Page<NotificationDto>> getUserNotifications(
      @PageableDefault(size = 20) Pageable pageable) {
    return Result.ok(notificationService.getUserNotifications(pageable));
  }

  /** 获取未读通知数量 */
  @GetMapping("/count-unread")
  public Result<Long> getUnreadCount() {
    return Result.ok(notificationService.countUnread());
  }

  /** 标记单条通知为已读 */
  @PutMapping("/{id}/read")
  public Result<Void> markAsRead(@PathVariable("id") Long notificationId) {
    notificationService.markAsRead(notificationId);
    return Result.ok(null);
  }

  /** 标记全部已读 */
  @PutMapping("/read-all")
  public Result<Void> markAllAsRead() {
    notificationService.markAllAsRead();
    return Result.ok(null);
  }
}