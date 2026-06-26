package com.kbd.pms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 分步审批请求
 * 部门负责人审批 / PM技术初评 / 合规意见 / PMC决策 / PM内部评审
 */
public record StepApproveRequest(
    @NotNull Long actorUserId,
    @NotBlank String stepCode,       // 步骤代码: DEPT_HEAD_APPROVE / PM_TECH_REVIEW / COMPLIANCE_OPINION / PMC_DECISION / PM_INTERNAL_REVIEW
    @NotBlank String decision,       // APPROVED / REJECTED (部门负责人), GO/CONDITIONAL_GO/NO_GO (PMC/PM内部评审)
    String opinion,                   // 审批意见
    String techReview,               // 技术初评内容 (PM_TECH_REVIEW步骤)
    String complianceOpinion         // 合规性意见内容 (COMPLIANCE_OPINION步骤)
) {}