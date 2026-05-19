package com.kbd.pms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 立项报告数据 DTO - 供前端生成 PDF
 */
public record InitiationReportResponse(
    long projectId,
    String projectCode,
    String projectName,
    String levelCode,
    String levelName,
    String targetPathway,
    String indication,
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
    String initiatorName,
    String initiationTime
) {}
