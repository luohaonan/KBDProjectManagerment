-- 文档管理模块数据库脚本
-- 创建文档表和审计日志表
-- 开发阶段：可单独执行（前置脚本已应用）

USE kbd_pm_system;

-- 创建文档表
CREATE TABLE IF NOT EXISTS `document` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `file_name` VARCHAR(256) NOT NULL COMMENT '文件名',
  `storage_path` VARCHAR(1024) NOT NULL COMMENT '存储路径',
  `file_type` VARCHAR(64) COMMENT '文件类型',
  `project_id` BIGINT UNSIGNED NOT NULL COMMENT '所属项目ID',
  `milestone_phase` ENUM('G0','G1','G2','G3','G4','G5','G6','G7','G8','G9') NOT NULL COMMENT '里程碑阶段',
  `uploader` BIGINT UNSIGNED NOT NULL COMMENT '上传人ID',
  `compliance_status` ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING' COMMENT '合规审核状态',
  `is_locked` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否锁定（归档后锁定）',
  `uploaded_at` DATETIME NOT NULL COMMENT '上传时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_project_phase` (`project_id`, `milestone_phase`),
  INDEX `idx_compliance_status` (`compliance_status`),
  INDEX `idx_uploader` (`uploader`),
  CONSTRAINT `fk_document_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_document_uploader` FOREIGN KEY (`uploader`) REFERENCES `iam_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档表';

-- 创建审计日志表
CREATE TABLE IF NOT EXISTS `audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '操作人ID',
  `action` VARCHAR(64) NOT NULL COMMENT '操作动作（UPLOAD/DOWNLOAD/DELETE/REVIEW）',
  `document_id` BIGINT NOT NULL COMMENT '文档ID',
  `timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `details` VARCHAR(1024) COMMENT '操作详情',
  PRIMARY KEY (`id`),
  INDEX `idx_document_id` (`document_id`),
  INDEX `idx_user_action` (`user_id`, `action`),
  INDEX `idx_timestamp` (`timestamp`),
  CONSTRAINT `fk_audit_log_user` FOREIGN KEY (`user_id`) REFERENCES `iam_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_audit_log_document` FOREIGN KEY (`document_id`) REFERENCES `document` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档审计日志表';

-- 插入文档管理相关权限
INSERT IGNORE INTO `permission` (`name`, `description`) VALUES
  ('DOCUMENT_UPLOAD', '上传文档'),
  ('DOCUMENT_DOWNLOAD', '下载文档'),
  ('DOCUMENT_DELETE', '删除文档'),
  ('DOCUMENT_REVIEW', '审核文档合规性'),
  ('DOCUMENT_VIEW_AUDIT', '查看文档审计日志');

-- 为相关角色分配文档管理权限
-- PMC 可以查看和下载文档
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `role` r, `permission` p
WHERE r.name = 'ROLE_PMC'
  AND p.name IN ('DOCUMENT_DOWNLOAD', 'DOCUMENT_VIEW_AUDIT');

-- PM 可以上传、下载、删除文档
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `role` r, `permission` p
WHERE r.name = 'ROLE_PM'
  AND p.name IN ('DOCUMENT_UPLOAD', 'DOCUMENT_DOWNLOAD', 'DOCUMENT_DELETE', 'DOCUMENT_VIEW_AUDIT');

-- 药政合规部可以审核文档
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `role` r, `permission` p
WHERE r.name = 'ROLE_COMPLIANCE'
  AND p.name IN ('DOCUMENT_DOWNLOAD', 'DOCUMENT_REVIEW', 'DOCUMENT_VIEW_AUDIT');

-- 管理员拥有所有权限
INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `role` r, `permission` p
WHERE r.name = 'ROLE_ADMIN';