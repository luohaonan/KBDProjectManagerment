package com.kbd.pms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 交付物上传请求
 */
public record DeliverableUploadRequest(
    @NotNull Long actorUserId,
    @NotNull Long milestonePhase,    // 里程碑阶段
    @NotBlank String deliverableSlotCode,  // 交付物槽位代码 (如 "LEAD_COMPOUND_REPORT")
    String fileName,
    String storagePath
) {}