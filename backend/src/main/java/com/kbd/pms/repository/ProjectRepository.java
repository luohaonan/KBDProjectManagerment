package com.kbd.pms.repository;

import com.kbd.pms.entity.ProjectEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {

  Optional<ProjectEntity> findByProjectCode(String projectCode);

  /**
   * 按项目分级代号查询（如 H-L、G-L），关联 {@code project_level.level_code}。
   */
  @Query(
      """
      select p from ProjectEntity p, ProjectLevelEntity l
      where p.levelId = l.id and l.levelCode = :levelCode
      """)
  List<ProjectEntity> findAllByLevelCode(@Param("levelCode") String levelCode);

  @Query(
      value =
          """
          select max(cast(substring(project_no, 4) as unsigned))
          from project
          where project_no regexp '^KBD[0-9]+$'
          """,
      nativeQuery = true)
  Long findMaxKbdNumericSuffix();
}
