-- ============================================================
-- 阶段 🔟：扩展 project 表，增加日期字段、阶段预算、风险评估、建议与所需支持
-- ============================================================
-- 新增字段说明：
-- 1. 预估PCC提名日期 (planned_pcc_date) - 对应里程碑G0的计划日期
-- 2. 预估IND获批日期 (planned_ind_date) - 对应里程碑G5的计划日期
-- 3. 预估NDA获批日期 (planned_nda_date) - 对应里程碑G9的计划日期
-- 4. 阶段预算至PCC (budget_to_pcc) - 阶段预算至PCC（万元）
-- 5. 科学风险 (risk_scientific) - 靶点有效性风险、成药性风险、安全性风险
-- 6. 竞争风险 (risk_competitive) - 主要竞品进展
-- 7. 注册风险 (risk_regulatory) - 法规路径不确定性
-- 8. 建议与所需支持 (suggestion_and_support) - 简述需要PMC提供的资源或决策支持
-- ============================================================
-- 注意：@Lob 字段使用 TINYTEXT 以匹配 Hibernate 映射（参考 alter_lob_text_columns_to_tinytext.sql）
-- ============================================================

ALTER TABLE `project`
  ADD COLUMN `planned_pcc_date` DATE NULL COMMENT '预估PCC提名日期（对应G0计划日期）' AFTER `end_date`,
  ADD COLUMN `planned_ind_date` DATE NULL COMMENT '预估IND获批日期（对应G5计划日期）' AFTER `planned_pcc_date`,
  ADD COLUMN `planned_nda_date` DATE NULL COMMENT '预估NDA获批日期（对应G9计划日期）' AFTER `planned_ind_date`,
  ADD COLUMN `budget_to_pcc` DECIMAL(18,2) NULL COMMENT '阶段预算至PCC（万元）' AFTER `budget_total`,
  ADD COLUMN `risk_scientific` TINYTEXT NULL COMMENT '科学风险：靶点有效性风险、成药性风险、安全性风险' AFTER `budget_to_pcc`,
  ADD COLUMN `risk_competitive` TINYTEXT NULL COMMENT '竞争风险：主要竞品进展' AFTER `risk_scientific`,
  ADD COLUMN `risk_regulatory` TINYTEXT NULL COMMENT '注册风险：法规路径不确定性' AFTER `risk_competitive`,
  ADD COLUMN `suggestion_and_support` TINYTEXT NULL COMMENT '建议与所需支持：简述需要PMC提供的资源或决策支持' AFTER `risk_regulatory`;
