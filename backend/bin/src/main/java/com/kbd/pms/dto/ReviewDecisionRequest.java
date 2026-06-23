package com.kbd.pms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 评审决策请求
 */
public record ReviewDecisionRequest(
    @NotNull Long actorUserId,
    @NotBlank String decision,
    String opinion
) {}
