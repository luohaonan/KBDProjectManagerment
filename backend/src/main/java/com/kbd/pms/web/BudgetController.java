package com.kbd.pms.web;

import com.kbd.pms.dto.ExpenseRequest;
import com.kbd.pms.service.BudgetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
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
        budgetService.processExpenditure(request.projectId(), request.amount(), request.category(),
                                        request.description(), request.createdBy());
        return ResponseEntity.ok("Expenditure processed successfully");
    }
}