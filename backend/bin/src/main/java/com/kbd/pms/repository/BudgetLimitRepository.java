package com.kbd.pms.repository;

import com.kbd.pms.entity.BudgetLimitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BudgetLimitRepository extends JpaRepository<BudgetLimitEntity, Long> {

    @Query("SELECT b FROM BudgetLimitEntity b WHERE b.projectId = :projectId AND b.milestoneCode = :milestoneCode")
    Optional<BudgetLimitEntity> findByProjectIdAndMilestoneCode(@Param("projectId") Long projectId, @Param("milestoneCode") String milestoneCode);

    void deleteByProjectId(Long projectId);
}