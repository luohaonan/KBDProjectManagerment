package com.kbd.pms.repository;

import com.kbd.pms.entity.MilestoneDeptRoleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilestoneDeptRoleRepository extends JpaRepository<MilestoneDeptRoleEntity, Long> {

  List<MilestoneDeptRoleEntity> findByMilestoneDefIdAndIsActiveTrue(Long milestoneDefId);

  List<MilestoneDeptRoleEntity> findByMilestoneDefIdAndRoleTypeAndIsActiveTrue(
      Long milestoneDefId, String roleType);

  List<MilestoneDeptRoleEntity> findByMilestoneDefIdAndDeptIdAndIsActiveTrue(
      Long milestoneDefId, Long deptId);
}