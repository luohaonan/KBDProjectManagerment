-- ============================================================
-- 阶段 9️⃣：扩展 project 表，增加立项表单所需的字段
-- ============================================================
-- 这些字段用于存储项目创建/编辑时的完整立项信息
-- 包括：项目描述、科学依据、目标产品概览(TPP)、差异化优势、预算等
-- ============================================================

ALTER TABLE `project`
  ADD COLUMN `description` TEXT NULL COMMENT '项目描述' AFTER `tpp_summary`,
  ADD COLUMN `mechanism` TEXT NULL COMMENT '生物学机制' AFTER `description`,
  ADD COLUMN `unmet_needs` VARCHAR(512) NULL COMMENT '未满足的临床需求' AFTER `mechanism`,
  ADD COLUMN `scientific_basis` TEXT NULL COMMENT '科学依据' AFTER `unmet_needs`,
  ADD COLUMN `expected_indication` VARCHAR(256) NULL COMMENT '预期适应症' AFTER `scientific_basis`,
  ADD COLUMN `administration_route` VARCHAR(64) NULL COMMENT '给药途径' AFTER `expected_indication`,
  ADD COLUMN `dosage_form` VARCHAR(64) NULL COMMENT '剂型' AFTER `administration_route`,
  ADD COLUMN `dosage_frequency` VARCHAR(64) NULL COMMENT '剂量频率' AFTER `dosage_form`,
  ADD COLUMN `efficacy_target` TEXT NULL COMMENT '预期疗效指标' AFTER `dosage_frequency`,
  ADD COLUMN `safety_advantage` TEXT NULL COMMENT '安全性优势' AFTER `efficacy_target`,
  ADD COLUMN `differentiation` TEXT NULL COMMENT '与竞品相比的核心优势' AFTER `safety_advantage`,
  ADD COLUMN `budget_total` DECIMAL(18,2) NULL COMMENT '总预算' AFTER `differentiation`;
