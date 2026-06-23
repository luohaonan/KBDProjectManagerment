package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "project_budget_snapshot")
public class ProjectBudgetSnapshotEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "project_id", nullable = false)
  private Long projectId;

  @Column(name = "budget_plan_id")
  private Long budgetPlanId;

  @Column(name = "snapshot_month", nullable = false, length = 7)
  private String snapshotMonth; // YYYY-MM

  @Column(name = "internal_spent", nullable = false, precision = 18, scale = 2)
  private BigDecimal internalSpent = BigDecimal.ZERO;

  @Column(name = "external_spent", nullable = false, precision = 18, scale = 2)
  private BigDecimal externalSpent = BigDecimal.ZERO;

  // generated column in DB
  @Column(name = "total_spent", precision = 18, scale = 2, insertable = false, updatable = false)
  private BigDecimal totalSpent;

  @Column(name = "planned_total_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal plannedTotalAmount = BigDecimal.ZERO;

  // generated column in DB
  @Column(name = "utilization_ratio", precision = 10, scale = 6, insertable = false, updatable = false)
  private BigDecimal utilizationRatio;

  @Enumerated(EnumType.STRING)
  @Column(name = "warning_level", nullable = false, length = 16)
  private Enums.WarningLevel warningLevel = Enums.WarningLevel.NONE;

  @Column(name = "generated_at", nullable = false)
  private Instant generatedAt;

  protected ProjectBudgetSnapshotEntity() {}

  public Long getId() { return id; }
  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public Long getBudgetPlanId() { return budgetPlanId; }
  public void setBudgetPlanId(Long budgetPlanId) { this.budgetPlanId = budgetPlanId; }
  public String getSnapshotMonth() { return snapshotMonth; }
  public void setSnapshotMonth(String snapshotMonth) { this.snapshotMonth = snapshotMonth; }
  public BigDecimal getInternalSpent() { return internalSpent; }
  public void setInternalSpent(BigDecimal internalSpent) { this.internalSpent = internalSpent; }
  public BigDecimal getExternalSpent() { return externalSpent; }
  public void setExternalSpent(BigDecimal externalSpent) { this.externalSpent = externalSpent; }
  public BigDecimal getTotalSpent() { return totalSpent; }
  public BigDecimal getPlannedTotalAmount() { return plannedTotalAmount; }
  public void setPlannedTotalAmount(BigDecimal plannedTotalAmount) { this.plannedTotalAmount = plannedTotalAmount; }
  public BigDecimal getUtilizationRatio() { return utilizationRatio; }
  public Enums.WarningLevel getWarningLevel() { return warningLevel; }
  public void setWarningLevel(Enums.WarningLevel warningLevel) { this.warningLevel = warningLevel; }
  public Instant getGeneratedAt() { return generatedAt; }
}

