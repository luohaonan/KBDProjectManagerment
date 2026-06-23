package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_change_request")
public class ProjectChangeRequestEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "project_id", nullable = false)
  private Long projectId;

  @Enumerated(EnumType.STRING)
  @Column(name = "change_type", nullable = false, length = 32)
  private Enums.ChangeType changeType;

  @Lob
  @Column(name = "reason_text", nullable = false)
  private String reasonText;

  @Lob
  @Column(name = "before_text")
  private String beforeText;

  @Lob
  @Column(name = "after_text")
  private String afterText;

  @Lob
  @Column(name = "impact_milestone_text")
  private String impactMilestoneText;

  @Lob
  @Column(name = "impact_budget_text")
  private String impactBudgetText;

  @Lob
  @Column(name = "impact_resource_text")
  private String impactResourceText;

  @Column(name = "requested_by")
  private Long requestedBy;

  @Column(name = "requested_at", nullable = false)
  private LocalDateTime requestedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private Enums.ApprovalStatus status = Enums.ApprovalStatus.DRAFT;

  @Column(name = "wf_instance_id")
  private Long wfInstanceId;

  @Enumerated(EnumType.STRING)
  @Column(name = "pmc_decision", length = 32)
  private Enums.PmcDecision pmcDecision;

  @Lob
  @Column(name = "pmc_decision_text")
  private String pmcDecisionText;

  @Column(name = "pmc_decided_at")
  private LocalDateTime pmcDecidedAt;

  @Column(name = "pmc_decided_by")
  private Long pmcDecidedBy;

  @Column(name = "target_milestone_id")
  private Long targetMilestoneId;

  @Column(name = "target_milestone_planned_date")
  private LocalDate targetMilestonePlannedDate;

  @Column(name = "previous_budget_amount", precision = 18, scale = 2)
  private BigDecimal previousBudgetAmount;

  @Column(name = "requested_budget_amount", precision = 18, scale = 2)
  private BigDecimal requestedBudgetAmount;

  @Column(name = "new_pm_user_id")
  private Long newPmUserId;

  @Column(name = "asset_disposal_confirmed")
  private Boolean assetDisposalConfirmed;

  @Column(name = "archive_confirmed")
  private Boolean archiveConfirmed;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public ProjectChangeRequestEntity() {}

  public Long getId() { return id; }
  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public Enums.ChangeType getChangeType() { return changeType; }
  public void setChangeType(Enums.ChangeType changeType) { this.changeType = changeType; }
  public String getReasonText() { return reasonText; }
  public void setReasonText(String reasonText) { this.reasonText = reasonText; }
  public String getBeforeText() { return beforeText; }
  public void setBeforeText(String beforeText) { this.beforeText = beforeText; }
  public String getAfterText() { return afterText; }
  public void setAfterText(String afterText) { this.afterText = afterText; }
  public String getImpactMilestoneText() { return impactMilestoneText; }
  public void setImpactMilestoneText(String impactMilestoneText) { this.impactMilestoneText = impactMilestoneText; }
  public String getImpactBudgetText() { return impactBudgetText; }
  public void setImpactBudgetText(String impactBudgetText) { this.impactBudgetText = impactBudgetText; }
  public String getImpactResourceText() { return impactResourceText; }
  public void setImpactResourceText(String impactResourceText) { this.impactResourceText = impactResourceText; }
  public Long getRequestedBy() { return requestedBy; }
  public void setRequestedBy(Long requestedBy) { this.requestedBy = requestedBy; }
  public LocalDateTime getRequestedAt() { return requestedAt; }
  public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
  public Enums.ApprovalStatus getStatus() { return status; }
  public void setStatus(Enums.ApprovalStatus status) { this.status = status; }
  public Long getWfInstanceId() { return wfInstanceId; }
  public void setWfInstanceId(Long wfInstanceId) { this.wfInstanceId = wfInstanceId; }
  public Enums.PmcDecision getPmcDecision() { return pmcDecision; }
  public void setPmcDecision(Enums.PmcDecision pmcDecision) { this.pmcDecision = pmcDecision; }
  public String getPmcDecisionText() { return pmcDecisionText; }
  public void setPmcDecisionText(String pmcDecisionText) { this.pmcDecisionText = pmcDecisionText; }
  public LocalDateTime getPmcDecidedAt() { return pmcDecidedAt; }
  public void setPmcDecidedAt(LocalDateTime pmcDecidedAt) { this.pmcDecidedAt = pmcDecidedAt; }
  public Long getPmcDecidedBy() { return pmcDecidedBy; }
  public void setPmcDecidedBy(Long pmcDecidedBy) { this.pmcDecidedBy = pmcDecidedBy; }
  public Long getTargetMilestoneId() { return targetMilestoneId; }
  public void setTargetMilestoneId(Long targetMilestoneId) { this.targetMilestoneId = targetMilestoneId; }
  public LocalDate getTargetMilestonePlannedDate() { return targetMilestonePlannedDate; }
  public void setTargetMilestonePlannedDate(LocalDate targetMilestonePlannedDate) { this.targetMilestonePlannedDate = targetMilestonePlannedDate; }
  public BigDecimal getPreviousBudgetAmount() { return previousBudgetAmount; }
  public void setPreviousBudgetAmount(BigDecimal previousBudgetAmount) { this.previousBudgetAmount = previousBudgetAmount; }
  public BigDecimal getRequestedBudgetAmount() { return requestedBudgetAmount; }
  public void setRequestedBudgetAmount(BigDecimal requestedBudgetAmount) { this.requestedBudgetAmount = requestedBudgetAmount; }
  public Long getNewPmUserId() { return newPmUserId; }
  public void setNewPmUserId(Long newPmUserId) { this.newPmUserId = newPmUserId; }
  public Boolean getAssetDisposalConfirmed() { return assetDisposalConfirmed; }
  public void setAssetDisposalConfirmed(Boolean assetDisposalConfirmed) { this.assetDisposalConfirmed = assetDisposalConfirmed; }
  public Boolean getArchiveConfirmed() { return archiveConfirmed; }
  public void setArchiveConfirmed(Boolean archiveConfirmed) { this.archiveConfirmed = archiveConfirmed; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

