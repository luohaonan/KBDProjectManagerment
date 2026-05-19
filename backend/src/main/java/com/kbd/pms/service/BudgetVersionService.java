package com.kbd.pms.service;

import com.kbd.pms.entity.Enums;
import com.kbd.pms.entity.ProjectBudgetPlanEntity;
import com.kbd.pms.exception.ApiException;
import com.kbd.pms.repository.ProjectBudgetPlanRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetVersionService {

  private final ProjectBudgetPlanRepository budgetPlanRepository;

  public BudgetVersionService(ProjectBudgetPlanRepository budgetPlanRepository) {
    this.budgetPlanRepository = budgetPlanRepository;
  }

  @Transactional
  public ProjectBudgetPlanEntity getOrCreateBaselinePlan(long projectId, Long actorUserId) {
    return budgetPlanRepository.findByProjectIdAndVersionNo(projectId, 1)
        .orElseGet(() -> createBaselinePlan(projectId, actorUserId));
  }

  @Transactional
  public ProjectBudgetPlanEntity createNextVersion(ProjectBudgetPlanEntity latest, Long actorUserId, String note) {
    if (latest == null) {
      throw new ApiException(500, "无法生成新的预算版本：缺少当前计划基线");
    }
    ProjectBudgetPlanEntity version = new ProjectBudgetPlanEntity();
    version.setProjectId(latest.getProjectId());
    version.setPlanType(latest.getPlanType());
    version.setFiscalYear(latest.getFiscalYear());
    version.setStageFromMilestoneId(latest.getStageFromMilestoneId());
    version.setStageToMilestoneId(latest.getStageToMilestoneId());
    version.setVersionNo(latest.getVersionNo() == null ? 2 : latest.getVersionNo() + 1);
    version.setInternalAmount(latest.getInternalAmount());
    version.setExternalAmount(latest.getExternalAmount());
    version.setApprovedStatus(Enums.ApprovalStatus.APPROVED);
    version.setApprovedAt(LocalDateTime.now());
    version.setApprovedBy(actorUserId);
    version.setNotes(note);
    version.setCreatedBy(actorUserId);
    version.setCreatedAt(Instant.now());
    version.setUpdatedBy(actorUserId);
    version.setUpdatedAt(Instant.now());
    return budgetPlanRepository.save(version);
  }

  public BaselineDeviation calculateBaselineDeviation(long projectId, BigDecimal currentSpent) {
    if (currentSpent == null) {
      currentSpent = BigDecimal.ZERO;
    }
    Optional<ProjectBudgetPlanEntity> baselineOpt = budgetPlanRepository.findByProjectIdAndVersionNo(projectId, 1);
    if (baselineOpt.isEmpty()) {
      return new BaselineDeviation(BigDecimal.ZERO, currentSpent, currentSpent, BigDecimal.ZERO);
    }
    ProjectBudgetPlanEntity baseline = baselineOpt.get();
    BigDecimal baselineAmount = baseline.getInternalAmount().add(baseline.getExternalAmount());
    BigDecimal varianceAmount = currentSpent.subtract(baselineAmount);
    BigDecimal variancePercent = BigDecimal.ZERO;
    if (baselineAmount.signum() != 0) {
      variancePercent = varianceAmount.divide(baselineAmount, 6, RoundingMode.HALF_UP);
    }
    return new BaselineDeviation(baselineAmount, currentSpent, varianceAmount, variancePercent);
  }

  private ProjectBudgetPlanEntity createBaselinePlan(long projectId, Long actorUserId) {
    ProjectBudgetPlanEntity baseline = new ProjectBudgetPlanEntity();
    baseline.setProjectId(projectId);
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

  public record BaselineDeviation(
      BigDecimal baselinePlannedAmount,
      BigDecimal currentSpent,
      BigDecimal varianceAmount,
      BigDecimal variancePercent) {}
}
