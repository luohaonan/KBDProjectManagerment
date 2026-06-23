package com.kbd.pms.service;

import com.kbd.pms.dto.ReviewApprovalDto;
import com.kbd.pms.dto.ReviewApprovalTaskDto;
import com.kbd.pms.dto.ReviewDecisionRequest;
import com.kbd.pms.dto.ReviewRecordDto;
import com.kbd.pms.dto.ReviewSubmitRequest;
import com.kbd.pms.dto.SaveDraftRequest;
import com.kbd.pms.entity.Enums;
import com.kbd.pms.entity.IamUserEntity;
import com.kbd.pms.entity.MilestoneDefEntity;
import com.kbd.pms.entity.MilestoneHistoryEntity;
import com.kbd.pms.entity.ProjectEntity;
import com.kbd.pms.entity.ProjectMilestoneEntity;
import com.kbd.pms.entity.ReviewApprovalEntity;
import com.kbd.pms.entity.ReviewApprovalTaskEntity;
import com.kbd.pms.entity.ReviewRecordEntity;
import com.kbd.pms.exception.ApiException;
import com.kbd.pms.repository.GovernanceCommitteeMemberRepository;
import com.kbd.pms.repository.IamUserRepository;
import com.kbd.pms.repository.MilestoneDefRepository;
import com.kbd.pms.repository.MilestoneHistoryRepository;
import com.kbd.pms.repository.ProjectMilestoneRepository;
import com.kbd.pms.repository.ProjectRepository;
import com.kbd.pms.repository.ReviewApprovalRepository;
import com.kbd.pms.repository.ReviewApprovalTaskRepository;
import com.kbd.pms.repository.ReviewRecordRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("null")
public class ReviewService {

  private final ProjectRepository projectRepository;
  private final ProjectMilestoneRepository projectMilestoneRepository;
  private final MilestoneDefRepository milestoneDefRepository;
  private final MilestoneHistoryRepository milestoneHistoryRepository;
  private final ReviewApprovalRepository reviewApprovalRepository;
  private final ReviewApprovalTaskRepository reviewApprovalTaskRepository;
  private final ReviewRecordRepository reviewRecordRepository;
  private final IamUserRepository iamUserRepository;
  private final GovernanceCommitteeMemberRepository governanceCommitteeMemberRepository;

  public ReviewService(
      ProjectRepository projectRepository,
      ProjectMilestoneRepository projectMilestoneRepository,
      MilestoneDefRepository milestoneDefRepository,
      MilestoneHistoryRepository milestoneHistoryRepository,
      ReviewApprovalRepository reviewApprovalRepository,
      ReviewApprovalTaskRepository reviewApprovalTaskRepository,
      ReviewRecordRepository reviewRecordRepository,
      IamUserRepository iamUserRepository,
      GovernanceCommitteeMemberRepository governanceCommitteeMemberRepository) {
    this.projectRepository = projectRepository;
    this.projectMilestoneRepository = projectMilestoneRepository;
    this.milestoneDefRepository = milestoneDefRepository;
    this.milestoneHistoryRepository = milestoneHistoryRepository;
    this.reviewApprovalRepository = reviewApprovalRepository;
    this.reviewApprovalTaskRepository = reviewApprovalTaskRepository;
    this.reviewRecordRepository = reviewRecordRepository;
    this.iamUserRepository = iamUserRepository;
    this.governanceCommitteeMemberRepository = governanceCommitteeMemberRepository;
  }

  // ==================== 公开 API ====================

  /**
   * 保存草稿 - 创建或更新评审审批草稿记录
   */
  @Transactional
  public ReviewApprovalDto saveDraft(long projectId, SaveDraftRequest request) {
    ProjectEntity project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ApiException(404, "项目不存在: id=" + projectId));

    ProjectMilestoneEntity pm = getCurrentProjectMilestone(project);

    ReviewApprovalEntity existing = reviewApprovalRepository
        .findTopByProjectIdAndProjectMilestoneIdOrderByCreatedAtDesc(projectId, pm.getId())
        .orElse(null);

    ReviewApprovalEntity approval;
    if (existing != null && existing.getStatus() == ReviewApprovalEntity.Status.DRAFT) {
      approval = existing;
      approval.setSubmitComment(request.submitComment());
      approval.setUpdatedAt(Instant.now());
    } else {
      approval = new ReviewApprovalEntity();
      approval.setProjectId(projectId);
      approval.setProjectMilestoneId(pm.getId());
      approval.setSubmitterUserId(request.actorUserId());
      approval.setSubmitComment(request.submitComment());
      approval.setStatus(ReviewApprovalEntity.Status.DRAFT);
      approval.setCreatedAt(Instant.now());
      approval.setUpdatedAt(Instant.now());
    }
    reviewApprovalRepository.save(approval);

    writeReviewRecord(projectId, pm.getId(), approval.getId(),
        "SAVE_DRAFT", request.actorUserId(), "DRAFT_SAVED", null);

    return toApprovalDto(approval);
  }

  /**
   * 提交评审申请 - PM 提交立项评审
   */
  @Transactional
  public ReviewApprovalDto submitReview(long projectId, ReviewSubmitRequest request) {
    ProjectEntity project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ApiException(404, "项目不存在: id=" + projectId));

    ProjectMilestoneEntity pm = getCurrentProjectMilestone(project);

    if (pm.getStatus() != Enums.ProjectMilestoneStatus.IN_PROGRESS) {
      throw new ApiException(409, "当前里程碑状态不允许提交评审: " + pm.getStatus());
    }

    ReviewApprovalEntity approval = reviewApprovalRepository
        .findTopByProjectIdAndProjectMilestoneIdOrderByCreatedAtDesc(projectId, pm.getId())
        .orElse(null);

    if (approval == null || approval.getStatus() != ReviewApprovalEntity.Status.DRAFT) {
      approval = new ReviewApprovalEntity();
      approval.setProjectId(projectId);
      approval.setProjectMilestoneId(pm.getId());
      approval.setSubmitterUserId(request.actorUserId());
      approval.setCreatedAt(Instant.now());
    }

    approval.setSubmitComment(request.submitComment());
    approval.setStatus(ReviewApprovalEntity.Status.SUBMITTED);
    approval.setSubmittedAt(LocalDateTime.now(ZoneOffset.UTC));
    approval.setUpdatedAt(Instant.now());
    reviewApprovalRepository.save(approval);

    // 更新里程碑状态
    String fromStatus = pm.getStatus().name();
    pm.setStatus(Enums.ProjectMilestoneStatus.SUBMITTED);
    pm.setUpdatedAt(Instant.now());
    projectMilestoneRepository.save(pm);

    // 更新项目评审状态
    project.setReviewStatus("IN_REVIEW");
    project.setReviewSubmittedAt(LocalDateTime.now(ZoneOffset.UTC));
    project.setUpdatedAt(Instant.now());
    projectRepository.save(project);

    // 自动创建审批人任务
    createApproverTasks(project, approval);

    writeMilestoneHistory(project.getId(), pm.getId(),
        MilestoneHistoryEntity.Action.SUBMIT_REVIEW,
        request.actorUserId(), fromStatus, pm.getStatus().name(),
        request.submitComment());

    writeReviewRecord(projectId, pm.getId(), approval.getId(),
        "SUBMIT", request.actorUserId(), "SUBMITTED", request.submitComment());

    return toApprovalDto(approval);
  }

  /**
   * 审批人执行决策 - Go / Conditional Go / No Go
   */
  @Transactional
  public ReviewApprovalDto executeDecision(long projectId, long taskId, ReviewDecisionRequest request) {
    ProjectEntity project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ApiException(404, "项目不存在: id=" + projectId));

    ReviewApprovalTaskEntity task = reviewApprovalTaskRepository.findById(taskId)
        .orElseThrow(() -> new ApiException(404, "审批任务不存在: id=" + taskId));

    if (task.getApproverUserId() != request.actorUserId()) {
      throw new ApiException(403, "您不是此审批任务的审批人");
    }

    if (task.getStatus() != ReviewApprovalTaskEntity.Status.PENDING) {
      throw new ApiException(409, "审批任务已处理，无法重复审批");
    }

    ReviewApprovalEntity approval = reviewApprovalRepository.findById(task.getReviewApprovalId())
        .orElseThrow(() -> new ApiException(500, "审批记录不存在"));

    if (approval.getStatus() != ReviewApprovalEntity.Status.SUBMITTED) {
      throw new ApiException(409, "审批记录状态不允许审批: " + approval.getStatus());
    }

    String decision = request.decision();
    if (!List.of("GO", "CONDITIONAL_GO", "NO_GO").contains(decision)) {
      throw new ApiException(400, "无效的决策类型: " + decision + "，有效值为: GO, CONDITIONAL_GO, NO_GO");
    }

    // 更新任务状态
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    boolean isApproved = "GO".equals(decision) || "CONDITIONAL_GO".equals(decision);
    task.setStatus(isApproved
        ? ReviewApprovalTaskEntity.Status.APPROVED
        : ReviewApprovalTaskEntity.Status.REJECTED);
    task.setDecision(decision);
    task.setOpinion(request.opinion());
    task.setDecidedAt(now);
    task.setUpdatedAt(Instant.now());
    reviewApprovalTaskRepository.save(task);

    String actionType = switch (decision) {
      case "GO" -> "GO";
      case "CONDITIONAL_GO" -> "CONDITIONAL_GO";
      case "NO_GO" -> "NO_GO";
      default -> "REJECT";
    };

    writeReviewRecord(projectId, approval.getProjectMilestoneId(), approval.getId(),
        actionType, request.actorUserId(), decision, request.opinion());

    // 检查所有审批人状态
    List<ReviewApprovalTaskEntity> allTasks =
        reviewApprovalTaskRepository.findByReviewApprovalIdOrderBySortOrderAsc(approval.getId());
    boolean allApproved = allTasks.stream()
        .allMatch(t -> t.getStatus() == ReviewApprovalTaskEntity.Status.APPROVED);
    boolean anyRejected = allTasks.stream()
        .anyMatch(t -> t.getStatus() == ReviewApprovalTaskEntity.Status.REJECTED);

    if (anyRejected) {
      // 任一审批人选择 NO_GO → 整体驳回
      approval.setStatus(ReviewApprovalEntity.Status.REJECTED);
      approval.setFinishedAt(now);
      approval.setUpdatedAt(Instant.now());
      reviewApprovalRepository.save(approval);

      ProjectMilestoneEntity pm = projectMilestoneRepository.findById(approval.getProjectMilestoneId())
          .orElse(null);
      if (pm != null) {
        String from = pm.getStatus().name();
        pm.setStatus(Enums.ProjectMilestoneStatus.REJECTED);
        pm.setDecisionResult(Enums.MilestoneDecisionResult.NO_GO);
        pm.setDecisionAt(now);
        pm.setDecidedBy(request.actorUserId());
        pm.setDecisionNotes(request.opinion());
        pm.setUpdatedAt(Instant.now());
        projectMilestoneRepository.save(pm);

        writeMilestoneHistory(project.getId(), pm.getId(),
            MilestoneHistoryEntity.Action.DECISION,
            request.actorUserId(), from, pm.getStatus().name(),
            "评审不通过 (No Go): " + request.opinion());
      }

      // No Go → 项目终止
      project.setReviewStatus("NO_GO");
      project.setStatus(Enums.ProjectStatus.TERMINATED);
      project.setUpdatedAt(Instant.now());
      projectRepository.save(project);

    } else if (allApproved) {
      // 所有审批人都已决策
      // 检查是否有 CONDITIONAL_GO
      boolean hasConditionalGo = allTasks.stream()
          .anyMatch(t -> "CONDITIONAL_GO".equals(t.getDecision()));

      if (hasConditionalGo) {
        // 有条件通过 - 里程碑状态变为 CONDITIONAL_APPROVED
        approval.setStatus(ReviewApprovalEntity.Status.APPROVED);
        approval.setFinishedAt(now);
        approval.setUpdatedAt(Instant.now());
        reviewApprovalRepository.save(approval);

        ProjectMilestoneEntity pm = projectMilestoneRepository.findById(approval.getProjectMilestoneId())
            .orElse(null);
        if (pm != null) {
          String from = pm.getStatus().name();
          pm.setStatus(Enums.ProjectMilestoneStatus.CONDITIONAL_APPROVED);
          pm.setDecisionResult(Enums.MilestoneDecisionResult.CONDITIONAL_GO);
          pm.setDecisionAt(now);
          pm.setDecidedBy(request.actorUserId());
          pm.setDecisionNotes(request.opinion());
          pm.setUpdatedAt(Instant.now());
          projectMilestoneRepository.save(pm);

          writeMilestoneHistory(project.getId(), pm.getId(),
              MilestoneHistoryEntity.Action.DECISION,
              request.actorUserId(), from, pm.getStatus().name(),
              "有条件通过 (Conditional Go): " + request.opinion());
        }

        project.setReviewStatus("CONDITIONAL_GO");
        project.setUpdatedAt(Instant.now());
        projectRepository.save(project);
      } else {
        // 全部 Go → 正常通过，进入下一阶段
        approval.setStatus(ReviewApprovalEntity.Status.APPROVED);
        approval.setFinishedAt(now);
        approval.setUpdatedAt(Instant.now());
        reviewApprovalRepository.save(approval);

        ProjectMilestoneEntity pm = projectMilestoneRepository.findById(approval.getProjectMilestoneId())
            .orElse(null);
        if (pm != null) {
          String from = pm.getStatus().name();
          pm.setStatus(Enums.ProjectMilestoneStatus.APPROVED);
          pm.setDecisionResult(Enums.MilestoneDecisionResult.GO);
          pm.setDecisionAt(now);
          pm.setDecidedBy(request.actorUserId());
          pm.setDecisionNotes(request.opinion());
          pm.setUpdatedAt(Instant.now());
          projectMilestoneRepository.save(pm);

          MilestoneDefEntity currentDef = milestoneDefRepository.findById(pm.getMilestoneId()).orElse(null);
          if (currentDef != null) {
            MilestoneDefEntity nextDef = findNextMilestone(currentDef);
            if (nextDef != null) {
              ProjectMilestoneEntity next =
                  projectMilestoneRepository.findByProjectIdAndMilestoneId(project.getId(), nextDef.getId());
              if (next != null && next.getStatus() == Enums.ProjectMilestoneStatus.NOT_STARTED) {
                next.setStatus(Enums.ProjectMilestoneStatus.IN_PROGRESS);
                next.setUpdatedAt(Instant.now());
                projectMilestoneRepository.save(next);
              }
              project.setCurrentMilestoneId(nextDef.getId());
            }
          }

          writeMilestoneHistory(project.getId(), pm.getId(),
              MilestoneHistoryEntity.Action.DECISION,
              request.actorUserId(), from, pm.getStatus().name(),
              "评审通过 (Go): " + request.opinion());
        }

        project.setReviewStatus("APPROVED");
        project.setStatus(Enums.ProjectStatus.ACTIVE);
        project.setUpdatedAt(Instant.now());
        projectRepository.save(project);
      }
    }

    return toApprovalDto(approval);
  }

  /**
   * 获取项目的评审审批记录
   */
  @Transactional(readOnly = true)
  public List<ReviewApprovalDto> getProjectReviews(long projectId) {
    List<ReviewApprovalEntity> approvals =
        reviewApprovalRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    return approvals.stream().map(this::toApprovalDto).toList();
  }

  /**
   * 获取评审记录（按项目）
   */
  @Transactional(readOnly = true)
  public List<ReviewRecordDto> getReviewRecords(long projectId) {
    List<ReviewRecordEntity> records =
        reviewRecordRepository.findByProjectIdOrderByActionAtDesc(projectId);
    return records.stream().map(this::toRecordDto).toList();
  }

  /**
   * 获取当前用户的审批任务（作为审批人）
   */
  @Transactional(readOnly = true)
  public List<ReviewApprovalTaskDto> getMyApprovalTasks(long userId) {
    List<ReviewApprovalTaskEntity> tasks =
        reviewApprovalTaskRepository.findByApproverUserIdOrderByCreatedAtDesc(userId);
    return tasks.stream().map(this::toTaskDto).toList();
  }

  /**
   * 获取当前用户提交的评审记录
   */
  @Transactional(readOnly = true)
  public List<ReviewRecordDto> getMyReviewRecords(long userId) {
    List<ReviewRecordEntity> records =
        reviewRecordRepository.findByActorUserIdOrderByActionAtDesc(userId);
    return records.stream().map(this::toRecordDto).toList();
  }

  // ==================== 私有辅助方法 ====================

  private ProjectMilestoneEntity getCurrentProjectMilestone(ProjectEntity project) {
    if (project.getCurrentMilestoneId() == null) {
      throw new ApiException(409, "项目未设置当前里程碑");
    }
    return projectMilestoneRepository
        .findByProjectIdAndMilestoneId(project.getId(), project.getCurrentMilestoneId());
  }

  private void createApproverTasks(ProjectEntity project, ReviewApprovalEntity approval) {
    // 1. 部门负责人审批（如果有主导部门配置）
    // TODO: 根据 milestone_dept_role 配置查找部门负责人
    // 当前简化实现：先创建 PMC 审批任务

    // 2. 项目经理审批
    if (project.getPmUserId() != null) {
      ReviewApprovalTaskEntity pmTask = new ReviewApprovalTaskEntity();
      pmTask.setReviewApprovalId(approval.getId());
      pmTask.setApproverUserId(project.getPmUserId());
      pmTask.setApproverRole("ROLE_PM");
      pmTask.setSortOrder(0);
      pmTask.setStatus(ReviewApprovalTaskEntity.Status.PENDING);
      pmTask.setCreatedAt(Instant.now());
      pmTask.setUpdatedAt(Instant.now());
      reviewApprovalTaskRepository.save(pmTask);
    }

    // 3. PMC 委员会审批
    if (project.getPmcCommitteeId() != null) {
      List<Long> pmcMemberIds = governanceCommitteeMemberRepository
          .findActiveMemberIds(project.getPmcCommitteeId(), LocalDate.now(ZoneOffset.UTC));

      if (!pmcMemberIds.isEmpty()) {
        int sortOrder = 1;
        for (Long memberId : pmcMemberIds) {
          ReviewApprovalTaskEntity task = new ReviewApprovalTaskEntity();
          task.setReviewApprovalId(approval.getId());
          task.setApproverUserId(memberId);
          task.setApproverRole("ROLE_PMC");
          task.setSortOrder(sortOrder++);
          task.setStatus(ReviewApprovalTaskEntity.Status.PENDING);
          task.setCreatedAt(Instant.now());
          task.setUpdatedAt(Instant.now());
          reviewApprovalTaskRepository.save(task);
        }
      }
    }
  }

  private MilestoneDefEntity findNextMilestone(MilestoneDefEntity current) {
    Integer sort = current.getSortNo();
    if (sort == null) return null;
    List<MilestoneDefEntity> defs = milestoneDefRepository.findAllByIsActiveTrueOrderBySortNoAsc();
    for (MilestoneDefEntity d : defs) {
      if (Objects.equals(d.getSortNo(), sort + 1)) {
        return d;
      }
    }
    return null;
  }

  private void writeMilestoneHistory(
      long projectId, long projectMilestoneId,
      MilestoneHistoryEntity.Action action,
      long actorUserId, String fromStatus, String toStatus,
      String notes) {
    MilestoneHistoryEntity h = new MilestoneHistoryEntity();
    h.setProjectId(projectId);
    h.setProjectMilestoneId(projectMilestoneId);
    h.setAction(action);
    h.setActorUserId(actorUserId);
    h.setFromStatus(fromStatus);
    h.setToStatus(toStatus);
    h.setNotes(notes);
    h.setActionAt(LocalDateTime.now(ZoneOffset.UTC));
    h.setCreatedAt(Instant.now());
    milestoneHistoryRepository.save(h);
  }

  private void writeReviewRecord(
      long projectId, long projectMilestoneId, Long reviewApprovalId,
      String action, long actorUserId, String result, String opinion) {
    // 从审批任务中获取审批人角色
    String actorRole = "ROLE_PMC";
    if (reviewApprovalId != null) {
      List<ReviewApprovalTaskEntity> tasks = reviewApprovalTaskRepository
          .findByReviewApprovalIdOrderBySortOrderAsc(reviewApprovalId);
      actorRole = tasks.stream()
          .filter(t -> t.getApproverUserId() != null && t.getApproverUserId() == actorUserId)
          .findFirst()
          .map(ReviewApprovalTaskEntity::getApproverRole)
          .orElse("ROLE_PMC");
    }

    ReviewRecordEntity record = new ReviewRecordEntity();
    record.setProjectId(projectId);
    record.setProjectMilestoneId(projectMilestoneId);
    record.setReviewApprovalId(reviewApprovalId);
    record.setAction(action);
    record.setActorUserId(actorUserId);
    record.setActorRole(actorRole);
    record.setResult(result);
    record.setOpinion(opinion);
    record.setActionAt(LocalDateTime.now(ZoneOffset.UTC));
    record.setCreatedAt(Instant.now());
    reviewRecordRepository.save(record);
  }

  private ReviewApprovalDto toApprovalDto(ReviewApprovalEntity entity) {
    List<ReviewApprovalTaskEntity> tasks =
        reviewApprovalTaskRepository.findByReviewApprovalIdOrderBySortOrderAsc(entity.getId());

    String submitterName = entity.getSubmitterUserId() != null
        ? iamUserRepository.findById(entity.getSubmitterUserId())
            .map(IamUserEntity::getDisplayName).orElse(null)
        : null;

    return new ReviewApprovalDto(
        entity.getId(),
        entity.getProjectId(),
        entity.getProjectMilestoneId(),
        entity.getSubmitterUserId(),
        submitterName,
        entity.getSubmitComment(),
        entity.getStatus().name(),
        entity.getSubmittedAt(),
        entity.getFinishedAt(),
        tasks.stream().map(this::toTaskDto).toList()
    );
  }

  private ReviewApprovalTaskDto toTaskDto(ReviewApprovalTaskEntity entity) {
    String approverName = iamUserRepository.findById(entity.getApproverUserId())
        .map(IamUserEntity::getDisplayName).orElse("未知用户");

    return new ReviewApprovalTaskDto(
        entity.getId(),
        entity.getReviewApprovalId(),
        entity.getApproverUserId(),
        approverName,
        entity.getApproverRole(),
        entity.getSortOrder(),
        entity.getStatus().name(),
        entity.getDecision(),
        entity.getOpinion(),
        entity.getDecidedAt()
    );
  }

  private ReviewRecordDto toRecordDto(ReviewRecordEntity entity) {
    String actorName = entity.getActorUserId() != null
        ? iamUserRepository.findById(entity.getActorUserId())
            .map(IamUserEntity::getDisplayName).orElse(null)
        : null;

    return new ReviewRecordDto(
        entity.getId(),
        entity.getProjectId(),
        entity.getProjectMilestoneId(),
        entity.getAction(),
        entity.getActorUserId(),
        actorName,
        entity.getActorRole(),
        entity.getResult(),
        entity.getOpinion(),
        entity.getActionAt()
    );
  }
}
