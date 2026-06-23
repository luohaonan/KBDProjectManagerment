package com.kbd.pms.dto;

import jakarta.validation.constraints.NotNull;

public record MilestoneSubmitRequest(@NotNull Long actorUserId) {}

