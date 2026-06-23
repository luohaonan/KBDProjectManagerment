package com.kbd.pms.repository;

import com.kbd.pms.entity.ProjectBudgetPlanEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectBudgetPlanRepository extends JpaRepository<ProjectBudgetPlanEntity, Long> {
  Optional<ProjectBudgetPlanEntity> findTopByProjectIdOrderByVersionNoDesc(Long projectId);
  Optional<ProjectBudgetPlanEntity> findByProjectIdAndVersionNo(Long projectId, Integer versionNo);

  void deleteByProjectId(Long projectId);
}
