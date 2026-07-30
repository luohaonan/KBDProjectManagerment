package com.kbd.pms.dto;

import java.time.LocalDateTime;

/**
 * 统一待办 DTO：聚合通知待办与评审批办，供前端统一展示。
 */
public record UnifiedPendingTodoDto(
    String source,
    String todoType,
    String sourceDetail,
    Long notificationId,
    Long taskId,
    Long reviewApprovalId,
    Long projectId,
    String projectName,
    String projectCode,
    String milestoneName,
    String milestoneCode,
    String submitterName,
    LocalDateTime submittedAt,
    String approverRole,
    String debugNotificationType,
    String debugStepCode,
    String debugCurrentActiveStep,
    Long debugApproverUserId
) {}