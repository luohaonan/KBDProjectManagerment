package com.kbd.pms.service;

import com.kbd.pms.entity.*;
import com.kbd.pms.exception.ApiException;
import com.kbd.pms.exception.BudgetExceededException;
import com.kbd.pms.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class BudgetService {

    private static final Logger logger = LoggerFactory.getLogger(BudgetService.class);

    private final ProjectBudgetLedgerRepository ledgerRepository;
    private final BudgetLimitRepository budgetLimitRepository;
    private final ProjectMilestoneRepository milestoneRepository;
    private final MilestoneDefRepository milestoneDefRepository;

    public BudgetService(ProjectBudgetLedgerRepository ledgerRepository,
                        BudgetLimitRepository budgetLimitRepository,
                        ProjectMilestoneRepository milestoneRepository,
                        MilestoneDefRepository milestoneDefRepository) {
        this.ledgerRepository = ledgerRepository;
        this.budgetLimitRepository = budgetLimitRepository;
        this.milestoneRepository = milestoneRepository;
        this.milestoneDefRepository = milestoneDefRepository;
    }

    /**
     * Process expenditure request with real-time validation
     */
    @Transactional
    public void processExpenditure(Long projectId, BigDecimal amount, Enums.ExpenseCategory category,
                                  String description, Long createdBy) {
        // Get current milestone code
        String currentMilestoneCode = getCurrentMilestoneCode(projectId);
        if (currentMilestoneCode == null) {
            throw new IllegalStateException("No current milestone found for project " + projectId);
        }

        // Get approved budget for current stage
        Optional<BudgetLimitEntity> budgetLimitOpt = budgetLimitRepository.findByProjectIdAndMilestoneCode(projectId, currentMilestoneCode);
        if (budgetLimitOpt.isEmpty()) {
            throw new IllegalStateException("No budget limit set for project " + projectId + " stage " + currentMilestoneCode);
        }
        BigDecimal approvedBudget = budgetLimitOpt.get().getApprovedBudget();

        // Calculate current spent for this stage
        BigDecimal currentSpent = calculateCurrentSpentForStage(projectId, currentMilestoneCode);

        // Calculate new total
        BigDecimal newTotal = currentSpent.add(amount);

        // Calculate utilization ratio
        BigDecimal utilizationRatio = newTotal.divide(approvedBudget, 6, RoundingMode.HALF_UP);

        // Check thresholds
        if (utilizationRatio.compareTo(BigDecimal.valueOf(0.95)) >= 0) {
            // Red warning: block expenditure
            throw new BudgetExceededException("Budget utilization reached 95%. Expenditure blocked. Need PMC approval for budget adjustment or plan change.");
        } else if (utilizationRatio.compareTo(BigDecimal.valueOf(0.80)) >= 0) {
            // Yellow warning: allow but notify
            logger.warn("Budget utilization reached 80% for project {} stage {}. Notifying PM and department head.", projectId, currentMilestoneCode);
            // Simulate notification
            sendNotification(projectId, "YELLOW_WARNING", "Budget utilization at " + utilizationRatio.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP) + "%");
        }

        // Save the expenditure
        ProjectBudgetLedgerEntity ledger = new ProjectBudgetLedgerEntity();
        ledger.setProjectId(projectId);
        ledger.setOccurredOn(LocalDate.now());
        ledger.setExpenseCategory(category);
        ledger.setAmount(amount);
        ledger.setDescription(description);
        ledger.setCreatedBy(createdBy);
        ledger.setCreatedAt(java.time.Instant.now());

        ledgerRepository.save(ledger);
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
        } else if (utilizationRatio.compareTo(BigDecimal.valueOf(0.80)) >= 0) {
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
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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
}