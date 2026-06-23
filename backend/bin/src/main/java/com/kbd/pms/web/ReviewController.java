package com.kbd.pms.web;

import com.kbd.pms.dto.ReviewApprovalDto;
import com.kbd.pms.dto.ReviewApprovalTaskDto;
import com.kbd.pms.dto.ReviewDecisionRequest;
import com.kbd.pms.dto.ReviewRecordDto;
import com.kbd.pms.dto.ReviewSubmitRequest;
import com.kbd.pms.dto.SaveDraftRequest;
import com.kbd.pms.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

  private final ReviewService reviewService;

  public ReviewController(ReviewService reviewService) {
    this.reviewService = reviewService;
  }

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
   * 提交评审申请
   */
  @PostMapping("/{projectId}/submit")
  @ResponseStatus(HttpStatus.OK)
  public Result<ReviewApprovalDto> submitReview(
      @PathVariable("projectId") long projectId,
      @Valid @RequestBody ReviewSubmitRequest request) {
    return Result.ok(reviewService.submitReview(projectId, request));
  }

  /**
   * 审批人执行决策（通过/驳回）
   */
  @PostMapping("/{projectId}/tasks/{taskId}/decision")
  @ResponseStatus(HttpStatus.OK)
  public Result<ReviewApprovalDto> executeDecision(
      @PathVariable("projectId") long projectId,
      @PathVariable("taskId") long taskId,
      @Valid @RequestBody ReviewDecisionRequest request) {
    return Result.ok(reviewService.executeDecision(projectId, taskId, request));
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
   * 获取当前用户的审批任务（作为审批人）
   */
  @GetMapping("/my-tasks")
  public Result<List<ReviewApprovalTaskDto>> getMyApprovalTasks(
      @RequestParam("userId") long userId) {
    return Result.ok(reviewService.getMyApprovalTasks(userId));
  }

  /**
   * 获取当前用户提交的评审记录
   */
  @GetMapping("/my-records")
  public Result<List<ReviewRecordDto>> getMyReviewRecords(
      @RequestParam("userId") long userId) {
    return Result.ok(reviewService.getMyReviewRecords(userId));
  }
}
