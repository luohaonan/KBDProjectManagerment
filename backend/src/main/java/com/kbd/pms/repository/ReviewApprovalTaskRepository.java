package com.kbd.pms.repository;

import com.kbd.pms.entity.ReviewApprovalTaskEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewApprovalTaskRepository extends JpaRepository<ReviewApprovalTaskEntity, Long> {

  List<ReviewApprovalTaskEntity> findByReviewApprovalIdOrderBySortOrderAsc(Long reviewApprovalId);

  List<ReviewApprovalTaskEntity> findByReviewApprovalIdAndStepCode(Long reviewApprovalId, String stepCode);

  List<ReviewApprovalTaskEntity> findByReviewApprovalIdAndStepCodeAndStatus(Long reviewApprovalId, String stepCode, ReviewApprovalTaskEntity.Status status);

  List<ReviewApprovalTaskEntity> findByApproverUserIdOrderByCreatedAtDesc(Long approverUserId);

  List<ReviewApprovalTaskEntity> findByApproverUserIdAndStatusOrderByCreatedAtDesc(Long approverUserId, ReviewApprovalTaskEntity.Status status);
}
