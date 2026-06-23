package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "iam_user")
public class IamUserEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_no", nullable = false, length = 32)
  private String userNo;

  @Column(name = "display_name", nullable = false, length = 64)
  private String displayName;

  @Column(name = "email", length = 128)
  private String email;

  @Column(name = "dept_id")
  private Long deptId;

  @Column(name = "title", length = 64)
  private String title;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = Boolean.TRUE;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected IamUserEntity() {}

  public Long getId() { return id; }
  public String getUserNo() { return userNo; }
  public void setUserNo(String userNo) { this.userNo = userNo; }
  public String getDisplayName() { return displayName; }
  public void setDisplayName(String displayName) { this.displayName = displayName; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public Long getDeptId() { return deptId; }
  public void setDeptId(Long deptId) { this.deptId = deptId; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public Boolean getIsActive() { return isActive; }
  public void setIsActive(Boolean active) { isActive = active; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}

