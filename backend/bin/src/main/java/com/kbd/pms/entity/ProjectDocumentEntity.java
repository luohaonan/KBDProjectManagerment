package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_document")
public class ProjectDocumentEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "project_id", nullable = false)
  private Long projectId;

  @Column(name = "milestone_id")
  private Long milestoneId;

  @Column(name = "doc_type", nullable = false, length = 64)
  private String docType;

  @Column(name = "doc_name", nullable = false, length = 256)
  private String docName;

  @Column(name = "storage_uri", nullable = false, length = 1024)
  private String storageUri;

  @Column(name = "uploaded_by")
  private Long uploadedBy;

  @Column(name = "uploaded_at", nullable = false)
  private LocalDateTime uploadedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public ProjectDocumentEntity() {}

  public Long getId() { return id; }
  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public Long getMilestoneId() { return milestoneId; }
  public void setMilestoneId(Long milestoneId) { this.milestoneId = milestoneId; }
  public String getDocType() { return docType; }
  public void setDocType(String docType) { this.docType = docType; }
  public String getDocName() { return docName; }
  public void setDocName(String docName) { this.docName = docName; }
  public String getStorageUri() { return storageUri; }
  public void setStorageUri(String storageUri) { this.storageUri = storageUri; }
  public Long getUploadedBy() { return uploadedBy; }
  public void setUploadedBy(Long uploadedBy) { this.uploadedBy = uploadedBy; }
  public LocalDateTime getUploadedAt() { return uploadedAt; }
  public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

