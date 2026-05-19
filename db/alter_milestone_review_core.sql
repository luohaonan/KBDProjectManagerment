-- Run once on existing databases created before milestone review core tables/columns were added.
-- MySQL 8.0

ALTER TABLE project
  ADD COLUMN terminated_reason TEXT NULL AFTER status;

ALTER TABLE project_milestone
  ADD COLUMN conditional_deadline DATETIME(3) NULL AFTER decision_result;

CREATE TABLE IF NOT EXISTS project_document (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_id BIGINT UNSIGNED NOT NULL,
  milestone_id BIGINT UNSIGNED NULL,
  doc_type VARCHAR(64) NOT NULL,
  doc_name VARCHAR(256) NOT NULL,
  storage_uri VARCHAR(1024) NOT NULL,
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

