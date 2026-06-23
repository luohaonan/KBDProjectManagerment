package com.kbd.pms.repository;

import com.kbd.pms.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

  List<AuditLogEntity> findByDocumentIdOrderByTimestampDesc(Long documentId);
}