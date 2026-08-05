package com.kbd.pms.repository;

import com.kbd.pms.entity.MilestoneDeliverableDefEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MilestoneDeliverableDefRepository extends JpaRepository<MilestoneDeliverableDefEntity, Long> {

  List<MilestoneDeliverableDefEntity> findByMilestoneCodeAndIsActiveTrueOrderBySortNoAsc(String milestoneCode);

  List<MilestoneDeliverableDefEntity> findByMilestoneCodeOrderBySortNoAsc(String milestoneCode);

  Optional<MilestoneDeliverableDefEntity> findByMilestoneCodeAndSlotCode(String milestoneCode, String slotCode);
}