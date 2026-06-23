package com.kbd.pms.repository;

import com.kbd.pms.entity.ProjectDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectDocumentRepository extends JpaRepository<ProjectDocumentEntity, Long> {

  boolean existsByProjectIdAndMilestoneIdAndDocType(Long projectId, Long milestoneId, String docType);

  void deleteByProjectId(Long projectId);
}

