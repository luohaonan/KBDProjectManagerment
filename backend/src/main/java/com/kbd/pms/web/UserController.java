package com.kbd.pms.web;

import com.kbd.pms.dto.UserDto;
import com.kbd.pms.entity.OrgDepartmentEntity;
import com.kbd.pms.entity.Permission;
import com.kbd.pms.entity.Role;
import com.kbd.pms.entity.User;
import com.kbd.pms.repository.OrgDepartmentRepository;
import com.kbd.pms.repository.PermissionRepository;
import com.kbd.pms.repository.RoleRepository;
import com.kbd.pms.repository.UserRepository;
import com.kbd.pms.exception.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private OrgDepartmentRepository orgDepartmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 获取所有用户列表（系统管理员）
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<Result<List<UserDto>>> listUsers() {
        List<User> users = userRepository.findAll();
        List<UserDto> dtos = users.stream().map(UserDto::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(Result.ok(dtos));
    }

    /**
     * 新建账号（系统管理员）
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Result<UserDto>> createUser(@RequestBody CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ApiException(400, "用户名已存在");
        }

        User user = new User(request.getUsername(), passwordEncoder.encode(request.getPassword()), request.getEmail());
        user.setIsActive(true);

        // 分配角色
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (String roleName : request.getRoles()) {
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new ApiException(400, "角色不存在: " + roleName));
                roles.add(role);
            }
            user.setRoles(roles);
        }

        // 设置部门
        if (request.getDepartmentId() != null) {
            OrgDepartmentEntity dept = orgDepartmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ApiException(400, "部门不存在"));
            user.setDepartment(dept);
        }

        user = userRepository.save(user);
        return ResponseEntity.ok(Result.ok(UserDto.fromEntity(user)));
    }

    /**
     * 修改用户密码（系统管理员可修改任何用户，普通用户只能修改自己的）
     */
    @PutMapping("/{userId}/password")
    @Transactional
    public ResponseEntity<Result<Void>> updatePassword(@PathVariable Long userId,
                                                       @RequestBody UpdatePasswordRequest request,
                                                       Authentication authentication) {
        User currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ApiException(401, "用户未登录"));

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_ADMIN"));

        // 非管理员只能修改自己的密码
        if (!isAdmin && !currentUser.getId().equals(userId)) {
            throw new ApiException(403, "无权修改其他用户的密码");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "用户不存在"));

        targetUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        targetUser.setUpdatedAt(Instant.now());
        userRepository.save(targetUser);

        return ResponseEntity.ok(Result.ok(null));
    }

    /**
     * 修改用户角色（系统管理员）
     */
    @PutMapping("/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Result<UserDto>> updateUserRoles(@PathVariable Long userId,
                                                           @RequestBody UpdateRolesRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "用户不存在"));

        Set<Role> roles = new HashSet<>();
        for (String roleName : request.getRoles()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ApiException(400, "角色不存在: " + roleName));
            roles.add(role);
        }
        user.setRoles(roles);
        user.setUpdatedAt(Instant.now());
        user = userRepository.save(user);

        return ResponseEntity.ok(Result.ok(UserDto.fromEntity(user)));
    }

    /**
     * 获取所有角色列表
     */
    @GetMapping("/roles")
    @Transactional(readOnly = true)
    public ResponseEntity<Result<List<RoleDto>>> listRoles() {
        List<Role> roles = roleRepository.findAll();
        List<RoleDto> dtos = roles.stream().map(role -> {
            RoleDto dto = new RoleDto();
            dto.setId(role.getId());
            dto.setName(role.getName());
            dto.setDescription(role.getDescription());
            dto.setPermissions(role.getPermissions().stream()
                    .map(p -> {
                        PermissionDto pd = new PermissionDto();
                        pd.setId(p.getId());
                        pd.setName(p.getName());
                        pd.setDescription(p.getDescription());
                        return pd;
                    })
                    .collect(Collectors.toList()));
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Result.ok(dtos));
    }

    /**
     * 获取所有权限列表
     */
    @GetMapping("/permissions")
    public ResponseEntity<Result<List<PermissionDto>>> listPermissions() {
        List<Permission> permissions = permissionRepository.findAll();
        List<PermissionDto> dtos = permissions.stream().map(p -> {
            PermissionDto dto = new PermissionDto();
            dto.setId(p.getId());
            dto.setName(p.getName());
            dto.setDescription(p.getDescription());
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Result.ok(dtos));
    }

    /**
     * 更新角色的权限（系统管理员）
     */
    @PutMapping("/roles/{roleId}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Result<RoleDto>> updateRolePermissions(@PathVariable Long roleId,
                                                                  @RequestBody UpdatePermissionsRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ApiException(404, "角色不存在"));

        Set<Permission> permissions = new HashSet<>();
        for (String permName : request.getPermissions()) {
            Permission perm = permissionRepository.findByName(permName)
                    .orElseThrow(() -> new ApiException(400, "权限不存在: " + permName));
            permissions.add(perm);
        }
        role.setPermissions(permissions);
        role.setUpdatedAt(Instant.now());
        role = roleRepository.save(role);

        RoleDto dto = new RoleDto();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        dto.setPermissions(role.getPermissions().stream()
                .map(p -> {
                    PermissionDto pd = new PermissionDto();
                    pd.setId(p.getId());
                    pd.setName(p.getName());
                    pd.setDescription(p.getDescription());
                    return pd;
                })
                .collect(Collectors.toList()));

        return ResponseEntity.ok(Result.ok(dto));
    }

    /**
     * 获取当前登录用户的信息（含权限）
     */
    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<Result<CurrentUserInfo>> getCurrentUser(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ApiException(404, "用户不存在"));

        CurrentUserInfo info = new CurrentUserInfo();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setEmail(user.getEmail());
        info.setRoles(user.getRoles().stream().map(Role::getName).collect(Collectors.toList()));
        info.setPermissions(user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .distinct()
                .collect(Collectors.toList()));
        if (user.getDepartment() != null) {
            info.setDepartmentId(user.getDepartment().getId());
            info.setDepartmentName(user.getDepartment().getDeptName());
        }

        return ResponseEntity.ok(Result.ok(info));
    }

    // ===== Request/Response DTOs =====

    public static class CreateUserRequest {
        private String username;
        private String password;
        private String email;
        private List<String> roles;
        private Long departmentId;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    }

    public static class UpdatePasswordRequest {
        private String newPassword;

        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class UpdateRolesRequest {
        private List<String> roles;

        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
    }

    public static class UpdatePermissionsRequest {
        private List<String> permissions;

        public List<String> getPermissions() { return permissions; }
        public void setPermissions(List<String> permissions) { this.permissions = permissions; }
    }

    public static class RoleDto {
        private Long id;
        private String name;
        private String description;
        private List<PermissionDto> permissions;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<PermissionDto> getPermissions() { return permissions; }
        public void setPermissions(List<PermissionDto> permissions) { this.permissions = permissions; }
    }

    public static class PermissionDto {
        private Long id;
        private String name;
        private String description;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class CurrentUserInfo {
        private Long id;
        private String username;
        private String email;
        private List<String> roles;
        private List<String> permissions;
        private Long departmentId;
        private String departmentName;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
        public List<String> getPermissions() { return permissions; }
        public void setPermissions(List<String> permissions) { this.permissions = permissions; }
        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    }
}
