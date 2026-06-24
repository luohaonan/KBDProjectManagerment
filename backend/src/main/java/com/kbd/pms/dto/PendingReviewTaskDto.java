package com.kbd.pms.dto;

import java.time.LocalDateTime;

/**
 * 统一待办评审任务 DTO - 用于评审中心展示
 * 同时支持里程碑评审和立项审批两种类型
 */
public record PendingReviewTaskDto(
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
    String reviewType
) {}