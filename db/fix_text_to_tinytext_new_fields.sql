-- ============================================================
-- 修复：将新添加的 TEXT 列修改为 TINYTEXT（匹配 Hibernate @Lob 映射）
-- 适用于已执行 alter_project_add_date_fields_and_risk.sql 但列类型为 TEXT 的情况
-- ============================================================

ALTER TABLE `project`
  MODIFY `risk_scientific` TINYTEXT NULL COMMENT '科学风险：靶点有效性风险、成药性风险、安全性风险',
  MODIFY `risk_competitive` TINYTEXT NULL COMMENT '竞争风险：主要竞品进展',
  MODIFY `risk_regulatory` TINYTEXT NULL COMMENT '注册风险：法规路径不确定性',
  MODIFY `suggestion_and_support` TINYTEXT NULL COMMENT '建议与所需支持：简述需要PMC提供的资源或决策支持';
