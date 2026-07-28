package com.kbd.pms.service;

import com.kbd.pms.PmsConstants;
import com.kbd.pms.dto.DashboardStats;
import com.kbd.pms.dto.InitiationReportResponse;
import com.kbd.pms.dto.ProjectCreateRequest;
import com.kbd.pms.dto.ProjectDetailResponse;
import com.kbd.pms.dto.ProjectDetailResponse.BudgetExecutionSummaryDto;
import com.kbd.pms.dto.ProjectDetailResponse.CurrentMilestoneDto;
import com.kbd.pms.dto.ProjectDetailResponse.ProcessOversightDeptDto;
import com.kbd.pms.dto.ProjectUpdateRequest;
import com.kbd.pms.entity.Enums;
import com.kbd.pms.entity.IamUserEntity;
import com.kbd.pms.entity.MilestoneDefEntity;
import com.kbd.pms.entity.OrgDepartmentEntity;
import com.kbd.pms.entity.ProjectBudgetPolicyEntity;
import com.kbd.pms.entity.ProjectEntity;
import com.kbd.pms.entity.ProjectLevelEntity;
import com.kbd.pms.entity.ProjectMilestoneEntity;
import com.kbd.pms.exception.ApiException;
import com.kbd.pms.repository.BudgetLimitRepository;
import com.kbd.pms.repository.DocumentRepository;
import com.kbd.pms.repository.IamUserRepository;
import com.kbd.pms.repository.MilestoneDefRepository;
import com.kbd.pms.repository.MilestoneDeptRoleRepository;
import com.kbd.pms.repository.MilestoneHistoryRepository;
import com.kbd.pms.repository.OrgDepartmentRepository;
import com.kbd.pms.repository.ProjectBudgetLedgerRepository;
import com.kbd.pms.repository.ProjectBudgetPlanRepository;
import com.kbd.pms.repository.ProjectBudgetPolicyRepository;
import com.kbd.pms.repository.ProjectBudgetSnapshotRepository;
import com.kbd.pms.repository.ProjectChangeRequestRepository;
import com.kbd.pms.repository.ProjectDocumentRepository;
import com.kbd.pms.repository.ProjectLevelRepository;
import com.kbd.pms.repository.ProjectMilestoneRepository;
import com.kbd.pms.repository.ProjectRepository;
import com.kbd.pms.repository.ProjectTeamMemberRepository;
import com.kbd.pms.repository.ProjectTerminationTaskRepository;
import com.kbd.pms.repository.ReviewApprovalRepository;
import com.kbd.pms.repository.ReviewRecordRepository;
import com.kbd.pms.repository.ReviewApprovalTaskRepository;
import com.kbd.pms.repository.InitiationApprovalTaskRepository;
import com.kbd.pms.repository.InitiationApprovalRepository;
import com.kbd.pms.entity.ReviewApprovalTaskEntity;
import com.kbd.pms.entity.InitiationApprovalTaskEntity;
import com.kbd.pms.entity.InitiationApprovalEntity;
import com.kbd.pms.entity.ReviewApprovalEntity;
import com.kbd.pms.workflow.WfProcessDefinition;
import com.kbd.pms.workflow.WfProcessNode;
import com.kbd.pms.workflow.WfProcessRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.time.Instant;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("null")
public class ProjectService {

  private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

  private final ProjectRepository projectRepository;
  private final ProjectLevelRepository projectLevelRepository;
  private final MilestoneDefRepository milestoneDefRepository;
  private final MilestoneDeptRoleRepository milestoneDeptRoleRepository;
  private final ProjectMilestoneRepository projectMilestoneRepository;
  private final ProjectBudgetPolicyRepository projectBudgetPolicyRepository;
  private final ProjectBudgetSnapshotRepository projectBudgetSnapshotRepository;
  private final OrgDepartmentRepository orgDepartmentRepository;
  private final IamUserRepository iamUserRepository;
  private final UserService userService;
  private final BudgetLimitRepository budgetLimitRepository;
  private final DocumentRepository documentRepository;
  private final MilestoneHistoryRepository milestoneHistoryRepository;
  private final ProjectBudgetLedgerRepository projectBudgetLedgerRepository;
  private final ProjectBudgetPlanRepository projectBudgetPlanRepository;
  private final ProjectChangeRequestRepository projectChangeRequestRepository;
  private final ProjectDocumentRepository projectDocumentRepository;
  private final ProjectTeamMemberRepository projectTeamMemberRepository;
  private final ProjectTerminationTaskRepository projectTerminationTaskRepository;
  private final ReviewApprovalRepository reviewApprovalRepository;
  private final ReviewRecordRepository reviewRecordRepository;
  private final ReviewApprovalTaskRepository reviewApprovalTaskRepository;
  private final InitiationApprovalTaskRepository initiationApprovalTaskRepository;
  private final InitiationApprovalRepository initiationApprovalRepository;
  private final WfProcessRepository wfProcessRepository;
  private final SecurityHelper securityHelper;
  private final com.kbd.pms.repository.UserRepository userRepository;
  private final NotificationService notificationService;

  public ProjectService(
      ProjectRepository projectRepository,
      ProjectLevelRepository projectLevelRepository,
      MilestoneDefRepository milestoneDefRepository,
      MilestoneDeptRoleRepository milestoneDeptRoleRepository,
      ProjectMilestoneRepository projectMilestoneRepository,
      ProjectBudgetPolicyRepository projectBudgetPolicyRepository,
      ProjectBudgetSnapshotRepository projectBudgetSnapshotRepository,
      OrgDepartmentRepository orgDepartmentRepository,
      IamUserRepository iamUserRepository,
      UserService userService,
      BudgetLimitRepository budgetLimitRepository,
      DocumentRepository documentRepository,
      MilestoneHistoryRepository milestoneHistoryRepository,
      ProjectBudgetLedgerRepository projectBudgetLedgerRepository,
      ProjectBudgetPlanRepository projectBudgetPlanRepository,
      ProjectChangeRequestRepository projectChangeRequestRepository,
      ProjectDocumentRepository projectDocumentRepository,
      ProjectTeamMemberRepository projectTeamMemberRepository,
      ProjectTerminationTaskRepository projectTerminationTaskRepository,
      ReviewApprovalRepository reviewApprovalRepository,
      ReviewRecordRepository reviewRecordRepository,
      ReviewApprovalTaskRepository reviewApprovalTaskRepository,
      InitiationApprovalTaskRepository initiationApprovalTaskRepository,
      InitiationApprovalRepository initiationApprovalRepository,
      WfProcessRepository wfProcessRepository,
      SecurityHelper securityHelper,
      com.kbd.pms.repository.UserRepository userRepository,
      NotificationService notificationService) {
    this.projectRepository = projectRepository;
    this.projectLevelRepository = projectLevelRepository;
    this.milestoneDefRepository = milestoneDefRepository;
    this.milestoneDeptRoleRepository = milestoneDeptRoleRepository;
    this.projectMilestoneRepository = projectMilestoneRepository;
    this.projectBudgetPolicyRepository = projectBudgetPolicyRepository;
    this.projectBudgetSnapshotRepository = projectBudgetSnapshotRepository;
    this.orgDepartmentRepository = orgDepartmentRepository;
    this.iamUserRepository = iamUserRepository;
    this.userService = userService;
    this.budgetLimitRepository = budgetLimitRepository;
    this.documentRepository = documentRepository;
    this.milestoneHistoryRepository = milestoneHistoryRepository;
    this.projectBudgetLedgerRepository = projectBudgetLedgerRepository;
    this.projectBudgetPlanRepository = projectBudgetPlanRepository;
    this.projectChangeRequestRepository = projectChangeRequestRepository;
    this.projectDocumentRepository = projectDocumentRepository;
    this.projectTeamMemberRepository = projectTeamMemberRepository;
    this.projectTerminationTaskRepository = projectTerminationTaskRepository;
    this.reviewApprovalRepository = reviewApprovalRepository;
    this.reviewRecordRepository = reviewRecordRepository;
    this.reviewApprovalTaskRepository = reviewApprovalTaskRepository;
    this.initiationApprovalTaskRepository = initiationApprovalTaskRepository;
    this.initiationApprovalRepository = initiationApprovalRepository;
    this.wfProcessRepository = wfProcessRepository;
    this.securityHelper = securityHelper;
    this.userRepository = userRepository;
    this.notificationService = notificationService;
  }

  @Transactional
  public ProjectDetailResponse createProject(ProjectCreateRequest request, String username) {
    String levelCode = request.levelCode().trim();
    ProjectLevelEntity level =
        projectLevelRepository
            .findByLevelCode(levelCode)
            .orElseThrow(() -> new ApiException(404, "未知的项目分级代号: " + levelCode));

    OrgDepartmentEntity oversightDept =
        orgDepartmentRepository
            .findByDeptCode(PmsConstants.EFFICIENCY_MANAGEMENT_DEPT_CODE)
            .orElseThrow(
                () ->
                    new ApiException(
                        500,
                        "未找到效率管理部部门记录，请确认已初始化 org_department.dept_code="
                            + PmsConstants.EFFICIENCY_MANAGEMENT_DEPT_CODE));

    MilestoneDefEntity g0 =
        milestoneDefRepository
            .findByMilestoneCode(PmsConstants.MILESTONE_CODE_G0)
            .orElseThrow(() -> new ApiException(500, "里程碑字典缺少 G0，请检查 milestone_def 种子数据"));

    String projectNo = allocateNextProjectNo();
    String projectCode = level.getLevelCode() + "-" + projectNo;
    if (projectRepository.findByProjectCode(projectCode).isPresent()) {
      throw new ApiException(409, "项目编号冲突，请重试: " + projectCode);
    }

    // ===== 确保所有需要引用 iam_user 外键的用户ID都在 iam_user 表中存在 =====
    // 1. 当前登录用户（创建者）
    Long iamUserId = securityHelper.getCurrentUserId(); // user表与iam_user表的ID设计为一致
    ensureIamUserExists(iamUserId, username);
    // 2. 项目经理（pmUserId）——如果不同且未同步，也自动同步
    if (request.pmUserId() != null && !request.pmUserId().equals(iamUserId)) {
      ensureIamUserExists(request.pmUserId(), null);
    }

    Instant now = Instant.now();
    ProjectEntity project = new ProjectEntity();
    project.setProjectNo(projectNo);
    project.setLevelId(level.getId());
    project.setProjectCode(projectCode);
    project.setProjectName(request.projectName().trim());
    project.setIndication(request.indication() == null ? null : request.indication().trim());
    project.setTargetPathway(
        request.targetPathway() == null ? null : request.targetPathway().trim());
    project.setTppSummary(request.tppSummary());
    project.setDescription(request.description());
    project.setMechanism(request.mechanism());
    project.setUnmetNeeds(request.unmetNeeds());
    project.setScientificBasis(request.scientificBasis());
    project.setExpectedIndication(request.expectedIndication());
    project.setAdministrationRoute(request.administrationRoute());
    project.setDosageForm(request.dosageForm());
    project.setDosageFrequency(request.dosageFrequency());
    project.setEfficacyTarget(request.efficacyTarget());
    project.setSafetyAdvantage(request.safetyAdvantage());
    project.setDifferentiation(request.differentiation());
    project.setBudgetTotal(request.budgetTotal());
    project.setPlannedPccDate(request.plannedPccDate());
    project.setPlannedIndDate(request.plannedIndDate());
    project.setPlannedNdaDate(request.plannedNdaDate());
    project.setPlannedEndDate(request.plannedEndDate());
    project.setBudgetToPcc(request.budgetToPcc());
    project.setRiskScientific(request.riskScientific());
    project.setRiskCompetitive(request.riskCompetitive());
    project.setRiskRegulatory(request.riskRegulatory());
    project.setSuggestionAndSupport(request.suggestionAndSupport());
    project.setPmUserId(request.pmUserId());
    project.setProcessOversightDeptId(oversightDept.getId());
    project.setCurrentMilestoneId(g0.getId());
    // 判断是否为管理员（非PM）创建：如果创建者与项目经理不同，且项目信息不完整 -> DRAFT状态
    boolean isAdminCreated = request.createdByUserId() != null
        && request.pmUserId() != null
        && !request.createdByUserId().equals(request.pmUserId())
        && (request.indication() == null || request.indication().isEmpty());
    if (isAdminCreated) {
      // 管理员创建项目（仅基本信息），状态设为 DRAFT，等待 PM 完善
      project.setStatus(Enums.ProjectStatus.DRAFT);
    } else {
      // PM 自己创建或有完整信息，直接 ACTIVE
      project.setStatus(Enums.ProjectStatus.ACTIVE);
    }
    // 使用服务端安全上下文获取的当前用户ID，而非客户端传值
    project.setCreatedBy(iamUserId);
    project.setUpdatedBy(iamUserId);
    project.setCreatedAt(now);
    project.setUpdatedAt(now);

    ProjectEntity saved = projectRepository.save(project);

    List<MilestoneDefEntity> defs = milestoneDefRepository.findAllByIsActiveTrueOrderBySortNoAsc();
    for (MilestoneDefEntity def : defs) {
      ProjectMilestoneEntity row = new ProjectMilestoneEntity();
      row.setProjectId(saved.getId());
      row.setMilestoneId(def.getId());
      row.setStatus(
          def.getMilestoneCode().equals(PmsConstants.MILESTONE_CODE_G0)
              ? Enums.ProjectMilestoneStatus.IN_PROGRESS
              : Enums.ProjectMilestoneStatus.NOT_STARTED);
      row.setCreatedAt(now);
      row.setUpdatedAt(now);
      projectMilestoneRepository.save(row);
    }

    ProjectBudgetPolicyEntity policy = new ProjectBudgetPolicyEntity();
    policy.setProjectId(saved.getId());
    policy.setYellowThreshold(new BigDecimal("0.8000"));
    policy.setRedThreshold(new BigDecimal("0.9500"));
    policy.setCurrencyCode("CNY");
    policy.setCreatedAt(now);
    policy.setUpdatedAt(now);
    projectBudgetPolicyRepository.save(policy);

    return getProjectDetail(saved.getId(), username);
  }

  @Transactional
  public ProjectDetailResponse updateProject(long projectId, ProjectUpdateRequest request, String username) {
    ProjectEntity project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ApiException(404, "项目不存在: id=" + projectId));

    String levelCode = request.levelCode().trim();
    ProjectLevelEntity level = projectLevelRepository.findByLevelCode(levelCode)
        .orElseThrow(() -> new ApiException(404, "未知的项目分级代号: " + levelCode));

    project.setProjectName(request.projectName().trim());
    project.setLevelId(level.getId());
    project.setIndication(request.indication() == null ? null : request.indication().trim());
    project.setTargetPathway(request.targetPathway() == null ? null : request.targetPathway().trim());
    project.setTppSummary(request.tppSummary());
    project.setDescription(request.description());
    project.setMechanism(request.mechanism());
    project.setUnmetNeeds(request.unmetNeeds());
    project.setScientificBasis(request.scientificBasis());
    project.setExpectedIndication(request.expectedIndication());
    project.setAdministrationRoute(request.administrationRoute());
    project.setDosageForm(request.dosageForm());
    project.setDosageFrequency(request.dosageFrequency());
    project.setEfficacyTarget(request.efficacyTarget());
    project.setSafetyAdvantage(request.safetyAdvantage());
    project.setDifferentiation(request.differentiation());
    project.setBudgetTotal(request.budgetTotal());
    project.setPlannedPccDate(request.plannedPccDate());
    project.setPlannedIndDate(request.plannedIndDate());
    project.setPlannedNdaDate(request.plannedNdaDate());
    project.setPlannedEndDate(request.plannedEndDate());
    project.setBudgetToPcc(request.budgetToPcc());
    project.setRiskScientific(request.riskScientific());
    project.setRiskCompetitive(request.riskCompetitive());
    project.setRiskRegulatory(request.riskRegulatory());
    project.setSuggestionAndSupport(request.suggestionAndSupport());
    // 如果项目处于 DRAFT 状态且 PM 完善了信息（有 indication），自动转为 ACTIVE
    boolean wasDraft = project.getStatus() == Enums.ProjectStatus.DRAFT;
    if (wasDraft
        && request.indication() != null
        && !request.indication().isEmpty()) {
      project.setStatus(Enums.ProjectStatus.ACTIVE);
    }
    // 不更新 updated_by（外键引用 iam_user 表），仅更新 updated_at
    project.setUpdatedAt(Instant.now());

    projectRepository.save(project);

    // 触发器：项目从 DRAFT 转为 ACTIVE → 通知当前里程碑（G0）的上传部门执行人
    if (wasDraft && project.getStatus() == Enums.ProjectStatus.ACTIVE) {
      try {
        notifyExecutorsForProjectActivation(project);
      } catch (Exception e) {
        // 通知发送失败不应影响主业务流程，但记录日志用于调试
        log.error("通知发送失败: projectId={}", project.getId(), e);
      }
    }

    return getProjectDetail(projectId, username);
  }

  @Transactional
  public void deleteProject(long projectId, String username) {
    ProjectEntity project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ApiException(404, "项目不存在: id=" + projectId));

    List<String> permissions = userService.getUserPermissions(username);

    // 仅拥有 PERMISSION_DELETE_PROJECT 权限的用户可以删除项目
    if (!permissions.contains("PERMISSION_DELETE_PROJECT")) {
      throw new ApiException(403, "无删除项目的权限");
    }

    // 级联删除所有关联子表记录（按外键依赖顺序，先删有外键引用的子表）
    reviewRecordRepository.deleteByProjectId(projectId);
    // 先删 review_approval_task（子表），再删 review_approval（父表）
    List<ReviewApprovalEntity> reviewApprovals = reviewApprovalRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    for (ReviewApprovalEntity ra : reviewApprovals) {
      reviewApprovalTaskRepository.deleteByReviewApprovalId(ra.getId());
    }
    reviewApprovalRepository.deleteByProjectId(projectId);
    // 先删 initiation_approval_task（子表），再删 initiation_approval（父表）
    List<InitiationApprovalEntity> initiationApprovals = initiationApprovalRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    for (InitiationApprovalEntity ia : initiationApprovals) {
      initiationApprovalTaskRepository.deleteByInitiationApprovalId(ia.getId());
    }
    initiationApprovalRepository.deleteByProjectId(projectId);
    projectTerminationTaskRepository.deleteByProjectId(projectId);
    projectTeamMemberRepository.deleteByProjectId(projectId);
    milestoneHistoryRepository.deleteByProjectId(projectId);
    projectMilestoneRepository.deleteByProjectId(projectId);
    projectDocumentRepository.deleteByProjectId(projectId);
    projectChangeRequestRepository.deleteByProjectId(projectId);
    projectBudgetSnapshotRepository.deleteByProjectId(projectId);
    projectBudgetPlanRepository.deleteByProjectId(projectId);
    projectBudgetPolicyRepository.deleteByProjectId(projectId);
    projectBudgetLedgerRepository.deleteByProjectId(projectId);
    documentRepository.deleteByProjectId(projectId);
    budgetLimitRepository.deleteByProjectId(projectId);

    projectRepository.delete(project);
  }

  @Transactional(readOnly = true)
  public ProjectDetailResponse getProjectDetail(long projectId) {
    ProjectEntity project =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> new ApiException(404, "项目不存在: id=" + projectId));

    ProjectLevelEntity level =
        projectLevelRepository
            .findById(project.getLevelId())
            .orElseThrow(() -> new ApiException(500, "项目分级数据缺失: level_id=" + project.getLevelId()));

    MilestoneDefEntity currentMilestone =
        project.getCurrentMilestoneId() == null
            ? null
            : milestoneDefRepository.findById(project.getCurrentMilestoneId()).orElse(null);

    ProcessOversightDeptDto oversightDto = null;
    if (project.getProcessOversightDeptId() != null) {
      oversightDto =
          orgDepartmentRepository
              .findById(project.getProcessOversightDeptId())
              .map(
                  d ->
                      new ProcessOversightDeptDto(
                          d.getId(), d.getDeptCode(), d.getDeptName()))
              .orElse(null);
    }

    CurrentMilestoneDto milestoneDto = null;
    String lifecyclePhaseLabel = null;
    if (currentMilestone != null) {
      List<String> executorDeptNames = buildExecutorDeptNames(currentMilestone.getMilestoneCode());
      milestoneDto =
          new CurrentMilestoneDto(
              currentMilestone.getMilestoneCode(),
              currentMilestone.getMilestoneName(),
              currentMilestone.getMilestoneCode() + "-" + currentMilestone.getMilestoneName(),
              executorDeptNames);
      lifecyclePhaseLabel = milestoneDto.phaseLabel();
    }

    BudgetExecutionSummaryDto budgetDto = buildBudgetSummary(project.getId());

    String pmUserName = project.getPmUserId() != null
        ? iamUserRepository.findById(project.getPmUserId())
            .map(IamUserEntity::getDisplayName).orElse(null)
        : null;

    return new ProjectDetailResponse(
        project.getId(),
        project.getProjectCode(),
        project.getProjectName(),
        level.getLevelCode(),
        level.getLevelName(),
        project.getIndication(),
        project.getTargetPathway(),
        project.getTppSummary(),
        project.getDescription(),
        project.getMechanism(),
        project.getUnmetNeeds(),
        project.getScientificBasis(),
        project.getExpectedIndication(),
        project.getAdministrationRoute(),
        project.getDosageForm(),
        project.getDosageFrequency(),
        project.getEfficacyTarget(),
        project.getSafetyAdvantage(),
        project.getDifferentiation(),
        project.getBudgetTotal(),
        project.getPlannedPccDate(),
        project.getPlannedIndDate(),
        project.getPlannedNdaDate(),
        project.getPlannedEndDate(),
        project.getBudgetToPcc(),
        project.getRiskScientific(),
        project.getRiskCompetitive(),
        project.getRiskRegulatory(),
        project.getSuggestionAndSupport(),
        project.getPmUserId(),
        pmUserName,
        project.getStatus().name(),
        lifecyclePhaseLabel,
        project.getInitiationStatus(),
        oversightDto,
        milestoneDto,
        budgetDto);
  }

  /**
   * 根据里程碑代码从流程引擎配置中获取执行部门名称列表（上传节点对应的部门）。
   */
  private List<String> buildExecutorDeptNames(String milestoneCode) {
    if (milestoneCode == null || milestoneCode.isEmpty()) {
      return List.of();
    }
    WfProcessDefinition def = wfProcessRepository
        .findByProcessTypeAndMilestoneCodeAndIsActiveTrue("MILESTONE", milestoneCode)
        .orElse(null);
    if (def == null) {
      return List.of();
    }
    List<String> deptNames = new ArrayList<>();
    for (WfProcessNode node : def.getNodes()) {
      if (Boolean.TRUE.equals(node.getIsUploader()) && node.getApproverValue() != null) {
        Arrays.stream(node.getApproverValue().split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .forEach(deptIdStr -> {
              try {
                Long deptId = Long.parseLong(deptIdStr);
                orgDepartmentRepository.findById(deptId)
                    .map(OrgDepartmentEntity::getDeptName)
                    .ifPresent(deptNames::add);
              } catch (NumberFormatException e) {
                // 忽略无效的部门ID
              }
            });
      }
    }
    return deptNames;
  }

  private BudgetExecutionSummaryDto buildBudgetSummary(long projectId) {
    return projectBudgetSnapshotRepository
        .findFirstByProjectIdOrderBySnapshotMonthDesc(projectId)
        .map(
            s ->
                new BudgetExecutionSummaryDto(
                    s.getPlannedTotalAmount(),
                    s.getTotalSpent(),
                    s.getUtilizationRatio(),
                    s.getWarningLevel() == null ? null : s.getWarningLevel().name(),
                    s.getSnapshotMonth()))
        .orElse(new BudgetExecutionSummaryDto(null, null, null, null, null));
  }

  public List<ProjectDetailResponse> getVisibleProjects(String username) {
    List<String> roles = userService.getUserRoles(username);
    List<ProjectEntity> projects;

    if (roles.contains("ROLE_PMC") || roles.contains("ROLE_PM") || roles.contains("ROLE_PROJECT_ADMIN")) {
      // PMC, PM and PROJECT_ADMIN can see all projects
      projects = projectRepository.findAll();
    } else {
      // Other users can only see projects they are involved in
      // For simplicity, assume based on team members, but here we return all for now
      // In real implementation, filter based on user participation
      projects = projectRepository.findAll();
    }

    return projects.stream().map(this::buildProjectDetailResponse).toList();
  }

  public DashboardStats getDashboardStats() {
    List<ProjectEntity> allProjects = projectRepository.findAll();
    long currentUserId = securityHelper.getCurrentUserId();

    int inProgressProjects = (int) allProjects.stream()
        .filter(p -> p.getStatus() == Enums.ProjectStatus.ACTIVE)
        .count();

    // Count pending milestone reviews assigned to current user
    int pendingMilestoneReviews = (int) reviewApprovalTaskRepository
        .findByApproverUserIdAndStatusOrderByCreatedAtDesc(currentUserId, ReviewApprovalTaskEntity.Status.PENDING)
        .stream()
        .filter(task -> {
          var approval = reviewApprovalRepository.findById(task.getReviewApprovalId()).orElse(null);
          return approval != null && approval.getStatus() == com.kbd.pms.entity.ReviewApprovalEntity.Status.SUBMITTED;
        })
        .count();

    // Count pending initiation approvals assigned to current user
    int pendingInitiationReviews = (int) initiationApprovalTaskRepository
        .findByApproverUserIdOrderByCreatedAtDesc(currentUserId)
        .stream()
        .filter(task -> task.getStatus() == InitiationApprovalTaskEntity.Status.PENDING)
        .filter(task -> {
          InitiationApprovalEntity approval = initiationApprovalRepository
              .findById(task.getInitiationApprovalId()).orElse(null);
          return approval != null && approval.getStatus() == InitiationApprovalEntity.Status.SUBMITTED;
        })
        .count();

    // Count budget alerts - projects where utilization ratio > 80%
    int budgetAlerts = (int) allProjects.stream()
        .filter(p -> {
          BudgetExecutionSummaryDto budget = buildBudgetSummary(p.getId());
          return budget.utilizationRatio() != null && budget.utilizationRatio().compareTo(BigDecimal.valueOf(80)) > 0;
        })
        .count();

    // Count projects pending completion (DRAFT status where current user is the PM)
    int pendingProjectCompletions = (int) allProjects.stream()
        .filter(p -> p.getStatus() == Enums.ProjectStatus.DRAFT
            && p.getPmUserId() != null
            && p.getPmUserId().longValue() == currentUserId)
        .count();

    return new DashboardStats(inProgressProjects, pendingMilestoneReviews, pendingInitiationReviews, budgetAlerts, pendingProjectCompletions);
  }

  public ProjectDetailResponse getProjectDetail(long id, String username) {
    ProjectEntity project = projectRepository.findById(id)
        .orElseThrow(() -> new ApiException(404, "项目不存在: id=" + id));

    List<String> roles = userService.getUserRoles(username);
    if (!roles.contains("ROLE_PMC") && !roles.contains("ROLE_PM")) {
      // Check if user is involved in the project
      // For simplicity, allow access, but in real implementation check team membership
    }

    return buildProjectDetailResponse(project);
  }

  private ProjectDetailResponse buildProjectDetailResponse(ProjectEntity project) {
    ProjectLevelEntity level =
        projectLevelRepository
            .findById(project.getLevelId())
            .orElseThrow(() -> new ApiException(500, "项目分级数据缺失: level_id=" + project.getLevelId()));

    MilestoneDefEntity currentMilestone =
        project.getCurrentMilestoneId() == null
            ? null
            : milestoneDefRepository.findById(project.getCurrentMilestoneId()).orElse(null);

    ProcessOversightDeptDto oversightDto = null;
    if (project.getProcessOversightDeptId() != null) {
      oversightDto =
          orgDepartmentRepository
              .findById(project.getProcessOversightDeptId())
              .map(
                  d ->
                      new ProcessOversightDeptDto(
                          d.getId(), d.getDeptCode(), d.getDeptName()))
              .orElse(null);
    }

    CurrentMilestoneDto milestoneDto = null;
    String lifecyclePhaseLabel = null;
    if (currentMilestone != null) {
      List<String> executorDeptNames = buildExecutorDeptNames(currentMilestone.getMilestoneCode());
      milestoneDto =
          new CurrentMilestoneDto(
              currentMilestone.getMilestoneCode(),
              currentMilestone.getMilestoneName(),
              currentMilestone.getMilestoneCode() + "-" + currentMilestone.getMilestoneName(),
              executorDeptNames);
      lifecyclePhaseLabel = milestoneDto.phaseLabel();
    }

    BudgetExecutionSummaryDto budgetDto = buildBudgetSummary(project.getId());

    String pmUserName = project.getPmUserId() != null
        ? iamUserRepository.findById(project.getPmUserId())
            .map(IamUserEntity::getDisplayName).orElse(null)
        : null;

    return new ProjectDetailResponse(
        project.getId(),
        project.getProjectCode(),
        project.getProjectName(),
        level.getLevelCode(),
        level.getLevelName(),
        project.getIndication(),
        project.getTargetPathway(),
        project.getTppSummary(),
        project.getDescription(),
        project.getMechanism(),
        project.getUnmetNeeds(),
        project.getScientificBasis(),
        project.getExpectedIndication(),
        project.getAdministrationRoute(),
        project.getDosageForm(),
        project.getDosageFrequency(),
        project.getEfficacyTarget(),
        project.getSafetyAdvantage(),
        project.getDifferentiation(),
        project.getBudgetTotal(),
        project.getPlannedPccDate(),
        project.getPlannedIndDate(),
        project.getPlannedNdaDate(),
        project.getPlannedEndDate(),
        project.getBudgetToPcc(),
        project.getRiskScientific(),
        project.getRiskCompetitive(),
        project.getRiskRegulatory(),
        project.getSuggestionAndSupport(),
        project.getPmUserId(),
        pmUserName,
        project.getStatus().name(),
        lifecyclePhaseLabel,
        project.getInitiationStatus(),
        oversightDto,
        milestoneDto,
        budgetDto);
  }

  /**
   * 获取立项报告数据
   */
  @Transactional(readOnly = true)
  public InitiationReportResponse getInitiationReport(long projectId) {
    ProjectEntity project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ApiException(404, "项目不存在: id=" + projectId));

    ProjectLevelEntity level = projectLevelRepository.findById(project.getLevelId())
        .orElseThrow(() -> new ApiException(500, "项目分级数据缺失: level_id=" + project.getLevelId()));

    // 查询发起人名称
    String initiatorName = "";
    if (project.getInitiatorUserId() != null) {
      initiatorName = iamUserRepository.findById(project.getInitiatorUserId())
          .map(IamUserEntity::getDisplayName)
          .orElse("未知用户");
    } else if (project.getPmUserId() != null) {
      // 兼容旧数据：如果 initiatorUserId 为空，使用 PM 作为发起人
      initiatorName = iamUserRepository.findById(project.getPmUserId())
          .map(IamUserEntity::getDisplayName)
          .orElse("未知用户");
    }

    String initiationTime = project.getReviewSubmittedAt() != null
        ? project.getReviewSubmittedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        : (project.getCreatedAt() != null
            ? java.time.LocalDateTime.ofInstant(project.getCreatedAt(), java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            : "");

    return new InitiationReportResponse(
        project.getId(),
        project.getProjectCode(),
        project.getProjectName(),
        level.getLevelCode(),
        level.getLevelName(),
        project.getTargetPathway(),
        project.getIndication(),
        project.getTppSummary(),
        project.getDescription(),
        project.getMechanism(),
        project.getUnmetNeeds(),
        project.getScientificBasis(),
        project.getExpectedIndication(),
        project.getAdministrationRoute(),
        project.getDosageForm(),
        project.getDosageFrequency(),
        project.getEfficacyTarget(),
        project.getSafetyAdvantage(),
        project.getDifferentiation(),
        project.getBudgetTotal(),
        project.getPlannedPccDate(),
        project.getPlannedIndDate(),
        project.getPlannedNdaDate(),
        project.getBudgetToPcc(),
        project.getRiskScientific(),
        project.getRiskCompetitive(),
        project.getRiskRegulatory(),
        project.getSuggestionAndSupport(),
        initiatorName,
        initiationTime
    );
  }

  /**
   * 确保指定用户ID在 iam_user 表中存在记录，如果不存在则自动从 user 表同步创建
   */
  private void ensureIamUserExists(Long userId, String username) {
    if (iamUserRepository.findById(userId).isPresent()) {
      return;
    }
    com.kbd.pms.entity.User user = null;
    if (username != null) {
      var opt = userService.findByUsername(username);
      if (opt.isPresent()) {
        user = opt.get();
      }
    } else {
      // 通过 userId 从 userRepository 查找（直接通过ID查询）
      var opt = userRepository.findById(userId);
      if (opt.isPresent()) {
        user = opt.get();
        username = user.getUsername();
      }
    }
    if (user == null) {
      return; // user 表中也没有该用户，跳过
    }
    IamUserEntity newIamUser = new IamUserEntity();
    newIamUser.setId(userId);
    newIamUser.setUserNo(username);
    newIamUser.setDisplayName(username);
    newIamUser.setEmail(user.getEmail());
    newIamUser.setIsActive(Boolean.TRUE);
    newIamUser.setCreatedAt(Instant.now());
    newIamUser.setUpdatedAt(Instant.now());
    iamUserRepository.save(newIamUser);
  }

  private String allocateNextProjectNo() {
    Long max = projectRepository.findMaxKbdNumericSuffix();
    long next = (max == null ? 0L : max) + 1L;
    return "KBD" + String.format("%04d", next);
  }

  /**
   * 触发器：项目从 DRAFT 转为 ACTIVE → 通知当前里程碑的上传部门执行人
   */
  private void notifyExecutorsForProjectActivation(ProjectEntity project) {
    // 查询当前里程碑定义
    MilestoneDefEntity currentMilestone = milestoneDefRepository
        .findById(project.getCurrentMilestoneId())
        .orElse(null);
    if (currentMilestone == null) {
      log.debug("未找到当前里程碑定义，跳过通知: projectId={}", project.getId());
      return;
    }

    String milestoneCode = currentMilestone.getMilestoneCode();
    String milestoneName = currentMilestone.getMilestoneName();
    if (milestoneCode == null) {
      log.debug("里程碑代码为空，跳过通知: projectId={}", project.getId());
      return;
    }

    // 从流程引擎获取上传节点对应的部门ID
    WfProcessDefinition def = wfProcessRepository
        .findByProcessTypeAndMilestoneCodeAndIsActiveTrue("MILESTONE", milestoneCode)
        .orElse(null);
    if (def == null) {
      log.debug("未找到里程碑 {} 的流程定义，跳过通知: projectId={}", milestoneCode, project.getId());
      return;
    }

    log.info("项目激活通知: projectCode={}, milestone={}, processDefId={}, nodes={}",
        project.getProjectCode(), milestoneCode, def.getId(), def.getNodes().size());

    String title = "项目已完善";
    String content = "[" + project.getProjectName() + "] 项目信息已完善，请上传 " + milestoneName + " 阶段交付物";

    Set<Long> notifiedDeptIds = new HashSet<>();
    for (WfProcessNode node : def.getNodes()) {
      log.debug("节点检查: code={}, isUploader={}, approverValue={}",
          node.getNodeCode(), node.getIsUploader(), node.getApproverValue());
      if (Boolean.TRUE.equals(node.getIsUploader()) && node.getApproverValue() != null) {
        for (String deptIdStr : node.getApproverValue().split(",")) {
          try {
            Long deptId = Long.parseLong(deptIdStr.trim());
            log.debug("向部门 {} 的执行人发送通知: projectId={}", deptId, project.getId());
            if (notifiedDeptIds.add(deptId)) {
              notificationService.sendNotificationToDeptExecutors(
                  deptId, "PROJECT_COMPLETION", title, content,
                  project.getId(), milestoneCode, null, true);
            }
          } catch (NumberFormatException e) {
            log.debug("无效的部门ID: {}, projectId={}", deptIdStr, project.getId());
          }
        }
      }
    }
    log.info("项目激活通知发送完成: projectId={}, 涉及部门数={}", project.getId(), notifiedDeptIds.size());
  }
}
