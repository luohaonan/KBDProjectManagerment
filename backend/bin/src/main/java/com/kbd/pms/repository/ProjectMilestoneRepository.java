package com.kbd.pms.repository;

import com.kbd.pms.entity.ProjectMilestoneEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestoneEntity, Long> {

  List<ProjectMilestoneEntity> findByProjectIdOrderByIdAsc(Long projectId);

  ProjectMilestoneEntity findByProjectIdAndMilestoneId(Long projectId, Long milestoneId);

  void deleteByProjectId(Long projectId);

  @Query(
      """
      select m from ProjectMilestoneEntity m
      where m.status = com.kbd.pms.entity.Enums.ProjectMilestoneStatus.CONDITIONAL_APPROVED
        and m.conditionalDeadline is not null
        and m.conditionalDeadline < :now
      """)
  List<ProjectMilestoneEntity> findExpiredConditionalApproved(@Param("now") LocalDateTime now);
}
