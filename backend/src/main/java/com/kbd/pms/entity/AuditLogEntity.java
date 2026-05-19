package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_log")
public class AuditLogEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "action", nullable = false, length = 64)
  private String action; // UPLOAD, DOWNLOAD, DELETE, REVIEW

  @Column(name = "document_id", nullable = false)
  private Long documentId;

  @Column(name = "timestamp", nullable = false)
  private Instant timestamp;

  @Column(name = "details", length = 1024)
  private String details;

  public AuditLogEntity() {}

  // Getters and Setters
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }

  public String getAction() { return action; }
  public void setAction(String action) { this.action = action; }

  public Long getDocumentId() { return documentId; }
  public void setDocumentId(Long documentId) { this.documentId = documentId; }

  public Instant getTimestamp() { return timestamp; }
  public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

  public String getDetails() { return details; }
  public void setDetails(String details) { this.details = details; }
}