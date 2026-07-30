package com.kbd.pms.repository;

import com.kbd.pms.entity.ProjectBudgetLedgerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectBudgetLedgerRepository extends JpaRepository<ProjectBudgetLedgerEntity, Long> {
    List<ProjectBudgetLedgerEntity> findByProjectId(Long projectId);

    List<ProjectBudgetLedgerEntity> findByProjectIdOrderByOccurredOnDescIdDesc(Long projectId);

    void deleteByProjectId(Long projectId);
}
