package com.kbd.pms.dto;

/**
 * 提交立项申请请求
 */
public record InitiationSubmitRequest(
    Long actorUserId,
    String applicationContent
) {}
