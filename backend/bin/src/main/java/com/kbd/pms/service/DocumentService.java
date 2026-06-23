package com.kbd.pms.service;

import com.kbd.pms.entity.DocumentEntity;
import com.kbd.pms.entity.Enums;
import com.kbd.pms.exception.ApiException;
import com.kbd.pms.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;

@Service
public class DocumentService {

  private final DocumentRepository documentRepository;
  private final FileStorageService fileStorageService;
  private final AuditLogService auditLogService;

  public DocumentService(DocumentRepository documentRepository, FileStorageService fileStorageService, AuditLogService auditLogService) {
    this.documentRepository = documentRepository;
    this.fileStorageService = fileStorageService;
    this.auditLogService = auditLogService;
  }

  @Transactional
  public DocumentEntity uploadDocument(MultipartFile file, Long projectId, String projectCode, Enums.MilestoneStage milestonePhase, String fileType, Long uploaderId) throws IOException {
    // 唯一性校验：同一里程碑下同类型的核心交付物不能有多个
    List<DocumentEntity> existing = documentRepository.findByProjectIdAndMilestonePhase(projectId, milestonePhase)
        .stream()
        .filter(d -> fileType.equals(d.getFileType()))
        .toList();
    if (!existing.isEmpty()) {
      throw new ApiException(409, "同一里程碑阶段下同类型的交付物已存在");
    }

    String storagePath = fileStorageService.storeFile(file, projectCode, milestonePhase, uploaderId);

    DocumentEntity doc = new DocumentEntity();
    doc.setFileName(file.getOriginalFilename());
    doc.setStoragePath(storagePath);
    doc.setFileType(fileType);
    doc.setProjectId(projectId);
    doc.setMilestonePhase(milestonePhase);
    doc.setUploader(uploaderId);
    doc.setComplianceStatus(Enums.ComplianceStatus.PENDING);
    doc.setIsLocked(false);
    doc.setUploadedAt(LocalDateTime.now());
    doc.setCreatedAt(Instant.now());

    DocumentEntity saved = documentRepository.save(doc);

    // 记录审计日志
    auditLogService.logAction(uploaderId, "UPLOAD", saved.getId(), "Document uploaded: " + file.getOriginalFilename());

    return saved;
  }

  public List<DocumentEntity> getDocumentsByProjectAndPhase(Long projectId, Enums.MilestoneStage milestonePhase) {
    return documentRepository.findByProjectIdAndMilestonePhase(projectId, milestonePhase);
  }

  @Transactional
  public void reviewDocument(long documentId, Enums.ComplianceStatus status, long reviewerId) {
    DocumentEntity doc = documentRepository.findById(documentId)
        .orElseThrow(() -> new ApiException(404, "文档不存在"));

    if (doc.getIsLocked()) {
      throw new ApiException(409, "文档已锁定，无法审核");
    }

    doc.setComplianceStatus(status);
    documentRepository.save(doc);

    auditLogService.logAction(reviewerId, "REVIEW", documentId, "Compliance status set to: " + status);
  }

  public DocumentEntity getDocumentById(long id) {
    return documentRepository.findById(id)
        .orElseThrow(() -> new ApiException(404, "文档不存在"));
  }

  public List<DocumentEntity> getPendingReviews() {
    return documentRepository.findByComplianceStatus(Enums.ComplianceStatus.PENDING);
  }
}