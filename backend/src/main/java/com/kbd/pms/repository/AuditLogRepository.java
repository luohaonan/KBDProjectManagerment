package com.kbd.pms.repository;

import com.kbd.pms.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

  List<AuditLogEntity> findByDocumentIdOrderByTimestampDesc(Long documentId);
}