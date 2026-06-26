package com.kbd.pms.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 里程碑评审进度响应
 * 展示当前里程碑的多步审批流程状态
 */
public record ReviewProgressResponse(
    Long projectId,
    Long projectMilestoneId,
    String milestoneCode,
    String milestoneName,
    String currentStep,              // 当前审批步骤代码
    String status,                   // 整体状态
    List<StepProgress> steps         // 各步骤进度
) {
  public record StepProgress(
      String stepCode,               // 步骤代码
      String stepName,               // 步骤名称
      String status,                 // PENDING / IN_PROGRESS / APPROVED / REJECTED
      LocalDateTime completedAt,
      List<TaskDetail> tasks         // 该步骤下的审批任务
  ) {}

  public record TaskDetail(
      Long taskId,
      Long approverUserId,
      String approverName,
      String approverRole,
      String deliverableSlotCode,
      String decision,
      String opinion,
      LocalDateTime decidedAt,
      String status
  ) {}
}