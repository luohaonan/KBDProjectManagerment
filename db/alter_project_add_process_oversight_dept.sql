-- Run once on existing databases created before process_oversight_dept_id was added.
-- MySQL 8.0

ALTER TABLE project
  ADD COLUMN process_oversight_dept_id BIGINT UNSIGNED NULL
    COMMENT '流程监管部门（默认：效率管理部 ROSS_EFF）'
    AFTER pmc_committee_id,
  ADD KEY idx_project_process_oversight_dept (process_oversight_dept_id),
  ADD CONSTRAINT fk_project_process_oversight_dept
    FOREIGN KEY (process_oversight_dept_id) REFERENCES org_department (id);
