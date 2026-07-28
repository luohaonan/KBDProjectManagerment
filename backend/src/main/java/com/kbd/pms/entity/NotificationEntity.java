package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notification")
public class NotificationEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "recipient_user_id", nullable = false)
  private Long recipientUserId;

  @Column(name = "type", nullable = false, length = 32)
  private String type;

  @Column(name = "title", nullable = false, length = 256)
  private String title;

  @Column(name = "content", columnDefinition = "TEXT")
  private String content;

  @Column(name = "project_id")
  private Long projectId;

  @Column(name = "milestone_code", length = 8)
  private String milestoneCode;

  @Column(name = "related_user_id")
  private Long relatedUserId;

  @Column(name = "is_read", nullable = false)
  private Boolean isRead = false;

  @Column(name = "is_todo", nullable = false)
  private Boolean isTodo = false;

  @Column(name = "is_done", nullable = false)
  private Boolean isDone = false;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public NotificationEntity() {}

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public Long getRecipientUserId() { return recipientUserId; }
  public void setRecipientUserId(Long recipientUserId) { this.recipientUserId = recipientUserId; }

  public String getType() { return type; }
  public void setType(String type) { this.type = type; }

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }

  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }

  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }

  public String getMilestoneCode() { return milestoneCode; }
  public void setMilestoneCode(String milestoneCode) { this.milestoneCode = milestoneCode; }

  public Long getRelatedUserId() { return relatedUserId; }
  public void setRelatedUserId(Long relatedUserId) { this.relatedUserId = relatedUserId; }

  public Boolean getIsRead() { return isRead; }
  public void setIsRead(Boolean isRead) { this.isRead = isRead; }

  public Boolean getIsTodo() { return isTodo; }
  public void setIsTodo(Boolean isTodo) { this.isTodo = isTodo; }

  public Boolean getIsDone() { return isDone; }
  public void setIsDone(Boolean isDone) { this.isDone = isDone; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}