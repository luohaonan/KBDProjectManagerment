package com.kbd.pms.dto;

import java.time.LocalDateTime;

/**
 * 项目变更申请DTO（用于提交和响应）
 */
public record ProjectChangeRequestDto(
    Long id,
    Long projectId,
    String projectName,
    String changeType,
    String reasonText,
    String attachmentUri,
    String beforeText,
    String afterText,
    String impactMilestoneText,
    String impactBudgetText,
    String impactResourceText,
    Long requestedBy,
    String requestedByName,
    LocalDateTime requestedAt,
    String status,
    Long efficiencyApproverId,
    String efficiencyApproverName,
    String efficiencyOpinion,
    LocalDateTime efficiencyDecidedAt,
    String pmcDecision,
    String pmcDecisionText,
    LocalDateTime pmcDecidedAt,
    Long pmcDecidedBy,
    String pmcDecidedByName
) {}