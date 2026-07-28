package com.kbd.pms.dto;

import java.time.Instant;

/**
 * 通知响应 DTO
 */
public record NotificationDto(
    Long id,
    Long recipientUserId,
    String type,
    String title,
    String content,
    Long projectId,
    String milestoneCode,
    Long relatedUserId,
    String relatedUserName,
    Boolean isRead,
    Boolean isTodo,
    Boolean isDone,
    Instant createdAt
) {}