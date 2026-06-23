package com.kbd.pms.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 立项申请审批记录 DTO
 */
public record InitiationApprovalDto(
    Long id,
    Long projectId,
    Long submitterUserId,
    String submitterName,
    String applicationContent,
    String status,
    LocalDateTime submittedAt,
    LocalDateTime finishedAt,
    List<InitiationApprovalTaskDto> tasks
) {}
