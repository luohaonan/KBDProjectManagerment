-- 修复 MySQL TEXT 列与 Hibernate @Lob 映射不匹配的问题
-- 执行顺序：第8阶段，在最新数据库结构导入后执行

USE kbd_pm_system;

ALTER TABLE milestone_def
  MODIFY stage_definition TINYTEXT NOT NULL,
  MODIFY core_deliverables TINYTEXT NOT NULL;

ALTER TABLE project_level
  MODIFY definition_text TINYTEXT NOT NULL,
  MODIFY governance_text TINYTEXT NULL;

ALTER TABLE project
  MODIFY description TINYTEXT NULL,
  MODIFY mechanism TINYTEXT NULL,
  MODIFY scientific_basis TINYTEXT NULL,
  MODIFY efficacy_target TINYTEXT NULL,
  MODIFY safety_advantage TINYTEXT NULL,
  MODIFY differentiation TINYTEXT NULL,
  MODIFY tpp_summary TINYTEXT NULL,
  MODIFY terminated_reason TINYTEXT NULL,
  MODIFY risk_scientific TINYTEXT NULL,
  MODIFY risk_competitive TINYTEXT NULL,
  MODIFY risk_regulatory TINYTEXT NULL,
  MODIFY suggestion_and_support TINYTEXT NULL;

ALTER TABLE project_budget_plan
  MODIFY notes TINYTEXT NULL;

ALTER TABLE milestone_history
  MODIFY notes TINYTEXT NULL;

ALTER TABLE project_milestone
  MODIFY decision_notes TINYTEXT NULL;

ALTER TABLE project_change_request
  MODIFY reason_text TINYTEXT NOT NULL,
  MODIFY before_text TINYTEXT NULL,
  MODIFY after_text TINYTEXT NULL,
  MODIFY impact_milestone_text TINYTEXT NULL,
  MODIFY impact_budget_text TINYTEXT NULL,
  MODIFY impact_resource_text TINYTEXT NULL,
  MODIFY pmc_decision_text TINYTEXT NULL;

ALTER TABLE project_termination_task
  MODIFY task_description TINYTEXT NOT NULL COMMENT '任务详细描述';

ALTER TABLE wf_task
  MODIFY decision_notes TINYTEXT NULL;

ALTER TABLE wf_template
  MODIFY description TINYTEXT NULL;
