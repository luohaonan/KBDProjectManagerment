package com.kbd.pms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 里程碑交付物上传请求
 */
public record DeliverableUploadRequest(
    @NotNull Long actorUserId,
    @NotNull Long projectId,
    String milestoneCode,
    @NotBlank String deliverableSlotCode,
    String fileName,
    String storagePath
) {}