package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "governance_committee_member")
public class GovernanceCommitteeMemberEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "committee_id", nullable = false)
  private Long committeeId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "member_role", nullable = false, length = 16)
  private Enums.CommitteeMemberRole memberRole = Enums.CommitteeMemberRole.MEMBER;

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected GovernanceCommitteeMemberEntity() {}

  public Long getId() { return id; }
  public Long getCommitteeId() { return committeeId; }
  public void setCommitteeId(Long committeeId) { this.committeeId = committeeId; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Enums.CommitteeMemberRole getMemberRole() { return memberRole; }
  public void setMemberRole(Enums.CommitteeMemberRole memberRole) { this.memberRole = memberRole; }
  public LocalDate getEffectiveFrom() { return effectiveFrom; }
  public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
  public LocalDate getEffectiveTo() { return effectiveTo; }
  public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
  public Instant getCreatedAt() { return createdAt; }
}

