package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 立项申请审批记录 - 存储每次立项申请的审批流程信息
 */
@Entity
@Table(name = "initiation_approval")
public class InitiationApprovalEntity {

  public enum Status { SUBMITTED, APPROVED, REJECTED }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "project_id", nullable = false)
  private Long projectId;

  @Column(name = "submitter_user_id")
  private Long submitterUserId;

  @Column(name = "application_content", columnDefinition = "TEXT")
  private String applicationContent;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private Status status = Status.SUBMITTED;

  @Column(name = "submitted_at")
  private LocalDateTime submittedAt;

  @Column(name = "finished_at")
  private LocalDateTime finishedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public InitiationApprovalEntity() {}

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public Long getSubmitterUserId() { return submitterUserId; }
  public void setSubmitterUserId(Long submitterUserId) { this.submitterUserId = submitterUserId; }
  public String getApplicationContent() { return applicationContent; }
  public void setApplicationContent(String applicationContent) { this.applicationContent = applicationContent; }
  public Status getStatus() { return status; }
  public void setStatus(Status status) { this.status = status; }
  public LocalDateTime getSubmittedAt() { return submittedAt; }
  public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
  public LocalDateTime getFinishedAt() { return finishedAt; }
  public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
