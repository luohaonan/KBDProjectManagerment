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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("null")
public class ProjectService {

  private final ProjectRepository projectRepository;
  private final ProjectLevelRepository projectLevelRepository;
  private final MilestoneDefRepository milestoneDefRepository;
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

  public ProjectService(
      ProjectRepository projectRepository,
      ProjectLevelRepository projectLevelRepository,
      MilestoneDefRepository milestoneDefRepository,
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
      ReviewRecordRepository reviewRecordRepository) {
    this.projectRepository = projectRepository;
    this.projectLevelRepository = projectLevelRepository;
    this.milestoneDefRepository = milestoneDefRepository;
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

    Instant now = Instant.now();
    ProjectEntity project = new ProjectEntity();
    project.setProjectNo(projectNo);
    project.setLevelId(level.getId());
    project.setProjectCode(projectCode);
    project.setProjectName(request.projectName().trim());
    project.setIndication(request.indication().trim());
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
    // 业务阶段处于 G0「项目立项」；project.status 使用 ACTIVE 表示已正式纳入管线执行（与制度"立项后进入 G0"一致）
    project.setStatus(Enums.ProjectStatus.ACTIVE);
    project.setCreatedBy(request.createdByUserId());
    project.setUpdatedBy(request.createdByUserId());
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
    // 不更新 updated_by（外键引用 iam_user 表），仅更新 updated_at
    project.setUpdatedAt(Instant.now());

    projectRepository.save(project);

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
    reviewApprovalRepository.deleteByProjectId(projectId);
    projectTerminationTaskRepository.deleteByProjectId(projectId);
    projectTeamMemberRepository.deleteByProjectId(projectId);
    projectMilestoneRepository.deleteByProjectId(projectId);
    projectDocumentRepository.deleteByProjectId(projectId);
    projectChangeRequestRepository.deleteByProjectId(projectId);
    projectBudgetSnapshotRepository.deleteByProjectId(projectId);
    projectBudgetPlanRepository.deleteByProjectId(projectId);
    projectBudgetPolicyRepository.deleteByProjectId(projectId);
    projectBudgetLedgerRepository.deleteByProjectId(projectId);
    milestoneHistoryRepository.deleteByProjectId(projectId);
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
      milestoneDto =
          new CurrentMilestoneDto(
              currentMilestone.getMilestoneCode(),
              currentMilestone.getMilestoneName(),
              currentMilestone.getMilestoneCode() + "-" + currentMilestone.getMilestoneName());
      lifecyclePhaseLabel = milestoneDto.phaseLabel();
    }

    BudgetExecutionSummaryDto budgetDto = buildBudgetSummary(project.getId());

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
        project.getStatus().name(),
        lifecyclePhaseLabel,
        project.getInitiationStatus(),
        oversightDto,
        milestoneDto,
        budgetDto);
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

    if (roles.contains("ROLE_PMC") || roles.contains("ROLE_PM")) {
      // PMC and PM can see all projects
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

    int inProgressProjects = (int) allProjects.stream()
        .filter(p -> p.getStatus() == Enums.ProjectStatus.ACTIVE)
        .count();

    // Count pending milestone reviews - projects with milestones in review status
    int pendingMilestoneReviews = (int) projectMilestoneRepository.findAll().stream()
        .filter(m -> m.getStatus() == Enums.ProjectMilestoneStatus.SUBMITTED)
        .count();

    // Count budget alerts - projects where utilization ratio > 80%
    int budgetAlerts = (int) allProjects.stream()
        .filter(p -> {
          BudgetExecutionSummaryDto budget = buildBudgetSummary(p.getId());
          return budget.utilizationRatio() != null && budget.utilizationRatio().compareTo(BigDecimal.valueOf(80)) > 0;
        })
        .count();

    return new DashboardStats(inProgressProjects, pendingMilestoneReviews, budgetAlerts);
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
      milestoneDto =
          new CurrentMilestoneDto(
              currentMilestone.getMilestoneCode(),
              currentMilestone.getMilestoneName(),
              currentMilestone.getMilestoneCode() + "-" + currentMilestone.getMilestoneName());
      lifecyclePhaseLabel = milestoneDto.phaseLabel();
    }

    BudgetExecutionSummaryDto budgetDto = buildBudgetSummary(project.getId());

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

  private String allocateNextProjectNo() {
    Long max = projectRepository.findMaxKbdNumericSuffix();
    long next = (max == null ? 0L : max) + 1L;
    return "KBD" + String.format("%04d", next);
  }
}
