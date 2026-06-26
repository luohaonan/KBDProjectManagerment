package com.kbd.pms.web;

import com.kbd.pms.dto.DeliverableUploadRequest;
import com.kbd.pms.dto.PendingReviewTaskDto;
import com.kbd.pms.dto.ReviewApprovalDto;
import com.kbd.pms.dto.ReviewApprovalTaskDto;
import com.kbd.pms.dto.ReviewDecisionRequest;
import com.kbd.pms.dto.ReviewProgressResponse;
import com.kbd.pms.dto.ReviewRecordDto;
import com.kbd.pms.dto.ReviewSubmitRequest;
import com.kbd.pms.dto.SaveDraftRequest;
import com.kbd.pms.dto.StepApproveRequest;
import com.kbd.pms.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

  private final ReviewService reviewService;

  public ReviewController(ReviewService reviewService) {
    this.reviewService = reviewService;
  }

  // ==================== 交付物管理 ====================

  /**
   * 部门执行人上传交付物
   */
  @PostMapping("/{projectId}/deliverables/upload")
  @ResponseStatus(HttpStatus.OK)
  public Result<?> uploadDeliverable(
      @PathVariable("projectId") long projectId,
      @Valid @RequestBody DeliverableUploadRequest request) {
    return Result.ok(reviewService.uploadDeliverable(projectId, request));
  }

  // ==================== 评审流程 ====================

  /**
   * 保存草稿
   */
  @PostMapping("/{projectId}/draft")
  @ResponseStatus(HttpStatus.OK)
  public Result<ReviewApprovalDto> saveDraft(
      @PathVariable("projectId") long projectId,
      @Valid @RequestBody SaveDraftRequest request) {
    return Result.ok(reviewService.saveDraft(projectId, request));
  }

  /**
   * 提交/发起评审申请（部门执行人）
   */
  @PostMapping("/{projectId}/submit")
  @ResponseStatus(HttpStatus.OK)
  public Result<ReviewApprovalDto> submitReview(
      @PathVariable("projectId") long projectId,
      @Valid @RequestBody ReviewSubmitRequest request) {
    return Result.ok(reviewService.submitReview(projectId, request));
  }

  /**
   * 分步审批 - 部门负责人/PM技术初评/合规意见/PMC决策/PM内部评审
   */
  @PostMapping("/{projectId}/approve-step")
  @ResponseStatus(HttpStatus.OK)
  public Result<ReviewApprovalDto> approveStep(
      @PathVariable("projectId") long projectId,
      @Valid @RequestBody StepApproveRequest request) {
    return Result.ok(reviewService.approveStep(projectId, request));
  }

  /**
   * 审批人执行决策（兼容旧API，通过taskId）
   */
  @PostMapping("/{projectId}/tasks/{taskId}/decision")
  @ResponseStatus(HttpStatus.OK)
  public Result<ReviewApprovalDto> executeDecision(
      @PathVariable("projectId") long projectId,
      @PathVariable("taskId") long taskId,
      @Valid @RequestBody ReviewDecisionRequest request) {
    return Result.ok(reviewService.executeDecision(projectId, taskId, request));
  }

  // ==================== 查询 ====================

  /**
   * 获取里程碑评审进度（含多步流程状态）
   */
  @GetMapping("/{projectId}/progress")
  public Result<ReviewProgressResponse> getReviewProgress(
      @PathVariable("projectId") long projectId) {
    return Result.ok(reviewService.getReviewProgress(projectId));
  }

  /**
   * 获取项目的评审审批记录
   */
  @GetMapping("/{projectId}")
  public Result<List<ReviewApprovalDto>> getProjectReviews(
      @PathVariable("projectId") long projectId) {
    return Result.ok(reviewService.getProjectReviews(projectId));
  }

  /**
   * 获取项目的评审记录（操作历史）
   */
  @GetMapping("/{projectId}/records")
  public Result<List<ReviewRecordDto>> getReviewRecords(
      @PathVariable("projectId") long projectId) {
    return Result.ok(reviewService.getReviewRecords(projectId));
  }

  /**
   * 获取统一待办任务列表（里程碑评审 + 立项审批）
   */
  @GetMapping("/pending-tasks")
  public Result<List<PendingReviewTaskDto>> getPendingTasks() {
    return Result.ok(reviewService.getPendingTasksForCurrentUser());
  }

  /**
   * 获取当前用户的评审历史
   */
  @GetMapping("/my-records")
  public Result<List<ReviewRecordDto>> getMyReviewRecords() {
    return Result.ok(reviewService.getMyReviewRecordsForCurrentUser());
  }
}