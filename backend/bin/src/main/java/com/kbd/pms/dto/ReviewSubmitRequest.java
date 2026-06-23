package com.kbd.pms.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 提交评审请求
 */
public record ReviewSubmitRequest(
    @NotNull Long actorUserId,
    String submitComment
) {}
