package com.kbd.pms.repository;

import com.kbd.pms.entity.MilestoneDefEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilestoneDefRepository extends JpaRepository<MilestoneDefEntity, Long> {

  Optional<MilestoneDefEntity> findByMilestoneCode(String milestoneCode);

  List<MilestoneDefEntity> findAllByIsActiveTrueOrderBySortNoAsc();
}
