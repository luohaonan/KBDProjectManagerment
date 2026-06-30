package com.kbd.pms.web;

import com.kbd.pms.dto.BudgetChangeRequest;
import com.kbd.pms.dto.MilestoneRescheduleRequest;
import com.kbd.pms.dto.ProjectChangeRequestDecisionRequest;
import com.kbd.pms.dto.ProjectChangeRequestDto;
import com.kbd.pms.service.BudgetService;
import com.kbd.pms.service.ProjectChangeRequestService;
import com.kbd.pms.service.MilestoneService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class ProjectChangeRequestController {

  private final ProjectChangeRequestService changeRequestService;
  private final BudgetService budgetService;
  private final MilestoneService milestoneService;

  public ProjectChangeRequestController(
      ProjectChangeRequestService changeRequestService,
      BudgetService budgetService,
      MilestoneService milestoneService) {
    this.changeRequestService = changeRequestService;
    this.budgetService = budgetService;
    this.milestoneService = milestoneService;
  }

  /** PM发起项目变更申请 */
  @PostMapping("/change-requests")
  @ResponseStatus(HttpStatus.CREATED)
  public Result<ProjectChangeRequestDto> createChangeRequest(
      @PathVariable("projectId") long projectId,
      @Valid @RequestBody ProjectChangeRequestDto request) {
    return Result.ok(changeRequestService.submitChange(projectId, request));
  }

  /** 获取项目变更申请列表 */
  @GetMapping("/change-requests")
  public Result<List<ProjectChangeRequestDto>> listChangeRequests(
      @PathVariable("projectId") long projectId) {
    return Result.ok(changeRequestService.getProjectChangeRequests(projectId));
  }

  /** 获取单个变更申请 */
  @GetMapping("/change-requests/{changeRequestId}")
  public Result<ProjectChangeRequestDto> getChangeRequest(
      @PathVariable("projectId") long projectId,
      @PathVariable("changeRequestId") long changeRequestId) {
    return Result.ok(changeRequestService.getChangeRequest(changeRequestId));
  }

  /** 执行变更审批决策（效率管理部或PMC） */
  @PostMapping("/change-requests/{changeRequestId}/decision")
  public Result<ProjectChangeRequestDto> executeDecision(
      @PathVariable("projectId") long projectId,
      @PathVariable("changeRequestId") long changeRequestId,
      @Valid @RequestBody ProjectChangeRequestDecisionRequest request) {
    return Result.ok(changeRequestService.executeDecision(changeRequestId, request.actorUserId(), request));
  }

  @PatchMapping("/milestones/{milestoneId}/reschedule")
  public Result<Void> rescheduleMilestone(
      @PathVariable("projectId") long projectId,
      @PathVariable("milestoneId") long milestoneId,
      @Valid @RequestBody MilestoneRescheduleRequest request) {
    milestoneService.rescheduleMilestone(projectId, milestoneId, request);
    return Result.ok(null);
  }

  @PatchMapping("/budgets/{milestoneCode}")
  public Result<Void> updateBudget(
      @PathVariable("projectId") long projectId,
      @PathVariable("milestoneCode") String milestoneCode,
      @Valid @RequestBody BudgetChangeRequest request) {
    budgetService.applyBudgetChange(projectId, milestoneCode, request.requestedBudget(), request.actorUserId());
    return Result.ok(null);
  }

  /** PM发起项目终止（委托给ProjectTerminationController的逻辑，此处统一入口） */
  @PostMapping("/terminate")
  public Result<?> terminateProject(
      @PathVariable("projectId") long projectId,
      @RequestBody Map<String, Object> body) {
    long actorUserId = ((Number) body.get("actorUserId")).longValue();
    String reason = (String) body.get("reason");
    String attachmentUri = (String) body.get("attachmentUri");
    return Result.ok(changeRequestService.submitChange(projectId,
        new ProjectChangeRequestDto(null, projectId, null, "PAUSE_TERMINATE", reason, attachmentUri,
            null, null, null, null, null, actorUserId, null, null, null, null, null, null, null, null, null, null, null, null)));
  }
}