package com.kbd.pms.workflow;

public record WorkflowStepDescriptor(
    String nodeCode,
    String nodeName,
    String nodeType,
    String normalizedStepCode,
    Integer sortOrder,
    String approverRule,
    String approverRuleLabel,
    String approverValue,
    Boolean isUploader,
    String decisionType,
    String expectedApproverLabel
) {}