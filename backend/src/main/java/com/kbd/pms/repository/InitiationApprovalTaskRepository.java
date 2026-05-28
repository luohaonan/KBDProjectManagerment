package com.kbd.pms.repository;

import com.kbd.pms.entity.InitiationApprovalTaskEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InitiationApprovalTaskRepository extends JpaRepository<InitiationApprovalTaskEntity, Long> {

  List<InitiationApprovalTaskEntity> findByInitiationApprovalIdOrderBySortOrderAsc(Long initiationApprovalId);

  List<InitiationApprovalTaskEntity> findByApproverUserIdOrderByCreatedAtDesc(Long approverUserId);
}
