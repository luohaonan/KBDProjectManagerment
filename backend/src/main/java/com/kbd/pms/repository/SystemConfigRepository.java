package com.kbd.pms.repository;

import com.kbd.pms.entity.SystemConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemConfigRepository extends JpaRepository<SystemConfigEntity, String> {
}