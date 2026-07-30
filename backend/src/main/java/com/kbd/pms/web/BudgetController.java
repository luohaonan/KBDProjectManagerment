package com.kbd.pms.web;

import com.kbd.pms.dto.ExpenseRequest;
import com.kbd.pms.service.BudgetService;
import com.kbd.pms.service.SecurityHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final SecurityHelper securityHelper;

    public BudgetController(BudgetService budgetService, SecurityHelper securityHelper) {
        this.budgetService = budgetService;
        this.securityHelper = securityHelper;
    }

    /**
     * Get budget execution overview for a project
     */
    @GetMapping("/status/{projectId}")
    public ResponseEntity<BudgetService.BudgetSnapshotResponse> getBudgetStatus(@PathVariable Long projectId) {
        BudgetService.BudgetSnapshotResponse response = budgetService.getBudgetStatus(projectId);
        return ResponseEntity.ok(response);
    }

    /**
     * Submit expenditure request
     */
    @PostMapping("/expense")
    public ResponseEntity<String> submitExpense(@RequestBody ExpenseRequest request) {
        budgetService.processExpenditure(
                request.projectId(),
                request.amount(),
                request.category(),
                null,
                null,
                null,
                request.description(),
                request.createdBy());
        return ResponseEntity.ok("Expenditure processed successfully");
    }

    @GetMapping("/projects/{projectId}/management")
    public Result<BudgetService.BudgetManagementResponse> getBudgetManagement(@PathVariable Long projectId) {
        return Result.ok(budgetService.getBudgetManagement(projectId, securityHelper.getCurrentUserId()));
    }

    @PostMapping("/projects/{projectId}/adjustments")
    public Result<Void> submitBudgetAdjustment(@PathVariable Long projectId,
                                               @RequestBody SubmitBudgetAdjustmentRequest request) {
        budgetService.submitBudgetAdjustment(
                projectId,
                request.requestedBudget(),
                request.reasonText(),
                securityHelper.getCurrentUserId());
        return Result.ok(null);
    }

    @PostMapping("/projects/{projectId}/expenditures")
    public Result<Void> createExpenditure(@PathVariable Long projectId,
                                          @RequestBody BudgetService.ExpenditureRecordRequest request) {
        budgetService.createExpenditureRecord(projectId, request, securityHelper.getCurrentUserId());
        return Result.ok(null);
    }

    @GetMapping("/warning-config")
    public Result<BudgetService.BudgetWarningConfigResponse> getBudgetWarningConfig() {
        return Result.ok(budgetService.getBudgetWarningConfig());
    }

    @GetMapping("/dashboard")
    public Result<List<BudgetService.BudgetDashboardItemResponse>> getBudgetDashboard() {
        return Result.ok(budgetService.getBudgetDashboard(securityHelper.getCurrentUserId()));
    }

    public record SubmitBudgetAdjustmentRequest(
            BigDecimal requestedBudget,
            String reasonText
    ) {}
}