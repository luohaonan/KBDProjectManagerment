package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 里程碑阶段-部门角色映射表
 * 定义每个里程碑由哪些部门的哪些角色参与，以及对应的交付物槽位
 */
@Entity
@Table(name = "milestone_dept_role")
public class MilestoneDeptRoleEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "milestone_def_id", nullable = false)
  private Long milestoneDefId;

  @Column(name = "dept_id", nullable = false)
  private Long deptId;

  @Column(name = "role_type", nullable = false, length = 50)
  private String roleType;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = Boolean.TRUE;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public MilestoneDeptRoleEntity() {}

  public Long getId() { return id; }
  public Long getMilestoneDefId() { return milestoneDefId; }
  public void setMilestoneDefId(Long milestoneDefId) { this.milestoneDefId = milestoneDefId; }
  public Long getDeptId() { return deptId; }
  public void setDeptId(Long deptId) { this.deptId = deptId; }
  public String getRoleType() { return roleType; }
  public void setRoleType(String roleType) { this.roleType = roleType; }
  public Boolean getIsActive() { return isActive; }
  public void setIsActive(Boolean isActive) { this.isActive = isActive; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}