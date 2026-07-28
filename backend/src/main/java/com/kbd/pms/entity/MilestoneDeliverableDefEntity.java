package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "milestone_deliverable_def")
public class MilestoneDeliverableDefEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "milestone_code", nullable = false, length = 4)
  private String milestoneCode;

  @Column(name = "slot_code", nullable = false, length = 64)
  private String slotCode;

  @Column(name = "slot_name", nullable = false, length = 256)
  private String slotName;

  @Column(name = "is_required", nullable = false)
  private Boolean isRequired = true;

  @Column(name = "sort_no", nullable = false)
  private Integer sortNo = 0;

  @Column(name = "description", length = 512)
  private String description;

  @Column(name = "allowed_file_types", length = 256)
  private String allowedFileTypes = ".pdf,.doc,.docx";

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public MilestoneDeliverableDefEntity() {}

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public String getMilestoneCode() { return milestoneCode; }
  public void setMilestoneCode(String milestoneCode) { this.milestoneCode = milestoneCode; }

  public String getSlotCode() { return slotCode; }
  public void setSlotCode(String slotCode) { this.slotCode = slotCode; }

  public String getSlotName() { return slotName; }
  public void setSlotName(String slotName) { this.slotName = slotName; }

  public Boolean getIsRequired() { return isRequired; }
  public void setIsRequired(Boolean isRequired) { this.isRequired = isRequired; }

  public Integer getSortNo() { return sortNo; }
  public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public String getAllowedFileTypes() { return allowedFileTypes; }
  public void setAllowedFileTypes(String allowedFileTypes) { this.allowedFileTypes = allowedFileTypes; }

  public Boolean getIsActive() { return isActive; }
  public void setIsActive(Boolean isActive) { this.isActive = isActive; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}