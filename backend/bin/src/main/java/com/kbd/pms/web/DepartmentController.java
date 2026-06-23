package com.kbd.pms.web;

import com.kbd.pms.dto.DepartmentDto;
import com.kbd.pms.dto.UserDto;
import com.kbd.pms.service.DepartmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    /**
     * 获取所有部门列表
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<List<DepartmentDto>>> listDepartments() {
        List<DepartmentDto> departments = departmentService.listDepartments();
        return ResponseEntity.ok(Result.ok(departments));
    }

    /**
     * 获取部门详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<DepartmentDto>> getDepartment(@PathVariable Long id) {
        DepartmentDto dept = departmentService.getDepartment(id);
        return ResponseEntity.ok(Result.ok(dept));
    }

    /**
     * 获取部门成员列表
     */
    @GetMapping("/{id}/members")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<List<UserDto>>> getDepartmentMembers(@PathVariable Long id) {
        List<UserDto> members = departmentService.getDepartmentMembers(id);
        return ResponseEntity.ok(Result.ok(members));
    }

    /**
     * 指派部门负责人
     * 请求体: { "userId": 123 } 或 { "userId": null } 取消负责人
     */
    @PutMapping("/{id}/head")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<Void>> assignHead(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Long userId = body.get("userId");
        departmentService.assignHead(id, userId);
        return ResponseEntity.ok(Result.ok(null));
    }

    /**
     * 添加部门成员
     * 请求体: { "userId": 123 }
     */
    @PutMapping("/{id}/members")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<Void>> addMember(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Long userId = body.get("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body(Result.fail(400, "userId不能为空"));
        }
        departmentService.addMember(id, userId);
        return ResponseEntity.ok(Result.ok(null));
    }

    /**
     * 移除部门成员
     */
    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<Void>> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        departmentService.removeMember(id, userId);
        return ResponseEntity.ok(Result.ok(null));
    }
}
