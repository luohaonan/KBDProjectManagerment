package com.kbd.pms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 更新项目信息请求体。
 */
public record ProjectUpdateRequest(
    @NotBlank @Size(max = 256) String projectName,
    /** 项目分级代号，如 H-L、G-L（对应 project_level.level_code） */
    @NotBlank @Size(max = 8) String levelCode,
    @Size(max = 256) String indication,
    @Size(max = 256) String targetPathway,
    String tppSummary,
    String description,
    String mechanism,
    @Size(max = 512) String unmetNeeds,
    String scientificBasis,
    @Size(max = 256) String expectedIndication,
    @Size(max = 64) String administrationRoute,
    @Size(max = 64) String dosageForm,
    @Size(max = 64) String dosageFrequency,
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
    String suggestionAndSupport
) {}
