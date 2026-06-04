-- ============================================================
-- 演示用新药研发测试项目（G0–G9 不同阶段）
-- 执行：mysql -u root -p kbd_pm_system < db/seed_demo_projects.sql
-- ============================================================

USE `kbd_pm_system`;

SET @oversight_dept_id = (SELECT id FROM org_department WHERE dept_code = 'ROSS_EFF' LIMIT 1);
SET @pm_iam_id = 2; -- 与 user 表 pm_user 对应（需先同步 iam_user）

-- 同步 iam_user（立项/项目外键依赖），ID 与 RBAC user 表对齐
INSERT INTO iam_user (id, user_no, display_name, email, dept_id, is_active, created_at, updated_at)
SELECT u.id, u.username, u.username, u.email, u.department_id, u.is_active, u.created_at, u.updated_at
FROM `user` u
ON DUPLICATE KEY UPDATE
  display_name = VALUES(display_name),
  email = VALUES(email),
  dept_id = VALUES(dept_id),
  is_active = VALUES(is_active);

-- 删除已存在的同编号演示项目（可重复执行）
DELETE pm FROM project_milestone pm
INNER JOIN project p ON p.id = pm.project_id
WHERE p.project_no IN ('KBD0007','KBD0008','KBD0009','KBD0010','KBD0011','KBD0012');

DELETE bp FROM project_budget_policy bp
INNER JOIN project p ON p.id = bp.project_id
WHERE p.project_no IN ('KBD0007','KBD0008','KBD0009','KBD0010','KBD0011','KBD0012');

DELETE ia FROM initiation_approval ia
INNER JOIN project p ON p.id = ia.project_id
WHERE p.project_no IN ('KBD0007','KBD0008','KBD0009','KBD0010','KBD0011','KBD0012');

DELETE FROM project
WHERE project_no IN ('KBD0007','KBD0008','KBD0009','KBD0010','KBD0011','KBD0012');

-- ---------- 1. G0 立项审批中 ----------
INSERT INTO project (
  project_no, level_id, project_code, project_name,
  target_pathway, indication, tpp_summary, description,
  pm_user_id, initiator_user_id, process_oversight_dept_id,
  current_milestone_id, status, initiation_status, initiation_submitted_at, initiation_application,
  budget_total, planned_pcc_date, planned_ind_date, planned_nda_date, planned_end_date,
  start_date, created_by, updated_by, created_at, updated_at
) VALUES (
  'KBD0007', 1, 'H-L-KBD0007', 'KU-101 酪氨酸激酶抑制剂',
  'EGFR/HER2', '非小细胞肺癌（NSCLC）',
  '口服小分子TKI，目标ORR≥45%，安全性优于三代药物。',
  '[Demo] G0 initiation - approval in progress (SUBMITTED).',
  @pm_iam_id, @pm_iam_id, @oversight_dept_id,
  (SELECT id FROM milestone_def WHERE milestone_code = 'G0'),
  'ACTIVE', 'SUBMITTED', UTC_TIMESTAMP(3),
  'Request PMC initiation review meeting and approval.',
  8500.00, '2027-06-30', '2029-12-31', '2032-06-30', '2033-12-31',
  '2026-01-15', @pm_iam_id, @pm_iam_id, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)
);

-- ---------- 2. G1 先导化合物（立项已通过）----------
INSERT INTO project (
  project_no, level_id, project_code, project_name,
  target_pathway, indication, tpp_summary, description,
  pm_user_id, process_oversight_dept_id,
  current_milestone_id, status, initiation_status, initiation_submitted_at,
  budget_total, planned_pcc_date, planned_ind_date, planned_nda_date,
  start_date, created_by, updated_by, created_at, updated_at
) VALUES (
  'KBD0008', 4, 'G-Q-KBD0008', 'BS-202 双特异性抗体',
  'PD-1 × CTLA-4', '晚期黑色素瘤',
  '双抗免疫联合机制，探索性重大临床前项目。',
  '[Demo] G1 lead compound stage - initiation APPROVED.',
  @pm_iam_id, @oversight_dept_id,
  (SELECT id FROM milestone_def WHERE milestone_code = 'G1'),
  'ACTIVE', 'APPROVED', '2025-11-01 10:00:00',
  12000.00, '2027-09-30', '2030-06-30', '2033-03-31',
  '2025-08-01', @pm_iam_id, @pm_iam_id, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)
);

-- ---------- 3. G3 候选化合物提名 PCC ----------
INSERT INTO project (
  project_no, level_id, project_code, project_name,
  target_pathway, indication, tpp_summary, description,
  pm_user_id, process_oversight_dept_id,
  current_milestone_id, status, initiation_status,
  budget_total, planned_pcc_date, planned_ind_date, planned_nda_date,
  start_date, created_by, updated_by, created_at, updated_at
) VALUES (
  'KBD0009', 2, 'G-L-KBD0009', 'SM-303 小分子抗肿瘤药',
  'KRAS G12C', '结直肠癌',
  '口服KRAS抑制剂，瞄准耐药后线治疗空白。',
  '[Demo] G3 PCC nomination - G0-G2 completed with Go.',
  @pm_iam_id, @oversight_dept_id,
  (SELECT id FROM milestone_def WHERE milestone_code = 'G3'),
  'ACTIVE', 'APPROVED',
  28000.00, '2026-12-31', '2028-06-30', '2031-12-31',
  '2024-03-01', @pm_iam_id, @pm_iam_id, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)
);

-- ---------- 4. G5 IND 获批 ----------
INSERT INTO project (
  project_no, level_id, project_code, project_name,
  target_pathway, indication, tpp_summary, description,
  pm_user_id, process_oversight_dept_id,
  current_milestone_id, status, initiation_status,
  budget_total, planned_pcc_date, planned_ind_date, planned_nda_date,
  start_date, created_by, updated_by, created_at, updated_at
) VALUES (
  'KBD0010', 3, 'H-Q-KBD0010', 'NA-404 核酸适配体药物',
  'VEGF', '湿性年龄相关性黄斑变性',
  '长效眼内给药核酸药物，减少注射频次。',
  '[Demo] G5 IND approved stage - preclinical complete.',
  @pm_iam_id, @oversight_dept_id,
  (SELECT id FROM milestone_def WHERE milestone_code = 'G5'),
  'ACTIVE', 'APPROVED',
  45000.00, '2025-06-30', '2026-05-28', '2029-12-31',
  '2022-06-01', @pm_iam_id, @pm_iam_id, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)
);

-- ---------- 5. G7 临床 II 期 ----------
INSERT INTO project (
  project_no, level_id, project_code, project_name,
  target_pathway, indication, tpp_summary, description,
  pm_user_id, process_oversight_dept_id,
  current_milestone_id, status, initiation_status,
  budget_total, planned_pcc_date, planned_ind_date, planned_nda_date,
  start_date, created_by, updated_by, created_at, updated_at
) VALUES (
  'KBD0011', 5, 'G-T-KBD0011', 'GT-505 体内基因治疗',
  'CFTR', '囊性纤维化',
  'AAV载体基因治疗，重大探索性管线。',
  '[Demo] G7 Phase II clinical trial in progress.',
  @pm_iam_id, @oversight_dept_id,
  (SELECT id FROM milestone_def WHERE milestone_code = 'G7'),
  'ACTIVE', 'APPROVED',
  62000.00, '2024-12-31', '2025-08-15', '2028-12-31',
  '2021-01-10', @pm_iam_id, @pm_iam_id, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)
);

-- ---------- 6. G9 NDA 上市申请 ----------
INSERT INTO project (
  project_no, level_id, project_code, project_name,
  target_pathway, indication, tpp_summary, description,
  pm_user_id, process_oversight_dept_id,
  current_milestone_id, status, initiation_status,
  budget_total, planned_pcc_date, planned_ind_date, planned_nda_date, planned_end_date,
  start_date, created_by, updated_by, created_at, updated_at
) VALUES (
  'KBD0012', 6, 'C-L-KBD0012', 'CG-606 改良型抗肿瘤生物药',
  'HER2', 'HER2阳性乳腺癌',
  '产能型临床项目，生物类似物+改良制剂。',
  '[Demo] G9 NDA filing stage - Phase III complete.',
  @pm_iam_id, @oversight_dept_id,
  (SELECT id FROM milestone_def WHERE milestone_code = 'G9'),
  'ACTIVE', 'APPROVED',
  38000.00, '2023-03-31', '2024-09-30', '2026-08-31', '2027-06-30',
  '2020-05-01', @pm_iam_id, @pm_iam_id, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)
);

-- ---------- Milestone rows G0-G9 per demo project ----------
-- KBD0007 @ G0
INSERT INTO project_milestone (project_id, milestone_id, planned_date, actual_date, status, decision_result, decision_at, created_at, updated_at)
SELECT p.id, md.id,
  DATE_ADD(CURDATE(), INTERVAL md.sort_no * 90 DAY),
  NULL,
  IF(md.sort_no = 0, 'IN_PROGRESS', 'NOT_STARTED'),
  NULL, NULL, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)
FROM project p CROSS JOIN milestone_def md
WHERE p.project_no = 'KBD0007' AND md.is_active = 1;

-- KBD0008 @ G1
INSERT INTO project_milestone (project_id, milestone_id, planned_date, actual_date, status, decision_result, decision_at, created_at, updated_at)
SELECT p.id, md.id,
  DATE_ADD(CURDATE(), INTERVAL (md.sort_no - 1) * 90 DAY),
  IF(md.sort_no < 1, DATE_SUB(CURDATE(), INTERVAL (1 - md.sort_no) * 60 DAY), NULL),
  CASE WHEN md.sort_no < 1 THEN 'APPROVED' WHEN md.sort_no = 1 THEN 'IN_PROGRESS' ELSE 'NOT_STARTED' END,
  IF(md.sort_no < 1, 'GO', NULL),
  IF(md.sort_no < 1, UTC_TIMESTAMP(3), NULL), UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)
FROM project p CROSS JOIN milestone_def md
WHERE p.project_no = 'KBD0008' AND md.is_active = 1;

-- KBD0009 @ G3
INSERT INTO project_milestone (project_id, milestone_id, planned_date, actual_date, status, decision_result, decision_at, created_at, updated_at)
SELECT p.id, md.id,
  DATE_ADD(CURDATE(), INTERVAL (md.sort_no - 3) * 90 DAY),
  IF(md.sort_no < 3, DATE_SUB(CURDATE(), INTERVAL (3 - md.sort_no) * 60 DAY), NULL),
  CASE WHEN md.sort_no < 3 THEN 'APPROVED' WHEN md.sort_no = 3 THEN 'IN_PROGRESS' ELSE 'NOT_STARTED' END,
  IF(md.sort_no < 3, 'GO', NULL),
  IF(md.sort_no < 3, UTC_TIMESTAMP(3), NULL), UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)
FROM project p CROSS JOIN milestone_def md
WHERE p.project_no = 'KBD0009' AND md.is_active = 1;

-- KBD0010 @ G5
INSERT INTO project_milestone (project_id, milestone_id, planned_date, actual_date, status, decision_result, decision_at, created_at, updated_at)
SELECT p.id, md.id,
  DATE_ADD(CURDATE(), INTERVAL (md.sort_no - 5) * 90 DAY),
  IF(md.sort_no < 5, DATE_SUB(CURDATE(), INTERVAL (5 - md.sort_no) * 60 DAY), NULL),
  CASE WHEN md.sort_no < 5 THEN 'APPROVED' WHEN md.sort_no = 5 THEN 'IN_PROGRESS' ELSE 'NOT_STARTED' END,
  IF(md.sort_no < 5, 'GO', NULL),
  IF(md.sort_no < 5, UTC_TIMESTAMP(3), NULL), UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)
FROM project p CROSS JOIN milestone_def md
WHERE p.project_no = 'KBD0010' AND md.is_active = 1;

-- KBD0011 @ G7
INSERT INTO project_milestone (project_id, milestone_id, planned_date, actual_date, status, decision_result, decision_at, created_at, updated_at)
SELECT p.id, md.id,
  DATE_ADD(CURDATE(), INTERVAL (md.sort_no - 7) * 90 DAY),
  IF(md.sort_no < 7, DATE_SUB(CURDATE(), INTERVAL (7 - md.sort_no) * 60 DAY), NULL),
  CASE WHEN md.sort_no < 7 THEN 'APPROVED' WHEN md.sort_no = 7 THEN 'IN_PROGRESS' ELSE 'NOT_STARTED' END,
  IF(md.sort_no < 7, 'GO', NULL),
  IF(md.sort_no < 7, UTC_TIMESTAMP(3), NULL), UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)
FROM project p CROSS JOIN milestone_def md
WHERE p.project_no = 'KBD0011' AND md.is_active = 1;

-- KBD0012 @ G9
INSERT INTO project_milestone (project_id, milestone_id, planned_date, actual_date, status, decision_result, decision_at, created_at, updated_at)
SELECT p.id, md.id,
  DATE_ADD(CURDATE(), INTERVAL (md.sort_no - 9) * 90 DAY),
  IF(md.sort_no < 9, DATE_SUB(CURDATE(), INTERVAL (9 - md.sort_no) * 60 DAY), NULL),
  CASE WHEN md.sort_no < 9 THEN 'APPROVED' WHEN md.sort_no = 9 THEN 'IN_PROGRESS' ELSE 'NOT_STARTED' END,
  IF(md.sort_no < 9, 'GO', NULL),
  IF(md.sort_no < 9, UTC_TIMESTAMP(3), NULL), UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)
FROM project p CROSS JOIN milestone_def md
WHERE p.project_no = 'KBD0012' AND md.is_active = 1;

-- ---------- 预算策略 ----------
INSERT INTO project_budget_policy (project_id, yellow_threshold, red_threshold, currency_code, created_at, updated_at)
SELECT p.id, 0.8000, 0.9500, 'CNY', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)
FROM project p
WHERE p.project_no IN ('KBD0007','KBD0008','KBD0009','KBD0010','KBD0011','KBD0012');

-- ---------- 可选：月度预算快照（仪表盘演示）----------
INSERT INTO project_budget_snapshot (
  project_id, snapshot_month, planned_total_amount, internal_spent, external_spent, warning_level
)
SELECT
  p.id,
  DATE_FORMAT(UTC_TIMESTAMP(), '%Y-%m'),
  COALESCE(p.budget_total, 0),
  ROUND(COALESCE(p.budget_total, 0) * (0.10 + (p.id % 5) * 0.05), 2),
  ROUND(COALESCE(p.budget_total, 0) * 0.05, 2),
  CASE
    WHEN (0.15 + (p.id % 5) * 0.08) >= 0.95 THEN 'RED'
    WHEN (0.15 + (p.id % 5) * 0.08) >= 0.80 THEN 'YELLOW'
    ELSE 'NONE'
  END
FROM project p
WHERE p.project_no IN ('KBD0007','KBD0008','KBD0009','KBD0010','KBD0011','KBD0012')
  AND NOT EXISTS (
    SELECT 1 FROM project_budget_snapshot s
    WHERE s.project_id = p.id AND s.snapshot_month = DATE_FORMAT(UTC_TIMESTAMP(), '%Y-%m')
  );

-- ---------- KBD0007 立项审批记录（演示审批流）----------
INSERT INTO initiation_approval (
  project_id, submitter_user_id, application_content, status, submitted_at, created_at, updated_at
)
SELECT
  p.id, @pm_iam_id,
  'Request PMC initiation review meeting and approval.',
  'SUBMITTED', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)
FROM project p WHERE p.project_no = 'KBD0007';

INSERT INTO initiation_approval_task (
  initiation_approval_id, approver_user_id, approver_role, sort_order, status, created_at, updated_at
)
SELECT
  ia.id, u.id, 'ROLE_PMC', ROW_NUMBER() OVER (ORDER BY u.id),
  'PENDING', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)
FROM initiation_approval ia
INNER JOIN project p ON p.id = ia.project_id AND p.project_no = 'KBD0007'
CROSS JOIN (
  SELECT DISTINCT u2.id
  FROM `user` u2
  INNER JOIN user_roles ur ON ur.user_id = u2.id
  INNER JOIN role r ON r.id = ur.role_id
  INNER JOIN role_permissions rp ON rp.role_id = r.id
  INNER JOIN permission perm ON perm.id = rp.permission_id
  WHERE perm.name = 'PERMISSION_APPROVE_INITIATION' AND u2.is_active = 1 AND u2.id <> @pm_iam_id
) u;

SELECT 'Demo projects seeded' AS message,
  project_no, project_code, project_name, initiation_status,
  (SELECT milestone_code FROM milestone_def md WHERE md.id = p.current_milestone_id) AS current_stage
FROM project p
WHERE project_no IN ('KBD0007','KBD0008','KBD0009','KBD0010','KBD0011','KBD0012')
ORDER BY project_no;
