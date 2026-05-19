package com.kbd.pms.repository;

import com.kbd.pms.entity.IamUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IamUserRepository extends JpaRepository<IamUserEntity, Long> {}
