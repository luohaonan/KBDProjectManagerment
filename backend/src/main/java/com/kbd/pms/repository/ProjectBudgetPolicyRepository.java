package com.kbd.pms.repository;

import com.kbd.pms.entity.ProjectBudgetPolicyEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectBudgetPolicyRepository extends JpaRepository<ProjectBudgetPolicyEntity, Long> {

  Optional<ProjectBudgetPolicyEntity> findByProjectId(Long projectId);

  void deleteByProjectId(Long projectId);
}
