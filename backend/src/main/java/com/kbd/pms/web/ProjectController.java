package com.kbd.pms.web;

import com.kbd.pms.dto.InitiationReportResponse;
import com.kbd.pms.dto.ProjectCreateRequest;
import com.kbd.pms.dto.ProjectDetailResponse;
import com.kbd.pms.dto.ProjectUpdateRequest;
import com.kbd.pms.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

  private final ProjectService projectService;

  public ProjectController(ProjectService projectService) {
    this.projectService = projectService;
  }

  @GetMapping
  public Result<List<ProjectDetailResponse>> list(Authentication authentication) {
    String username = authentication.getName();
    return Result.ok(projectService.getVisibleProjects(username));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Result<ProjectDetailResponse> create(@Valid @RequestBody ProjectCreateRequest request, Authentication authentication) {
    String username = authentication.getName();
    return Result.ok(projectService.createProject(request, username));
  }

  @GetMapping("/{id}")
  public Result<ProjectDetailResponse> get(@PathVariable("id") long id, Authentication authentication) {
    String username = authentication.getName();
    return Result.ok(projectService.getProjectDetail(id, username));
  }

  @PutMapping("/{id}")
  public Result<ProjectDetailResponse> update(
      @PathVariable("id") long id,
      @Valid @RequestBody ProjectUpdateRequest request,
      Authentication authentication) {
    String username = authentication.getName();
    return Result.ok(projectService.updateProject(id, request, username));
  }

  /**
   * 获取立项报告数据（供前端生成 PDF）
   */
  @GetMapping("/{id}/initiation-report")
  public Result<InitiationReportResponse> getInitiationReport(@PathVariable("id") long id) {
    return Result.ok(projectService.getInitiationReport(id));
  }

  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable("id") long id, Authentication authentication) {
    String username = authentication.getName();
    projectService.deleteProject(id, username);
    return Result.ok(null);
  }

  @GetMapping("/export")
  public ResponseEntity<byte[]> exportProjects() {
    List<ProjectDetailResponse> projects = projectService.getVisibleProjects("system"); // For export, use system user

    // Simple CSV export
    StringBuilder csv = new StringBuilder();
    csv.append("项目代码,项目名称,分级,状态,当前阶段,预算执行率\n");

    for (ProjectDetailResponse p : projects) {
      csv.append(p.projectCode()).append(",")
         .append(p.projectName()).append(",")
         .append(p.levelCode()).append(",")
         .append(p.projectStatus()).append(",")
         .append(p.lifecyclePhaseLabel() != null ? p.lifecyclePhaseLabel() : "").append(",")
         .append(p.budgetExecution() != null && p.budgetExecution().utilizationRatio() != null
                 ? p.budgetExecution().utilizationRatio().toString() + "%" : "").append("\n");
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_PLAIN);
    headers.setContentDispositionFormData("attachment", "projects.csv");

    return ResponseEntity.ok()
        .headers(headers)
        .body(csv.toString().getBytes(StandardCharsets.UTF_8));
  }
}
