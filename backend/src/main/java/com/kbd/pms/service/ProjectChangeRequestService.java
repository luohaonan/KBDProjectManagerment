package com.kbd.pms.service;

import com.kbd.pms.dto.ProjectChangeRequestDecisionRequest;
import com.kbd.pms.dto.ProjectChangeRequestDto;
import com.kbd.pms.entity.Enums;
import com.kbd.pms.entity.IamUserEntity;
import com.kbd.pms.entity.OrgDepartmentEntity;
import com.kbd.pms.entity.ProjectChangeRequestEntity;
import com.kbd.pms.entity.ProjectEntity;
import com.kbd.pms.entity.Role;
import com.kbd.pms.entity.User;
import com.kbd.pms.exception.ApiException;
import com.kbd.pms.repository.*;
import com.kbd.pms.workflow.WfProcessDefinition;
import com.kbd.pms.workflow.WfProcessNode;
import com.kbd.pms.workflow.WfProcessRepository;
import com.kbd.pms.workflow.WfProcessService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("null")
public class ProjectChangeRequestService {

  private final ProjectRepository projectRepository;
  private final ProjectChangeRequestRepository changeRequestRepository;
  private final UserRepository userRepository;
  private final IamUserRepository iamUserRepository;
  private final OrgDepartmentRepository orgDepartmentRepository;
  private final WfProcessRepository wfProcessRepository;
  private final WfProcessService wfProcessService;
  private final NotificationService notificationService;

  public ProjectChangeRequestService(
      ProjectRepository projectRepository,
      ProjectChangeRequestRepository changeRequestRepository,
      UserRepository userRepository,
      IamUserRepository iamUserRepository,
      OrgDepartmentRepository orgDepartmentRepository,
      WfProcessRepository wfProcessRepository,
      WfProcessService wfProcessService,
      NotificationService notificationService) {
    this.projectRepository = projectRepository;
    this.changeRequestRepository = changeRequestRepository;
    this.userRepository = userRepository;
    this.iamUserRepository = iamUserRepository;
    this.orgDepartmentRepository = orgDepartmentRepository;
    this.wfProcessRepository = wfProcessRepository;
    this.wfProcessService = wfProcessService;
    this.notificationService = notificationService;
  }

  /**
   * PM发起项目变更申请
   */
  @Transactional
  public ProjectChangeRequestDto submitChange(long projectId, ProjectChangeRequestDto request) {
    ProjectEntity project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ApiException(404, "项目不存在"));

    // 验证发起人是否为项目经理
    if (project.getPmUserId() == null || !project.getPmUserId().equals(request.requestedBy())) {
      throw new ApiException(403, "只有项目经理才能发起项目变更申请");
    }

    // 检查是否有进行中的变更申请
    List<ProjectChangeRequestEntity> activeList = changeRequestRepository
        .findByProjectIdOrderByRequestedAtDesc(projectId);
    for (ProjectChangeRequestEntity existing : activeList) {
      String status = existing.getStatus();
      if ("SUBMITTED".equals(status) || "EFFICIENCY_APPROVED".equals(status)) {
        throw new ApiException(409, "该项目已有进行中的变更申请");
      }
    }

    ProjectChangeRequestEntity entity = new ProjectChangeRequestEntity();
    entity.setProjectId(projectId);
    entity.setChangeType(request.changeType() != null ? Enums.ChangeType.valueOf(request.changeType()) : Enums.ChangeType.OTHER);
    entity.setReasonText(request.reasonText());
    entity.setAttachmentUri(request.attachmentUri());
    entity.setBeforeText(request.beforeText());
    entity.setAfterText(request.afterText());
    entity.setImpactMilestoneText(request.impactMilestoneText());
    entity.setImpactBudgetText(request.impactBudgetText());
    entity.setImpactResourceText(request.impactResourceText());
    entity.setRequestedBy(request.requestedBy());
    entity.setRequestedAt(LocalDateTime.now(ZoneOffset.UTC));
    entity.setStatus("SUBMITTED"); // 等待效率管理部审批
    entity.setCreatedAt(Instant.now());
    entity.setUpdatedAt(Instant.now());
    changeRequestRepository.save(entity);

    return toDto(entity);
  }

  @Transactional
  public ProjectChangeRequestDto submitBudgetChange(
      long projectId,
      java.math.BigDecimal requestedBudgetAmount,
      String reasonText,
      long requestedBy) {
    ProjectEntity project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ApiException(404, "项目不存在"));

    User requester = userRepository.findById(requestedBy)
        .orElseThrow(() -> new ApiException(404, "用户不存在"));
    boolean isProjectPm = project.getPmUserId() != null && project.getPmUserId().equals(requestedBy);
    boolean isProjectAdmin = requester.getRoles().stream()
        .map(Role::getName)
        .anyMatch(role -> "ROLE_PROJECT_ADMIN".equals(role) || "ROLE_ADMIN".equals(role));

    if (!isProjectPm && !isProjectAdmin) {
      throw new ApiException(403, "只有项目经理或项目管理员才能发起预算调整申请");
    }
    if (requestedBudgetAmount == null || requestedBudgetAmount.signum() <= 0) {
      throw new ApiException(400, "调整后预算必须大于0");
    }

    List<ProjectChangeRequestEntity> activeList = changeRequestRepository
        .findByProjectIdOrderByRequestedAtDesc(projectId);
    for (ProjectChangeRequestEntity existing : activeList) {
      String status = existing.getStatus();
      if (existing.getChangeType() == Enums.ChangeType.BUDGET
          && ("SUBMITTED".equals(status) || "EFFICIENCY_APPROVED".equals(status))) {
        throw new ApiException(409, "该项目已有进行中的预算调整申请");
      }
    }

    java.math.BigDecimal previousBudget = project.getBudgetTotal() == null
        ? java.math.BigDecimal.ZERO
        : project.getBudgetTotal();

    ProjectChangeRequestEntity entity = new ProjectChangeRequestEntity();
    entity.setProjectId(projectId);
    entity.setChangeType(Enums.ChangeType.BUDGET);
    entity.setReasonText(reasonText == null || reasonText.isBlank() ? "预算调整申请" : reasonText.trim());
    entity.setAttachmentUri(null);
    entity.setBeforeText("项目总预算：" + previousBudget);
    entity.setAfterText("项目总预算：" + requestedBudgetAmount);
    entity.setImpactBudgetText("项目总预算由 " + previousBudget + " 调整为 " + requestedBudgetAmount);
    entity.setRequestedBy(requestedBy);
    entity.setRequestedAt(LocalDateTime.now(ZoneOffset.UTC));
    entity.setStatus("SUBMITTED");
    entity.setPreviousBudgetAmount(previousBudget);
    entity.setRequestedBudgetAmount(requestedBudgetAmount);
    entity.setAdjustmentAmount(requestedBudgetAmount.subtract(previousBudget));
    entity.setCreatedAt(Instant.now());
    entity.setUpdatedAt(Instant.now());
    entity = changeRequestRepository.save(entity);

    startBudgetWorkflow(entity, project);

    return toDto(entity);
  }

  /**
   * 效率管理部部门负责人审批变更申请
   */
  @Transactional
  public ProjectChangeRequestDto approveByEfficiency(long changeId, long approverId, String decision, String opinion) {
    ProjectChangeRequestEntity entity = changeRequestRepository.findById(changeId)
        .orElseThrow(() -> new ApiException(404, "变更申请不存在"));

    if (!"SUBMITTED".equals(entity.getStatus())) {
      throw new ApiException(409, "当前状态不允许效率管理部审批: " + entity.getStatus());
    }

    // 验证用户是否为效率管理部负责人
    IamUserEntity user = iamUserRepository.findById(approverId)
        .orElseThrow(() -> new ApiException(404, "用户不存在"));
    OrgDepartmentEntity effDept = orgDepartmentRepository.findByDeptCode("ROSS_EFF").orElse(null);
    if (effDept == null || !effDept.getHeadUserId().equals(approverId)) {
      throw new ApiException(403, "只有效率管理部部门负责人才能审批");
    }

    if ("APPROVED".equals(decision)) {
      entity.setStatus("EFFICIENCY_APPROVED");
      // 进入PMC审批阶段
    } else {
      entity.setStatus("EFFICIENCY_REJECTED");
    }
    entity.setEfficiencyApproverId(approverId);
    entity.setEfficiencyOpinion(opinion);
    entity.setEfficiencyDecidedAt(LocalDateTime.now(ZoneOffset.UTC));
    entity.setUpdatedAt(Instant.now());
    changeRequestRepository.save(entity);

    return toDto(entity);
  }

  /**
   * PMC审批变更申请
   */
  @Transactional
  public ProjectChangeRequestDto approveByPmc(long changeId, long approverId, ProjectChangeRequestDecisionRequest request) {
    ProjectChangeRequestEntity entity = changeRequestRepository.findById(changeId)
        .orElseThrow(() -> new ApiException(404, "变更申请不存在"));

    if (!"EFFICIENCY_APPROVED".equals(entity.getStatus())) {
      throw new ApiException(409, "当前状态不允许PMC审批: " + entity.getStatus());
    }

    switch (request.decision()) {
      case APPROVE -> {
        entity.setStatus("APPROVED");
        entity.setPmcDecision(Enums.PmcDecision.APPROVE);
        // 应用变更到项目
        applyChange(entity);
      }
      case REJECT -> {
        entity.setStatus("REJECTED");
        entity.setPmcDecision(Enums.PmcDecision.REJECT);
      }
      case CONDITIONAL_APPROVE -> {
        entity.setStatus("APPROVED");
        entity.setPmcDecision(Enums.PmcDecision.CONDITIONAL_APPROVE);
        applyChange(entity);
      }
    }

    entity.setPmcDecisionText(request.opinion());
    entity.setPmcDecidedAt(LocalDateTime.now(ZoneOffset.UTC));
    entity.setPmcDecidedBy(approverId);
    entity.setUpdatedAt(Instant.now());
    changeRequestRepository.save(entity);

    return toDto(entity);
  }

  /**
   * 将变更应用到项目
   */
  private void applyChange(ProjectChangeRequestEntity entity) {
    ProjectEntity project = projectRepository.findById(entity.getProjectId())
        .orElseThrow(() -> new ApiException(500, "项目不存在"));

    switch (entity.getChangeType()) {
      case MILESTONE_SCHEDULE -> {
        if (entity.getTargetMilestonePlannedDate() != null) {
          project.setPlannedPccDate(entity.getTargetMilestonePlannedDate());
        }
      }
      case BUDGET -> {
        if (entity.getRequestedBudgetAmount() != null) {
          project.setBudgetTotal(entity.getRequestedBudgetAmount());
        }
      }
      case OWNER_PM -> {
        if (entity.getNewPmUserId() != null) {
          project.setPmUserId(entity.getNewPmUserId());
        }
      }
      case PAUSE_TERMINATE -> {
        project.setStatus(Enums.ProjectStatus.TERMINATED);
        project.setTerminatedReason(entity.getReasonText());
      }
      // OBJECTIVE_SCOPE, OTHER: 不自动修改项目属性，由PMC手动确认
      default -> { /* no-op */ }
    }

    project.setUpdatedAt(Instant.now());
    projectRepository.save(project);
  }

  /**
   * 获取项目的变更申请列表
   */
  @Transactional(readOnly = true)
  public List<ProjectChangeRequestDto> getProjectChangeRequests(long projectId) {
    return changeRequestRepository.findByProjectIdOrderByRequestedAtDesc(projectId)
        .stream().map(this::toDto).toList();
  }

  /**
   * 获取单个变更申请详情
   */
  @Transactional(readOnly = true)
  public ProjectChangeRequestDto getChangeRequest(long changeId) {
    return changeRequestRepository.findById(changeId)
        .map(this::toDto)
        .orElseThrow(() -> new ApiException(404, "变更申请不存在"));
  }

  /**
   * 获取待审批的变更申请列表（效率管理部或PMC）
   */
  @Transactional(readOnly = true)
  public List<ProjectChangeRequestDto> getPendingChangeRequests() {
    List<ProjectChangeRequestEntity> all = changeRequestRepository.findAll();
    return all.stream()
        .filter(e -> "SUBMITTED".equals(e.getStatus()) || "EFFICIENCY_APPROVED".equals(e.getStatus()))
        .map(this::toDto)
        .toList();
  }

  // 兼容旧API
  @Transactional
  public ProjectChangeRequestDto executeDecision(long changeId, long approverId, ProjectChangeRequestDecisionRequest request) {
    ProjectChangeRequestEntity entity = changeRequestRepository.findById(changeId)
        .orElseThrow(() -> new ApiException(404, "变更申请不存在"));
    if (entity.getChangeType() == Enums.ChangeType.BUDGET && isWorkflowStatus(entity.getStatus())) {
      return executeBudgetWorkflowDecision(entity, approverId, request);
    }
    if ("SUBMITTED".equals(entity.getStatus())) {
      return approveByEfficiency(changeId, approverId, request.decision().name(), request.opinion());
    }
    return approveByPmc(changeId, approverId, request);
  }

  private void startBudgetWorkflow(ProjectChangeRequestEntity entity, ProjectEntity project) {
    WfProcessDefinition process = wfProcessRepository
        .findByProcessTypeAndMilestoneCodeIsNullAndIsActiveTrue("BUDGET")
        .orElseThrow(() -> new ApiException(500, "未配置预算变更流程"));
    List<WfProcessNode> approvalNodes = getOrderedBudgetApprovalNodes(process);
    if (approvalNodes.isEmpty()) {
      throw new ApiException(500, "预算变更流程未配置审批节点");
    }
    WfProcessNode firstNode = approvalNodes.get(0);
    entity.setStatus(toPendingStatus(firstNode.getNodeCode()));
    entity.setUpdatedAt(Instant.now());
    changeRequestRepository.save(entity);
    sendBudgetApprovalNotifications(entity, project, firstNode, "预算调整申请待审批");
  }

  private ProjectChangeRequestDto executeBudgetWorkflowDecision(
      ProjectChangeRequestEntity entity,
      long approverId,
      ProjectChangeRequestDecisionRequest request) {
    ProjectEntity project = projectRepository.findById(entity.getProjectId())
        .orElseThrow(() -> new ApiException(404, "项目不存在"));
    WfProcessDefinition process = wfProcessRepository
        .findByProcessTypeAndMilestoneCodeIsNullAndIsActiveTrue("BUDGET")
        .orElseThrow(() -> new ApiException(500, "未配置预算变更流程"));
    List<WfProcessNode> approvalNodes = getOrderedBudgetApprovalNodes(process);
    String currentNodeCode = extractPendingNodeCode(entity.getStatus());
    WfProcessNode currentNode = approvalNodes.stream()
        .filter(node -> Objects.equals(node.getNodeCode(), currentNodeCode))
        .findFirst()
        .orElseThrow(() -> new ApiException(409, "当前预算审批节点不存在: " + currentNodeCode));

    if (request.stepCode() != null && !request.stepCode().isBlank()
        && !currentNodeCode.equalsIgnoreCase(request.stepCode().trim())) {
      throw new ApiException(409, "当前审批节点已变化，请刷新后重试");
    }

    validateBudgetApprover(project, currentNode, approverId);
    notificationService.completeTodoByProjectAndMilestone(entity.getProjectId(), currentNodeCode, "BUDGET_APPROVAL");

    if (request.decision() == ProjectChangeRequestDecisionRequest.Decision.REJECT) {
      entity.setStatus("REJECTED");
      fillBudgetDecisionAudit(entity, approverId, request.opinion(), currentNodeCode, false);
      changeRequestRepository.save(entity);
      notifyBudgetRequesterResult(entity, project, false, currentNode.getNodeName());
      return toDto(entity);
    }

    int currentIndex = -1;
    for (int i = 0; i < approvalNodes.size(); i++) {
      if (Objects.equals(approvalNodes.get(i).getNodeCode(), currentNodeCode)) {
        currentIndex = i;
        break;
      }
    }
    if (currentIndex < 0) {
      throw new ApiException(409, "当前审批节点不存在");
    }

    fillBudgetDecisionAudit(entity, approverId, request.opinion(), currentNodeCode, true);
    if (currentIndex == approvalNodes.size() - 1) {
      entity.setStatus("APPROVED");
      entity.setPmcDecision(request.decision() == ProjectChangeRequestDecisionRequest.Decision.CONDITIONAL_APPROVE
          ? Enums.PmcDecision.CONDITIONAL_APPROVE
          : Enums.PmcDecision.APPROVE);
      applyChange(entity);
      changeRequestRepository.save(entity);
      notifyBudgetRequesterResult(entity, project, true, currentNode.getNodeName());
      return toDto(entity);
    }

    WfProcessNode nextNode = approvalNodes.get(currentIndex + 1);
    entity.setStatus(toPendingStatus(nextNode.getNodeCode()));
    entity.setUpdatedAt(Instant.now());
    changeRequestRepository.save(entity);
    sendBudgetApprovalNotifications(entity, project, nextNode, "预算调整申请待审批");
    return toDto(entity);
  }

  private void fillBudgetDecisionAudit(ProjectChangeRequestEntity entity, long approverId, String opinion,
                                       String currentNodeCode, boolean approved) {
    String normalizedNodeCode = currentNodeCode == null ? "" : currentNodeCode.toUpperCase(Locale.ROOT);
    if (normalizedNodeCode.contains("EFF") || normalizedNodeCode.contains("DEPT")) {
      entity.setEfficiencyApproverId(approverId);
      entity.setEfficiencyOpinion(opinion);
      entity.setEfficiencyDecidedAt(LocalDateTime.now(ZoneOffset.UTC));
    } else {
      entity.setPmcDecidedBy(approverId);
      entity.setPmcDecisionText(opinion);
      entity.setPmcDecidedAt(LocalDateTime.now(ZoneOffset.UTC));
      entity.setPmcDecision(approved ? Enums.PmcDecision.APPROVE : Enums.PmcDecision.REJECT);
    }
    entity.setUpdatedAt(Instant.now());
  }

  private void validateBudgetApprover(ProjectEntity project, WfProcessNode node, long approverId) {
    List<Long> approvers = wfProcessService.resolveApprovers(node, project);
    if (!approvers.contains(approverId)) {
      throw new ApiException(403, "当前用户无权审批该预算节点");
    }
  }

  private List<WfProcessNode> getOrderedBudgetApprovalNodes(WfProcessDefinition process) {
    List<WfProcessNode> nodes = new ArrayList<>(process.getNodes() == null ? List.of() : process.getNodes());
    return nodes.stream()
        .filter(node -> node.getNodeType() != null)
        .filter(node -> !"START".equalsIgnoreCase(node.getNodeType()))
        .filter(node -> !"END".equalsIgnoreCase(node.getNodeType()))
        .sorted(Comparator.comparing(WfProcessNode::getSortOrder, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(WfProcessNode::getId, Comparator.nullsLast(Long::compareTo)))
        .toList();
  }

  private void sendBudgetApprovalNotifications(ProjectChangeRequestEntity entity, ProjectEntity project,
                                               WfProcessNode node, String title) {
    List<Long> approverIds = wfProcessService.resolveApprovers(node, project);
    if (approverIds.isEmpty()) {
      throw new ApiException(500, "预算流程节点未匹配到审批人: " + node.getNodeName());
    }
    String content = String.format("项目【%s】的预算调整申请待您审批。当前节点：%s；申请预算：%s。",
        project.getProjectName(), node.getNodeName(), entity.getRequestedBudgetAmount());
    for (Long approverId : approverIds) {
      notificationService.sendNotification(
          approverId,
          "BUDGET_APPROVAL",
          title,
          content,
          entity.getProjectId(),
          node.getNodeCode(),
          entity.getRequestedBy(),
          true);
    }
  }

  private void notifyBudgetRequesterResult(ProjectChangeRequestEntity entity, ProjectEntity project,
                                           boolean approved, String nodeName) {
    if (entity.getRequestedBy() == null) {
      return;
    }
    String title = approved ? "预算调整申请已通过" : "预算调整申请未通过";
    String content = String.format("项目【%s】的预算调整申请已由%s%s。",
        project.getProjectName(), nodeName, approved ? "审批通过" : "驳回");
    notificationService.sendNotification(
        entity.getRequestedBy(),
        "BUDGET_RESULT",
        title,
        content,
        entity.getProjectId(),
        null,
        null,
        false);
  }

  @Transactional(readOnly = true)
  public BudgetWorkflowProgressResponse getBudgetWorkflowProgress(Long projectId) {
    List<ProjectChangeRequestEntity> requests = changeRequestRepository.findByProjectIdOrderByRequestedAtDesc(projectId);
    ProjectChangeRequestEntity entity = requests.stream()
        .filter(req -> req.getChangeType() == Enums.ChangeType.BUDGET)
        .findFirst()
        .orElse(null);
    if (entity == null) {
      return new BudgetWorkflowProgressResponse(null, "NONE", null, List.of());
    }

    WfProcessDefinition process = wfProcessRepository
        .findByProcessTypeAndMilestoneCodeIsNullAndIsActiveTrue("BUDGET")
        .orElse(null);
    List<WfProcessNode> nodes = process == null ? List.of() : getOrderedBudgetApprovalNodes(process);
    String currentNodeCode = isWorkflowStatus(entity.getStatus()) ? extractPendingNodeCode(entity.getStatus()) : null;
    List<BudgetWorkflowStepResponse> steps = new ArrayList<>();
    for (WfProcessNode node : nodes) {
      String stepStatus = "PENDING";
      if ("REJECTED".equals(entity.getStatus())) {
        if (Objects.equals(currentNodeCode, node.getNodeCode())) {
          stepStatus = "REJECTED";
        } else if (isStepCompletedBeforeCurrent(nodes, node.getNodeCode(), currentNodeCode)) {
          stepStatus = "COMPLETED";
        }
      } else if ("APPROVED".equals(entity.getStatus())) {
        stepStatus = "COMPLETED";
      } else if (Objects.equals(currentNodeCode, node.getNodeCode())) {
        stepStatus = "IN_PROGRESS";
      } else if (isStepCompletedBeforeCurrent(nodes, node.getNodeCode(), currentNodeCode)) {
        stepStatus = "COMPLETED";
      }
      steps.add(new BudgetWorkflowStepResponse(node.getNodeCode(), node.getNodeName(), stepStatus));
    }
    String currentNodeName = nodes.stream()
        .filter(node -> Objects.equals(node.getNodeCode(), currentNodeCode))
        .map(WfProcessNode::getNodeName)
        .findFirst()
        .orElse(null);
    return new BudgetWorkflowProgressResponse(entity.getId(), entity.getStatus(), currentNodeName, steps);
  }

  private boolean isStepCompletedBeforeCurrent(List<WfProcessNode> nodes, String stepCode, String currentNodeCode) {
    if (stepCode == null || currentNodeCode == null) {
      return false;
    }
    int stepIndex = -1;
    int currentIndex = -1;
    for (int i = 0; i < nodes.size(); i++) {
      if (Objects.equals(nodes.get(i).getNodeCode(), stepCode)) {
        stepIndex = i;
      }
      if (Objects.equals(nodes.get(i).getNodeCode(), currentNodeCode)) {
        currentIndex = i;
      }
    }
    return stepIndex >= 0 && currentIndex >= 0 && stepIndex < currentIndex;
  }

  public record BudgetWorkflowProgressResponse(
      Long requestId,
      String status,
      String currentNodeName,
      List<BudgetWorkflowStepResponse> steps
  ) {}

  public record BudgetWorkflowStepResponse(
      String stepCode,
      String stepName,
      String status
  ) {}

  private boolean isWorkflowStatus(String status) {
    return status != null && status.startsWith("PENDING_");
  }

  private String toPendingStatus(String nodeCode) {
    return "PENDING_" + nodeCode;
  }

  private String extractPendingNodeCode(String status) {
    return status != null && status.startsWith("PENDING_") ? status.substring("PENDING_".length()) : status;
  }

  private ProjectChangeRequestDto toDto(ProjectChangeRequestEntity entity) {
    String projectName = projectRepository.findById(entity.getProjectId())
        .map(ProjectEntity::getProjectName).orElse(null);
    String requesterName = entity.getRequestedBy() != null
        ? iamUserRepository.findById(entity.getRequestedBy())
            .map(IamUserEntity::getDisplayName).orElse(null) : null;
    String efficiencyApproverName = entity.getEfficiencyApproverId() != null
        ? iamUserRepository.findById(entity.getEfficiencyApproverId())
            .map(IamUserEntity::getDisplayName).orElse(null) : null;
    String pmcDeciderName = entity.getPmcDecidedBy() != null
        ? iamUserRepository.findById(entity.getPmcDecidedBy())
            .map(IamUserEntity::getDisplayName).orElse(null) : null;

    return new ProjectChangeRequestDto(
        entity.getId(), entity.getProjectId(), projectName,
        entity.getChangeType() != null ? entity.getChangeType().name() : null,
        entity.getReasonText(), entity.getAttachmentUri(),
        entity.getBeforeText(), entity.getAfterText(),
        entity.getImpactMilestoneText(), entity.getImpactBudgetText(),
        entity.getImpactResourceText(), entity.getRequestedBy(), requesterName,
        entity.getRequestedAt(), entity.getStatus(),
        entity.getEfficiencyApproverId(), efficiencyApproverName,
        entity.getEfficiencyOpinion(), entity.getEfficiencyDecidedAt(),
        entity.getPmcDecision() != null ? entity.getPmcDecision().name() : null,
        entity.getPmcDecisionText(), entity.getPmcDecidedAt(),
        entity.getPmcDecidedBy(), pmcDeciderName
    );
  }
}