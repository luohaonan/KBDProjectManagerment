package com.kbd.pms.repository;

import com.kbd.pms.entity.MilestoneHistoryEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MilestoneHistoryRepository extends JpaRepository<MilestoneHistoryEntity, Long> {

  List<MilestoneHistoryEntity> findByProjectMilestoneIdOrderByActionAtAsc(Long projectMilestoneId);

  void deleteByProjectId(Long projectId);

  @Query(
      """
      select h from MilestoneHistoryEntity h
      where h.action = com.kbd.pms.entity.MilestoneHistoryEntity.Action.DECISION
        and h.actionAt >= :fromTime and h.actionAt <= :toTime
      """)
  List<MilestoneHistoryEntity> findDecisionsBetween(
      @Param("fromTime") LocalDateTime fromTime, @Param("toTime") LocalDateTime toTime);
}

