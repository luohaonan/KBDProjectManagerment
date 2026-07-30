package com.kbd.pms.service;

import com.kbd.pms.entity.*;
import com.kbd.pms.exception.ApiException;
import com.kbd.pms.exception.BudgetExceededException;
import com.kbd.pms.repository.*;
import com.kbd.pms.workflow.WfProcessDefinition;
import com.kbd.pms.workflow.WfProcessNode;
import com.kbd.pms.workflow.WfProcessRepository;
import com.kbd.pms.workflow.WfProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
public class BudgetService {

    private static final Logger logger = LoggerFactory.getLogger(BudgetService.class);

    private final ProjectBudgetLedgerRepository ledgerRepository;
    private final BudgetLimitRepository budgetLimitRepository;
    private final ProjectMilestoneRepository milestoneRepository;
    private final MilestoneDefRepository milestoneDefRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectTeamMemberRepository projectTeamMemberRepository;
    private final ProjectChangeRequestRepository projectChangeRequestRepository;
    private final ProjectChangeRequestService projectChangeRequestService;
    private final NotificationService notificationService;
    private final WfProcessRepository wfProcessRepository;
    private final WfProcessService wfProcessService;

    @Autowired
    public BudgetService(ProjectBudgetLedgerRepository ledgerRepository,
                        BudgetLimitRepository budgetLimitRepository,
                        ProjectMilestoneRepository milestoneRepository,
                        MilestoneDefRepository milestoneDefRepository,
                        ProjectRepository projectRepository,
                        UserRepository userRepository,
                        ProjectTeamMemberRepository projectTeamMemberRepository,
                        ProjectChangeRequestRepository projectChangeRequestRepository,
                        ProjectChangeRequestService projectChangeRequestService,
                        NotificationService notificationService,
                         WfProcessRepository wfProcessRepository,
                         WfProcessService wfProcessService) {
        this.ledgerRepository = ledgerRepository;
        this.budgetLimitRepository = budgetLimitRepository;
        this.milestoneRepository = milestoneRepository;
        this.milestoneDefRepository = milestoneDefRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.projectTeamMemberRepository = projectTeamMemberRepository;
        this.projectChangeRequestRepository = projectChangeRequestRepository;
        this.projectChangeRequestService = projectChangeRequestService;
        this.notificationService = notificationService;
        this.wfProcessRepository = wfProcessRepository;
        this.wfProcessService = wfProcessService;
    }

    /**
     * Process expenditure request with real-time validation
     */
    @Transactional
    public void processExpenditure(Long projectId, BigDecimal amount, Enums.ExpenseCategory category,
                                  LocalDate occurredOn, String vendorName, String referenceNo,
                                  String description, Long createdBy) {
        // Get current milestone code
        String currentMilestoneCode = getCurrentMilestoneCode(projectId);
        BigDecimal approvedBudget = resolveAvailableBudget(projectId, currentMilestoneCode);
        if (approvedBudget == null || approvedBudget.signum() <= 0) {
            throw new ApiException(409, "当前项目尚未配置可用预算，无法新增支出记录");
        }

        // Calculate current spent for this stage
        BigDecimal currentSpent = calculateCurrentSpentForStage(projectId, currentMilestoneCode);

        BigDecimal remainingBudget = approvedBudget.subtract(currentSpent);
        if (amount.compareTo(remainingBudget) > 0) {
            throw new BudgetExceededException("Expenditure amount exceeds remaining budget.");
        }

        // Calculate new total
        BigDecimal newTotal = currentSpent.add(amount);

        // Calculate utilization ratio
        BigDecimal utilizationRatio = newTotal.divide(approvedBudget, 6, RoundingMode.HALF_UP);

        // Check thresholds
        BigDecimal thresholdPercent = resolveBudgetWarningThresholdPercent();
        BigDecimal thresholdRatio = thresholdPercent.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        if (utilizationRatio.compareTo(thresholdRatio) >= 0) {
            logger.warn("Budget utilization reached configured threshold {}% for project {} stage {}.", thresholdPercent, projectId, currentMilestoneCode);
        }

        // Save the expenditure
        ProjectBudgetLedgerEntity ledger = new ProjectBudgetLedgerEntity();
        ledger.setProjectId(projectId);
        ledger.setOccurredOn(occurredOn != null ? occurredOn : LocalDate.now());
        ledger.setExpenseCategory(category);
        ledger.setAmount(amount);
        ledger.setVendorName(vendorName);
        ledger.setReferenceNo(referenceNo);
        ledger.setDescription(description);
        ledger.setCreatedBy(createdBy);
        ledger.setCreatedAt(java.time.Instant.now());

        ledgerRepository.save(ledger);
        if (utilizationRatio.compareTo(thresholdRatio) >= 0) {
            sendBudgetUsageWarning(projectId,
                    utilizationRatio.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP),
                    thresholdPercent);
        }
    }

    /**
     * Apply budget change - mark for PMC approval if change > 10%
     */
    @Transactional
    public void applyBudgetChange(Long projectId, String milestoneCode, BigDecimal newBudget, Long updatedBy) {
        Optional<BudgetLimitEntity> existingLimitOpt = budgetLimitRepository.findByProjectIdAndMilestoneCode(projectId, milestoneCode);
        if (existingLimitOpt.isEmpty()) {
            throw new IllegalStateException("No existing budget limit for project " + projectId + " stage " + milestoneCode);
        }

        BigDecimal oldBudget = existingLimitOpt.get().getApprovedBudget();
        BigDecimal changeRatio = newBudget.subtract(oldBudget).divide(oldBudget, 6, RoundingMode.HALF_UP);

        if (changeRatio.compareTo(BigDecimal.valueOf(0.10)) > 0) {
            throw new ApiException(409, "预算变更超过 10%，必须通过变更申请处理。");
        }

        // Update budget limit
        BudgetLimitEntity limit = existingLimitOpt.get();
        limit.setApprovedBudget(newBudget);
        limit.setUpdatedBy(updatedBy);
        limit.setUpdatedAt(java.time.Instant.now());

        budgetLimitRepository.save(limit);
    }

    /**
     * Get budget status overview
     */
    public BudgetSnapshotResponse getBudgetStatus(Long projectId) {
        String currentMilestoneCode = getCurrentMilestoneCode(projectId);
        if (currentMilestoneCode == null) {
            return new BudgetSnapshotResponse(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, Enums.WarningLevel.NONE);
        }

        Optional<BudgetLimitEntity> budgetLimitOpt = budgetLimitRepository.findByProjectIdAndMilestoneCode(projectId, currentMilestoneCode);
        if (budgetLimitOpt.isEmpty()) {
            return new BudgetSnapshotResponse(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, Enums.WarningLevel.NONE);
        }

        BigDecimal approvedBudget = budgetLimitOpt.get().getApprovedBudget();
        BigDecimal currentSpent = calculateCurrentSpentForStage(projectId, currentMilestoneCode);
        BigDecimal utilizationRatio = currentSpent.divide(approvedBudget, 6, RoundingMode.HALF_UP);

        Enums.WarningLevel warningLevel = Enums.WarningLevel.NONE;
        if (utilizationRatio.compareTo(BigDecimal.valueOf(0.95)) >= 0) {
            warningLevel = Enums.WarningLevel.RED;
        } else if (utilizationRatio.compareTo(resolveBudgetWarningThresholdPercent().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)) >= 0) {
            warningLevel = Enums.WarningLevel.YELLOW;
        }

        return new BudgetSnapshotResponse(approvedBudget, currentSpent, utilizationRatio, warningLevel);
    }

    /**
     * Generate monthly budget execution report
     */
    public String generateMonthlyReport(Long projectId, YearMonth month) {
        // Simplified report - in real implementation, would aggregate data
        BudgetSnapshotResponse status = getBudgetStatus(projectId);
        BigDecimal deviation = status.totalSpent().subtract(status.approvedBudget().multiply(BigDecimal.valueOf(0.8))); // Example deviation calculation

        StringBuilder report = new StringBuilder();
        report.append("Budget Execution Report for Project ").append(projectId).append(" - ").append(month).append("\n");
        report.append("Approved Budget: ").append(status.approvedBudget()).append("\n");
        report.append("Total Spent: ").append(status.totalSpent()).append("\n");
        report.append("Utilization Ratio: ").append(status.utilizationRatio().multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)).append("%\n");
        report.append("Warning Level: ").append(status.warningLevel()).append("\n");
        report.append("Deviation Analysis: ").append(deviation).append(" (example calculation)\n");
        report.append("Main Deviation Causes: [To be analyzed by efficiency management department]\n");

        return report.toString();
    }

    private String getCurrentMilestoneCode(Long projectId) {
        // Find the latest milestone for the project
        List<ProjectMilestoneEntity> milestones = milestoneRepository.findByProjectIdOrderByIdAsc(projectId);
        if (milestones.isEmpty()) {
            return null;
        }
        // Assume the last one is current
        ProjectMilestoneEntity currentMilestone = milestones.get(milestones.size() - 1);
        return milestoneDefRepository.findById(currentMilestone.getMilestoneId())
                .map(MilestoneDefEntity::getMilestoneCode)
                .orElse(null);
    }

    private BigDecimal calculateCurrentSpentForStage(Long projectId, String milestoneCode) {
        // Simplified: sum all expenditures for the project (in real implementation, filter by stage period)
        List<ProjectBudgetLedgerEntity> ledgers = ledgerRepository.findByProjectId(projectId);
        return ledgers.stream()
                .map(ProjectBudgetLedgerEntity::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal resolveAvailableBudget(Long projectId, String currentMilestoneCode) {
        if (currentMilestoneCode != null) {
            Optional<BudgetLimitEntity> budgetLimitOpt = budgetLimitRepository.findByProjectIdAndMilestoneCode(projectId, currentMilestoneCode);
            if (budgetLimitOpt.isPresent() && budgetLimitOpt.get().getApprovedBudget() != null
                    && budgetLimitOpt.get().getApprovedBudget().signum() > 0) {
                return budgetLimitOpt.get().getApprovedBudget();
            }
        }

        ProjectEntity project = getProject(projectId);
        if (project.getBudgetTotal() != null && project.getBudgetTotal().signum() > 0) {
            return project.getBudgetTotal();
        }

        return BigDecimal.ZERO;
    }

    private void sendNotification(Long projectId, String warningType, String message) {
        // Simulate sending notification to PM and department head
        logger.info("Sending {} notification for project {}: {}", warningType, projectId, message);
        // In real implementation, integrate with notification service
    }

    // Java 21 Record for immutable response
    public record BudgetSnapshotResponse(
            BigDecimal approvedBudget,
            BigDecimal totalSpent,
            BigDecimal utilizationRatio,
            Enums.WarningLevel warningLevel
    ) {}

    public record BudgetLedgerItemResponse(
            Long id,
            String occurredOn,
            String expenseCategory,
            BigDecimal amount,
            String vendorName,
            String referenceNo,
            String description
    ) {}

    public record BudgetAdjustmentSummaryResponse(
            Long id,
            BigDecimal previousBudgetAmount,
            BigDecimal requestedBudgetAmount,
            BigDecimal adjustmentAmount,
            String reasonText,
            String status,
            String requestedByName,
            String requestedAt,
            ProjectChangeRequestService.BudgetWorkflowProgressResponse progress
    ) {}

    public record BudgetWarningConfigResponse(
            BigDecimal warningThresholdPercent
    ) {}

    public record ExpenditureRecordRequest(
            LocalDate occurredOn,
            String expenseCategory,
            BigDecimal amount,
            String vendorName,
            String referenceNo,
            String description
    ) {}

    public record BudgetManagementResponse(
            Long projectId,
            String projectCode,
            String projectName,
            BigDecimal totalBudget,
            BigDecimal totalSpent,
            BigDecimal remainingBudget,
            BigDecimal utilizationRatio,
            String warningLevel,
            BigDecimal warningThresholdPercent,
            boolean canManage,
            boolean canApprove,
            List<BudgetLedgerItemResponse> ledgerItems,
            List<BudgetAdjustmentSummaryResponse> adjustments,
            ProjectChangeRequestService.BudgetWorkflowProgressResponse currentApprovalProgress
    ) {}

    public record BudgetDashboardItemResponse(
            Long projectId,
            String projectCode,
            String projectName,
            String lifecyclePhaseLabel,
            BigDecimal totalBudget,
            BigDecimal totalSpent,
            BigDecimal remainingBudget,
            BigDecimal utilizationRatio,
            String warningLevel
    ) {}

    @Transactional(readOnly = true)
    public BudgetManagementResponse getBudgetManagement(Long projectId, Long currentUserId) {
        ProjectEntity project = getProject(projectId);
        boolean canView = canViewBudget(project, currentUserId);
        boolean canManage = canManageBudget(project, currentUserId);
        boolean canApprove = isCurrentBudgetApprover(project, currentUserId);
        if (!canView && !isAdmin(currentUserId)) {
            throw new ApiException(403, "无预算管理权限");
        }

        BigDecimal totalBudget = project.getBudgetTotal() == null ? BigDecimal.ZERO : project.getBudgetTotal();
        List<ProjectBudgetLedgerEntity> ledgers = ledgerRepository.findByProjectId(projectId);
        BigDecimal totalSpent = ledgers.stream()
                .map(ProjectBudgetLedgerEntity::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remainingBudget = totalBudget.subtract(totalSpent);
        BigDecimal utilizationRatio = totalBudget.signum() <= 0
                ? BigDecimal.ZERO
                : totalSpent.divide(totalBudget, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        Enums.WarningLevel warningLevel = resolveWarningLevel(utilizationRatio);
        BigDecimal warningThresholdPercent = resolveBudgetWarningThresholdPercent();

        List<BudgetLedgerItemResponse> ledgerItems = ledgers.stream()
                .sorted(Comparator.comparing(ProjectBudgetLedgerEntity::getOccurredOn).reversed()
                        .thenComparing(ProjectBudgetLedgerEntity::getId, Comparator.reverseOrder()))
                .map(item -> new BudgetLedgerItemResponse(
                        item.getId(),
                        item.getOccurredOn() != null ? item.getOccurredOn().toString() : null,
                        item.getExpenseCategory() != null ? item.getExpenseCategory().name() : null,
                        item.getAmount(),
                        item.getVendorName(),
                        item.getReferenceNo(),
                        item.getDescription()))
                .toList();

        List<BudgetAdjustmentSummaryResponse> adjustments = projectChangeRequestRepository
                .findByProjectIdOrderByRequestedAtDesc(projectId)
                .stream()
                .filter(request -> request.getChangeType() == Enums.ChangeType.BUDGET)
                .map(request -> new BudgetAdjustmentSummaryResponse(
                        request.getId(),
                        request.getPreviousBudgetAmount(),
                        request.getRequestedBudgetAmount(),
                        request.getAdjustmentAmount(),
                        request.getReasonText(),
                        request.getStatus(),
                        resolveUserDisplayName(request.getRequestedBy()),
                        request.getRequestedAt() != null ? request.getRequestedAt().toString() : null,
                        buildBudgetProgress(request)))
                .toList();

        ProjectChangeRequestService.BudgetWorkflowProgressResponse currentApprovalProgress = projectChangeRequestService.getBudgetWorkflowProgress(projectId);

        return new BudgetManagementResponse(
                project.getId(),
                project.getProjectCode(),
                project.getProjectName(),
                totalBudget,
                totalSpent,
                remainingBudget,
                utilizationRatio,
                warningLevel.name(),
                warningThresholdPercent,
                canManage,
                canApprove,
                ledgerItems,
                adjustments,
                currentApprovalProgress);
    }

    private boolean canViewBudget(ProjectEntity project, Long userId) {
        if (userRepository == null) {
            return false;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "用户不存在"));
        Set<String> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());

        boolean isProjectPm = project.getPmUserId() != null && project.getPmUserId().equals(userId);
        boolean isProjectAdmin = roles.contains("ROLE_PROJECT_ADMIN")
                || projectTeamMemberRepository.isActiveMemberWithRole(
                        project.getId(), userId, Enums.ProjectTeamRole.PM, LocalDate.now());
        boolean hasBudgetApprovalAccess = hasBudgetApprovalAccess(project, userId);
        return isProjectPm || isProjectAdmin || hasBudgetApprovalAccess || roles.contains("ROLE_ADMIN");
    }

    private boolean hasBudgetApprovalAccess(ProjectEntity project, Long userId) {
        if (project == null || userId == null) {
            return false;
        }

        if (isCurrentBudgetApprover(project, userId)) {
            return true;
        }

        ProjectChangeRequestEntity latestBudgetRequest = findLatestBudgetRequest(project.getId());
        if (latestBudgetRequest == null) {
            return false;
        }

        return userId.equals(latestBudgetRequest.getEfficiencyApproverId())
                || userId.equals(latestBudgetRequest.getPmcDecidedBy())
                || userId.equals(latestBudgetRequest.getRequestedBy());
    }

    private ProjectChangeRequestEntity findLatestBudgetRequest(Long projectId) {
        if (projectId == null) {
            return null;
        }
        return projectChangeRequestRepository
                .findByProjectIdOrderByRequestedAtDesc(projectId)
                .stream()
                .filter(request -> request.getChangeType() == Enums.ChangeType.BUDGET)
                .findFirst()
                .orElse(null);
    }

    private boolean isCurrentBudgetApprover(ProjectEntity project, Long userId) {
        if (project == null || userId == null || wfProcessService == null) {
            return false;
        }

        ProjectChangeRequestEntity latestBudgetRequest = findLatestBudgetRequest(project.getId());
        if (latestBudgetRequest == null) {
            return false;
        }

        String status = latestBudgetRequest.getStatus();
        if (status == null || !status.startsWith("PENDING_")) {
            return false;
        }

        String currentNodeCode = status.substring("PENDING_".length());
        if (currentNodeCode.isBlank()) {
            return false;
        }

        WfProcessDefinition process = wfProcessRepository
                .findByProcessTypeAndMilestoneCodeIsNullAndIsActiveTrue("BUDGET")
                .orElse(null);
        if (process == null || process.getNodes() == null) {
            return false;
        }

        WfProcessNode currentNode = process.getNodes().stream()
                .filter(node -> node.getNodeCode() != null)
                .filter(node -> currentNodeCode.equalsIgnoreCase(node.getNodeCode()))
                .findFirst()
                .orElse(null);
        if (currentNode == null) {
            return false;
        }

        return wfProcessService.resolveApprovers(currentNode, project).contains(userId);
    }

    @Transactional(readOnly = true)
    public List<BudgetDashboardItemResponse> getBudgetDashboard(Long currentUserId) {
        return projectRepository.findAll().stream()
                .filter(project -> isAdmin(currentUserId) || canManageBudget(project, currentUserId))
                .map(project -> {
                    BigDecimal totalBudget = project.getBudgetTotal() == null ? BigDecimal.ZERO : project.getBudgetTotal();
                    BigDecimal totalSpent = ledgerRepository.findByProjectId(project.getId()).stream()
                            .map(ProjectBudgetLedgerEntity::getAmount)
                            .filter(java.util.Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal remainingBudget = totalBudget.subtract(totalSpent);
                    BigDecimal utilizationRatio = totalBudget.signum() <= 0
                            ? BigDecimal.ZERO
                            : totalSpent.divide(totalBudget, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100));
                    return new BudgetDashboardItemResponse(
                            project.getId(),
                            project.getProjectCode(),
                            project.getProjectName(),
                            resolveLifecyclePhaseLabel(project),
                            totalBudget,
                            totalSpent,
                            remainingBudget,
                            utilizationRatio,
                            resolveWarningLevel(utilizationRatio).name());
                })
                .sorted(Comparator.comparing(BudgetDashboardItemResponse::utilizationRatio).reversed())
                .toList();
    }

    @Transactional
    public void submitBudgetAdjustment(Long projectId, BigDecimal requestedBudget, String reasonText,
                                       Long currentUserId) {
        ProjectEntity project = getProject(projectId);
        if (!canManageBudget(project, currentUserId)) {
            throw new ApiException(403, "无预算调整权限");
        }
        projectChangeRequestService.submitBudgetChange(projectId, requestedBudget, reasonText, currentUserId);
    }

    @Transactional
    public void createExpenditureRecord(Long projectId, ExpenditureRecordRequest request, Long currentUserId) {
        ProjectEntity project = getProject(projectId);
        if (!canManageBudget(project, currentUserId)) {
            throw new ApiException(403, "无支出管理权限");
        }
        if (request == null || request.amount() == null || request.amount().signum() <= 0) {
            throw new ApiException(400, "支出金额必须大于0");
        }
        Enums.ExpenseCategory category;
        try {
            category = request.expenseCategory() == null ? Enums.ExpenseCategory.INTERNAL : Enums.ExpenseCategory.valueOf(request.expenseCategory());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(400, "无效的支出分类");
        }
        processExpenditure(
                projectId,
                request.amount(),
                category,
                request.occurredOn(),
                request.vendorName(),
                request.referenceNo(),
                request.description(),
                currentUserId);
    }

    @Transactional(readOnly = true)
    public BudgetWarningConfigResponse getBudgetWarningConfig() {
        return new BudgetWarningConfigResponse(resolveBudgetWarningThresholdPercent());
    }

    private ProjectEntity getProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException(404, "项目不存在"));
    }

    private boolean canManageBudget(ProjectEntity project, Long userId) {
        if (userRepository == null || projectTeamMemberRepository == null) {
            return false;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "用户不存在"));
        Set<String> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());

        boolean hasBudgetPermission = permissions.contains("PERMISSION_BUDGET_MANAGE")
                || roles.contains("ROLE_ADMIN");
        boolean isProjectPm = project.getPmUserId() != null && project.getPmUserId().equals(userId);
        boolean isProjectAdmin = roles.contains("ROLE_PROJECT_ADMIN")
                || projectTeamMemberRepository.isActiveMemberWithRole(
                        project.getId(), userId, Enums.ProjectTeamRole.PM, LocalDate.now());
        return hasBudgetPermission && (isProjectPm || isProjectAdmin || roles.contains("ROLE_ADMIN"));
    }

    private boolean isAdmin(Long userId) {
        if (userRepository == null) {
            return false;
        }
        return userRepository.findById(userId)
                .map(user -> user.getRoles().stream().map(Role::getName).anyMatch("ROLE_ADMIN"::equals))
                .orElse(false);
    }

    private String resolveUserDisplayName(Long userId) {
        if (userId == null) {
            return null;
        }
        if (userRepository == null) {
            return String.valueOf(userId);
        }
        return userRepository.findById(userId)
                .map(User::getUsername)
                .orElseGet(() -> iamUserNameFallback(userId));
    }

    private String resolveLifecyclePhaseLabel(ProjectEntity project) {
        if (project == null || project.getCurrentMilestoneId() == null) {
            return null;
        }
        return milestoneDefRepository.findById(project.getCurrentMilestoneId())
                .map(def -> def.getMilestoneCode() + "-" + def.getMilestoneName())
                .orElse(null);
    }

    private String iamUserNameFallback(Long userId) {
        return String.valueOf(userId);
    }

    private Enums.WarningLevel resolveWarningLevel(BigDecimal utilizationPercent) {
        if (utilizationPercent.compareTo(BigDecimal.valueOf(95)) >= 0) {
            return Enums.WarningLevel.RED;
        }
        if (utilizationPercent.compareTo(resolveBudgetWarningThresholdPercent()) >= 0) {
            return Enums.WarningLevel.YELLOW;
        }
        return Enums.WarningLevel.NONE;
    }

    private BigDecimal resolveBudgetWarningThresholdPercent() {
        return wfProcessRepository.findByProcessTypeAndMilestoneCodeIsNullAndIsActiveTrue("BUDGET")
                .map(WfProcessDefinition::getBudgetWarningThreshold)
                .filter(value -> value != null && value.signum() > 0)
                .orElse(BigDecimal.valueOf(80));
    }

    private void sendBudgetUsageWarning(Long projectId, BigDecimal utilizationPercent, BigDecimal thresholdPercent) {
        ProjectEntity project = getProject(projectId);
        Set<Long> recipients = new LinkedHashSet<>();
        if (project.getPmUserId() != null) {
            recipients.add(project.getPmUserId());
        }
        if (projectTeamMemberRepository != null) {
            userRepository.findAll().stream()
                    .filter(user -> projectTeamMemberRepository.isActiveMemberWithRole(projectId, user.getId(), Enums.ProjectTeamRole.PM, LocalDate.now())
                            || user.getRoles().stream().map(Role::getName).anyMatch("ROLE_PROJECT_ADMIN"::equals))
                    .map(User::getId)
                    .forEach(recipients::add);
        }
        String title = "预算使用率达到预警阈值";
        String content = String.format("项目【%s】预算使用率已达到 %s%%，超过预警阈值 %s%%，请及时关注预算执行与支出安排。",
                project.getProjectName(), utilizationPercent, thresholdPercent);
        for (Long recipient : recipients) {
            notificationService.sendNotification(recipient, "BUDGET_WARNING", title, content, projectId, null, null, false);
        }
    }

    private ProjectChangeRequestService.BudgetWorkflowProgressResponse buildBudgetProgress(ProjectChangeRequestEntity request) {
        if (request == null || request.getProjectId() == null) {
            return new ProjectChangeRequestService.BudgetWorkflowProgressResponse(null, "NONE", null, List.of());
        }
        return projectChangeRequestService.getBudgetWorkflowProgress(request.getProjectId());
    }
}