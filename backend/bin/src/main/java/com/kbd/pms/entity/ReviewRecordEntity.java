package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 评审记录 - 用于展示评审历史
 */
@Entity
@Table(name = "review_record")
public class ReviewRecordEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "project_id", nullable = false)
  private Long projectId;

  @Column(name = "project_milestone_id", nullable = false)
  private Long projectMilestoneId;

  @Column(name = "review_approval_id")
  private Long reviewApprovalId;

  @Column(name = "action", nullable = false, length = 32)
  private String action;

  @Column(name = "actor_user_id")
  private Long actorUserId;

  @Column(name = "actor_role", length = 64)
  private String actorRole;

  @Column(name = "result", length = 32)
  private String result;

  @Column(name = "opinion", columnDefinition = "TEXT")
  private String opinion;

  @Column(name = "action_at", nullable = false)
  private LocalDateTime actionAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public ReviewRecordEntity() {}

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public Long getProjectMilestoneId() { return projectMilestoneId; }
  public void setProjectMilestoneId(Long projectMilestoneId) { this.projectMilestoneId = projectMilestoneId; }
  public Long getReviewApprovalId() { return reviewApprovalId; }
  public void setReviewApprovalId(Long reviewApprovalId) { this.reviewApprovalId = reviewApprovalId; }
  public String getAction() { return action; }
  public void setAction(String action) { this.action = action; }
  public Long getActorUserId() { return actorUserId; }
  public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }
  public String getActorRole() { return actorRole; }
  public void setActorRole(String actorRole) { this.actorRole = actorRole; }
  public String getResult() { return result; }
  public void setResult(String result) { this.result = result; }
  public String getOpinion() { return opinion; }
  public void setOpinion(String opinion) { this.opinion = opinion; }
  public LocalDateTime getActionAt() { return actionAt; }
  public void setActionAt(LocalDateTime actionAt) { this.actionAt = actionAt; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
