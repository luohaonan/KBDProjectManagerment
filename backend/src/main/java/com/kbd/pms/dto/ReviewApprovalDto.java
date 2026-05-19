package com.kbd.pms.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评审审批记录 DTO
 */
public record ReviewApprovalDto(
    long id,
    long projectId,
    long projectMilestoneId,
    Long submitterUserId,
    String submitterName,
    String submitComment,
    String status,
    LocalDateTime submittedAt,
    LocalDateTime finishedAt,
    List<ReviewApprovalTaskDto> tasks
) {}
