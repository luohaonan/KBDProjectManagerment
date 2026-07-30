package com.kbd.pms.repository;

import com.kbd.pms.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("null")
public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = {"roles", "departments"})
    Optional<User> findById(Long id);

    @EntityGraph(attributePaths = {"roles", "departments"})
    List<User> findAll();

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles r JOIN u.departments d WHERE d.id = :departmentId")
    List<User> findByDepartmentId(@Param("departmentId") Long departmentId);

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

    @EntityGraph(attributePaths = {"roles", "departments"})
    @Query("""
        SELECT DISTINCT u FROM User u
        JOIN u.roles r
        WHERE r.name = :roleName AND u.isActive = true
        ORDER BY u.id ASC
        """)
    List<User> findActiveUsersByRoleName(@Param("roleName") String roleName);
}