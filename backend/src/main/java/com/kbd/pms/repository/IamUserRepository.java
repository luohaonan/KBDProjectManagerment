package com.kbd.pms.repository;

import com.kbd.pms.entity.IamUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IamUserRepository extends JpaRepository<IamUserEntity, Long> {

  List<IamUserEntity> findByDeptId(Long deptId);

  List<IamUserEntity> findByDeptIdAndIsActiveTrue(Long deptId);
}
