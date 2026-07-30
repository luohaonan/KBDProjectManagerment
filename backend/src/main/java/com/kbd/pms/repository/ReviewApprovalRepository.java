package com.kbd.pms.repository;

import com.kbd.pms.entity.ReviewApprovalEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewApprovalRepository extends JpaRepository<ReviewApprovalEntity, Long> {

  List<ReviewApprovalEntity> findByProjectIdOrderByCreatedAtDesc(Long projectId);

  List<ReviewApprovalEntity> findByProjectIdAndStatusIn(Long projectId, List<ReviewApprovalEntity.Status> statuses);

  Optional<ReviewApprovalEntity> findTopByProjectIdAndProjectMilestoneIdOrderByCreatedAtDesc(
      Long projectId, Long projectMilestoneId);

  List<ReviewApprovalEntity> findBySubmitterUserIdOrderByCreatedAtDesc(Long submitterUserId);

  void deleteByProjectId(Long projectId);
}
