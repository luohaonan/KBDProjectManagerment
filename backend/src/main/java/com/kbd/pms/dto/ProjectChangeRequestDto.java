package com.kbd.pms.dto;

import com.kbd.pms.entity.Enums;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class ProjectChangeRequestDto {

  private ProjectChangeRequestDto() {}

  public record ProjectChangeRequestCreateRequest(
      @NotNull Long requestedBy,
      @NotNull Enums.ChangeType changeType,
      @NotBlank String reasonText,
      String beforeText,
      String afterText,
      String impactMilestoneText,
      String impactBudgetText,
      String impactResourceText,
      Long targetMilestoneId,
      LocalDate targetMilestonePlannedDate,
      BigDecimal previousBudgetAmount,
      BigDecimal requestedBudgetAmount,
      Long newPmUserId,
      Boolean assetDisposalConfirmed,
      Boolean archiveConfirmed
  ) {}

  public record ProjectChangeRequestResponse(
      Long id,
      Long projectId,
      Enums.ChangeType changeType,
      String status,
      String reasonText,
      String beforeText,
      String afterText,
      String impactMilestoneText,
      String impactBudgetText,
      String impactResourceText,
      Long targetMilestoneId,
      LocalDate targetMilestonePlannedDate,
      BigDecimal previousBudgetAmount,
      BigDecimal requestedBudgetAmount,
      Long newPmUserId,
      Boolean assetDisposalConfirmed,
      Boolean archiveConfirmed,
      String pmcDecision,
      String pmcDecisionText,
      LocalDateTime requestedAt,
      LocalDateTime pmcDecidedAt
  ) {}
}
