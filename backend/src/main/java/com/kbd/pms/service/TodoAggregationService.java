package com.kbd.pms.service;

import com.kbd.pms.dto.NotificationDto;
import com.kbd.pms.dto.PendingReviewTaskDto;
import com.kbd.pms.dto.UnifiedPendingTodoDto;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TodoAggregationService {

  private final NotificationService notificationService;
  private final ReviewService reviewService;

  public TodoAggregationService(NotificationService notificationService,
                                ReviewService reviewService) {
    this.notificationService = notificationService;
    this.reviewService = reviewService;
  }

  @Transactional
  public List<UnifiedPendingTodoDto> getPendingTodosForCurrentUser() {
    List<UnifiedPendingTodoDto> todos = new ArrayList<>();

    for (NotificationDto item : notificationService.getPendingTodos()) {
      todos.add(new UnifiedPendingTodoDto(
          "NOTIFICATION",
          normalizeNotificationTodoType(item.type()),
          "notification",
          item.id(),
          null,
          null,
          item.projectId(),
          extractProjectName(item.title(), item.content()),
          null,
          item.milestoneCode() != null ? item.milestoneCode() + "阶段" : null,
          item.milestoneCode(),
          item.relatedUserName(),
          item.createdAt() != null ? LocalDateTime.ofInstant(item.createdAt(), ZoneOffset.UTC) : null,
          null,
          item.type(),
          null,
          null,
          item.recipientUserId()
      ));
    }

    for (PendingReviewTaskDto task : reviewService.getPendingTasksForCurrentUser()) {
      todos.add(new UnifiedPendingTodoDto(
          "REVIEW",
          "INITIATION".equals(task.reviewType()) ? "INITIATION" : "MILESTONE",
          "INITIATION".equals(task.reviewType()) ? "initiation_approval_task" : "review_approval_task",
          null,
          task.taskId(),
          task.reviewApprovalId(),
          task.projectId(),
          task.projectName(),
          task.projectCode(),
          task.milestoneName(),
          task.milestoneCode(),
          task.submitterName(),
          task.submittedAt(),
          task.approverRole(),
          null,
          task.approverRole(),
          null,
          null
      ));
    }

    todos.sort(Comparator.comparing(UnifiedPendingTodoDto::submittedAt,
        Comparator.nullsLast(Comparator.reverseOrder())));
    return todos;
  }

  private String normalizeNotificationTodoType(String type) {
    if ("BUDGET_APPROVAL".equals(type)) {
      return "BUDGET";
    }
    if ("INITIATION".equals(type)) {
      return "INITIATION";
    }
    if ("PROJECT_COMPLETION".equals(type)) {
      return "PROJECT_COMPLETION";
    }
    return "DELIVERABLE";
  }

  private String extractProjectName(String title, String content) {
    String text = (title == null ? "" : title) + " " + (content == null ? "" : content);
    int start = text.indexOf('[');
    int end = text.indexOf(']');
    if (start >= 0 && end > start) {
      return text.substring(start + 1, end);
    }
    return "待处理项目";
  }
}