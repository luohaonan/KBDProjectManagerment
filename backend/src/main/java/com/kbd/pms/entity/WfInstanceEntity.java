package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "wf_instance")
public class WfInstanceEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "template_id", nullable = false)
  private Long templateId;

  @Column(name = "business_type", nullable = false, length = 64)
  private String businessType;

  @Column(name = "business_id", nullable = false)
  private Long businessId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private Enums.WfInstanceStatus status = Enums.WfInstanceStatus.DRAFT;

  @Column(name = "started_by")
  private Long startedBy;

  @Column(name = "started_at")
  private LocalDateTime startedAt;

  @Column(name = "finished_at")
  private LocalDateTime finishedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected WfInstanceEntity() {}

  public Long getId() { return id; }
  public Long getTemplateId() { return templateId; }
  public void setTemplateId(Long templateId) { this.templateId = templateId; }
  public String getBusinessType() { return businessType; }
  public void setBusinessType(String businessType) { this.businessType = businessType; }
  public Long getBusinessId() { return businessId; }
  public void setBusinessId(Long businessId) { this.businessId = businessId; }
  public Enums.WfInstanceStatus getStatus() { return status; }
  public void setStatus(Enums.WfInstanceStatus status) { this.status = status; }
  public Long getStartedBy() { return startedBy; }
  public void setStartedBy(Long startedBy) { this.startedBy = startedBy; }
  public LocalDateTime getStartedAt() { return startedAt; }
  public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
  public LocalDateTime getFinishedAt() { return finishedAt; }
  public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}

