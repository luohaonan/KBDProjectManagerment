package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "wf_action_log")
public class WfActionLogEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "instance_id", nullable = false)
  private Long instanceId;

  @Column(name = "task_id")
  private Long taskId;

  @Enumerated(EnumType.STRING)
  @Column(name = "action", nullable = false, length = 16)
  private Enums.WfAction action;

  @Column(name = "actor_user_id")
  private Long actorUserId;

  @Column(name = "action_at", nullable = false)
  private LocalDateTime actionAt;

  @Column(name = "payload_json", columnDefinition = "json")
  private String payloadJson;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected WfActionLogEntity() {}

  public Long getId() { return id; }
  public Long getInstanceId() { return instanceId; }
  public void setInstanceId(Long instanceId) { this.instanceId = instanceId; }
  public Long getTaskId() { return taskId; }
  public void setTaskId(Long taskId) { this.taskId = taskId; }
  public Enums.WfAction getAction() { return action; }
  public void setAction(Enums.WfAction action) { this.action = action; }
  public Long getActorUserId() { return actorUserId; }
  public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }
  public LocalDateTime getActionAt() { return actionAt; }
  public void setActionAt(LocalDateTime actionAt) { this.actionAt = actionAt; }
  public String getPayloadJson() { return payloadJson; }
  public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
  public Instant getCreatedAt() { return createdAt; }
}

