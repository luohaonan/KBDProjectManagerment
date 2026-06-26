package com.kbd.pms.web;

import com.kbd.pms.dto.TerminationRequestDto;
import com.kbd.pms.service.ProjectTerminationService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/terminations")
public class ProjectTerminationController {

  private final ProjectTerminationService terminationService;

  public ProjectTerminationController(ProjectTerminationService terminationService) {
    this.terminationService = terminationService;
  }

  /**
   * PM发起项目终止申请
   */
  @PostMapping
  @ResponseStatus(HttpStatus.OK)
  public Result<TerminationRequestDto> submitTermination(
      @PathVariable long projectId,
      @RequestBody Map<String, Object> body) {
    long actorUserId = ((Number) body.get("actorUserId")).longValue();
    String reason = (String) body.get("reason");
    String attachmentUri = (String) body.get("attachmentUri");
    return Result.ok(terminationService.submitTermination(projectId, actorUserId, reason, attachmentUri));
  }

  /**
   * 获取项目的终止申请列表
   */
  @GetMapping
  public Result<List<TerminationRequestDto>> getTerminations(
      @PathVariable long projectId) {
    return Result.ok(terminationService.getProjectTerminations(projectId));
  }

  /**
   * 获取单个终止申请详情
   */
  @GetMapping("/{terminationId}")
  public Result<TerminationRequestDto> getTermination(
      @PathVariable long projectId,
      @PathVariable long terminationId) {
    return Result.ok(terminationService.getTermination(terminationId));
  }

  /**
   * 执行终止审批决策（效率管理部或PMC）
   */
  @PostMapping("/{terminationId}/decision")
  @ResponseStatus(HttpStatus.OK)
  public Result<TerminationRequestDto> executeDecision(
      @PathVariable long projectId,
      @PathVariable long terminationId,
      @RequestBody Map<String, Object> body) {
    long approverId = ((Number) body.get("approverId")).longValue();
    String decision = (String) body.get("decision");
    String opinion = (String) body.get("opinion");
    return Result.ok(terminationService.executePmcDecision(terminationId, approverId, decision, opinion));
  }

  /**
   * PM完成终止任务（上传总结报告、确认资产处置和归档）
   */
  @PostMapping("/{terminationId}/complete")
  @ResponseStatus(HttpStatus.OK)
  public Result<TerminationRequestDto> completeTermination(
      @PathVariable long projectId,
      @PathVariable long terminationId,
      @RequestBody Map<String, Object> body) {
    long actorUserId = ((Number) body.get("actorUserId")).longValue();
    String summaryReportUri = (String) body.get("summaryReportUri");
    boolean assetDisposalConfirmed = (boolean) body.getOrDefault("assetDisposalConfirmed", false);
    boolean archiveConfirmed = (boolean) body.getOrDefault("archiveConfirmed", false);
    return Result.ok(terminationService.completeTermination(
        terminationId, actorUserId, summaryReportUri, assetDisposalConfirmed, archiveConfirmed));
  }

  /**
   * 获取所有待处理的终止申请
   */
  @GetMapping("/pending")
  public Result<List<TerminationRequestDto>> getPendingTerminations() {
    return Result.ok(terminationService.getPendingTerminations());
  }
}