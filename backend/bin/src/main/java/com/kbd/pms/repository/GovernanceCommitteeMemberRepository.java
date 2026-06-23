package com.kbd.pms.repository;

import com.kbd.pms.entity.GovernanceCommitteeMemberEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GovernanceCommitteeMemberRepository
    extends JpaRepository<GovernanceCommitteeMemberEntity, Long> {

  @Query(
      """
      select (count(m) > 0) from GovernanceCommitteeMemberEntity m
      where m.committeeId = :committeeId
        and m.userId = :userId
        and m.effectiveFrom <= :onDate
        and (m.effectiveTo is null or m.effectiveTo >= :onDate)
      """)
  boolean isActiveCommitteeMember(
      @Param("committeeId") Long committeeId,
      @Param("userId") Long userId,
      @Param("onDate") LocalDate onDate);

  @Query(
      """
      select m.userId from GovernanceCommitteeMemberEntity m
      where m.committeeId = :committeeId
        and m.effectiveFrom <= :onDate
        and (m.effectiveTo is null or m.effectiveTo >= :onDate)
      order by m.id
      """)
  List<Long> findActiveMemberIds(
      @Param("committeeId") Long committeeId,
      @Param("onDate") LocalDate onDate);
}

