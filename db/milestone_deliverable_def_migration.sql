-- 里程碑交付物定义表
-- 结构化存储每个里程碑阶段需要的核心交付物清单
CREATE TABLE IF NOT EXISTS milestone_deliverable_def (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  milestone_code VARCHAR(4) NOT NULL COMMENT '里程碑阶段代码 G0-G9',
  slot_code VARCHAR(64) NOT NULL COMMENT '交付物槽位编码，如 INITIATION_REPORT',
  slot_name VARCHAR(256) NOT NULL COMMENT '交付物名称，如 立项报告',
  is_required TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否必填',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
  description VARCHAR(512) DEFAULT NULL COMMENT '交付物说明',
  allowed_file_types VARCHAR(256) DEFAULT '.pdf,.doc,.docx' COMMENT '允许的文件类型',
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_milestone_slot (milestone_code, slot_code),
  KEY idx_milestone (milestone_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='里程碑交付物定义表';

-- 初始化 G0-G9 各阶段的核心交付物定义
INSERT INTO milestone_deliverable_def (milestone_code, slot_code, slot_name, is_required, sort_no, description) VALUES
-- G0 项目立项
('G0', 'INITIATION_REPORT', '立项报告', 1, 1, '项目立项报告，包含项目背景、目标、计划等'),
('G0', 'TARGET_EVALUATION', '靶点评估文档', 1, 2, '靶点可行性评估报告'),

-- G1 先导化合物确认
('G1', 'LEAD_COMPOUND', '先导化合物', 1, 1, '先导化合物结构及数据'),
('G1', 'PATENT_ANALYSIS_G1', '专利分析', 1, 2, '专利自由实施分析报告'),

-- G2 优选化合物
('G2', 'OPTIMIZED_COMPOUND', '优选化合物', 1, 1, '优选化合物结构及数据'),
('G2', 'PATENT_ANALYSIS_G2', '专利分析', 1, 2, '专利自由实施分析报告'),

-- G3 PCC提名
('G3', 'PCC_NOMINATION', 'PCC 提名报告', 1, 1, '候选化合物提名报告'),
('G3', 'IN_VITRO_IN_VIVO_EFFICACY', '体内外药效数据', 1, 2, '体内外药效学研究数据'),
('G3', 'PRELIMINARY_ADME', '初步ADME数据', 1, 3, '初步ADME性质研究数据'),
('G3', 'PRELIMINARY_SAFETY', '初步安全性评估', 1, 4, '初步安全性评估数据'),
('G3', 'PATENT_STRATEGY', '专利策略文档', 1, 5, '专利布局策略文档'),

-- G4 临床前开发完成
('G4', 'GLP_TOXICOLOGY', 'GLP毒理报告', 1, 1, 'GLP毒理学研究报告'),
('G4', 'EFFICACY_SUMMARY', '药效总结报告', 1, 2, '药效学研究总结报告'),
('G4', 'CMC_PRELIMINARY', 'CMC初步总结报告', 1, 3, '化学、生产和控制初步总结'),
('G4', 'PATENT_FTO', '专利FTO报告', 1, 4, '专利自由实施分析报告'),

-- G5 IND获批
('G5', 'IND_DOSSIER', 'IND申报资料', 1, 1, '临床试验申请申报资料'),
('G5', 'ACCEPTANCE_NOTICE', '受理通知书', 1, 2, 'NMPA受理通知书'),
('G5', 'CLINICAL_APPROVAL', '临床试验批件', 1, 3, '临床试验批准通知书'),

-- G6 临床I期
('G6', 'PHASE1_SUMMARY', '临床I期总结报告', 1, 1, '临床I期试验总结报告'),
('G6', 'PHASE2_PROTOCOL', '临床II期试验方案', 1, 2, '临床II期试验方案'),

-- G7 临床II期
('G7', 'PHASE2_SUMMARY', '临床II期总结报告', 1, 1, '临床II期试验总结报告'),
('G7', 'PHASE3_PROTOCOL', '临床III期试验方案', 1, 2, '临床III期试验方案'),
('G7', 'REGISTRATION_STRATEGY', '注册策略确认文档', 1, 3, '注册策略确认文档'),

-- G8 临床III期
('G8', 'PHASE3_REPORT', '临床III期研究报告', 1, 1, '临床III期研究总结报告'),
('G8', 'POST_MARKETING_COMMITMENT', '上市后承诺文档', 1, 2, '上市后承诺相关文档'),

-- G9 NDA获批
('G9', 'NDA_DOSSIER', 'NDA申报资料', 1, 1, '新药上市申请申报资料'),
('G9', 'NDA_ACCEPTANCE', '受理通知书', 1, 2, 'NMPA受理通知书'),
('G9', 'MARKETING_AUTHORIZATION', '药品注册证书', 1, 3, '药品注册证书/上市许可');