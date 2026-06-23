package com.kbd.pms.repository;

import com.kbd.pms.entity.ProjectLevelEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectLevelRepository extends JpaRepository<ProjectLevelEntity, Long> {

  Optional<ProjectLevelEntity> findByLevelCode(String levelCode);
}
