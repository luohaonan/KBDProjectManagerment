package com.kbd.pms.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record BudgetChangeRequest(
    @NotNull Long actorUserId,
    @NotNull BigDecimal requestedBudget,
    String note
) {}
