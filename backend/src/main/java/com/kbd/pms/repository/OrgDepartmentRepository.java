package com.kbd.pms.repository;

import com.kbd.pms.entity.OrgDepartmentEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgDepartmentRepository extends JpaRepository<OrgDepartmentEntity, Long> {

  Optional<OrgDepartmentEntity> findByDeptCode(String deptCode);
}
