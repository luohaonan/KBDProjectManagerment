package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "governance_committee")
public class GovernanceCommitteeEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "committee_code", nullable = false, length = 32)
  private String committeeCode;

  @Column(name = "committee_name", nullable = false, length = 128)
  private String committeeName;

  @Column(name = "chair_user_id")
  private Long chairUserId;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = Boolean.TRUE;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected GovernanceCommitteeEntity() {}

  public Long getId() { return id; }
  public String getCommitteeCode() { return committeeCode; }
  public void setCommitteeCode(String committeeCode) { this.committeeCode = committeeCode; }
  public String getCommitteeName() { return committeeName; }
  public void setCommitteeName(String committeeName) { this.committeeName = committeeName; }
  public Long getChairUserId() { return chairUserId; }
  public void setChairUserId(Long chairUserId) { this.chairUserId = chairUserId; }
  public Boolean getIsActive() { return isActive; }
  public void setIsActive(Boolean active) { isActive = active; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}

