package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "org_department")
public class OrgDepartmentEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "dept_code", nullable = false, length = 32)
  private String deptCode;

  @Column(name = "dept_name", nullable = false, length = 128)
  private String deptName;

  @Enumerated(EnumType.STRING)
  @Column(name = "dept_type", nullable = false, length = 16)
  private Enums.DepartmentType deptType = Enums.DepartmentType.OTHER;

  @Column(name = "parent_id")
  private Long parentId;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = Boolean.TRUE;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected OrgDepartmentEntity() {}

  public Long getId() { return id; }
  public String getDeptCode() { return deptCode; }
  public void setDeptCode(String deptCode) { this.deptCode = deptCode; }
  public String getDeptName() { return deptName; }
  public void setDeptName(String deptName) { this.deptName = deptName; }
  public Enums.DepartmentType getDeptType() { return deptType; }
  public void setDeptType(Enums.DepartmentType deptType) { this.deptType = deptType; }
  public Long getParentId() { return parentId; }
  public void setParentId(Long parentId) { this.parentId = parentId; }
  public Boolean getIsActive() { return isActive; }
  public void setIsActive(Boolean active) { isActive = active; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}

