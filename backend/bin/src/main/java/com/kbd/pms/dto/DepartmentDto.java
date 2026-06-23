package com.kbd.pms.dto;

import com.kbd.pms.entity.OrgDepartmentEntity;
import java.time.Instant;

public class DepartmentDto {

    private Long id;
    private String deptCode;
    private String deptName;
    private String deptType;
    private Long parentId;
    private Boolean isActive;
    private Long headUserId;
    private String headUserName;
    private int memberCount;
    private Instant createdAt;
    private Instant updatedAt;

    public static DepartmentDto fromEntity(OrgDepartmentEntity entity) {
        DepartmentDto dto = new DepartmentDto();
        dto.setId(entity.getId());
        dto.setDeptCode(entity.getDeptCode());
        dto.setDeptName(entity.getDeptName());
        dto.setDeptType(entity.getDeptType() != null ? entity.getDeptType().name() : null);
        dto.setParentId(entity.getParentId());
        dto.setIsActive(entity.getIsActive());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDeptCode() { return deptCode; }
    public void setDeptCode(String deptCode) { this.deptCode = deptCode; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public String getDeptType() { return deptType; }
    public void setDeptType(String deptType) { this.deptType = deptType; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public Long getHeadUserId() { return headUserId; }
    public void setHeadUserId(Long headUserId) { this.headUserId = headUserId; }
    public String getHeadUserName() { return headUserName; }
    public void setHeadUserName(String headUserName) { this.headUserName = headUserName; }
    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
