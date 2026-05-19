package com.kbd.pms.service;

import com.kbd.pms.entity.AuditLogEntity;
import com.kbd.pms.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AuditLogService {

  private final AuditLogRepository auditLogRepository;

  public AuditLogService(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  public void logAction(Long userId, String action, Long documentId, String details) {
    AuditLogEntity log = new AuditLogEntity();
    log.setUserId(userId);
    log.setAction(action);
    log.setDocumentId(documentId);
    log.setTimestamp(Instant.now());
    log.setDetails(details);
    auditLogRepository.save(log);
  }

  public List<AuditLogEntity> getAuditLogsForDocument(Long documentId) {
    return auditLogRepository.findByDocumentIdOrderByTimestampDesc(documentId);
  }
}