package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_milestone")
public class ProjectMilestoneEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "project_id", nullable = false)
  private Long projectId;

  @Column(name = "milestone_id", nullable = false)
  private Long milestoneId;

  @Column(name = "planned_date")
  private LocalDate plannedDate;

  @Column(name = "actual_date")
  private LocalDate actualDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private Enums.ProjectMilestoneStatus status = Enums.ProjectMilestoneStatus.NOT_STARTED;

  @Enumerated(EnumType.STRING)
  @Column(name = "decision_result", length = 32)
  private Enums.MilestoneDecisionResult decisionResult;

  @Column(name = "conditional_deadline")
  private LocalDateTime conditionalDeadline;

  @Lob
  @Column(name = "decision_notes")
  private String decisionNotes;

  @Column(name = "decision_at")
  private LocalDateTime decisionAt;

  @Column(name = "decided_by")
  private Long decidedBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public ProjectMilestoneEntity() {}

  public Long getId() { return id; }
  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public Long getMilestoneId() { return milestoneId; }
  public void setMilestoneId(Long milestoneId) { this.milestoneId = milestoneId; }
  public LocalDate getPlannedDate() { return plannedDate; }
  public void setPlannedDate(LocalDate plannedDate) { this.plannedDate = plannedDate; }
  public LocalDate getActualDate() { return actualDate; }
  public void setActualDate(LocalDate actualDate) { this.actualDate = actualDate; }
  public Enums.ProjectMilestoneStatus getStatus() { return status; }
  public void setStatus(Enums.ProjectMilestoneStatus status) { this.status = status; }
  public Enums.MilestoneDecisionResult getDecisionResult() { return decisionResult; }
  public void setDecisionResult(Enums.MilestoneDecisionResult decisionResult) { this.decisionResult = decisionResult; }
  public LocalDateTime getConditionalDeadline() { return conditionalDeadline; }
  public void setConditionalDeadline(LocalDateTime conditionalDeadline) { this.conditionalDeadline = conditionalDeadline; }
  public String getDecisionNotes() { return decisionNotes; }
  public void setDecisionNotes(String decisionNotes) { this.decisionNotes = decisionNotes; }
  public LocalDateTime getDecisionAt() { return decisionAt; }
  public void setDecisionAt(LocalDateTime decisionAt) { this.decisionAt = decisionAt; }
  public Long getDecidedBy() { return decidedBy; }
  public void setDecidedBy(Long decidedBy) { this.decidedBy = decidedBy; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

