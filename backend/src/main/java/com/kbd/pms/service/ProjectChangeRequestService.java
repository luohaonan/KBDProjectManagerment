package com.kbd.pms.service;

import com.kbd.pms.dto.ProjectChangeRequestDecisionRequest;
import com.kbd.pms.dto.ProjectChangeRequestDto;
import com.kbd.pms.entity.BudgetLimitEntity;
import com.kbd.pms.entity.Enums;
import com.kbd.pms.entity.MilestoneDefEntity;
import com.kbd.pms.entity.ProjectBudgetPlanEntity;
import com.kbd.pms.entity.ProjectChangeRequestEntity;
import com.kbd.pms.entity.ProjectEntity;
import com.kbd.pms.entity.ProjectMilestoneEntity;
import com.kbd.pms.exception.ApiException;
import com.kbd.pms.repository.BudgetLimitRepository;
import com.kbd.pms.repository.MilestoneDefRepository;
import com.kbd.pms.repository.ProjectBudgetPlanRepository;
import com.kbd.pms.repository.ProjectChangeRequestRepository;
import com.kbd.pms.repository.ProjectMilestoneRepository;
import com.kbd.pms.repository.ProjectRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectChangeRequestService {

  private final ProjectRepository projectRepository;
  private final ProjectChangeRequestRepository changeRequestRepository;
  private final UserService userService;
  private final ProjectMilestoneRepository milestoneRepository;
  private final MilestoneDefRepository milestoneDefRepository;
  private final BudgetLimitRepository budgetLimitRepository;
  private final ProjectBudgetPlanRepository budgetPlanRepository;

  public ProjectChangeRequestService(
      ProjectRepository projectRepository,
      ProjectChangeRequestRepository changeRequestRepository,
      UserService userService,
      ProjectMilestoneRepository milestoneRepository,
      MilestoneDefRepository milestoneDefRepository,
      BudgetLimitRepository budgetLimitRepository,
      ProjectBudgetPlanRepository budgetPlanRepository) {
    this.projectRepository = projectRepository;
    this.changeRequestRepository = changeRequestRepository;
    this.userService = userService;
    this.milestoneRepository = milestoneRepository;
    this.milestoneDefRepository = milestoneDefRepository;
    this.budgetLimitRepository = budgetLimitRepository;
    this.budgetPlanRepository = budgetPlanRepository;
  }

  @Transactional
  public ProjectChangeRequestDto.ProjectChangeRequestResponse createChangeRequest(
      long projectId, ProjectChangeRequestDto.ProjectChangeRequestCreateRequest request) {
    projectRepository.findById(projectId)
        .orElseThrow(() -> new ApiException(404, "项目不存在: id=" + projectId));

    if (request.changeType() == Enums.ChangeType.MILESTONE_SCHEDULE) {
      validateMilestoneScheduleRequest(request);
    }
    if (request.changeType() == Enums.ChangeType.BUDGET) {
      validateBudgetChangeRequest(request);
    }
    if (request.changeType() == Enums.ChangeType.PAUSE_TERMINATE) {
      if (request.assetDisposalConfirmed() == null || !request.assetDisposalConfirmed()) {
        throw new ApiException(400, "项目终止前必须确认资产处置");
      }
      if (request.archiveConfirmed() == null || !request.archiveConfirmed()) {
        throw new ApiException(400, "项目终止前必须确认归档完成");
      }
    }

    ProjectChangeRequestEntity entity = new ProjectChangeRequestEntity();
    entity.setProjectId(projectId);
    entity.setChangeType(request.changeType());
    entity.setReasonText(request.reasonText());
    entity.setBeforeText(request.beforeText());
    entity.setAfterText(request.afterText());
    entity.setImpactMilestoneText(request.impactMilestoneText());
    entity.setImpactBudgetText(request.impactBudgetText());
    entity.setImpactResourceText(request.impactResourceText());
    entity.setRequestedBy(request.requestedBy());
    entity.setRequestedAt(LocalDateTime.now());
    entity.setStatus(Enums.ApprovalStatus.SUBMITTED);
    entity.setTargetMilestoneId(request.targetMilestoneId());
    entity.setTargetMilestonePlannedDate(request.targetMilestonePlannedDate());
    entity.setPreviousBudgetAmount(request.previousBudgetAmount());
    entity.setRequestedBudgetAmount(request.requestedBudgetAmount());
    entity.setNewPmUserId(request.newPmUserId());
    entity.setAssetDisposalConfirmed(request.assetDisposalConfirmed());
    entity.setArchiveConfirmed(request.archiveConfirmed());
    entity.setCreatedAt(Instant.now());
    entity.setUpdatedAt(Instant.now());

    changeRequestRepository.save(entity);
    return toResponse(entity);
  }

  public List<ProjectChangeRequestDto.ProjectChangeRequestResponse> listProjectChangeRequests(long projectId) {
    return changeRequestRepository.findByProjectIdOrderByRequestedAtDesc(projectId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public ProjectChangeRequestDto.ProjectChangeRequestResponse approveChangeRequest(
      long projectId,
      long changeRequestId,
      ProjectChangeRequestDecisionRequest request,
      String username) {
    ensurePmc(username);
    ProjectChangeRequestEntity entity = findChangeRequest(projectId, changeRequestId);
    if (entity.getStatus() != Enums.ApprovalStatus.SUBMITTED) {
      throw new ApiException(409, "仅允许审批待审状态的变更申请");
    }
    entity.setStatus(Enums.ApprovalStatus.APPROVED);
    entity.setPmcDecision(Enums.PmcDecision.APPROVE);
    entity.setPmcDecisionText(request.decisionText());
    entity.setPmcDecidedAt(LocalDateTime.now());
    entity.setPmcDecidedBy(request.actorUserId());
    entity.setUpdatedAt(Instant.now());

    applyApprovedChange(entity);
    createSnapshotVersionIfNeeded(entity);
    changeRequestRepository.save(entity);
    return toResponse(entity);
  }

  @Transactional
  public ProjectChangeRequestDto.ProjectChangeRequestResponse rejectChangeRequest(
      long projectId,
      long changeRequestId,
      ProjectChangeRequestDecisionRequest request,
      String username) {
    ensurePmc(username);
    ProjectChangeRequestEntity entity = findChangeRequest(projectId, changeRequestId);
    if (entity.getStatus() != Enums.ApprovalStatus.SUBMITTED) {
      throw new ApiException(409, "仅允许拒绝待审状态的变更申请");
    }
    entity.setStatus(Enums.ApprovalStatus.REJECTED);
    entity.setPmcDecision(Enums.PmcDecision.REJECT);
    entity.setPmcDecisionText(request.decisionText());
    entity.setPmcDecidedAt(LocalDateTime.now());
    entity.setPmcDecidedBy(request.actorUserId());
    entity.setUpdatedAt(Instant.now());
    changeRequestRepository.save(entity);
    return toResponse(entity);
  }

  @Transactional
  public ProjectChangeRequestDto.ProjectChangeRequestResponse terminateProject(
      long projectId,
      ProjectChangeRequestDto.ProjectChangeRequestCreateRequest request,
      String username) {
    ensurePmc(username);
    request = new ProjectChangeRequestDto.ProjectChangeRequestCreateRequest(
        request.requestedBy(),
        Enums.ChangeType.PAUSE_TERMINATE,
        request.reasonText(),
        request.beforeText(),
        request.afterText(),
        request.impactMilestoneText(),
        request.impactBudgetText(),
        request.impactResourceText(),
        request.targetMilestoneId(),
        request.targetMilestonePlannedDate(),
        request.previousBudgetAmount(),
        request.requestedBudgetAmount(),
        request.newPmUserId(),
        Boolean.TRUE,
        Boolean.TRUE);
    ProjectChangeRequestDto.ProjectChangeRequestResponse response = createChangeRequest(projectId, request);
    approveChangeRequest(projectId, response.id(), new ProjectChangeRequestDecisionRequest(request.requestedBy(), ProjectChangeRequestDecisionRequest.Decision.APPROVE, "PMC批准终止；请在一个月内完成资产处置和归档确认。"), username);
    return response;
  }

  private void validateMilestoneScheduleRequest(ProjectChangeRequestDto.ProjectChangeRequestCreateRequest request) {
    if (request.targetMilestoneId() == null || request.targetMilestonePlannedDate() == null) {
      throw new ApiException(400, "里程碑变更必须包含目标里程碑和新的计划日期");
    }
    Long targetId = request.targetMilestoneId();
    ProjectMilestoneEntity milestone = milestoneRepository.findById(targetId)
        .orElseThrow(() -> new ApiException(404, "里程碑不存在: id=" + targetId));
    if (milestone.getPlannedDate() != null) {
      long delayMonths = ChronoUnit.MONTHS.between(milestone.getPlannedDate(), request.targetMilestonePlannedDate());
      if (delayMonths > 3) {
        // 已经是变更申请流程，不做直接修改；仅记录触发条件
      }
    }
  }

  private void validateBudgetChangeRequest(ProjectChangeRequestDto.ProjectChangeRequestCreateRequest request) {
    if (request.requestedBudgetAmount() == null || request.previousBudgetAmount() == null) {
      throw new ApiException(400, "预算变更申请必须包含当前预算和申请预算金额");
    }
    BigDecimal oldBudget = request.previousBudgetAmount();
    if (oldBudget.signum() <= 0) {
      throw new ApiException(400, "当前预算必须是正数");
    }
    BigDecimal delta = request.requestedBudgetAmount().subtract(oldBudget).abs();
    BigDecimal ratio = delta.divide(oldBudget, 6, RoundingMode.HALF_UP);
    if (ratio.compareTo(BigDecimal.valueOf(0.10)) <= 0) {
      // 小于或等于 10% 的预算调整可由常规预算流程处理，但仍允许提交审批记录
    }
  }

  private void ensurePmc(String username) {
    List<String> roles = userService.getUserRoles(username);
    if (!roles.contains("ROLE_PMC")) {
      throw new ApiException(403, "仅限 PMC 成员执行该操作");
    }
  }

  private ProjectChangeRequestEntity findChangeRequest(long projectId, long changeRequestId) {
    return changeRequestRepository
        .findByIdAndProjectId(changeRequestId, projectId)
        .orElseThrow(() -> new ApiException(404, "变更申请不存在: id=" + changeRequestId));
  }

  private void applyApprovedChange(ProjectChangeRequestEntity entity) {
    Long pid = entity.getProjectId();
    ProjectEntity project = projectRepository.findById(pid)
        .orElseThrow(() -> new ApiException(404, "项目不存在: id=" + pid));

    if (entity.getChangeType() == Enums.ChangeType.BUDGET && entity.getRequestedBudgetAmount() != null) {
      applyBudgetAdjustment(entity);
    }
    if (entity.getChangeType() == Enums.ChangeType.MILESTONE_SCHEDULE && entity.getTargetMilestoneId() != null && entity.getTargetMilestonePlannedDate() != null) {
      Long targetId = entity.getTargetMilestoneId();
      ProjectMilestoneEntity milestone = milestoneRepository.findById(targetId)
          .orElseThrow(() -> new ApiException(404, "里程碑不存在: id=" + targetId));
      milestone.setPlannedDate(entity.getTargetMilestonePlannedDate());
      milestone.setUpdatedAt(Instant.now());
      milestoneRepository.save(milestone);
    }
    if (entity.getChangeType() == Enums.ChangeType.OWNER_PM && entity.getNewPmUserId() != null) {
      project.setPmUserId(entity.getNewPmUserId());
    }
    if (entity.getChangeType() == Enums.ChangeType.OBJECTIVE_SCOPE && entity.getAfterText() != null && !entity.getAfterText().isBlank()) {
      project.setTppSummary(entity.getAfterText());
    }
    if (entity.getChangeType() == Enums.ChangeType.PAUSE_TERMINATE) {
      project.setStatus(Enums.ProjectStatus.TERMINATED);
      String terminationNote = entity.getPmcDecisionText();
      if (terminationNote == null || terminationNote.isBlank()) {
        terminationNote = "PMC批准终止，系统自动生成终止任务清单。";
      }
      project.setTerminatedReason(terminationNote);
    }
    project.setUpdatedAt(Instant.now());
    projectRepository.save(project);
  }

  private void applyBudgetAdjustment(ProjectChangeRequestEntity entity) {
    if (entity.getTargetMilestoneId() == null) {
      return;
    }
    Long targetId = entity.getTargetMilestoneId();
    MilestoneDefEntity def = milestoneDefRepository.findById(targetId)
        .orElseThrow(() -> new ApiException(404, "里程碑不存在: id=" + targetId));
    String milestoneCode = def.getMilestoneCode();

    BudgetLimitEntity limit = budgetLimitRepository.findByProjectIdAndMilestoneCode(entity.getProjectId(), milestoneCode)
        .orElseGet(() -> {
          BudgetLimitEntity fallback = new BudgetLimitEntity();
          fallback.setProjectId(entity.getProjectId());
          fallback.setMilestoneCode(milestoneCode);
          fallback.setCreatedAt(Instant.now());
          fallback.setCreatedBy(entity.getRequestedBy());
          return fallback;
        });
    limit.setApprovedBudget(entity.getRequestedBudgetAmount());
    limit.setUpdatedBy(entity.getPmcDecidedBy());
    limit.setUpdatedAt(Instant.now());
    budgetLimitRepository.save(limit);
  }

  private void createSnapshotVersionIfNeeded(ProjectChangeRequestEntity entity) {
    Long pid = entity.getProjectId();
    ProjectEntity project = projectRepository.findById(pid)
        .orElseThrow(() -> new ApiException(404, "项目不存在: id=" + pid));

    ProjectBudgetPlanEntity latest = budgetPlanRepository.findTopByProjectIdOrderByVersionNoDesc(project.getId())
        .orElseGet(() -> createBaselinePlan(project, entity.getPmcDecidedBy()));

    ProjectBudgetPlanEntity version = new ProjectBudgetPlanEntity();
    version.setProjectId(project.getId());
    version.setPlanType(latest.getPlanType());
    version.setFiscalYear(latest.getFiscalYear());
    version.setStageFromMilestoneId(latest.getStageFromMilestoneId());
    version.setStageToMilestoneId(latest.getStageToMilestoneId());
    version.setVersionNo(latest.getVersionNo() == null ? 2 : latest.getVersionNo() + 1);
    version.setInternalAmount(latest.getInternalAmount());
    version.setExternalAmount(latest.getExternalAmount());
    version.setApprovedStatus(Enums.ApprovalStatus.APPROVED);
    version.setApprovedAt(LocalDateTime.now());
    version.setApprovedBy(entity.getPmcDecidedBy());
    version.setNotes("变更批准后自动生成计划版本快照，来源变更请求 id=" + entity.getId());
    version.setCreatedBy(entity.getPmcDecidedBy());
    version.setCreatedAt(Instant.now());
    version.setUpdatedBy(entity.getPmcDecidedBy());
    version.setUpdatedAt(Instant.now());
    budgetPlanRepository.save(version);
  }

  private ProjectBudgetPlanEntity createBaselinePlan(ProjectEntity project, Long actorUserId) {
    ProjectBudgetPlanEntity baseline = new ProjectBudgetPlanEntity();
    baseline.setProjectId(project.getId());
    baseline.setPlanType(Enums.BudgetPlanType.LIFECYCLE);
    baseline.setVersionNo(1);
    baseline.setInternalAmount(BigDecimal.ZERO);
    baseline.setExternalAmount(BigDecimal.ZERO);
    baseline.setApprovedStatus(Enums.ApprovalStatus.APPROVED);
    baseline.setApprovedAt(LocalDateTime.now());
    baseline.setApprovedBy(actorUserId);
    baseline.setNotes("项目基线计划：G0 决策通过后自动保存。");
    baseline.setCreatedBy(actorUserId);
    baseline.setCreatedAt(Instant.now());
    baseline.setUpdatedBy(actorUserId);
    baseline.setUpdatedAt(Instant.now());
    return budgetPlanRepository.save(baseline);
  }

  public record BaselineDeviation(BigDecimal baselinePlannedAmount, BigDecimal currentSpent, BigDecimal varianceAmount, BigDecimal variancePercent) {}

  private ProjectChangeRequestDto.ProjectChangeRequestResponse toResponse(ProjectChangeRequestEntity entity) {
    return new ProjectChangeRequestDto.ProjectChangeRequestResponse(
        entity.getId(),
        entity.getProjectId(),
        entity.getChangeType(),
        entity.getStatus().name(),
        entity.getReasonText(),
        entity.getBeforeText(),
        entity.getAfterText(),
        entity.getImpactMilestoneText(),
        entity.getImpactBudgetText(),
        entity.getImpactResourceText(),
        entity.getTargetMilestoneId(),
        entity.getTargetMilestonePlannedDate(),
        entity.getPreviousBudgetAmount(),
        entity.getRequestedBudgetAmount(),
        entity.getNewPmUserId(),
        entity.getAssetDisposalConfirmed(),
        entity.getArchiveConfirmed(),
        entity.getPmcDecision() == null ? null : entity.getPmcDecision().name(),
        entity.getPmcDecisionText(),
        entity.getRequestedAt(),
        entity.getPmcDecidedAt());
  }
}
