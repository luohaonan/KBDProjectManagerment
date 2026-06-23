package com.kbd.pms.repository;

import com.kbd.pms.entity.ProjectBudgetSnapshotEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectBudgetSnapshotRepository extends JpaRepository<ProjectBudgetSnapshotEntity, Long> {

  Optional<ProjectBudgetSnapshotEntity> findFirstByProjectIdOrderBySnapshotMonthDesc(Long projectId);

  void deleteByProjectId(Long projectId);
}
