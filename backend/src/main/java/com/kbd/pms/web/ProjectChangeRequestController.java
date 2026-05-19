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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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

  @PostMapping("/change-requests")
  @ResponseStatus(HttpStatus.CREATED)
  public Result<ProjectChangeRequestDto.ProjectChangeRequestResponse> createChangeRequest(
      @PathVariable("projectId") long projectId,
      @Valid @RequestBody ProjectChangeRequestDto.ProjectChangeRequestCreateRequest request) {
    return Result.ok(changeRequestService.createChangeRequest(projectId, request));
  }

  @GetMapping("/change-requests")
  public Result<List<ProjectChangeRequestDto.ProjectChangeRequestResponse>> listChangeRequests(
      @PathVariable("projectId") long projectId) {
    return Result.ok(changeRequestService.listProjectChangeRequests(projectId));
  }

  @PostMapping("/change-requests/{changeRequestId}/approve")
  public Result<ProjectChangeRequestDto.ProjectChangeRequestResponse> approveChangeRequest(
      @PathVariable("projectId") long projectId,
      @PathVariable("changeRequestId") long changeRequestId,
      @Valid @RequestBody ProjectChangeRequestDecisionRequest request,
      Authentication authentication) {
    return Result.ok(changeRequestService.approveChangeRequest(projectId, changeRequestId, request, authentication.getName()));
  }

  @PostMapping("/change-requests/{changeRequestId}/reject")
  public Result<ProjectChangeRequestDto.ProjectChangeRequestResponse> rejectChangeRequest(
      @PathVariable("projectId") long projectId,
      @PathVariable("changeRequestId") long changeRequestId,
      @Valid @RequestBody ProjectChangeRequestDecisionRequest request,
      Authentication authentication) {
    return Result.ok(changeRequestService.rejectChangeRequest(projectId, changeRequestId, request, authentication.getName()));
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

  @PostMapping("/terminate")
  public Result<ProjectChangeRequestDto.ProjectChangeRequestResponse> terminateProject(
      @PathVariable("projectId") long projectId,
      @Valid @RequestBody ProjectChangeRequestDto.ProjectChangeRequestCreateRequest request,
      Authentication authentication) {
    return Result.ok(changeRequestService.terminateProject(projectId, request, authentication.getName()));
  }
}
