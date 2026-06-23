-- MySQL dump 10.13  Distrib 8.0.43, for Win64 (x86_64)
--
-- Host: localhost    Database: kbd_pm_system
-- ------------------------------------------------------
-- Server version	8.0.43

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `kbd_pm_system`
--

/*!40000 DROP DATABASE IF EXISTS `kbd_pm_system`*/;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `kbd_pm_system` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `kbd_pm_system`;

--
-- Table structure for table `audit_log`
--

DROP TABLE IF EXISTS `audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL COMMENT '操作人ID',
  `action` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作动作（UPLOAD/DOWNLOAD/DELETE/REVIEW）',
  `document_id` bigint NOT NULL COMMENT '文档ID',
  `timestamp` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `details` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作详情',
  PRIMARY KEY (`id`),
  KEY `idx_document_id` (`document_id`),
  KEY `idx_user_action` (`user_id`,`action`),
  KEY `idx_timestamp` (`timestamp`),
  CONSTRAINT `fk_audit_log_document` FOREIGN KEY (`document_id`) REFERENCES `document` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_audit_log_user` FOREIGN KEY (`user_id`) REFERENCES `iam_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档审计日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_log`
--

LOCK TABLES `audit_log` WRITE;
/*!40000 ALTER TABLE `audit_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `audit_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `budget_limit`
--

DROP TABLE IF EXISTS `budget_limit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `budget_limit` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `project_id` bigint unsigned NOT NULL,
  `milestone_code` varchar(4) NOT NULL,
  `approved_budget` decimal(18,2) NOT NULL DEFAULT '0.00',
  `created_by` bigint unsigned DEFAULT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint unsigned DEFAULT NULL,
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_budget_limit_project_milestone` (`project_id`,`milestone_code`),
  KEY `idx_budget_limit_project` (`project_id`),
  CONSTRAINT `fk_budget_limit_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `ck_budget_limit_budget` CHECK ((`approved_budget` >= 0)),
  CONSTRAINT `ck_budget_limit_milestone` CHECK (regexp_like(`milestone_code`,_utf8mb4'^G[0-9]$'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `budget_limit`
--

LOCK TABLES `budget_limit` WRITE;
/*!40000 ALTER TABLE `budget_limit` DISABLE KEYS */;
/*!40000 ALTER TABLE `budget_limit` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `document`
--

DROP TABLE IF EXISTS `document`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `document` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file_name` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件名',
  `storage_path` varchar(1024) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '存储路径',
  `file_type` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件类型',
  `project_id` bigint unsigned NOT NULL COMMENT '所属项目ID',
  `milestone_phase` enum('G0','G1','G2','G3','G4','G5','G6','G7','G8','G9') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '里程碑阶段',
  `uploader` bigint unsigned NOT NULL COMMENT '上传人ID',
  `compliance_status` enum('PENDING','APPROVED','REJECTED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '合规审核状态',
  `is_locked` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否锁定（归档后锁定）',
  `uploaded_at` datetime NOT NULL COMMENT '上传时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_project_phase` (`project_id`,`milestone_phase`),
  KEY `idx_compliance_status` (`compliance_status`),
  KEY `idx_uploader` (`uploader`),
  CONSTRAINT `fk_document_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_document_uploader` FOREIGN KEY (`uploader`) REFERENCES `iam_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `document`
--

LOCK TABLES `document` WRITE;
/*!40000 ALTER TABLE `document` DISABLE KEYS */;
/*!40000 ALTER TABLE `document` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `governance_committee`
--

DROP TABLE IF EXISTS `governance_committee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `governance_committee` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `committee_code` varchar(32) NOT NULL,
  `committee_name` varchar(128) NOT NULL,
  `chair_user_id` bigint unsigned DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_governance_committee_code` (`committee_code`),
  KEY `idx_governance_committee_chair` (`chair_user_id`),
  CONSTRAINT `fk_governance_committee_chair` FOREIGN KEY (`chair_user_id`) REFERENCES `iam_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `governance_committee`
--

LOCK TABLES `governance_committee` WRITE;
/*!40000 ALTER TABLE `governance_committee` DISABLE KEYS */;
/*!40000 ALTER TABLE `governance_committee` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `governance_committee_member`
--

DROP TABLE IF EXISTS `governance_committee_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `governance_committee_member` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `committee_id` bigint unsigned NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `member_role` enum('CHAIR','MEMBER','SECRETARY','OBSERVER') NOT NULL DEFAULT 'MEMBER',
  `effective_from` date NOT NULL,
  `effective_to` date DEFAULT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_committee_member_active` (`committee_id`,`user_id`,`effective_from`),
  KEY `idx_committee_member_user` (`user_id`),
  CONSTRAINT `fk_committee_member_committee` FOREIGN KEY (`committee_id`) REFERENCES `governance_committee` (`id`),
  CONSTRAINT `fk_committee_member_user` FOREIGN KEY (`user_id`) REFERENCES `iam_user` (`id`),
  CONSTRAINT `ck_committee_member_dates` CHECK (((`effective_to` is null) or (`effective_to` >= `effective_from`)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `governance_committee_member`
--

LOCK TABLES `governance_committee_member` WRITE;
/*!40000 ALTER TABLE `governance_committee_member` DISABLE KEYS */;
/*!40000 ALTER TABLE `governance_committee_member` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `iam_user`
--

DROP TABLE IF EXISTS `iam_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iam_user` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_no` varchar(32) NOT NULL,
  `display_name` varchar(64) NOT NULL,
  `email` varchar(128) DEFAULT NULL,
  `dept_id` bigint unsigned DEFAULT NULL,
  `title` varchar(64) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iam_user_no` (`user_no`),
  UNIQUE KEY `uk_iam_user_email` (`email`),
  KEY `idx_iam_user_dept` (`dept_id`),
  CONSTRAINT `fk_iam_user_dept` FOREIGN KEY (`dept_id`) REFERENCES `org_department` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `iam_user`
--

LOCK TABLES `iam_user` WRITE;
/*!40000 ALTER TABLE `iam_user` DISABLE KEYS */;
INSERT INTO `iam_user` VALUES (1,'pmc_user','pmc_user','pmc@example.com',NULL,NULL,1,'2026-04-27 05:58:56.286','2026-05-20 02:21:26.000'),(2,'pm_user','pm_user','pm@example.com',NULL,NULL,1,'2026-04-27 05:58:56.286','2026-05-20 02:21:26.000'),(3,'dept_head','dept_head','dept@example.com',NULL,NULL,1,'2026-04-27 05:58:56.286','2026-05-20 02:21:26.000'),(4,'efficiency_user','efficiency_user','efficiency@example.com',NULL,NULL,1,'2026-04-27 05:58:56.286','2026-05-20 02:21:26.000'),(5,'compliance_user','compliance_user','compliance@example.com',NULL,NULL,1,'2026-04-27 05:58:56.286','2026-05-20 02:21:26.000'),(6,'admin_user','admin_user','admin@example.com',10,NULL,1,'2026-04-27 05:58:56.286','2026-05-27 19:52:45.160');
/*!40000 ALTER TABLE `iam_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `initiation_approval`
--

DROP TABLE IF EXISTS `initiation_approval`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `initiation_approval` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `project_id` bigint unsigned NOT NULL COMMENT '椤圭洰ID',
  `submitter_user_id` bigint unsigned DEFAULT NULL COMMENT '鎻愪氦浜猴紙椤圭洰缁忕悊锛',
  `application_content` text COMMENT '绔嬮」鐢宠?鍐呭?',
  `status` varchar(32) NOT NULL DEFAULT 'SUBMITTED' COMMENT '鐘舵?锛歋UBMITTED/APPROVED/REJECTED',
  `submitted_at` datetime(3) DEFAULT NULL COMMENT '鎻愪氦鏃堕棿',
  `finished_at` datetime(3) DEFAULT NULL COMMENT '瀹屾垚鏃堕棿锛堝叏閮ㄥ?鎵瑰畬鎴愶級',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_ia_project` (`project_id`),
  KEY `idx_ia_status` (`status`),
  KEY `fk_ia_submitter` (`submitter_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `initiation_approval`
--

LOCK TABLES `initiation_approval` WRITE;
/*!40000 ALTER TABLE `initiation_approval` DISABLE KEYS */;
INSERT INTO `initiation_approval` VALUES (2,10,2,'测试立项申请内容','SUBMITTED','2026-05-26 23:39:54.056',NULL,'2026-05-26 23:39:54.056','2026-05-26 23:39:54.056'),(3,11,6,'测试','SUBMITTED','2026-05-27 19:44:28.405',NULL,'2026-05-27 19:44:28.405','2026-05-27 19:44:28.405'),(4,12,2,'Request PMC initiation review meeting and approval.','SUBMITTED','2026-05-28 06:58:55.869',NULL,'2026-05-27 22:58:55.869','2026-05-27 22:58:55.869');
/*!40000 ALTER TABLE `initiation_approval` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `initiation_approval_task`
--

DROP TABLE IF EXISTS `initiation_approval_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `initiation_approval_task` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `initiation_approval_id` bigint unsigned NOT NULL COMMENT '绔嬮」鐢宠?瀹℃壒璁板綍ID',
  `approver_user_id` bigint unsigned NOT NULL COMMENT '瀹℃壒浜虹敤鎴稩D锛圥MC鎴愬憳锛',
  `approver_role` varchar(64) DEFAULT NULL COMMENT '瀹℃壒浜鸿?鑹',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '瀹℃壒椤哄簭',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '鐘舵?锛歅ENDING/APPROVED/REJECTED',
  `decision` varchar(32) DEFAULT NULL COMMENT '鍐崇瓥锛欰PPROVED/REJECTED',
  `opinion` text COMMENT '瀹℃壒鎰忚?',
  `decided_at` datetime(3) DEFAULT NULL COMMENT '鍐崇瓥鏃堕棿',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_iat_approval` (`initiation_approval_id`),
  KEY `idx_iat_approver` (`approver_user_id`,`status`),
  CONSTRAINT `fk_iat_approval` FOREIGN KEY (`initiation_approval_id`) REFERENCES `initiation_approval` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `initiation_approval_task`
--

LOCK TABLES `initiation_approval_task` WRITE;
/*!40000 ALTER TABLE `initiation_approval_task` DISABLE KEYS */;
INSERT INTO `initiation_approval_task` VALUES (1,3,1,'ROLE_PMC',1,'PENDING',NULL,NULL,NULL,'2026-05-27 22:52:55.478','2026-05-27 22:52:55.478'),(2,4,1,'ROLE_PMC',1,'PENDING',NULL,NULL,NULL,'2026-05-27 22:58:55.874','2026-05-27 22:58:55.874'),(3,4,6,'ROLE_PMC',2,'APPROVED','APPROVED','同意','2026-05-28 00:49:37.659','2026-05-27 22:58:55.874','2026-05-28 00:49:37.659');
/*!40000 ALTER TABLE `initiation_approval_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `milestone_def`
--

DROP TABLE IF EXISTS `milestone_def`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `milestone_def` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `milestone_code` varchar(4) NOT NULL,
  `milestone_name` varchar(128) NOT NULL,
  `stage_definition` tinytext NOT NULL,
  `core_deliverables` tinytext NOT NULL,
  `lead_dept_text` varchar(256) NOT NULL,
  `decision_gate` varchar(128) NOT NULL,
  `sort_no` int NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_milestone_def_code` (`milestone_code`),
  CONSTRAINT `ck_milestone_def_code` CHECK (regexp_like(`milestone_code`,_utf8mb4'^G[0-9]$'))
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `milestone_def`
--

LOCK TABLES `milestone_def` WRITE;
/*!40000 ALTER TABLE `milestone_def` DISABLE KEYS */;
INSERT INTO `milestone_def` VALUES (1,'G0','项目立项','完成靶点评估、立项申请','立项报告','新药资讯部','PMC立项决策',0,1,'2026-04-23 09:19:50.262','2026-04-23 09:19:50.262'),(2,'G1','先导化合物确认','获得具有明确活性的先导化合物系列','先导化合物、专利申请号','新药化学部','项目组内部评审',1,1,'2026-04-23 09:19:50.262','2026-04-23 09:19:50.262'),(3,'G2','优选化合物','获得具有明确体内药效的优选化合物','优选化合物、专利申请号','新药化学部','项目组内部评审',2,1,'2026-04-23 09:19:50.262','2026-04-23 09:19:50.262'),(4,'G3','候选化合物提名 (PCC)','综合评估后，正式提名一个或多个化合物作为临床前开发候选物','PCC提名报告（含体内外药效、初步ADME、初步安全性、专利策略）','新药化学部','PMC评审',3,1,'2026-04-23 09:19:50.262','2026-04-23 09:19:50.262'),(5,'G4','临床前开发完成 (GLP)','完成所有GLP毒理研究、药效及药代动力学研究，具备申请IND条件','GLP毒理报告、药效总结报告、CMC初步总结报告、专利FTO报告','新药生物部/新药化学部','PMC评审',4,1,'2026-04-23 09:19:50.262','2026-04-23 09:19:50.262'),(6,'G5','临床试验申请获批 (IND)','向监管机构递交IND申请并获批','IND申报资料、受理通知书、临床试验批件/默示许可文件','药政合规部','PMC评审',5,1,'2026-04-23 09:19:50.262','2026-04-23 09:19:50.262'),(7,'G6','临床Ⅰ期','完成健康受试者或患者的药代动力学、安全性和耐受性研究','Ⅰ期总结报告、Ⅰ期临床试验方案','新药临床部','PMC评审',6,1,'2026-04-23 09:19:50.262','2026-04-23 09:19:50.262'),(8,'G7','临床Ⅱ期','完成在目标患者群体中的初步疗效和安全性验证','Ⅱ期总结报告、Ⅱ期临床试验方案、关键注册策略确认','新药临床部','PMC评审',7,1,'2026-04-23 09:19:50.262','2026-04-23 09:19:50.262'),(9,'G8','临床Ⅲ期','完成关键性注册临床试验','Ⅲ期临床研究报告','新药临床部','PMC评审',8,1,'2026-04-23 09:19:50.262','2026-04-23 09:19:50.262'),(10,'G9','新药上市申请获批 (NDA)','递交NDA并获批上市','NDA申报资料、受理通知书、药品注册证书','药政合规部','PMC结项评审',9,1,'2026-04-23 09:19:50.262','2026-04-23 09:19:50.262');
/*!40000 ALTER TABLE `milestone_def` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `milestone_dept_role`
--

DROP TABLE IF EXISTS `milestone_dept_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `milestone_dept_role` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `milestone_def_id` bigint unsigned NOT NULL COMMENT '里程碑定义ID',
  `dept_id` bigint unsigned NOT NULL COMMENT '部门ID',
  `role_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色类型: DEPT_EXECUTOR(部门执行人) / DEPT_HEAD(部门负责人)',
  `is_active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_milestone_dept_role` (`milestone_def_id`,`dept_id`,`role_type`),
  KEY `idx_milestone_def` (`milestone_def_id`),
  KEY `idx_dept` (`dept_id`),
  CONSTRAINT `fk_milestone_dept_role_dept` FOREIGN KEY (`dept_id`) REFERENCES `org_department` (`id`),
  CONSTRAINT `fk_milestone_dept_role_milestone_def` FOREIGN KEY (`milestone_def_id`) REFERENCES `milestone_def` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='里程碑阶段-部门角色映射表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `milestone_dept_role`
--

LOCK TABLES `milestone_dept_role` WRITE;
/*!40000 ALTER TABLE `milestone_dept_role` DISABLE KEYS */;
/*!40000 ALTER TABLE `milestone_dept_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `milestone_history`
--

DROP TABLE IF EXISTS `milestone_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `milestone_history` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `project_id` bigint unsigned NOT NULL,
  `project_milestone_id` bigint unsigned NOT NULL,
  `action` enum('SUBMIT_REVIEW','DECISION') NOT NULL,
  `from_status` varchar(32) DEFAULT NULL,
  `to_status` varchar(32) DEFAULT NULL,
  `actor_user_id` bigint unsigned DEFAULT NULL,
  `action_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `notes` tinytext,
  `payload_json` json DEFAULT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_mh_project` (`project_id`,`action_at`),
  KEY `idx_mh_pm` (`project_milestone_id`,`action_at`),
  KEY `fk_mh_actor` (`actor_user_id`),
  CONSTRAINT `fk_mh_actor` FOREIGN KEY (`actor_user_id`) REFERENCES `iam_user` (`id`),
  CONSTRAINT `fk_mh_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `fk_mh_project_milestone` FOREIGN KEY (`project_milestone_id`) REFERENCES `project_milestone` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `milestone_history`
--

LOCK TABLES `milestone_history` WRITE;
/*!40000 ALTER TABLE `milestone_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `milestone_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `org_department`
--

DROP TABLE IF EXISTS `org_department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `org_department` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `dept_code` varchar(32) NOT NULL,
  `dept_name` varchar(128) NOT NULL,
  `dept_type` enum('PDT','ROSS','OTHER') NOT NULL DEFAULT 'OTHER',
  `parent_id` bigint unsigned DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `head_user_id` bigint unsigned DEFAULT NULL COMMENT '部门负责人用户ID（关联user表）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_org_department_code` (`dept_code`),
  UNIQUE KEY `uk_dept_code` (`dept_code`),
  KEY `idx_org_department_parent` (`parent_id`),
  CONSTRAINT `fk_org_department_parent` FOREIGN KEY (`parent_id`) REFERENCES `org_department` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `org_department`
--

LOCK TABLES `org_department` WRITE;
/*!40000 ALTER TABLE `org_department` DISABLE KEYS */;
INSERT INTO `org_department` VALUES (1,'PDT_CHEM','新药化学部','PDT',NULL,1,'2026-04-23 09:19:50.305','2026-05-27 23:35:59.214',3),(2,'PDT_BIO','新药生物部','PDT',NULL,1,'2026-04-23 09:19:50.305','2026-04-23 09:19:50.305',NULL),(3,'PDT_CLIN','新药临床部','PDT',NULL,1,'2026-04-23 09:19:50.305','2026-04-23 09:19:50.305',NULL),(4,'ROSS_INFO','新药资讯部','ROSS',NULL,1,'2026-04-23 09:19:50.305','2026-04-23 09:19:50.305',NULL),(5,'ROSS_BD','商务拓展部','ROSS',NULL,1,'2026-04-23 09:19:50.305','2026-04-23 09:19:50.305',NULL),(6,'ROSS_EFF','效率管理部','ROSS',NULL,1,'2026-04-23 09:19:50.305','2026-05-27 23:35:39.626',4),(7,'ROSS_REG','药政合规部','ROSS',NULL,1,'2026-04-23 09:19:50.305','2026-04-23 09:19:50.305',NULL),(9,'SYSTEM','System','OTHER',NULL,1,'2026-05-21 01:12:15.000','2026-05-21 01:12:15.000',NULL),(10,'PMC','项目管理委员会','OTHER',NULL,1,'2026-05-21 01:12:15.000','2026-05-21 01:12:15.000',NULL),(11,'PM','项目经理组','OTHER',NULL,1,'2026-05-21 01:12:15.000','2026-05-27 23:35:05.004',2);
/*!40000 ALTER TABLE `org_department` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `permission`
--

DROP TABLE IF EXISTS `permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permission` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL COMMENT '权限名称',
  `description` varchar(255) DEFAULT NULL COMMENT '权限描述',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  UNIQUE KEY `uk_permission_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `permission`
--

LOCK TABLES `permission` WRITE;
/*!40000 ALTER TABLE `permission` DISABLE KEYS */;
INSERT INTO `permission` VALUES (1,'PERMISSION_SUBMIT_REVIEW','提交里程碑评审','2026-04-27 05:58:56.267','2026-04-27 05:58:56.267'),(2,'PERMISSION_APPROVE_MILESTONE','批准里程碑（PMC Go/No Go决策）','2026-04-27 05:58:56.267','2026-04-27 05:58:56.267'),(3,'PERMISSION_VIEW_MILESTONE','查看里程碑信息','2026-04-27 05:58:56.267','2026-04-27 05:58:56.267'),(4,'PERMISSION_VIEW_BUDGET','查看预算信息','2026-04-27 05:58:56.267','2026-04-27 05:58:56.267'),(5,'PERMISSION_APPROVE_BUDGET','批准预算调整','2026-04-27 05:58:56.267','2026-04-27 05:58:56.267'),(6,'PERMISSION_MANAGE_BUDGET','管理预算计划','2026-04-27 05:58:56.267','2026-04-27 05:58:56.267'),(7,'PERMISSION_CREATE_PROJECT','创建项目','2026-04-27 05:58:56.267','2026-04-27 05:58:56.267'),(8,'PERMISSION_VIEW_PROJECT','查看项目信息','2026-04-27 05:58:56.267','2026-04-27 05:58:56.267'),(9,'PERMISSION_EDIT_PROJECT','编辑项目','2026-04-27 05:58:56.267','2026-04-27 05:58:56.267'),(10,'PERMISSION_TERMINATE_PROJECT','终止项目','2026-04-27 05:58:56.267','2026-04-27 05:58:56.267'),(11,'PERMISSION_UPLOAD_DOCUMENT','上传交付物','2026-04-27 05:58:56.267','2026-04-27 05:58:56.267'),(12,'PERMISSION_VIEW_DOCUMENT','查看交付物','2026-04-27 05:58:56.267','2026-04-27 05:58:56.267'),(13,'PERMISSION_REVIEW_DOCUMENT','审查交付物','2026-04-27 05:58:56.267','2026-04-27 05:58:56.267'),(14,'PERMISSION_SUBMIT_CHANGE_REQUEST','提交变更申请','2026-04-27 05:58:56.267','2026-04-27 05:58:56.267'),(15,'PERMISSION_APPROVE_CHANGE_REQUEST','批准变更申请','2026-04-27 05:58:56.267','2026-04-27 05:58:56.267'),(16,'PERMISSION_VIEW_CHANGE_REQUEST','查看变更申请','2026-04-27 05:58:56.267','2026-04-27 05:58:56.267'),(17,'PERMISSION_MANAGE_USERS','管理用户','2026-04-27 05:58:56.267','2026-04-27 05:58:56.267'),(18,'PERMISSION_MANAGE_ROLES','管理角色','2026-04-27 05:58:56.267','2026-04-27 05:58:56.267'),(19,'PERMISSION_VIEW_REPORTS','查看报表','2026-04-27 05:58:56.267','2026-04-27 05:58:56.267'),(20,'PERMISSION_SYSTEM_MAINTENANCE','系统维护','2026-04-27 05:58:56.267','2026-04-27 05:58:56.267'),(21,'DOCUMENT_UPLOAD','上传文档','2026-04-27 08:01:48.564','2026-04-27 08:01:48.564'),(22,'DOCUMENT_DOWNLOAD','下载文档','2026-04-27 08:01:48.564','2026-04-27 08:01:48.564'),(23,'DOCUMENT_DELETE','删除文档','2026-04-27 08:01:48.564','2026-04-27 08:01:48.564'),(24,'DOCUMENT_REVIEW','审核文档合规性','2026-04-27 08:01:48.564','2026-04-27 08:01:48.564'),(25,'DOCUMENT_VIEW_AUDIT','查看文档审计日志','2026-04-27 08:01:48.564','2026-04-27 08:01:48.564'),(26,'PERMISSION_REVIEW_INITIATION','评审立项申请','2026-05-11 01:20:18.554','2026-05-11 01:20:18.554'),(27,'PERMISSION_APPROVE_INITIATION','审批立项申请','2026-05-11 01:20:18.554','2026-05-11 01:20:18.554'),(28,'PERMISSION_VIEW_REVIEW_RECORD','查看评审记录','2026-05-11 01:20:18.554','2026-05-11 01:20:18.554'),(29,'PERMISSION_DELETE_PROJECT','删除项目','2026-05-13 03:06:03.000','2026-05-13 03:06:03.000');
/*!40000 ALTER TABLE `permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project`
--

DROP TABLE IF EXISTS `project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `project_no` varchar(16) NOT NULL,
  `level_id` bigint unsigned NOT NULL,
  `project_code` varchar(32) NOT NULL,
  `project_name` varchar(256) NOT NULL,
  `target_pathway` varchar(256) DEFAULT NULL,
  `indication` varchar(256) DEFAULT NULL,
  `tpp_summary` tinytext,
  `description` tinytext,
  `mechanism` tinytext,
  `unmet_needs` varchar(512) DEFAULT NULL COMMENT '未满足的临床需求',
  `scientific_basis` tinytext,
  `expected_indication` varchar(256) DEFAULT NULL COMMENT '预期适应症',
  `administration_route` varchar(64) DEFAULT NULL COMMENT '给药途径',
  `dosage_form` varchar(64) DEFAULT NULL COMMENT '剂型',
  `dosage_frequency` varchar(64) DEFAULT NULL COMMENT '剂量频率',
  `efficacy_target` tinytext,
  `safety_advantage` tinytext,
  `differentiation` tinytext,
  `budget_total` decimal(18,2) DEFAULT NULL COMMENT '总预算',
  `budget_to_pcc` decimal(18,2) DEFAULT NULL COMMENT '阶段预算至PCC（万元）',
  `risk_scientific` tinytext COMMENT '科学风险：靶点有效性风险、成药性风险、安全性风险',
  `risk_competitive` tinytext COMMENT '竞争风险：主要竞品进展',
  `risk_regulatory` tinytext COMMENT '注册风险：法规路径不确定性',
  `suggestion_and_support` tinytext COMMENT '建议与所需支持：简述需要PMC提供的资源或决策支持',
  `pm_user_id` bigint unsigned DEFAULT NULL,
  `initiator_user_id` bigint unsigned DEFAULT NULL COMMENT '发起人（立项申请人）',
  `pmc_committee_id` bigint unsigned DEFAULT NULL,
  `process_oversight_dept_id` bigint unsigned DEFAULT NULL COMMENT '流程监管部门（默认：效率管理部 ROSS_EFF）',
  `current_milestone_id` bigint unsigned DEFAULT NULL,
  `status` enum('DRAFT','ACTIVE','PAUSED','TERMINATED','CLOSED') NOT NULL DEFAULT 'DRAFT',
  `review_status` varchar(32) DEFAULT NULL COMMENT '评审状态：PENDING_REVIEW/IN_REVIEW/APPROVED/REJECTED',
  `review_submitted_at` datetime(3) DEFAULT NULL COMMENT '评审提交时间',
  `initiation_status` varchar(32) DEFAULT NULL COMMENT '绔嬮」鐘舵?锛歯ull(鏈?敵璇?/SUBMITTED(宸叉彁浜?/APPROVED(宸查?杩?/REJECTED(宸查┏鍥?',
  `initiation_submitted_at` datetime(3) DEFAULT NULL COMMENT '绔嬮」鐢宠?鎻愪氦鏃堕棿',
  `initiation_application` tinytext COMMENT '立项申请信息（项目经理填写的申请内容）',
  `terminated_reason` tinytext,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `planned_pcc_date` date DEFAULT NULL COMMENT '预估PCC提名日期（对应G0计划日期）',
  `planned_ind_date` date DEFAULT NULL COMMENT '预估IND获批日期（对应G5计划日期）',
  `planned_nda_date` date DEFAULT NULL COMMENT '预估NDA获批日期（对应G9计划日期）',
  `created_by` bigint unsigned DEFAULT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint unsigned DEFAULT NULL,
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `planned_end_date` date DEFAULT NULL COMMENT '预估项目结束日期',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_no` (`project_no`),
  UNIQUE KEY `uk_project_code` (`project_code`),
  KEY `idx_project_level` (`level_id`),
  KEY `idx_project_pm` (`pm_user_id`),
  KEY `idx_project_status` (`status`),
  KEY `idx_project_current_milestone` (`current_milestone_id`),
  KEY `fk_project_pmc` (`pmc_committee_id`),
  KEY `fk_project_created_by` (`created_by`),
  KEY `fk_project_updated_by` (`updated_by`),
  KEY `idx_project_process_oversight_dept` (`process_oversight_dept_id`),
  CONSTRAINT `fk_project_created_by` FOREIGN KEY (`created_by`) REFERENCES `iam_user` (`id`),
  CONSTRAINT `fk_project_current_milestone` FOREIGN KEY (`current_milestone_id`) REFERENCES `milestone_def` (`id`),
  CONSTRAINT `fk_project_level` FOREIGN KEY (`level_id`) REFERENCES `project_level` (`id`),
  CONSTRAINT `fk_project_pm` FOREIGN KEY (`pm_user_id`) REFERENCES `iam_user` (`id`),
  CONSTRAINT `fk_project_pmc` FOREIGN KEY (`pmc_committee_id`) REFERENCES `governance_committee` (`id`),
  CONSTRAINT `fk_project_process_oversight_dept` FOREIGN KEY (`process_oversight_dept_id`) REFERENCES `org_department` (`id`),
  CONSTRAINT `fk_project_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `iam_user` (`id`),
  CONSTRAINT `ck_project_code` CHECK (regexp_like(`project_code`,_utf8mb4'^(H-L|G-L|H-Q|G-Q|G-T|C-L|C-Q)-KBD[0-9]{4,}$')),
  CONSTRAINT `ck_project_dates` CHECK (((`end_date` is null) or (`start_date` is null) or (`end_date` >= `start_date`))),
  CONSTRAINT `ck_project_no` CHECK (regexp_like(`project_no`,_utf8mb4'^KBD[0-9]{4,}$'))
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project`
--

LOCK TABLES `project` WRITE;
/*!40000 ALTER TABLE `project` DISABLE KEYS */;
INSERT INTO `project` VALUES (12,'KBD0007',1,'H-L-KBD0007','KU-101 酪氨酸激酶抑制剂','EGFR/HER2','非小细胞肺癌（NSCLC）','口服小分子TKI，目标ORR≥45%，安全性优于三代药物。','[Demo] G0 initiation - approval in progress (SUBMITTED).',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,8500.00,NULL,NULL,NULL,NULL,NULL,2,2,NULL,6,1,'ACTIVE',NULL,NULL,'SUBMITTED','2026-05-28 06:58:55.777','Request PMC initiation review meeting and approval.',NULL,'2026-01-15',NULL,'2027-06-30','2029-12-31','2032-06-30',2,'2026-05-27 22:58:55.777',2,'2026-05-27 22:58:55.777','2033-12-31'),(13,'KBD0008',4,'G-Q-KBD0008','BS-202 双特异性抗体','PD-1 × CTLA-4','晚期黑色素瘤','双抗免疫联合机制，探索性重大临床前项目。','[Demo] G1 lead compound stage - initiation APPROVED.',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,12000.00,NULL,NULL,NULL,NULL,NULL,2,NULL,NULL,6,2,'ACTIVE',NULL,NULL,'APPROVED','2025-11-01 10:00:00.000',NULL,NULL,'2025-08-01',NULL,'2027-09-30','2030-06-30','2033-03-31',2,'2026-05-27 22:58:55.787',2,'2026-05-27 22:58:55.787',NULL),(14,'KBD0009',2,'G-L-KBD0009','SM-303 小分子抗肿瘤药','KRAS G12C','结直肠癌','口服KRAS抑制剂，瞄准耐药后线治疗空白。','[Demo] G3 PCC nomination - G0-G2 completed with Go.',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,28000.00,NULL,NULL,NULL,NULL,NULL,2,NULL,NULL,6,4,'ACTIVE',NULL,NULL,'APPROVED',NULL,NULL,NULL,'2024-03-01',NULL,'2026-12-31','2028-06-30','2031-12-31',2,'2026-05-27 22:58:55.793',2,'2026-05-27 22:58:55.793',NULL),(15,'KBD0010',3,'H-Q-KBD0010','NA-404 核酸适配体药物','VEGF','湿性年龄相关性黄斑变性','长效眼内给药核酸药物，减少注射频次。','[Demo] G5 IND approved stage - preclinical complete.',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,45000.00,NULL,NULL,NULL,NULL,NULL,2,NULL,NULL,6,6,'ACTIVE',NULL,NULL,'APPROVED',NULL,NULL,NULL,'2022-06-01',NULL,'2025-06-30','2026-05-28','2029-12-31',2,'2026-05-27 22:58:55.800',2,'2026-05-27 22:58:55.800',NULL),(16,'KBD0011',5,'G-T-KBD0011','GT-505 体内基因治疗','CFTR','囊性纤维化','AAV载体基因治疗，重大探索性管线。','[Demo] G7 Phase II clinical trial in progress.',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,62000.00,NULL,NULL,NULL,NULL,NULL,2,NULL,NULL,6,8,'ACTIVE',NULL,NULL,'APPROVED',NULL,NULL,NULL,'2021-01-10',NULL,'2024-12-31','2025-08-15','2028-12-31',2,'2026-05-27 22:58:55.805',2,'2026-05-27 22:58:55.805',NULL),(17,'KBD0012',6,'C-L-KBD0012','CG-606 改良型抗肿瘤生物药','HER2','HER2阳性乳腺癌','产能型临床项目，生物类似物+改良制剂。','[Demo] G9 NDA filing stage - Phase III complete.',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,38000.00,NULL,NULL,NULL,NULL,NULL,2,NULL,NULL,6,10,'ACTIVE',NULL,NULL,'APPROVED',NULL,NULL,NULL,'2020-05-01',NULL,'2023-03-31','2024-09-30','2026-08-31',2,'2026-05-27 22:58:55.809',2,'2026-05-27 22:58:55.809','2027-06-30');
/*!40000 ALTER TABLE `project` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_budget_ledger`
--

DROP TABLE IF EXISTS `project_budget_ledger`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_budget_ledger` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `project_id` bigint unsigned NOT NULL,
  `occurred_on` date NOT NULL,
  `expense_category` enum('INTERNAL','EXTERNAL') NOT NULL,
  `amount` decimal(18,2) NOT NULL,
  `vendor_name` varchar(256) DEFAULT NULL,
  `reference_no` varchar(64) DEFAULT NULL,
  `description` varchar(512) DEFAULT NULL,
  `created_by` bigint unsigned DEFAULT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_budget_ledger_project_date` (`project_id`,`occurred_on`),
  KEY `idx_budget_ledger_project_category` (`project_id`,`expense_category`),
  KEY `fk_budget_ledger_created_by` (`created_by`),
  CONSTRAINT `fk_budget_ledger_created_by` FOREIGN KEY (`created_by`) REFERENCES `iam_user` (`id`),
  CONSTRAINT `fk_budget_ledger_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `ck_budget_ledger_amount` CHECK ((`amount` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_budget_ledger`
--

LOCK TABLES `project_budget_ledger` WRITE;
/*!40000 ALTER TABLE `project_budget_ledger` DISABLE KEYS */;
/*!40000 ALTER TABLE `project_budget_ledger` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_budget_plan`
--

DROP TABLE IF EXISTS `project_budget_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_budget_plan` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `project_id` bigint unsigned NOT NULL,
  `plan_type` enum('LIFECYCLE','ANNUAL','STAGE_ROLLING') NOT NULL,
  `fiscal_year` int DEFAULT NULL,
  `stage_from_milestone_id` bigint unsigned DEFAULT NULL,
  `stage_to_milestone_id` bigint unsigned DEFAULT NULL,
  `version_no` int NOT NULL DEFAULT '1',
  `internal_amount` decimal(18,2) NOT NULL DEFAULT '0.00',
  `external_amount` decimal(18,2) NOT NULL DEFAULT '0.00',
  `total_amount` decimal(18,2) GENERATED ALWAYS AS ((`internal_amount` + `external_amount`)) STORED,
  `approved_status` enum('DRAFT','SUBMITTED','APPROVED','REJECTED') NOT NULL DEFAULT 'DRAFT',
  `approved_at` datetime(3) DEFAULT NULL,
  `approved_by` bigint unsigned DEFAULT NULL,
  `notes` tinytext,
  `created_by` bigint unsigned DEFAULT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_by` bigint unsigned DEFAULT NULL,
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_budget_plan_project` (`project_id`),
  KEY `idx_budget_plan_type_year` (`plan_type`,`fiscal_year`),
  KEY `fk_budget_plan_from` (`stage_from_milestone_id`),
  KEY `fk_budget_plan_to` (`stage_to_milestone_id`),
  KEY `fk_budget_plan_approved_by` (`approved_by`),
  KEY `fk_budget_plan_created_by` (`created_by`),
  KEY `fk_budget_plan_updated_by` (`updated_by`),
  KEY `idx_budget_plan_project_version` (`project_id`,`version_no`),
  CONSTRAINT `fk_budget_plan_approved_by` FOREIGN KEY (`approved_by`) REFERENCES `iam_user` (`id`),
  CONSTRAINT `fk_budget_plan_created_by` FOREIGN KEY (`created_by`) REFERENCES `iam_user` (`id`),
  CONSTRAINT `fk_budget_plan_from` FOREIGN KEY (`stage_from_milestone_id`) REFERENCES `milestone_def` (`id`),
  CONSTRAINT `fk_budget_plan_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `fk_budget_plan_to` FOREIGN KEY (`stage_to_milestone_id`) REFERENCES `milestone_def` (`id`),
  CONSTRAINT `fk_budget_plan_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `iam_user` (`id`),
  CONSTRAINT `ck_budget_plan_amounts` CHECK (((`internal_amount` >= 0) and (`external_amount` >= 0))),
  CONSTRAINT `ck_budget_plan_year` CHECK (((`plan_type` <> _utf8mb4'ANNUAL') or (`fiscal_year` is not null)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_budget_plan`
--

LOCK TABLES `project_budget_plan` WRITE;
/*!40000 ALTER TABLE `project_budget_plan` DISABLE KEYS */;
/*!40000 ALTER TABLE `project_budget_plan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_budget_policy`
--

DROP TABLE IF EXISTS `project_budget_policy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_budget_policy` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `project_id` bigint unsigned NOT NULL,
  `yellow_threshold` decimal(6,4) NOT NULL DEFAULT '0.8000',
  `red_threshold` decimal(6,4) NOT NULL DEFAULT '0.9500',
  `currency_code` varchar(3) NOT NULL DEFAULT 'CNY',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_budget_policy_project` (`project_id`),
  CONSTRAINT `fk_project_budget_policy_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `ck_budget_thresholds` CHECK (((`yellow_threshold` > 0) and (`red_threshold` > 0) and (`red_threshold` > `yellow_threshold`) and (`red_threshold` <= 1.0000)))
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_budget_policy`
--

LOCK TABLES `project_budget_policy` WRITE;
/*!40000 ALTER TABLE `project_budget_policy` DISABLE KEYS */;
INSERT INTO `project_budget_policy` VALUES (12,12,0.8000,0.9500,'CNY','2026-05-27 22:58:55.854','2026-05-27 22:58:55.854'),(13,13,0.8000,0.9500,'CNY','2026-05-27 22:58:55.854','2026-05-27 22:58:55.854'),(14,14,0.8000,0.9500,'CNY','2026-05-27 22:58:55.854','2026-05-27 22:58:55.854'),(15,15,0.8000,0.9500,'CNY','2026-05-27 22:58:55.854','2026-05-27 22:58:55.854'),(16,16,0.8000,0.9500,'CNY','2026-05-27 22:58:55.854','2026-05-27 22:58:55.854'),(17,17,0.8000,0.9500,'CNY','2026-05-27 22:58:55.854','2026-05-27 22:58:55.854');
/*!40000 ALTER TABLE `project_budget_policy` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_budget_snapshot`
--

DROP TABLE IF EXISTS `project_budget_snapshot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_budget_snapshot` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `project_id` bigint unsigned NOT NULL,
  `budget_plan_id` bigint unsigned DEFAULT NULL,
  `snapshot_month` varchar(7) NOT NULL,
  `internal_spent` decimal(18,2) NOT NULL DEFAULT '0.00',
  `external_spent` decimal(18,2) NOT NULL DEFAULT '0.00',
  `total_spent` decimal(18,2) GENERATED ALWAYS AS ((`internal_spent` + `external_spent`)) STORED,
  `planned_total_amount` decimal(18,2) NOT NULL DEFAULT '0.00',
  `utilization_ratio` decimal(10,6) GENERATED ALWAYS AS ((case when (`planned_total_amount` = 0) then 0 else (`total_spent` / `planned_total_amount`) end)) STORED,
  `warning_level` enum('NONE','YELLOW','RED') NOT NULL DEFAULT 'NONE',
  `generated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_budget_snapshot` (`project_id`,`snapshot_month`,`budget_plan_id`),
  KEY `idx_budget_snapshot_project_month` (`project_id`,`snapshot_month`),
  KEY `fk_budget_snapshot_plan` (`budget_plan_id`),
  CONSTRAINT `fk_budget_snapshot_plan` FOREIGN KEY (`budget_plan_id`) REFERENCES `project_budget_plan` (`id`),
  CONSTRAINT `fk_budget_snapshot_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `ck_budget_snapshot_amounts` CHECK (((`internal_spent` >= 0) and (`external_spent` >= 0) and (`planned_total_amount` >= 0))),
  CONSTRAINT `ck_budget_snapshot_month` CHECK (regexp_like(`snapshot_month`,_utf8mb4'^[0-9]{4}-[0-9]{2}$'))
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_budget_snapshot`
--

LOCK TABLES `project_budget_snapshot` WRITE;
/*!40000 ALTER TABLE `project_budget_snapshot` DISABLE KEYS */;
INSERT INTO `project_budget_snapshot` (`id`, `project_id`, `budget_plan_id`, `snapshot_month`, `internal_spent`, `external_spent`, `planned_total_amount`, `warning_level`, `generated_at`) VALUES (1,12,NULL,'2026-05',1700.00,425.00,8500.00,'NONE','2026-05-28 06:58:55.859'),(2,13,NULL,'2026-05',3000.00,600.00,12000.00,'NONE','2026-05-28 06:58:55.859'),(3,14,NULL,'2026-05',8400.00,1400.00,28000.00,'NONE','2026-05-28 06:58:55.859'),(4,15,NULL,'2026-05',4500.00,2250.00,45000.00,'NONE','2026-05-28 06:58:55.859'),(5,16,NULL,'2026-05',9300.00,3100.00,62000.00,'NONE','2026-05-28 06:58:55.859'),(6,17,NULL,'2026-05',7600.00,1900.00,38000.00,'NONE','2026-05-28 06:58:55.859');
/*!40000 ALTER TABLE `project_budget_snapshot` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_change_request`
--

DROP TABLE IF EXISTS `project_change_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_change_request` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `project_id` bigint unsigned NOT NULL,
  `change_type` enum('OBJECTIVE_SCOPE','MILESTONE_SCHEDULE','BUDGET','OWNER_PM','PAUSE_TERMINATE','OTHER') NOT NULL,
  `reason_text` tinytext NOT NULL,
  `before_text` tinytext,
  `after_text` tinytext,
  `impact_milestone_text` tinytext,
  `impact_budget_text` tinytext,
  `impact_resource_text` tinytext,
  `requested_by` bigint unsigned DEFAULT NULL,
  `requested_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `status` enum('DRAFT','SUBMITTED','APPROVED','REJECTED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
  `wf_instance_id` bigint unsigned DEFAULT NULL,
  `pmc_decision` enum('APPROVE','REJECT','CONDITIONAL_APPROVE') DEFAULT NULL,
  `pmc_decision_text` tinytext,
  `pmc_decided_at` datetime(3) DEFAULT NULL,
  `pmc_decided_by` bigint unsigned DEFAULT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `target_milestone_id` bigint unsigned DEFAULT NULL COMMENT '目标里程碑ID（里程碑调整时使用）',
  `target_milestone_planned_date` date DEFAULT NULL COMMENT '目标里程碑新计划日期',
  `previous_budget_amount` decimal(18,2) DEFAULT NULL COMMENT '变更前预算金额',
  `requested_budget_amount` decimal(18,2) DEFAULT NULL COMMENT '申请预算金额',
  `new_pm_user_id` bigint unsigned DEFAULT NULL COMMENT '新PM用户ID（负责人变更时使用）',
  `asset_disposal_confirmed` tinyint(1) DEFAULT '0' COMMENT '资产处置确认（终止时使用）',
  `archive_confirmed` tinyint(1) DEFAULT '0' COMMENT '归档确认（终止时使用）',
  PRIMARY KEY (`id`),
  KEY `idx_change_project` (`project_id`,`requested_at`),
  KEY `idx_change_status` (`status`),
  KEY `fk_change_requested_by` (`requested_by`),
  KEY `fk_change_wf_instance` (`wf_instance_id`),
  KEY `fk_change_pmc_decided_by` (`pmc_decided_by`),
  KEY `fk_change_target_milestone` (`target_milestone_id`),
  KEY `fk_change_new_pm` (`new_pm_user_id`),
  CONSTRAINT `fk_change_new_pm` FOREIGN KEY (`new_pm_user_id`) REFERENCES `iam_user` (`id`),
  CONSTRAINT `fk_change_pmc_decided_by` FOREIGN KEY (`pmc_decided_by`) REFERENCES `iam_user` (`id`),
  CONSTRAINT `fk_change_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `fk_change_requested_by` FOREIGN KEY (`requested_by`) REFERENCES `iam_user` (`id`),
  CONSTRAINT `fk_change_target_milestone` FOREIGN KEY (`target_milestone_id`) REFERENCES `milestone_def` (`id`),
  CONSTRAINT `fk_change_wf_instance` FOREIGN KEY (`wf_instance_id`) REFERENCES `wf_instance` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_change_request`
--

LOCK TABLES `project_change_request` WRITE;
/*!40000 ALTER TABLE `project_change_request` DISABLE KEYS */;
/*!40000 ALTER TABLE `project_change_request` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_document`
--

DROP TABLE IF EXISTS `project_document`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_document` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `project_id` bigint unsigned NOT NULL,
  `milestone_id` bigint unsigned DEFAULT NULL,
  `doc_type` varchar(64) NOT NULL,
  `doc_name` varchar(256) NOT NULL,
  `storage_uri` varchar(1024) NOT NULL,
  `uploaded_by` bigint unsigned DEFAULT NULL,
  `uploaded_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_doc_project_milestone` (`project_id`,`milestone_id`),
  KEY `idx_doc_project_type` (`project_id`,`doc_type`),
  KEY `fk_doc_milestone` (`milestone_id`),
  KEY `fk_doc_uploaded_by` (`uploaded_by`),
  CONSTRAINT `fk_doc_milestone` FOREIGN KEY (`milestone_id`) REFERENCES `milestone_def` (`id`),
  CONSTRAINT `fk_doc_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `fk_doc_uploaded_by` FOREIGN KEY (`uploaded_by`) REFERENCES `iam_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_document`
--

LOCK TABLES `project_document` WRITE;
/*!40000 ALTER TABLE `project_document` DISABLE KEYS */;
/*!40000 ALTER TABLE `project_document` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_level`
--

DROP TABLE IF EXISTS `project_level`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_level` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `level_code` varchar(8) NOT NULL,
  `level_name` varchar(64) NOT NULL,
  `definition_text` tinytext NOT NULL,
  `governance_text` tinytext,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_level_code` (`level_code`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_level`
--

LOCK TABLES `project_level` WRITE;
/*!40000 ALTER TABLE `project_level` DISABLE KEYS */;
INSERT INTO `project_level` VALUES (1,'H-L','火力全开 临床重大','公司战略核心；FIC/BIC潜力；重大未满足临床需求；重磅潜质；国际化与对外授权价值。','资源保障力★★★★★ 洞察决策力★★★★★ 人才驱动力★★★★★ 体系管控力★★★★★',1,'2026-04-23 09:19:50.239','2026-04-23 09:19:50.239'),(2,'G-L','临床重大','公司战略核心；FIC/BIC潜力；重大未满足临床需求；重磅潜质。','资源保障力★★★★ 洞察决策力★★★★★ 人才驱动力★★★★ 体系管控力★★★★★',1,'2026-04-23 09:19:50.239','2026-04-23 09:19:50.239'),(3,'H-Q','火力全开 重大临床前','公司研发管线核心；FIC/BIC潜力；重大未满足临床需求；重磅潜质；国际化与对外授权价值。','资源保障力★★★★★ 洞察决策力★★★★★ 人才驱动力★★★★★ 体系管控力★★★★',1,'2026-04-23 09:19:50.239','2026-04-23 09:19:50.239'),(4,'G-Q','重大临床前','公司研发管线核心；FIC/BIC潜力；重大未满足临床需求；重磅潜质。','资源保障力★★★★ 洞察决策力★★★★ 人才驱动力★★★★ 体系管控力★★★★',1,'2026-04-23 09:19:50.239','2026-04-23 09:19:50.239'),(5,'G-T','重大探索','探索性新靶点/新机制或技术风险较高；作为研发管线补充；具有重大市场价值。','资源保障力★★★ 洞察决策力★★★★ 人才驱动力★★★ 体系管控力★★★★',1,'2026-04-23 09:19:50.239','2026-04-23 09:19:50.239'),(6,'C-L','产能项目（临床）','具有巨大市场潜能；快速布局创新开发；锚定行业管线缺口。','资源保障力★★ 洞察决策力★★★ 人才驱动力★★ 体系管控力★★★',1,'2026-04-23 09:19:50.239','2026-04-23 09:19:50.239'),(7,'C-Q','产能项目（临床前）','具有巨大市场潜能；快速布局创新开发；锚定行业管线缺口。','资源保障力★★ 洞察决策力★★★ 人才驱动力★★ 体系管控力★★★',1,'2026-04-23 09:19:50.239','2026-04-23 09:19:50.239');
/*!40000 ALTER TABLE `project_level` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_milestone`
--

DROP TABLE IF EXISTS `project_milestone`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_milestone` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `project_id` bigint unsigned NOT NULL,
  `milestone_id` bigint unsigned NOT NULL,
  `planned_date` date DEFAULT NULL,
  `actual_date` date DEFAULT NULL,
  `status` enum('NOT_STARTED','IN_PROGRESS','SUBMITTED','APPROVED','CONDITIONAL_APPROVED','REJECTED') NOT NULL DEFAULT 'NOT_STARTED',
  `decision_result` enum('GO','CONDITIONAL_GO','NO_GO') DEFAULT NULL,
  `conditional_deadline` datetime(3) DEFAULT NULL,
  `decision_notes` tinytext,
  `decision_at` datetime(3) DEFAULT NULL,
  `decided_by` bigint unsigned DEFAULT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `conditional_attachments` json DEFAULT NULL COMMENT 'Conditional Go条件附件列表',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_milestone` (`project_id`,`milestone_id`),
  KEY `idx_project_milestone_project` (`project_id`),
  KEY `idx_project_milestone_status` (`status`),
  KEY `fk_project_milestone_def` (`milestone_id`),
  KEY `fk_project_milestone_decided_by` (`decided_by`),
  CONSTRAINT `fk_project_milestone_decided_by` FOREIGN KEY (`decided_by`) REFERENCES `iam_user` (`id`),
  CONSTRAINT `fk_project_milestone_def` FOREIGN KEY (`milestone_id`) REFERENCES `milestone_def` (`id`),
  CONSTRAINT `fk_project_milestone_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `ck_project_milestone_dates` CHECK (((`actual_date` is null) or (`planned_date` is null) or (`actual_date` >= `planned_date`)))
) ENGINE=InnoDB AUTO_INCREMENT=196 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_milestone`
--

LOCK TABLES `project_milestone` WRITE;
/*!40000 ALTER TABLE `project_milestone` DISABLE KEYS */;
INSERT INTO `project_milestone` VALUES (111,12,1,'2026-05-28',NULL,'IN_PROGRESS',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.813','2026-05-27 22:58:55.813',NULL),(112,12,2,'2026-08-26',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.813','2026-05-27 22:58:55.813',NULL),(113,12,3,'2026-11-24',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.813','2026-05-27 22:58:55.813',NULL),(114,12,4,'2027-02-22',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.813','2026-05-27 22:58:55.813',NULL),(115,12,5,'2027-05-23',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.813','2026-05-27 22:58:55.813',NULL),(116,12,6,'2027-08-21',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.813','2026-05-27 22:58:55.813',NULL),(117,12,7,'2027-11-19',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.813','2026-05-27 22:58:55.813',NULL),(118,12,8,'2028-02-17',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.813','2026-05-27 22:58:55.813',NULL),(119,12,9,'2028-05-17',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.813','2026-05-27 22:58:55.813',NULL),(120,12,10,'2028-08-15',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.813','2026-05-27 22:58:55.813',NULL),(126,13,1,'2026-02-27','2026-03-29','APPROVED','GO',NULL,NULL,'2026-05-28 06:58:55.820',NULL,'2026-05-27 22:58:55.820','2026-05-27 22:58:55.820',NULL),(127,13,2,'2026-05-28',NULL,'IN_PROGRESS',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.820','2026-05-27 22:58:55.820',NULL),(128,13,3,'2026-08-26',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.820','2026-05-27 22:58:55.820',NULL),(129,13,4,'2026-11-24',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.820','2026-05-27 22:58:55.820',NULL),(130,13,5,'2027-02-22',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.820','2026-05-27 22:58:55.820',NULL),(131,13,6,'2027-05-23',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.820','2026-05-27 22:58:55.820',NULL),(132,13,7,'2027-08-21',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.820','2026-05-27 22:58:55.820',NULL),(133,13,8,'2027-11-19',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.820','2026-05-27 22:58:55.820',NULL),(134,13,9,'2028-02-17',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.820','2026-05-27 22:58:55.820',NULL),(135,13,10,'2028-05-17',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.820','2026-05-27 22:58:55.820',NULL),(141,14,1,'2025-08-31','2025-11-29','APPROVED','GO',NULL,NULL,'2026-05-28 06:58:55.827',NULL,'2026-05-27 22:58:55.827','2026-05-27 22:58:55.827',NULL),(142,14,2,'2025-11-29','2026-01-28','APPROVED','GO',NULL,NULL,'2026-05-28 06:58:55.827',NULL,'2026-05-27 22:58:55.827','2026-05-27 22:58:55.827',NULL),(143,14,3,'2026-02-27','2026-03-29','APPROVED','GO',NULL,NULL,'2026-05-28 06:58:55.827',NULL,'2026-05-27 22:58:55.827','2026-05-27 22:58:55.827',NULL),(144,14,4,'2026-05-28',NULL,'IN_PROGRESS',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.827','2026-05-27 22:58:55.827',NULL),(145,14,5,'2026-08-26',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.827','2026-05-27 22:58:55.827',NULL),(146,14,6,'2026-11-24',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.827','2026-05-27 22:58:55.827',NULL),(147,14,7,'2027-02-22',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.827','2026-05-27 22:58:55.827',NULL),(148,14,8,'2027-05-23',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.827','2026-05-27 22:58:55.827',NULL),(149,14,9,'2027-08-21',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.827','2026-05-27 22:58:55.827',NULL),(150,14,10,'2027-11-19',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.827','2026-05-27 22:58:55.827',NULL),(156,15,1,'2025-03-04','2025-08-01','APPROVED','GO',NULL,NULL,'2026-05-28 06:58:55.834',NULL,'2026-05-27 22:58:55.834','2026-05-27 22:58:55.834',NULL),(157,15,2,'2025-06-02','2025-09-30','APPROVED','GO',NULL,NULL,'2026-05-28 06:58:55.834',NULL,'2026-05-27 22:58:55.834','2026-05-27 22:58:55.834',NULL),(158,15,3,'2025-08-31','2025-11-29','APPROVED','GO',NULL,NULL,'2026-05-28 06:58:55.834',NULL,'2026-05-27 22:58:55.834','2026-05-27 22:58:55.834',NULL),(159,15,4,'2025-11-29','2026-01-28','APPROVED','GO',NULL,NULL,'2026-05-28 06:58:55.834',NULL,'2026-05-27 22:58:55.834','2026-05-27 22:58:55.834',NULL),(160,15,5,'2026-02-27','2026-03-29','APPROVED','GO',NULL,NULL,'2026-05-28 06:58:55.834',NULL,'2026-05-27 22:58:55.834','2026-05-27 22:58:55.834',NULL),(161,15,6,'2026-05-28',NULL,'IN_PROGRESS',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.834','2026-05-27 22:58:55.834',NULL),(162,15,7,'2026-08-26',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.834','2026-05-27 22:58:55.834',NULL),(163,15,8,'2026-11-24',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.834','2026-05-27 22:58:55.834',NULL),(164,15,9,'2027-02-22',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.834','2026-05-27 22:58:55.834',NULL),(165,15,10,'2027-05-23',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.834','2026-05-27 22:58:55.834',NULL),(171,16,1,'2024-09-05','2025-04-03','APPROVED','GO',NULL,NULL,'2026-05-28 06:58:55.840',NULL,'2026-05-27 22:58:55.840','2026-05-27 22:58:55.840',NULL),(172,16,2,'2024-12-04','2025-06-02','APPROVED','GO',NULL,NULL,'2026-05-28 06:58:55.840',NULL,'2026-05-27 22:58:55.840','2026-05-27 22:58:55.840',NULL),(173,16,3,'2025-03-04','2025-08-01','APPROVED','GO',NULL,NULL,'2026-05-28 06:58:55.840',NULL,'2026-05-27 22:58:55.840','2026-05-27 22:58:55.840',NULL),(174,16,4,'2025-06-02','2025-09-30','APPROVED','GO',NULL,NULL,'2026-05-28 06:58:55.840',NULL,'2026-05-27 22:58:55.840','2026-05-27 22:58:55.840',NULL),(175,16,5,'2025-08-31','2025-11-29','APPROVED','GO',NULL,NULL,'2026-05-28 06:58:55.840',NULL,'2026-05-27 22:58:55.840','2026-05-27 22:58:55.840',NULL),(176,16,6,'2025-11-29','2026-01-28','APPROVED','GO',NULL,NULL,'2026-05-28 06:58:55.840',NULL,'2026-05-27 22:58:55.840','2026-05-27 22:58:55.840',NULL),(177,16,7,'2026-02-27','2026-03-29','APPROVED','GO',NULL,NULL,'2026-05-28 06:58:55.840',NULL,'2026-05-27 22:58:55.840','2026-05-27 22:58:55.840',NULL),(178,16,8,'2026-05-28',NULL,'IN_PROGRESS',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.840','2026-05-27 22:58:55.840',NULL),(179,16,9,'2026-08-26',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.840','2026-05-27 22:58:55.840',NULL),(180,16,10,'2026-11-24',NULL,'NOT_STARTED',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.840','2026-05-27 22:58:55.840',NULL),(186,17,1,'2024-03-09','2024-12-04','APPROVED','GO',NULL,NULL,'2026-05-28 06:58:55.847',NULL,'2026-05-27 22:58:55.847','2026-05-27 22:58:55.847',NULL),(187,17,2,'2024-06-07','2025-02-02','APPROVED','GO',NULL,NULL,'2026-05-28 06:58:55.847',NULL,'2026-05-27 22:58:55.847','2026-05-27 22:58:55.847',NULL),(188,17,3,'2024-09-05','2025-04-03','APPROVED','GO',NULL,NULL,'2026-05-28 06:58:55.847',NULL,'2026-05-27 22:58:55.847','2026-05-27 22:58:55.847',NULL),(189,17,4,'2024-12-04','2025-06-02','APPROVED','GO',NULL,NULL,'2026-05-28 06:58:55.847',NULL,'2026-05-27 22:58:55.847','2026-05-27 22:58:55.847',NULL),(190,17,5,'2025-03-04','2025-08-01','APPROVED','GO',NULL,NULL,'2026-05-28 06:58:55.847',NULL,'2026-05-27 22:58:55.847','2026-05-27 22:58:55.847',NULL),(191,17,6,'2025-06-02','2025-09-30','APPROVED','GO',NULL,NULL,'2026-05-28 06:58:55.847',NULL,'2026-05-27 22:58:55.847','2026-05-27 22:58:55.847',NULL),(192,17,7,'2025-08-31','2025-11-29','APPROVED','GO',NULL,NULL,'2026-05-28 06:58:55.847',NULL,'2026-05-27 22:58:55.847','2026-05-27 22:58:55.847',NULL),(193,17,8,'2025-11-29','2026-01-28','APPROVED','GO',NULL,NULL,'2026-05-28 06:58:55.847',NULL,'2026-05-27 22:58:55.847','2026-05-27 22:58:55.847',NULL),(194,17,9,'2026-02-27','2026-03-29','APPROVED','GO',NULL,NULL,'2026-05-28 06:58:55.847',NULL,'2026-05-27 22:58:55.847','2026-05-27 22:58:55.847',NULL),(195,17,10,'2026-05-28',NULL,'IN_PROGRESS',NULL,NULL,NULL,NULL,NULL,'2026-05-27 22:58:55.847','2026-05-27 22:58:55.847',NULL);
/*!40000 ALTER TABLE `project_milestone` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_team_member`
--

DROP TABLE IF EXISTS `project_team_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_team_member` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `project_id` bigint unsigned NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `dept_id` bigint unsigned DEFAULT NULL,
  `team_role` enum('PM','PDT_LEAD','FUNCTION_LEAD','MEMBER') NOT NULL DEFAULT 'MEMBER',
  `effective_from` date NOT NULL,
  `effective_to` date DEFAULT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_team_member` (`project_id`,`user_id`,`effective_from`),
  KEY `idx_project_team_project` (`project_id`),
  KEY `idx_project_team_user` (`user_id`),
  KEY `idx_project_team_dept` (`dept_id`),
  CONSTRAINT `fk_project_team_dept` FOREIGN KEY (`dept_id`) REFERENCES `org_department` (`id`),
  CONSTRAINT `fk_project_team_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `fk_project_team_user` FOREIGN KEY (`user_id`) REFERENCES `iam_user` (`id`),
  CONSTRAINT `ck_project_team_dates` CHECK (((`effective_to` is null) or (`effective_to` >= `effective_from`)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_team_member`
--

LOCK TABLES `project_team_member` WRITE;
/*!40000 ALTER TABLE `project_team_member` DISABLE KEYS */;
/*!40000 ALTER TABLE `project_team_member` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_termination_task`
--

DROP TABLE IF EXISTS `project_termination_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_termination_task` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `project_id` bigint unsigned NOT NULL,
  `change_request_id` bigint unsigned DEFAULT NULL,
  `task_code` varchar(64) NOT NULL COMMENT '任务代码，如 ASSET_DISPOSAL, DOCUMENT_ARCHIVE',
  `task_description` tinytext NOT NULL COMMENT '任务详细描述',
  `status` enum('OPEN','COMPLETED','OVERDUE') NOT NULL DEFAULT 'OPEN' COMMENT '任务状态',
  `due_date` date DEFAULT NULL COMMENT '截止日期',
  `completed_at` datetime(3) DEFAULT NULL COMMENT '完成时间',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_termination_project` (`project_id`),
  KEY `idx_termination_status` (`status`),
  KEY `fk_termination_change_request` (`change_request_id`),
  CONSTRAINT `fk_termination_change_request` FOREIGN KEY (`change_request_id`) REFERENCES `project_change_request` (`id`),
  CONSTRAINT `fk_termination_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目终止任务清单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_termination_task`
--

LOCK TABLES `project_termination_task` WRITE;
/*!40000 ALTER TABLE `project_termination_task` DISABLE KEYS */;
/*!40000 ALTER TABLE `project_termination_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `review_approval`
--

DROP TABLE IF EXISTS `review_approval`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review_approval` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `project_id` bigint unsigned NOT NULL COMMENT '项目ID',
  `project_milestone_id` bigint unsigned NOT NULL COMMENT '项目里程碑ID',
  `wf_instance_id` bigint unsigned DEFAULT NULL COMMENT '工作流实例ID',
  `submitter_user_id` bigint unsigned DEFAULT NULL COMMENT '提交人（发起人）',
  `submit_comment` text COMMENT '提交备注',
  `status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/SUBMITTED/APPROVED/REJECTED',
  `submitted_at` datetime(3) DEFAULT NULL COMMENT '提交时间',
  `finished_at` datetime(3) DEFAULT NULL COMMENT '完成时间',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `review_type` varchar(50) DEFAULT 'MILESTONE' COMMENT '评审类型: INITIATION(立项评审) / MILESTONE(里程碑评审)',
  PRIMARY KEY (`id`),
  KEY `idx_ra_project` (`project_id`),
  KEY `idx_ra_milestone` (`project_milestone_id`),
  KEY `idx_ra_status` (`status`),
  KEY `fk_ra_wf_instance` (`wf_instance_id`),
  KEY `fk_ra_submitter` (`submitter_user_id`),
  CONSTRAINT `fk_ra_milestone` FOREIGN KEY (`project_milestone_id`) REFERENCES `project_milestone` (`id`),
  CONSTRAINT `fk_ra_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `fk_ra_submitter` FOREIGN KEY (`submitter_user_id`) REFERENCES `iam_user` (`id`),
  CONSTRAINT `fk_ra_wf_instance` FOREIGN KEY (`wf_instance_id`) REFERENCES `wf_instance` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `review_approval`
--

LOCK TABLES `review_approval` WRITE;
/*!40000 ALTER TABLE `review_approval` DISABLE KEYS */;
/*!40000 ALTER TABLE `review_approval` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `review_approval_task`
--

DROP TABLE IF EXISTS `review_approval_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review_approval_task` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `review_approval_id` bigint unsigned NOT NULL COMMENT '评审审批记录ID',
  `approver_user_id` bigint unsigned NOT NULL COMMENT '审批人用户ID',
  `approver_role` varchar(64) DEFAULT NULL COMMENT '审批人角色',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '审批顺序',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED',
  `decision` varchar(32) DEFAULT NULL COMMENT '决策：APPROVED/REJECTED',
  `opinion` text COMMENT '审批意见',
  `decided_at` datetime(3) DEFAULT NULL COMMENT '决策时间',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `conditional_attachment_required` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否需要条件附件(Conditional Go时)',
  PRIMARY KEY (`id`),
  KEY `idx_rat_approval` (`review_approval_id`),
  KEY `idx_rat_approver` (`approver_user_id`,`status`),
  CONSTRAINT `fk_rat_approval` FOREIGN KEY (`review_approval_id`) REFERENCES `review_approval` (`id`),
  CONSTRAINT `fk_rat_approver` FOREIGN KEY (`approver_user_id`) REFERENCES `iam_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `review_approval_task`
--

LOCK TABLES `review_approval_task` WRITE;
/*!40000 ALTER TABLE `review_approval_task` DISABLE KEYS */;
/*!40000 ALTER TABLE `review_approval_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `review_record`
--

DROP TABLE IF EXISTS `review_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `project_id` bigint unsigned NOT NULL COMMENT '项目ID',
  `project_milestone_id` bigint unsigned NOT NULL COMMENT '项目里程碑ID',
  `review_approval_id` bigint unsigned DEFAULT NULL COMMENT '关联的审批记录ID',
  `action` varchar(32) NOT NULL COMMENT '操作类型：SUBMIT/APPROVE/REJECT/SAVE_DRAFT',
  `actor_user_id` bigint unsigned DEFAULT NULL COMMENT '操作人',
  `actor_role` varchar(64) DEFAULT NULL COMMENT '操作人角色',
  `result` varchar(32) DEFAULT NULL COMMENT '结果：PASS/FAIL/SUBMITTED/DRAFT_SAVED',
  `opinion` text COMMENT '意见',
  `action_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '操作时间',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `review_type` varchar(50) DEFAULT 'MILESTONE' COMMENT '评审类型: INITIATION(立项评审) / MILESTONE(里程碑评审)',
  PRIMARY KEY (`id`),
  KEY `idx_rr_project` (`project_id`,`action_at`),
  KEY `idx_rr_milestone` (`project_milestone_id`),
  KEY `idx_rr_actor` (`actor_user_id`),
  KEY `fk_rr_approval` (`review_approval_id`),
  CONSTRAINT `fk_rr_actor` FOREIGN KEY (`actor_user_id`) REFERENCES `iam_user` (`id`),
  CONSTRAINT `fk_rr_approval` FOREIGN KEY (`review_approval_id`) REFERENCES `review_approval` (`id`),
  CONSTRAINT `fk_rr_milestone` FOREIGN KEY (`project_milestone_id`) REFERENCES `project_milestone` (`id`),
  CONSTRAINT `fk_rr_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `review_record`
--

LOCK TABLES `review_record` WRITE;
/*!40000 ALTER TABLE `review_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `review_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role`
--

DROP TABLE IF EXISTS `role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL COMMENT '角色名称',
  `description` varchar(255) DEFAULT NULL COMMENT '角色描述',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  UNIQUE KEY `uk_role_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role`
--

LOCK TABLES `role` WRITE;
/*!40000 ALTER TABLE `role` DISABLE KEYS */;
INSERT INTO `role` VALUES (1,'ROLE_PMC','PMC（项目管理委员会）- 拥有最高决策权，负责Go/No Go决策、重大变更审批及预算追加审批','2026-04-27 05:58:56.252','2026-04-27 05:58:56.252'),(2,'ROLE_PM','PM（项目经理）- 负责横向贯通，拥有制定计划、监控预算、组织评审及提交变更申请的权限','2026-04-27 05:58:56.252','2026-04-27 05:58:56.252'),(3,'ROLE_DEPT_HEAD','职能部门负责人 - 负责所属领域的交付物提交','2026-04-27 05:58:56.252','2026-04-27 05:58:56.252'),(4,'ROLE_EFFICIENCY','效率管理部 - 负责系统维护、月度预算核算、预警监控及项目考核','2026-04-27 05:58:56.252','2026-04-27 05:58:56.252'),(5,'ROLE_COMPLIANCE','药政合规部 - 负责合规性意见出具及申报文档审查','2026-04-27 05:58:56.252','2026-04-27 05:58:56.252'),(6,'ROLE_ADMIN','系统管理员 - 拥有系统管理和配置权限','2026-04-27 05:58:56.252','2026-05-19 19:04:34.670'),(8,'ROLE_DEPT_EXECUTOR','部门执行人 - 可上传交付物并发起评审','2026-05-20 08:53:00.000','2026-05-20 08:53:00.000');
/*!40000 ALTER TABLE `role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role_permissions`
--

DROP TABLE IF EXISTS `role_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_permissions` (
  `role_id` bigint unsigned NOT NULL,
  `permission_id` bigint unsigned NOT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`role_id`,`permission_id`),
  KEY `idx_role_permissions_permission` (`permission_id`),
  CONSTRAINT `fk_role_permissions_permission` FOREIGN KEY (`permission_id`) REFERENCES `permission` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_role_permissions_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色-权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role_permissions`
--

LOCK TABLES `role_permissions` WRITE;
/*!40000 ALTER TABLE `role_permissions` DISABLE KEYS */;
INSERT INTO `role_permissions` VALUES (1,2,'2026-04-27 05:58:56.272'),(1,4,'2026-04-27 05:58:56.272'),(1,5,'2026-04-27 05:58:56.272'),(1,8,'2026-04-27 05:58:56.272'),(1,12,'2026-04-27 05:58:56.272'),(1,15,'2026-04-27 05:58:56.272'),(1,16,'2026-04-27 05:58:56.272'),(1,22,'2026-04-27 08:01:48.568'),(1,25,'2026-04-27 08:01:48.568'),(1,27,'2026-05-11 01:20:18.556'),(1,28,'2026-05-11 01:20:18.556'),(2,1,'2026-04-27 05:58:56.277'),(2,4,'2026-04-27 05:58:56.277'),(2,6,'2026-04-27 05:58:56.277'),(2,7,'2026-04-27 05:58:56.277'),(2,8,'2026-04-27 05:58:56.277'),(2,9,'2026-04-27 05:58:56.277'),(2,11,'2026-04-27 05:58:56.277'),(2,12,'2026-04-27 05:58:56.277'),(2,14,'2026-04-27 05:58:56.277'),(2,16,'2026-04-27 05:58:56.277'),(2,21,'2026-04-27 08:01:48.571'),(2,22,'2026-04-27 08:01:48.571'),(2,23,'2026-04-27 08:01:48.571'),(2,25,'2026-04-27 08:01:48.571'),(2,26,'2026-05-11 01:20:18.560'),(2,28,'2026-05-11 01:20:18.560'),(3,4,'2026-04-27 05:58:56.279'),(3,8,'2026-04-27 05:58:56.279'),(3,11,'2026-04-27 05:58:56.279'),(3,12,'2026-04-27 05:58:56.279'),(3,28,'2026-05-11 01:20:18.561'),(4,4,'2026-04-27 05:58:56.281'),(4,8,'2026-04-27 05:58:56.281'),(4,16,'2026-04-27 05:58:56.281'),(4,19,'2026-04-27 05:58:56.281'),(4,20,'2026-04-27 05:58:56.281'),(4,28,'2026-05-11 01:20:18.561'),(5,8,'2026-04-27 05:58:56.283'),(5,12,'2026-04-27 05:58:56.283'),(5,13,'2026-04-27 05:58:56.283'),(5,22,'2026-04-27 08:01:48.572'),(5,24,'2026-04-27 08:01:48.572'),(5,25,'2026-04-27 08:01:48.572'),(6,1,'2026-05-20 03:04:34.756'),(6,2,'2026-05-20 03:04:34.741'),(6,3,'2026-05-20 03:04:34.717'),(6,4,'2026-05-20 03:04:34.697'),(6,5,'2026-05-20 03:04:34.738'),(6,6,'2026-05-20 03:04:34.770'),(6,7,'2026-05-20 03:04:34.783'),(6,8,'2026-05-20 03:04:34.799'),(6,9,'2026-05-20 03:04:34.792'),(6,10,'2026-05-20 03:04:34.694'),(6,11,'2026-05-20 03:04:34.765'),(6,12,'2026-05-20 03:04:34.700'),(6,13,'2026-05-20 03:04:34.802'),(6,14,'2026-05-20 03:04:34.805'),(6,15,'2026-05-20 03:04:34.809'),(6,16,'2026-05-20 03:04:34.707'),(6,17,'2026-05-20 03:04:34.775'),(6,18,'2026-05-20 03:04:34.767'),(6,19,'2026-05-20 03:04:34.781'),(6,20,'2026-05-20 03:04:34.735'),(6,21,'2026-05-20 03:04:34.703'),(6,22,'2026-05-20 03:04:34.733'),(6,23,'2026-05-20 03:04:34.773'),(6,24,'2026-05-20 03:04:34.786'),(6,25,'2026-05-20 03:04:34.778'),(6,26,'2026-05-20 03:04:34.789'),(6,27,'2026-05-20 03:04:34.720'),(6,28,'2026-05-20 03:04:34.713'),(6,29,'2026-05-20 03:04:34.796'),(8,1,'2026-05-20 08:56:18.669'),(8,8,'2026-05-20 08:56:18.669'),(8,11,'2026-05-20 08:56:18.669');
/*!40000 ALTER TABLE `role_permissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL COMMENT '用户名',
  `password` varchar(255) NOT NULL COMMENT '加密密码',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
  `is_active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否激活',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `department_id` bigint unsigned DEFAULT NULL COMMENT '所属部门ID（关联org_department表）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `uk_user_username` (`username`),
  KEY `idx_user_email` (`email`),
  KEY `idx_user_is_active` (`is_active`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统认证用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'pmc_user','$2a$10$BKt1k9PEGF45c44yibwo/e8Qo6Eavns9FpeatKhsqMUXIbkW6BehW','pmc@example.com',1,'2026-04-27 05:58:56.286','2026-05-20 02:21:26.000',NULL),(2,'pm_user','$2a$10$BKt1k9PEGF45c44yibwo/e8Qo6Eavns9FpeatKhsqMUXIbkW6BehW','pm@example.com',1,'2026-04-27 05:58:56.286','2026-05-27 23:35:02.096',11),(3,'dept_head','$2a$10$BKt1k9PEGF45c44yibwo/e8Qo6Eavns9FpeatKhsqMUXIbkW6BehW','dept@example.com',1,'2026-04-27 05:58:56.286','2026-05-27 23:35:55.716',1),(4,'efficiency_user','$2a$10$BKt1k9PEGF45c44yibwo/e8Qo6Eavns9FpeatKhsqMUXIbkW6BehW','efficiency@example.com',1,'2026-04-27 05:58:56.286','2026-05-27 23:35:36.716',6),(5,'compliance_user','$2a$10$BKt1k9PEGF45c44yibwo/e8Qo6Eavns9FpeatKhsqMUXIbkW6BehW','compliance@example.com',1,'2026-04-27 05:58:56.286','2026-05-20 02:21:26.000',NULL),(6,'admin_user','$2a$10$qDqrtouAyM39n.vOymTJ4uV7Hz5c1h0lq5LwB75hxhH3beXiE5SYu','admin@example.com',1,'2026-04-27 05:58:56.286','2026-06-23 03:06:05.619',10);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_roles`
--

DROP TABLE IF EXISTS `user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
  `user_id` bigint unsigned NOT NULL,
  `role_id` bigint unsigned NOT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `idx_user_roles_role` (`role_id`),
  CONSTRAINT `fk_user_roles_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_roles_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户-角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_roles`
--

LOCK TABLES `user_roles` WRITE;
/*!40000 ALTER TABLE `user_roles` DISABLE KEYS */;
INSERT INTO `user_roles` VALUES (1,1,'2026-04-27 05:58:56.292'),(2,2,'2026-04-27 05:58:56.297'),(3,3,'2026-04-27 05:58:56.299'),(4,4,'2026-04-27 05:58:56.300'),(5,5,'2026-04-27 05:58:56.301'),(6,1,'2026-05-20 09:19:43.623'),(6,2,'2026-05-20 09:19:43.778'),(6,3,'2026-05-20 09:19:43.643'),(6,4,'2026-05-20 09:19:43.831'),(6,5,'2026-05-20 09:19:43.670'),(6,6,'2026-05-20 09:19:43.735'),(6,8,'2026-05-20 09:19:43.697');
/*!40000 ALTER TABLE `user_roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `v_pending_review_tasks`
--

DROP TABLE IF EXISTS `v_pending_review_tasks`;
/*!50001 DROP VIEW IF EXISTS `v_pending_review_tasks`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_pending_review_tasks` AS SELECT 
 1 AS `task_id`,
 1 AS `review_approval_id`,
 1 AS `approver_user_id`,
 1 AS `approver_role`,
 1 AS `task_status`,
 1 AS `project_id`,
 1 AS `project_milestone_id`,
 1 AS `submitter_user_id`,
 1 AS `submit_comment`,
 1 AS `approval_status`,
 1 AS `submitted_at`,
 1 AS `review_type`,
 1 AS `project_code`,
 1 AS `project_name`,
 1 AS `current_milestone_id`,
 1 AS `milestone_code`,
 1 AS `milestone_name`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `wf_action_log`
--

DROP TABLE IF EXISTS `wf_action_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_action_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `instance_id` bigint unsigned NOT NULL,
  `task_id` bigint unsigned DEFAULT NULL,
  `action` enum('SUBMIT','APPROVE','REJECT','CANCEL','COMMENT','SYSTEM') NOT NULL,
  `actor_user_id` bigint unsigned DEFAULT NULL,
  `action_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `payload_json` json DEFAULT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_wf_action_instance` (`instance_id`,`action_at`),
  KEY `idx_wf_action_task` (`task_id`),
  KEY `fk_wf_action_actor` (`actor_user_id`),
  CONSTRAINT `fk_wf_action_actor` FOREIGN KEY (`actor_user_id`) REFERENCES `iam_user` (`id`),
  CONSTRAINT `fk_wf_action_instance` FOREIGN KEY (`instance_id`) REFERENCES `wf_instance` (`id`),
  CONSTRAINT `fk_wf_action_task` FOREIGN KEY (`task_id`) REFERENCES `wf_task` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_action_log`
--

LOCK TABLES `wf_action_log` WRITE;
/*!40000 ALTER TABLE `wf_action_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `wf_action_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wf_instance`
--

DROP TABLE IF EXISTS `wf_instance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_instance` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `template_id` bigint unsigned NOT NULL,
  `business_type` varchar(64) NOT NULL,
  `business_id` bigint unsigned NOT NULL,
  `status` enum('DRAFT','RUNNING','APPROVED','REJECTED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
  `started_by` bigint unsigned DEFAULT NULL,
  `started_at` datetime(3) DEFAULT NULL,
  `finished_at` datetime(3) DEFAULT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_wf_instance_business` (`business_type`,`business_id`),
  KEY `idx_wf_instance_template` (`template_id`),
  KEY `fk_wf_instance_started_by` (`started_by`),
  CONSTRAINT `fk_wf_instance_started_by` FOREIGN KEY (`started_by`) REFERENCES `iam_user` (`id`),
  CONSTRAINT `fk_wf_instance_template` FOREIGN KEY (`template_id`) REFERENCES `wf_template` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_instance`
--

LOCK TABLES `wf_instance` WRITE;
/*!40000 ALTER TABLE `wf_instance` DISABLE KEYS */;
/*!40000 ALTER TABLE `wf_instance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wf_task`
--

DROP TABLE IF EXISTS `wf_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_task` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `instance_id` bigint unsigned NOT NULL,
  `node_id` bigint unsigned NOT NULL,
  `task_name` varchar(128) NOT NULL,
  `assignee_user_id` bigint unsigned DEFAULT NULL,
  `status` enum('PENDING','APPROVED','REJECTED','CANCELLED') NOT NULL DEFAULT 'PENDING',
  `decided_at` datetime(3) DEFAULT NULL,
  `decision_notes` tinytext,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_wf_task_instance` (`instance_id`),
  KEY `idx_wf_task_assignee` (`assignee_user_id`,`status`),
  KEY `fk_wf_task_node` (`node_id`),
  CONSTRAINT `fk_wf_task_assignee` FOREIGN KEY (`assignee_user_id`) REFERENCES `iam_user` (`id`),
  CONSTRAINT `fk_wf_task_instance` FOREIGN KEY (`instance_id`) REFERENCES `wf_instance` (`id`),
  CONSTRAINT `fk_wf_task_node` FOREIGN KEY (`node_id`) REFERENCES `wf_template_node` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_task`
--

LOCK TABLES `wf_task` WRITE;
/*!40000 ALTER TABLE `wf_task` DISABLE KEYS */;
/*!40000 ALTER TABLE `wf_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wf_template`
--

DROP TABLE IF EXISTS `wf_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_template` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `template_code` varchar(64) NOT NULL,
  `template_name` varchar(128) NOT NULL,
  `description` tinytext,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wf_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_template`
--

LOCK TABLES `wf_template` WRITE;
/*!40000 ALTER TABLE `wf_template` DISABLE KEYS */;
/*!40000 ALTER TABLE `wf_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wf_template_node`
--

DROP TABLE IF EXISTS `wf_template_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_template_node` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `template_id` bigint unsigned NOT NULL,
  `node_code` varchar(64) NOT NULL,
  `node_name` varchar(128) NOT NULL,
  `node_type` enum('START','APPROVAL','CONDITION','END') NOT NULL,
  `sort_no` int NOT NULL,
  `approver_mode` enum('USER','ROLE','COMMITTEE') DEFAULT NULL,
  `approver_ref` varchar(128) DEFAULT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wf_template_node` (`template_id`,`node_code`),
  KEY `idx_wf_template_node_template` (`template_id`),
  CONSTRAINT `fk_wf_template_node_template` FOREIGN KEY (`template_id`) REFERENCES `wf_template` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_template_node`
--

LOCK TABLES `wf_template_node` WRITE;
/*!40000 ALTER TABLE `wf_template_node` DISABLE KEYS */;
/*!40000 ALTER TABLE `wf_template_node` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'kbd_pm_system'
--

--
-- Current Database: `kbd_pm_system`
--

USE `kbd_pm_system`;

--
-- Final view structure for view `v_pending_review_tasks`
--

/*!50001 DROP VIEW IF EXISTS `v_pending_review_tasks`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_pending_review_tasks` AS select `t`.`id` AS `task_id`,`t`.`review_approval_id` AS `review_approval_id`,`t`.`approver_user_id` AS `approver_user_id`,`t`.`approver_role` AS `approver_role`,`t`.`status` AS `task_status`,`a`.`project_id` AS `project_id`,`a`.`project_milestone_id` AS `project_milestone_id`,`a`.`submitter_user_id` AS `submitter_user_id`,`a`.`submit_comment` AS `submit_comment`,`a`.`status` AS `approval_status`,`a`.`submitted_at` AS `submitted_at`,`a`.`review_type` AS `review_type`,`p`.`project_code` AS `project_code`,`p`.`project_name` AS `project_name`,`p`.`current_milestone_id` AS `current_milestone_id`,`md`.`milestone_code` AS `milestone_code`,`md`.`milestone_name` AS `milestone_name` from ((((`review_approval_task` `t` join `review_approval` `a` on((`t`.`review_approval_id` = `a`.`id`))) join `project` `p` on((`a`.`project_id` = `p`.`id`))) left join `project_milestone` `pm` on((`a`.`project_milestone_id` = `pm`.`id`))) left join `milestone_def` `md` on((`pm`.`milestone_id` = `md`.`id`))) where ((`t`.`status` = 'PENDING') and (`a`.`status` = 'SUBMITTED') and (`p`.`status` in ('ACTIVE','DRAFT'))) order by `a`.`submitted_at` desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-23 11:21:26
