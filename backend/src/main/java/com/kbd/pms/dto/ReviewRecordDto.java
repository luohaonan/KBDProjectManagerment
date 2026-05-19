package com.kbd.pms.dto;

import java.time.LocalDateTime;

/**
 * 评审记录 DTO - 用于展示评审历史
 */
public record ReviewRecordDto(
    long id,
    long projectId,
    long projectMilestoneId,
    String action,
    Long actorUserId,
    String actorName,
    String actorRole,
    String result,
    String opinion,
    LocalDateTime actionAt
) {}
