package com.kbd.pms.service;

import com.kbd.pms.dto.DepartmentDto;
import com.kbd.pms.dto.UserDto;
import com.kbd.pms.entity.OrgDepartmentEntity;
import com.kbd.pms.entity.User;
import com.kbd.pms.exception.ApiException;
import com.kbd.pms.repository.OrgDepartmentRepository;
import com.kbd.pms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
public class DepartmentService {

    private final OrgDepartmentRepository deptRepository;
    private final UserRepository userRepository;

    public DepartmentService(OrgDepartmentRepository deptRepository, UserRepository userRepository) {
        this.deptRepository = deptRepository;
        this.userRepository = userRepository;
    }

    /**
     * 获取所有部门列表（含成员数和负责人信息）
     */
    @Transactional(readOnly = true)
    public List<DepartmentDto> listDepartments() {
        List<OrgDepartmentEntity> departments = deptRepository.findAll();
        return departments.stream().map(dept -> {
            DepartmentDto dto = DepartmentDto.fromEntity(dept);
            // 统计部门成员数
            List<User> members = userRepository.findByDepartmentId(dept.getId());
            dto.setMemberCount(members.size());
            // 设置负责人信息
            if (dept.getHeadUserId() != null) {
                dto.setHeadUserId(dept.getHeadUserId());
                // 查找负责人用户名
                userRepository.findById(dept.getHeadUserId()).ifPresent(headUser -> {
                    dto.setHeadUserName(headUser.getUsername());
                });
            }
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 获取部门详情
     */
    @Transactional(readOnly = true)
    public DepartmentDto getDepartment(Long deptId) {
        OrgDepartmentEntity dept = deptRepository.findById(deptId)
                .orElseThrow(() -> new ApiException(404, "部门不存在"));
        DepartmentDto dto = DepartmentDto.fromEntity(dept);
        List<User> members = userRepository.findByDepartmentId(dept.getId());
        dto.setMemberCount(members.size());
        if (dept.getHeadUserId() != null) {
            dto.setHeadUserId(dept.getHeadUserId());
            userRepository.findById(dept.getHeadUserId()).ifPresent(headUser -> {
                dto.setHeadUserName(headUser.getUsername());
            });
        }
        return dto;
    }

    /**
     * 获取部门成员列表
     */
    @Transactional(readOnly = true)
    public List<UserDto> getDepartmentMembers(Long deptId) {
        if (!deptRepository.existsById(deptId)) {
            throw new ApiException(404, "部门不存在");
        }
        List<User> members = userRepository.findByDepartmentId(deptId);
        return members.stream().map(UserDto::fromEntity).collect(Collectors.toList());
    }

    /**
     * 指派部门负责人
     */
    @Transactional
    public void assignHead(Long deptId, Long userId) {
        OrgDepartmentEntity dept = deptRepository.findById(deptId)
                .orElseThrow(() -> new ApiException(404, "部门不存在"));

        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(404, "用户不存在"));
            // 确保用户属于该部门（检查多对多关联）
            boolean belongsToDept = user.getDepartments() != null && 
                user.getDepartments().stream().anyMatch(d -> d.getId().equals(deptId));
            if (!belongsToDept) {
                throw new ApiException(400, "该用户不属于此部门，不能设为部门负责人");
            }
            dept.setHeadUserId(userId);
        } else {
            dept.setHeadUserId(null);
        }
        dept.setUpdatedAt(Instant.now());
        deptRepository.save(dept);
    }

    /**
     * 添加部门成员（将用户分配到部门，支持多部门）
     */
    @Transactional
    public void addMember(Long deptId, Long userId) {
        if (!deptRepository.existsById(deptId)) {
            throw new ApiException(404, "部门不存在");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "用户不存在"));
        OrgDepartmentEntity dept = deptRepository.findById(deptId)
                .orElseThrow(() -> new ApiException(404, "部门不存在"));

        // 检查是否已属于该部门
        if (user.getDepartments() != null && 
            user.getDepartments().stream().anyMatch(d -> d.getId().equals(deptId))) {
            throw new ApiException(400, "该用户已属于此部门");
        }

        if (user.getDepartments() == null) {
            user.setDepartments(new java.util.HashSet<>());
        }
        user.getDepartments().add(dept);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    /**
     * 移除部门成员（只从该部门移除，如用户没有其他部门则保留空集合）
     */
    @Transactional
    public void removeMember(Long deptId, Long userId) {
        if (!deptRepository.existsById(deptId)) {
            throw new ApiException(404, "部门不存在");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "用户不存在"));
        // 如果该用户是部门负责人，取消负责人身份
        OrgDepartmentEntity dept = deptRepository.findById(deptId)
                .orElseThrow(() -> new ApiException(404, "部门不存在"));
        if (dept.getHeadUserId() != null && dept.getHeadUserId().equals(userId)) {
            dept.setHeadUserId(null);
            dept.setUpdatedAt(Instant.now());
            deptRepository.save(dept);
        }
        if (user.getDepartments() != null) {
            user.getDepartments().removeIf(d -> d.getId().equals(deptId));
        }
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }
}
