-- 修正 G0-G9 固定交付物槽位定义，使交付物管理页按固定文件名称占位展示。
-- 幂等执行：先失活旧定义，再按最新需求 upsert 固定清单。

UPDATE milestone_deliverable_def SET is_active = 0, updated_at = NOW();

INSERT INTO milestone_deliverable_def
  (milestone_code, slot_code, slot_name, is_required, sort_no, description, allowed_file_types, is_active, created_at, updated_at)
VALUES
  ('G0', 'INITIATION_REPORT', '立项报告', 1, 1, 'G0阶段固定交付物：立项报告', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, NOW(), NOW()),
  ('G0', 'TARGET_EVALUATION_REPORT', '靶点评估报告', 1, 2, 'G0阶段固定交付物：靶点评估报告', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, NOW(), NOW()),

  ('G1', 'LEAD_COMPOUND_REPORT', '先导化合物报告', 1, 1, 'G1阶段固定交付物：先导化合物报告', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, NOW(), NOW()),
  ('G1', 'PATENT_APPLICATION_REPORT_G1', '专利申请报告(G1)', 1, 2, 'G1阶段固定交付物：专利申请报告(G1)', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, NOW(), NOW()),

  ('G2', 'OPTIMIZED_COMPOUND_REPORT', '优选化合物报告', 1, 1, 'G2阶段固定交付物：优选化合物报告', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, NOW(), NOW()),
  ('G2', 'PATENT_APPLICATION_REPORT_G2', '专利申请报告(G2)', 1, 2, 'G2阶段固定交付物：专利申请报告(G2)', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, NOW(), NOW()),

  ('G3', 'PCC_NOMINATION_REPORT', 'PCC提名报告', 1, 1, 'G3阶段固定交付物：PCC提名报告', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, NOW(), NOW()),

  ('G4', 'CMC_PRELIMINARY_SUMMARY_REPORT', 'CMC初步总结报告', 1, 1, 'G4阶段固定交付物：CMC初步总结报告', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, NOW(), NOW()),
  ('G4', 'PATENT_FTO_REPORT', '专利FTO报告', 1, 2, 'G4阶段固定交付物：专利FTO报告', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, NOW(), NOW()),
  ('G4', 'GLP_TOXICOLOGY_REPORT', 'GLP毒理报告', 1, 3, 'G4阶段固定交付物：GLP毒理报告', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, NOW(), NOW()),
  ('G4', 'EFFICACY_SUMMARY_REPORT', '药效总结报告', 1, 4, 'G4阶段固定交付物：药效总结报告', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, NOW(), NOW()),

  ('G5', 'IND_DOSSIER', 'IND申报资料', 1, 1, 'G5阶段固定交付物：IND申报资料', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, NOW(), NOW()),
  ('G5', 'ACCEPTANCE_NOTICE', '受理通知书', 1, 2, 'G5阶段固定交付物：受理通知书', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, NOW(), NOW()),
  ('G5', 'CLINICAL_TRIAL_APPROVAL_OR_IMPLIED_LICENSE', '临床试验批件/默示许可文件', 1, 3, 'G5阶段固定交付物：临床试验批件/默示许可文件', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, NOW(), NOW()),

  ('G6', 'CLINICAL_SUMMARY_REPORT_G6', '临床期总结报告G6', 1, 1, 'G6阶段固定交付物：临床期总结报告G6', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, NOW(), NOW()),
  ('G6', 'CLINICAL_TRIAL_PROTOCOL_G6', '临床期试验方案G6', 1, 2, 'G6阶段固定交付物：临床期试验方案G6', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, NOW(), NOW()),

  ('G7', 'CLINICAL_SUMMARY_REPORT_G7', '临床期总结报告G7', 1, 1, 'G7阶段固定交付物：临床期总结报告G7', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, NOW(), NOW()),
  ('G7', 'CLINICAL_TRIAL_PROTOCOL_G7', '临床期试验方案G7', 1, 2, 'G7阶段固定交付物：临床期试验方案G7', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, NOW(), NOW()),
  ('G7', 'KEY_REGISTRATION_STRATEGY_CONFIRMATION', '关键注册策略确认', 1, 3, 'G7阶段固定交付物：关键注册策略确认', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, NOW(), NOW()),

  ('G8', 'CLINICAL_RESEARCH_REPORT', '临床期临床研究报告', 1, 1, 'G8阶段固定交付物：临床期临床研究报告', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, NOW(), NOW()),

  ('G9', 'NDA_DOSSIER', 'NDA申报资料', 1, 1, 'G9阶段固定交付物：NDA申报资料', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, NOW(), NOW()),
  ('G9', 'NDA_ACCEPTANCE_NOTICE', '受理通知书', 1, 2, 'G9阶段固定交付物：受理通知书', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, NOW(), NOW()),
  ('G9', 'DRUG_REGISTRATION_CERTIFICATE', '药品注册证书', 1, 3, 'G9阶段固定交付物：药品注册证书', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  slot_name = VALUES(slot_name),
  is_required = VALUES(is_required),
  sort_no = VALUES(sort_no),
  description = VALUES(description),
  allowed_file_types = VALUES(allowed_file_types),
  is_active = VALUES(is_active),
  updated_at = NOW();