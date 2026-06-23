package com.kbd.pms.dto.project;

import java.math.BigDecimal;

/**
 * 项目详情响应：基本信息 + 当前里程碑 + 预算执行概况 + 流程监管信息。
 */
public record ProjectDetailResponse(
    Long id,
    String projectCode,
    String projectNo,
    String projectName,
    String levelCode,
    String levelName,
    String indication,
    String targetPathway,
    String tppSummary,
    Long pmUserId,
    /** 工作流/生命周期在库中的状态枚举名，如 ACTIVE */
    String projectStatus,
    /**
     * 面向业务的阶段描述，立项后默认处于 G0：{@code G0-项目立项}。
     */
    String lifecyclePhaseLabel,
    ProcessOversightDeptDto processOversightDept,
    CurrentMilestoneDto currentMilestone,
    BudgetExecutionSummaryDto budgetExecution
) {

  public record ProcessOversightDeptDto(Long deptId, String deptCode, String deptName) {}

  public record CurrentMilestoneDto(String milestoneCode, String milestoneName, String phaseLabel) {}

  /**
   * 预算执行概况：来自最近一次月度快照（若无快照则字段可为 null）。
   */
  public record BudgetExecutionSummaryDto(
      BigDecimal plannedTotalAmount,
      BigDecimal totalSpent,
      BigDecimal utilizationRatio,
      String warningLevel,
      String snapshotMonth
  ) {}
}
