package com.kbd.pms.service;

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
import com.kbd.pms.entity.DocumentEntity;
import com.kbd.pms.entity.Enums;
import com.kbd.pms.entity.IamUserEntity;
import com.kbd.pms.entity.InitiationApprovalEntity;
import com.kbd.pms.entity.InitiationApprovalTaskEntity;
import com.kbd.pms.entity.MilestoneDefEntity;
import com.kbd.pms.entity.MilestoneDeptRoleEntity;
import com.kbd.pms.entity.MilestoneHistoryEntity;
import com.kbd.pms.entity.OrgDepartmentEntity;
import com.kbd.pms.entity.ProjectEntity;
import com.kbd.pms.entity.ProjectMilestoneEntity;
import com.kbd.pms.entity.ReviewApprovalEntity;
import com.kbd.pms.entity.ReviewApprovalTaskEntity;
import com.kbd.pms.entity.ReviewRecordEntity;
import com.kbd.pms.exception.ApiException;
import com.kbd.pms.repository.DocumentRepository;
import com.kbd.pms.repository.GovernanceCommitteeMemberRepository;
import com.kbd.pms.repository.IamUserRepository;
import com.kbd.pms.repository.InitiationApprovalRepository;
import com.kbd.pms.repository.InitiationApprovalTaskRepository;
import com.kbd.pms.repository.MilestoneDefRepository;
import com.kbd.pms.repository.MilestoneDeptRoleRepository;
import com.kbd.pms.repository.MilestoneHistoryRepository;
import com.kbd.pms.repository.OrgDepartmentRepository;
import com.kbd.pms.repository.ProjectMilestoneRepository;
import com.kbd.pms.repository.ProjectRepository;
import com.kbd.pms.repository.ReviewApprovalRepository;
import com.kbd.pms.repository.ReviewApprovalTaskRepository;
import com.kbd.pms.repository.ReviewRecordRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("null")
public class ReviewService {

  // 步骤代码常量
  private static final String STEP_UPLOAD = "UPLOAD";
  private static final String STEP_DEPT_HEAD_APPROVE = "DEPT_HEAD_APPROVE";
  private static final String STEP_PM_TECH_REVIEW = "PM_TECH_REVIEW";
  private static final String STEP_COMPLIANCE_OPINION = "COMPLIANCE_OPINION";
  private static final String STEP_PMC_DECISION = "PMC_DECISION";
  private static final String STEP_PM_INTERNAL_REVIEW = "PM_INTERNAL_REVIEW";

  // 跳过合规意见步骤的里程碑（药政合规部自己发起的）
  private static final List<String> SKIP_COMPLIANCE_MILESTONES = List.of("G5", "G9");

  // 项目组内部评审的里程碑（G1, G2）
  private static final List<String> PM_INTERNAL_REVIEW_MILESTONES = List.of("G1", "G2");

  private final ProjectRepository projectRepository;
  private final ProjectMilestoneRepository projectMilestoneRepository;
  private final MilestoneDefRepository milestoneDefRepository;
  private final MilestoneDeptRoleRepository milestoneDeptRoleRepository;
  private final MilestoneHistoryRepository milestoneHistoryRepository;
  private final ReviewApprovalRepository reviewApprovalRepository;
  private final ReviewApprovalTaskRepository reviewApprovalTaskRepository;
  private final ReviewRecordRepository reviewRecordRepository;
  private final IamUserRepository iamUserRepository;
  private final OrgDepartmentRepository orgDepartmentRepository;
  private final DocumentRepository documentRepository;
  private final GovernanceCommitteeMemberRepository governanceCommitteeMemberRepository;
  private final InitiationApprovalRepository initiationApprovalRepository;
  private final InitiationApprovalTaskRepository initiationApprovalTaskRepository;
  private final SecurityHelper securityHelper;

  public ReviewService(
      ProjectRepository projectRepository,
      ProjectMilestoneRepository projectMilestoneRepository,
      MilestoneDefRepository milestoneDefRepository,
      MilestoneDeptRoleRepository milestoneDeptRoleRepository,
      MilestoneHistoryRepository milestoneHistoryRepository,
      ReviewApprovalRepository reviewApprovalRepository,
      ReviewApprovalTaskRepository reviewApprovalTaskRepository,
      ReviewRecordRepository reviewRecordRepository,
      IamUserRepository iamUserRepository,
      OrgDepartmentRepository orgDepartmentRepository,
      DocumentRepository documentRepository,
      GovernanceCommitteeMemberRepository governanceCommitteeMemberRepository,
      InitiationApprovalRepository initiationApprovalRepository,
      InitiationApprovalTaskRepository initiationApprovalTaskRepository,
      SecurityHelper securityHelper) {
    this.projectRepository = projectRepository;
    this.projectMilestoneRepository = projectMilestoneRepository;
    this.milestoneDefRepository = milestoneDefRepository;
    this.milestoneDeptRoleRepository = milestoneDeptRoleRepository;
    this.milestoneHistoryRepository = milestoneHistoryRepository;
    this.reviewApprovalRepository = reviewApprovalRepository;
    this.reviewApprovalTaskRepository = reviewApprovalTaskRepository;
    this.reviewRecordRepository = reviewRecordRepository;
    this.iamUserRepository = iamUserRepository;
    this.orgDepartmentRepository = orgDepartmentRepository;
    this.documentRepository = documentRepository;
    this.governanceCommitteeMemberRepository = governanceCommitteeMemberRepository;
    this.initiationApprovalRepository = initiationApprovalRepository;
    this.initiationApprovalTaskRepository = initiationApprovalTaskRepository;
    this.securityHelper = securityHelper;
  }

  // ==================== 交付物上传 ====================

  /**
   * 部门执行人上传交付物到指定里程碑的交付物槽位
   * 同一部门多个执行人都可上传（抢占式），但每个槽位只保留最新的
   */
  @Transactional
  public DocumentEntity uploadDeliverable(long projectId, DeliverableUploadRequest request) {
    ProjectEntity project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ApiException(404, "项目不存在: id=" + projectId));

    ProjectMilestoneEntity pm = getCurrentProjectMilestone(project);
    MilestoneDefEntity def = milestoneDefRepository.findById(pm.getMilestoneId())
        .orElseThrow(() -> new ApiException(500, "里程碑字典缺失"));

    IamUserEntity actor = iamUserRepository.findById(request.actorUserId())
        .orElseThrow(() -> new ApiException(404, "用户不存在"));

    // 验证该用户是否为对应部门的执行人
    List<MilestoneDeptRoleEntity> roles = milestoneDeptRoleRepository
        .findByMilestoneDefIdAndRoleTypeAndIsActiveTrue(def.getId(), "DEPT_EXECUTOR");
    boolean authorized = roles.stream().anyMatch(r ->
        r.getDeptId() != null && r.getDeptId().equals(actor.getDeptId()));
    if (!authorized) {
      throw new ApiException(403, "您不是当前里程碑阶段的部门执行人，无法上传交付物");
    }

    // 检查该槽位是否已有文档，如有则覆盖
    DocumentEntity existing = documentRepository
        .findByProjectIdAndMilestonePhaseAndDeliverableSlotCode(projectId,
            Enums.MilestoneStage.valueOf(def.getMilestoneCode()), request.deliverableSlotCode())
        .stream().findFirst().orElse(null);

    DocumentEntity doc;
    if (existing != null) {
      doc = existing;
      doc.setFileName(request.fileName());
      doc.setStoragePath(request.storagePath());
      doc.setUploader(request.actorUserId());
      doc.setUploadedAt(LocalDateTime.now(ZoneOffset.UTC));
    } else {
      doc = new DocumentEntity();
      doc.setProjectId(projectId);
      doc.setMilestonePhase(Enums.MilestoneStage.valueOf(def.getMilestoneCode()));
      doc.setDeliverableSlotCode(request.deliverableSlotCode());
      doc.setFileName(request.fileName());
      doc.setStoragePath(request.storagePath());
      doc.setUploader(request.actorUserId());
      doc.setUploadedAt(LocalDateTime.now(ZoneOffset.UTC));
      doc.setCreatedAt(Instant.now());
    }
    documentRepository.save(doc);

    writeRecord(projectId, pm.getId(), null, "UPLOAD", request.actorUserId(),
        "DOCUMENT_UPLOADED", "上传交付物: " + request.deliverableSlotCode());

    return doc;
  }

  // ==================== 发起里程碑评审 ====================

  /**
   * 部门执行人在所有交付物上传完毕后发起审批
   * 系统自动创建多步审批任务链
   */
  @Transactional
  public ReviewApprovalDto initiateReview(long projectId, ReviewSubmitRequest request) {
    ProjectEntity project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ApiException(404, "项目不存在: id=" + projectId));

    ProjectMilestoneEntity pm = getCurrentProjectMilestone(project);
    MilestoneDefEntity def = milestoneDefRepository.findById(pm.getMilestoneId())
        .orElseThrow(() -> new ApiException(500, "里程碑字典缺失"));

    if (pm.getStatus() != Enums.ProjectMilestoneStatus.IN_PROGRESS
        && pm.getStatus() != Enums.ProjectMilestoneStatus.SUBMITTED) {
      throw new ApiException(409, "当前里程碑状态不允许发起评审: " + pm.getStatus());
    }

    // 检查是否已有进行中的审批
    reviewApprovalRepository
        .findTopByProjectIdAndProjectMilestoneIdOrderByCreatedAtDesc(projectId, pm.getId())
        .ifPresent(existing -> {
          if (existing.getStatus() == ReviewApprovalEntity.Status.SUBMITTED) {
            throw new ApiException(409, "当前里程碑已有进行中的评审申请");
          }
        });

    // 创建评审审批记录
    ReviewApprovalEntity approval = new ReviewApprovalEntity();
    approval.setProjectId(projectId);
    approval.setProjectMilestoneId(pm.getId());
    approval.setSubmitterUserId(request.actorUserId());
    approval.setSubmitComment(request.submitComment());
    approval.setStatus(ReviewApprovalEntity.Status.SUBMITTED);
    approval.setSubmittedAt(LocalDateTime.now(ZoneOffset.UTC));
    approval.setCreatedAt(Instant.now());
    approval.setUpdatedAt(Instant.now());
    reviewApprovalRepository.save(approval);

    // 更新里程碑和项目状态
    pm.setStatus(Enums.ProjectMilestoneStatus.SUBMITTED);
    pm.setUpdatedAt(Instant.now());
    projectMilestoneRepository.save(pm);

    project.setReviewStatus("IN_REVIEW");
    project.setReviewSubmittedAt(LocalDateTime.now(ZoneOffset.UTC));
    project.setUpdatedAt(Instant.now());
    projectRepository.save(project);

    // 创建多步审批任务链
    createMultiStepTasks(project, pm, def, approval);

    writeMilestoneHistory(project.getId(), pm.getId(),
        MilestoneHistoryEntity.Action.SUBMIT_REVIEW,
        request.actorUserId(), "IN_PROGRESS", pm.getStatus().name(),
        request.submitComment());

    writeRecord(projectId, pm.getId(), approval.getId(),
        "SUBMIT", request.actorUserId(), "SUBMITTED", request.submitComment());

    return toApprovalDto(approval);
  }

  // ==================== 分步审批 ====================

  /**
   * 执行审批步骤:
   * DEPT_HEAD_APPROVE - 部门负责人审批
   * PM_TECH_REVIEW - PM技术初评
   * COMPLIANCE_OPINION - 药政合规部合规意见
   * PMC_DECISION - PMC并行决策
   * PM_INTERNAL_REVIEW - PM项目组内部评审(G1/G2)
   */
  @Transactional
  public ReviewApprovalDto approveStep(long projectId, StepApproveRequest request) {
    ProjectEntity project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ApiException(404, "项目不存在: id=" + projectId));

    ProjectMilestoneEntity pm = getCurrentProjectMilestone(project);
    MilestoneDefEntity def = milestoneDefRepository.findById(pm.getMilestoneId())
        .orElseThrow(() -> new ApiException(500, "里程碑字典缺失"));

    String stepCode = request.stepCode();
    String decision = request.decision();

    // 根据步骤类型查找可处理的审批任务
    List<ReviewApprovalTaskEntity> stepTasks;
    ReviewApprovalEntity approval;

    if (STEP_PMC_DECISION.equals(stepCode) || STEP_PM_INTERNAL_REVIEW.equals(stepCode)) {
      // PMC决策/PM内部评审: 所有该步骤的pending任务
      approval = reviewApprovalRepository
          .findTopByProjectIdAndProjectMilestoneIdOrderByCreatedAtDesc(projectId, pm.getId())
          .orElseThrow(() -> new ApiException(404, "未找到评审记录"));
      stepTasks = reviewApprovalTaskRepository
          .findByReviewApprovalIdAndStepCodeAndStatus(approval.getId(), stepCode,
              ReviewApprovalTaskEntity.Status.PENDING);
    } else {
      // 部门负责人/PM/合规: 按审批人匹配
      approval = reviewApprovalRepository
          .findTopByProjectIdAndProjectMilestoneIdOrderByCreatedAtDesc(projectId, pm.getId())
          .orElseThrow(() -> new ApiException(404, "未找到评审记录"));
      stepTasks = reviewApprovalTaskRepository
          .findByReviewApprovalIdAndStepCodeAndStatus(approval.getId(), stepCode,
              ReviewApprovalTaskEntity.Status.PENDING);
    }

    if (stepTasks.isEmpty()) {
      throw new ApiException(409, "当前步骤没有待审批的任务: " + stepCode);
    }

    // 查找当前用户的任务
    ReviewApprovalTaskEntity myTask = stepTasks.stream()
        .filter(t -> t.getApproverUserId() != null && t.getApproverUserId().equals(request.actorUserId()))
        .findFirst()
        .orElse(null);

    if (myTask == null) {
      throw new ApiException(403, "您不在当前步骤的审批人列表中");
    }

    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

    switch (stepCode) {
      case STEP_DEPT_HEAD_APPROVE -> {
        return handleDeptHeadApprove(project, pm, def, approval, myTask, request, now);
      }
      case STEP_PM_TECH_REVIEW -> {
        return handlePmTechReview(project, pm, def, approval, myTask, request, now);
      }
      case STEP_COMPLIANCE_OPINION -> {
        return handleComplianceOpinion(project, pm, def, approval, myTask, request, now);
      }
      case STEP_PMC_DECISION -> {
        return handlePmcDecision(project, pm, def, approval, myTask, stepTasks, request, now);
      }
      case STEP_PM_INTERNAL_REVIEW -> {
        return handlePmInternalReview(project, pm, def, approval, myTask, request, now);
      }
      default -> throw new ApiException(400, "未知的审批步骤代码: " + stepCode);
    }
  }

  // ---- 部门负责人审批 ----
  private ReviewApprovalDto handleDeptHeadApprove(ProjectEntity project, ProjectMilestoneEntity pm,
      MilestoneDefEntity def, ReviewApprovalEntity approval, ReviewApprovalTaskEntity task,
      StepApproveRequest request, LocalDateTime now) {

    task.setStatus("REJECTED".equals(request.decision())
        ? ReviewApprovalTaskEntity.Status.REJECTED
        : ReviewApprovalTaskEntity.Status.APPROVED);
    task.setDecision(request.decision());
    task.setOpinion(request.opinion());
    task.setDecidedAt(now);
    task.setUpdatedAt(Instant.now());
    reviewApprovalTaskRepository.save(task);

    writeRecord(project.getId(), pm.getId(), approval.getId(),
        request.decision(), request.actorUserId(), request.decision(), request.opinion());

    if ("REJECTED".equals(request.decision())) {
      // 部门负责人拒绝: 退回UPLOAD步骤
      resetToUploadStep(approval, pm, project, request.opinion());
    } else {
      // 检查该步骤所有部门负责人是否都已通过
      List<ReviewApprovalTaskEntity> allDeptHeadTasks = reviewApprovalTaskRepository
          .findByReviewApprovalIdAndStepCode(approval.getId(), STEP_DEPT_HEAD_APPROVE);
      boolean allApproved = allDeptHeadTasks.stream()
          .allMatch(t -> t.getStatus() == ReviewApprovalTaskEntity.Status.APPROVED);

      if (allApproved) {
        // 进入下一步: PM_TECH_REVIEW
        // (PM任务在创建时已经生成，无需额外操作)
      }
    }

    return toApprovalDto(approval);
  }

  // ---- PM技术初评 ----
  private ReviewApprovalDto handlePmTechReview(ProjectEntity project, ProjectMilestoneEntity pm,
      MilestoneDefEntity def, ReviewApprovalEntity approval, ReviewApprovalTaskEntity task,
      StepApproveRequest request, LocalDateTime now) {

    task.setStatus("REJECTED".equals(request.decision())
        ? ReviewApprovalTaskEntity.Status.REJECTED
        : ReviewApprovalTaskEntity.Status.APPROVED);
    task.setDecision(request.decision());
    task.setOpinion(request.opinion() != null ? request.opinion()
        : "技术初评: " + (request.techReview() != null ? request.techReview() : "同意"));
    task.setDecidedAt(now);
    task.setUpdatedAt(Instant.now());
    reviewApprovalTaskRepository.save(task);

    writeRecord(project.getId(), pm.getId(), approval.getId(),
        "PM_TECH_REVIEW", request.actorUserId(), request.decision(),
        task.getOpinion());

    if ("REJECTED".equals(request.decision())) {
      resetToUploadStep(approval, pm, project, "PM技术初评不通过: " + request.opinion());
    }
    // 通过后自然流转到下一步骤(COMPLIANCE_OPINION或PMC_DECISION)

    return toApprovalDto(approval);
  }

  // ---- 药政合规部合规意见 ----
  private ReviewApprovalDto handleComplianceOpinion(ProjectEntity project, ProjectMilestoneEntity pm,
      MilestoneDefEntity def, ReviewApprovalEntity approval, ReviewApprovalTaskEntity task,
      StepApproveRequest request, LocalDateTime now) {

    task.setStatus("REJECTED".equals(request.decision())
        ? ReviewApprovalTaskEntity.Status.REJECTED
        : ReviewApprovalTaskEntity.Status.APPROVED);
    task.setDecision(request.decision());
    task.setOpinion(request.opinion() != null ? request.opinion()
        : "合规性意见: " + (request.complianceOpinion() != null ? request.complianceOpinion() : "合规"));
    task.setDecidedAt(now);
    task.setUpdatedAt(Instant.now());
    reviewApprovalTaskRepository.save(task);

    writeRecord(project.getId(), pm.getId(), approval.getId(),
        "COMPLIANCE_OPINION", request.actorUserId(), request.decision(),
        task.getOpinion());

    if ("REJECTED".equals(request.decision())) {
      resetToUploadStep(approval, pm, project, "合规性审核不通过: " + request.opinion());
    }

    return toApprovalDto(approval);
  }

  // ---- PMC决策 (并行操作) ----
  private ReviewApprovalDto handlePmcDecision(ProjectEntity project, ProjectMilestoneEntity pm,
      MilestoneDefEntity def, ReviewApprovalEntity approval, ReviewApprovalTaskEntity myTask,
      List<ReviewApprovalTaskEntity> allStepTasks, StepApproveRequest request, LocalDateTime now) {

    // 验证决策值
    String decision = request.decision();
    if (!List.of("GO", "CONDITIONAL_GO", "NO_GO").contains(decision)) {
      throw new ApiException(400, "PMC决策无效: " + decision + "，有效值为: GO, CONDITIONAL_GO, NO_GO");
    }

    boolean isPositive = "GO".equals(decision) || "CONDITIONAL_GO".equals(decision);
    myTask.setStatus(isPositive
        ? ReviewApprovalTaskEntity.Status.APPROVED
        : ReviewApprovalTaskEntity.Status.REJECTED);
    myTask.setDecision(decision);
    myTask.setOpinion(request.opinion());
    myTask.setDecidedAt(now);
    myTask.setUpdatedAt(Instant.now());
    reviewApprovalTaskRepository.save(myTask);

    writeRecord(project.getId(), pm.getId(), approval.getId(),
        decision, request.actorUserId(), decision, request.opinion());

    // 检查所有PMC成员决策状态: 全部通过 / 有条件通过 / 不通过
    List<ReviewApprovalTaskEntity> allPmcTasks = reviewApprovalTaskRepository
        .findByReviewApprovalIdAndStepCode(approval.getId(), STEP_PMC_DECISION);

    boolean allDecided = allPmcTasks.stream()
        .allMatch(t -> t.getStatus() != ReviewApprovalTaskEntity.Status.PENDING);
    if (!allDecided) {
      return toApprovalDto(approval); // 还有人未决策，等待
    }

    boolean anyNoGo = allPmcTasks.stream()
        .anyMatch(t -> "NO_GO".equals(t.getDecision()));
    boolean anyConditionalGo = allPmcTasks.stream()
        .anyMatch(t -> "CONDITIONAL_GO".equals(t.getDecision()));

    if (anyNoGo) {
      return finalizeDecisionNoGo(project, pm, def, approval, now, request);
    } else if (anyConditionalGo) {
      return finalizeDecisionConditionalGo(project, pm, def, approval, now, request);
    } else {
      return finalizeDecisionGo(project, pm, def, approval, now, request);
    }
  }

  // ---- PM项目组内部评审 (G1/G2) ----
  private ReviewApprovalDto handlePmInternalReview(ProjectEntity project, ProjectMilestoneEntity pm,
      MilestoneDefEntity def, ReviewApprovalEntity approval, ReviewApprovalTaskEntity task,
      StepApproveRequest request, LocalDateTime now) {

    String decision = request.decision();
    if (!List.of("GO", "CONDITIONAL_GO", "NO_GO").contains(decision)) {
      throw new ApiException(400, "内部评审决策无效: " + decision + "，有效值为: GO, CONDITIONAL_GO, NO_GO");
    }

    task.setStatus(ReviewApprovalTaskEntity.Status.APPROVED);
    task.setDecision(decision);
    task.setOpinion(request.opinion());
    task.setDecidedAt(now);
    task.setUpdatedAt(Instant.now());
    reviewApprovalTaskRepository.save(task);

    writeRecord(project.getId(), pm.getId(), approval.getId(),
        decision, request.actorUserId(), decision, request.opinion());

    switch (decision) {
      case "GO" -> { return finalizeDecisionGo(project, pm, def, approval, now, request); }
      case "CONDITIONAL_GO" -> { return finalizeDecisionConditionalGo(project, pm, def, approval, now, request); }
      case "NO_GO" -> { return finalizeDecisionNoGo(project, pm, def, approval, now, request); }
    }
    return toApprovalDto(approval);
  }

  // ---- Go: 通过，进入下一阶段 ----
  private ReviewApprovalDto finalizeDecisionGo(ProjectEntity project, ProjectMilestoneEntity pm,
      MilestoneDefEntity def, ReviewApprovalEntity approval, LocalDateTime now,
      StepApproveRequest request) {
    approval.setStatus(ReviewApprovalEntity.Status.APPROVED);
    approval.setFinishedAt(now);
    approval.setUpdatedAt(Instant.now());
    reviewApprovalRepository.save(approval);

    pm.setStatus(Enums.ProjectMilestoneStatus.APPROVED);
    pm.setDecisionResult(Enums.MilestoneDecisionResult.GO);
    pm.setDecisionAt(now);
    pm.setDecisionNotes(request.opinion());
    pm.setActualDate(LocalDate.now(ZoneOffset.UTC));
    pm.setUpdatedAt(Instant.now());
    projectMilestoneRepository.save(pm);

    // 打开下一个里程碑
    MilestoneDefEntity nextDef = findNextMilestone(def);
    if (nextDef != null) {
      ProjectMilestoneEntity next = projectMilestoneRepository
          .findByProjectIdAndMilestoneId(project.getId(), nextDef.getId());
      if (next != null && next.getStatus() == Enums.ProjectMilestoneStatus.NOT_STARTED) {
        next.setStatus(Enums.ProjectMilestoneStatus.IN_PROGRESS);
        next.setUpdatedAt(Instant.now());
        projectMilestoneRepository.save(next);
      }
      project.setCurrentMilestoneId(nextDef.getId());
    }

    project.setReviewStatus("APPROVED");
    project.setStatus(Enums.ProjectStatus.ACTIVE);
    project.setUpdatedAt(Instant.now());
    projectRepository.save(project);

    writeMilestoneHistory(project.getId(), pm.getId(),
        MilestoneHistoryEntity.Action.DECISION,
        request.actorUserId(), "SUBMITTED", pm.getStatus().name(),
        "评审通过 (Go): " + (request.opinion() != null ? request.opinion() : ""));

    return toApprovalDto(approval);
  }

  // ---- Conditional Go: 有条件通过 ----
  private ReviewApprovalDto finalizeDecisionConditionalGo(ProjectEntity project,
      ProjectMilestoneEntity pm, MilestoneDefEntity def, ReviewApprovalEntity approval,
      LocalDateTime now, StepApproveRequest request) {
    approval.setStatus(ReviewApprovalEntity.Status.APPROVED);
    approval.setFinishedAt(now);
    approval.setUpdatedAt(Instant.now());
    reviewApprovalRepository.save(approval);

    pm.setStatus(Enums.ProjectMilestoneStatus.CONDITIONAL_APPROVED);
    pm.setDecisionResult(Enums.MilestoneDecisionResult.CONDITIONAL_GO);
    pm.setDecisionAt(now);
    pm.setDecisionNotes(request.opinion());
    pm.setConditionalDeadline(now.plusMonths(3));
    pm.setUpdatedAt(Instant.now());
    projectMilestoneRepository.save(pm);

    project.setReviewStatus("CONDITIONAL_GO");
    project.setUpdatedAt(Instant.now());
    projectRepository.save(project);

    writeMilestoneHistory(project.getId(), pm.getId(),
        MilestoneHistoryEntity.Action.DECISION,
        request.actorUserId(), "SUBMITTED", pm.getStatus().name(),
        "有条件通过 (Conditional Go): " + (request.opinion() != null ? request.opinion() : ""));

    return toApprovalDto(approval);
  }

  // ---- No Go: 不通过，项目终止 ----
  private ReviewApprovalDto finalizeDecisionNoGo(ProjectEntity project, ProjectMilestoneEntity pm,
      MilestoneDefEntity def, ReviewApprovalEntity approval, LocalDateTime now,
      StepApproveRequest request) {
    approval.setStatus(ReviewApprovalEntity.Status.REJECTED);
    approval.setFinishedAt(now);
    approval.setUpdatedAt(Instant.now());
    reviewApprovalRepository.save(approval);

    pm.setStatus(Enums.ProjectMilestoneStatus.REJECTED);
    pm.setDecisionResult(Enums.MilestoneDecisionResult.NO_GO);
    pm.setDecisionAt(now);
    pm.setDecisionNotes(request.opinion());
    pm.setUpdatedAt(Instant.now());
    projectMilestoneRepository.save(pm);

    project.setReviewStatus("NO_GO");
    project.setStatus(Enums.ProjectStatus.TERMINATED);
    project.setTerminatedReason(request.opinion());
    project.setUpdatedAt(Instant.now());
    projectRepository.save(project);

    writeMilestoneHistory(project.getId(), pm.getId(),
        MilestoneHistoryEntity.Action.DECISION,
        request.actorUserId(), "SUBMITTED", pm.getStatus().name(),
        "评审不通过 (No Go): " + (request.opinion() != null ? request.opinion() : ""));

    return toApprovalDto(approval);
  }

  // ---- 退回上传步骤 ----
  private void resetToUploadStep(ReviewApprovalEntity approval, ProjectMilestoneEntity pm,
      ProjectEntity project, String reason) {
    // 将所有后续未处理的审批任务标记为CANCELLED
    List<ReviewApprovalTaskEntity> allTasks = reviewApprovalTaskRepository
        .findByReviewApprovalIdOrderBySortOrderAsc(approval.getId());
    for (ReviewApprovalTaskEntity t : allTasks) {
      if (t.getStatus() == ReviewApprovalTaskEntity.Status.PENDING) {
        t.setStatus(ReviewApprovalTaskEntity.Status.REJECTED); // 用REJECTED表示因上一步驳回而跳过
        t.setUpdatedAt(Instant.now());
        reviewApprovalTaskRepository.save(t);
      }
    }

    // 审批记录标记为REJECTED
    approval.setStatus(ReviewApprovalEntity.Status.REJECTED);
    approval.setUpdatedAt(Instant.now());
    reviewApprovalRepository.save(approval);

    // 里程碑退回IN_PROGRESS，允许重新上传
    pm.setStatus(Enums.ProjectMilestoneStatus.IN_PROGRESS);
    pm.setUpdatedAt(Instant.now());
    projectMilestoneRepository.save(pm);

    project.setReviewStatus(null);
    project.setUpdatedAt(Instant.now());
    projectRepository.save(project);
  }

  // ==================== 创建多步审批任务链 ====================

  private void createMultiStepTasks(ProjectEntity project, ProjectMilestoneEntity pm,
      MilestoneDefEntity def, ReviewApprovalEntity approval) {
    String milestoneCode = def.getMilestoneCode();
    int sortBase = 0;

    // Step 2: 部门负责人审批 (并行)
    List<MilestoneDeptRoleEntity> deptHeadRoles = milestoneDeptRoleRepository
        .findByMilestoneDefIdAndRoleTypeAndIsActiveTrue(def.getId(), "DEPT_HEAD");
    if (!deptHeadRoles.isEmpty()) {
      for (MilestoneDeptRoleEntity role : deptHeadRoles) {
        // 查找该部门的负责人
        OrgDepartmentEntity dept = orgDepartmentRepository.findById(role.getDeptId()).orElse(null);
        if (dept != null && dept.getHeadUserId() != null) {
          createTask(approval.getId(), dept.getHeadUserId(), "ROLE_DEPT_HEAD",
              STEP_DEPT_HEAD_APPROVE, null, sortBase++);
        }
      }
    }

    // Step 3: PM技术初评
    if (project.getPmUserId() != null) {
      createTask(approval.getId(), project.getPmUserId(), "ROLE_PM",
          STEP_PM_TECH_REVIEW, null, sortBase++);
    }

    // Step 4: 药政合规部合规意见 (G5/G9跳过)
    if (!SKIP_COMPLIANCE_MILESTONES.contains(milestoneCode)) {
      List<MilestoneDeptRoleEntity> complianceRoles = milestoneDeptRoleRepository
          .findByMilestoneDefIdAndDeptIdAndIsActiveTrue(def.getId(), 7L); // dept_id=7 = 药政合规部
      for (MilestoneDeptRoleEntity role : complianceRoles) {
        if ("DEPT_HEAD".equals(role.getRoleType())) {
          OrgDepartmentEntity dept = orgDepartmentRepository.findById(7L).orElse(null);
          if (dept != null && dept.getHeadUserId() != null) {
            createTask(approval.getId(), dept.getHeadUserId(), "ROLE_COMPLIANCE",
                STEP_COMPLIANCE_OPINION, null, sortBase++);
          }
        }
      }
    }

    // Step 5: PMC决策 或 PM项目组内部评审
    if (PM_INTERNAL_REVIEW_MILESTONES.contains(milestoneCode)) {
      // G1/G2: PM项目组内部评审
      if (project.getPmUserId() != null) {
        createTask(approval.getId(), project.getPmUserId(), "ROLE_PM",
            STEP_PM_INTERNAL_REVIEW, null, sortBase++);
      }
    } else {
      // 其他里程碑: PMC并行评审
      if (project.getPmcCommitteeId() != null) {
        List<Long> pmcMemberIds = governanceCommitteeMemberRepository
            .findActiveMemberIds(project.getPmcCommitteeId(), LocalDate.now(ZoneOffset.UTC));
        for (Long memberId : pmcMemberIds) {
          createTask(approval.getId(), memberId, "ROLE_PMC",
              STEP_PMC_DECISION, null, sortBase++);
        }
      }
    }
  }

  private ReviewApprovalTaskEntity createTask(Long approvalId, Long approverUserId,
      String approverRole, String stepCode, String deliverableSlotCode, int sortOrder) {
    ReviewApprovalTaskEntity task = new ReviewApprovalTaskEntity();
    task.setReviewApprovalId(approvalId);
    task.setApproverUserId(approverUserId);
    task.setApproverRole(approverRole);
    task.setStepCode(stepCode);
    task.setDeliverableSlotCode(deliverableSlotCode);
    task.setSortOrder(sortOrder);
    task.setStatus(ReviewApprovalTaskEntity.Status.PENDING);
    task.setCreatedAt(Instant.now());
    task.setUpdatedAt(Instant.now());
    return reviewApprovalTaskRepository.save(task);
  }

  // ==================== 评审进度查询 ====================

  /**
   * 获取里程碑评审进度
   */
  @Transactional(readOnly = true)
  public ReviewProgressResponse getReviewProgress(long projectId) {
    ProjectEntity project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ApiException(404, "项目不存在"));

    ProjectMilestoneEntity pm = getCurrentProjectMilestone(project);
    MilestoneDefEntity def = milestoneDefRepository.findById(pm.getMilestoneId())
        .orElseThrow(() -> new ApiException(500, "里程碑字典缺失"));

    ReviewApprovalEntity approval = reviewApprovalRepository
        .findTopByProjectIdAndProjectMilestoneIdOrderByCreatedAtDesc(projectId, pm.getId())
        .orElse(null);

    String milestoneCode = def.getMilestoneCode();

    // 构建步骤列表
    List<ReviewProgressResponse.StepProgress> steps = new ArrayList<>();

    // 判断是否跳过合规步骤
    boolean skipCompliance = SKIP_COMPLIANCE_MILESTONES.contains(milestoneCode);
    boolean isPmInternal = PM_INTERNAL_REVIEW_MILESTONES.contains(milestoneCode);

    // Step 1: UPLOAD
    steps.add(buildStepProgress(approval, milestoneCode, STEP_UPLOAD, "交付物上传"));

    // Step 2: DEPT_HEAD_APPROVE
    steps.add(buildStepProgress(approval, milestoneCode, STEP_DEPT_HEAD_APPROVE, "部门负责人审批"));

    // Step 3: PM_TECH_REVIEW
    steps.add(buildStepProgress(approval, milestoneCode, STEP_PM_TECH_REVIEW, "PM技术初评"));

    // Step 4: COMPLIANCE_OPINION (G5/G9跳过)
    if (!skipCompliance) {
      steps.add(buildStepProgress(approval, milestoneCode, STEP_COMPLIANCE_OPINION, "药政合规部合规意见"));
    }

    // Step 5: PMC_DECISION or PM_INTERNAL_REVIEW
    String finalStepName = isPmInternal ? "PM项目组内部评审" : "PMC决策评审";
    String finalStepCode = isPmInternal ? STEP_PM_INTERNAL_REVIEW : STEP_PMC_DECISION;
    steps.add(buildStepProgress(approval, milestoneCode, finalStepCode, finalStepName));

    // 确定当前步骤
    String currentStep = determineCurrentStep(steps, pm.getStatus());

    return new ReviewProgressResponse(
        projectId, pm.getId(), milestoneCode, def.getMilestoneName(),
        currentStep, pm.getStatus().name(), steps);
  }

  private ReviewProgressResponse.StepProgress buildStepProgress(
      ReviewApprovalEntity approval, String milestoneCode, String stepCode, String stepName) {
    if (approval == null) {
      return new ReviewProgressResponse.StepProgress(
          stepCode, stepName, "PENDING", null, List.of());
    }

    List<ReviewApprovalTaskEntity> tasks = reviewApprovalTaskRepository
        .findByReviewApprovalIdAndStepCode(approval.getId(), stepCode);

    String status = tasks.isEmpty() ? "PENDING" : determineStepStatus(tasks);

    LocalDateTime completedAt = tasks.stream()
        .filter(t -> t.getDecidedAt() != null)
        .map(ReviewApprovalTaskEntity::getDecidedAt)
        .max(LocalDateTime::compareTo)
        .orElse(null);

    List<ReviewProgressResponse.TaskDetail> taskDetails = tasks.stream()
        .map(t -> {
          String name = iamUserRepository.findById(t.getApproverUserId())
              .map(IamUserEntity::getDisplayName).orElse("未知用户");
          return new ReviewProgressResponse.TaskDetail(
              t.getId(), t.getApproverUserId(), name, t.getApproverRole(),
              t.getDeliverableSlotCode(), t.getDecision(), t.getOpinion(),
              t.getDecidedAt(), t.getStatus().name());
        }).collect(Collectors.toList());

    return new ReviewProgressResponse.StepProgress(
        stepCode, stepName, status, completedAt, taskDetails);
  }

  private String determineStepStatus(List<ReviewApprovalTaskEntity> tasks) {
    boolean allApproved = tasks.stream()
        .allMatch(t -> t.getStatus() == ReviewApprovalTaskEntity.Status.APPROVED);
    boolean anyRejected = tasks.stream()
        .anyMatch(t -> t.getStatus() == ReviewApprovalTaskEntity.Status.REJECTED);
    boolean anyPending = tasks.stream()
        .anyMatch(t -> t.getStatus() == ReviewApprovalTaskEntity.Status.PENDING);

    if (anyRejected && !anyPending) return "REJECTED";
    if (allApproved) return "APPROVED";
    if (anyPending) return "IN_PROGRESS";
    return "PENDING";
  }

  private String determineCurrentStep(List<ReviewProgressResponse.StepProgress> steps,
      Enums.ProjectMilestoneStatus milestoneStatus) {
    for (ReviewProgressResponse.StepProgress step : steps) {
      if ("IN_PROGRESS".equals(step.status()) || "PENDING".equals(step.status())) {
        return step.stepCode();
      }
    }
    return steps.get(steps.size() - 1).stepCode();
  }

  // ==================== 保留旧的公开API兼容性 ====================

  @Transactional
  public ReviewApprovalDto saveDraft(long projectId, SaveDraftRequest request) {
    ProjectEntity project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ApiException(404, "项目不存在"));
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

    writeRecord(projectId, pm.getId(), approval.getId(),
        "SAVE_DRAFT", request.actorUserId(), "DRAFT_SAVED", null);
    return toApprovalDto(approval);
  }

  /**
   * 提交评审 (兼容旧API)
   */
  @Transactional
  public ReviewApprovalDto submitReview(long projectId, ReviewSubmitRequest request) {
    return initiateReview(projectId, request);
  }

  /**
   * 审批人执行决策 (兼容旧API)
   */
  @Transactional
  public ReviewApprovalDto executeDecision(long projectId, long taskId, ReviewDecisionRequest request) {
    // 查找task确定stepCode，然后调用approveStep
    ReviewApprovalTaskEntity task = reviewApprovalTaskRepository.findById(taskId)
        .orElseThrow(() -> new ApiException(404, "审批任务不存在"));

    String stepCode = task.getStepCode();
    if (stepCode == null) {
      // 兼容旧数据：从approverRole推断
      stepCode = switch (task.getApproverRole()) {
        case "ROLE_DEPT_HEAD" -> STEP_DEPT_HEAD_APPROVE;
        case "ROLE_PM" -> STEP_PM_TECH_REVIEW;
        case "ROLE_COMPLIANCE" -> STEP_COMPLIANCE_OPINION;
        case "ROLE_PMC" -> STEP_PMC_DECISION;
        default -> STEP_PMC_DECISION;
      };
    }

    StepApproveRequest stepRequest = new StepApproveRequest(
        request.actorUserId(), stepCode, request.decision(), request.opinion(), null, null);
    return approveStep(projectId, stepRequest);
  }

  @Transactional(readOnly = true)
  public List<ReviewApprovalDto> getProjectReviews(long projectId) {
    return reviewApprovalRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
        .stream().map(this::toApprovalDto).toList();
  }

  @Transactional(readOnly = true)
  public List<ReviewRecordDto> getReviewRecords(long projectId) {
    return reviewRecordRepository.findByProjectIdOrderByActionAtDesc(projectId)
        .stream().map(this::toRecordDto).toList();
  }

  @Transactional(readOnly = true)
  public List<ReviewApprovalTaskDto> getMyApprovalTasks(long userId) {
    return reviewApprovalTaskRepository.findByApproverUserIdOrderByCreatedAtDesc(userId)
        .stream().map(this::toTaskDto).toList();
  }

  @Transactional(readOnly = true)
  public List<ReviewRecordDto> getMyReviewRecords(long userId) {
    return reviewRecordRepository.findByActorUserIdOrderByActionAtDesc(userId)
        .stream().map(this::toRecordDto).toList();
  }

  @Transactional(readOnly = true)
  public List<PendingReviewTaskDto> getPendingTasksForCurrentUser() {
    long userId = securityHelper.getCurrentUserId();
    return getPendingTasks(userId);
  }

  @Transactional(readOnly = true)
  public List<ReviewRecordDto> getMyReviewRecordsForCurrentUser() {
    long userId = securityHelper.getCurrentUserId();
    List<ReviewRecordDto> records = new ArrayList<>();

    // 里程碑评审记录
    List<ReviewRecordEntity> milestoneRecords =
        reviewRecordRepository.findByActorUserIdOrderByActionAtDesc(userId);
    for (ReviewRecordEntity rec : milestoneRecords) {
      try {
        ProjectEntity project = projectRepository.findById(rec.getProjectId()).orElse(null);
        if (project == null) continue;
        String projectName = project.getProjectName();
        records.add(new ReviewRecordDto(
            rec.getId(), rec.getProjectId(), rec.getProjectMilestoneId(),
            rec.getAction(), rec.getActorUserId(), projectName,
            rec.getActorRole(), rec.getResult(), rec.getOpinion(), rec.getActionAt()));
      } catch (Exception e) { /* skip */ }
    }

    // 立项审批决策记录
    List<InitiationApprovalTaskEntity> initiationTasks =
        initiationApprovalTaskRepository.findByApproverUserIdOrderByCreatedAtDesc(userId);
    for (InitiationApprovalTaskEntity task : initiationTasks) {
      try {
        if (task.getStatus() == InitiationApprovalTaskEntity.Status.PENDING) continue;
        InitiationApprovalEntity approval = initiationApprovalRepository
            .findById(task.getInitiationApprovalId()).orElse(null);
        if (approval == null) continue;
        ProjectEntity project = projectRepository.findById(approval.getProjectId()).orElse(null);
        if (project == null) continue;

        String action = "APPROVED".equals(task.getDecision()) ? "APPROVE" : "REJECT";
        records.add(new ReviewRecordDto(
            task.getId(), project.getId(), 0L, action, task.getApproverUserId(),
            project.getProjectName(), task.getApproverRole(),
            task.getDecision(), task.getOpinion(), task.getDecidedAt()));
      } catch (Exception e) { /* skip */ }
    }

    records.sort((a, b) -> {
      if (a.actionAt() == null) return 1;
      if (b.actionAt() == null) return -1;
      return b.actionAt().compareTo(a.actionAt());
    });
    return records;
  }

  @Transactional(readOnly = true)
  public List<PendingReviewTaskDto> getPendingTasks(long userId) {
    List<PendingReviewTaskDto> result = new ArrayList<>();

    // 里程碑评审待办
    List<ReviewApprovalTaskEntity> milestoneTasks =
        reviewApprovalTaskRepository.findByApproverUserIdAndStatusOrderByCreatedAtDesc(
            userId, ReviewApprovalTaskEntity.Status.PENDING);
    for (ReviewApprovalTaskEntity task : milestoneTasks) {
      try {
        ReviewApprovalEntity approval = reviewApprovalRepository
            .findById(task.getReviewApprovalId()).orElse(null);
        if (approval == null || approval.getStatus() != ReviewApprovalEntity.Status.SUBMITTED) continue;
        ProjectEntity project = projectRepository.findById(approval.getProjectId()).orElse(null);
        if (project == null) continue;

        String milestoneName = "", milestoneCode = "";
        ProjectMilestoneEntity pm = projectMilestoneRepository
            .findById(approval.getProjectMilestoneId()).orElse(null);
        if (pm != null) {
          MilestoneDefEntity def = milestoneDefRepository.findById(pm.getMilestoneId()).orElse(null);
          if (def != null) { milestoneName = def.getMilestoneName(); milestoneCode = def.getMilestoneCode(); }
        }

        String submitterName = approval.getSubmitterUserId() != null
            ? iamUserRepository.findById(approval.getSubmitterUserId())
                .map(IamUserEntity::getDisplayName).orElse(null) : null;

        result.add(new PendingReviewTaskDto(
            task.getId(), approval.getId(), project.getId(), project.getProjectName(),
            project.getProjectCode(), milestoneName, milestoneCode,
            submitterName, approval.getSubmittedAt(), task.getStepCode() != null ? task.getStepCode() : task.getApproverRole(),
            "MILESTONE"));
      } catch (Exception e) { /* skip */ }
    }

    // 立项审批待办
    List<InitiationApprovalTaskEntity> initiationTasks =
        initiationApprovalTaskRepository.findByApproverUserIdOrderByCreatedAtDesc(userId);
    for (InitiationApprovalTaskEntity task : initiationTasks) {
      try {
        if (task.getStatus() != InitiationApprovalTaskEntity.Status.PENDING) continue;
        InitiationApprovalEntity approval = initiationApprovalRepository
            .findById(task.getInitiationApprovalId()).orElse(null);
        if (approval == null || approval.getStatus() != InitiationApprovalEntity.Status.SUBMITTED) continue;
        ProjectEntity project = projectRepository.findById(approval.getProjectId()).orElse(null);
        if (project == null) continue;

        MilestoneDefEntity g0Def = milestoneDefRepository.findAllByIsActiveTrueOrderBySortNoAsc().stream()
            .filter(d -> "G0".equals(d.getMilestoneCode())).findFirst().orElse(null);

        String submitterName = approval.getSubmitterUserId() != null
            ? iamUserRepository.findById(approval.getSubmitterUserId())
                .map(IamUserEntity::getDisplayName).orElse(null) : null;

        result.add(new PendingReviewTaskDto(
            task.getId(), approval.getId(), project.getId(), project.getProjectName(),
            project.getProjectCode(), g0Def != null ? g0Def.getMilestoneName() : "立项审批",
            "G0", submitterName, approval.getSubmittedAt(),
            task.getApproverRole(), "INITIATION"));
      } catch (Exception e) { /* skip */ }
    }

    result.sort((a, b) -> {
      if (a.submittedAt() == null) return 1;
      if (b.submittedAt() == null) return -1;
      return b.submittedAt().compareTo(a.submittedAt());
    });
    return result;
  }

  // ==================== 私有辅助方法 ====================

  private ProjectMilestoneEntity getCurrentProjectMilestone(ProjectEntity project) {
    if (project.getCurrentMilestoneId() == null) {
      throw new ApiException(409, "项目未设置当前里程碑");
    }
    return projectMilestoneRepository
        .findByProjectIdAndMilestoneId(project.getId(), project.getCurrentMilestoneId());
  }

  private MilestoneDefEntity findNextMilestone(MilestoneDefEntity current) {
    Integer sort = current.getSortNo();
    if (sort == null) return null;
    List<MilestoneDefEntity> defs = milestoneDefRepository.findAllByIsActiveTrueOrderBySortNoAsc();
    return defs.stream().filter(d -> Objects.equals(d.getSortNo(), sort + 1)).findFirst().orElse(null);
  }

  private void writeMilestoneHistory(long projectId, long pmId,
      MilestoneHistoryEntity.Action action, long actorUserId,
      String fromStatus, String toStatus, String notes) {
    MilestoneHistoryEntity h = new MilestoneHistoryEntity();
    h.setProjectId(projectId);
    h.setProjectMilestoneId(pmId);
    h.setAction(action);
    h.setActorUserId(actorUserId);
    h.setFromStatus(fromStatus);
    h.setToStatus(toStatus);
    h.setNotes(notes);
    h.setActionAt(LocalDateTime.now(ZoneOffset.UTC));
    h.setCreatedAt(Instant.now());
    milestoneHistoryRepository.save(h);
  }

  private void writeRecord(long projectId, long pmId, Long approvalId,
      String action, long actorUserId, String result, String opinion) {
    String actorRole = "ROLE_PMC";
    if (approvalId != null) {
      List<ReviewApprovalTaskEntity> tasks = reviewApprovalTaskRepository
          .findByReviewApprovalIdOrderBySortOrderAsc(approvalId);
      actorRole = tasks.stream()
          .filter(t -> t.getApproverUserId() != null && t.getApproverUserId() == actorUserId)
          .findFirst().map(ReviewApprovalTaskEntity::getApproverRole).orElse("ROLE_PMC");
    }
    ReviewRecordEntity record = new ReviewRecordEntity();
    record.setProjectId(projectId);
    record.setProjectMilestoneId(pmId);
    record.setReviewApprovalId(approvalId);
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
            .map(IamUserEntity::getDisplayName).orElse(null) : null;
    return new ReviewApprovalDto(
        entity.getId(), entity.getProjectId(), entity.getProjectMilestoneId(),
        entity.getSubmitterUserId(), submitterName, entity.getSubmitComment(),
        entity.getStatus().name(), entity.getSubmittedAt(), entity.getFinishedAt(),
        tasks.stream().map(this::toTaskDto).toList());
  }

  private ReviewApprovalTaskDto toTaskDto(ReviewApprovalTaskEntity entity) {
    String approverName = iamUserRepository.findById(entity.getApproverUserId())
        .map(IamUserEntity::getDisplayName).orElse("未知用户");
    return new ReviewApprovalTaskDto(
        entity.getId(), entity.getReviewApprovalId(), entity.getApproverUserId(),
        approverName, entity.getApproverRole(), entity.getSortOrder(),
        entity.getStatus().name(), entity.getDecision(), entity.getOpinion(),
        entity.getDecidedAt());
  }

  private ReviewRecordDto toRecordDto(ReviewRecordEntity entity) {
    String actorName = entity.getActorUserId() != null
        ? iamUserRepository.findById(entity.getActorUserId())
            .map(IamUserEntity::getDisplayName).orElse(null) : null;
    return new ReviewRecordDto(
        entity.getId(), entity.getProjectId(), entity.getProjectMilestoneId(),
        entity.getAction(), entity.getActorUserId(), actorName,
        entity.getActorRole(), entity.getResult(), entity.getOpinion(),
        entity.getActionAt());
  }
}