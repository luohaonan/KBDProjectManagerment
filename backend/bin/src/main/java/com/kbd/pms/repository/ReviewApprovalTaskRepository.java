package com.kbd.pms.repository;

import com.kbd.pms.entity.ReviewApprovalTaskEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewApprovalTaskRepository extends JpaRepository<ReviewApprovalTaskEntity, Long> {

  List<ReviewApprovalTaskEntity> findByReviewApprovalIdOrderBySortOrderAsc(Long reviewApprovalId);

  List<ReviewApprovalTaskEntity> findByApproverUserIdOrderByCreatedAtDesc(Long approverUserId);
}
