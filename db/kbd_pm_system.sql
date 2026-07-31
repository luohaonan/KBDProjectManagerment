/*
 Navicat MySQL Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80043 (8.0.43)
 Source Host           : localhost:3306
 Source Schema         : kbd_pm_system

 Target Server Type    : MySQL
 Target Server Version : 80043 (8.0.43)
 File Encoding         : 65001

 Date: 31/07/2026 18:09:21
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for audit_log
-- ----------------------------
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint UNSIGNED NOT NULL COMMENT '操作人ID',
  `action` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作动作（UPLOAD/DOWNLOAD/DELETE/REVIEW）',
  `document_id` bigint NOT NULL COMMENT '文档ID',
  `timestamp` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `details` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作详情',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_document_id`(`document_id` ASC) USING BTREE,
  INDEX `idx_user_action`(`user_id` ASC, `action` ASC) USING BTREE,
  INDEX `idx_timestamp`(`timestamp` ASC) USING BTREE,
  CONSTRAINT `fk_audit_log_document` FOREIGN KEY (`document_id`) REFERENCES `document` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_audit_log_user` FOREIGN KEY (`user_id`) REFERENCES `iam_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 27 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文档审计日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of audit_log
-- ----------------------------
INSERT INTO `audit_log` VALUES (23, 8, 'UPLOAD_DELIVERABLE', 22, '2026-07-29 16:55:37', 'Milestone G0 deliverable uploaded: 立项报告');
INSERT INTO `audit_log` VALUES (24, 8, 'UPLOAD_DELIVERABLE', 23, '2026-07-29 16:55:41', 'Milestone G0 deliverable uploaded: 靶点评估文档');
INSERT INTO `audit_log` VALUES (25, 6, 'IMPORT_DELIVERABLE', 24, '2026-07-30 16:12:43', 'Imported historical deliverable into G0/TARGET_EVALUATION_REPORT');
INSERT INTO `audit_log` VALUES (26, 6, 'IMPORT_DELIVERABLE', 25, '2026-07-30 16:13:23', 'Imported historical deliverable into G0/INITIATION_REPORT');

-- ----------------------------
-- Table structure for budget_limit
-- ----------------------------
DROP TABLE IF EXISTS `budget_limit`;
CREATE TABLE `budget_limit`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id` bigint UNSIGNED NOT NULL,
  `milestone_code` varchar(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `approved_budget` decimal(18, 2) NOT NULL DEFAULT 0.00,
  `created_by` bigint UNSIGNED NULL DEFAULT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint UNSIGNED NULL DEFAULT NULL,
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_budget_limit_project_milestone`(`project_id` ASC, `milestone_code` ASC) USING BTREE,
  INDEX `idx_budget_limit_project`(`project_id` ASC) USING BTREE,
  CONSTRAINT `fk_budget_limit_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_budget_limit_budget` CHECK (`approved_budget` >= 0),
  CONSTRAINT `ck_budget_limit_milestone` CHECK (regexp_like(`milestone_code`,_utf8mb4'^G[0-9]$'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of budget_limit
-- ----------------------------

-- ----------------------------
-- Table structure for document
-- ----------------------------
DROP TABLE IF EXISTS `document`;
CREATE TABLE `document`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件名',
  `storage_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '存储路径',
  `file_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件类型',
  `project_id` bigint UNSIGNED NOT NULL COMMENT '所属项目ID',
  `milestone_phase` enum('G0','G1','G2','G3','G4','G5','G6','G7','G8','G9') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '里程碑阶段',
  `deliverable_slot_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '交付物槽位代码',
  `uploader` bigint UNSIGNED NOT NULL COMMENT '上传人ID',
  `compliance_status` enum('PENDING','APPROVED','REJECTED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '合规审核状态',
  `is_locked` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否锁定（归档后锁定）',
  `uploaded_at` datetime NOT NULL COMMENT '上传时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_phase`(`project_id` ASC, `milestone_phase` ASC) USING BTREE,
  INDEX `idx_compliance_status`(`compliance_status` ASC) USING BTREE,
  INDEX `idx_uploader`(`uploader` ASC) USING BTREE,
  CONSTRAINT `fk_document_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_document_uploader` FOREIGN KEY (`uploader`) REFERENCES `iam_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 26 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文档表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of document
-- ----------------------------
INSERT INTO `document` VALUES (22, 'IT资料.docx', 'C:\\KBD_PMS\\uploads\\H-L-KBD0013\\G0\\baebe81c-0557-426f-aa88-2c0a57a019a3.docx', '立项报告', 40, 'G0', 'INITIATION_REPORT', 8, 'PENDING', 0, '2026-07-29 16:55:37', '2026-07-29 16:55:37');
INSERT INTO `document` VALUES (23, 'IT资料.docx', 'C:\\KBD_PMS\\uploads\\H-L-KBD0013\\G0\\9c21eba5-7d98-4286-a983-415d80fe3534.docx', '靶点评估文档', 40, 'G0', 'TARGET_EVALUATION', 8, 'PENDING', 0, '2026-07-29 16:55:41', '2026-07-29 16:55:41');
INSERT INTO `document` VALUES (24, 'IT资料.docx', 'C:\\KBD_PMS\\uploads\\H-L-KBD0015\\G0\\e9bb657d-d689-4a3e-a420-debf11544cb8.docx', '靶点评估报告', 43, 'G0', 'TARGET_EVALUATION_REPORT', 6, 'PENDING', 0, '2026-07-30 16:12:43', '2026-07-30 16:12:43');
INSERT INTO `document` VALUES (25, 'PMC成员及各项目经理名单和工号-V2.xlsx', 'C:\\KBD_PMS\\uploads\\H-L-KBD0015\\G0\\2cf052b1-0f08-42a7-a3e7-73b907cc2a1a.xlsx', '立项报告', 43, 'G0', 'INITIATION_REPORT', 6, 'PENDING', 0, '2026-07-30 16:13:23', '2026-07-30 16:13:23');

-- ----------------------------
-- Table structure for governance_committee
-- ----------------------------
DROP TABLE IF EXISTS `governance_committee`;
CREATE TABLE `governance_committee`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `committee_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `committee_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `chair_user_id` bigint UNSIGNED NULL DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_governance_committee_code`(`committee_code` ASC) USING BTREE,
  INDEX `idx_governance_committee_chair`(`chair_user_id` ASC) USING BTREE,
  CONSTRAINT `fk_governance_committee_chair` FOREIGN KEY (`chair_user_id`) REFERENCES `iam_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of governance_committee
-- ----------------------------

-- ----------------------------
-- Table structure for governance_committee_member
-- ----------------------------
DROP TABLE IF EXISTS `governance_committee_member`;
CREATE TABLE `governance_committee_member`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `committee_id` bigint UNSIGNED NOT NULL,
  `user_id` bigint UNSIGNED NOT NULL,
  `member_role` enum('CHAIR','MEMBER','SECRETARY','OBSERVER') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'MEMBER',
  `effective_from` date NOT NULL,
  `effective_to` date NULL DEFAULT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_committee_member_active`(`committee_id` ASC, `user_id` ASC, `effective_from` ASC) USING BTREE,
  INDEX `idx_committee_member_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_committee_member_committee` FOREIGN KEY (`committee_id`) REFERENCES `governance_committee` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_committee_member_user` FOREIGN KEY (`user_id`) REFERENCES `iam_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_committee_member_dates` CHECK ((`effective_to` is null) or (`effective_to` >= `effective_from`))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of governance_committee_member
-- ----------------------------

-- ----------------------------
-- Table structure for iam_user
-- ----------------------------
DROP TABLE IF EXISTS `iam_user`;
CREATE TABLE `iam_user`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `display_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `dept_id` bigint UNSIGNED NULL DEFAULT NULL,
  `title` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_iam_user_no`(`user_no` ASC) USING BTREE,
  INDEX `idx_iam_user_dept`(`dept_id` ASC) USING BTREE,
  CONSTRAINT `fk_iam_user_dept` FOREIGN KEY (`dept_id`) REFERENCES `org_department` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of iam_user
-- ----------------------------
INSERT INTO `iam_user` VALUES (1, 'pmc_user', 'pmc_user', 'pmc@example.com', NULL, NULL, 1, '2026-04-27 13:58:56.286', '2026-05-20 10:21:26.000');
INSERT INTO `iam_user` VALUES (2, 'pm_user', 'pm_user', '836487344@qq.com', 9, NULL, 1, '2026-04-27 13:58:56.286', '2026-07-29 17:31:59.737');
INSERT INTO `iam_user` VALUES (3, 'dept_head', 'dept_head', 'dept@example.com', NULL, NULL, 1, '2026-04-27 13:58:56.286', '2026-05-20 10:21:26.000');
INSERT INTO `iam_user` VALUES (4, 'efficiency_user', 'efficiency_user', 'efficiency@example.com', NULL, NULL, 1, '2026-04-27 13:58:56.286', '2026-05-20 10:21:26.000');
INSERT INTO `iam_user` VALUES (5, 'compliance_user', 'compliance_user', 'compliance@example.com', NULL, NULL, 1, '2026-04-27 13:58:56.286', '2026-05-20 10:21:26.000');
INSERT INTO `iam_user` VALUES (6, 'admin_user', 'admin_user', 'admin@example.com', 10, NULL, 1, '2026-04-27 13:58:56.286', '2026-05-28 03:52:45.160');
INSERT INTO `iam_user` VALUES (7, 'test_user', 'test_user', NULL, 1, NULL, 1, '2026-07-26 20:47:45.898', '2026-07-26 20:47:45.898');
INSERT INTO `iam_user` VALUES (8, '资讯部执行人', '资讯部执行人', NULL, 4, NULL, 1, '2026-07-26 20:48:04.906', '2026-07-26 20:48:04.906');
INSERT INTO `iam_user` VALUES (9, '资讯部负责人', '资讯部负责人', NULL, 4, NULL, 1, '2026-07-26 20:48:35.573', '2026-07-26 20:48:35.573');
INSERT INTO `iam_user` VALUES (10, '化学部执行人', '化学部执行人', NULL, 1, NULL, 1, '2026-07-26 20:49:05.219', '2026-07-26 20:49:05.219');
INSERT INTO `iam_user` VALUES (11, '化学部负责人', '化学部负责人', NULL, 1, NULL, 1, '2026-07-26 20:49:22.962', '2026-07-26 20:49:22.962');
INSERT INTO `iam_user` VALUES (12, '药政合规部负责人', '药政合规部负责人', NULL, 7, NULL, 1, '2026-07-28 10:49:55.000', '2026-07-30 00:20:11.485');
INSERT INTO `iam_user` VALUES (13, '项目管理员', '项目管理员', '', NULL, NULL, 1, '2026-07-27 06:39:06.749', '2026-07-27 06:39:06.749');
INSERT INTO `iam_user` VALUES (17, '效率管理部负责人', '效率管理部负责人', '836487344@qq.com', 6, NULL, 1, '2026-07-30 02:16:57.917', '2026-07-30 02:16:57.917');
INSERT INTO `iam_user` VALUES (18, 'pmc_user1', 'pmc_user1', NULL, 10, NULL, 1, '2026-07-29 16:04:14.530', '2026-07-29 16:04:14.530');

-- ----------------------------
-- Table structure for initiation_approval
-- ----------------------------
DROP TABLE IF EXISTS `initiation_approval`;
CREATE TABLE `initiation_approval`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id` bigint UNSIGNED NOT NULL COMMENT '椤圭洰ID',
  `submitter_user_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '鎻愪氦浜猴紙椤圭洰缁忕悊锛',
  `application_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '绔嬮」鐢宠?鍐呭?',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SUBMITTED' COMMENT '鐘舵?锛歋UBMITTED/APPROVED/REJECTED',
  `submitted_at` datetime(3) NULL DEFAULT NULL COMMENT '鎻愪氦鏃堕棿',
  `finished_at` datetime(3) NULL DEFAULT NULL COMMENT '瀹屾垚鏃堕棿锛堝叏閮ㄥ?鎵瑰畬鎴愶級',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ia_project`(`project_id` ASC) USING BTREE,
  INDEX `idx_ia_status`(`status` ASC) USING BTREE,
  INDEX `fk_ia_submitter`(`submitter_user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of initiation_approval
-- ----------------------------
INSERT INTO `initiation_approval` VALUES (2, 10, 2, '测试立项申请内容', 'SUBMITTED', '2026-05-26 23:39:54.056', NULL, '2026-05-27 07:39:54.056', '2026-05-27 07:39:54.056');
INSERT INTO `initiation_approval` VALUES (3, 11, 6, '测试', 'SUBMITTED', '2026-05-27 19:44:28.405', NULL, '2026-05-28 03:44:28.405', '2026-05-28 03:44:28.405');
INSERT INTO `initiation_approval` VALUES (4, 12, 2, 'Request PMC initiation review meeting and approval.', 'APPROVED', '2026-05-28 06:58:55.869', '2026-06-23 21:48:59.769', '2026-05-28 06:58:55.869', '2026-06-24 05:48:59.848');
INSERT INTO `initiation_approval` VALUES (5, 18, 6, '测试', 'APPROVED', '2026-06-23 01:00:38.029', '2026-06-23 20:00:06.309', '2026-06-23 09:00:38.029', '2026-06-24 04:00:06.460');
INSERT INTO `initiation_approval` VALUES (6, 19, 6, '测试', 'APPROVED', '2026-06-28 23:47:25.942', '2026-06-28 23:50:56.285', '2026-06-29 07:47:25.942', '2026-06-29 07:50:56.299');

-- ----------------------------
-- Table structure for initiation_approval_task
-- ----------------------------
DROP TABLE IF EXISTS `initiation_approval_task`;
CREATE TABLE `initiation_approval_task`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `initiation_approval_id` bigint UNSIGNED NOT NULL COMMENT '绔嬮」鐢宠?瀹℃壒璁板綍ID',
  `approver_user_id` bigint UNSIGNED NOT NULL COMMENT '瀹℃壒浜虹敤鎴稩D锛圥MC鎴愬憳锛',
  `approver_role` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '瀹℃壒浜鸿?鑹',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '瀹℃壒椤哄簭',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '鐘舵?锛歅ENDING/APPROVED/REJECTED',
  `decision` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鍐崇瓥锛欰PPROVED/REJECTED',
  `opinion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '瀹℃壒鎰忚?',
  `decided_at` datetime(3) NULL DEFAULT NULL COMMENT '鍐崇瓥鏃堕棿',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_iat_approval`(`initiation_approval_id` ASC) USING BTREE,
  INDEX `idx_iat_approver`(`approver_user_id` ASC, `status` ASC) USING BTREE,
  CONSTRAINT `fk_iat_approval` FOREIGN KEY (`initiation_approval_id`) REFERENCES `initiation_approval` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of initiation_approval_task
-- ----------------------------
INSERT INTO `initiation_approval_task` VALUES (1, 3, 1, 'ROLE_PMC', 1, 'PENDING', NULL, NULL, NULL, '2026-05-28 06:52:55.478', '2026-05-28 06:52:55.478');
INSERT INTO `initiation_approval_task` VALUES (2, 4, 1, 'ROLE_PMC', 1, 'APPROVED', 'APPROVED', '测试', '2026-06-23 21:48:59.769', '2026-05-28 06:58:55.874', '2026-06-24 05:48:59.769');
INSERT INTO `initiation_approval_task` VALUES (3, 4, 6, 'ROLE_PMC', 2, 'APPROVED', 'APPROVED', '同意', '2026-05-28 00:49:37.659', '2026-05-28 06:58:55.874', '2026-05-28 08:49:37.659');
INSERT INTO `initiation_approval_task` VALUES (4, 5, 1, 'ROLE_PMC', 1, 'APPROVED', 'APPROVED', '同意', '2026-06-23 20:00:06.309', '2026-06-23 09:00:38.085', '2026-06-24 04:00:06.309');
INSERT INTO `initiation_approval_task` VALUES (5, 6, 1, 'ROLE_PMC', 1, 'APPROVED', 'APPROVED', '同意', '2026-06-28 23:50:56.285', '2026-06-29 07:47:26.037', '2026-06-29 07:50:56.285');

-- ----------------------------
-- Table structure for milestone_def
-- ----------------------------
DROP TABLE IF EXISTS `milestone_def`;
CREATE TABLE `milestone_def`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `milestone_code` varchar(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `milestone_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `stage_definition` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `core_deliverables` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `lead_dept_text` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `decision_gate` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `sort_no` int NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_milestone_def_code`(`milestone_code` ASC) USING BTREE,
  CONSTRAINT `ck_milestone_def_code` CHECK (regexp_like(`milestone_code`,_utf8mb4'^G[0-9]$'))
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of milestone_def
-- ----------------------------
INSERT INTO `milestone_def` VALUES (1, 'G0', '项目立项', '完成靶点评估、立项申请', '立项报告', '新药资讯部', 'PMC立项决策', 0, 1, '2026-04-23 17:19:50.262', '2026-04-23 17:19:50.262');
INSERT INTO `milestone_def` VALUES (2, 'G1', '先导化合物确认', '获得具有明确活性的先导化合物系列', '先导化合物、专利申请号', '新药化学部', '项目组内部评审', 1, 1, '2026-04-23 17:19:50.262', '2026-04-23 17:19:50.262');
INSERT INTO `milestone_def` VALUES (3, 'G2', '优选化合物', '获得具有明确体内药效的优选化合物', '优选化合物、专利申请号', '新药化学部', '项目组内部评审', 2, 1, '2026-04-23 17:19:50.262', '2026-04-23 17:19:50.262');
INSERT INTO `milestone_def` VALUES (4, 'G3', '候选化合物提名 (PCC)', '综合评估后，正式提名一个或多个化合物作为临床前开发候选物', 'PCC提名报告（含体内外药效、初步ADME、初步安全性、专利策略）', '新药化学部', 'PMC评审', 3, 1, '2026-04-23 17:19:50.262', '2026-04-23 17:19:50.262');
INSERT INTO `milestone_def` VALUES (5, 'G4', '临床前开发完成 (GLP)', '完成所有GLP毒理研究、药效及药代动力学研究，具备申请IND条件', 'GLP毒理报告、药效总结报告、CMC初步总结报告、专利FTO报告', '新药生物部/新药化学部', 'PMC评审', 4, 1, '2026-04-23 17:19:50.262', '2026-04-23 17:19:50.262');
INSERT INTO `milestone_def` VALUES (6, 'G5', '临床试验申请获批 (IND)', '向监管机构递交IND申请并获批', 'IND申报资料、受理通知书、临床试验批件/默示许可文件', '药政合规部', 'PMC评审', 5, 1, '2026-04-23 17:19:50.262', '2026-04-23 17:19:50.262');
INSERT INTO `milestone_def` VALUES (7, 'G6', '临床Ⅰ期', '完成健康受试者或患者的药代动力学、安全性和耐受性研究', 'Ⅰ期总结报告、Ⅰ期临床试验方案', '新药临床部', 'PMC评审', 6, 1, '2026-04-23 17:19:50.262', '2026-04-23 17:19:50.262');
INSERT INTO `milestone_def` VALUES (8, 'G7', '临床Ⅱ期', '完成在目标患者群体中的初步疗效和安全性验证', 'Ⅱ期总结报告、Ⅱ期临床试验方案、关键注册策略确认', '新药临床部', 'PMC评审', 7, 1, '2026-04-23 17:19:50.262', '2026-04-23 17:19:50.262');
INSERT INTO `milestone_def` VALUES (9, 'G8', '临床Ⅲ期', '完成关键性注册临床试验', 'Ⅲ期临床研究报告', '新药临床部', 'PMC评审', 8, 1, '2026-04-23 17:19:50.262', '2026-04-23 17:19:50.262');
INSERT INTO `milestone_def` VALUES (10, 'G9', '新药上市申请获批 (NDA)', '递交NDA并获批上市', 'NDA申报资料、受理通知书、药品注册证书', '药政合规部', 'PMC结项评审', 9, 1, '2026-04-23 17:19:50.262', '2026-04-23 17:19:50.262');

-- ----------------------------
-- Table structure for milestone_deliverable_def
-- ----------------------------
DROP TABLE IF EXISTS `milestone_deliverable_def`;
CREATE TABLE `milestone_deliverable_def`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `milestone_code` varchar(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '閲岀▼纰戦樁娈典唬鐮?G0-G9',
  `slot_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '浜や粯鐗╂Ы浣嶇紪鐮侊紝濡?INITIATION_REPORT',
  `slot_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '浜や粯鐗╁悕绉帮紝濡?绔嬮」鎶ュ憡',
  `is_required` tinyint(1) NOT NULL DEFAULT 1 COMMENT '鏄?惁蹇呭～',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '鎺掑簭鍙',
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '浜や粯鐗╄?鏄',
  `allowed_file_types` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '.pdf,.doc,.docx' COMMENT '鍏佽?鐨勬枃浠剁被鍨',
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_milestone_slot`(`milestone_code` ASC, `slot_code` ASC) USING BTREE,
  INDEX `idx_milestone`(`milestone_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 48 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '閲岀▼纰戜氦浠樼墿瀹氫箟琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of milestone_deliverable_def
-- ----------------------------
INSERT INTO `milestone_deliverable_def` VALUES (1, 'G0', 'INITIATION_REPORT', '立项报告', 1, 1, 'G0阶段固定交付物：立项报告', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (2, 'G0', 'TARGET_EVALUATION', '靶点评估文档', 1, 2, '靶点可行性评估报告', '.pdf,.doc,.docx', 0, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (3, 'G1', 'LEAD_COMPOUND', '先导化合物', 1, 1, '先导化合物结构及数据', '.pdf,.doc,.docx', 0, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (4, 'G1', 'PATENT_ANALYSIS_G1', '专利分析', 1, 2, '专利自由实施分析报告', '.pdf,.doc,.docx', 0, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (5, 'G2', 'OPTIMIZED_COMPOUND', '优选化合物', 1, 1, '优选化合物结构及数据', '.pdf,.doc,.docx', 0, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (6, 'G2', 'PATENT_ANALYSIS_G2', '专利分析', 1, 2, '专利自由实施分析报告', '.pdf,.doc,.docx', 0, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (7, 'G3', 'PCC_NOMINATION', 'PCC 提名报告', 1, 1, '候选化合物提名报告', '.pdf,.doc,.docx', 0, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (8, 'G3', 'IN_VITRO_IN_VIVO_EFFICACY', '体内外药效数据', 1, 2, '体内外药效学研究数据', '.pdf,.doc,.docx', 0, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (9, 'G3', 'PRELIMINARY_ADME', '初步ADME数据', 1, 3, '初步ADME性质研究数据', '.pdf,.doc,.docx', 0, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (10, 'G3', 'PRELIMINARY_SAFETY', '初步安全性评估', 1, 4, '初步安全性评估数据', '.pdf,.doc,.docx', 0, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (11, 'G3', 'PATENT_STRATEGY', '专利策略文档', 1, 5, '专利布局策略文档', '.pdf,.doc,.docx', 0, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (12, 'G4', 'GLP_TOXICOLOGY', 'GLP毒理报告', 1, 1, 'GLP毒理学研究报告', '.pdf,.doc,.docx', 0, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (13, 'G4', 'EFFICACY_SUMMARY', '药效总结报告', 1, 2, '药效学研究总结报告', '.pdf,.doc,.docx', 0, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (14, 'G4', 'CMC_PRELIMINARY', 'CMC初步总结报告', 1, 3, '化学、生产和控制初步总结', '.pdf,.doc,.docx', 0, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (15, 'G4', 'PATENT_FTO', '专利FTO报告', 1, 4, '专利自由实施分析报告', '.pdf,.doc,.docx', 0, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (16, 'G5', 'IND_DOSSIER', 'IND申报资料', 1, 1, 'G5阶段固定交付物：IND申报资料', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (17, 'G5', 'ACCEPTANCE_NOTICE', '受理通知书', 1, 2, 'G5阶段固定交付物：受理通知书', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (18, 'G5', 'CLINICAL_APPROVAL', '临床试验批件', 1, 3, '临床试验批准通知书', '.pdf,.doc,.docx', 0, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (19, 'G6', 'PHASE1_SUMMARY', '临床I期总结报告', 1, 1, '临床I期试验总结报告', '.pdf,.doc,.docx', 0, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (20, 'G6', 'PHASE2_PROTOCOL', '临床II期试验方案', 1, 2, '临床II期试验方案', '.pdf,.doc,.docx', 0, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (21, 'G7', 'PHASE2_SUMMARY', '临床II期总结报告', 1, 1, '临床II期试验总结报告', '.pdf,.doc,.docx', 0, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (22, 'G7', 'PHASE3_PROTOCOL', '临床III期试验方案', 1, 2, '临床III期试验方案', '.pdf,.doc,.docx', 0, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (23, 'G7', 'REGISTRATION_STRATEGY', '注册策略确认文档', 1, 3, '注册策略确认文档', '.pdf,.doc,.docx', 0, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (24, 'G8', 'PHASE3_REPORT', '临床III期研究报告', 1, 1, '临床III期研究总结报告', '.pdf,.doc,.docx', 0, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (25, 'G8', 'POST_MARKETING_COMMITMENT', '上市后承诺文档', 1, 2, '上市后承诺相关文档', '.pdf,.doc,.docx', 0, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (26, 'G9', 'NDA_DOSSIER', 'NDA申报资料', 1, 1, 'G9阶段固定交付物：NDA申报资料', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (27, 'G9', 'NDA_ACCEPTANCE', '受理通知书', 1, 2, 'NMPA受理通知书', '.pdf,.doc,.docx', 0, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (28, 'G9', 'MARKETING_AUTHORIZATION', '药品注册证书', 1, 3, '药品注册证书/上市许可', '.pdf,.doc,.docx', 0, '2026-07-26 23:46:08', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (29, 'G0', 'TARGET_EVALUATION_REPORT', '靶点评估报告', 1, 2, 'G0阶段固定交付物：靶点评估报告', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, '2026-07-30 23:56:41', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (30, 'G1', 'LEAD_COMPOUND_REPORT', '先导化合物报告', 1, 1, 'G1阶段固定交付物：先导化合物报告', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, '2026-07-30 23:56:41', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (31, 'G1', 'PATENT_APPLICATION_REPORT_G1', '专利申请报告(G1)', 1, 2, 'G1阶段固定交付物：专利申请报告(G1)', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, '2026-07-30 23:56:41', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (32, 'G2', 'OPTIMIZED_COMPOUND_REPORT', '优选化合物报告', 1, 1, 'G2阶段固定交付物：优选化合物报告', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, '2026-07-30 23:56:41', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (33, 'G2', 'PATENT_APPLICATION_REPORT_G2', '专利申请报告(G2)', 1, 2, 'G2阶段固定交付物：专利申请报告(G2)', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, '2026-07-30 23:56:41', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (34, 'G3', 'PCC_NOMINATION_REPORT', 'PCC提名报告', 1, 1, 'G3阶段固定交付物：PCC提名报告', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, '2026-07-30 23:56:41', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (35, 'G4', 'CMC_PRELIMINARY_SUMMARY_REPORT', 'CMC初步总结报告', 1, 1, 'G4阶段固定交付物：CMC初步总结报告', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, '2026-07-30 23:56:41', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (36, 'G4', 'PATENT_FTO_REPORT', '专利FTO报告', 1, 2, 'G4阶段固定交付物：专利FTO报告', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, '2026-07-30 23:56:41', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (37, 'G4', 'GLP_TOXICOLOGY_REPORT', 'GLP毒理报告', 1, 3, 'G4阶段固定交付物：GLP毒理报告', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, '2026-07-30 23:56:41', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (38, 'G4', 'EFFICACY_SUMMARY_REPORT', '药效总结报告', 1, 4, 'G4阶段固定交付物：药效总结报告', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, '2026-07-30 23:56:41', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (39, 'G5', 'CLINICAL_TRIAL_APPROVAL_OR_IMPLIED_LICENSE', '临床试验批件/默示许可文件', 1, 3, 'G5阶段固定交付物：临床试验批件/默示许可文件', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, '2026-07-30 23:56:41', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (40, 'G6', 'CLINICAL_SUMMARY_REPORT_G6', '临床期总结报告G6', 1, 1, 'G6阶段固定交付物：临床期总结报告G6', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, '2026-07-30 23:56:41', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (41, 'G6', 'CLINICAL_TRIAL_PROTOCOL_G6', '临床期试验方案G6', 1, 2, 'G6阶段固定交付物：临床期试验方案G6', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, '2026-07-30 23:56:41', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (42, 'G7', 'CLINICAL_SUMMARY_REPORT_G7', '临床期总结报告G7', 1, 1, 'G7阶段固定交付物：临床期总结报告G7', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, '2026-07-30 23:56:41', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (43, 'G7', 'CLINICAL_TRIAL_PROTOCOL_G7', '临床期试验方案G7', 1, 2, 'G7阶段固定交付物：临床期试验方案G7', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, '2026-07-30 23:56:41', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (44, 'G7', 'KEY_REGISTRATION_STRATEGY_CONFIRMATION', '关键注册策略确认', 1, 3, 'G7阶段固定交付物：关键注册策略确认', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, '2026-07-30 23:56:41', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (45, 'G8', 'CLINICAL_RESEARCH_REPORT', '临床期临床研究报告', 1, 1, 'G8阶段固定交付物：临床期临床研究报告', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, '2026-07-30 23:56:41', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (46, 'G9', 'NDA_ACCEPTANCE_NOTICE', '受理通知书', 1, 2, 'G9阶段固定交付物：受理通知书', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, '2026-07-30 23:56:41', '2026-07-30 23:56:41');
INSERT INTO `milestone_deliverable_def` VALUES (47, 'G9', 'DRUG_REGISTRATION_CERTIFICATE', '药品注册证书', 1, 3, 'G9阶段固定交付物：药品注册证书', '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx', 1, '2026-07-30 23:56:41', '2026-07-30 23:56:41');

-- ----------------------------
-- Table structure for milestone_dept_role
-- ----------------------------
DROP TABLE IF EXISTS `milestone_dept_role`;
CREATE TABLE `milestone_dept_role`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `milestone_def_id` bigint UNSIGNED NOT NULL COMMENT '里程碑定义ID',
  `dept_id` bigint UNSIGNED NOT NULL COMMENT '部门ID',
  `role_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色类型: DEPT_EXECUTOR(部门执行人) / DEPT_HEAD(部门负责人)',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_milestone_dept_role`(`milestone_def_id` ASC, `dept_id` ASC, `role_type` ASC) USING BTREE,
  INDEX `idx_milestone_def`(`milestone_def_id` ASC) USING BTREE,
  INDEX `idx_dept`(`dept_id` ASC) USING BTREE,
  CONSTRAINT `fk_milestone_dept_role_dept` FOREIGN KEY (`dept_id`) REFERENCES `org_department` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_milestone_dept_role_milestone_def` FOREIGN KEY (`milestone_def_id`) REFERENCES `milestone_def` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 57 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '里程碑阶段-部门角色映射表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of milestone_dept_role
-- ----------------------------
INSERT INTO `milestone_dept_role` VALUES (1, 1, 4, 'DEPT_EXECUTOR', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (2, 1, 4, 'DEPT_HEAD', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (4, 1, 7, 'DEPT_HEAD', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (5, 1, 10, 'ROLE_PMC', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (6, 2, 1, 'DEPT_EXECUTOR', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (7, 2, 4, 'DEPT_EXECUTOR', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (8, 2, 1, 'DEPT_HEAD', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (9, 2, 4, 'DEPT_HEAD', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (11, 2, 7, 'DEPT_HEAD', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (13, 3, 1, 'DEPT_EXECUTOR', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (14, 3, 4, 'DEPT_EXECUTOR', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (15, 3, 1, 'DEPT_HEAD', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (16, 3, 4, 'DEPT_HEAD', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (18, 3, 7, 'DEPT_HEAD', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (20, 4, 1, 'DEPT_EXECUTOR', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (21, 4, 1, 'DEPT_HEAD', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (23, 4, 7, 'DEPT_HEAD', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (24, 4, 10, 'ROLE_PMC', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (25, 5, 1, 'DEPT_EXECUTOR', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (26, 5, 4, 'DEPT_EXECUTOR', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (27, 5, 2, 'DEPT_EXECUTOR', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (28, 5, 1, 'DEPT_HEAD', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (29, 5, 4, 'DEPT_HEAD', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (30, 5, 2, 'DEPT_HEAD', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (32, 5, 7, 'DEPT_HEAD', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (33, 5, 10, 'ROLE_PMC', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (34, 6, 7, 'DEPT_EXECUTOR', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (35, 6, 7, 'DEPT_HEAD', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (37, 6, 10, 'ROLE_PMC', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (38, 7, 3, 'DEPT_EXECUTOR', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (39, 7, 3, 'DEPT_HEAD', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (41, 7, 7, 'DEPT_HEAD', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (42, 7, 10, 'ROLE_PMC', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (43, 8, 3, 'DEPT_EXECUTOR', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (44, 8, 3, 'DEPT_HEAD', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (46, 8, 7, 'DEPT_HEAD', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (47, 8, 10, 'ROLE_PMC', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (48, 9, 3, 'DEPT_EXECUTOR', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (49, 9, 3, 'DEPT_HEAD', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (51, 9, 7, 'DEPT_HEAD', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (52, 9, 10, 'ROLE_PMC', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (53, 10, 7, 'DEPT_EXECUTOR', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (54, 10, 7, 'DEPT_HEAD', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');
INSERT INTO `milestone_dept_role` VALUES (56, 10, 10, 'ROLE_PMC', 1, '2026-06-29 14:57:19', '2026-06-29 14:57:19');

-- ----------------------------
-- Table structure for milestone_history
-- ----------------------------
DROP TABLE IF EXISTS `milestone_history`;
CREATE TABLE `milestone_history`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id` bigint UNSIGNED NOT NULL,
  `project_milestone_id` bigint UNSIGNED NOT NULL,
  `action` enum('SUBMIT_REVIEW','DECISION') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `from_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `to_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `actor_user_id` bigint UNSIGNED NULL DEFAULT NULL,
  `action_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `notes` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `payload_json` json NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_mh_project`(`project_id` ASC, `action_at` ASC) USING BTREE,
  INDEX `idx_mh_pm`(`project_milestone_id` ASC, `action_at` ASC) USING BTREE,
  INDEX `fk_mh_actor`(`actor_user_id` ASC) USING BTREE,
  CONSTRAINT `fk_mh_actor` FOREIGN KEY (`actor_user_id`) REFERENCES `iam_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_mh_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_mh_project_milestone` FOREIGN KEY (`project_milestone_id`) REFERENCES `project_milestone` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of milestone_history
-- ----------------------------
INSERT INTO `milestone_history` VALUES (10, 40, 406, 'SUBMIT_REVIEW', 'IN_PROGRESS', 'SUBMITTED', 8, '2026-07-29 08:55:48.893', '111', NULL, '2026-07-29 16:55:48.893');
INSERT INTO `milestone_history` VALUES (11, 40, 406, 'DECISION', 'SUBMITTED', 'APPROVED', 1, '2026-07-29 09:03:07.195', '评审通过 (Go): ', NULL, '2026-07-29 17:03:07.195');

-- ----------------------------
-- Table structure for notification
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `recipient_user_id` bigint NOT NULL COMMENT '鎺ユ敹浜篒D锛堝?搴?iam_user.id锛',
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '閫氱煡绫诲瀷: PROJECT_COMPLETION / DELIVERABLE_UPLOADED / REVIEW_SUBMITTED / REVIEW_DECIDED / MILESTONE_APPROVED / REVIEW_REJECTED',
  `title` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '閫氱煡鏍囬?',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '閫氱煡鍐呭?',
  `project_id` bigint NULL DEFAULT NULL COMMENT '鍏宠仈椤圭洰ID',
  `milestone_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联里程碑代码/流程节点编码',
  `related_user_id` bigint NULL DEFAULT NULL COMMENT '鍏宠仈鎿嶄綔浜篒D',
  `is_read` tinyint(1) NOT NULL DEFAULT 0 COMMENT '鏄?惁宸茶?',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_todo` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否作为待办持久展示',
  `is_done` tinyint(1) NOT NULL DEFAULT 0 COMMENT '待办是否已完成',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_recipient_read`(`recipient_user_id` ASC, `is_read` ASC) USING BTREE,
  INDEX `idx_recipient_created`(`recipient_user_id` ASC, `created_at` DESC) USING BTREE,
  INDEX `idx_recipient_todo`(`recipient_user_id` ASC, `is_todo` ASC, `is_done` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 70 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '閫氱煡琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notification
-- ----------------------------
INSERT INTO `notification` VALUES (46, 2, 'PROJECT_COMPLETION', '请完善项目信息', '项目管理员已创建项目 [测试]，请尽快完善项目信息', 40, 'G0', 13, 1, '2026-07-29 16:54:55', 1, 1);
INSERT INTO `notification` VALUES (47, 8, 'DELIVERABLE', '项目已完善', '[测试] 项目信息已完善，请上传 项目立项 阶段交付物', 40, 'G0', NULL, 0, '2026-07-29 16:55:15', 1, 1);
INSERT INTO `notification` VALUES (48, 9, 'REVIEW_APPROVAL', '评审待办：项目立项', '[测试][项目立项] 当前待处理节点：资讯部负责人审批', 40, 'G0', 8, 0, '2026-07-29 16:55:49', 1, 1);
INSERT INTO `notification` VALUES (49, 2, 'REVIEW_APPROVAL', '评审待办：项目立项', '[测试][项目立项] 当前待处理节点：PM技术初评', 40, 'G0', 8, 1, '2026-07-29 16:56:26', 1, 1);
INSERT INTO `notification` VALUES (50, 12, 'REVIEW_APPROVAL', '评审待办：项目立项', '[测试][项目立项] 当前待处理节点：药政合规部意见', 40, 'G0', 8, 0, '2026-07-29 16:57:50', 1, 1);
INSERT INTO `notification` VALUES (51, 1, 'REVIEW_APPROVAL', '评审待办：项目立项', '[测试][项目立项] 当前待处理节点：PMC决策-1', 40, 'G0', 8, 0, '2026-07-29 17:00:20', 1, 1);
INSERT INTO `notification` VALUES (52, 1, 'REVIEW_APPROVAL', '评审待办：项目立项', '[测试][项目立项] 当前待处理节点：PMC决策-1', 40, 'G0', 8, 0, '2026-07-29 17:00:43', 1, 1);
INSERT INTO `notification` VALUES (53, 1, 'REVIEW_APPROVAL', '评审待办：项目立项', '[测试][项目立项] 当前待处理节点：PMC决策-1', 40, 'G0', 8, 0, '2026-07-29 17:01:00', 1, 1);
INSERT INTO `notification` VALUES (54, 2, 'PROJECT_COMPLETION', '请完善项目信息', '项目管理员已创建项目 [111]，请尽快完善项目信息', 41, 'G0', 13, 1, '2026-07-29 17:32:30', 1, 1);
INSERT INTO `notification` VALUES (56, 2, 'PROJECT_COMPLETION', '请完善项目信息', '项目管理员已创建项目 [预算测试]，请尽快完善项目信息', 43, 'G0', 13, 1, '2026-07-30 01:56:53', 1, 1);
INSERT INTO `notification` VALUES (57, 8, 'DELIVERABLE', '项目已完善', '[预算测试] 项目信息已完善，请上传 项目立项 阶段交付物', 43, 'G0', NULL, 0, '2026-07-30 02:20:13', 1, 1);
INSERT INTO `notification` VALUES (58, 17, 'BUDGET_APPROVAL', '预算调整申请待审批', '项目【预算测试】的预算调整申请待您审批。当前节点：效率管理部负责人审批；申请预算：12000。', 43, 'BUDGET_EFF_APPROVE', 2, 1, '2026-07-30 05:20:18', 1, 1);
INSERT INTO `notification` VALUES (59, 8, 'DELIVERABLE', '项目已完善', '[111] 项目信息已完善，请上传 项目立项 阶段交付物', 41, 'G0', NULL, 0, '2026-07-30 06:14:47', 1, 0);
INSERT INTO `notification` VALUES (60, 17, 'BUDGET_APPROVAL', '预算调整申请待审批', '项目【预算测试】的预算调整申请待您审批。当前节点：效率管理部负责人审批；申请预算：13000。', 43, 'BUDGET_EFF_APPROVE', 2, 1, '2026-07-30 06:28:58', 1, 1);
INSERT INTO `notification` VALUES (61, 1, 'BUDGET_APPROVAL', '预算调整申请待审批', '项目【预算测试】的预算调整申请待您审批。当前节点：PMC审批；申请预算：13000.00。', 43, 'BUDGET_PMC_APPROVE', 2, 0, '2026-07-30 06:59:13', 1, 1);
INSERT INTO `notification` VALUES (62, 6, 'BUDGET_APPROVAL', '预算调整申请待审批', '项目【预算测试】的预算调整申请待您审批。当前节点：PMC审批；申请预算：13000.00。', 43, 'BUDGET_PMC_APPROVE', 2, 0, '2026-07-30 06:59:13', 1, 1);
INSERT INTO `notification` VALUES (63, 18, 'BUDGET_APPROVAL', '预算调整申请待审批', '项目【预算测试】的预算调整申请待您审批。当前节点：PMC审批；申请预算：13000.00。', 43, 'BUDGET_PMC_APPROVE', 2, 0, '2026-07-30 06:59:13', 1, 1);
INSERT INTO `notification` VALUES (64, 2, 'BUDGET_RESULT', '预算调整申请已通过', '项目【预算测试】的预算调整申请已由PMC审批审批通过。', 43, NULL, NULL, 1, '2026-07-30 07:16:07', 0, 0);
INSERT INTO `notification` VALUES (65, 2, 'BUDGET_WARNING', '预算使用率达到预警阈值', '项目【预算测试】预算使用率已达到 93.08%，超过预警阈值 80.00%，请及时关注预算执行与支出安排。', 43, NULL, NULL, 1, '2026-07-30 08:32:15', 0, 0);
INSERT INTO `notification` VALUES (66, 13, 'BUDGET_WARNING', '预算使用率达到预警阈值', '项目【预算测试】预算使用率已达到 93.08%，超过预警阈值 80.00%，请及时关注预算执行与支出安排。', 43, NULL, NULL, 0, '2026-07-30 08:32:15', 0, 0);
INSERT INTO `notification` VALUES (67, 8, 'DELIVERABLE', '请上传G0阶段交付物', '项目 [预算测试] 已进入 G0-项目立项，请上传本阶段交付物并提交评审。', 43, 'G0', NULL, 0, '2026-07-30 16:12:43', 1, 0);
INSERT INTO `notification` VALUES (68, 10, 'DELIVERABLE', '请上传G1阶段交付物', '项目 [预算测试] 已进入 G1-先导化合物确认，请上传本阶段交付物并提交评审。', 43, 'G1', NULL, 0, '2026-07-30 16:13:23', 1, 0);
INSERT INTO `notification` VALUES (69, 8, 'DELIVERABLE', '请上传G1阶段交付物', '项目 [预算测试] 已进入 G1-先导化合物确认，请上传本阶段交付物并提交评审。', 43, 'G1', NULL, 0, '2026-07-30 16:13:23', 1, 0);

-- ----------------------------
-- Table structure for org_department
-- ----------------------------
DROP TABLE IF EXISTS `org_department`;
CREATE TABLE `org_department`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `dept_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `dept_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `dept_type` enum('PDT','ROSS','OTHER') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'OTHER',
  `parent_id` bigint UNSIGNED NULL DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `head_user_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '部门负责人用户ID（关联user表）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_org_department_code`(`dept_code` ASC) USING BTREE,
  UNIQUE INDEX `uk_dept_code`(`dept_code` ASC) USING BTREE,
  INDEX `idx_org_department_parent`(`parent_id` ASC) USING BTREE,
  CONSTRAINT `fk_org_department_parent` FOREIGN KEY (`parent_id`) REFERENCES `org_department` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of org_department
-- ----------------------------
INSERT INTO `org_department` VALUES (1, 'PDT_CHEM', '新药化学部', 'PDT', NULL, 1, '2026-04-23 17:19:50.305', '2026-07-28 16:05:59.220', 11);
INSERT INTO `org_department` VALUES (2, 'PDT_BIO', '新药生物部', 'PDT', NULL, 1, '2026-04-23 17:19:50.305', '2026-07-28 16:06:12.610', NULL);
INSERT INTO `org_department` VALUES (3, 'PDT_CLIN', '新药临床部', 'PDT', NULL, 1, '2026-04-23 17:19:50.305', '2026-07-28 16:06:23.672', NULL);
INSERT INTO `org_department` VALUES (4, 'ROSS_INFO', '新药资讯部', 'ROSS', NULL, 1, '2026-04-23 17:19:50.305', '2026-07-24 07:19:13.785', 9);
INSERT INTO `org_department` VALUES (5, 'ROSS_BD', '商务拓展部', 'ROSS', NULL, 1, '2026-04-23 17:19:50.305', '2026-06-23 08:50:10.288', 3);
INSERT INTO `org_department` VALUES (6, 'ROSS_EFF', '效率管理部', 'ROSS', NULL, 1, '2026-04-23 17:19:50.305', '2026-07-28 16:08:41.773', 17);
INSERT INTO `org_department` VALUES (7, 'ROSS_REG', '药政合规部', 'ROSS', NULL, 1, '2026-04-23 17:19:50.305', '2026-07-26 15:50:06.273', 12);
INSERT INTO `org_department` VALUES (9, 'SYSTEM', 'System', 'OTHER', NULL, 1, '2026-05-21 09:12:15.000', '2026-06-23 08:33:02.839', 6);
INSERT INTO `org_department` VALUES (10, 'PMC', '项目管理委员会', 'OTHER', NULL, 1, '2026-05-21 09:12:15.000', '2026-06-23 05:29:55.507', 1);

-- ----------------------------
-- Table structure for permission
-- ----------------------------
DROP TABLE IF EXISTS `permission`;
CREATE TABLE `permission`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权限名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '权限描述',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `name`(`name` ASC) USING BTREE,
  UNIQUE INDEX `uk_permission_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 38 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of permission
-- ----------------------------
INSERT INTO `permission` VALUES (1, 'PERMISSION_SUBMIT_REVIEW', '提交里程碑评审', '2026-04-27 13:58:56.267', '2026-04-27 13:58:56.267');
INSERT INTO `permission` VALUES (2, 'PERMISSION_APPROVE_MILESTONE', '批准里程碑（PMC Go/No Go决策）', '2026-04-27 13:58:56.267', '2026-04-27 13:58:56.267');
INSERT INTO `permission` VALUES (3, 'PERMISSION_VIEW_MILESTONE', '查看里程碑信息', '2026-04-27 13:58:56.267', '2026-04-27 13:58:56.267');
INSERT INTO `permission` VALUES (4, 'PERMISSION_VIEW_BUDGET', '查看预算信息', '2026-04-27 13:58:56.267', '2026-04-27 13:58:56.267');
INSERT INTO `permission` VALUES (5, 'PERMISSION_APPROVE_BUDGET', '批准预算调整', '2026-04-27 13:58:56.267', '2026-04-27 13:58:56.267');
INSERT INTO `permission` VALUES (6, 'PERMISSION_MANAGE_BUDGET', '管理预算计划', '2026-04-27 13:58:56.267', '2026-04-27 13:58:56.267');
INSERT INTO `permission` VALUES (7, 'PERMISSION_CREATE_PROJECT', '创建项目', '2026-04-27 13:58:56.267', '2026-04-27 13:58:56.267');
INSERT INTO `permission` VALUES (8, 'PERMISSION_VIEW_PROJECT', '查看项目信息', '2026-04-27 13:58:56.267', '2026-04-27 13:58:56.267');
INSERT INTO `permission` VALUES (9, 'PERMISSION_EDIT_PROJECT', '编辑项目', '2026-04-27 13:58:56.267', '2026-04-27 13:58:56.267');
INSERT INTO `permission` VALUES (10, 'PERMISSION_TERMINATE_PROJECT', '终止项目', '2026-04-27 13:58:56.267', '2026-04-27 13:58:56.267');
INSERT INTO `permission` VALUES (11, 'PERMISSION_UPLOAD_DOCUMENT', '上传交付物', '2026-04-27 13:58:56.267', '2026-04-27 13:58:56.267');
INSERT INTO `permission` VALUES (12, 'PERMISSION_VIEW_DOCUMENT', '查看交付物', '2026-04-27 13:58:56.267', '2026-04-27 13:58:56.267');
INSERT INTO `permission` VALUES (13, 'PERMISSION_REVIEW_DOCUMENT', '审查交付物', '2026-04-27 13:58:56.267', '2026-04-27 13:58:56.267');
INSERT INTO `permission` VALUES (14, 'PERMISSION_SUBMIT_CHANGE_REQUEST', '提交变更申请', '2026-04-27 13:58:56.267', '2026-04-27 13:58:56.267');
INSERT INTO `permission` VALUES (15, 'PERMISSION_APPROVE_CHANGE_REQUEST', '批准变更申请', '2026-04-27 13:58:56.267', '2026-04-27 13:58:56.267');
INSERT INTO `permission` VALUES (16, 'PERMISSION_VIEW_CHANGE_REQUEST', '查看变更申请', '2026-04-27 13:58:56.267', '2026-04-27 13:58:56.267');
INSERT INTO `permission` VALUES (17, 'PERMISSION_MANAGE_USERS', '管理用户', '2026-04-27 13:58:56.267', '2026-04-27 13:58:56.267');
INSERT INTO `permission` VALUES (18, 'PERMISSION_MANAGE_ROLES', '管理角色', '2026-04-27 13:58:56.267', '2026-04-27 13:58:56.267');
INSERT INTO `permission` VALUES (19, 'PERMISSION_VIEW_REPORTS', '查看报表', '2026-04-27 13:58:56.267', '2026-04-27 13:58:56.267');
INSERT INTO `permission` VALUES (20, 'PERMISSION_SYSTEM_MAINTENANCE', '系统维护', '2026-04-27 13:58:56.267', '2026-04-27 13:58:56.267');
INSERT INTO `permission` VALUES (21, 'DOCUMENT_UPLOAD', '上传文档', '2026-04-27 16:01:48.564', '2026-04-27 16:01:48.564');
INSERT INTO `permission` VALUES (22, 'DOCUMENT_DOWNLOAD', '下载文档', '2026-04-27 16:01:48.564', '2026-04-27 16:01:48.564');
INSERT INTO `permission` VALUES (23, 'DOCUMENT_DELETE', '删除文档', '2026-04-27 16:01:48.564', '2026-04-27 16:01:48.564');
INSERT INTO `permission` VALUES (24, 'DOCUMENT_REVIEW', '审核文档合规性', '2026-04-27 16:01:48.564', '2026-04-27 16:01:48.564');
INSERT INTO `permission` VALUES (25, 'DOCUMENT_VIEW_AUDIT', '查看文档审计日志', '2026-04-27 16:01:48.564', '2026-04-27 16:01:48.564');
INSERT INTO `permission` VALUES (26, 'PERMISSION_REVIEW_INITIATION', '评审立项申请', '2026-05-11 09:20:18.554', '2026-05-11 09:20:18.554');
INSERT INTO `permission` VALUES (27, 'PERMISSION_APPROVE_INITIATION', '审批立项申请', '2026-05-11 09:20:18.554', '2026-05-11 09:20:18.554');
INSERT INTO `permission` VALUES (28, 'PERMISSION_VIEW_REVIEW_RECORD', '查看评审记录', '2026-05-11 09:20:18.554', '2026-05-11 09:20:18.554');
INSERT INTO `permission` VALUES (29, 'PERMISSION_DELETE_PROJECT', '删除项目', '2026-05-13 11:06:03.000', '2026-05-13 11:06:03.000');
INSERT INTO `permission` VALUES (31, 'PERMISSION_VIEW_ALL_PROJECTS', '查看所有项目', '2026-07-03 17:58:50.000', '2026-07-03 17:58:50.000');
INSERT INTO `permission` VALUES (33, 'PERMISSION_BUDGET_VIEW', '预算管理查看权限', '2026-07-30 07:49:38.167', '2026-07-30 07:49:38.167');
INSERT INTO `permission` VALUES (34, 'PERMISSION_BUDGET_MANAGE', '预算管理操作与预算调整发起权限', '2026-07-30 07:49:38.178', '2026-07-30 07:49:38.178');
INSERT INTO `permission` VALUES (35, 'PERMISSION_DELIVERABLE_VIEW', '鏌ョ湅鏈?汉涓婁紶鎴栨湰浜鸿瘎瀹¤繃鐨勪氦浠樼墿', '2026-07-30 17:39:26.000', '2026-07-30 17:39:26.000');
INSERT INTO `permission` VALUES (36, 'PERMISSION_DELIVERABLE_VIEW_ALL', '鏌ョ湅椤圭洰鍏ㄩ儴閲岀▼纰戜氦浠樼墿', '2026-07-30 17:39:26.000', '2026-07-30 17:39:26.000');
INSERT INTO `permission` VALUES (37, 'PERMISSION_DELIVERABLE_IMPORT', '灏嗗巻鍙叉枃浠跺?鍏ユ寚瀹?G0-G9 浜や粯鐗╂Ы浣', '2026-07-30 17:39:26.000', '2026-07-30 17:39:26.000');

-- ----------------------------
-- Table structure for project
-- ----------------------------
DROP TABLE IF EXISTS `project`;
CREATE TABLE `project`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_no` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `level_id` bigint UNSIGNED NOT NULL,
  `project_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `project_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `target_pathway` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `indication` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `tpp_summary` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `description` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `mechanism` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `unmet_needs` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '未满足的临床需求',
  `scientific_basis` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `expected_indication` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预期适应症',
  `administration_route` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '给药途径',
  `dosage_form` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '剂型',
  `dosage_frequency` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '剂量频率',
  `efficacy_target` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `safety_advantage` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `differentiation` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `budget_total` decimal(18, 2) NULL DEFAULT NULL COMMENT '总预算',
  `budget_to_pcc` decimal(18, 2) NULL DEFAULT NULL COMMENT '阶段预算至PCC（万元）',
  `risk_scientific` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '科学风险：靶点有效性风险、成药性风险、安全性风险',
  `risk_competitive` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '竞争风险：主要竞品进展',
  `risk_regulatory` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '注册风险：法规路径不确定性',
  `suggestion_and_support` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '建议与所需支持：简述需要PMC提供的资源或决策支持',
  `pm_user_id` bigint UNSIGNED NULL DEFAULT NULL,
  `initiator_user_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '发起人（立项申请人）',
  `pmc_committee_id` bigint UNSIGNED NULL DEFAULT NULL,
  `process_oversight_dept_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '流程监管部门（默认：效率管理部 ROSS_EFF）',
  `current_milestone_id` bigint UNSIGNED NULL DEFAULT NULL,
  `status` enum('DRAFT','ACTIVE','PAUSED','TERMINATED','CLOSED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DRAFT',
  `review_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '评审状态：PENDING_REVIEW/IN_REVIEW/APPROVED/REJECTED',
  `review_submitted_at` datetime(3) NULL DEFAULT NULL COMMENT '评审提交时间',
  `initiation_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '绔嬮」鐘舵?锛歯ull(鏈?敵璇?/SUBMITTED(宸叉彁浜?/APPROVED(宸查?杩?/REJECTED(宸查┏鍥?',
  `initiation_submitted_at` datetime(3) NULL DEFAULT NULL COMMENT '绔嬮」鐢宠?鎻愪氦鏃堕棿',
  `initiation_application` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '立项申请信息（项目经理填写的申请内容）',
  `terminated_reason` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `start_date` date NULL DEFAULT NULL,
  `end_date` date NULL DEFAULT NULL,
  `planned_pcc_date` date NULL DEFAULT NULL COMMENT '预估PCC提名日期（对应G0计划日期）',
  `planned_ind_date` date NULL DEFAULT NULL COMMENT '预估IND获批日期（对应G5计划日期）',
  `planned_nda_date` date NULL DEFAULT NULL COMMENT '预估NDA获批日期（对应G9计划日期）',
  `created_by` bigint UNSIGNED NULL DEFAULT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint UNSIGNED NULL DEFAULT NULL,
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `planned_end_date` date NULL DEFAULT NULL COMMENT '预估项目结束日期',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_project_no`(`project_no` ASC) USING BTREE,
  UNIQUE INDEX `uk_project_code`(`project_code` ASC) USING BTREE,
  INDEX `idx_project_level`(`level_id` ASC) USING BTREE,
  INDEX `idx_project_pm`(`pm_user_id` ASC) USING BTREE,
  INDEX `idx_project_status`(`status` ASC) USING BTREE,
  INDEX `idx_project_current_milestone`(`current_milestone_id` ASC) USING BTREE,
  INDEX `fk_project_pmc`(`pmc_committee_id` ASC) USING BTREE,
  INDEX `fk_project_created_by`(`created_by` ASC) USING BTREE,
  INDEX `fk_project_updated_by`(`updated_by` ASC) USING BTREE,
  INDEX `idx_project_process_oversight_dept`(`process_oversight_dept_id` ASC) USING BTREE,
  CONSTRAINT `fk_project_created_by` FOREIGN KEY (`created_by`) REFERENCES `iam_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_project_current_milestone` FOREIGN KEY (`current_milestone_id`) REFERENCES `milestone_def` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_project_level` FOREIGN KEY (`level_id`) REFERENCES `project_level` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_project_pm` FOREIGN KEY (`pm_user_id`) REFERENCES `iam_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_project_pmc` FOREIGN KEY (`pmc_committee_id`) REFERENCES `governance_committee` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_project_process_oversight_dept` FOREIGN KEY (`process_oversight_dept_id`) REFERENCES `org_department` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_project_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `iam_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_project_code` CHECK (regexp_like(`project_code`,_utf8mb4'^(H-L|G-L|H-Q|G-Q|G-T|C-L|C-Q)-KBD[0-9]{4,}$')),
  CONSTRAINT `ck_project_dates` CHECK ((`end_date` is null) or (`start_date` is null) or (`end_date` >= `start_date`)),
  CONSTRAINT `ck_project_no` CHECK (regexp_like(`project_no`,_utf8mb4'^KBD[0-9]{4,}$'))
) ENGINE = InnoDB AUTO_INCREMENT = 44 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of project
-- ----------------------------
INSERT INTO `project` VALUES (12, 'KBD0007', 1, 'H-L-KBD0007', 'KU-101 酪氨酸激酶抑制剂', 'EGFR/HER2', '非小细胞肺癌（NSCLC）', '口服小分子TKI，目标ORR≥45%，安全性优于三代药物。', '[Demo] G0 initiation - approval in progress (SUBMITTED).', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 8500.00, NULL, NULL, NULL, NULL, NULL, 2, 2, NULL, 6, 1, 'ACTIVE', NULL, NULL, 'APPROVED', '2026-05-28 06:58:55.777', 'Request PMC initiation review meeting and approval.', NULL, '2026-01-15', NULL, '2027-06-30', '2029-12-31', '2032-06-30', 2, '2026-05-28 06:58:55.777', 2, '2026-06-24 05:48:59.849', '2033-12-31');
INSERT INTO `project` VALUES (13, 'KBD0008', 4, 'G-Q-KBD0008', 'BS-202 双特异性抗体', 'PD-1 × CTLA-4', '晚期黑色素瘤', '双抗免疫联合机制，探索性重大临床前项目。', '[Demo] G1 lead compound stage - initiation APPROVED.', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 12000.00, NULL, NULL, NULL, NULL, NULL, 2, NULL, NULL, 6, 2, 'ACTIVE', NULL, NULL, 'APPROVED', '2025-11-01 10:00:00.000', NULL, NULL, '2025-08-01', NULL, '2027-09-30', '2030-06-30', '2033-03-31', 2, '2026-05-28 06:58:55.787', 2, '2026-05-28 06:58:55.787', NULL);
INSERT INTO `project` VALUES (14, 'KBD0009', 2, 'G-L-KBD0009', 'SM-303 小分子抗肿瘤药', 'KRAS G12C', '结直肠癌', '口服KRAS抑制剂，瞄准耐药后线治疗空白。', '[Demo] G3 PCC nomination - G0-G2 completed with Go.', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 28000.00, NULL, NULL, NULL, NULL, NULL, 2, NULL, NULL, 6, 4, 'ACTIVE', NULL, NULL, 'APPROVED', NULL, NULL, NULL, '2024-03-01', NULL, '2026-12-31', '2028-06-30', '2031-12-31', 2, '2026-05-28 06:58:55.793', 2, '2026-05-28 06:58:55.793', NULL);
INSERT INTO `project` VALUES (15, 'KBD0010', 3, 'H-Q-KBD0010', 'NA-404 核酸适配体药物', 'VEGF', '湿性年龄相关性黄斑变性', '长效眼内给药核酸药物，减少注射频次。', '[Demo] G5 IND approved stage - preclinical complete.', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 45000.00, NULL, NULL, NULL, NULL, NULL, 2, NULL, NULL, 6, 6, 'ACTIVE', NULL, NULL, 'APPROVED', NULL, NULL, NULL, '2022-06-01', NULL, '2025-06-30', '2026-05-28', '2029-12-31', 2, '2026-05-28 06:58:55.800', 2, '2026-05-28 06:58:55.800', NULL);
INSERT INTO `project` VALUES (16, 'KBD0011', 5, 'G-T-KBD0011', 'GT-505 体内基因治疗', 'CFTR', '囊性纤维化', 'AAV载体基因治疗，重大探索性管线。', '[Demo] G7 Phase II clinical trial in progress.', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 62000.00, NULL, NULL, NULL, NULL, NULL, 2, NULL, NULL, 6, 8, 'ACTIVE', NULL, NULL, 'APPROVED', NULL, NULL, NULL, '2021-01-10', NULL, '2024-12-31', '2025-08-15', '2028-12-31', 2, '2026-05-28 06:58:55.805', 2, '2026-05-28 06:58:55.805', NULL);
INSERT INTO `project` VALUES (17, 'KBD0012', 6, 'C-L-KBD0012', 'CG-606 改良型抗肿瘤生物药', 'HER2', 'HER2阳性乳腺癌', '产能型临床项目，生物类似物+改良制剂。', '[Demo] G9 NDA filing stage - Phase III complete.', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 38000.00, NULL, NULL, NULL, NULL, NULL, 2, NULL, NULL, 6, 10, 'ACTIVE', NULL, NULL, 'APPROVED', NULL, NULL, NULL, '2020-05-01', NULL, '2023-03-31', '2024-09-30', '2026-08-31', 2, '2026-05-28 06:58:55.809', 2, '2026-05-28 06:58:55.809', '2027-06-30');
INSERT INTO `project` VALUES (40, 'KBD0013', 1, 'H-L-KBD0013', '测试', '', '', '嘀嘀打车', '嘀嘀打车', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0.00, 0.00, NULL, NULL, NULL, NULL, 2, NULL, NULL, 6, 2, 'ACTIVE', 'APPROVED', '2026-07-29 08:55:48.843', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 13, '2026-07-29 16:54:55.016', 13, '2026-07-29 17:03:07.195', NULL);
INSERT INTO `project` VALUES (41, 'KBD0014', 1, 'H-L-KBD0014', '111', '1', '1', '111', '111', '1', '1', '1', '1', '鼻用', '滴眼剂', '1', '1', '1', '1', 10000.00, 10000.00, '1', '1', '1', '1', 2, NULL, NULL, 6, 1, 'ACTIVE', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-07-30', '2026-07-30', '2026-07-30', 13, '2026-07-29 17:32:29.752', 13, '2026-07-30 06:14:46.555', '2026-07-30');
INSERT INTO `project` VALUES (43, 'KBD0015', 1, 'H-L-KBD0015', '预算测试', '', '', '111', '111', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 13000.00, 10000.00, NULL, NULL, NULL, NULL, 2, NULL, NULL, 6, 2, 'ACTIVE', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-07-30', '2026-07-30', '2026-07-30', 13, '2026-07-30 01:56:53.275', 13, '2026-07-30 16:13:22.752', '2026-07-30');

-- ----------------------------
-- Table structure for project_budget_ledger
-- ----------------------------
DROP TABLE IF EXISTS `project_budget_ledger`;
CREATE TABLE `project_budget_ledger`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id` bigint UNSIGNED NOT NULL,
  `occurred_on` date NOT NULL,
  `expense_category` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '支出分类',
  `amount` decimal(18, 2) NOT NULL,
  `vendor_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `reference_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_by` bigint UNSIGNED NULL DEFAULT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_budget_ledger_project_date`(`project_id` ASC, `occurred_on` ASC) USING BTREE,
  INDEX `idx_budget_ledger_project_category`(`project_id` ASC, `expense_category` ASC) USING BTREE,
  INDEX `fk_budget_ledger_created_by`(`created_by` ASC) USING BTREE,
  CONSTRAINT `fk_budget_ledger_created_by` FOREIGN KEY (`created_by`) REFERENCES `iam_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_budget_ledger_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_budget_ledger_amount` CHECK (`amount` >= 0)
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of project_budget_ledger
-- ----------------------------
INSERT INTO `project_budget_ledger` VALUES (1, 43, '2026-07-31', 'INTERNAL', 1000.00, '11', '11', '11', 2, '2026-07-30 07:17:48.148');
INSERT INTO `project_budget_ledger` VALUES (2, 43, '2026-07-30', 'EQUIPMENT', 100.00, '1', '1', '1', 2, '2026-07-30 08:10:17.449');
INSERT INTO `project_budget_ledger` VALUES (3, 43, '2026-07-30', 'CONSULTING', 11000.00, '1', '1', '111', 2, '2026-07-30 08:32:14.670');

-- ----------------------------
-- Table structure for project_budget_plan
-- ----------------------------
DROP TABLE IF EXISTS `project_budget_plan`;
CREATE TABLE `project_budget_plan`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id` bigint UNSIGNED NOT NULL,
  `plan_type` enum('LIFECYCLE','ANNUAL','STAGE_ROLLING') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `fiscal_year` int NULL DEFAULT NULL,
  `stage_from_milestone_id` bigint UNSIGNED NULL DEFAULT NULL,
  `stage_to_milestone_id` bigint UNSIGNED NULL DEFAULT NULL,
  `version_no` int NOT NULL DEFAULT 1,
  `internal_amount` decimal(18, 2) NOT NULL DEFAULT 0.00,
  `external_amount` decimal(18, 2) NOT NULL DEFAULT 0.00,
  `total_amount` decimal(18, 2) GENERATED ALWAYS AS ((`internal_amount` + `external_amount`)) STORED NULL,
  `approved_status` enum('DRAFT','SUBMITTED','APPROVED','REJECTED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DRAFT',
  `approved_at` datetime(3) NULL DEFAULT NULL,
  `approved_by` bigint UNSIGNED NULL DEFAULT NULL,
  `notes` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `created_by` bigint UNSIGNED NULL DEFAULT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint UNSIGNED NULL DEFAULT NULL,
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_budget_plan_project`(`project_id` ASC) USING BTREE,
  INDEX `idx_budget_plan_type_year`(`plan_type` ASC, `fiscal_year` ASC) USING BTREE,
  INDEX `fk_budget_plan_from`(`stage_from_milestone_id` ASC) USING BTREE,
  INDEX `fk_budget_plan_to`(`stage_to_milestone_id` ASC) USING BTREE,
  INDEX `fk_budget_plan_approved_by`(`approved_by` ASC) USING BTREE,
  INDEX `fk_budget_plan_created_by`(`created_by` ASC) USING BTREE,
  INDEX `fk_budget_plan_updated_by`(`updated_by` ASC) USING BTREE,
  INDEX `idx_budget_plan_project_version`(`project_id` ASC, `version_no` ASC) USING BTREE,
  CONSTRAINT `fk_budget_plan_approved_by` FOREIGN KEY (`approved_by`) REFERENCES `iam_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_budget_plan_created_by` FOREIGN KEY (`created_by`) REFERENCES `iam_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_budget_plan_from` FOREIGN KEY (`stage_from_milestone_id`) REFERENCES `milestone_def` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_budget_plan_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_budget_plan_to` FOREIGN KEY (`stage_to_milestone_id`) REFERENCES `milestone_def` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_budget_plan_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `iam_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_budget_plan_amounts` CHECK ((`internal_amount` >= 0) and (`external_amount` >= 0)),
  CONSTRAINT `ck_budget_plan_year` CHECK ((`plan_type` <> _utf8mb4'ANNUAL') or (`fiscal_year` is not null))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of project_budget_plan
-- ----------------------------

-- ----------------------------
-- Table structure for project_budget_policy
-- ----------------------------
DROP TABLE IF EXISTS `project_budget_policy`;
CREATE TABLE `project_budget_policy`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id` bigint UNSIGNED NOT NULL,
  `yellow_threshold` decimal(6, 4) NOT NULL DEFAULT 0.8000,
  `red_threshold` decimal(6, 4) NOT NULL DEFAULT 0.9500,
  `currency_code` varchar(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'CNY',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_project_budget_policy_project`(`project_id` ASC) USING BTREE,
  CONSTRAINT `fk_project_budget_policy_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_budget_thresholds` CHECK ((`yellow_threshold` > 0) and (`red_threshold` > 0) and (`red_threshold` > `yellow_threshold`) and (`red_threshold` <= 1.0000))
) ENGINE = InnoDB AUTO_INCREMENT = 43 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of project_budget_policy
-- ----------------------------
INSERT INTO `project_budget_policy` VALUES (12, 12, 0.8000, 0.9500, 'CNY', '2026-05-28 06:58:55.854', '2026-05-28 06:58:55.854');
INSERT INTO `project_budget_policy` VALUES (13, 13, 0.8000, 0.9500, 'CNY', '2026-05-28 06:58:55.854', '2026-05-28 06:58:55.854');
INSERT INTO `project_budget_policy` VALUES (14, 14, 0.8000, 0.9500, 'CNY', '2026-05-28 06:58:55.854', '2026-05-28 06:58:55.854');
INSERT INTO `project_budget_policy` VALUES (15, 15, 0.8000, 0.9500, 'CNY', '2026-05-28 06:58:55.854', '2026-05-28 06:58:55.854');
INSERT INTO `project_budget_policy` VALUES (16, 16, 0.8000, 0.9500, 'CNY', '2026-05-28 06:58:55.854', '2026-05-28 06:58:55.854');
INSERT INTO `project_budget_policy` VALUES (17, 17, 0.8000, 0.9500, 'CNY', '2026-05-28 06:58:55.854', '2026-05-28 06:58:55.854');
INSERT INTO `project_budget_policy` VALUES (39, 40, 0.8000, 0.9500, 'CNY', '2026-07-29 16:54:55.016', '2026-07-29 16:54:55.016');
INSERT INTO `project_budget_policy` VALUES (40, 41, 0.8000, 0.9500, 'CNY', '2026-07-29 17:32:29.752', '2026-07-29 17:32:29.752');
INSERT INTO `project_budget_policy` VALUES (42, 43, 0.8000, 0.9500, 'CNY', '2026-07-30 01:56:53.275', '2026-07-30 01:56:53.275');

-- ----------------------------
-- Table structure for project_budget_snapshot
-- ----------------------------
DROP TABLE IF EXISTS `project_budget_snapshot`;
CREATE TABLE `project_budget_snapshot`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id` bigint UNSIGNED NOT NULL,
  `budget_plan_id` bigint UNSIGNED NULL DEFAULT NULL,
  `snapshot_month` varchar(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `internal_spent` decimal(18, 2) NOT NULL DEFAULT 0.00,
  `external_spent` decimal(18, 2) NOT NULL DEFAULT 0.00,
  `total_spent` decimal(18, 2) GENERATED ALWAYS AS ((`internal_spent` + `external_spent`)) STORED NULL,
  `planned_total_amount` decimal(18, 2) NOT NULL DEFAULT 0.00,
  `utilization_ratio` decimal(10, 6) GENERATED ALWAYS AS ((case when (`planned_total_amount` = 0) then 0 else (`total_spent` / `planned_total_amount`) end)) STORED NULL,
  `warning_level` enum('NONE','YELLOW','RED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NONE',
  `generated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_budget_snapshot`(`project_id` ASC, `snapshot_month` ASC, `budget_plan_id` ASC) USING BTREE,
  INDEX `idx_budget_snapshot_project_month`(`project_id` ASC, `snapshot_month` ASC) USING BTREE,
  INDEX `fk_budget_snapshot_plan`(`budget_plan_id` ASC) USING BTREE,
  CONSTRAINT `fk_budget_snapshot_plan` FOREIGN KEY (`budget_plan_id`) REFERENCES `project_budget_plan` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_budget_snapshot_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_budget_snapshot_amounts` CHECK ((`internal_spent` >= 0) and (`external_spent` >= 0) and (`planned_total_amount` >= 0)),
  CONSTRAINT `ck_budget_snapshot_month` CHECK (regexp_like(`snapshot_month`,_utf8mb4'^[0-9]{4}-[0-9]{2}$'))
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of project_budget_snapshot
-- ----------------------------
INSERT INTO `project_budget_snapshot` VALUES (1, 12, NULL, '2026-05', 1700.00, 425.00, DEFAULT, 8500.00, DEFAULT, 'NONE', '2026-05-28 14:58:55.859');
INSERT INTO `project_budget_snapshot` VALUES (2, 13, NULL, '2026-05', 3000.00, 600.00, DEFAULT, 12000.00, DEFAULT, 'NONE', '2026-05-28 14:58:55.859');
INSERT INTO `project_budget_snapshot` VALUES (3, 14, NULL, '2026-05', 8400.00, 1400.00, DEFAULT, 28000.00, DEFAULT, 'NONE', '2026-05-28 14:58:55.859');
INSERT INTO `project_budget_snapshot` VALUES (4, 15, NULL, '2026-05', 4500.00, 2250.00, DEFAULT, 45000.00, DEFAULT, 'NONE', '2026-05-28 14:58:55.859');
INSERT INTO `project_budget_snapshot` VALUES (5, 16, NULL, '2026-05', 9300.00, 3100.00, DEFAULT, 62000.00, DEFAULT, 'NONE', '2026-05-28 14:58:55.859');
INSERT INTO `project_budget_snapshot` VALUES (6, 17, NULL, '2026-05', 7600.00, 1900.00, DEFAULT, 38000.00, DEFAULT, 'NONE', '2026-05-28 14:58:55.859');

-- ----------------------------
-- Table structure for project_change_request
-- ----------------------------
DROP TABLE IF EXISTS `project_change_request`;
CREATE TABLE `project_change_request`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id` bigint UNSIGNED NOT NULL,
  `change_type` enum('OBJECTIVE_SCOPE','MILESTONE_SCHEDULE','BUDGET','OWNER_PM','PAUSE_TERMINATE','OTHER') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `reason_text` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `attachment_uri` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '变更附件路径',
  `before_text` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `after_text` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `impact_milestone_text` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `impact_budget_text` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `impact_resource_text` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `requested_by` bigint UNSIGNED NULL DEFAULT NULL,
  `requested_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
  `efficiency_approver_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '效率管理部审批人',
  `efficiency_opinion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '效率管理部审批意见',
  `efficiency_decided_at` datetime(3) NULL DEFAULT NULL COMMENT '效率管理部审批时间',
  `wf_instance_id` bigint UNSIGNED NULL DEFAULT NULL,
  `pmc_decision` enum('APPROVE','REJECT','CONDITIONAL_APPROVE') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `pmc_decision_text` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `pmc_decided_at` datetime(3) NULL DEFAULT NULL,
  `pmc_decided_by` bigint UNSIGNED NULL DEFAULT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `target_milestone_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '目标里程碑ID（里程碑调整时使用）',
  `target_milestone_planned_date` date NULL DEFAULT NULL COMMENT '目标里程碑新计划日期',
  `previous_budget_amount` decimal(18, 2) NULL DEFAULT NULL COMMENT '变更前预算金额',
  `requested_budget_amount` decimal(18, 2) NULL DEFAULT NULL COMMENT '申请预算金额',
  `new_pm_user_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '新PM用户ID（负责人变更时使用）',
  `asset_disposal_confirmed` tinyint(1) NULL DEFAULT 0 COMMENT '资产处置确认（终止时使用）',
  `archive_confirmed` tinyint(1) NULL DEFAULT 0 COMMENT '归档确认（终止时使用）',
  `adjustment_amount` decimal(18, 2) NULL DEFAULT NULL COMMENT '本次预算调整增减金额',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_change_project`(`project_id` ASC, `requested_at` ASC) USING BTREE,
  INDEX `idx_change_status`(`status` ASC) USING BTREE,
  INDEX `fk_change_requested_by`(`requested_by` ASC) USING BTREE,
  INDEX `fk_change_wf_instance`(`wf_instance_id` ASC) USING BTREE,
  INDEX `fk_change_pmc_decided_by`(`pmc_decided_by` ASC) USING BTREE,
  INDEX `fk_change_target_milestone`(`target_milestone_id` ASC) USING BTREE,
  INDEX `fk_change_new_pm`(`new_pm_user_id` ASC) USING BTREE,
  INDEX `fk_change_efficiency_approver`(`efficiency_approver_id` ASC) USING BTREE,
  CONSTRAINT `fk_change_efficiency_approver` FOREIGN KEY (`efficiency_approver_id`) REFERENCES `iam_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_change_new_pm` FOREIGN KEY (`new_pm_user_id`) REFERENCES `iam_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_change_pmc_decided_by` FOREIGN KEY (`pmc_decided_by`) REFERENCES `iam_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_change_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_change_requested_by` FOREIGN KEY (`requested_by`) REFERENCES `iam_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_change_target_milestone` FOREIGN KEY (`target_milestone_id`) REFERENCES `milestone_def` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_change_wf_instance` FOREIGN KEY (`wf_instance_id`) REFERENCES `wf_instance` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of project_change_request
-- ----------------------------
INSERT INTO `project_change_request` VALUES (3, 43, 'BUDGET', '111', NULL, '项目总预算：10000.00', '项目总预算：12000', NULL, '项目总预算由 10000.00 调整为 12000', NULL, 2, '2026-07-29 21:20:18.009', 'PENDING_BUDGET_EFF_APPROVE', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-07-30 05:20:18.009', '2026-07-30 05:20:18.087', NULL, NULL, 10000.00, 12000.00, NULL, NULL, NULL, 2000.00);
INSERT INTO `project_change_request` VALUES (4, 43, 'BUDGET', '111', NULL, '项目总预算：10000.00', '项目总预算：13000', NULL, '项目总预算由 10000.00 调整为 13000', NULL, 2, '2026-07-29 22:28:57.784', 'APPROVED', 17, NULL, '2026-07-29 22:59:13.102', NULL, 'APPROVE', '111', '2026-07-29 23:16:06.741', 1, '2026-07-30 06:28:57.785', '2026-07-30 07:16:06.741', NULL, NULL, 10000.00, 13000.00, NULL, NULL, NULL, 3000.00);

-- ----------------------------
-- Table structure for project_document
-- ----------------------------
DROP TABLE IF EXISTS `project_document`;
CREATE TABLE `project_document`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id` bigint UNSIGNED NOT NULL,
  `milestone_id` bigint UNSIGNED NULL DEFAULT NULL,
  `doc_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `doc_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `storage_uri` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `uploaded_by` bigint UNSIGNED NULL DEFAULT NULL,
  `uploaded_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_doc_project_milestone`(`project_id` ASC, `milestone_id` ASC) USING BTREE,
  INDEX `idx_doc_project_type`(`project_id` ASC, `doc_type` ASC) USING BTREE,
  INDEX `fk_doc_milestone`(`milestone_id` ASC) USING BTREE,
  INDEX `fk_doc_uploaded_by`(`uploaded_by` ASC) USING BTREE,
  CONSTRAINT `fk_doc_milestone` FOREIGN KEY (`milestone_id`) REFERENCES `milestone_def` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_doc_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_doc_uploaded_by` FOREIGN KEY (`uploaded_by`) REFERENCES `iam_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of project_document
-- ----------------------------

-- ----------------------------
-- Table structure for project_level
-- ----------------------------
DROP TABLE IF EXISTS `project_level`;
CREATE TABLE `project_level`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `level_code` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `level_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `definition_text` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `governance_text` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_project_level_code`(`level_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of project_level
-- ----------------------------
INSERT INTO `project_level` VALUES (1, 'H-L', '火力全开 临床重大', '公司战略核心；FIC/BIC潜力；重大未满足临床需求；重磅潜质；国际化与对外授权价值。', '资源保障力★★★★★ 洞察决策力★★★★★ 人才驱动力★★★★★ 体系管控力★★★★★', 1, '2026-04-23 17:19:50.239', '2026-04-23 17:19:50.239');
INSERT INTO `project_level` VALUES (2, 'G-L', '临床重大', '公司战略核心；FIC/BIC潜力；重大未满足临床需求；重磅潜质。', '资源保障力★★★★ 洞察决策力★★★★★ 人才驱动力★★★★ 体系管控力★★★★★', 1, '2026-04-23 17:19:50.239', '2026-04-23 17:19:50.239');
INSERT INTO `project_level` VALUES (3, 'H-Q', '火力全开 重大临床前', '公司研发管线核心；FIC/BIC潜力；重大未满足临床需求；重磅潜质；国际化与对外授权价值。', '资源保障力★★★★★ 洞察决策力★★★★★ 人才驱动力★★★★★ 体系管控力★★★★', 1, '2026-04-23 17:19:50.239', '2026-04-23 17:19:50.239');
INSERT INTO `project_level` VALUES (4, 'G-Q', '重大临床前', '公司研发管线核心；FIC/BIC潜力；重大未满足临床需求；重磅潜质。', '资源保障力★★★★ 洞察决策力★★★★ 人才驱动力★★★★ 体系管控力★★★★', 1, '2026-04-23 17:19:50.239', '2026-04-23 17:19:50.239');
INSERT INTO `project_level` VALUES (5, 'G-T', '重大探索', '探索性新靶点/新机制或技术风险较高；作为研发管线补充；具有重大市场价值。', '资源保障力★★★ 洞察决策力★★★★ 人才驱动力★★★ 体系管控力★★★★', 1, '2026-04-23 17:19:50.239', '2026-04-23 17:19:50.239');
INSERT INTO `project_level` VALUES (6, 'C-L', '产能项目（临床）', '具有巨大市场潜能；快速布局创新开发；锚定行业管线缺口。', '资源保障力★★ 洞察决策力★★★ 人才驱动力★★ 体系管控力★★★', 1, '2026-04-23 17:19:50.239', '2026-04-23 17:19:50.239');
INSERT INTO `project_level` VALUES (7, 'C-Q', '产能项目（临床前）', '具有巨大市场潜能；快速布局创新开发；锚定行业管线缺口。', '资源保障力★★ 洞察决策力★★★ 人才驱动力★★ 体系管控力★★★', 1, '2026-04-23 17:19:50.239', '2026-04-23 17:19:50.239');

-- ----------------------------
-- Table structure for project_milestone
-- ----------------------------
DROP TABLE IF EXISTS `project_milestone`;
CREATE TABLE `project_milestone`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id` bigint UNSIGNED NOT NULL,
  `milestone_id` bigint UNSIGNED NOT NULL,
  `planned_date` date NULL DEFAULT NULL,
  `actual_date` date NULL DEFAULT NULL,
  `status` enum('NOT_STARTED','IN_PROGRESS','SUBMITTED','APPROVED','CONDITIONAL_APPROVED','REJECTED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NOT_STARTED',
  `decision_result` enum('GO','CONDITIONAL_GO','NO_GO') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `conditional_deadline` datetime(3) NULL DEFAULT NULL,
  `decision_notes` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `decision_at` datetime(3) NULL DEFAULT NULL,
  `decided_by` bigint UNSIGNED NULL DEFAULT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `conditional_attachments` json NULL COMMENT 'Conditional Go条件附件列表',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_project_milestone`(`project_id` ASC, `milestone_id` ASC) USING BTREE,
  INDEX `idx_project_milestone_project`(`project_id` ASC) USING BTREE,
  INDEX `idx_project_milestone_status`(`status` ASC) USING BTREE,
  INDEX `fk_project_milestone_def`(`milestone_id` ASC) USING BTREE,
  INDEX `fk_project_milestone_decided_by`(`decided_by` ASC) USING BTREE,
  CONSTRAINT `fk_project_milestone_decided_by` FOREIGN KEY (`decided_by`) REFERENCES `iam_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_project_milestone_def` FOREIGN KEY (`milestone_id`) REFERENCES `milestone_def` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_project_milestone_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_project_milestone_dates` CHECK ((`actual_date` is null) or (`planned_date` is null) or (`actual_date` >= `planned_date`))
) ENGINE = InnoDB AUTO_INCREMENT = 446 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of project_milestone
-- ----------------------------
INSERT INTO `project_milestone` VALUES (111, 12, 1, '2026-05-28', NULL, 'IN_PROGRESS', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.813', '2026-05-28 06:58:55.813', NULL);
INSERT INTO `project_milestone` VALUES (112, 12, 2, '2026-08-26', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.813', '2026-05-28 06:58:55.813', NULL);
INSERT INTO `project_milestone` VALUES (113, 12, 3, '2026-11-24', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.813', '2026-05-28 06:58:55.813', NULL);
INSERT INTO `project_milestone` VALUES (114, 12, 4, '2027-02-22', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.813', '2026-05-28 06:58:55.813', NULL);
INSERT INTO `project_milestone` VALUES (115, 12, 5, '2027-05-23', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.813', '2026-05-28 06:58:55.813', NULL);
INSERT INTO `project_milestone` VALUES (116, 12, 6, '2027-08-21', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.813', '2026-05-28 06:58:55.813', NULL);
INSERT INTO `project_milestone` VALUES (117, 12, 7, '2027-11-19', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.813', '2026-05-28 06:58:55.813', NULL);
INSERT INTO `project_milestone` VALUES (118, 12, 8, '2028-02-17', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.813', '2026-05-28 06:58:55.813', NULL);
INSERT INTO `project_milestone` VALUES (119, 12, 9, '2028-05-17', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.813', '2026-05-28 06:58:55.813', NULL);
INSERT INTO `project_milestone` VALUES (120, 12, 10, '2028-08-15', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.813', '2026-05-28 06:58:55.813', NULL);
INSERT INTO `project_milestone` VALUES (126, 13, 1, '2026-02-27', '2026-03-29', 'APPROVED', 'GO', NULL, NULL, '2026-05-28 06:58:55.820', NULL, '2026-05-28 06:58:55.820', '2026-05-28 06:58:55.820', NULL);
INSERT INTO `project_milestone` VALUES (127, 13, 2, '2026-05-28', NULL, 'IN_PROGRESS', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.820', '2026-05-28 06:58:55.820', NULL);
INSERT INTO `project_milestone` VALUES (128, 13, 3, '2026-08-26', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.820', '2026-05-28 06:58:55.820', NULL);
INSERT INTO `project_milestone` VALUES (129, 13, 4, '2026-11-24', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.820', '2026-05-28 06:58:55.820', NULL);
INSERT INTO `project_milestone` VALUES (130, 13, 5, '2027-02-22', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.820', '2026-05-28 06:58:55.820', NULL);
INSERT INTO `project_milestone` VALUES (131, 13, 6, '2027-05-23', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.820', '2026-05-28 06:58:55.820', NULL);
INSERT INTO `project_milestone` VALUES (132, 13, 7, '2027-08-21', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.820', '2026-05-28 06:58:55.820', NULL);
INSERT INTO `project_milestone` VALUES (133, 13, 8, '2027-11-19', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.820', '2026-05-28 06:58:55.820', NULL);
INSERT INTO `project_milestone` VALUES (134, 13, 9, '2028-02-17', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.820', '2026-05-28 06:58:55.820', NULL);
INSERT INTO `project_milestone` VALUES (135, 13, 10, '2028-05-17', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.820', '2026-05-28 06:58:55.820', NULL);
INSERT INTO `project_milestone` VALUES (141, 14, 1, '2025-08-31', '2025-11-29', 'APPROVED', 'GO', NULL, NULL, '2026-05-28 06:58:55.827', NULL, '2026-05-28 06:58:55.827', '2026-05-28 06:58:55.827', NULL);
INSERT INTO `project_milestone` VALUES (142, 14, 2, '2025-11-29', '2026-01-28', 'APPROVED', 'GO', NULL, NULL, '2026-05-28 06:58:55.827', NULL, '2026-05-28 06:58:55.827', '2026-05-28 06:58:55.827', NULL);
INSERT INTO `project_milestone` VALUES (143, 14, 3, '2026-02-27', '2026-03-29', 'APPROVED', 'GO', NULL, NULL, '2026-05-28 06:58:55.827', NULL, '2026-05-28 06:58:55.827', '2026-05-28 06:58:55.827', NULL);
INSERT INTO `project_milestone` VALUES (144, 14, 4, '2026-05-28', NULL, 'IN_PROGRESS', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.827', '2026-05-28 06:58:55.827', NULL);
INSERT INTO `project_milestone` VALUES (145, 14, 5, '2026-08-26', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.827', '2026-05-28 06:58:55.827', NULL);
INSERT INTO `project_milestone` VALUES (146, 14, 6, '2026-11-24', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.827', '2026-05-28 06:58:55.827', NULL);
INSERT INTO `project_milestone` VALUES (147, 14, 7, '2027-02-22', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.827', '2026-05-28 06:58:55.827', NULL);
INSERT INTO `project_milestone` VALUES (148, 14, 8, '2027-05-23', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.827', '2026-05-28 06:58:55.827', NULL);
INSERT INTO `project_milestone` VALUES (149, 14, 9, '2027-08-21', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.827', '2026-05-28 06:58:55.827', NULL);
INSERT INTO `project_milestone` VALUES (150, 14, 10, '2027-11-19', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.827', '2026-05-28 06:58:55.827', NULL);
INSERT INTO `project_milestone` VALUES (156, 15, 1, '2025-03-04', '2025-08-01', 'APPROVED', 'GO', NULL, NULL, '2026-05-28 06:58:55.834', NULL, '2026-05-28 06:58:55.834', '2026-05-28 06:58:55.834', NULL);
INSERT INTO `project_milestone` VALUES (157, 15, 2, '2025-06-02', '2025-09-30', 'APPROVED', 'GO', NULL, NULL, '2026-05-28 06:58:55.834', NULL, '2026-05-28 06:58:55.834', '2026-05-28 06:58:55.834', NULL);
INSERT INTO `project_milestone` VALUES (158, 15, 3, '2025-08-31', '2025-11-29', 'APPROVED', 'GO', NULL, NULL, '2026-05-28 06:58:55.834', NULL, '2026-05-28 06:58:55.834', '2026-05-28 06:58:55.834', NULL);
INSERT INTO `project_milestone` VALUES (159, 15, 4, '2025-11-29', '2026-01-28', 'APPROVED', 'GO', NULL, NULL, '2026-05-28 06:58:55.834', NULL, '2026-05-28 06:58:55.834', '2026-05-28 06:58:55.834', NULL);
INSERT INTO `project_milestone` VALUES (160, 15, 5, '2026-02-27', '2026-03-29', 'APPROVED', 'GO', NULL, NULL, '2026-05-28 06:58:55.834', NULL, '2026-05-28 06:58:55.834', '2026-05-28 06:58:55.834', NULL);
INSERT INTO `project_milestone` VALUES (161, 15, 6, '2026-05-28', NULL, 'IN_PROGRESS', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.834', '2026-05-28 06:58:55.834', NULL);
INSERT INTO `project_milestone` VALUES (162, 15, 7, '2026-08-26', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.834', '2026-05-28 06:58:55.834', NULL);
INSERT INTO `project_milestone` VALUES (163, 15, 8, '2026-11-24', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.834', '2026-05-28 06:58:55.834', NULL);
INSERT INTO `project_milestone` VALUES (164, 15, 9, '2027-02-22', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.834', '2026-05-28 06:58:55.834', NULL);
INSERT INTO `project_milestone` VALUES (165, 15, 10, '2027-05-23', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.834', '2026-05-28 06:58:55.834', NULL);
INSERT INTO `project_milestone` VALUES (171, 16, 1, '2024-09-05', '2025-04-03', 'APPROVED', 'GO', NULL, NULL, '2026-05-28 06:58:55.840', NULL, '2026-05-28 06:58:55.840', '2026-05-28 06:58:55.840', NULL);
INSERT INTO `project_milestone` VALUES (172, 16, 2, '2024-12-04', '2025-06-02', 'APPROVED', 'GO', NULL, NULL, '2026-05-28 06:58:55.840', NULL, '2026-05-28 06:58:55.840', '2026-05-28 06:58:55.840', NULL);
INSERT INTO `project_milestone` VALUES (173, 16, 3, '2025-03-04', '2025-08-01', 'APPROVED', 'GO', NULL, NULL, '2026-05-28 06:58:55.840', NULL, '2026-05-28 06:58:55.840', '2026-05-28 06:58:55.840', NULL);
INSERT INTO `project_milestone` VALUES (174, 16, 4, '2025-06-02', '2025-09-30', 'APPROVED', 'GO', NULL, NULL, '2026-05-28 06:58:55.840', NULL, '2026-05-28 06:58:55.840', '2026-05-28 06:58:55.840', NULL);
INSERT INTO `project_milestone` VALUES (175, 16, 5, '2025-08-31', '2025-11-29', 'APPROVED', 'GO', NULL, NULL, '2026-05-28 06:58:55.840', NULL, '2026-05-28 06:58:55.840', '2026-05-28 06:58:55.840', NULL);
INSERT INTO `project_milestone` VALUES (176, 16, 6, '2025-11-29', '2026-01-28', 'APPROVED', 'GO', NULL, NULL, '2026-05-28 06:58:55.840', NULL, '2026-05-28 06:58:55.840', '2026-05-28 06:58:55.840', NULL);
INSERT INTO `project_milestone` VALUES (177, 16, 7, '2026-02-27', '2026-03-29', 'APPROVED', 'GO', NULL, NULL, '2026-05-28 06:58:55.840', NULL, '2026-05-28 06:58:55.840', '2026-05-28 06:58:55.840', NULL);
INSERT INTO `project_milestone` VALUES (178, 16, 8, '2026-05-28', NULL, 'IN_PROGRESS', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.840', '2026-05-28 06:58:55.840', NULL);
INSERT INTO `project_milestone` VALUES (179, 16, 9, '2026-08-26', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.840', '2026-05-28 06:58:55.840', NULL);
INSERT INTO `project_milestone` VALUES (180, 16, 10, '2026-11-24', NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.840', '2026-05-28 06:58:55.840', NULL);
INSERT INTO `project_milestone` VALUES (186, 17, 1, '2024-03-09', '2024-12-04', 'APPROVED', 'GO', NULL, NULL, '2026-05-28 06:58:55.847', NULL, '2026-05-28 06:58:55.847', '2026-05-28 06:58:55.847', NULL);
INSERT INTO `project_milestone` VALUES (187, 17, 2, '2024-06-07', '2025-02-02', 'APPROVED', 'GO', NULL, NULL, '2026-05-28 06:58:55.847', NULL, '2026-05-28 06:58:55.847', '2026-05-28 06:58:55.847', NULL);
INSERT INTO `project_milestone` VALUES (188, 17, 3, '2024-09-05', '2025-04-03', 'APPROVED', 'GO', NULL, NULL, '2026-05-28 06:58:55.847', NULL, '2026-05-28 06:58:55.847', '2026-05-28 06:58:55.847', NULL);
INSERT INTO `project_milestone` VALUES (189, 17, 4, '2024-12-04', '2025-06-02', 'APPROVED', 'GO', NULL, NULL, '2026-05-28 06:58:55.847', NULL, '2026-05-28 06:58:55.847', '2026-05-28 06:58:55.847', NULL);
INSERT INTO `project_milestone` VALUES (190, 17, 5, '2025-03-04', '2025-08-01', 'APPROVED', 'GO', NULL, NULL, '2026-05-28 06:58:55.847', NULL, '2026-05-28 06:58:55.847', '2026-05-28 06:58:55.847', NULL);
INSERT INTO `project_milestone` VALUES (191, 17, 6, '2025-06-02', '2025-09-30', 'APPROVED', 'GO', NULL, NULL, '2026-05-28 06:58:55.847', NULL, '2026-05-28 06:58:55.847', '2026-05-28 06:58:55.847', NULL);
INSERT INTO `project_milestone` VALUES (192, 17, 7, '2025-08-31', '2025-11-29', 'APPROVED', 'GO', NULL, NULL, '2026-05-28 06:58:55.847', NULL, '2026-05-28 06:58:55.847', '2026-05-28 06:58:55.847', NULL);
INSERT INTO `project_milestone` VALUES (193, 17, 8, '2025-11-29', '2026-01-28', 'APPROVED', 'GO', NULL, NULL, '2026-05-28 06:58:55.847', NULL, '2026-05-28 06:58:55.847', '2026-05-28 06:58:55.847', NULL);
INSERT INTO `project_milestone` VALUES (194, 17, 9, '2026-02-27', '2026-03-29', 'APPROVED', 'GO', NULL, NULL, '2026-05-28 06:58:55.847', NULL, '2026-05-28 06:58:55.847', '2026-05-28 06:58:55.847', NULL);
INSERT INTO `project_milestone` VALUES (195, 17, 10, '2026-05-28', NULL, 'IN_PROGRESS', NULL, NULL, NULL, NULL, NULL, '2026-05-28 06:58:55.847', '2026-05-28 06:58:55.847', NULL);
INSERT INTO `project_milestone` VALUES (406, 40, 1, NULL, '2026-07-29', 'APPROVED', 'GO', NULL, '', '2026-07-29 09:03:07.171', NULL, '2026-07-29 16:54:55.016', '2026-07-29 17:03:07.181', NULL);
INSERT INTO `project_milestone` VALUES (407, 40, 2, NULL, NULL, 'IN_PROGRESS', NULL, NULL, NULL, NULL, NULL, '2026-07-29 16:54:55.016', '2026-07-29 17:03:07.195', NULL);
INSERT INTO `project_milestone` VALUES (408, 40, 3, NULL, NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-07-29 16:54:55.016', '2026-07-29 16:54:55.016', NULL);
INSERT INTO `project_milestone` VALUES (409, 40, 4, NULL, NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-07-29 16:54:55.016', '2026-07-29 16:54:55.016', NULL);
INSERT INTO `project_milestone` VALUES (410, 40, 5, NULL, NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-07-29 16:54:55.016', '2026-07-29 16:54:55.016', NULL);
INSERT INTO `project_milestone` VALUES (411, 40, 6, NULL, NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-07-29 16:54:55.016', '2026-07-29 16:54:55.016', NULL);
INSERT INTO `project_milestone` VALUES (412, 40, 7, NULL, NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-07-29 16:54:55.016', '2026-07-29 16:54:55.016', NULL);
INSERT INTO `project_milestone` VALUES (413, 40, 8, NULL, NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-07-29 16:54:55.016', '2026-07-29 16:54:55.016', NULL);
INSERT INTO `project_milestone` VALUES (414, 40, 9, NULL, NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-07-29 16:54:55.016', '2026-07-29 16:54:55.016', NULL);
INSERT INTO `project_milestone` VALUES (415, 40, 10, NULL, NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-07-29 16:54:55.016', '2026-07-29 16:54:55.016', NULL);
INSERT INTO `project_milestone` VALUES (416, 41, 1, NULL, NULL, 'IN_PROGRESS', NULL, NULL, NULL, NULL, NULL, '2026-07-29 17:32:29.752', '2026-07-29 17:32:29.752', NULL);
INSERT INTO `project_milestone` VALUES (417, 41, 2, NULL, NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-07-29 17:32:29.752', '2026-07-29 17:32:29.752', NULL);
INSERT INTO `project_milestone` VALUES (418, 41, 3, NULL, NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-07-29 17:32:29.752', '2026-07-29 17:32:29.752', NULL);
INSERT INTO `project_milestone` VALUES (419, 41, 4, NULL, NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-07-29 17:32:29.752', '2026-07-29 17:32:29.752', NULL);
INSERT INTO `project_milestone` VALUES (420, 41, 5, NULL, NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-07-29 17:32:29.752', '2026-07-29 17:32:29.752', NULL);
INSERT INTO `project_milestone` VALUES (421, 41, 6, NULL, NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-07-29 17:32:29.752', '2026-07-29 17:32:29.752', NULL);
INSERT INTO `project_milestone` VALUES (422, 41, 7, NULL, NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-07-29 17:32:29.752', '2026-07-29 17:32:29.752', NULL);
INSERT INTO `project_milestone` VALUES (423, 41, 8, NULL, NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-07-29 17:32:29.752', '2026-07-29 17:32:29.752', NULL);
INSERT INTO `project_milestone` VALUES (424, 41, 9, NULL, NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-07-29 17:32:29.752', '2026-07-29 17:32:29.752', NULL);
INSERT INTO `project_milestone` VALUES (425, 41, 10, NULL, NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-07-29 17:32:29.752', '2026-07-29 17:32:29.752', NULL);
INSERT INTO `project_milestone` VALUES (436, 43, 1, NULL, NULL, 'APPROVED', 'GO', NULL, NULL, '2026-07-30 16:13:22.768', NULL, '2026-07-30 01:56:53.275', '2026-07-30 16:13:22.752', NULL);
INSERT INTO `project_milestone` VALUES (437, 43, 2, NULL, NULL, 'IN_PROGRESS', NULL, NULL, NULL, NULL, NULL, '2026-07-30 01:56:53.275', '2026-07-30 16:13:22.752', NULL);
INSERT INTO `project_milestone` VALUES (438, 43, 3, NULL, NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-07-30 01:56:53.275', '2026-07-30 16:13:22.752', NULL);
INSERT INTO `project_milestone` VALUES (439, 43, 4, NULL, NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-07-30 01:56:53.275', '2026-07-30 16:13:22.752', NULL);
INSERT INTO `project_milestone` VALUES (440, 43, 5, NULL, NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-07-30 01:56:53.275', '2026-07-30 16:13:22.752', NULL);
INSERT INTO `project_milestone` VALUES (441, 43, 6, NULL, NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-07-30 01:56:53.275', '2026-07-30 16:13:22.752', NULL);
INSERT INTO `project_milestone` VALUES (442, 43, 7, NULL, NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-07-30 01:56:53.275', '2026-07-30 16:13:22.752', NULL);
INSERT INTO `project_milestone` VALUES (443, 43, 8, NULL, NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-07-30 01:56:53.275', '2026-07-30 16:13:22.752', NULL);
INSERT INTO `project_milestone` VALUES (444, 43, 9, NULL, NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-07-30 01:56:53.275', '2026-07-30 16:13:22.752', NULL);
INSERT INTO `project_milestone` VALUES (445, 43, 10, NULL, NULL, 'NOT_STARTED', NULL, NULL, NULL, NULL, NULL, '2026-07-30 01:56:53.275', '2026-07-30 16:13:22.752', NULL);

-- ----------------------------
-- Table structure for project_team_member
-- ----------------------------
DROP TABLE IF EXISTS `project_team_member`;
CREATE TABLE `project_team_member`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id` bigint UNSIGNED NOT NULL,
  `user_id` bigint UNSIGNED NOT NULL,
  `dept_id` bigint UNSIGNED NULL DEFAULT NULL,
  `team_role` enum('PM','PDT_LEAD','FUNCTION_LEAD','MEMBER') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'MEMBER',
  `effective_from` date NOT NULL,
  `effective_to` date NULL DEFAULT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_project_team_member`(`project_id` ASC, `user_id` ASC, `effective_from` ASC) USING BTREE,
  INDEX `idx_project_team_project`(`project_id` ASC) USING BTREE,
  INDEX `idx_project_team_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_project_team_dept`(`dept_id` ASC) USING BTREE,
  CONSTRAINT `fk_project_team_dept` FOREIGN KEY (`dept_id`) REFERENCES `org_department` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_project_team_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_project_team_user` FOREIGN KEY (`user_id`) REFERENCES `iam_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_project_team_dates` CHECK ((`effective_to` is null) or (`effective_to` >= `effective_from`))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of project_team_member
-- ----------------------------

-- ----------------------------
-- Table structure for project_termination_request
-- ----------------------------
DROP TABLE IF EXISTS `project_termination_request`;
CREATE TABLE `project_termination_request`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id` bigint UNSIGNED NOT NULL COMMENT '项目ID',
  `requested_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '发起人（项目经理）',
  `termination_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '终止原因',
  `attachment_uri` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '终止建议附件路径',
  `status` enum('DRAFT','SUBMITTED','EFFICIENCY_APPROVED','EFFICIENCY_REJECTED','PMC_APPROVED','PMC_REJECTED','COMPLETED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DRAFT' COMMENT '终止流程状态',
  `efficiency_approver_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '效率管理部审批人',
  `efficiency_opinion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '效率管理部审批意见',
  `efficiency_decided_at` datetime(3) NULL DEFAULT NULL COMMENT '效率管理部审批时间',
  `pmc_approver_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT 'PMC审批人',
  `pmc_opinion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'PMC审批意见',
  `pmc_decided_at` datetime(3) NULL DEFAULT NULL COMMENT 'PMC审批时间',
  `summary_report_uri` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '项目总结报告附件路径',
  `asset_disposal_confirmed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '资产处置确认',
  `archive_confirmed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '归档确认',
  `submitted_at` datetime(3) NULL DEFAULT NULL COMMENT '提交时间',
  `finished_at` datetime(3) NULL DEFAULT NULL COMMENT '完成时间',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_termination_project`(`project_id` ASC) USING BTREE,
  INDEX `idx_termination_status`(`status` ASC) USING BTREE,
  INDEX `fk_termination_requested_by`(`requested_by` ASC) USING BTREE,
  CONSTRAINT `fk_termination_req_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_termination_requested_by` FOREIGN KEY (`requested_by`) REFERENCES `iam_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '项目终止申请表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of project_termination_request
-- ----------------------------

-- ----------------------------
-- Table structure for project_termination_task
-- ----------------------------
DROP TABLE IF EXISTS `project_termination_task`;
CREATE TABLE `project_termination_task`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id` bigint UNSIGNED NOT NULL,
  `change_request_id` bigint UNSIGNED NULL DEFAULT NULL,
  `task_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务代码，如 ASSET_DISPOSAL, DOCUMENT_ARCHIVE',
  `task_description` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务详细描述',
  `status` enum('OPEN','COMPLETED','OVERDUE','CANCELLED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'OPEN' COMMENT '任务状态',
  `due_date` date NULL DEFAULT NULL COMMENT '截止日期',
  `completed_at` datetime(3) NULL DEFAULT NULL COMMENT '完成时间',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_termination_project`(`project_id` ASC) USING BTREE,
  INDEX `idx_termination_status`(`status` ASC) USING BTREE,
  INDEX `fk_termination_change_request`(`change_request_id` ASC) USING BTREE,
  CONSTRAINT `fk_termination_change_request` FOREIGN KEY (`change_request_id`) REFERENCES `project_change_request` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_termination_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '项目终止任务清单' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of project_termination_task
-- ----------------------------

-- ----------------------------
-- Table structure for review_approval
-- ----------------------------
DROP TABLE IF EXISTS `review_approval`;
CREATE TABLE `review_approval`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id` bigint UNSIGNED NOT NULL COMMENT '项目ID',
  `project_milestone_id` bigint UNSIGNED NOT NULL COMMENT '项目里程碑ID',
  `wf_instance_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '工作流实例ID',
  `submitter_user_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '提交人（发起人）',
  `submit_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '提交备注',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/SUBMITTED/APPROVED/REJECTED',
  `submitted_at` datetime(3) NULL DEFAULT NULL COMMENT '提交时间',
  `finished_at` datetime(3) NULL DEFAULT NULL COMMENT '完成时间',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `review_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'MILESTONE' COMMENT '评审类型: INITIATION(立项评审) / MILESTONE(里程碑评审)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ra_project`(`project_id` ASC) USING BTREE,
  INDEX `idx_ra_milestone`(`project_milestone_id` ASC) USING BTREE,
  INDEX `idx_ra_status`(`status` ASC) USING BTREE,
  INDEX `fk_ra_wf_instance`(`wf_instance_id` ASC) USING BTREE,
  INDEX `fk_ra_submitter`(`submitter_user_id` ASC) USING BTREE,
  CONSTRAINT `fk_ra_milestone` FOREIGN KEY (`project_milestone_id`) REFERENCES `project_milestone` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ra_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ra_submitter` FOREIGN KEY (`submitter_user_id`) REFERENCES `iam_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ra_wf_instance` FOREIGN KEY (`wf_instance_id`) REFERENCES `wf_instance` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of review_approval
-- ----------------------------
INSERT INTO `review_approval` VALUES (19, 40, 406, NULL, 8, '111', 'APPROVED', '2026-07-29 08:55:48.839', '2026-07-29 09:03:07.171', '2026-07-29 16:55:48.839', '2026-07-29 17:03:07.181', 'MILESTONE');

-- ----------------------------
-- Table structure for review_approval_task
-- ----------------------------
DROP TABLE IF EXISTS `review_approval_task`;
CREATE TABLE `review_approval_task`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `review_approval_id` bigint UNSIGNED NOT NULL COMMENT '评审审批记录ID',
  `approver_user_id` bigint UNSIGNED NOT NULL COMMENT '审批人用户ID',
  `approver_role` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审批人角色',
  `step_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审批步骤代码: UPLOAD/DEPT_HEAD_APPROVE/PM_TECH_REVIEW/COMPLIANCE_OPINION/PMC_DECISION/PM_INTERNAL_REVIEW',
  `deliverable_slot_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '交付物槽位代码（上传步骤使用）',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '审批顺序',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED',
  `decision` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '决策：APPROVED/REJECTED',
  `opinion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '审批意见',
  `decided_at` datetime(3) NULL DEFAULT NULL COMMENT '决策时间',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `conditional_attachment_required` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否需要条件附件(Conditional Go时)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rat_approval`(`review_approval_id` ASC) USING BTREE,
  INDEX `idx_rat_approver`(`approver_user_id` ASC, `status` ASC) USING BTREE,
  CONSTRAINT `fk_rat_approval` FOREIGN KEY (`review_approval_id`) REFERENCES `review_approval` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_rat_approver` FOREIGN KEY (`approver_user_id`) REFERENCES `iam_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 67 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of review_approval_task
-- ----------------------------
INSERT INTO `review_approval_task` VALUES (61, 19, 9, 'DEPT_HEAD', 'DEPT_HEAD_APPROVE', NULL, 0, 'APPROVED', 'GO', '111', '2026-07-29 08:56:25.791', '2026-07-29 16:55:48.853', '2026-07-29 16:56:25.791', 0);
INSERT INTO `review_approval_task` VALUES (62, 19, 2, 'ROLE_PM', 'PM_TECH_REVIEW', NULL, 1, 'APPROVED', 'GO', '111', '2026-07-29 08:57:50.284', '2026-07-29 16:55:48.853', '2026-07-29 16:57:50.284', 0);
INSERT INTO `review_approval_task` VALUES (63, 19, 12, 'DEPT_HEAD', 'COMPLIANCE_OPINION', NULL, 2, 'APPROVED', 'GO', '111', '2026-07-29 09:00:20.068', '2026-07-29 16:55:48.856', '2026-07-29 17:00:20.068', 0);
INSERT INTO `review_approval_task` VALUES (64, 19, 1, 'ROLE_PMC', 'PMC_DECISION', NULL, 3, 'APPROVED', 'GO', '', '2026-07-29 09:03:07.171', '2026-07-29 16:55:48.863', '2026-07-29 17:03:07.171', 0);
INSERT INTO `review_approval_task` VALUES (65, 19, 6, 'ROLE_PMC', 'PMC_DECISION', NULL, 4, 'APPROVED', 'GO', '', '2026-07-29 09:00:42.790', '2026-07-29 16:55:48.863', '2026-07-29 17:00:42.790', 0);
INSERT INTO `review_approval_task` VALUES (66, 19, 18, 'ROLE_PMC', 'PMC_DECISION', NULL, 5, 'APPROVED', 'GO', '', '2026-07-29 09:00:59.778', '2026-07-29 16:55:48.863', '2026-07-29 17:00:59.778', 0);

-- ----------------------------
-- Table structure for review_record
-- ----------------------------
DROP TABLE IF EXISTS `review_record`;
CREATE TABLE `review_record`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id` bigint UNSIGNED NOT NULL COMMENT '项目ID',
  `project_milestone_id` bigint UNSIGNED NOT NULL COMMENT '项目里程碑ID',
  `review_approval_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '关联的审批记录ID',
  `action` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作类型：SUBMIT/APPROVE/REJECT/SAVE_DRAFT',
  `actor_user_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '操作人',
  `actor_role` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人角色',
  `result` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '结果：PASS/FAIL/SUBMITTED/DRAFT_SAVED',
  `opinion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '意见',
  `action_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '操作时间',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `review_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'MILESTONE' COMMENT '评审类型: INITIATION(立项评审) / MILESTONE(里程碑评审)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rr_project`(`project_id` ASC, `action_at` ASC) USING BTREE,
  INDEX `idx_rr_milestone`(`project_milestone_id` ASC) USING BTREE,
  INDEX `idx_rr_actor`(`actor_user_id` ASC) USING BTREE,
  INDEX `fk_rr_approval`(`review_approval_id` ASC) USING BTREE,
  CONSTRAINT `fk_rr_actor` FOREIGN KEY (`actor_user_id`) REFERENCES `iam_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_rr_approval` FOREIGN KEY (`review_approval_id`) REFERENCES `review_approval` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_rr_milestone` FOREIGN KEY (`project_milestone_id`) REFERENCES `project_milestone` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_rr_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 29 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of review_record
-- ----------------------------
INSERT INTO `review_record` VALUES (22, 40, 406, 19, 'SUBMIT', 8, 'ROLE_PMC', 'SUBMITTED', '111', '2026-07-29 08:55:48.899', '2026-07-29 16:55:48.899', 'MILESTONE');
INSERT INTO `review_record` VALUES (23, 40, 406, 19, 'GO', 9, 'DEPT_HEAD', 'GO', '111', '2026-07-29 08:56:25.799', '2026-07-29 16:56:25.799', 'MILESTONE');
INSERT INTO `review_record` VALUES (24, 40, 406, 19, 'PM_TECH_REVIEW', 2, 'ROLE_PM', 'GO', '111', '2026-07-29 08:57:50.289', '2026-07-29 16:57:50.289', 'MILESTONE');
INSERT INTO `review_record` VALUES (25, 40, 406, 19, 'COMPLIANCE_OPINION', 12, 'DEPT_HEAD', 'GO', '111', '2026-07-29 09:00:20.074', '2026-07-29 17:00:20.074', 'MILESTONE');
INSERT INTO `review_record` VALUES (26, 40, 406, 19, 'GO', 6, 'ROLE_PMC', 'GO', '', '2026-07-29 09:00:42.797', '2026-07-29 17:00:42.797', 'MILESTONE');
INSERT INTO `review_record` VALUES (27, 40, 406, 19, 'GO', 18, 'ROLE_PMC', 'GO', '', '2026-07-29 09:00:59.785', '2026-07-29 17:00:59.785', 'MILESTONE');
INSERT INTO `review_record` VALUES (28, 40, 406, 19, 'GO', 1, 'ROLE_PMC', 'GO', '', '2026-07-29 09:03:07.176', '2026-07-29 17:03:07.176', 'MILESTONE');

-- ----------------------------
-- Table structure for role
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '角色描述',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `name`(`name` ASC) USING BTREE,
  UNIQUE INDEX `uk_role_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of role
-- ----------------------------
INSERT INTO `role` VALUES (1, 'ROLE_PMC', 'PMC（项目管理委员会）- 拥有最高决策权，负责Go/No Go决策、重大变更审批及预算追加审批', '2026-04-27 13:58:56.252', '2026-07-30 06:30:33.347');
INSERT INTO `role` VALUES (2, 'ROLE_PM', 'PM（项目经理）- 负责横向贯通，拥有制定计划、监控预算、组织评审及提交变更申请的权限', '2026-04-27 13:58:56.252', '2026-07-30 02:33:40.169');
INSERT INTO `role` VALUES (3, 'ROLE_DEPT_HEAD', '职能部门负责人 - 负责所属领域的交付物提交', '2026-04-27 13:58:56.252', '2026-07-30 06:33:18.866');
INSERT INTO `role` VALUES (6, 'ROLE_ADMIN', '系统管理员 - 拥有系统管理和配置权限', '2026-04-27 13:58:56.252', '2026-07-30 06:33:24.988');
INSERT INTO `role` VALUES (8, 'ROLE_DEPT_EXECUTOR', '部门执行人 - 可上传交付物并发起评审', '2026-05-20 16:53:00.000', '2026-05-20 16:53:00.000');
INSERT INTO `role` VALUES (13, 'ROLE_PROJECT_ADMIN', '项目管理员 - 可查看所有项目，可创建新项目', '2026-07-03 17:58:50.000', '2026-07-03 17:58:50.000');

-- ----------------------------
-- Table structure for role_permissions
-- ----------------------------
DROP TABLE IF EXISTS `role_permissions`;
CREATE TABLE `role_permissions`  (
  `role_id` bigint UNSIGNED NOT NULL,
  `permission_id` bigint UNSIGNED NOT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`role_id`, `permission_id`) USING BTREE,
  INDEX `idx_role_permissions_permission`(`permission_id` ASC) USING BTREE,
  CONSTRAINT `fk_role_permissions_permission` FOREIGN KEY (`permission_id`) REFERENCES `permission` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_role_permissions_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色-权限关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of role_permissions
-- ----------------------------
INSERT INTO `role_permissions` VALUES (1, 2, '2026-07-30 14:30:33.375');
INSERT INTO `role_permissions` VALUES (1, 4, '2026-07-30 14:30:33.373');
INSERT INTO `role_permissions` VALUES (1, 5, '2026-07-30 14:30:33.366');
INSERT INTO `role_permissions` VALUES (1, 8, '2026-07-30 14:30:33.367');
INSERT INTO `role_permissions` VALUES (1, 12, '2026-07-30 14:30:33.372');
INSERT INTO `role_permissions` VALUES (1, 13, '2026-07-30 14:30:33.369');
INSERT INTO `role_permissions` VALUES (1, 15, '2026-07-30 14:30:33.368');
INSERT INTO `role_permissions` VALUES (1, 16, '2026-07-30 14:30:33.374');
INSERT INTO `role_permissions` VALUES (1, 22, '2026-07-30 14:30:33.365');
INSERT INTO `role_permissions` VALUES (1, 25, '2026-07-30 14:30:33.371');
INSERT INTO `role_permissions` VALUES (1, 27, '2026-07-30 14:30:33.369');
INSERT INTO `role_permissions` VALUES (1, 28, '2026-07-30 14:30:33.372');
INSERT INTO `role_permissions` VALUES (1, 31, '2026-07-30 14:30:33.370');
INSERT INTO `role_permissions` VALUES (1, 33, '2026-07-30 14:30:33.367');
INSERT INTO `role_permissions` VALUES (1, 34, '2026-07-30 14:30:33.368');
INSERT INTO `role_permissions` VALUES (1, 35, '2026-07-30 17:39:26.226');
INSERT INTO `role_permissions` VALUES (1, 36, '2026-07-30 17:39:26.236');
INSERT INTO `role_permissions` VALUES (2, 1, '2026-07-30 10:33:40.244');
INSERT INTO `role_permissions` VALUES (2, 4, '2026-07-30 10:33:40.272');
INSERT INTO `role_permissions` VALUES (2, 6, '2026-07-30 10:33:40.258');
INSERT INTO `role_permissions` VALUES (2, 7, '2026-07-30 10:33:40.252');
INSERT INTO `role_permissions` VALUES (2, 8, '2026-07-30 10:33:40.245');
INSERT INTO `role_permissions` VALUES (2, 9, '2026-07-30 10:33:40.265');
INSERT INTO `role_permissions` VALUES (2, 11, '2026-07-30 10:33:40.251');
INSERT INTO `role_permissions` VALUES (2, 12, '2026-07-30 10:33:40.249');
INSERT INTO `role_permissions` VALUES (2, 14, '2026-07-30 10:33:40.243');
INSERT INTO `role_permissions` VALUES (2, 16, '2026-07-30 10:33:40.267');
INSERT INTO `role_permissions` VALUES (2, 21, '2026-07-30 10:33:40.247');
INSERT INTO `role_permissions` VALUES (2, 22, '2026-07-30 10:33:40.260');
INSERT INTO `role_permissions` VALUES (2, 23, '2026-07-30 10:33:40.254');
INSERT INTO `role_permissions` VALUES (2, 25, '2026-07-30 10:33:40.269');
INSERT INTO `role_permissions` VALUES (2, 26, '2026-07-30 10:33:40.246');
INSERT INTO `role_permissions` VALUES (2, 28, '2026-07-30 10:33:40.250');
INSERT INTO `role_permissions` VALUES (2, 31, '2026-07-30 10:33:40.257');
INSERT INTO `role_permissions` VALUES (2, 33, '2026-07-30 10:33:40.262');
INSERT INTO `role_permissions` VALUES (2, 34, '2026-07-30 10:33:40.255');
INSERT INTO `role_permissions` VALUES (2, 35, '2026-07-30 17:39:26.226');
INSERT INTO `role_permissions` VALUES (3, 2, '2026-07-30 14:33:18.895');
INSERT INTO `role_permissions` VALUES (3, 3, '2026-07-30 14:33:18.889');
INSERT INTO `role_permissions` VALUES (3, 4, '2026-07-30 14:33:18.888');
INSERT INTO `role_permissions` VALUES (3, 5, '2026-07-30 14:33:18.873');
INSERT INTO `role_permissions` VALUES (3, 6, '2026-07-30 14:33:18.894');
INSERT INTO `role_permissions` VALUES (3, 8, '2026-07-30 14:33:18.891');
INSERT INTO `role_permissions` VALUES (3, 11, '2026-07-30 14:33:18.885');
INSERT INTO `role_permissions` VALUES (3, 12, '2026-07-30 14:33:18.872');
INSERT INTO `role_permissions` VALUES (3, 13, '2026-07-30 14:33:18.887');
INSERT INTO `role_permissions` VALUES (3, 28, '2026-07-30 14:33:18.886');
INSERT INTO `role_permissions` VALUES (3, 33, '2026-07-30 14:33:18.893');
INSERT INTO `role_permissions` VALUES (3, 34, '2026-07-30 14:33:18.890');
INSERT INTO `role_permissions` VALUES (3, 35, '2026-07-30 17:39:26.226');
INSERT INTO `role_permissions` VALUES (6, 1, '2026-07-30 14:33:25.003');
INSERT INTO `role_permissions` VALUES (6, 2, '2026-07-30 14:33:25.001');
INSERT INTO `role_permissions` VALUES (6, 3, '2026-07-30 14:33:24.999');
INSERT INTO `role_permissions` VALUES (6, 4, '2026-07-30 14:33:25.006');
INSERT INTO `role_permissions` VALUES (6, 5, '2026-07-30 14:33:24.997');
INSERT INTO `role_permissions` VALUES (6, 6, '2026-07-30 14:33:25.013');
INSERT INTO `role_permissions` VALUES (6, 7, '2026-07-30 14:33:24.999');
INSERT INTO `role_permissions` VALUES (6, 8, '2026-07-30 14:33:25.003');
INSERT INTO `role_permissions` VALUES (6, 9, '2026-07-30 14:33:25.008');
INSERT INTO `role_permissions` VALUES (6, 10, '2026-07-30 14:33:25.004');
INSERT INTO `role_permissions` VALUES (6, 11, '2026-07-30 14:33:25.010');
INSERT INTO `role_permissions` VALUES (6, 12, '2026-07-30 14:33:25.002');
INSERT INTO `role_permissions` VALUES (6, 13, '2026-07-30 14:33:24.998');
INSERT INTO `role_permissions` VALUES (6, 14, '2026-07-30 14:33:24.995');
INSERT INTO `role_permissions` VALUES (6, 15, '2026-07-30 14:33:24.998');
INSERT INTO `role_permissions` VALUES (6, 16, '2026-07-30 14:33:25.007');
INSERT INTO `role_permissions` VALUES (6, 17, '2026-07-30 14:33:25.000');
INSERT INTO `role_permissions` VALUES (6, 18, '2026-07-30 14:33:25.005');
INSERT INTO `role_permissions` VALUES (6, 19, '2026-07-30 14:33:25.011');
INSERT INTO `role_permissions` VALUES (6, 20, '2026-07-30 14:33:24.995');
INSERT INTO `role_permissions` VALUES (6, 21, '2026-07-30 14:33:25.007');
INSERT INTO `role_permissions` VALUES (6, 22, '2026-07-30 14:33:25.009');
INSERT INTO `role_permissions` VALUES (6, 23, '2026-07-30 14:33:24.997');
INSERT INTO `role_permissions` VALUES (6, 24, '2026-07-30 14:33:25.012');
INSERT INTO `role_permissions` VALUES (6, 25, '2026-07-30 14:33:25.010');
INSERT INTO `role_permissions` VALUES (6, 26, '2026-07-30 14:33:25.001');
INSERT INTO `role_permissions` VALUES (6, 27, '2026-07-30 14:33:25.006');
INSERT INTO `role_permissions` VALUES (6, 28, '2026-07-30 14:33:25.012');
INSERT INTO `role_permissions` VALUES (6, 29, '2026-07-30 14:33:25.008');
INSERT INTO `role_permissions` VALUES (6, 31, '2026-07-30 14:33:25.004');
INSERT INTO `role_permissions` VALUES (6, 33, '2026-07-30 14:33:24.996');
INSERT INTO `role_permissions` VALUES (6, 34, '2026-07-30 14:33:25.005');
INSERT INTO `role_permissions` VALUES (6, 35, '2026-07-30 17:39:26.226');
INSERT INTO `role_permissions` VALUES (6, 36, '2026-07-30 17:39:26.236');
INSERT INTO `role_permissions` VALUES (6, 37, '2026-07-30 17:39:26.258');
INSERT INTO `role_permissions` VALUES (8, 1, '2026-05-20 16:56:18.669');
INSERT INTO `role_permissions` VALUES (8, 8, '2026-05-20 16:56:18.669');
INSERT INTO `role_permissions` VALUES (8, 11, '2026-05-20 16:56:18.669');
INSERT INTO `role_permissions` VALUES (8, 35, '2026-07-30 17:39:26.226');
INSERT INTO `role_permissions` VALUES (13, 7, '2026-07-03 17:58:50.131');
INSERT INTO `role_permissions` VALUES (13, 31, '2026-07-03 17:58:50.137');
INSERT INTO `role_permissions` VALUES (13, 35, '2026-07-30 17:39:26.226');
INSERT INTO `role_permissions` VALUES (13, 36, '2026-07-30 17:39:26.236');
INSERT INTO `role_permissions` VALUES (13, 37, '2026-07-30 17:39:26.258');

-- ----------------------------
-- Table structure for system_config
-- ----------------------------
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config`  (
  `config_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '閰嶇疆閿',
  `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '閰嶇疆鍊',
  `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '閰嶇疆璇存槑',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`config_key`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '绯荤粺閰嶇疆琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of system_config
-- ----------------------------
INSERT INTO `system_config` VALUES ('mail.enabled', 'true', 'Mail enabled', '2026-07-29 17:33:31');
INSERT INTO `system_config` VALUES ('mail.from.address', 'kbd_pms@baiyu.cn', 'From address', '2026-07-29 17:33:31');
INSERT INTO `system_config` VALUES ('mail.smtp.host', 'smtp.qiye.163.com', 'SMTP host', '2026-07-29 17:33:31');
INSERT INTO `system_config` VALUES ('mail.smtp.password', 'NW#nsVxQV7bk%HGg', 'SMTP password', '2026-07-28 02:02:31');
INSERT INTO `system_config` VALUES ('mail.smtp.port', '465', 'SMTP port', '2026-07-29 17:33:31');
INSERT INTO `system_config` VALUES ('mail.smtp.ssl', 'true', 'SSL enabled', '2026-07-29 17:33:31');
INSERT INTO `system_config` VALUES ('mail.smtp.username', 'kbd_pms@baiyu.cn', 'SMTP username', '2026-07-29 17:33:31');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '加密密码',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否激活',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uk_user_username`(`username` ASC) USING BTREE,
  INDEX `idx_user_email`(`email` ASC) USING BTREE,
  INDEX `idx_user_is_active`(`is_active` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统认证用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, 'pmc_user', '$2a$10$.rBvirRTOg2dzJ7OZ0iv4eAFuN951wUbt9BIf4P8maDT5Q1OTKnu2', 'pmc@example.com', 1, '2026-04-27 13:58:56.286', '2026-07-29 17:01:48.208');
INSERT INTO `user` VALUES (2, 'pm_user', '$2a$10$FgtNe4crQCIAmPc5Of5lLOquGJYbiNa50TN0iUy5/IqgzVfivKOS2', '836487344@qq.com', 1, '2026-04-27 13:58:56.286', '2026-07-29 17:31:59.735');
INSERT INTO `user` VALUES (3, 'dept_head', '$2a$10$BKt1k9PEGF45c44yibwo/e8Qo6Eavns9FpeatKhsqMUXIbkW6BehW', 'dept@example.com', 1, '2026-04-27 13:58:56.286', '2026-07-28 16:06:23.675');
INSERT INTO `user` VALUES (6, 'admin_user', '$2b$12$NcQmzq3T6d0ObMRep.HN3unEy67hJmDnix83ZuThjpA0RDqgo135i', 'admin@example.com', 1, '2026-04-27 13:58:56.286', '2026-07-29 23:28:04.962');
INSERT INTO `user` VALUES (8, '资讯部执行人', '$2a$10$IkgbtCtZc70Q/GtofAprLe7Bsp5jOPIrHiKCz1UOCrsuMPMzPfs2q', '', 1, '2026-07-22 06:02:29.635', '2026-07-26 12:25:38.535');
INSERT INTO `user` VALUES (9, '资讯部负责人', '$2a$10$u1RfxKmj51zY4A4TAPGV6.as/BKZzcse6bIldA8I79Ik7jH71dVsS', '', 1, '2026-07-22 06:06:35.748', '2026-07-22 06:06:35.748');
INSERT INTO `user` VALUES (10, '化学部执行人', '$2a$10$54VyXErmlu5iwfWGKZt2kOlngBMSLY3lLnYzCnJp7cVdilm7EaXhS', '', 1, '2026-07-22 06:08:16.878', '2026-07-22 06:08:16.878');
INSERT INTO `user` VALUES (11, '化学部负责人', '$2a$10$66ZipidAGCddCKOY5bTfqeQXCfYzx8mIOCxhAjCiLqop5rs5Hf/cC', '', 1, '2026-07-22 06:08:44.299', '2026-07-22 06:08:44.299');
INSERT INTO `user` VALUES (12, '药政合规部负责人', '$2a$10$w2CN5oUBkHSQ0KUEvhgGA.pHhlXtSZKTV9vD4I3kvW/OC/K/oWSme', '', 1, '2026-07-26 15:49:51.981', '2026-07-30 00:20:11.467');
INSERT INTO `user` VALUES (13, '项目管理员', '$2a$10$EsmUp/tDkxsb6Q8Fuit0qesH4FWkSp/gNgQgtBKYZuoW4FSudvibe', '', 1, '2026-07-27 03:27:05.875', '2026-07-27 03:27:05.875');
INSERT INTO `user` VALUES (14, '生物部负责人', '$2a$10$qFx6AL0flpyMkliKfesXSeDkmtw8mwVtZNAK/MHg64a1BvPqpnm3K', '', 1, '2026-07-28 16:06:58.598', '2026-07-28 16:06:58.598');
INSERT INTO `user` VALUES (15, '生物部执行人', '$2a$10$N6qNFTEflPZR2hBihVkchuqWAk6BZWWqKqGQ2Epk2A38UzOE9l8kK', '', 1, '2026-07-28 16:07:19.846', '2026-07-28 16:07:19.846');
INSERT INTO `user` VALUES (17, '效率管理部负责人', '$2a$10$E4lasheYHlDVJUyYgbciB.3XSX3jDymOTijtLONsTPPCh73HpfqEi', '836487344@qq.com', 1, '2026-07-28 16:07:57.479', '2026-07-30 02:16:57.909');
INSERT INTO `user` VALUES (18, 'pmc_user1', '$2a$10$VDME5vZjQTg5oZnaPqYeEeDX7J4ranUFjB2wD8E1QFqhkp6pvrCem', '', 1, '2026-07-28 16:08:30.800', '2026-07-28 16:08:30.800');

-- ----------------------------
-- Table structure for user_departments
-- ----------------------------
DROP TABLE IF EXISTS `user_departments`;
CREATE TABLE `user_departments`  (
  `user_id` bigint UNSIGNED NOT NULL,
  `department_id` bigint UNSIGNED NOT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`user_id`, `department_id`) USING BTREE,
  INDEX `idx_ud_department`(`department_id` ASC) USING BTREE,
  CONSTRAINT `fk_ud_department` FOREIGN KEY (`department_id`) REFERENCES `org_department` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_ud_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_departments
-- ----------------------------
INSERT INTO `user_departments` VALUES (1, 10, '2026-06-23 13:55:47.385');
INSERT INTO `user_departments` VALUES (2, 9, '2026-07-30 01:31:59.744');
INSERT INTO `user_departments` VALUES (3, 5, '2026-06-23 16:50:04.529');
INSERT INTO `user_departments` VALUES (6, 9, '2026-06-23 16:33:00.135');
INSERT INTO `user_departments` VALUES (8, 4, '2026-07-22 14:02:29.786');
INSERT INTO `user_departments` VALUES (9, 4, '2026-07-22 14:06:35.768');
INSERT INTO `user_departments` VALUES (10, 1, '2026-07-22 14:08:16.890');
INSERT INTO `user_departments` VALUES (11, 1, '2026-07-22 14:08:44.308');
INSERT INTO `user_departments` VALUES (12, 7, '2026-07-26 23:49:52.075');
INSERT INTO `user_departments` VALUES (13, 9, '2026-07-27 11:27:06.100');
INSERT INTO `user_departments` VALUES (14, 2, '2026-07-29 00:06:58.614');
INSERT INTO `user_departments` VALUES (15, 2, '2026-07-29 00:07:19.860');
INSERT INTO `user_departments` VALUES (17, 6, '2026-07-30 10:16:57.982');
INSERT INTO `user_departments` VALUES (18, 10, '2026-07-29 00:08:30.809');

-- ----------------------------
-- Table structure for user_roles
-- ----------------------------
DROP TABLE IF EXISTS `user_roles`;
CREATE TABLE `user_roles`  (
  `user_id` bigint UNSIGNED NOT NULL,
  `role_id` bigint UNSIGNED NOT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`user_id`, `role_id`) USING BTREE,
  INDEX `idx_user_roles_role`(`role_id` ASC) USING BTREE,
  CONSTRAINT `fk_user_roles_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_user_roles_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户-角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_roles
-- ----------------------------
INSERT INTO `user_roles` VALUES (1, 1, '2026-04-27 13:58:56.292');
INSERT INTO `user_roles` VALUES (2, 2, '2026-04-27 13:58:56.297');
INSERT INTO `user_roles` VALUES (3, 3, '2026-04-27 13:58:56.299');
INSERT INTO `user_roles` VALUES (6, 1, '2026-07-27 10:52:54.143');
INSERT INTO `user_roles` VALUES (6, 2, '2026-07-27 10:52:54.150');
INSERT INTO `user_roles` VALUES (6, 3, '2026-07-27 10:52:54.145');
INSERT INTO `user_roles` VALUES (6, 6, '2026-07-27 10:52:54.148');
INSERT INTO `user_roles` VALUES (6, 8, '2026-07-27 10:52:54.146');
INSERT INTO `user_roles` VALUES (8, 8, '2026-07-22 14:02:29.791');
INSERT INTO `user_roles` VALUES (9, 3, '2026-07-22 14:06:35.770');
INSERT INTO `user_roles` VALUES (10, 8, '2026-07-22 14:08:16.892');
INSERT INTO `user_roles` VALUES (11, 3, '2026-07-22 14:08:44.310');
INSERT INTO `user_roles` VALUES (12, 3, '2026-07-26 23:49:52.077');
INSERT INTO `user_roles` VALUES (13, 13, '2026-07-27 11:27:06.105');
INSERT INTO `user_roles` VALUES (14, 3, '2026-07-29 00:06:58.617');
INSERT INTO `user_roles` VALUES (15, 8, '2026-07-29 00:07:19.863');
INSERT INTO `user_roles` VALUES (17, 3, '2026-07-29 00:07:57.488');
INSERT INTO `user_roles` VALUES (18, 1, '2026-07-29 00:08:30.810');

-- ----------------------------
-- Table structure for wf_action_log
-- ----------------------------
DROP TABLE IF EXISTS `wf_action_log`;
CREATE TABLE `wf_action_log`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `instance_id` bigint UNSIGNED NOT NULL,
  `task_id` bigint UNSIGNED NULL DEFAULT NULL,
  `action` enum('SUBMIT','APPROVE','REJECT','CANCEL','COMMENT','SYSTEM') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `actor_user_id` bigint UNSIGNED NULL DEFAULT NULL,
  `action_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `payload_json` json NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_wf_action_instance`(`instance_id` ASC, `action_at` ASC) USING BTREE,
  INDEX `idx_wf_action_task`(`task_id` ASC) USING BTREE,
  INDEX `fk_wf_action_actor`(`actor_user_id` ASC) USING BTREE,
  CONSTRAINT `fk_wf_action_actor` FOREIGN KEY (`actor_user_id`) REFERENCES `iam_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_wf_action_instance` FOREIGN KEY (`instance_id`) REFERENCES `wf_instance` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_wf_action_task` FOREIGN KEY (`task_id`) REFERENCES `wf_task` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wf_action_log
-- ----------------------------

-- ----------------------------
-- Table structure for wf_instance
-- ----------------------------
DROP TABLE IF EXISTS `wf_instance`;
CREATE TABLE `wf_instance`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `template_id` bigint UNSIGNED NOT NULL,
  `business_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `business_id` bigint UNSIGNED NOT NULL,
  `status` enum('DRAFT','RUNNING','APPROVED','REJECTED','CANCELLED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DRAFT',
  `started_by` bigint UNSIGNED NULL DEFAULT NULL,
  `started_at` datetime(3) NULL DEFAULT NULL,
  `finished_at` datetime(3) NULL DEFAULT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_wf_instance_business`(`business_type` ASC, `business_id` ASC) USING BTREE,
  INDEX `idx_wf_instance_template`(`template_id` ASC) USING BTREE,
  INDEX `fk_wf_instance_started_by`(`started_by` ASC) USING BTREE,
  CONSTRAINT `fk_wf_instance_started_by` FOREIGN KEY (`started_by`) REFERENCES `iam_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_wf_instance_template` FOREIGN KEY (`template_id`) REFERENCES `wf_template` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wf_instance
-- ----------------------------

-- ----------------------------
-- Table structure for wf_process_definition
-- ----------------------------
DROP TABLE IF EXISTS `wf_process_definition`;
CREATE TABLE `wf_process_definition`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `process_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程类型: MILESTONE/CHANGE/TERMINATION',
  `milestone_code` varchar(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '里程碑代码(G0-G9)，仅MILESTONE类型使用',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '流程说明',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `budget_warning_threshold` decimal(5, 2) NULL DEFAULT NULL COMMENT '预算预警阈值(%)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_process_type_milestone`(`process_type` ASC, `milestone_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流程定义表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wf_process_definition
-- ----------------------------
INSERT INTO `wf_process_definition` VALUES (1, 'MILESTONE', 'G0', 'G0 项目立项里程碑审批流程', 1, '2026-07-10 02:44:00.281', '2026-07-29 14:15:51.920', NULL);
INSERT INTO `wf_process_definition` VALUES (2, 'MILESTONE', 'G1', 'G1 先导化合物确认里程碑审批流程', 1, '2026-07-10 02:44:00.281', '2026-07-10 02:44:00.281', NULL);
INSERT INTO `wf_process_definition` VALUES (3, 'MILESTONE', 'G2', 'G2 优选化合物里程碑审批流程', 1, '2026-07-10 02:44:00.281', '2026-07-10 02:44:00.281', NULL);
INSERT INTO `wf_process_definition` VALUES (4, 'MILESTONE', 'G3', 'G3 候选化合物提名(PCC)里程碑审批流程', 1, '2026-07-10 02:44:00.281', '2026-07-28 07:57:49.424', NULL);
INSERT INTO `wf_process_definition` VALUES (5, 'MILESTONE', 'G4', 'G4 临床前开发完成(GLP)里程碑审批流程', 1, '2026-07-10 02:44:00.281', '2026-07-28 08:10:57.920', NULL);
INSERT INTO `wf_process_definition` VALUES (6, 'MILESTONE', 'G5', 'G5 临床试验申请获批(IND)里程碑审批流程', 1, '2026-07-10 02:44:00.281', '2026-07-28 08:10:51.116', NULL);
INSERT INTO `wf_process_definition` VALUES (7, 'MILESTONE', 'G6', 'G6 临床Ⅰ期里程碑审批流程', 1, '2026-07-10 02:44:00.281', '2026-07-10 02:44:00.281', NULL);
INSERT INTO `wf_process_definition` VALUES (8, 'MILESTONE', 'G7', 'G7 临床Ⅱ期里程碑审批流程', 1, '2026-07-10 02:44:00.281', '2026-07-10 02:44:00.281', NULL);
INSERT INTO `wf_process_definition` VALUES (9, 'MILESTONE', 'G8', 'G8 临床Ⅲ期里程碑审批流程', 1, '2026-07-10 02:44:00.281', '2026-07-10 02:44:00.281', NULL);
INSERT INTO `wf_process_definition` VALUES (10, 'MILESTONE', 'G9', 'G9 新药上市申请获批(NDA)里程碑审批流程', 1, '2026-07-10 02:44:00.281', '2026-07-10 02:44:00.281', NULL);
INSERT INTO `wf_process_definition` VALUES (11, 'CHANGE', NULL, '项目变更审批流程', 1, '2026-07-10 02:44:00.281', '2026-07-10 02:44:00.281', NULL);
INSERT INTO `wf_process_definition` VALUES (12, 'TERMINATION', NULL, '项目终止审批流程', 1, '2026-07-10 02:44:00.281', '2026-07-10 02:44:00.281', NULL);
INSERT INTO `wf_process_definition` VALUES (13, 'BUDGET', NULL, '预算变更审批流程', 1, '2026-07-30 07:49:38.179', '2026-07-30 08:11:28.392', 80.00);

-- ----------------------------
-- Table structure for wf_process_edge
-- ----------------------------
DROP TABLE IF EXISTS `wf_process_edge`;
CREATE TABLE `wf_process_edge`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `process_definition_id` bigint UNSIGNED NOT NULL COMMENT '所属流程定义',
  `from_node_id` bigint UNSIGNED NOT NULL COMMENT '源节点',
  `to_node_id` bigint UNSIGNED NOT NULL COMMENT '目标节点',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_from_node`(`from_node_id` ASC) USING BTREE,
  INDEX `idx_to_node`(`to_node_id` ASC) USING BTREE,
  INDEX `fk_edge_process_def`(`process_definition_id` ASC) USING BTREE,
  CONSTRAINT `fk_edge_from_node` FOREIGN KEY (`from_node_id`) REFERENCES `wf_process_node` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_edge_process_def` FOREIGN KEY (`process_definition_id`) REFERENCES `wf_process_definition` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_edge_to_node` FOREIGN KEY (`to_node_id`) REFERENCES `wf_process_node` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 190 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流程连线表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wf_process_edge
-- ----------------------------
INSERT INTO `wf_process_edge` VALUES (7, 2, 8, 10, '2026-07-10 02:44:00.308');
INSERT INTO `wf_process_edge` VALUES (8, 2, 9, 11, '2026-07-10 02:44:00.308');
INSERT INTO `wf_process_edge` VALUES (9, 2, 10, 12, '2026-07-10 02:44:00.308');
INSERT INTO `wf_process_edge` VALUES (10, 2, 11, 12, '2026-07-10 02:44:00.308');
INSERT INTO `wf_process_edge` VALUES (11, 2, 12, 13, '2026-07-10 02:44:00.308');
INSERT INTO `wf_process_edge` VALUES (12, 2, 13, 14, '2026-07-10 02:44:00.308');
INSERT INTO `wf_process_edge` VALUES (13, 3, 15, 16, '2026-07-10 02:44:00.327');
INSERT INTO `wf_process_edge` VALUES (14, 3, 16, 17, '2026-07-10 02:44:00.327');
INSERT INTO `wf_process_edge` VALUES (15, 3, 17, 18, '2026-07-10 02:44:00.327');
INSERT INTO `wf_process_edge` VALUES (16, 3, 18, 19, '2026-07-10 02:44:00.328');
INSERT INTO `wf_process_edge` VALUES (34, 7, 40, 41, '2026-07-10 02:44:00.339');
INSERT INTO `wf_process_edge` VALUES (35, 7, 41, 42, '2026-07-10 02:44:00.339');
INSERT INTO `wf_process_edge` VALUES (36, 7, 42, 43, '2026-07-10 02:44:00.339');
INSERT INTO `wf_process_edge` VALUES (37, 7, 43, 44, '2026-07-10 02:44:00.340');
INSERT INTO `wf_process_edge` VALUES (38, 7, 43, 45, '2026-07-10 02:44:00.340');
INSERT INTO `wf_process_edge` VALUES (39, 7, 43, 46, '2026-07-10 02:44:00.340');
INSERT INTO `wf_process_edge` VALUES (40, 8, 47, 48, '2026-07-10 02:44:00.342');
INSERT INTO `wf_process_edge` VALUES (41, 8, 48, 49, '2026-07-10 02:44:00.342');
INSERT INTO `wf_process_edge` VALUES (42, 8, 49, 50, '2026-07-10 02:44:00.342');
INSERT INTO `wf_process_edge` VALUES (43, 8, 50, 51, '2026-07-10 02:44:00.343');
INSERT INTO `wf_process_edge` VALUES (44, 8, 50, 52, '2026-07-10 02:44:00.343');
INSERT INTO `wf_process_edge` VALUES (45, 8, 50, 53, '2026-07-10 02:44:00.343');
INSERT INTO `wf_process_edge` VALUES (46, 9, 54, 55, '2026-07-10 02:44:00.345');
INSERT INTO `wf_process_edge` VALUES (47, 9, 55, 56, '2026-07-10 02:44:00.345');
INSERT INTO `wf_process_edge` VALUES (48, 9, 56, 57, '2026-07-10 02:44:00.346');
INSERT INTO `wf_process_edge` VALUES (49, 9, 57, 58, '2026-07-10 02:44:00.348');
INSERT INTO `wf_process_edge` VALUES (50, 9, 57, 59, '2026-07-10 02:44:00.348');
INSERT INTO `wf_process_edge` VALUES (51, 9, 57, 60, '2026-07-10 02:44:00.348');
INSERT INTO `wf_process_edge` VALUES (52, 10, 61, 62, '2026-07-10 02:44:00.350');
INSERT INTO `wf_process_edge` VALUES (53, 10, 62, 63, '2026-07-10 02:44:00.350');
INSERT INTO `wf_process_edge` VALUES (54, 10, 63, 64, '2026-07-10 02:44:00.353');
INSERT INTO `wf_process_edge` VALUES (55, 10, 63, 65, '2026-07-10 02:44:00.353');
INSERT INTO `wf_process_edge` VALUES (56, 10, 63, 66, '2026-07-10 02:44:00.353');
INSERT INTO `wf_process_edge` VALUES (57, 11, 67, 68, '2026-07-10 02:44:00.371');
INSERT INTO `wf_process_edge` VALUES (58, 12, 69, 70, '2026-07-10 02:44:00.374');
INSERT INTO `wf_process_edge` VALUES (59, 12, 70, 71, '2026-07-10 02:44:00.374');
INSERT INTO `wf_process_edge` VALUES (166, 4, 763, 764, '2026-07-28 07:57:49.494');
INSERT INTO `wf_process_edge` VALUES (167, 4, 762, 763, '2026-07-28 07:57:49.494');
INSERT INTO `wf_process_edge` VALUES (168, 4, 761, 762, '2026-07-28 07:57:49.494');
INSERT INTO `wf_process_edge` VALUES (169, 4, 764, 765, '2026-07-28 07:57:49.494');
INSERT INTO `wf_process_edge` VALUES (177, 6, 777, 778, '2026-07-28 08:10:51.224');
INSERT INTO `wf_process_edge` VALUES (178, 6, 776, 777, '2026-07-28 08:10:51.224');
INSERT INTO `wf_process_edge` VALUES (179, 6, 775, 776, '2026-07-28 08:10:51.224');
INSERT INTO `wf_process_edge` VALUES (180, 5, 779, 780, '2026-07-28 08:10:57.987');
INSERT INTO `wf_process_edge` VALUES (181, 5, 782, 783, '2026-07-28 08:10:57.987');
INSERT INTO `wf_process_edge` VALUES (182, 5, 780, 781, '2026-07-28 08:10:57.987');
INSERT INTO `wf_process_edge` VALUES (183, 5, 781, 782, '2026-07-28 08:10:57.987');
INSERT INTO `wf_process_edge` VALUES (184, 1, 786, 787, '2026-07-29 14:15:52.070');
INSERT INTO `wf_process_edge` VALUES (185, 1, 785, 786, '2026-07-29 14:15:52.070');
INSERT INTO `wf_process_edge` VALUES (186, 1, 787, 788, '2026-07-29 14:15:52.070');
INSERT INTO `wf_process_edge` VALUES (187, 1, 784, 785, '2026-07-29 14:15:52.070');
INSERT INTO `wf_process_edge` VALUES (189, 13, 791, 792, '2026-07-30 08:11:28.479');

-- ----------------------------
-- Table structure for wf_process_node
-- ----------------------------
DROP TABLE IF EXISTS `wf_process_node`;
CREATE TABLE `wf_process_node`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `process_definition_id` bigint UNSIGNED NOT NULL COMMENT '所属流程定义',
  `node_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '节点代码',
  `node_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '节点显示名',
  `node_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '节点类型: UPLOAD/DEPT_HEAD_APPROVE/ROLE_APPROVE/DECISION',
  `approver_rule` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审批人规则: DEPT_HEAD/ROLE_PM/ROLE_PMC/ROLE_COMPLIANCE/SPECIFIC_USER',
  `approver_value` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规则参数(dept_id/role_name/user_id逗号分隔)',
  `decision_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '决策类型: GO_NO_GO/APPROVE_REJECT/NONE',
  `is_uploader` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否为上传节点(Step1)',
  `deliverable_slot_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '交付物槽位代码',
  `position_x` int NOT NULL DEFAULT 0 COMMENT '画布X坐标',
  `position_y` int NOT NULL DEFAULT 0 COMMENT '画布Y坐标',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_process_def`(`process_definition_id` ASC) USING BTREE,
  CONSTRAINT `fk_node_process_def` FOREIGN KEY (`process_definition_id`) REFERENCES `wf_process_definition` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 793 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流程节点表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wf_process_node
-- ----------------------------
INSERT INTO `wf_process_node` VALUES (8, 2, 'UPLOAD_G1_CHEM', '化学部执行人上传', 'UPLOAD', 'DEPT_HEAD', '1', 'NONE', 1, NULL, 50, 60, 1, '2026-07-10 02:44:00.301', '2026-07-10 02:44:00.301');
INSERT INTO `wf_process_node` VALUES (9, 2, 'UPLOAD_G1_INFO', '资讯部执行人上传', 'UPLOAD', 'DEPT_HEAD', '4', 'NONE', 1, NULL, 50, 120, 1, '2026-07-10 02:44:00.301', '2026-07-10 02:44:00.301');
INSERT INTO `wf_process_node` VALUES (10, 2, 'DEPT_HEAD_G1_CHEM', '化学部负责人审批', 'DEPT_HEAD_APPROVE', 'DEPT_HEAD', '1', 'APPROVE_REJECT', 0, NULL, 200, 60, 2, '2026-07-10 02:44:00.301', '2026-07-10 02:44:00.301');
INSERT INTO `wf_process_node` VALUES (11, 2, 'DEPT_HEAD_G1_INFO', '资讯部负责人审批', 'DEPT_HEAD_APPROVE', 'DEPT_HEAD', '4', 'APPROVE_REJECT', 0, NULL, 200, 120, 2, '2026-07-10 02:44:00.301', '2026-07-10 02:44:00.301');
INSERT INTO `wf_process_node` VALUES (12, 2, 'PM_TECH_G1', 'PM技术初评', 'ROLE_APPROVE', 'ROLE_PM', NULL, 'APPROVE_REJECT', 0, NULL, 350, 90, 3, '2026-07-10 02:44:00.301', '2026-07-10 02:44:00.301');
INSERT INTO `wf_process_node` VALUES (13, 2, 'COMPLIANCE_G1', '药政合规部意见', 'ROLE_APPROVE', 'DEPT_HEAD', '7', 'APPROVE_REJECT', 0, NULL, 500, 90, 4, '2026-07-10 02:44:00.301', '2026-07-27 00:12:39.515');
INSERT INTO `wf_process_node` VALUES (14, 2, 'PM_INTERNAL_G1', 'PM项目组内部评审', 'DECISION', 'ROLE_PM', NULL, 'GO_NO_GO', 0, NULL, 650, 90, 5, '2026-07-10 02:44:00.301', '2026-07-10 02:44:00.301');
INSERT INTO `wf_process_node` VALUES (15, 3, 'UPLOAD_G2', '新药化学部执行人上传', 'UPLOAD', 'DEPT_HEAD', '1', 'NONE', 1, NULL, 50, 80, 1, '2026-07-10 02:44:00.326', '2026-07-10 02:44:00.326');
INSERT INTO `wf_process_node` VALUES (16, 3, 'DEPT_HEAD_G2', '新药化学部负责人审批', 'DEPT_HEAD_APPROVE', 'DEPT_HEAD', '1', 'APPROVE_REJECT', 0, NULL, 200, 80, 2, '2026-07-10 02:44:00.326', '2026-07-10 02:44:00.326');
INSERT INTO `wf_process_node` VALUES (17, 3, 'PM_TECH_G2', 'PM技术初评', 'ROLE_APPROVE', 'ROLE_PM', NULL, 'APPROVE_REJECT', 0, NULL, 350, 80, 3, '2026-07-10 02:44:00.327', '2026-07-10 02:44:00.327');
INSERT INTO `wf_process_node` VALUES (18, 3, 'COMPLIANCE_G2', '药政合规部意见', 'ROLE_APPROVE', 'DEPT_HEAD', '7', 'APPROVE_REJECT', 0, NULL, 500, 80, 4, '2026-07-10 02:44:00.327', '2026-07-27 00:12:39.515');
INSERT INTO `wf_process_node` VALUES (19, 3, 'PM_INTERNAL_G2', 'PM项目组内部评审', 'DECISION', 'ROLE_PM', NULL, 'GO_NO_GO', 0, NULL, 650, 90, 5, '2026-07-10 02:44:00.327', '2026-07-10 02:44:00.327');
INSERT INTO `wf_process_node` VALUES (40, 7, 'UPLOAD_G6', '新药临床部执行人上传', 'UPLOAD', 'DEPT_HEAD', '3', 'NONE', 1, NULL, 50, 80, 1, '2026-07-10 02:44:00.338', '2026-07-10 02:44:00.338');
INSERT INTO `wf_process_node` VALUES (41, 7, 'DEPT_HEAD_G6', '新药临床部负责人审批', 'DEPT_HEAD_APPROVE', 'DEPT_HEAD', '3', 'APPROVE_REJECT', 0, NULL, 200, 80, 2, '2026-07-10 02:44:00.339', '2026-07-10 02:44:00.339');
INSERT INTO `wf_process_node` VALUES (42, 7, 'PM_TECH_G6', 'PM技术初评', 'ROLE_APPROVE', 'ROLE_PM', NULL, 'APPROVE_REJECT', 0, NULL, 350, 80, 3, '2026-07-10 02:44:00.339', '2026-07-10 02:44:00.339');
INSERT INTO `wf_process_node` VALUES (43, 7, 'COMPLIANCE_G6', '药政合规部意见', 'ROLE_APPROVE', 'DEPT_HEAD', '7', 'APPROVE_REJECT', 0, NULL, 500, 80, 4, '2026-07-10 02:44:00.339', '2026-07-27 00:12:39.515');
INSERT INTO `wf_process_node` VALUES (44, 7, 'PMC_DEC_G6_1', 'PMC决策-1', 'DECISION', 'ROLE_PMC', NULL, 'GO_NO_GO', 0, NULL, 650, 60, 5, '2026-07-10 02:44:00.339', '2026-07-10 02:44:00.339');
INSERT INTO `wf_process_node` VALUES (45, 7, 'PMC_DEC_G6_2', 'PMC决策-2', 'DECISION', 'ROLE_PMC', NULL, 'GO_NO_GO', 0, NULL, 650, 100, 5, '2026-07-10 02:44:00.339', '2026-07-10 02:44:00.339');
INSERT INTO `wf_process_node` VALUES (46, 7, 'PMC_DEC_G6_3', 'PMC决策-3', 'DECISION', 'ROLE_PMC', NULL, 'GO_NO_GO', 0, NULL, 650, 140, 5, '2026-07-10 02:44:00.339', '2026-07-10 02:44:00.339');
INSERT INTO `wf_process_node` VALUES (47, 8, 'UPLOAD_G7', '新药临床部执行人上传', 'UPLOAD', 'DEPT_HEAD', '3', 'NONE', 1, NULL, 50, 80, 1, '2026-07-10 02:44:00.341', '2026-07-10 02:44:00.341');
INSERT INTO `wf_process_node` VALUES (48, 8, 'DEPT_HEAD_G7', '新药临床部负责人审批', 'DEPT_HEAD_APPROVE', 'DEPT_HEAD', '3', 'APPROVE_REJECT', 0, NULL, 200, 80, 2, '2026-07-10 02:44:00.341', '2026-07-10 02:44:00.341');
INSERT INTO `wf_process_node` VALUES (49, 8, 'PM_TECH_G7', 'PM技术初评', 'ROLE_APPROVE', 'ROLE_PM', NULL, 'APPROVE_REJECT', 0, NULL, 350, 80, 3, '2026-07-10 02:44:00.341', '2026-07-10 02:44:00.341');
INSERT INTO `wf_process_node` VALUES (50, 8, 'COMPLIANCE_G7', '药政合规部意见', 'ROLE_APPROVE', 'DEPT_HEAD', '7', 'APPROVE_REJECT', 0, NULL, 500, 80, 4, '2026-07-10 02:44:00.342', '2026-07-27 00:12:39.515');
INSERT INTO `wf_process_node` VALUES (51, 8, 'PMC_DEC_G7_1', 'PMC决策-1', 'DECISION', 'ROLE_PMC', NULL, 'GO_NO_GO', 0, NULL, 650, 60, 5, '2026-07-10 02:44:00.342', '2026-07-10 02:44:00.342');
INSERT INTO `wf_process_node` VALUES (52, 8, 'PMC_DEC_G7_2', 'PMC决策-2', 'DECISION', 'ROLE_PMC', NULL, 'GO_NO_GO', 0, NULL, 650, 100, 5, '2026-07-10 02:44:00.342', '2026-07-10 02:44:00.342');
INSERT INTO `wf_process_node` VALUES (53, 8, 'PMC_DEC_G7_3', 'PMC决策-3', 'DECISION', 'ROLE_PMC', NULL, 'GO_NO_GO', 0, NULL, 650, 140, 5, '2026-07-10 02:44:00.342', '2026-07-10 02:44:00.342');
INSERT INTO `wf_process_node` VALUES (54, 9, 'UPLOAD_G8', '新药临床部执行人上传', 'UPLOAD', 'DEPT_HEAD', '3', 'NONE', 1, NULL, 50, 80, 1, '2026-07-10 02:44:00.345', '2026-07-10 02:44:00.345');
INSERT INTO `wf_process_node` VALUES (55, 9, 'DEPT_HEAD_G8', '新药临床部负责人审批', 'DEPT_HEAD_APPROVE', 'DEPT_HEAD', '3', 'APPROVE_REJECT', 0, NULL, 200, 80, 2, '2026-07-10 02:44:00.345', '2026-07-10 02:44:00.345');
INSERT INTO `wf_process_node` VALUES (56, 9, 'PM_TECH_G8', 'PM技术初评', 'ROLE_APPROVE', 'ROLE_PM', NULL, 'APPROVE_REJECT', 0, NULL, 350, 80, 3, '2026-07-10 02:44:00.345', '2026-07-10 02:44:00.345');
INSERT INTO `wf_process_node` VALUES (57, 9, 'COMPLIANCE_G8', '药政合规部意见', 'ROLE_APPROVE', 'DEPT_HEAD', '7', 'APPROVE_REJECT', 0, NULL, 500, 80, 4, '2026-07-10 02:44:00.346', '2026-07-27 00:12:39.515');
INSERT INTO `wf_process_node` VALUES (58, 9, 'PMC_DEC_G8_1', 'PMC决策-1', 'DECISION', 'ROLE_PMC', NULL, 'GO_NO_GO', 0, NULL, 650, 60, 5, '2026-07-10 02:44:00.347', '2026-07-10 02:44:00.347');
INSERT INTO `wf_process_node` VALUES (59, 9, 'PMC_DEC_G8_2', 'PMC决策-2', 'DECISION', 'ROLE_PMC', NULL, 'GO_NO_GO', 0, NULL, 650, 100, 5, '2026-07-10 02:44:00.347', '2026-07-10 02:44:00.347');
INSERT INTO `wf_process_node` VALUES (60, 9, 'PMC_DEC_G8_3', 'PMC决策-3', 'DECISION', 'ROLE_PMC', NULL, 'GO_NO_GO', 0, NULL, 650, 140, 5, '2026-07-10 02:44:00.347', '2026-07-10 02:44:00.347');
INSERT INTO `wf_process_node` VALUES (61, 10, 'UPLOAD_G9', '药政合规部执行人上传', 'UPLOAD', 'DEPT_HEAD', '7', 'NONE', 1, NULL, 50, 80, 1, '2026-07-10 02:44:00.350', '2026-07-10 02:44:00.350');
INSERT INTO `wf_process_node` VALUES (62, 10, 'DEPT_HEAD_G9', '药政合规部负责人审批', 'DEPT_HEAD_APPROVE', 'DEPT_HEAD', '7', 'APPROVE_REJECT', 0, NULL, 200, 80, 2, '2026-07-10 02:44:00.350', '2026-07-10 02:44:00.350');
INSERT INTO `wf_process_node` VALUES (63, 10, 'PM_TECH_G9', 'PM技术初评', 'ROLE_APPROVE', 'ROLE_PM', NULL, 'APPROVE_REJECT', 0, NULL, 350, 80, 3, '2026-07-10 02:44:00.350', '2026-07-10 02:44:00.350');
INSERT INTO `wf_process_node` VALUES (64, 10, 'PMC_DEC_G9_1', 'PMC决策-1', 'DECISION', 'ROLE_PMC', NULL, 'GO_NO_GO', 0, NULL, 650, 60, 5, '2026-07-10 02:44:00.351', '2026-07-10 02:44:00.351');
INSERT INTO `wf_process_node` VALUES (65, 10, 'PMC_DEC_G9_2', 'PMC决策-2', 'DECISION', 'ROLE_PMC', NULL, 'GO_NO_GO', 0, NULL, 650, 100, 5, '2026-07-10 02:44:00.351', '2026-07-10 02:44:00.351');
INSERT INTO `wf_process_node` VALUES (66, 10, 'PMC_DEC_G9_3', 'PMC决策-3', 'DECISION', 'ROLE_PMC', NULL, 'GO_NO_GO', 0, NULL, 650, 140, 5, '2026-07-10 02:44:00.351', '2026-07-10 02:44:00.351');
INSERT INTO `wf_process_node` VALUES (67, 11, 'CHANGE_EFF_APPROVE', '效率管理部审批', 'DEPT_HEAD_APPROVE', 'DEPT_HEAD', '6', 'APPROVE_REJECT', 0, NULL, 200, 80, 1, '2026-07-10 02:44:00.369', '2026-07-10 02:44:00.369');
INSERT INTO `wf_process_node` VALUES (68, 11, 'CHANGE_PMC_APPROVE', 'PMC审批', 'DECISION', 'ROLE_PMC', NULL, 'APPROVE_REJECT', 0, NULL, 400, 80, 2, '2026-07-10 02:44:00.369', '2026-07-10 02:44:00.369');
INSERT INTO `wf_process_node` VALUES (69, 12, 'TERM_EFF_APPROVE', '效率管理部审批', 'DEPT_HEAD_APPROVE', 'DEPT_HEAD', '6', 'APPROVE_REJECT', 0, NULL, 200, 80, 1, '2026-07-10 02:44:00.372', '2026-07-10 02:44:00.372');
INSERT INTO `wf_process_node` VALUES (70, 12, 'TERM_PMC_APPROVE', 'PMC审批', 'DECISION', 'ROLE_PMC', NULL, 'APPROVE_REJECT', 0, NULL, 400, 80, 2, '2026-07-10 02:44:00.372', '2026-07-10 02:44:00.372');
INSERT INTO `wf_process_node` VALUES (71, 12, 'TERM_PM_COMPLETE', 'PM完成终止任务', 'ROLE_APPROVE', 'ROLE_PM', NULL, 'NONE', 0, NULL, 600, 80, 3, '2026-07-10 02:44:00.372', '2026-07-10 02:44:00.372');
INSERT INTO `wf_process_node` VALUES (761, 4, 'UPLOAD_G3', '新药化学部执行人上传', 'UPLOAD', 'DEPT_HEAD', '1', 'NONE', 1, NULL, 50, 80, 1, '2026-07-28 07:57:49.464', '2026-07-28 07:57:49.464');
INSERT INTO `wf_process_node` VALUES (762, 4, 'DEPT_HEAD_G3', '新药化学部负责人审批', 'DEPT_HEAD_APPROVE', 'DEPT_HEAD', '1', 'APPROVE_REJECT', 0, NULL, 200, 80, 2, '2026-07-28 07:57:49.464', '2026-07-28 07:57:49.464');
INSERT INTO `wf_process_node` VALUES (763, 4, 'PM_TECH_G3', 'PM技术初评', 'ROLE_APPROVE', 'ROLE_PM', NULL, 'APPROVE_REJECT', 0, NULL, 350, 80, 3, '2026-07-28 07:57:49.464', '2026-07-28 07:57:49.464');
INSERT INTO `wf_process_node` VALUES (764, 4, 'COMPLIANCE_G3', '药政合规部意见', 'ROLE_APPROVE', 'DEPT_HEAD', '7', 'APPROVE_REJECT', 0, NULL, 500, 80, 4, '2026-07-28 07:57:49.464', '2026-07-28 07:57:49.464');
INSERT INTO `wf_process_node` VALUES (765, 4, 'PMC_DEC_G3_3', 'PMC决策', 'DECISION', 'ROLE_PMC', NULL, 'GO_NO_GO', 0, NULL, 664, 92, 5, '2026-07-28 07:57:49.464', '2026-07-28 07:57:49.464');
INSERT INTO `wf_process_node` VALUES (775, 6, 'UPLOAD_G5', '药政合规部执行人上传', 'UPLOAD', 'DEPT_HEAD', '7', 'NONE', 1, NULL, 50, 80, 1, '2026-07-28 08:10:51.198', '2026-07-28 08:10:51.198');
INSERT INTO `wf_process_node` VALUES (776, 6, 'DEPT_HEAD_G5', '药政合规部负责人审批', 'DEPT_HEAD_APPROVE', 'DEPT_HEAD', '7', 'APPROVE_REJECT', 0, NULL, 200, 80, 2, '2026-07-28 08:10:51.198', '2026-07-28 08:10:51.198');
INSERT INTO `wf_process_node` VALUES (777, 6, 'PM_TECH_G5', 'PM技术初评', 'ROLE_APPROVE', 'ROLE_PM', NULL, 'APPROVE_REJECT', 0, NULL, 350, 80, 3, '2026-07-28 08:10:51.198', '2026-07-28 08:10:51.198');
INSERT INTO `wf_process_node` VALUES (778, 6, 'PMC_DEC_G5_1', 'PMC决策', 'DECISION', 'ROLE_PMC', NULL, 'GO_NO_GO', 0, NULL, 564, 81, 5, '2026-07-28 08:10:51.198', '2026-07-28 08:10:51.198');
INSERT INTO `wf_process_node` VALUES (779, 5, 'UPLOAD_G4', '新药化学部执行人上传', 'UPLOAD', 'DEPT_HEAD', '1', 'NONE', 1, NULL, 50, 80, 1, '2026-07-28 08:10:57.963', '2026-07-28 08:10:57.963');
INSERT INTO `wf_process_node` VALUES (780, 5, 'DEPT_HEAD_G4', '新药化学部负责人审批', 'DEPT_HEAD_APPROVE', 'DEPT_HEAD', '1', 'APPROVE_REJECT', 0, NULL, 200, 80, 2, '2026-07-28 08:10:57.963', '2026-07-28 08:10:57.963');
INSERT INTO `wf_process_node` VALUES (781, 5, 'PM_TECH_G4', 'PM技术初评', 'ROLE_APPROVE', 'ROLE_PM', NULL, 'APPROVE_REJECT', 0, NULL, 350, 80, 3, '2026-07-28 08:10:57.963', '2026-07-28 08:10:57.963');
INSERT INTO `wf_process_node` VALUES (782, 5, 'COMPLIANCE_G4', '药政合规部意见', 'ROLE_APPROVE', 'DEPT_HEAD', '7', 'APPROVE_REJECT', 0, NULL, 500, 80, 4, '2026-07-28 08:10:57.963', '2026-07-28 08:10:57.963');
INSERT INTO `wf_process_node` VALUES (783, 5, 'PMC_DEC_G4_1', 'PMC决策', 'DECISION', 'ROLE_PMC', NULL, 'GO_NO_GO', 0, NULL, 659, 90, 5, '2026-07-28 08:10:57.963', '2026-07-28 08:10:57.963');
INSERT INTO `wf_process_node` VALUES (784, 1, 'UPLOAD_G0_INFO', '资讯部执行人上传', 'UPLOAD', 'DEPT_HEAD', '4', 'NONE', 1, NULL, 50, 80, 0, '2026-07-29 14:15:52.021', '2026-07-29 14:15:52.021');
INSERT INTO `wf_process_node` VALUES (785, 1, 'DEPT_HEAD_G0_INFO', '资讯部负责人审批', 'DEPT_HEAD_APPROVE', 'DEPT_HEAD', '4', 'APPROVE_REJECT', 0, NULL, 210, 80, 100, '2026-07-29 14:15:52.021', '2026-07-29 14:15:52.021');
INSERT INTO `wf_process_node` VALUES (786, 1, 'PM_TECH_G0', 'PM技术初评', 'ROLE_APPROVE', 'ROLE_PM', NULL, 'APPROVE_REJECT', 0, NULL, 370, 80, 200, '2026-07-29 14:15:52.021', '2026-07-29 14:15:52.021');
INSERT INTO `wf_process_node` VALUES (787, 1, 'COMPLIANCE_G0', '药政合规部意见', 'ROLE_APPROVE', 'DEPT_HEAD', '7', 'GO_NO_GO', 0, NULL, 530, 80, 300, '2026-07-29 14:15:52.021', '2026-07-29 14:15:52.021');
INSERT INTO `wf_process_node` VALUES (788, 1, 'PMC_DECISION_G0_1', 'PMC决策-1', 'DECISION', 'ROLE_PMC', NULL, 'GO_NO_GO', 0, NULL, 690, 80, 400, '2026-07-29 14:15:52.021', '2026-07-29 14:15:52.021');
INSERT INTO `wf_process_node` VALUES (791, 13, 'BUDGET_EFF_APPROVE', '效率管理部负责人审批', 'DEPT_HEAD_APPROVE', 'DEPT_HEAD', '6', 'APPROVE_REJECT', 0, NULL, 220, 80, 1, '2026-07-30 08:11:28.454', '2026-07-30 08:11:28.454');
INSERT INTO `wf_process_node` VALUES (792, 13, 'BUDGET_PMC_APPROVE', 'PMC审批', 'DECISION', 'ROLE_PMC', NULL, 'APPROVE_REJECT', 0, NULL, 420, 80, 2, '2026-07-30 08:11:28.454', '2026-07-30 08:11:28.454');

-- ----------------------------
-- Table structure for wf_task
-- ----------------------------
DROP TABLE IF EXISTS `wf_task`;
CREATE TABLE `wf_task`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `instance_id` bigint UNSIGNED NOT NULL,
  `node_id` bigint UNSIGNED NOT NULL,
  `task_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `assignee_user_id` bigint UNSIGNED NULL DEFAULT NULL,
  `status` enum('PENDING','APPROVED','REJECTED','CANCELLED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING',
  `decided_at` datetime(3) NULL DEFAULT NULL,
  `decision_notes` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_wf_task_instance`(`instance_id` ASC) USING BTREE,
  INDEX `idx_wf_task_assignee`(`assignee_user_id` ASC, `status` ASC) USING BTREE,
  INDEX `fk_wf_task_node`(`node_id` ASC) USING BTREE,
  CONSTRAINT `fk_wf_task_assignee` FOREIGN KEY (`assignee_user_id`) REFERENCES `iam_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_wf_task_instance` FOREIGN KEY (`instance_id`) REFERENCES `wf_instance` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_wf_task_node` FOREIGN KEY (`node_id`) REFERENCES `wf_template_node` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wf_task
-- ----------------------------

-- ----------------------------
-- Table structure for wf_template
-- ----------------------------
DROP TABLE IF EXISTS `wf_template`;
CREATE TABLE `wf_template`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `template_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `template_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_wf_template_code`(`template_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wf_template
-- ----------------------------

-- ----------------------------
-- Table structure for wf_template_node
-- ----------------------------
DROP TABLE IF EXISTS `wf_template_node`;
CREATE TABLE `wf_template_node`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `template_id` bigint UNSIGNED NOT NULL,
  `node_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `node_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `node_type` enum('START','APPROVAL','CONDITION','END') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `sort_no` int NOT NULL,
  `approver_mode` enum('USER','ROLE','COMMITTEE') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `approver_ref` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_wf_template_node`(`template_id` ASC, `node_code` ASC) USING BTREE,
  INDEX `idx_wf_template_node_template`(`template_id` ASC) USING BTREE,
  CONSTRAINT `fk_wf_template_node_template` FOREIGN KEY (`template_id`) REFERENCES `wf_template` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wf_template_node
-- ----------------------------

-- ----------------------------
-- View structure for v_pending_review_tasks
-- ----------------------------
DROP VIEW IF EXISTS `v_pending_review_tasks`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY DEFINER VIEW `v_pending_review_tasks` AS select `t`.`id` AS `task_id`,`t`.`review_approval_id` AS `review_approval_id`,`t`.`approver_user_id` AS `approver_user_id`,`t`.`approver_role` AS `approver_role`,`t`.`status` AS `task_status`,`a`.`project_id` AS `project_id`,`a`.`project_milestone_id` AS `project_milestone_id`,`a`.`submitter_user_id` AS `submitter_user_id`,`a`.`submit_comment` AS `submit_comment`,`a`.`status` AS `approval_status`,`a`.`submitted_at` AS `submitted_at`,`a`.`review_type` AS `review_type`,`p`.`project_code` AS `project_code`,`p`.`project_name` AS `project_name`,`p`.`current_milestone_id` AS `current_milestone_id`,`md`.`milestone_code` AS `milestone_code`,`md`.`milestone_name` AS `milestone_name` from ((((`review_approval_task` `t` join `review_approval` `a` on((`t`.`review_approval_id` = `a`.`id`))) join `project` `p` on((`a`.`project_id` = `p`.`id`))) left join `project_milestone` `pm` on((`a`.`project_milestone_id` = `pm`.`id`))) left join `milestone_def` `md` on((`pm`.`milestone_id` = `md`.`id`))) where ((`t`.`status` = 'PENDING') and (`a`.`status` = 'SUBMITTED') and (`p`.`status` in ('ACTIVE','DRAFT'))) order by `a`.`submitted_at` desc;

SET FOREIGN_KEY_CHECKS = 1;
