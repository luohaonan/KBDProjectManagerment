package com.kbd.pms.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 保存草稿请求
 */
public record SaveDraftRequest(
    @NotNull Long actorUserId,
    String submitComment
) {}
