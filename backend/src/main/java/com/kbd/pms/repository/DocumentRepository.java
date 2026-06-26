package com.kbd.pms.repository;

import com.kbd.pms.entity.DocumentEntity;
import com.kbd.pms.entity.Enums;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

  List<DocumentEntity> findByProjectIdAndMilestonePhase(Long projectId, Enums.MilestoneStage milestonePhase);

  List<DocumentEntity> findByProjectIdAndMilestonePhaseAndDeliverableSlotCode(Long projectId, Enums.MilestoneStage milestonePhase, String deliverableSlotCode);

  List<DocumentEntity> findByProjectIdAndUploader(Long projectId, Long uploader);

  List<DocumentEntity> findByProjectId(Long projectId);

  @Modifying
  @Query("UPDATE DocumentEntity d SET d.isLocked = true WHERE d.projectId = :projectId AND d.milestonePhase = :milestonePhase")
  void lockDocumentsByProjectAndPhase(@Param("projectId") Long projectId, @Param("milestonePhase") Enums.MilestoneStage milestonePhase);

  List<DocumentEntity> findByComplianceStatus(Enums.ComplianceStatus status);

  void deleteByProjectId(Long projectId);
}