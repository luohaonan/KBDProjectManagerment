package com.kbd.pms.service;

import com.kbd.pms.entity.*;
import com.kbd.pms.exception.BudgetExceededException;
import com.kbd.pms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private ProjectBudgetLedgerRepository ledgerRepository;
    @Mock
    private BudgetLimitRepository budgetLimitRepository;
    @Mock
    private ProjectMilestoneRepository milestoneRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private MilestoneDefRepository milestoneDefRepository;

    private BudgetService budgetService;

    @BeforeEach
    void setUp() {
        budgetService = new BudgetService(ledgerRepository, budgetLimitRepository, 
            milestoneRepository, projectRepository, milestoneDefRepository);
    }

    @Test
    void testProcessExpenditure_YellowWarning() {
        // Setup
        Long projectId = 1L;
        Long milestoneId = 2L;
        BigDecimal amount = BigDecimal.valueOf(100);
        String milestoneCode = "G3";

        // Mock milestone definition
        MilestoneDefEntity def = new MilestoneDefEntity();
        def.setMilestoneCode(milestoneCode);
        when(milestoneDefRepository.findById(milestoneId)).thenReturn(Optional.of(def));

        // Mock current milestone
        ProjectMilestoneEntity milestone = new ProjectMilestoneEntity();
        milestone.setProjectId(projectId);
        milestone.setMilestoneId(milestoneId);
        when(milestoneRepository.findByProjectIdOrderByIdAsc(projectId)).thenReturn(List.of(milestone));

        // Mock budget limit: 1000 total
        BudgetLimitEntity limit = createBudgetLimit(projectId, milestoneCode, BigDecimal.valueOf(1000));
        when(budgetLimitRepository.findByProjectIdAndMilestoneCode(projectId, milestoneCode)).thenReturn(Optional.of(limit));

        // Mock current spent: 750 (75%) - yellow warning threshold
        ProjectBudgetLedgerEntity ledger = createLedger(projectId, BigDecimal.valueOf(750));
        when(ledgerRepository.findByProjectId(projectId)).thenReturn(List.of(ledger));

        // Execute
        assertDoesNotThrow(() -> budgetService.processExpenditure(projectId, amount, Enums.ExpenseCategory.INTERNAL, "Test", 1L));

        // Verify ledger saved
        verify(ledgerRepository).save(any());
    }

    @Test
    void testProcessExpenditure_RedWarning() {
        // Setup
        Long projectId = 1L;
        Long milestoneId = 2L;
        BigDecimal amount = BigDecimal.valueOf(100);
        String milestoneCode = "G3";

        // Mock milestone definition
        MilestoneDefEntity def = new MilestoneDefEntity();
        def.setMilestoneCode(milestoneCode);
        when(milestoneDefRepository.findById(milestoneId)).thenReturn(Optional.of(def));

        // Mock current milestone
        ProjectMilestoneEntity milestone = new ProjectMilestoneEntity();
        milestone.setProjectId(projectId);
        milestone.setMilestoneId(milestoneId);
        when(milestoneRepository.findByProjectIdOrderByIdAsc(projectId)).thenReturn(List.of(milestone));

        // Mock budget limit: 1000 total
        BudgetLimitEntity limit = createBudgetLimit(projectId, milestoneCode, BigDecimal.valueOf(1000));
        when(budgetLimitRepository.findByProjectIdAndMilestoneCode(projectId, milestoneCode)).thenReturn(Optional.of(limit));

        // Mock current spent: 950 (95%) - red warning threshold
        ProjectBudgetLedgerEntity ledger = createLedger(projectId, BigDecimal.valueOf(950));
        when(ledgerRepository.findByProjectId(projectId)).thenReturn(List.of(ledger));

        // Execute & Verify
        BudgetExceededException exception = assertThrows(BudgetExceededException.class,
            () -> budgetService.processExpenditure(projectId, amount, Enums.ExpenseCategory.INTERNAL, "Test", 1L));
        assertTrue(exception.getMessage().contains("95%"));
    }

    @Test
    void testGetBudgetStatus() {
        // Setup
        Long projectId = 1L;
        Long milestoneId = 2L;
        String milestoneCode = "G3";

        // Mock milestone definition
        MilestoneDefEntity def = new MilestoneDefEntity();
        def.setMilestoneCode(milestoneCode);
        when(milestoneDefRepository.findById(milestoneId)).thenReturn(Optional.of(def));

        // Mock current milestone
        ProjectMilestoneEntity milestone = new ProjectMilestoneEntity();
        milestone.setProjectId(projectId);
        milestone.setMilestoneId(milestoneId);
        when(milestoneRepository.findByProjectIdOrderByIdAsc(projectId)).thenReturn(List.of(milestone));

        // Mock budget limit
        BudgetLimitEntity limit = createBudgetLimit(projectId, milestoneCode, BigDecimal.valueOf(1000));
        when(budgetLimitRepository.findByProjectIdAndMilestoneCode(projectId, milestoneCode)).thenReturn(Optional.of(limit));

        // Mock current spent
        ProjectBudgetLedgerEntity ledger = createLedger(projectId, BigDecimal.valueOf(500));
        when(ledgerRepository.findByProjectId(projectId)).thenReturn(List.of(ledger));

        // Execute
        BudgetService.BudgetSnapshotResponse response = budgetService.getBudgetStatus(projectId);

        // Verify
        assertEquals(BigDecimal.valueOf(1000), response.approvedBudget());
        assertEquals(BigDecimal.valueOf(500), response.totalSpent());
        assertEquals(BigDecimal.valueOf(0.5), response.utilizationRatio());
        assertEquals(Enums.WarningLevel.NONE, response.warningLevel());
    }

    // Helper methods to create entities with required fields
    private BudgetLimitEntity createBudgetLimit(Long projectId, String milestoneCode, BigDecimal budget) {
        BudgetLimitEntity entity = new BudgetLimitEntity();
        entity.setProjectId(projectId);
        entity.setMilestoneCode(milestoneCode);
        entity.setApprovedBudget(budget);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }

    private ProjectBudgetLedgerEntity createLedger(Long projectId, BigDecimal amount) {
        ProjectBudgetLedgerEntity entity = new ProjectBudgetLedgerEntity();
        entity.setProjectId(projectId);
        entity.setAmount(amount);
        entity.setOccurredOn(java.time.LocalDate.now());
        entity.setExpenseCategory(Enums.ExpenseCategory.INTERNAL);
        entity.setCreatedAt(Instant.now());
        return entity;
    }
}
