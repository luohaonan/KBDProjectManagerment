package com.kbd.pms.web;

import com.kbd.pms.dto.InitiationApprovalDto;
import com.kbd.pms.dto.InitiationApprovalTaskDto;
import com.kbd.pms.dto.InitiationDecisionRequest;
import com.kbd.pms.dto.InitiationSubmitRequest;
import com.kbd.pms.service.InitiationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/initiations")
public class InitiationController {

  private final InitiationService initiationService;

  public InitiationController(InitiationService initiationService) {
    this.initiationService = initiationService;
  }

  /**
   * 项目经理提交立项申请
   */
  @PostMapping("/{projectId}/submit")
  public Result<InitiationApprovalDto> submitInitiation(
      @PathVariable long projectId,
      @RequestBody InitiationSubmitRequest request) {
    InitiationApprovalDto dto = initiationService.submitInitiation(projectId, request);
    return Result.ok(dto);
  }

  /**
   * PMC成员执行审批决策
   */
  @PostMapping("/{projectId}/tasks/{taskId}/decision")
  public Result<InitiationApprovalDto> executeDecision(
      @PathVariable long projectId,
      @PathVariable long taskId,
      @RequestBody InitiationDecisionRequest request) {
    InitiationApprovalDto dto = initiationService.executeDecision(projectId, taskId, request);
    return Result.ok(dto);
  }

  /**
   * 获取项目的立项申请审批状态
   */
  @GetMapping("/{projectId}")
  public Result<InitiationApprovalDto> getInitiationStatus(@PathVariable long projectId) {
    InitiationApprovalDto dto = initiationService.getInitiationStatus(projectId);
    return Result.ok(dto);
  }

  /**
   * 获取当前用户的待审批任务
   */
  @GetMapping("/my-tasks")
  public Result<List<InitiationApprovalTaskDto>> getMyTasks(@RequestParam long userId) {
    List<InitiationApprovalTaskDto> tasks = initiationService.getMyApprovalTasks(userId);
    return Result.ok(tasks);
  }
}
