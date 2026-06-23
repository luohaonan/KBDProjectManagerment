package com.kbd.pms.dto;

import java.time.LocalDateTime;

/**
 * 评审审批人任务 DTO
 */
public record ReviewApprovalTaskDto(
    long id,
    long reviewApprovalId,
    long approverUserId,
    String approverName,
    String approverRole,
    int sortOrder,
    String status,
    String decision,
    String opinion,
    LocalDateTime decidedAt
) {}
