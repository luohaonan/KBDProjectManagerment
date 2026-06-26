package com.kbd.pms.repository;

import com.kbd.pms.entity.ProjectTerminationRequestEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectTerminationRequestRepository extends JpaRepository<ProjectTerminationRequestEntity, Long> {

  Optional<ProjectTerminationRequestEntity> findTopByProjectIdOrderByCreatedAtDesc(Long projectId);

  List<ProjectTerminationRequestEntity> findByProjectIdOrderByCreatedAtDesc(Long projectId);

  List<ProjectTerminationRequestEntity> findByStatusOrderByCreatedAtDesc(ProjectTerminationRequestEntity.Status status);
}