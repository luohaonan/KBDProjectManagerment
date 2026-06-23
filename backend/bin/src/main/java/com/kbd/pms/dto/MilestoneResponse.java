package com.kbd.pms.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MilestoneResponse(
    String code,
    String name,
    String status,
    LocalDate plannedDate,
    LocalDate actualDate,
    String leadDeptText,
    Integer sortNo,
    String decisionResult,
    LocalDateTime decisionAt,
    String decisionNotes
) {}
