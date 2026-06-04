package com.kbd.pms.repository;

import com.kbd.pms.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = {"roles"})
    List<User> findAll();

    @EntityGraph(attributePaths = {"roles"})
    List<User> findByDepartmentId(Long departmentId);
    
    boolean existsByUsername(String username);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    @Query("""
        SELECT DISTINCT u FROM User u
        JOIN u.roles r
        JOIN r.permissions p
        WHERE p.name = :permissionName AND u.isActive = true
        ORDER BY u.id ASC
        """)
    List<User> findActiveUsersByPermissionName(@Param("permissionName") String permissionName);
}