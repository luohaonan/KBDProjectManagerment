package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "document")
public class DocumentEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "file_name", nullable = false, length = 256)
  private String fileName;

  @Column(name = "storage_path", nullable = false, length = 1024)
  private String storagePath;

  @Column(name = "file_type", length = 64)
  private String fileType;

  @Column(name = "project_id", nullable = false)
  private Long projectId;

  @Enumerated(EnumType.STRING)
  @Column(name = "milestone_phase", nullable = false)
  private Enums.MilestoneStage milestonePhase;

  @Column(name = "uploader", nullable = false)
  private Long uploader;

  @Enumerated(EnumType.STRING)
  @Column(name = "compliance_status", nullable = false)
  private Enums.ComplianceStatus complianceStatus = Enums.ComplianceStatus.PENDING;

  @Column(name = "is_locked", nullable = false)
  private Boolean isLocked = false;

  @Column(name = "uploaded_at", nullable = false)
  private LocalDateTime uploadedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public DocumentEntity() {}

  // Getters and Setters
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public String getFileName() { return fileName; }
  public void setFileName(String fileName) { this.fileName = fileName; }

  public String getStoragePath() { return storagePath; }
  public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

  public String getFileType() { return fileType; }
  public void setFileType(String fileType) { this.fileType = fileType; }

  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }

  public Enums.MilestoneStage getMilestonePhase() { return milestonePhase; }
  public void setMilestonePhase(Enums.MilestoneStage milestonePhase) { this.milestonePhase = milestonePhase; }

  public Long getUploader() { return uploader; }
  public void setUploader(Long uploader) { this.uploader = uploader; }

  public Enums.ComplianceStatus getComplianceStatus() { return complianceStatus; }
  public void setComplianceStatus(Enums.ComplianceStatus complianceStatus) { this.complianceStatus = complianceStatus; }

  public Boolean getIsLocked() { return isLocked; }
  public void setIsLocked(Boolean isLocked) { this.isLocked = isLocked; }

  public LocalDateTime getUploadedAt() { return uploadedAt; }
  public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}