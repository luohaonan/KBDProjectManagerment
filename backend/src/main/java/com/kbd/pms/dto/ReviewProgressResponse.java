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
      String nodeCode,               // 流程图节点编码
      String nodeType,               // 流程图节点类型
      String status,                 // PENDING / IN_PROGRESS / APPROVED / REJECTED
      LocalDateTime completedAt,
      String approverRule,           // 审批规则
      String approverRuleLabel,      // 审批规则展示名称
      String expectedApproverLabel,  // 未来节点的预期审批人/角色文案
      boolean active,                // 是否当前激活节点
      boolean future,                // 是否未来节点
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