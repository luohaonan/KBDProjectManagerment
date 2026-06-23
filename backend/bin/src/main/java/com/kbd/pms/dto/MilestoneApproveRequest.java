package com.kbd.pms.dto;

import jakarta.validation.constraints.NotNull;

public record MilestoneApproveRequest(
    @NotNull Long actorUserId,
    @NotNull Decision decision,
    String reviewNotes,
    /** No-Go 时建议填写终止原因（会落入 project.terminated_reason） */
    String terminationReason,
    /** Conditional Go 的附加条件（可选） */
    String conditionalNotes
) {
  public enum Decision { GO, CONDITIONAL_GO, NO_GO }
}

