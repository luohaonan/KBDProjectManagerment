package com.kbd.pms.dto;

/**
 * 立项审批决策请求
 */
public record InitiationDecisionRequest(
    Long actorUserId,
    String decision,  // APPROVED / REJECTED
    String opinion
) {}
