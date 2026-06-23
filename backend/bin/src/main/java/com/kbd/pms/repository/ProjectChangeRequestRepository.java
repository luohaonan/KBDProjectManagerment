package com.kbd.pms.repository;

import com.kbd.pms.entity.ProjectChangeRequestEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectChangeRequestRepository extends JpaRepository<ProjectChangeRequestEntity, Long> {
  List<ProjectChangeRequestEntity> findByProjectIdOrderByRequestedAtDesc(Long projectId);
  Optional<ProjectChangeRequestEntity> findByIdAndProjectId(Long id, Long projectId);

  void deleteByProjectId(Long projectId);
}
