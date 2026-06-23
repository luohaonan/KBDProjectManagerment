package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "project_team_member")
public class ProjectTeamMemberEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "project_id", nullable = false)
  private Long projectId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "dept_id")
  private Long deptId;

  @Enumerated(EnumType.STRING)
  @Column(name = "team_role", nullable = false, length = 32)
  private Enums.ProjectTeamRole teamRole = Enums.ProjectTeamRole.MEMBER;

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected ProjectTeamMemberEntity() {}

  public Long getId() { return id; }
  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Long getDeptId() { return deptId; }
  public void setDeptId(Long deptId) { this.deptId = deptId; }
  public Enums.ProjectTeamRole getTeamRole() { return teamRole; }
  public void setTeamRole(Enums.ProjectTeamRole teamRole) { this.teamRole = teamRole; }
  public LocalDate getEffectiveFrom() { return effectiveFrom; }
  public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
  public LocalDate getEffectiveTo() { return effectiveTo; }
  public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
  public Instant getCreatedAt() { return createdAt; }
}

