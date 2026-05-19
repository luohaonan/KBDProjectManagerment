package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "milestone_def")
public class MilestoneDefEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "milestone_code", nullable = false, length = 4)
  private String milestoneCode;

  @Column(name = "milestone_name", nullable = false, length = 128)
  private String milestoneName;

  @Lob
  @Column(name = "stage_definition", nullable = false)
  private String stageDefinition;

  @Lob
  @Column(name = "core_deliverables", nullable = false)
  private String coreDeliverables;

  @Column(name = "lead_dept_text", nullable = false, length = 256)
  private String leadDeptText;

  @Column(name = "decision_gate", nullable = false, length = 128)
  private String decisionGate;

  @Column(name = "sort_no", nullable = false)
  private Integer sortNo;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = Boolean.TRUE;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public MilestoneDefEntity() {}

  public Long getId() { return id; }
  public String getMilestoneCode() { return milestoneCode; }
  public void setMilestoneCode(String milestoneCode) { this.milestoneCode = milestoneCode; }
  public String getMilestoneName() { return milestoneName; }
  public void setMilestoneName(String milestoneName) { this.milestoneName = milestoneName; }
  public String getStageDefinition() { return stageDefinition; }
  public void setStageDefinition(String stageDefinition) { this.stageDefinition = stageDefinition; }
  public String getCoreDeliverables() { return coreDeliverables; }
  public void setCoreDeliverables(String coreDeliverables) { this.coreDeliverables = coreDeliverables; }
  public String getLeadDeptText() { return leadDeptText; }
  public void setLeadDeptText(String leadDeptText) { this.leadDeptText = leadDeptText; }
  public String getDecisionGate() { return decisionGate; }
  public void setDecisionGate(String decisionGate) { this.decisionGate = decisionGate; }
  public Integer getSortNo() { return sortNo; }
  public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
  public Boolean getIsActive() { return isActive; }
  public void setIsActive(Boolean active) { isActive = active; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}

