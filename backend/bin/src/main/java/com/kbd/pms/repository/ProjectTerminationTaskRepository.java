package com.kbd.pms.repository;

import com.kbd.pms.entity.ProjectTerminationTaskEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectTerminationTaskRepository extends JpaRepository<ProjectTerminationTaskEntity, Long> {
  List<ProjectTerminationTaskEntity> findByProjectIdOrderByDueDateAsc(Long projectId);

  void deleteByProjectId(Long projectId);
}
