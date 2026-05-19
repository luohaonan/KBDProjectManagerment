package com.kbd.pms.entity;

public final class Enums {
  private Enums() {}

  /**
   * 里程碑业务状态（制度口径）。
   *
   * <p>注意：数据库层的 {@link ProjectMilestoneStatus} 还包含 SUBMITTED 等工作流态，
   * 此枚举用于对外/业务表达（MilestoneService 会做映射）。</p>
   */
  public enum MilestoneStatus { NOT_STARTED, IN_PROGRESS, GO, CONDITIONAL_GO, NO_GO }

  /** 十个里程碑阶段：G0..G9 */
  public enum MilestoneStage { G0, G1, G2, G3, G4, G5, G6, G7, G8, G9 }

  public enum DepartmentType { PDT, ROSS, OTHER }

  public enum CommitteeMemberRole { CHAIR, MEMBER, SECRETARY, OBSERVER }

  public enum ProjectStatus { DRAFT, ACTIVE, PAUSED, TERMINATED, CLOSED }

  public enum ProjectTeamRole { PM, PDT_LEAD, FUNCTION_LEAD, MEMBER }

  public enum ProjectMilestoneStatus {
    NOT_STARTED, IN_PROGRESS, SUBMITTED, APPROVED, CONDITIONAL_APPROVED, REJECTED
  }

  public enum MilestoneDecisionResult { GO, CONDITIONAL_GO, NO_GO }

  public enum BudgetPlanType { LIFECYCLE, ANNUAL, STAGE_ROLLING }

  public enum ApprovalStatus { DRAFT, SUBMITTED, APPROVED, REJECTED }

  public enum ExpenseCategory { INTERNAL, EXTERNAL }

  public enum WarningLevel { NONE, YELLOW, RED }

  public enum WfNodeType { START, APPROVAL, CONDITION, END }

  public enum ApproverMode { USER, ROLE, COMMITTEE }

  public enum WfInstanceStatus { DRAFT, RUNNING, APPROVED, REJECTED, CANCELLED }

  public enum WfTaskStatus { PENDING, APPROVED, REJECTED, CANCELLED }

  public enum WfAction { SUBMIT, APPROVE, REJECT, CANCEL, COMMENT, SYSTEM }

  public enum ChangeType {
    OBJECTIVE_SCOPE,
    MILESTONE_SCHEDULE,
    BUDGET,
    OWNER_PM,
    PAUSE_TERMINATE,
    OTHER
  }

  public enum PmcDecision { APPROVE, REJECT, CONDITIONAL_APPROVE }

  public enum ComplianceStatus { PENDING, APPROVED, REJECTED }

  public enum TerminationTaskStatus { OPEN, COMPLETED, OVERDUE }
}

