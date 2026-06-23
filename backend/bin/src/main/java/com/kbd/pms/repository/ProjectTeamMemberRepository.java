package com.kbd.pms.repository;

import com.kbd.pms.entity.Enums;
import com.kbd.pms.entity.ProjectTeamMemberEntity;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectTeamMemberRepository extends JpaRepository<ProjectTeamMemberEntity, Long> {

  void deleteByProjectId(Long projectId);

  @Query(
      """
      select (count(m) > 0) from ProjectTeamMemberEntity m
      where m.projectId = :projectId
        and m.userId = :userId
        and m.teamRole = :role
        and m.effectiveFrom <= :onDate
        and (m.effectiveTo is null or m.effectiveTo >= :onDate)
      """)
  boolean isActiveMemberWithRole(
      @Param("projectId") Long projectId,
      @Param("userId") Long userId,
      @Param("role") Enums.ProjectTeamRole role,
      @Param("onDate") LocalDate onDate);
}

