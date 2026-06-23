package com.kbd.pms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 项目详情响应：基本信息 + 当前里程碑 + 预算执行概况（快照维度）。
 */
public record ProjectDetailResponse(
    long id,
    String projectCode,
    String projectName,
    String levelCode,
    String levelName,
    String indication,
    String targetPathway,
    String tppSummary,
    String description,
    String mechanism,
    String unmetNeeds,
    String scientificBasis,
    String expectedIndication,
    String administrationRoute,
    String dosageForm,
    String dosageFrequency,
    String efficacyTarget,
    String safetyAdvantage,
    String differentiation,
    BigDecimal budgetTotal,
    /** 预估PCC提名日期（对应G0计划日期） */
    LocalDate plannedPccDate,
    /** 预估IND获批日期（对应G5计划日期） */
    LocalDate plannedIndDate,
    /** 预估NDA获批日期（对应G9计划日期） */
    LocalDate plannedNdaDate,
    /** 预估项目结束日期 */
    LocalDate plannedEndDate,
    /** 阶段预算至PCC（万元） */
    BigDecimal budgetToPcc,
    /** 科学风险：靶点有效性风险、成药性风险、安全性风险 */
    String riskScientific,
    /** 竞争风险：主要竞品进展 */
    String riskCompetitive,
    /** 注册风险：法规路径不确定性 */
    String riskRegulatory,
    /** 建议与所需支持：简述需要PMC提供的资源或决策支持 */
    String suggestionAndSupport,
    Long pmUserId,
    /** 数据库中的 workflow/生命周期状态枚举名（DRAFT/ACTIVE/...） */
    String projectStatus,
    /**
     * 面向业务的阶段描述，例如立项后处于 G0：{@code G0-项目立项}。
     */
    String lifecyclePhaseLabel,
    /** 立项状态：null / SUBMITTED / APPROVED / REJECTED */
    String initiationStatus,
    ProcessOversightDeptDto processOversightDept,
    CurrentMilestoneDto currentMilestone,
    BudgetExecutionSummaryDto budgetExecution
) {

  public record ProcessOversightDeptDto(long deptId, String deptCode, String deptName) {}

  public record CurrentMilestoneDto(String code, String name, String phaseLabel) {}

  public record BudgetExecutionSummaryDto(
      BigDecimal plannedTotalAmount,
      BigDecimal totalSpent,
      BigDecimal utilizationRatio,
      String warningLevel,
      /** 快照月份 YYYY-MM，无快照时为 null */
      String snapshotMonth
  ) {}
}
