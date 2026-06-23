package com.kbd.pms.dto;

import java.time.LocalDateTime;

/**
 * 立项申请审批人任务 DTO
 */
public record InitiationApprovalTaskDto(
    Long id,
    Long initiationApprovalId,
    Long approverUserId,
    String approverName,
    String approverRole,
    Integer sortOrder,
    String status,
    String decision,
    String opinion,
    LocalDateTime decidedAt
) {}
