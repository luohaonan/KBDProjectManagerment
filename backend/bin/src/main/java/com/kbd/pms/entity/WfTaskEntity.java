package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "wf_task")
public class WfTaskEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "instance_id", nullable = false)
  private Long instanceId;

  @Column(name = "node_id", nullable = false)
  private Long nodeId;

  @Column(name = "task_name", nullable = false, length = 128)
  private String taskName;

  @Column(name = "assignee_user_id")
  private Long assigneeUserId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private Enums.WfTaskStatus status = Enums.WfTaskStatus.PENDING;

  @Column(name = "decided_at")
  private LocalDateTime decidedAt;

  @Lob
  @Column(name = "decision_notes")
  private String decisionNotes;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected WfTaskEntity() {}

  public Long getId() { return id; }
  public Long getInstanceId() { return instanceId; }
  public void setInstanceId(Long instanceId) { this.instanceId = instanceId; }
  public Long getNodeId() { return nodeId; }
  public void setNodeId(Long nodeId) { this.nodeId = nodeId; }
  public String getTaskName() { return taskName; }
  public void setTaskName(String taskName) { this.taskName = taskName; }
  public Long getAssigneeUserId() { return assigneeUserId; }
  public void setAssigneeUserId(Long assigneeUserId) { this.assigneeUserId = assigneeUserId; }
  public Enums.WfTaskStatus getStatus() { return status; }
  public void setStatus(Enums.WfTaskStatus status) { this.status = status; }
  public LocalDateTime getDecidedAt() { return decidedAt; }
  public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
  public String getDecisionNotes() { return decisionNotes; }
  public void setDecisionNotes(String decisionNotes) { this.decisionNotes = decisionNotes; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}

