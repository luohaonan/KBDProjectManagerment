package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "milestone_history")
public class MilestoneHistoryEntity {
  public enum Action { SUBMIT_REVIEW, DECISION }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "project_id", nullable = false)
  private Long projectId;

  @Column(name = "project_milestone_id", nullable = false)
  private Long projectMilestoneId;

  @Enumerated(EnumType.STRING)
  @Column(name = "action", nullable = false, length = 16)
  private Action action;

  @Column(name = "from_status", length = 32)
  private String fromStatus;

  @Column(name = "to_status", length = 32)
  private String toStatus;

  @Column(name = "actor_user_id")
  private Long actorUserId;

  @Column(name = "action_at", nullable = false)
  private LocalDateTime actionAt;

  @Lob
  @Column(name = "notes")
  private String notes;

  @Column(name = "payload_json", columnDefinition = "json")
  private String payloadJson;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public MilestoneHistoryEntity() {}

  public Long getId() { return id; }
  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public Long getProjectMilestoneId() { return projectMilestoneId; }
  public void setProjectMilestoneId(Long projectMilestoneId) { this.projectMilestoneId = projectMilestoneId; }
  public Action getAction() { return action; }
  public void setAction(Action action) { this.action = action; }
  public String getFromStatus() { return fromStatus; }
  public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
  public String getToStatus() { return toStatus; }
  public void setToStatus(String toStatus) { this.toStatus = toStatus; }
  public Long getActorUserId() { return actorUserId; }
  public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }
  public LocalDateTime getActionAt() { return actionAt; }
  public void setActionAt(LocalDateTime actionAt) { this.actionAt = actionAt; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public String getPayloadJson() { return payloadJson; }
  public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

