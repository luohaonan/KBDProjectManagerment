package com.kbd.pms.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record MilestoneRescheduleRequest(
    @NotNull Long actorUserId,
    @NotNull LocalDate newPlannedDate
) {}
