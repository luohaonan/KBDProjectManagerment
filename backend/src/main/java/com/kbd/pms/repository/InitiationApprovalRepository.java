package com.kbd.pms.repository;

import com.kbd.pms.entity.InitiationApprovalEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InitiationApprovalRepository extends JpaRepository<InitiationApprovalEntity, Long> {

  List<InitiationApprovalEntity> findByProjectIdOrderByCreatedAtDesc(Long projectId);

  Optional<InitiationApprovalEntity> findTopByProjectIdOrderByCreatedAtDesc(Long projectId);

  List<InitiationApprovalEntity> findBySubmitterUserIdOrderByCreatedAtDesc(Long submitterUserId);
}
