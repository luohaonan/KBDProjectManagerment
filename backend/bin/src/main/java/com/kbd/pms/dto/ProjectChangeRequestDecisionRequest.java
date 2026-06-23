package com.kbd.pms.dto;

import jakarta.validation.constraints.NotNull;

public record ProjectChangeRequestDecisionRequest(
    @NotNull Long actorUserId,
    @NotNull Decision decision,
    String decisionText
) {

  public enum Decision {
    APPROVE,
    REJECT
  }
}
