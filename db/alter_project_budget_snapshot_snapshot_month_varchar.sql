-- 修复 project_budget_snapshot.snapshot_month 列类型与实体映射不匹配的问题
-- 执行顺序：第8阶段，在最新数据库结构导入后执行

USE kbd_pm_system;

ALTER TABLE project_budget_snapshot
  MODIFY snapshot_month VARCHAR(7) NOT NULL;
