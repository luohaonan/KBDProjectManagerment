-- MySQL 8.0 DDL for “创新药研发项目管理系统”
-- Charset / collation choices aim for Chinese text + stable comparisons.

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET time_zone = '+00:00';

-- -----------------------------
-- Core dictionaries
-- -----------------------------

CREATE TABLE IF NOT EXISTS org_department (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dept_code VARCHAR(32) NOT NULL,
  dept_name VARCHAR(128) NOT NULL,
  dept_type ENUM('PDT','ROSS','OTHER') NOT NULL DEFAULT 'OTHER',
  parent_id BIGINT UNSIGNED NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_org_department_code (dept_code),
  KEY idx_org_department_parent (parent_id),
  CONSTRAINT fk_org_department_parent FOREIGN KEY (parent_id) REFERENCES org_department(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS iam_user (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_no VARCHAR(32) NOT NULL,
  display_name VARCHAR(64) NOT NULL,
  email VARCHAR(128) NULL,
  dept_id BIGINT UNSIGNED NULL,
  title VARCHAR(64) NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_iam_user_no (user_no),
  UNIQUE KEY uk_iam_user_email (email),
  KEY idx_iam_user_dept (dept_id),
  CONSTRAINT fk_iam_user_dept FOREIGN KEY (dept_id) REFERENCES org_department(id)
) ENGINE=InnoDB;

-- PMC is a committee; members can change over time.
CREATE TABLE IF NOT EXISTS governance_committee (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  committee_code VARCHAR(32) NOT NULL,
  committee_name VARCHAR(128) NOT NULL,
  chair_user_id BIGINT UNSIGNED NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_governance_committee_code (committee_code),
  KEY idx_governance_committee_chair (chair_user_id),
  CONSTRAINT fk_governance_committee_chair FOREIGN KEY (chair_user_id) REFERENCES iam_user(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS governance_committee_member (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  committee_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  member_role ENUM('CHAIR','MEMBER','SECRETARY','OBSERVER') NOT NULL DEFAULT 'MEMBER',
  effective_from DATE NOT NULL,
  effective_to DATE NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_committee_member_active (committee_id, user_id, effective_from),
  KEY idx_committee_member_user (user_id),
  CONSTRAINT fk_committee_member_committee FOREIGN KEY (committee_id) REFERENCES governance_committee(id),
  CONSTRAINT fk_committee_member_user FOREIGN KEY (user_id) REFERENCES iam_user(id),
  CONSTRAINT ck_committee_member_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
) ENGINE=InnoDB;

-- 项目分级（六级标准，来自制度附件表）
CREATE TABLE IF NOT EXISTS project_level (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  level_code VARCHAR(8) NOT NULL,          -- e.g. H-L, G-L, H-Q, G-Q, G-T, C-L, C-Q (制度里写 C-L/C-Q)
  level_name VARCHAR(64) NOT NULL,         -- e.g. 火力全开 临床重大
  definition_text TEXT NOT NULL,
  governance_text TEXT NULL,               -- 管理特点（星级、管控等）
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_project_level_code (level_code)
) ENGINE=InnoDB;

-- 里程碑字典：G0-G9（制度附件表）
CREATE TABLE IF NOT EXISTS milestone_def (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  milestone_code VARCHAR(4) NOT NULL,      -- G0..G9
  milestone_name VARCHAR(128) NOT NULL,
  stage_definition TEXT NOT NULL,
  core_deliverables TEXT NOT NULL,
  lead_dept_text VARCHAR(256) NOT NULL,    -- 文档中“主导部门”可能是多部门，先存文本；如需结构化可再拆映射表
  decision_gate VARCHAR(128) NOT NULL,     -- PMC评审 / 项目组内部评审 / PMC结项评审 等
  sort_no INT NOT NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_milestone_def_code (milestone_code),
  CONSTRAINT ck_milestone_def_code CHECK (REGEXP_LIKE(milestone_code, '^G[0-9]$'))
) ENGINE=InnoDB;

-- -----------------------------
-- Project core
-- -----------------------------

CREATE TABLE IF NOT EXISTS project (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_no VARCHAR(16) NOT NULL,         -- e.g. KBD0001
  level_id BIGINT UNSIGNED NOT NULL,
  -- Full display/project identifier required by制度: e.g. H-L-KBD0001
  -- NOTE: MySQL generated columns cannot reference other tables, so this is stored explicitly
  -- and validated by CHECK; application/service layer should also enforce consistency with level_id.
  project_code VARCHAR(32) NOT NULL,
  project_name VARCHAR(256) NOT NULL,
  target_pathway VARCHAR(256) NULL,        -- 靶点/通路
  indication VARCHAR(256) NULL,            -- 适应症
  tpp_summary TEXT NULL,                   -- TPP 初拟摘要
  pm_user_id BIGINT UNSIGNED NULL,         -- 项目经理
  pmc_committee_id BIGINT UNSIGNED NULL,   -- PMC（默认公司级，可按项目指定）
  process_oversight_dept_id BIGINT UNSIGNED NULL, -- 流程监管/效率管理部（制度：效率管理部维护 PMS）
  current_milestone_id BIGINT UNSIGNED NULL,
  status ENUM('DRAFT','ACTIVE','PAUSED','TERMINATED','CLOSED') NOT NULL DEFAULT 'DRAFT',
  terminated_reason TEXT NULL,             -- No-Go/终止原因
  start_date DATE NULL,
  end_date DATE NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_by BIGINT UNSIGNED NULL,
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_project_no (project_no),
  UNIQUE KEY uk_project_code (project_code),
  KEY idx_project_level (level_id),
  KEY idx_project_pm (pm_user_id),
  KEY idx_project_process_oversight_dept (process_oversight_dept_id),
  KEY idx_project_status (status),
  KEY idx_project_current_milestone (current_milestone_id),
  CONSTRAINT fk_project_level FOREIGN KEY (level_id) REFERENCES project_level(id),
  CONSTRAINT fk_project_pm FOREIGN KEY (pm_user_id) REFERENCES iam_user(id),
  CONSTRAINT fk_project_pmc FOREIGN KEY (pmc_committee_id) REFERENCES governance_committee(id),
  CONSTRAINT fk_project_process_oversight_dept FOREIGN KEY (process_oversight_dept_id) REFERENCES org_department(id),
  CONSTRAINT fk_project_current_milestone FOREIGN KEY (current_milestone_id) REFERENCES milestone_def(id),
  CONSTRAINT fk_project_created_by FOREIGN KEY (created_by) REFERENCES iam_user(id),
  CONSTRAINT fk_project_updated_by FOREIGN KEY (updated_by) REFERENCES iam_user(id),
  CONSTRAINT ck_project_no CHECK (REGEXP_LIKE(project_no, '^KBD[0-9]{4,}$')),
  CONSTRAINT ck_project_code CHECK (REGEXP_LIKE(project_code, '^(H-L|G-L|H-Q|G-Q|G-T|C-L|C-Q)-KBD[0-9]{4,}$')),
  CONSTRAINT ck_project_dates CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
) ENGINE=InnoDB;

-- PDT/矩阵团队：项目-部门-人员（可表达“各职能部参与”）
CREATE TABLE IF NOT EXISTS project_team_member (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  dept_id BIGINT UNSIGNED NULL,
  team_role ENUM('PM','PDT_LEAD','FUNCTION_LEAD','MEMBER') NOT NULL DEFAULT 'MEMBER',
  effective_from DATE NOT NULL,
  effective_to DATE NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_project_team_member (project_id, user_id, effective_from),
  KEY idx_project_team_project (project_id),
  KEY idx_project_team_user (user_id),
  KEY idx_project_team_dept (dept_id),
  CONSTRAINT fk_project_team_project FOREIGN KEY (project_id) REFERENCES project(id),
  CONSTRAINT fk_project_team_user FOREIGN KEY (user_id) REFERENCES iam_user(id),
  CONSTRAINT fk_project_team_dept FOREIGN KEY (dept_id) REFERENCES org_department(id),
  CONSTRAINT ck_project_team_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
) ENGINE=InnoDB;

-- 每个项目每个里程碑的计划/实际/评审结果（Go/Conditional/No Go）
CREATE TABLE IF NOT EXISTS project_milestone (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_id BIGINT UNSIGNED NOT NULL,
  milestone_id BIGINT UNSIGNED NOT NULL,
  planned_date DATE NULL,
  actual_date DATE NULL,
  status ENUM('NOT_STARTED','IN_PROGRESS','SUBMITTED','APPROVED','CONDITIONAL_APPROVED','REJECTED') NOT NULL DEFAULT 'NOT_STARTED',
  decision_result ENUM('GO','CONDITIONAL_GO','NO_GO') NULL,
  conditional_deadline DATETIME(3) NULL, -- Conditional Go 观察期截止（<=3个月）
  decision_notes TEXT NULL,
  decision_at DATETIME(3) NULL,
  decided_by BIGINT UNSIGNED NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_project_milestone (project_id, milestone_id),
  KEY idx_project_milestone_project (project_id),
  KEY idx_project_milestone_status (status),
  CONSTRAINT fk_project_milestone_project FOREIGN KEY (project_id) REFERENCES project(id),
  CONSTRAINT fk_project_milestone_def FOREIGN KEY (milestone_id) REFERENCES milestone_def(id),
  CONSTRAINT fk_project_milestone_decided_by FOREIGN KEY (decided_by) REFERENCES iam_user(id),
  CONSTRAINT ck_project_milestone_dates CHECK (actual_date IS NULL OR planned_date IS NULL OR actual_date >= planned_date)
) ENGINE=InnoDB;

-- 交付物/附件上传记录（用于里程碑评审“核心交付物已上传”校验）
CREATE TABLE IF NOT EXISTS project_document (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_id BIGINT UNSIGNED NOT NULL,
  milestone_id BIGINT UNSIGNED NULL,
  doc_type VARCHAR(64) NOT NULL,           -- e.g. PCC_NOMINATION_REPORT
  doc_name VARCHAR(256) NOT NULL,
  storage_uri VARCHAR(1024) NOT NULL,      -- 文件存储地址（对象存储URL/文件服务器路径等）
  uploaded_by BIGINT UNSIGNED NULL,
  uploaded_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_doc_project_milestone (project_id, milestone_id),
  KEY idx_doc_project_type (project_id, doc_type),
  CONSTRAINT fk_doc_project FOREIGN KEY (project_id) REFERENCES project(id),
  CONSTRAINT fk_doc_milestone FOREIGN KEY (milestone_id) REFERENCES milestone_def(id),
  CONSTRAINT fk_doc_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES iam_user(id)
) ENGINE=InnoDB;

-- 里程碑历史（可追溯性：提交/审批每一次动作都落日志）
CREATE TABLE IF NOT EXISTS milestone_history (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_id BIGINT UNSIGNED NOT NULL,
  project_milestone_id BIGINT UNSIGNED NOT NULL,
  action ENUM('SUBMIT_REVIEW','DECISION') NOT NULL,
  from_status VARCHAR(32) NULL,
  to_status VARCHAR(32) NULL,
  actor_user_id BIGINT UNSIGNED NULL,
  action_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  notes TEXT NULL,
  payload_json JSON NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_mh_project (project_id, action_at),
  KEY idx_mh_pm (project_milestone_id, action_at),
  CONSTRAINT fk_mh_project FOREIGN KEY (project_id) REFERENCES project(id),
  CONSTRAINT fk_mh_project_milestone FOREIGN KEY (project_milestone_id) REFERENCES project_milestone(id),
  CONSTRAINT fk_mh_actor FOREIGN KEY (actor_user_id) REFERENCES iam_user(id)
) ENGINE=InnoDB;

-- -----------------------------
-- Budgeting
-- -----------------------------

CREATE TABLE IF NOT EXISTS project_budget_policy (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_id BIGINT UNSIGNED NOT NULL,
  yellow_threshold DECIMAL(6,4) NOT NULL DEFAULT 0.8000, -- 80%
  red_threshold DECIMAL(6,4) NOT NULL DEFAULT 0.9500,    -- 95%
  currency_code CHAR(3) NOT NULL DEFAULT 'CNY',
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_project_budget_policy_project (project_id),
  CONSTRAINT fk_project_budget_policy_project FOREIGN KEY (project_id) REFERENCES project(id),
  CONSTRAINT ck_budget_thresholds CHECK (yellow_threshold > 0 AND red_threshold > 0 AND red_threshold > yellow_threshold AND red_threshold <= 1.0000)
) ENGINE=InnoDB;

-- 预算计划（全周期/年度/阶段滚动）；内部/外部费用拆分
CREATE TABLE IF NOT EXISTS project_budget_plan (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_id BIGINT UNSIGNED NOT NULL,
  plan_type ENUM('LIFECYCLE','ANNUAL','STAGE_ROLLING') NOT NULL,
  fiscal_year INT NULL,                                  -- ANNUAL 时必填
  stage_from_milestone_id BIGINT UNSIGNED NULL,           -- STAGE_ROLLING 可填，表示覆盖区间
  stage_to_milestone_id BIGINT UNSIGNED NULL,
  version_no INT NOT NULL DEFAULT 1,
  internal_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  external_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  total_amount DECIMAL(18,2) GENERATED ALWAYS AS (internal_amount + external_amount) STORED,
  approved_status ENUM('DRAFT','SUBMITTED','APPROVED','REJECTED') NOT NULL DEFAULT 'DRAFT',
  approved_at DATETIME(3) NULL,
  approved_by BIGINT UNSIGNED NULL,
  notes TEXT NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_by BIGINT UNSIGNED NULL,
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_budget_plan_project (project_id),
  KEY idx_budget_plan_type_year (plan_type, fiscal_year),
  CONSTRAINT fk_budget_plan_project FOREIGN KEY (project_id) REFERENCES project(id),
  CONSTRAINT fk_budget_plan_from FOREIGN KEY (stage_from_milestone_id) REFERENCES milestone_def(id),
  CONSTRAINT fk_budget_plan_to FOREIGN KEY (stage_to_milestone_id) REFERENCES milestone_def(id),
  CONSTRAINT fk_budget_plan_approved_by FOREIGN KEY (approved_by) REFERENCES iam_user(id),
  CONSTRAINT fk_budget_plan_created_by FOREIGN KEY (created_by) REFERENCES iam_user(id),
  CONSTRAINT fk_budget_plan_updated_by FOREIGN KEY (updated_by) REFERENCES iam_user(id),
  CONSTRAINT ck_budget_plan_year CHECK (plan_type <> 'ANNUAL' OR fiscal_year IS NOT NULL),
  CONSTRAINT ck_budget_plan_amounts CHECK (internal_amount >= 0 AND external_amount >= 0)
) ENGINE=InnoDB;

-- 实际支出流水（“无预算不支出”在业务层校验；这里提供账本与追溯）
CREATE TABLE IF NOT EXISTS project_budget_ledger (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_id BIGINT UNSIGNED NOT NULL,
  occurred_on DATE NOT NULL,
  expense_category ENUM('INTERNAL','EXTERNAL') NOT NULL,
  amount DECIMAL(18,2) NOT NULL,
  vendor_name VARCHAR(256) NULL,         -- 外部：CRO/CDMO/中心等
  reference_no VARCHAR(64) NULL,         -- 合同/发票/报销单号等
  description VARCHAR(512) NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_budget_ledger_project_date (project_id, occurred_on),
  KEY idx_budget_ledger_project_category (project_id, expense_category),
  CONSTRAINT fk_budget_ledger_project FOREIGN KEY (project_id) REFERENCES project(id),
  CONSTRAINT fk_budget_ledger_created_by FOREIGN KEY (created_by) REFERENCES iam_user(id),
  CONSTRAINT ck_budget_ledger_amount CHECK (amount >= 0)
) ENGINE=InnoDB;

-- 月度预算执行快照：用于报表与阈值预警（80%/95%）
CREATE TABLE IF NOT EXISTS project_budget_snapshot (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_id BIGINT UNSIGNED NOT NULL,
  budget_plan_id BIGINT UNSIGNED NULL,     -- 对应某次“阶段预算/年度预算/全周期预算”的监控口径
  snapshot_month VARCHAR(7) NOT NULL,         -- YYYY-MM
  internal_spent DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  external_spent DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  total_spent DECIMAL(18,2) GENERATED ALWAYS AS (internal_spent + external_spent) STORED,
  planned_total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  utilization_ratio DECIMAL(10,6) GENERATED ALWAYS AS (CASE WHEN planned_total_amount = 0 THEN 0 ELSE total_spent / planned_total_amount END) STORED,
  warning_level ENUM('NONE','YELLOW','RED') NOT NULL DEFAULT 'NONE',
  generated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_budget_snapshot (project_id, snapshot_month, budget_plan_id),
  KEY idx_budget_snapshot_project_month (project_id, snapshot_month),
  CONSTRAINT fk_budget_snapshot_project FOREIGN KEY (project_id) REFERENCES project(id),
  CONSTRAINT fk_budget_snapshot_plan FOREIGN KEY (budget_plan_id) REFERENCES project_budget_plan(id),
  CONSTRAINT ck_budget_snapshot_month CHECK (REGEXP_LIKE(snapshot_month, '^[0-9]{4}-[0-9]{2}$')),
  CONSTRAINT ck_budget_snapshot_amounts CHECK (internal_spent >= 0 AND external_spent >= 0 AND planned_total_amount >= 0)
) ENGINE=InnoDB;

-- 阶段预算限额（G0-G9）
CREATE TABLE IF NOT EXISTS budget_limit (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_id BIGINT UNSIGNED NOT NULL,
  milestone_code VARCHAR(4) NOT NULL,       -- G0, G1, ..., G9
  approved_budget DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  created_by BIGINT UNSIGNED NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_by BIGINT UNSIGNED NULL,
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_budget_limit_project_milestone (project_id, milestone_code),
  KEY idx_budget_limit_project (project_id),
  CONSTRAINT fk_budget_limit_project FOREIGN KEY (project_id) REFERENCES project(id),
  CONSTRAINT ck_budget_limit_budget CHECK (approved_budget >= 0),
  CONSTRAINT ck_budget_limit_milestone CHECK (milestone_code REGEXP '^G[0-9]$')
) ENGINE=InnoDB;

-- -----------------------------
-- Workflow (approval flow)
-- -----------------------------

CREATE TABLE IF NOT EXISTS wf_template (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  template_code VARCHAR(64) NOT NULL,       -- e.g. PROJECT_INITIATION, MILESTONE_REVIEW, PROJECT_CHANGE
  template_name VARCHAR(128) NOT NULL,
  description TEXT NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_wf_template_code (template_code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS wf_template_node (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  template_id BIGINT UNSIGNED NOT NULL,
  node_code VARCHAR(64) NOT NULL,
  node_name VARCHAR(128) NOT NULL,
  node_type ENUM('START','APPROVAL','CONDITION','END') NOT NULL,
  sort_no INT NOT NULL,
  approver_mode ENUM('USER','ROLE','COMMITTEE') NULL, -- APPROVAL 节点用
  approver_ref VARCHAR(128) NULL,                     -- USER: user_no / ROLE: role_code / COMMITTEE: committee_code
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_wf_template_node (template_id, node_code),
  KEY idx_wf_template_node_template (template_id),
  CONSTRAINT fk_wf_template_node_template FOREIGN KEY (template_id) REFERENCES wf_template(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS wf_instance (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  template_id BIGINT UNSIGNED NOT NULL,
  business_type VARCHAR(64) NOT NULL,   -- e.g. PROJECT, MILESTONE, CHANGE_REQUEST
  business_id BIGINT UNSIGNED NOT NULL,
  status ENUM('DRAFT','RUNNING','APPROVED','REJECTED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
  started_by BIGINT UNSIGNED NULL,
  started_at DATETIME(3) NULL,
  finished_at DATETIME(3) NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_wf_instance_business (business_type, business_id),
  KEY idx_wf_instance_template (template_id),
  CONSTRAINT fk_wf_instance_template FOREIGN KEY (template_id) REFERENCES wf_template(id),
  CONSTRAINT fk_wf_instance_started_by FOREIGN KEY (started_by) REFERENCES iam_user(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS wf_task (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  instance_id BIGINT UNSIGNED NOT NULL,
  node_id BIGINT UNSIGNED NOT NULL,
  task_name VARCHAR(128) NOT NULL,
  assignee_user_id BIGINT UNSIGNED NULL,
  status ENUM('PENDING','APPROVED','REJECTED','CANCELLED') NOT NULL DEFAULT 'PENDING',
  decided_at DATETIME(3) NULL,
  decision_notes TEXT NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_wf_task_instance (instance_id),
  KEY idx_wf_task_assignee (assignee_user_id, status),
  CONSTRAINT fk_wf_task_instance FOREIGN KEY (instance_id) REFERENCES wf_instance(id),
  CONSTRAINT fk_wf_task_node FOREIGN KEY (node_id) REFERENCES wf_template_node(id),
  CONSTRAINT fk_wf_task_assignee FOREIGN KEY (assignee_user_id) REFERENCES iam_user(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS wf_action_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  instance_id BIGINT UNSIGNED NOT NULL,
  task_id BIGINT UNSIGNED NULL,
  action ENUM('SUBMIT','APPROVE','REJECT','CANCEL','COMMENT','SYSTEM') NOT NULL,
  actor_user_id BIGINT UNSIGNED NULL,
  action_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  payload_json JSON NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_wf_action_instance (instance_id, action_at),
  KEY idx_wf_action_task (task_id),
  CONSTRAINT fk_wf_action_instance FOREIGN KEY (instance_id) REFERENCES wf_instance(id),
  CONSTRAINT fk_wf_action_task FOREIGN KEY (task_id) REFERENCES wf_task(id),
  CONSTRAINT fk_wf_action_actor FOREIGN KEY (actor_user_id) REFERENCES iam_user(id)
) ENGINE=InnoDB;

-- -----------------------------
-- Project change control (变更记录 + 审批流挂接)
-- -----------------------------

CREATE TABLE IF NOT EXISTS project_change_request (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_id BIGINT UNSIGNED NOT NULL,
  change_type ENUM(
    'OBJECTIVE_SCOPE',
    'MILESTONE_SCHEDULE',
    'BUDGET',
    'OWNER_PM',
    'PAUSE_TERMINATE',
    'OTHER'
  ) NOT NULL,
  reason_text TEXT NOT NULL,
  before_text TEXT NULL,
  after_text TEXT NULL,
  impact_milestone_text TEXT NULL,
  impact_budget_text TEXT NULL,
  impact_resource_text TEXT NULL,
  requested_by BIGINT UNSIGNED NULL,
  requested_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  status ENUM('DRAFT','SUBMITTED','APPROVED','REJECTED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
  wf_instance_id BIGINT UNSIGNED NULL,
  pmc_decision ENUM('APPROVE','REJECT','CONDITIONAL_APPROVE') NULL,
  pmc_decision_text TEXT NULL,
  pmc_decided_at DATETIME(3) NULL,
  pmc_decided_by BIGINT UNSIGNED NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_change_project (project_id, requested_at),
  KEY idx_change_status (status),
  CONSTRAINT fk_change_project FOREIGN KEY (project_id) REFERENCES project(id),
  CONSTRAINT fk_change_requested_by FOREIGN KEY (requested_by) REFERENCES iam_user(id),
  CONSTRAINT fk_change_wf_instance FOREIGN KEY (wf_instance_id) REFERENCES wf_instance(id),
  CONSTRAINT fk_change_pmc_decided_by FOREIGN KEY (pmc_decided_by) REFERENCES iam_user(id)
) ENGINE=InnoDB;

-- -----------------------------
-- Seed data (制度内置字典)
-- -----------------------------

INSERT INTO project_level (level_code, level_name, definition_text, governance_text)
VALUES
('H-L', '火力全开 临床重大', '公司战略核心；FIC/BIC潜力；重大未满足临床需求；重磅潜质；国际化与对外授权价值。', '资源保障力★★★★★ 洞察决策力★★★★★ 人才驱动力★★★★★ 体系管控力★★★★★'),
('G-L', '临床重大', '公司战略核心；FIC/BIC潜力；重大未满足临床需求；重磅潜质。', '资源保障力★★★★ 洞察决策力★★★★★ 人才驱动力★★★★ 体系管控力★★★★★'),
('H-Q', '火力全开 重大临床前', '公司研发管线核心；FIC/BIC潜力；重大未满足临床需求；重磅潜质；国际化与对外授权价值。', '资源保障力★★★★★ 洞察决策力★★★★★ 人才驱动力★★★★★ 体系管控力★★★★'),
('G-Q', '重大临床前', '公司研发管线核心；FIC/BIC潜力；重大未满足临床需求；重磅潜质。', '资源保障力★★★★ 洞察决策力★★★★ 人才驱动力★★★★ 体系管控力★★★★'),
('G-T', '重大探索', '探索性新靶点/新机制或技术风险较高；作为研发管线补充；具有重大市场价值。', '资源保障力★★★ 洞察决策力★★★★ 人才驱动力★★★ 体系管控力★★★★'),
('C-L', '产能项目（临床）', '具有巨大市场潜能；快速布局创新开发；锚定行业管线缺口。', '资源保障力★★ 洞察决策力★★★ 人才驱动力★★ 体系管控力★★★'),
('C-Q', '产能项目（临床前）', '具有巨大市场潜能；快速布局创新开发；锚定行业管线缺口。', '资源保障力★★ 洞察决策力★★★ 人才驱动力★★ 体系管控力★★★')
ON DUPLICATE KEY UPDATE
  level_name = VALUES(level_name),
  definition_text = VALUES(definition_text),
  governance_text = VALUES(governance_text);

INSERT INTO milestone_def (milestone_code, milestone_name, stage_definition, core_deliverables, lead_dept_text, decision_gate, sort_no)
VALUES
('G0', '项目立项', '完成靶点评估、立项申请', '立项报告', '新药资讯部', 'PMC立项决策', 0),
('G1', '先导化合物确认', '获得具有明确活性的先导化合物系列', '先导化合物、专利申请号', '新药化学部', '项目组内部评审', 1),
('G2', '优选化合物', '获得具有明确体内药效的优选化合物', '优选化合物、专利申请号', '新药化学部', '项目组内部评审', 2),
('G3', '候选化合物提名 (PCC)', '综合评估后，正式提名一个或多个化合物作为临床前开发候选物', 'PCC提名报告（含体内外药效、初步ADME、初步安全性、专利策略）', '新药化学部', 'PMC评审', 3),
('G4', '临床前开发完成 (GLP)', '完成所有GLP毒理研究、药效及药代动力学研究，具备申请IND条件', 'GLP毒理报告、药效总结报告、CMC初步总结报告、专利FTO报告', '新药生物部/新药化学部', 'PMC评审', 4),
('G5', '临床试验申请获批 (IND)', '向监管机构递交IND申请并获批', 'IND申报资料、受理通知书、临床试验批件/默示许可文件', '药政合规部', 'PMC评审', 5),
('G6', '临床Ⅰ期', '完成健康受试者或患者的药代动力学、安全性和耐受性研究', 'Ⅰ期总结报告、Ⅰ期临床试验方案', '新药临床部', 'PMC评审', 6),
('G7', '临床Ⅱ期', '完成在目标患者群体中的初步疗效和安全性验证', 'Ⅱ期总结报告、Ⅱ期临床试验方案、关键注册策略确认', '新药临床部', 'PMC评审', 7),
('G8', '临床Ⅲ期', '完成关键性注册临床试验', 'Ⅲ期临床研究报告', '新药临床部', 'PMC评审', 8),
('G9', '新药上市申请获批 (NDA)', '递交NDA并获批上市', 'NDA申报资料、受理通知书、药品注册证书', '药政合规部', 'PMC结项评审', 9)
ON DUPLICATE KEY UPDATE
  milestone_name = VALUES(milestone_name),
  stage_definition = VALUES(stage_definition),
  core_deliverables = VALUES(core_deliverables),
  lead_dept_text = VALUES(lead_dept_text),
  decision_gate = VALUES(decision_gate),
  sort_no = VALUES(sort_no);

-- Recommended baseline departments (制度第二章)
INSERT INTO org_department (dept_code, dept_name, dept_type)
VALUES
('PDT_CHEM', '新药化学部', 'PDT'),
('PDT_BIO', '新药生物部', 'PDT'),
('PDT_CLIN', '新药临床部', 'PDT'),
('ROSS_INFO', '新药资讯部', 'ROSS'),
('ROSS_BD', '商务拓展部', 'ROSS'),
('ROSS_EFF', '效率管理部', 'ROSS'),
('ROSS_REG', '药政合规部', 'ROSS')
ON DUPLICATE KEY UPDATE
  dept_name = VALUES(dept_name),
  dept_type = VALUES(dept_type);

