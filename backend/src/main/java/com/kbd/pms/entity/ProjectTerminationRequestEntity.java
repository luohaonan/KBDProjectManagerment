package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 项目终止申请表
 * 流程: PM发起→效率管理部审批→PMC审批→PM完成终止任务→结项
 */
@Entity
@Table(name = "project_termination_request")
public class ProjectTerminationRequestEntity {

  public enum Status {
    DRAFT, SUBMITTED,
    EFFICIENCY_APPROVED, EFFICIENCY_REJECTED,
    PMC_APPROVED, PMC_REJECTED,
    COMPLETED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "project_id", nullable = false)
  private Long projectId;

  @Column(name = "requested_by")
  private Long requestedBy;

  @Column(name = "termination_reason", columnDefinition = "TEXT")
  private String terminationReason;

  @Column(name = "attachment_uri", length = 1024)
  private String attachmentUri;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private Status status = Status.DRAFT;

  @Column(name = "efficiency_approver_id")
  private Long efficiencyApproverId;

  @Column(name = "efficiency_opinion", columnDefinition = "TEXT")
  private String efficiencyOpinion;

  @Column(name = "efficiency_decided_at")
  private LocalDateTime efficiencyDecidedAt;

  @Column(name = "pmc_approver_id")
  private Long pmcApproverId;

  @Column(name = "pmc_opinion", columnDefinition = "TEXT")
  private String pmcOpinion;

  @Column(name = "pmc_decided_at")
  private LocalDateTime pmcDecidedAt;

  @Column(name = "summary_report_uri", length = 1024)
  private String summaryReportUri;

  @Column(name = "asset_disposal_confirmed", nullable = false)
  private Boolean assetDisposalConfirmed = Boolean.FALSE;

  @Column(name = "archive_confirmed", nullable = false)
  private Boolean archiveConfirmed = Boolean.FALSE;

  @Column(name = "submitted_at")
  private LocalDateTime submittedAt;

  @Column(name = "finished_at")
  private LocalDateTime finishedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public ProjectTerminationRequestEntity() {}

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public Long getRequestedBy() { return requestedBy; }
  public void setRequestedBy(Long requestedBy) { this.requestedBy = requestedBy; }
  public String getTerminationReason() { return terminationReason; }
  public void setTerminationReason(String terminationReason) { this.terminationReason = terminationReason; }
  public String getAttachmentUri() { return attachmentUri; }
  public void setAttachmentUri(String attachmentUri) { this.attachmentUri = attachmentUri; }
  public Status getStatus() { return status; }
  public void setStatus(Status status) { this.status = status; }
  public Long getEfficiencyApproverId() { return efficiencyApproverId; }
  public void setEfficiencyApproverId(Long efficiencyApproverId) { this.efficiencyApproverId = efficiencyApproverId; }
  public String getEfficiencyOpinion() { return efficiencyOpinion; }
  public void setEfficiencyOpinion(String efficiencyOpinion) { this.efficiencyOpinion = efficiencyOpinion; }
  public LocalDateTime getEfficiencyDecidedAt() { return efficiencyDecidedAt; }
  public void setEfficiencyDecidedAt(LocalDateTime efficiencyDecidedAt) { this.efficiencyDecidedAt = efficiencyDecidedAt; }
  public Long getPmcApproverId() { return pmcApproverId; }
  public void setPmcApproverId(Long pmcApproverId) { this.pmcApproverId = pmcApproverId; }
  public String getPmcOpinion() { return pmcOpinion; }
  public void setPmcOpinion(String pmcOpinion) { this.pmcOpinion = pmcOpinion; }
  public LocalDateTime getPmcDecidedAt() { return pmcDecidedAt; }
  public void setPmcDecidedAt(LocalDateTime pmcDecidedAt) { this.pmcDecidedAt = pmcDecidedAt; }
  public String getSummaryReportUri() { return summaryReportUri; }
  public void setSummaryReportUri(String summaryReportUri) { this.summaryReportUri = summaryReportUri; }
  public Boolean getAssetDisposalConfirmed() { return assetDisposalConfirmed; }
  public void setAssetDisposalConfirmed(Boolean assetDisposalConfirmed) { this.assetDisposalConfirmed = assetDisposalConfirmed; }
  public Boolean getArchiveConfirmed() { return archiveConfirmed; }
  public void setArchiveConfirmed(Boolean archiveConfirmed) { this.archiveConfirmed = archiveConfirmed; }
  public LocalDateTime getSubmittedAt() { return submittedAt; }
  public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
  public LocalDateTime getFinishedAt() { return finishedAt; }
  public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}