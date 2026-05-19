package com.kbd.pms.dto;

import com.kbd.pms.entity.Enums;
import java.math.BigDecimal;

public record ExpenseRequest(
        Long projectId,
        BigDecimal amount,
        Enums.ExpenseCategory category,
        String description,
        Long createdBy
) {}