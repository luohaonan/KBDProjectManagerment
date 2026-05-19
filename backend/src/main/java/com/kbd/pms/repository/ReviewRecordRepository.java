package com.kbd.pms.repository;

import com.kbd.pms.entity.ReviewRecordEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRecordRepository extends JpaRepository<ReviewRecordEntity, Long> {

  List<ReviewRecordEntity> findByProjectIdOrderByActionAtDesc(Long projectId);

  List<ReviewRecordEntity> findByProjectIdAndProjectMilestoneIdOrderByActionAtDesc(
      Long projectId, Long projectMilestoneId);

  List<ReviewRecordEntity> findByActorUserIdOrderByActionAtDesc(Long actorUserId);

  void deleteByProjectId(Long projectId);
}
