package com.kbd.pms.dto;

import com.kbd.pms.entity.OrgDepartmentEntity;
import com.kbd.pms.entity.User;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UserDto {

    private Long id;
    private String username;
    private String email;
    private Boolean isActive;
    private List<String> roles;
    private List<Long> departmentIds;
    private List<String> departmentNames;
    private Instant createdAt;
    private Instant updatedAt;

    public static UserDto fromEntity(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setIsActive(user.getIsActive());
        dto.setRoles(user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toList()));
        Set<OrgDepartmentEntity> depts = user.getDepartments();
        if (depts != null && !depts.isEmpty()) {
            dto.setDepartmentIds(depts.stream().map(OrgDepartmentEntity::getId).collect(Collectors.toList()));
            dto.setDepartmentNames(depts.stream().map(OrgDepartmentEntity::getDeptName).collect(Collectors.toList()));
        } else {
            dto.setDepartmentIds(Collections.emptyList());
            dto.setDepartmentNames(Collections.emptyList());
        }
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public List<Long> getDepartmentIds() { return departmentIds; }
    public void setDepartmentIds(List<Long> departmentIds) { this.departmentIds = departmentIds; }
    public List<String> getDepartmentNames() { return departmentNames; }
    public void setDepartmentNames(List<String> departmentNames) { this.departmentNames = departmentNames; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
