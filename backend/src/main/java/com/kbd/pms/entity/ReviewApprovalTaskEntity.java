package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 评审审批人任务 - 每个审批人的审批任务
 */
@Entity
@Table(name = "review_approval_task")
public class ReviewApprovalTaskEntity {

  public enum Status { PENDING, APPROVED, REJECTED }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "review_approval_id", nullable = false)
  private Long reviewApprovalId;

  @Column(name = "approver_user_id", nullable = false)
  private Long approverUserId;

  @Column(name = "approver_role", length = 64)
  private String approverRole;

  @Column(name = "step_code", length = 32)
  private String stepCode;

  @Column(name = "deliverable_slot_code", length = 64)
  private String deliverableSlotCode;

  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder = 0;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private Status status = Status.PENDING;

  @Column(name = "decision", length = 32)
  private String decision;

  @Column(name = "opinion", columnDefinition = "TEXT")
  private String opinion;

  @Column(name = "decided_at")
  private LocalDateTime decidedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public ReviewApprovalTaskEntity() {}

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getReviewApprovalId() { return reviewApprovalId; }
  public void setReviewApprovalId(Long reviewApprovalId) { this.reviewApprovalId = reviewApprovalId; }
  public Long getApproverUserId() { return approverUserId; }
  public void setApproverUserId(Long approverUserId) { this.approverUserId = approverUserId; }
  public String getApproverRole() { return approverRole; }
  public void setApproverRole(String approverRole) { this.approverRole = approverRole; }
  public String getStepCode() { return stepCode; }
  public void setStepCode(String stepCode) { this.stepCode = stepCode; }
  public String getDeliverableSlotCode() { return deliverableSlotCode; }
  public void setDeliverableSlotCode(String deliverableSlotCode) { this.deliverableSlotCode = deliverableSlotCode; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
  public Status getStatus() { return status; }
  public void setStatus(Status status) { this.status = status; }
  public String getDecision() { return decision; }
  public void setDecision(String decision) { this.decision = decision; }
  public String getOpinion() { return opinion; }
  public void setOpinion(String opinion) { this.opinion = opinion; }
  public LocalDateTime getDecidedAt() { return decidedAt; }
  public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
