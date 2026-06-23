package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_budget_plan")
public class ProjectBudgetPlanEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "project_id", nullable = false)
  private Long projectId;

  @Enumerated(EnumType.STRING)
  @Column(name = "plan_type", nullable = false, length = 32)
  private Enums.BudgetPlanType planType;

  @Column(name = "fiscal_year")
  private Integer fiscalYear;

  @Column(name = "stage_from_milestone_id")
  private Long stageFromMilestoneId;

  @Column(name = "stage_to_milestone_id")
  private Long stageToMilestoneId;

  @Column(name = "version_no", nullable = false)
  private Integer versionNo = 1;

  @Column(name = "internal_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal internalAmount = BigDecimal.ZERO;

  @Column(name = "external_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal externalAmount = BigDecimal.ZERO;

  // generated column in DB
  @Column(name = "total_amount", precision = 18, scale = 2, insertable = false, updatable = false)
  private BigDecimal totalAmount;

  @Enumerated(EnumType.STRING)
  @Column(name = "approved_status", nullable = false, length = 16)
  private Enums.ApprovalStatus approvedStatus = Enums.ApprovalStatus.DRAFT;

  @Column(name = "approved_at")
  private LocalDateTime approvedAt;

  @Column(name = "approved_by")
  private Long approvedBy;

  @Lob
  @Column(name = "notes")
  private String notes;

  @Column(name = "created_by")
  private Long createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_by")
  private Long updatedBy;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public ProjectBudgetPlanEntity() {}

  public Long getId() { return id; }
  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public Enums.BudgetPlanType getPlanType() { return planType; }
  public void setPlanType(Enums.BudgetPlanType planType) { this.planType = planType; }
  public Integer getFiscalYear() { return fiscalYear; }
  public void setFiscalYear(Integer fiscalYear) { this.fiscalYear = fiscalYear; }
  public Long getStageFromMilestoneId() { return stageFromMilestoneId; }
  public void setStageFromMilestoneId(Long stageFromMilestoneId) { this.stageFromMilestoneId = stageFromMilestoneId; }
  public Long getStageToMilestoneId() { return stageToMilestoneId; }
  public void setStageToMilestoneId(Long stageToMilestoneId) { this.stageToMilestoneId = stageToMilestoneId; }
  public Integer getVersionNo() { return versionNo; }
  public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
  public BigDecimal getInternalAmount() { return internalAmount; }
  public void setInternalAmount(BigDecimal internalAmount) { this.internalAmount = internalAmount; }
  public BigDecimal getExternalAmount() { return externalAmount; }
  public void setExternalAmount(BigDecimal externalAmount) { this.externalAmount = externalAmount; }
  public BigDecimal getTotalAmount() { return totalAmount; }
  public Enums.ApprovalStatus getApprovedStatus() { return approvedStatus; }
  public void setApprovedStatus(Enums.ApprovalStatus approvedStatus) { this.approvedStatus = approvedStatus; }
  public LocalDateTime getApprovedAt() { return approvedAt; }
  public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
  public Long getApprovedBy() { return approvedBy; }
  public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Long getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

