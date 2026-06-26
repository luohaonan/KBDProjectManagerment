package com.kbd.pms.dto;

import java.time.LocalDateTime;

/**
 * 项目终止申请DTO
 */
public record TerminationRequestDto(
    Long id,
    Long projectId,
    Long requestedBy,
    String requestedByName,
    String terminationReason,
    String attachmentUri,
    String status,
    Long efficiencyApproverId,
    String efficiencyApproverName,
    String efficiencyOpinion,
    LocalDateTime efficiencyDecidedAt,
    Long pmcApproverId,
    String pmcApproverName,
    String pmcOpinion,
    LocalDateTime pmcDecidedAt,
    String summaryReportUri,
    Boolean assetDisposalConfirmed,
    Boolean archiveConfirmed,
    LocalDateTime submittedAt,
    LocalDateTime finishedAt
) {}