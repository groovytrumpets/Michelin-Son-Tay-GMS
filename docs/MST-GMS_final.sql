-- MySQL dump 10.13  Distrib 8.0.43, for Win64 (x86_64)
--
-- Host: localhost    Database: michelin_garage
-- ------------------------------------------------------
-- Server version	9.4.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `attendance_checkin`
--

DROP TABLE IF EXISTS `attendance_checkin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance_checkin` (
  `checkin_id` int NOT NULL AUTO_INCREMENT,
  `staff_id` int NOT NULL,
  `attendance_date` date NOT NULL,
  `shift_id` int DEFAULT NULL,
  `check_in_time` time DEFAULT NULL,
  `check_out_time` time DEFAULT NULL,
  `status` enum('PRESENT','LATE') COLLATE utf8mb4_unicode_ci DEFAULT 'PRESENT',
  `notes` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`checkin_id`),
  UNIQUE KEY `uk_staff_date_shift` (`staff_id`,`attendance_date`,`shift_id`),
  KEY `idx_staff_date` (`staff_id`,`attendance_date`),
  KEY `fk_attendance_checkin_shift` (`shift_id`),
  CONSTRAINT `attendance_checkin_ibfk_1` FOREIGN KEY (`staff_id`) REFERENCES `staff_profile` (`staff_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_attendance_checkin_shift` FOREIGN KEY (`shift_id`) REFERENCES `work_shift` (`shift_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `booking`
--

DROP TABLE IF EXISTS `booking`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `booking` (
  `booking_id` int NOT NULL AUTO_INCREMENT,
  `booking_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Formatted booking code: BK-YYYYMMDD-XXXX',
  `customer_id` int DEFAULT NULL,
  `scheduled_date` date NOT NULL,
  `scheduled_time` time NOT NULL,
  `queue_order` int DEFAULT NULL,
  `status` enum('CONFIRMED','DONE','CANCELLED','NOT_ARRIVED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'CONFIRMED',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `is_guest` bit(1) DEFAULT NULL,
  `vehicle_id` int DEFAULT NULL,
  `estimate_time` int DEFAULT NULL,
  `estimate_id` int DEFAULT NULL,
  PRIMARY KEY (`booking_id`),
  UNIQUE KEY `idx_booking_code` (`booking_code`),
  KEY `fk_booking_customer` (`customer_id`),
  KEY `FKejehywt60rdh29uvn8ejths82` (`vehicle_id`),
  KEY `fk_booking_estimate1_idx` (`estimate_id`),
  CONSTRAINT `fk_booking_customer` FOREIGN KEY (`customer_id`) REFERENCES `customer_profile` (`customer_id`),
  CONSTRAINT `fk_booking_estimate1` FOREIGN KEY (`estimate_id`) REFERENCES `estimate` (`estimate_id`),
  CONSTRAINT `FKejehywt60rdh29uvn8ejths82` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`vehicle_id`)
) ENGINE=InnoDB AUTO_INCREMENT=115 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `booking_code_sequence`
--

DROP TABLE IF EXISTS `booking_code_sequence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `booking_code_sequence` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sequence_date` date NOT NULL,
  `code_prefix` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `current_sequence` int NOT NULL DEFAULT '0',
  `version` int NOT NULL DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sequence_date_prefix` (`sequence_date`,`code_prefix`),
  KEY `idx_sequence_date` (`sequence_date`),
  KEY `idx_code_prefix` (`code_prefix`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `booking_details`
--

DROP TABLE IF EXISTS `booking_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `booking_details` (
  `booking_id` int NOT NULL,
  `item_id` int NOT NULL,
  PRIMARY KEY (`booking_id`,`item_id`),
  KEY `FKljjcpvha5rnj8ybmacyltmxwn` (`item_id`),
  KEY `FKdi4hhcv3pwr6b14qfhf9gahex` (`booking_id`),
  CONSTRAINT `FKdi4hhcv3pwr6b14qfhf9gahex` FOREIGN KEY (`booking_id`) REFERENCES `booking` (`booking_id`),
  CONSTRAINT `FKljjcpvha5rnj8ybmacyltmxwn` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `booking_request`
--

DROP TABLE IF EXISTS `booking_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `booking_request` (
  `request_id` int NOT NULL AUTO_INCREMENT,
  `request_code` varchar(20) DEFAULT NULL COMMENT 'Formatted request code: RQ-YYYYMMDD-XXXX',
  `client_ip` varchar(45) DEFAULT NULL,
  `confirmed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text,
  `expires_at` datetime(6) DEFAULT NULL,
  `full_name` varchar(100) DEFAULT NULL,
  `is_guest` bit(1) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `rejection_reason` varchar(255) DEFAULT NULL,
  `scheduled_date` date NOT NULL,
  `scheduled_time` time(6) NOT NULL,
  `service_category` varchar(50) DEFAULT NULL,
  `status` enum('CONFIRMED','EXPIRED','PENDING','REJECTED','CONTACTED','SPAM') NOT NULL,
  `confirmed_by` int DEFAULT NULL,
  `customer_id` int DEFAULT NULL,
  `note` text,
  PRIMARY KEY (`request_id`),
  UNIQUE KEY `idx_request_code` (`request_code`),
  KEY `idx_phone` (`phone`),
  KEY `idx_status` (`status`),
  KEY `idx_created` (`created_at`),
  KEY `idx_customer` (`customer_id`),
  KEY `idx_expires` (`expires_at`),
  KEY `FKdp4k8r8tt0h3e8nn60ia1gvtb` (`confirmed_by`),
  CONSTRAINT `FK9qukneuwqv93375m4js6xu5sy` FOREIGN KEY (`customer_id`) REFERENCES `customer_profile` (`customer_id`),
  CONSTRAINT `FKdp4k8r8tt0h3e8nn60ia1gvtb` FOREIGN KEY (`confirmed_by`) REFERENCES `staff_profile` (`staff_id`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `booking_request_details`
--

DROP TABLE IF EXISTS `booking_request_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `booking_request_details` (
  `request_detail_id` int NOT NULL AUTO_INCREMENT,
  `item_id` int NOT NULL,
  `request_id` int NOT NULL,
  PRIMARY KEY (`request_detail_id`),
  KEY `FKet8mj8afww9wxtxv5f6kb5ygg` (`item_id`),
  KEY `FKkxjw38d2lxiop48e6ai52fxdf` (`request_id`),
  CONSTRAINT `FKet8mj8afww9wxtxv5f6kb5ygg` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`),
  CONSTRAINT `FKkxjw38d2lxiop48e6ai52fxdf` FOREIGN KEY (`request_id`) REFERENCES `booking_request` (`request_id`)
) ENGINE=InnoDB AUTO_INCREMENT=57 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `booking_slot_reservation`
--

DROP TABLE IF EXISTS `booking_slot_reservation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `booking_slot_reservation` (
  `reservation_id` int NOT NULL AUTO_INCREMENT,
  `reserved_date` date NOT NULL,
  `start_time` time(6) NOT NULL,
  `booking_id` int NOT NULL,
  PRIMARY KEY (`reservation_id`),
  KEY `FKicu22eusvp8a7olusuklnt37k` (`booking_id`),
  CONSTRAINT `FKicu22eusvp8a7olusuklnt37k` FOREIGN KEY (`booking_id`) REFERENCES `booking` (`booking_id`)
) ENGINE=InnoDB AUTO_INCREMENT=260 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `brand`
--

DROP TABLE IF EXISTS `brand`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `brand` (
  `brand_id` int NOT NULL AUTO_INCREMENT,
  `brand_name` varchar(100) DEFAULT NULL,
  `logo_url` varchar(255) DEFAULT NULL,
  `is_active` tinyint DEFAULT '1',
  PRIMARY KEY (`brand_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `catalog_item`
--

DROP TABLE IF EXISTS `catalog_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_item` (
  `item_id` int NOT NULL AUTO_INCREMENT,
  `item_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `item_type` enum('SERVICE','PART','EQUIPMENT','COMBO','MAINTENANCE_PACKAGE') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `sku` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `price` decimal(12,2) DEFAULT NULL,
  `show_price` bit(1) DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `image_url` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `warranty_duration_months` int DEFAULT '0',
  `is_active` tinyint(1) DEFAULT '1',
  `unit` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `service_service_id` bigint DEFAULT NULL,
  `combo_duration_months` int DEFAULT NULL COMMENT 'Thời hạn combo (tháng)',
  `combo_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'Mô tả chi tiết combo',
  `is_recurring` tinyint(1) DEFAULT '0' COMMENT 'Combo định kỳ hay không',
  `brand_id` int DEFAULT NULL,
  `product_line_id` int DEFAULT NULL,
  `made_in` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tax_rule_id` int DEFAULT NULL,
  `work_category_id` int DEFAULT NULL,
  `part_number` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `barcode` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `color` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`item_id`),
  KEY `FKqvnmnwkvbiovu007nykefefi0` (`service_service_id`),
  KEY `fk_catalog_item_brand1_idx` (`brand_id`),
  KEY `fk_catalog_item_product_line1_idx` (`product_line_id`),
  KEY `fk_catalog_item_tax_rule1_idx` (`tax_rule_id`),
  KEY `fk_catalog_item_work_category1_idx` (`work_category_id`),
  KEY `idx_ci_sku` (`sku`),
  KEY `idx_ci_part_number` (`part_number`),
  KEY `idx_ci_barcode` (`barcode`),
  KEY `idx_ci_type` (`item_type`),
  CONSTRAINT `fk_catalog_item_brand1` FOREIGN KEY (`brand_id`) REFERENCES `brand` (`brand_id`),
  CONSTRAINT `fk_catalog_item_product_line1` FOREIGN KEY (`product_line_id`) REFERENCES `product_line` (`product_line_id`),
  CONSTRAINT `fk_catalog_item_tax_rule1` FOREIGN KEY (`tax_rule_id`) REFERENCES `tax_rule` (`idtax_rule`),
  CONSTRAINT `fk_catalog_item_work_category1` FOREIGN KEY (`work_category_id`) REFERENCES `work_category` (`idwork_category`),
  CONSTRAINT `FKqvnmnwkvbiovu007nykefefi0` FOREIGN KEY (`service_service_id`) REFERENCES `service` (`service_id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `combo_items`
--

DROP TABLE IF EXISTS `combo_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `combo_items` (
  `combo_item_id` int NOT NULL AUTO_INCREMENT,
  `combo_id` int NOT NULL COMMENT 'ID của item có type=COMBO',
  `included_item_id` int NOT NULL COMMENT 'Item được bao gồm trong combo',
  `quantity` int DEFAULT '1' COMMENT 'Số lượng item trong combo',
  PRIMARY KEY (`combo_item_id`),
  UNIQUE KEY `uk_combo_item_unique` (`combo_id`,`included_item_id`),
  KEY `idx_combo` (`combo_id`),
  KEY `idx_included_item` (`included_item_id`),
  CONSTRAINT `combo_items_ibfk_1` FOREIGN KEY (`combo_id`) REFERENCES `catalog_item` (`item_id`) ON DELETE CASCADE,
  CONSTRAINT `combo_items_ibfk_2` FOREIGN KEY (`included_item_id`) REFERENCES `catalog_item` (`item_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Chi tiết các sản phẩm trong combo';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `commission_config`
--

DROP TABLE IF EXISTS `commission_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `commission_config` (
  `config_id` int NOT NULL AUTO_INCREMENT,
  `item_id` int NOT NULL,
  `commission_rate` decimal(5,2) NOT NULL,
  `commission_quantity_threshold` int NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_by` int NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`config_id`),
  KEY `idx_cc_item` (`item_id`),
  KEY `idx_cc_active` (`is_active`),
  CONSTRAINT `fk_cc_item` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Cấu hình hoa hồng tư vấn theo sản phẩm';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `commission_record`
--

DROP TABLE IF EXISTS `commission_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `commission_record` (
  `record_id` int NOT NULL AUTO_INCREMENT,
  `staff_id` int NOT NULL,
  `item_id` int NOT NULL,
  `issue_id` int NOT NULL,
  `quantity` int NOT NULL,
  `final_price` decimal(12,2) NOT NULL,
  `commission_rate` decimal(5,2) NOT NULL,
  `commission_value` decimal(12,2) NOT NULL,
  `period_month` char(7) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Kỳ tính hoa hồng: YYYY-MM',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`record_id`),
  KEY `idx_cr_staff_period` (`staff_id`,`period_month`),
  KEY `idx_cr_item` (`item_id`),
  KEY `fk_cr_issue` (`issue_id`),
  CONSTRAINT `fk_cr_issue` FOREIGN KEY (`issue_id`) REFERENCES `stock_issue` (`issue_id`),
  CONSTRAINT `fk_cr_item` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bản ghi hoa hồng tư vấn theo kỳ';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `customer_auth`
--

DROP TABLE IF EXISTS `customer_auth`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customer_auth` (
  `customer_auth_id` int NOT NULL AUTO_INCREMENT,
  `customer_id` int DEFAULT NULL,
  `pin_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('ACTIVE','LOCKED','INACTIVE','DELETED') COLLATE utf8mb4_unicode_ci DEFAULT 'INACTIVE',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `failed_attempt_count` int DEFAULT '0',
  `otp_attempt_count` int DEFAULT '0',
  `last_login_at` datetime DEFAULT NULL,
  PRIMARY KEY (`customer_auth_id`),
  KEY `fk_customer_auth_profile` (`customer_id`),
  CONSTRAINT `fk_customer_auth_profile` FOREIGN KEY (`customer_id`) REFERENCES `customer_profile` (`customer_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `customer_profile`
--

DROP TABLE IF EXISTS `customer_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customer_profile` (
  `customer_id` int NOT NULL AUTO_INCREMENT,
  `full_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `dob` date DEFAULT NULL,
  `gender` enum('MALE','FEMALE','OTHER') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `first_booking_at` datetime DEFAULT NULL,
  PRIMARY KEY (`customer_id`),
  UNIQUE KEY `phone` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `discount_config`
--

DROP TABLE IF EXISTS `discount_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `discount_config` (
  `config_id` int NOT NULL AUTO_INCREMENT,
  `item_id` int DEFAULT NULL COMMENT 'NULL = áp dụng cho tất cả item',
  `issue_type` enum('SERVICE_TICKET','RETAIL','WHOLESALE') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `quantity_threshold` int DEFAULT NULL COMMENT 'NULL = không theo ngưỡng số lượng',
  `discount_rate` decimal(5,2) NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_by` int NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`config_id`),
  KEY `idx_dc_item` (`item_id`),
  KEY `idx_dc_active` (`is_active`),
  CONSTRAINT `fk_dc_item` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Cấu hình chiết khấu theo loại xuất hoặc ngưỡng số lượng';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `estimate`
--

DROP TABLE IF EXISTS `estimate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `estimate` (
  `estimate_id` int NOT NULL AUTO_INCREMENT,
  `service_ticket_id` int DEFAULT NULL,
  `estimate_type` enum('INITIAL','SUPPLEMENT','REVISION','FINAL') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('DRAFT','SENT','APPROVED','REJECTED','ARCHIVED','CANCELLED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `approved_at` datetime DEFAULT NULL,
  `version` int DEFAULT '1',
  `revised_from_id` int DEFAULT NULL,
  `total_price` decimal(12,2) DEFAULT NULL,
  PRIMARY KEY (`estimate_id`),
  KEY `fk_estimate_ticket` (`service_ticket_id`),
  CONSTRAINT `fk_estimate_ticket` FOREIGN KEY (`service_ticket_id`) REFERENCES `service_ticket` (`service_ticket_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `estimate_item`
--

DROP TABLE IF EXISTS `estimate_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `estimate_item` (
  `estimate_item_id` int NOT NULL AUTO_INCREMENT,
  `estimate_id` int NOT NULL,
  `item_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `item_id` int DEFAULT NULL,
  `quantity` int DEFAULT NULL,
  `unit_price` decimal(12,2) DEFAULT NULL,
  `is_overridden` tinyint(1) DEFAULT '0',
  `override_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `warehouse_id` int DEFAULT NULL COMMENT 'Kho dự kiến xuất hàng',
  `work_category_idwork_category` int DEFAULT NULL,
  `total_price` decimal(12,2) DEFAULT NULL,
  `is_checked` tinyint(1) DEFAULT '0',
  `is_removed` tinyint(1) DEFAULT '0',
  `promotion_id` int DEFAULT NULL,
  `tax_amount` decimal(12,2) DEFAULT NULL,
  `applied_tax_rate` decimal(5,2) DEFAULT NULL,
  `unit` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `revised_from_item_id` int DEFAULT NULL,
  PRIMARY KEY (`estimate_item_id`),
  KEY `fk_estimate_item_estimate` (`estimate_id`),
  KEY `fk_estimate_item_catalog` (`item_id`),
  KEY `fk_estimate_item_warehouse` (`warehouse_id`),
  KEY `fk_estimate_item_work_category1_idx` (`work_category_idwork_category`),
  KEY `fk_estimate_item_promotion1_idx` (`promotion_id`),
  CONSTRAINT `fk_estimate_item_catalog` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`),
  CONSTRAINT `fk_estimate_item_estimate` FOREIGN KEY (`estimate_id`) REFERENCES `estimate` (`estimate_id`),
  CONSTRAINT `fk_estimate_item_promotion1` FOREIGN KEY (`promotion_id`) REFERENCES `promotion` (`promotion_id`),
  CONSTRAINT `fk_estimate_item_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse` (`warehouse_id`),
  CONSTRAINT `fk_estimate_item_work_category1` FOREIGN KEY (`work_category_idwork_category`) REFERENCES `work_category` (`idwork_category`)
) ENGINE=InnoDB AUTO_INCREMENT=725 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `feedback`
--

DROP TABLE IF EXISTS `feedback`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `feedback` (
  `feedback_id` int NOT NULL AUTO_INCREMENT,
  `service_ticket_id` int NOT NULL,
  `star_rating` int DEFAULT NULL,
  `comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `detail_feedback` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`feedback_id`),
  KEY `fk_feedback_ticket` (`service_ticket_id`),
  CONSTRAINT `fk_feedback_ticket` FOREIGN KEY (`service_ticket_id`) REFERENCES `service_ticket` (`service_ticket_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `inventory`
--

DROP TABLE IF EXISTS `inventory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inventory` (
  `inventory_id` int NOT NULL AUTO_INCREMENT,
  `warehouse_id` int NOT NULL COMMENT 'Kho',
  `item_id` int NOT NULL COMMENT 'Sản phẩm/Dịch vụ',
  `quantity` int NOT NULL DEFAULT '0' COMMENT 'Số lượng tồn hiện tại',
  `min_stock_level` int DEFAULT '0' COMMENT 'Mức tồn tối thiểu (cảnh báo)',
  `max_stock_level` int DEFAULT '0' COMMENT 'Mức tồn tối đa',
  `last_updated` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `reserved_quantity` int NOT NULL DEFAULT '0' COMMENT 'Số lượng đang được giữ chỗ (RESERVED) cho các ServiceTicket',
  PRIMARY KEY (`inventory_id`),
  UNIQUE KEY `unique_warehouse_item` (`warehouse_id`,`item_id`),
  KEY `idx_warehouse` (`warehouse_id`),
  KEY `idx_item` (`item_id`),
  KEY `idx_low_stock` (`warehouse_id`,`quantity`,`min_stock_level`),
  CONSTRAINT `inventory_ibfk_1` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse` (`warehouse_id`) ON DELETE CASCADE,
  CONSTRAINT `inventory_ibfk_2` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Quản lý tồn kho theo từng kho và sản phẩm';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `inventory_transaction`
--

DROP TABLE IF EXISTS `inventory_transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inventory_transaction` (
  `transaction_id` int NOT NULL AUTO_INCREMENT,
  `warehouse_id` int NOT NULL COMMENT 'Kho',
  `item_id` int NOT NULL COMMENT 'Sản phẩm',
  `transaction_type` enum('IN','OUT','TRANSFER_IN','TRANSFER_OUT','ADJUSTMENT') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Loại giao dịch',
  `quantity` int NOT NULL COMMENT 'Số lượng (+ hoặc -)',
  `balance_after` int NOT NULL COMMENT 'Tồn kho sau giao dịch',
  `reference_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Loại chứng từ (TRANSFER, BILL, PURCHASE...)',
  `reference_id` int DEFAULT NULL COMMENT 'ID chứng từ',
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'Ghi chú',
  `created_by` int NOT NULL COMMENT 'Người thực hiện',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`transaction_id`),
  KEY `item_id` (`item_id`),
  KEY `created_by` (`created_by`),
  KEY `idx_warehouse_item` (`warehouse_id`,`item_id`),
  KEY `idx_transaction_type` (`transaction_type`),
  KEY `idx_reference` (`reference_type`,`reference_id`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `inventory_transaction_ibfk_1` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse` (`warehouse_id`),
  CONSTRAINT `inventory_transaction_ibfk_2` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`),
  CONSTRAINT `inventory_transaction_ibfk_3` FOREIGN KEY (`created_by`) REFERENCES `staff_profile` (`staff_id`)
) ENGINE=InnoDB AUTO_INCREMENT=278 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lịch sử tất cả giao dịch xuất nhập kho (audit log)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ip_blacklist`
--

DROP TABLE IF EXISTS `ip_blacklist`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ip_blacklist` (
  `blacklist_id` int NOT NULL AUTO_INCREMENT,
  `blocked_at` datetime(6) DEFAULT NULL,
  `ip_address` varchar(45) NOT NULL,
  `is_active` bit(1) NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `blocked_by` int DEFAULT NULL,
  PRIMARY KEY (`blacklist_id`),
  UNIQUE KEY `UKg8rrnqfujqdedr48q92tb49n1` (`ip_address`),
  KEY `idx_ip` (`ip_address`),
  KEY `idx_active` (`is_active`),
  KEY `FK81xwpo39m65pf42y6voxlv9nm` (`blocked_by`),
  CONSTRAINT `FK81xwpo39m65pf42y6voxlv9nm` FOREIGN KEY (`blocked_by`) REFERENCES `staff_profile` (`staff_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `odometer_history`
--

DROP TABLE IF EXISTS `odometer_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `odometer_history` (
  `reading_id` int NOT NULL AUTO_INCREMENT,
  `vehicle_id` int NOT NULL,
  `reading` int NOT NULL COMMENT 'Số công tơ mét (km)',
  `recorded_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời gian ghi nhận',
  `recorded_by` int NOT NULL COMMENT 'Staff ID người ghi nhận',
  `service_ticket_id` int DEFAULT NULL COMMENT 'Liên kết với service ticket',
  `rollback_detected` tinyint(1) DEFAULT '0' COMMENT 'TRUE nếu số mới < số cũ',
  `previous_reading` int DEFAULT NULL COMMENT 'Số công tơ mét lần trước',
  PRIMARY KEY (`reading_id`),
  KEY `idx_vehicle_id` (`vehicle_id`),
  KEY `idx_recorded_at` (`recorded_at`),
  KEY `idx_rollback_detected` (`rollback_detected`),
  KEY `fk_odometer_staff` (`recorded_by`),
  KEY `fk_odometer_service_ticket` (`service_ticket_id`),
  CONSTRAINT `fk_odometer_service_ticket` FOREIGN KEY (`service_ticket_id`) REFERENCES `service_ticket` (`service_ticket_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_odometer_staff` FOREIGN KEY (`recorded_by`) REFERENCES `staff_profile` (`staff_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_odometer_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`vehicle_id`) ON DELETE CASCADE,
  CONSTRAINT `chk_reading_positive` CHECK ((`reading` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lịch sử số công tơ mét - phát hiện rollback';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `otp_verification`
--

DROP TABLE IF EXISTS `otp_verification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `otp_verification` (
  `otp_id` int NOT NULL AUTO_INCREMENT,
  `auth_type` enum('STAFF','CUSTOMER') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `auth_id` int DEFAULT NULL,
  `otp_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `expired_at` datetime DEFAULT NULL,
  `verified_at` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`otp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `part_warranty`
--

DROP TABLE IF EXISTS `part_warranty`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `part_warranty` (
  `warranty_id` int NOT NULL AUTO_INCREMENT,
  `estimate_item_id` int NOT NULL,
  `vehicle_id` int NOT NULL,
  `customer_id` int NOT NULL,
  `warranty_start_date` date DEFAULT NULL,
  `warranty_end_date` date DEFAULT NULL,
  `status` enum('ACTIVE','EXPIRED','CLAIMED','VOID') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (`warranty_id`),
  KEY `fk_warranty_item` (`estimate_item_id`),
  KEY `fk_warranty_vehicle` (`vehicle_id`),
  KEY `fk_warranty_customer` (`customer_id`),
  CONSTRAINT `fk_warranty_customer` FOREIGN KEY (`customer_id`) REFERENCES `customer_profile` (`customer_id`),
  CONSTRAINT `fk_warranty_item` FOREIGN KEY (`estimate_item_id`) REFERENCES `estimate_item` (`estimate_item_id`),
  CONSTRAINT `fk_warranty_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`vehicle_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `payment_transaction`
--

DROP TABLE IF EXISTS `payment_transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_transaction` (
  `transaction_id` int NOT NULL AUTO_INCREMENT,
  `bill_id` int NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `method` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('SUCCESS','FAILED','REFUNDED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `paid_at` datetime DEFAULT NULL,
  PRIMARY KEY (`transaction_id`),
  KEY `fk_transaction_bill` (`bill_id`),
  CONSTRAINT `fk_transaction_bill` FOREIGN KEY (`bill_id`) REFERENCES `service_bill` (`bill_id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_line`
--

DROP TABLE IF EXISTS `product_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_line` (
  `product_line_id` int NOT NULL AUTO_INCREMENT,
  `brand_id` int NOT NULL,
  `line_name` varchar(100) DEFAULT NULL,
  `is_active` tinyint DEFAULT '1',
  PRIMARY KEY (`product_line_id`),
  KEY `fk_product_line_brand1_idx` (`brand_id`),
  CONSTRAINT `fk_product_line_brand1` FOREIGN KEY (`brand_id`) REFERENCES `brand` (`brand_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `promotion`
--

DROP TABLE IF EXISTS `promotion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `promotion` (
  `promotion_id` int NOT NULL AUTO_INCREMENT,
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `type` enum('PERCENT','BUY_X_GET_Y') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `discount_percent` decimal(12,2) DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT '1',
  `apply_to` enum('ALL','SPECIFIC') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `buy_item_id` int DEFAULT NULL,
  `buy_quantity` int DEFAULT NULL,
  `get_item_id` int DEFAULT NULL,
  `get_quantity` int DEFAULT NULL,
  `target_type` enum('ALL','SPECIFIC') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `min_order_value` decimal(12,2) DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `usage_limit` int DEFAULT NULL,
  `used_count` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`promotion_id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `promotion_customer`
--

DROP TABLE IF EXISTS `promotion_customer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `promotion_customer` (
  `promotion_customer_id` int NOT NULL AUTO_INCREMENT,
  `promotion_promotion_id` int NOT NULL,
  `customer_profile_customer_id` int NOT NULL,
  PRIMARY KEY (`promotion_customer_id`),
  KEY `fk_promotion_customer_promotion1_idx` (`promotion_promotion_id`),
  KEY `fk_promotion_customer_customer_profile1_idx` (`customer_profile_customer_id`),
  CONSTRAINT `fk_promotion_customer_customer_profile1` FOREIGN KEY (`customer_profile_customer_id`) REFERENCES `customer_profile` (`customer_id`),
  CONSTRAINT `fk_promotion_customer_promotion1` FOREIGN KEY (`promotion_promotion_id`) REFERENCES `promotion` (`promotion_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `promotion_item`
--

DROP TABLE IF EXISTS `promotion_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `promotion_item` (
  `promotion_item_id` int NOT NULL AUTO_INCREMENT,
  `promotion_promotion_id` int NOT NULL,
  `catalog_item_item_id` int NOT NULL,
  PRIMARY KEY (`promotion_item_id`),
  KEY `fk_promotion_item_promotion1_idx` (`promotion_promotion_id`),
  KEY `fk_promotion_item_catalog_item1_idx` (`catalog_item_item_id`),
  CONSTRAINT `fk_promotion_item_catalog_item1` FOREIGN KEY (`catalog_item_item_id`) REFERENCES `catalog_item` (`item_id`),
  CONSTRAINT `fk_promotion_item_promotion1` FOREIGN KEY (`promotion_promotion_id`) REFERENCES `promotion` (`promotion_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `promotion_usage`
--

DROP TABLE IF EXISTS `promotion_usage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `promotion_usage` (
  `usage_id` int NOT NULL AUTO_INCREMENT,
  `promotion_id` int NOT NULL,
  `customer_id` int DEFAULT NULL,
  `estimate_id` int DEFAULT NULL,
  `discount_amount` decimal(12,2) DEFAULT NULL,
  `used_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`usage_id`),
  KEY `fk_promotion_usage_promotion1_idx` (`promotion_id`),
  KEY `fk_promotion_usage_estimate1_idx` (`estimate_id`),
  CONSTRAINT `fk_promotion_usage_estimate1` FOREIGN KEY (`estimate_id`) REFERENCES `estimate` (`estimate_id`),
  CONSTRAINT `fk_promotion_usage_promotion1` FOREIGN KEY (`promotion_id`) REFERENCES `promotion` (`promotion_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `return_entry`
--

DROP TABLE IF EXISTS `return_entry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `return_entry` (
  `return_id` int NOT NULL AUTO_INCREMENT,
  `return_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Mã phiếu trả: TH-YYYYMMDD-seq',
  `warehouse_id` int NOT NULL,
  `return_reason` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Lý do hoàn hàng chung',
  `source_issue_id` int DEFAULT NULL COMMENT 'Liên kết phiếu xuất gốc nếu có',
  `status` enum('DRAFT','CONFIRMED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `confirmed_by` int DEFAULT NULL,
  `confirmed_at` datetime DEFAULT NULL,
  `created_by` int NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `return_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CUSTOMER_RETURN',
  PRIMARY KEY (`return_id`),
  UNIQUE KEY `return_code` (`return_code`),
  KEY `idx_re_code` (`return_code`),
  KEY `idx_re_status` (`status`),
  KEY `fk_re_warehouse` (`warehouse_id`),
  KEY `fk_re_source` (`source_issue_id`),
  CONSTRAINT `fk_re_source` FOREIGN KEY (`source_issue_id`) REFERENCES `stock_issue` (`issue_id`),
  CONSTRAINT `fk_re_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Phiếu hoàn hàng (header)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `return_entry_item`
--

DROP TABLE IF EXISTS `return_entry_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `return_entry_item` (
  `return_item_id` int NOT NULL AUTO_INCREMENT,
  `return_id` int NOT NULL,
  `item_id` int NOT NULL,
  `quantity` int NOT NULL,
  `condition_note` text COLLATE utf8mb4_unicode_ci,
  `is_exchange_item` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`return_item_id`),
  KEY `idx_rei_return` (`return_id`),
  KEY `idx_rei_item` (`item_id`),
  CONSTRAINT `fk_rei_item` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`),
  CONSTRAINT `fk_rei_return` FOREIGN KEY (`return_id`) REFERENCES `return_entry` (`return_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Chi tiết từng sản phẩm trong phiếu hoàn hàng';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `role`
--

DROP TABLE IF EXISTS `role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role` (
  `role_id` int NOT NULL AUTO_INCREMENT,
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `role_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `role_code` (`role_code`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `safety_inspection`
--

DROP TABLE IF EXISTS `safety_inspection`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `safety_inspection` (
  `inspection_id` int NOT NULL AUTO_INCREMENT,
  `service_ticket_id` int NOT NULL,
  `technician_id` int DEFAULT NULL,
  `general_notes` text COLLATE utf8mb4_unicode_ci COMMENT 'Ghi chú chung về tình trạng xe',
  `inspection_status` enum('PENDING','COMPLETED','SKIPPED') COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING' COMMENT 'Trạng thái kiểm tra',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời gian tạo inspection',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời gian cập nhật',
  `technician_notes` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`inspection_id`),
  UNIQUE KEY `uk_service_ticket` (`service_ticket_id`),
  KEY `idx_technician` (`technician_id`),
  KEY `idx_status` (`inspection_status`),
  CONSTRAINT `fk_inspection_service_ticket` FOREIGN KEY (`service_ticket_id`) REFERENCES `service_ticket` (`service_ticket_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=57 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bảng lưu thông tin kiểm tra an toàn xe';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `safety_inspection_item`
--

DROP TABLE IF EXISTS `safety_inspection_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `safety_inspection_item` (
  `item_id` int NOT NULL AUTO_INCREMENT,
  `inspection_id` int NOT NULL,
  `work_category_id` int DEFAULT NULL,
  `item_status` enum('GOOD','WARNING','REPLACE') COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Trạng thái: Tốt/Lưu ý/Thay',
  `advisor_note` text COLLATE utf8mb4_unicode_ci,
  `custom_category_id` int DEFAULT NULL,
  PRIMARY KEY (`item_id`),
  UNIQUE KEY `uk_inspection_work_category` (`inspection_id`,`work_category_id`),
  KEY `idx_inspection` (`inspection_id`),
  KEY `idx_status` (`item_status`),
  KEY `idx_work_category` (`work_category_id`),
  KEY `fk_sii_custom_category` (`custom_category_id`),
  CONSTRAINT `fk_inspection_item_work_category` FOREIGN KEY (`work_category_id`) REFERENCES `work_category` (`idwork_category`) ON DELETE RESTRICT,
  CONSTRAINT `fk_item_inspection` FOREIGN KEY (`inspection_id`) REFERENCES `safety_inspection` (`inspection_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_sii_custom_category` FOREIGN KEY (`custom_category_id`) REFERENCES `ticket_custom_category` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=766 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bảng lưu kết quả kiểm tra các hạng mục an toàn';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `safety_inspection_tire`
--

DROP TABLE IF EXISTS `safety_inspection_tire`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `safety_inspection_tire` (
  `tire_id` int NOT NULL AUTO_INCREMENT,
  `inspection_id` int NOT NULL,
  `tire_position` enum('FRONT_LEFT','FRONT_RIGHT','REAR_LEFT','REAR_RIGHT','SPARE') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Vị trí lốp (bao gồm lốp dự phòng)',
  `tread_depth` decimal(5,2) DEFAULT NULL COMMENT 'Độ mòn lốp (mm)',
  `pressure` decimal(5,2) DEFAULT NULL COMMENT 'Áp suất lốp (psi hoặc bar)',
  `pressure_unit` enum('PSI','BAR') COLLATE utf8mb4_unicode_ci DEFAULT 'PSI' COMMENT 'Đơn vị áp suất',
  `tire_specification` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Thông số lốp (e.g., 225/65R17)',
  `recommended_tire_size` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Size lốp khuyến cáo',
  `recommended_pressure` decimal(5,2) DEFAULT NULL COMMENT 'Áp suất lốp khuyến cáo',
  `recommended_pressure_unit` enum('PSI','BAR') COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Đơn vị áp suất khuyến cáo',
  PRIMARY KEY (`tire_id`),
  UNIQUE KEY `uk_inspection_position` (`inspection_id`,`tire_position`),
  KEY `idx_inspection` (`inspection_id`),
  CONSTRAINT `fk_tire_inspection` FOREIGN KEY (`inspection_id`) REFERENCES `safety_inspection` (`inspection_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=212 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bảng lưu thông tin đo lốp xe';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `service`
--

DROP TABLE IF EXISTS `service`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `service` (
  `service_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `display_from` datetime DEFAULT NULL,
  `display_price` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `display_to` datetime DEFAULT NULL,
  `full_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `media_thumbnail` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `short_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `show_price` bit(1) NOT NULL,
  `status` enum('ACTIVE','DELETED','DRAFT','INACTIVE') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `estimate_time` int DEFAULT NULL,
  PRIMARY KEY (`service_id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `service_bill`
--

DROP TABLE IF EXISTS `service_bill`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `service_bill` (
  `bill_id` int NOT NULL AUTO_INCREMENT,
  `service_ticket_id` int NOT NULL,
  `sub_total` decimal(12,2) DEFAULT NULL,
  `discount_amount` decimal(12,2) DEFAULT NULL,
  `final_amount` decimal(12,2) DEFAULT NULL,
  `payment_status` enum('UNPAID','PAID','REFUNDED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'UNPAID',
  `paid_at` datetime DEFAULT NULL,
  `warehouse_id` int DEFAULT NULL COMMENT 'Kho xuất hàng cho hóa đơn này',
  `estimate_id` int NOT NULL,
  `promotion_id` int DEFAULT NULL,
  PRIMARY KEY (`bill_id`),
  KEY `fk_bill_ticket` (`service_ticket_id`),
  KEY `fk_service_bill_warehouse` (`warehouse_id`),
  KEY `fk_service_bill_estimate1_idx` (`estimate_id`),
  KEY `fk_service_bill_promotion1_idx` (`promotion_id`),
  CONSTRAINT `fk_bill_ticket` FOREIGN KEY (`service_ticket_id`) REFERENCES `service_ticket` (`service_ticket_id`),
  CONSTRAINT `fk_service_bill_estimate1` FOREIGN KEY (`estimate_id`) REFERENCES `estimate` (`estimate_id`),
  CONSTRAINT `fk_service_bill_promotion1` FOREIGN KEY (`promotion_id`) REFERENCES `promotion` (`promotion_id`),
  CONSTRAINT `fk_service_bill_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse` (`warehouse_id`)
) ENGINE=InnoDB AUTO_INCREMENT=40 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `service_item`
--

DROP TABLE IF EXISTS `service_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `service_item` (
  `service_item_id` bigint NOT NULL AUTO_INCREMENT,
  `category_id` bigint NOT NULL,
  `service_id` bigint NOT NULL,
  PRIMARY KEY (`service_item_id`),
  KEY `FK82rintgbc4qe75p45eerua9ix` (`service_id`),
  CONSTRAINT `FK82rintgbc4qe75p45eerua9ix` FOREIGN KEY (`service_id`) REFERENCES `service` (`service_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `service_media`
--

DROP TABLE IF EXISTS `service_media`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `service_media` (
  `service_media_id` bigint NOT NULL AUTO_INCREMENT,
  `display_order` int NOT NULL,
  `media_description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `media_type` enum('IMAGE','VIDEO') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `media_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `service_id` bigint NOT NULL,
  PRIMARY KEY (`service_media_id`),
  KEY `FKacjlme4j60r9dki6l1w3rhodk` (`service_id`),
  CONSTRAINT `FKacjlme4j60r9dki6l1w3rhodk` FOREIGN KEY (`service_id`) REFERENCES `service` (`service_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5014 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `service_reminder`
--

DROP TABLE IF EXISTS `service_reminder`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `service_reminder` (
  `reminder_id` int NOT NULL AUTO_INCREMENT,
  `service_ticket_id` int NOT NULL,
  `vehicle_id` int NOT NULL,
  `customer_id` int NOT NULL,
  `staff_id` int DEFAULT NULL,
  `reminder_date` date DEFAULT NULL,
  `reminder_time` time DEFAULT NULL,
  `note` text,
  `status` enum('PENDING','NOTIFIED','SKIPPED','CANCELLED','CONFIRMED','BOOKED') DEFAULT 'PENDING',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `reason` text,
  `booking_id` int DEFAULT NULL,
  PRIMARY KEY (`reminder_id`),
  KEY `fk_service_reminder_vehicle1_idx` (`vehicle_id`),
  KEY `fk_service_reminder_customer_profile1_idx` (`customer_id`),
  KEY `fk_service_reminder_staff_profile1_idx` (`staff_id`),
  KEY `fk_service_reminder_booking1_idx` (`booking_id`),
  KEY `fk_service_reminder_service_ticket2_idx` (`service_ticket_id`),
  CONSTRAINT `fk_service_reminder_booking1` FOREIGN KEY (`booking_id`) REFERENCES `booking` (`booking_id`),
  CONSTRAINT `fk_service_reminder_customer_profile1` FOREIGN KEY (`customer_id`) REFERENCES `customer_profile` (`customer_id`),
  CONSTRAINT `fk_service_reminder_service_ticket2` FOREIGN KEY (`service_ticket_id`) REFERENCES `service_ticket` (`service_ticket_id`),
  CONSTRAINT `fk_service_reminder_staff_profile1` FOREIGN KEY (`staff_id`) REFERENCES `staff_profile` (`staff_id`),
  CONSTRAINT `fk_service_reminder_vehicle1` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`vehicle_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `service_rule`
--

DROP TABLE IF EXISTS `service_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `service_rule` (
  `rule_id` int NOT NULL AUTO_INCREMENT,
  `vehicle_type_pattern` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Pattern khớp tên xe (LIKE hoặc regex)',
  `km_threshold` int NOT NULL COMMENT 'Ngưỡng km để gợi ý dịch vụ',
  `suggested_item_ids` json NOT NULL COMMENT 'Danh sách item_id được gợi ý: [1, 2, 3]',
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Lý do gợi ý hiển thị cho tư vấn viên',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_by` int NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`rule_id`),
  KEY `idx_sr_active` (`is_active`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Quy tắc gợi ý dịch vụ theo loại xe và số km';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `service_ticket`
--

DROP TABLE IF EXISTS `service_ticket`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `service_ticket` (
  `service_ticket_id` int NOT NULL AUTO_INCREMENT,
  `vehicle_id` int NOT NULL,
  `customer_id` int NOT NULL,
  `booking_id` int DEFAULT NULL,
  `customer_request` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `technician_notes` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `received_at` datetime DEFAULT NULL,
  `delivered_at` datetime DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL COMMENT 'Thời gian hoàn thành service ticket',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `created_by` int DEFAULT NULL,
  `is_deleted` tinyint(1) DEFAULT '0',
  `ticket_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Mã service ticket (ST_XXXXXX)',
  `ticket_status` enum('CREATED','INSPECTING','PENDING','INSPECTED','ESTIMATED','REPAIRING','CANCELLED','COMPLETED','PAID') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `check_in_notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'Ghi chú check-in',
  `immutable` tinyint(1) DEFAULT '0' COMMENT 'Không cho sửa sau check-in',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời gian cập nhật',
  `is_printed` tinyint(1) DEFAULT '0',
  `printed_at` datetime DEFAULT NULL,
  `safety_inspection_enabled` tinyint(1) DEFAULT '0' COMMENT 'Flag to indicate if safety inspection has been enabled for this ticket',
  `queue_number` int DEFAULT NULL,
  `estimated_delivery_at` datetime DEFAULT NULL COMMENT 'Thời gian hẹn lấy xe dự kiến (advisor set)',
  PRIMARY KEY (`service_ticket_id`),
  UNIQUE KEY `uk_ticket_code` (`ticket_code`),
  UNIQUE KEY `uk_booking_id` (`booking_id`),
  KEY `fk_ticket_vehicle` (`vehicle_id`),
  KEY `fk_ticket_customer` (`customer_id`),
  KEY `fk_ticket_booking` (`booking_id`),
  KEY `fk_ticket_staff` (`created_by`),
  KEY `idx_ticket_status` (`ticket_status`),
  KEY `idx_received_at` (`received_at`),
  KEY `idx_booking_id` (`booking_id`),
  KEY `idx_vehicle_id` (`vehicle_id`),
  CONSTRAINT `fk_ticket_booking` FOREIGN KEY (`booking_id`) REFERENCES `booking` (`booking_id`),
  CONSTRAINT `fk_ticket_customer` FOREIGN KEY (`customer_id`) REFERENCES `customer_profile` (`customer_id`),
  CONSTRAINT `fk_ticket_staff` FOREIGN KEY (`created_by`) REFERENCES `staff_profile` (`staff_id`),
  CONSTRAINT `fk_ticket_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`vehicle_id`)
) ENGINE=InnoDB AUTO_INCREMENT=56 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `service_ticket_assignment`
--

DROP TABLE IF EXISTS `service_ticket_assignment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `service_ticket_assignment` (
  `assignment_id` int NOT NULL AUTO_INCREMENT,
  `service_ticket_id` int NOT NULL,
  `staff_id` int NOT NULL,
  `role_in_ticket` enum('ADVISOR','TECHNICIAN','RECEPTION') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `assigned_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `is_primary` tinyint(1) DEFAULT '0',
  `status` enum('PENDING','ACTIVE','DONE','CANCELLED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `note` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Cmt',
  PRIMARY KEY (`assignment_id`),
  KEY `fk_assignment_ticket` (`service_ticket_id`),
  KEY `fk_assignment_staff` (`staff_id`),
  CONSTRAINT `fk_assignment_staff` FOREIGN KEY (`staff_id`) REFERENCES `staff_profile` (`staff_id`),
  CONSTRAINT `fk_assignment_ticket` FOREIGN KEY (`service_ticket_id`) REFERENCES `service_ticket` (`service_ticket_id`)
) ENGINE=InnoDB AUTO_INCREMENT=118 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `service_ticket_log`
--

DROP TABLE IF EXISTS `service_ticket_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `service_ticket_log` (
  `log_id` int NOT NULL AUTO_INCREMENT,
  `service_ticket_id` int NOT NULL,
  `action` varchar(50) DEFAULT NULL,
  `old_value` text,
  `new_value` text,
  `changed_by` int DEFAULT NULL,
  `changed_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `service_ticket_status_history`
--

DROP TABLE IF EXISTS `service_ticket_status_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `service_ticket_status_history` (
  `history_id` int NOT NULL AUTO_INCREMENT,
  `service_ticket_id` int NOT NULL,
  `old_status` varchar(50) DEFAULT NULL,
  `new_status` varchar(50) DEFAULT NULL,
  `changed_by` int DEFAULT NULL,
  `changed_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`history_id`),
  KEY `fk_status_history_ticket` (`service_ticket_id`),
  KEY `fk_status_history_staff` (`changed_by`),
  CONSTRAINT `fk_status_history_staff` FOREIGN KEY (`changed_by`) REFERENCES `staff_profile` (`staff_id`),
  CONSTRAINT `fk_status_history_ticket` FOREIGN KEY (`service_ticket_id`) REFERENCES `service_ticket` (`service_ticket_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `spec_attribute`
--

DROP TABLE IF EXISTS `spec_attribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `spec_attribute` (
  `attribute_id` int NOT NULL AUTO_INCREMENT,
  `attribute_code` varchar(45) DEFAULT NULL,
  `display_name` varchar(45) DEFAULT NULL,
  `unit` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`attribute_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `specification`
--

DROP TABLE IF EXISTS `specification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `specification` (
  `spec_id` int NOT NULL AUTO_INCREMENT,
  `item_id` int NOT NULL,
  `attribute_id` int NOT NULL,
  `spec_value` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`spec_id`),
  KEY `fk_specification_catalog_item1_idx` (`item_id`),
  KEY `fk_specification_spec_attribute1_idx` (`attribute_id`),
  CONSTRAINT `fk_specification_catalog_item1` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`),
  CONSTRAINT `fk_specification_spec_attribute1` FOREIGN KEY (`attribute_id`) REFERENCES `spec_attribute` (`attribute_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `staff_auth`
--

DROP TABLE IF EXISTS `staff_auth`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `staff_auth` (
  `created_at` datetime(6) DEFAULT NULL,
  `failed_login_count` bigint NOT NULL,
  `last_login_at` datetime(6) DEFAULT NULL,
  `locked_until` datetime(6) DEFAULT NULL,
  `staff_auth_id` bigint NOT NULL AUTO_INCREMENT,
  `staff_id` int DEFAULT NULL,
  `auth_provider` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `google_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`staff_auth_id`),
  KEY `staff_auth_staff_profile_staff_id_fk` (`staff_id`),
  CONSTRAINT `staff_auth_staff_profile_staff_id_fk` FOREIGN KEY (`staff_id`) REFERENCES `staff_profile` (`staff_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `staff_notification`
--

DROP TABLE IF EXISTS `staff_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `staff_notification` (
  `notification_id` int NOT NULL AUTO_INCREMENT,
  `staff_id` int DEFAULT NULL COMMENT 'NULL = broadcast tất cả nhân viên',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `message` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `notification_type` enum('INFO','WARNING','URGENT') COLLATE utf8mb4_unicode_ci DEFAULT 'INFO',
  `is_read` tinyint(1) DEFAULT '0',
  `sent_by` int NOT NULL,
  `sent_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`notification_id`),
  KEY `sent_by` (`sent_by`),
  KEY `idx_staff_read` (`staff_id`,`is_read`),
  KEY `idx_sent_at` (`sent_at`),
  CONSTRAINT `staff_notification_ibfk_1` FOREIGN KEY (`staff_id`) REFERENCES `staff_profile` (`staff_id`) ON DELETE CASCADE,
  CONSTRAINT `staff_notification_ibfk_2` FOREIGN KEY (`sent_by`) REFERENCES `staff_profile` (`staff_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `staff_profile`
--

DROP TABLE IF EXISTS `staff_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `staff_profile` (
  `staff_id` int NOT NULL AUTO_INCREMENT,
  `avatar` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `dob` date DEFAULT NULL,
  `full_name` varchar(255) DEFAULT NULL,
  `gender` varchar(255) DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `position` varchar(255) DEFAULT NULL,
  `employee_no` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`staff_id`),
  UNIQUE KEY `employee_no` (`employee_no`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `staff_role`
--

DROP TABLE IF EXISTS `staff_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `staff_role` (
  `staff_id` int NOT NULL,
  `role_id` int NOT NULL,
  PRIMARY KEY (`staff_id`,`role_id`),
  KEY `fk_staff_role_role` (`role_id`),
  CONSTRAINT `fk_staff_role_profile` FOREIGN KEY (`staff_id`) REFERENCES `staff_profile` (`staff_id`),
  CONSTRAINT `fk_staff_role_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_allocation`
--

DROP TABLE IF EXISTS `stock_allocation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_allocation` (
  `allocation_id` int NOT NULL AUTO_INCREMENT,
  `service_ticket_id` int NOT NULL,
  `issue_id` int DEFAULT NULL,
  `estimate_item_id` int NOT NULL,
  `estimate_id` int NOT NULL,
  `warehouse_id` int NOT NULL,
  `item_id` int NOT NULL,
  `quantity` int NOT NULL,
  `status` enum('RESERVED','COMMITTED','RELEASED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'RESERVED',
  `created_by` int DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`allocation_id`),
  KEY `idx_alloc_ticket` (`service_ticket_id`),
  KEY `idx_alloc_item` (`warehouse_id`,`item_id`),
  KEY `idx_alloc_status` (`status`),
  KEY `fk_alloc_item` (`item_id`),
  KEY `idx_alloc_issue` (`issue_id`),
  CONSTRAINT `fk_alloc_item` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`),
  CONSTRAINT `fk_alloc_ticket` FOREIGN KEY (`service_ticket_id`) REFERENCES `service_ticket` (`service_ticket_id`),
  CONSTRAINT `fk_alloc_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse` (`warehouse_id`),
  CONSTRAINT `fk_stock_allocation_issue` FOREIGN KEY (`issue_id`) REFERENCES `stock_issue` (`issue_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=435 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Giữ chỗ hàng (reserve) theo ServiceTicket — trạng thái RESERVED/COMMITTED/RELEASED';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_entry`
--

DROP TABLE IF EXISTS `stock_entry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_entry` (
  `entry_id` int NOT NULL AUTO_INCREMENT,
  `entry_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Mã phiếu nhập: NK-YYYYMMDD-seq',
  `warehouse_id` int NOT NULL,
  `supplier_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Nhà cung cấp',
  `entry_date` date NOT NULL COMMENT 'Ngày nhập hàng',
  `status` enum('DRAFT','CONFIRMED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `notes` text COLLATE utf8mb4_unicode_ci,
  `confirmed_by` int DEFAULT NULL,
  `confirmed_at` datetime DEFAULT NULL,
  `created_by` int NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`entry_id`),
  UNIQUE KEY `entry_code` (`entry_code`),
  KEY `idx_entry_code` (`entry_code`),
  KEY `idx_entry_wh` (`warehouse_id`),
  KEY `idx_entry_status` (`status`),
  CONSTRAINT `fk_entry_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse` (`warehouse_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Phiếu nhập kho (header)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_entry_item`
--

DROP TABLE IF EXISTS `stock_entry_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_entry_item` (
  `entry_item_id` int NOT NULL AUTO_INCREMENT,
  `entry_id` int NOT NULL,
  `item_id` int NOT NULL,
  `quantity` int NOT NULL,
  `import_price` decimal(12,2) NOT NULL COMMENT 'Giá nhập lô này',
  `markup_multiplier` decimal(6,4) NOT NULL DEFAULT '1.0000' COMMENT 'Hệ số markup fallback: dùng khi warehouse_pricing chưa cấu hình',
  `remaining_quantity` int NOT NULL DEFAULT '0' COMMENT 'Số lượng còn lại trong lô (dùng cho Weighted Average)',
  `notes` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`entry_item_id`),
  KEY `idx_sei_entry` (`entry_id`),
  KEY `idx_sei_item` (`item_id`),
  CONSTRAINT `fk_sei_entry` FOREIGN KEY (`entry_id`) REFERENCES `stock_entry` (`entry_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_sei_item` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Chi tiết từng phụ tùng trong phiếu nhập kho';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_issue`
--

DROP TABLE IF EXISTS `stock_issue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_issue` (
  `issue_id` int NOT NULL AUTO_INCREMENT,
  `issue_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Mã phiếu xuất: XK-YYYYMMDD-seq',
  `warehouse_id` int NOT NULL,
  `issue_type` enum('SERVICE_TICKET','RETAIL','WHOLESALE') COLLATE utf8mb4_unicode_ci NOT NULL,
  `issue_reason` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `service_ticket_id` int DEFAULT NULL COMMENT 'Liên kết nếu xuất từ commit allocation',
  `discount_rate` decimal(5,2) NOT NULL DEFAULT '0.00',
  `status` enum('DRAFT','CONFIRMED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `confirmed_by` int DEFAULT NULL,
  `confirmed_at` datetime DEFAULT NULL,
  `created_by` int NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`issue_id`),
  UNIQUE KEY `issue_code` (`issue_code`),
  KEY `idx_issue_code` (`issue_code`),
  KEY `idx_issue_ticket` (`service_ticket_id`),
  KEY `idx_issue_status` (`status`),
  KEY `fk_issue_warehouse` (`warehouse_id`),
  CONSTRAINT `fk_issue_ticket` FOREIGN KEY (`service_ticket_id`) REFERENCES `service_ticket` (`service_ticket_id`),
  CONSTRAINT `fk_issue_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse` (`warehouse_id`)
) ENGINE=InnoDB AUTO_INCREMENT=66 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Phiếu xuất kho';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_issue_item`
--

DROP TABLE IF EXISTS `stock_issue_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_issue_item` (
  `issue_item_id` int NOT NULL AUTO_INCREMENT,
  `issue_id` int NOT NULL,
  `item_id` int NOT NULL,
  `entry_item_id` int NOT NULL DEFAULT '0' COMMENT 'Ref đến stock_entry_item — lô nhập tương ứng (FIFO)',
  `quantity` int NOT NULL,
  `export_price` decimal(12,2) NOT NULL,
  `estimate_unit_price` decimal(12,2) DEFAULT NULL,
  `import_price` decimal(12,2) NOT NULL COMMENT 'Snapshot giá nhập tại thời điểm xuất',
  `discount_rate` decimal(5,2) NOT NULL DEFAULT '0.00',
  `final_price` decimal(12,2) NOT NULL,
  `gross_profit` decimal(12,2) GENERATED ALWAYS AS (((`final_price` - `import_price`) * `quantity`)) STORED COMMENT 'Lãi gộp = (final_price - import_price) × quantity',
  PRIMARY KEY (`issue_item_id`),
  KEY `idx_sii_issue` (`issue_id`),
  KEY `idx_sii_item` (`item_id`),
  KEY `idx_sii_entry_item` (`entry_item_id`),
  CONSTRAINT `fk_sii_issue` FOREIGN KEY (`issue_id`) REFERENCES `stock_issue` (`issue_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_sii_item` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`)
) ENGINE=InnoDB AUTO_INCREMENT=282 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Chi tiết mặt hàng trong phiếu xuất kho';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_transfer`
--

DROP TABLE IF EXISTS `stock_transfer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_transfer` (
  `transfer_id` int NOT NULL AUTO_INCREMENT,
  `transfer_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Mã phiếu chuyển kho (PXK-001)',
  `from_warehouse_id` int NOT NULL COMMENT 'Kho xuất',
  `to_warehouse_id` int NOT NULL COMMENT 'Kho nhập',
  `transfer_type` enum('TRANSFER','RETURN','ADJUSTMENT') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'TRANSFER' COMMENT 'Loại: Chuyển kho, Trả hàng, Điều chỉnh',
  `status` enum('DRAFT','PENDING','APPROVED','IN_TRANSIT','COMPLETED','CANCELLED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `requested_by` int NOT NULL COMMENT 'Người yêu cầu',
  `approved_by` int DEFAULT NULL COMMENT 'Người duyệt',
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'Ghi chú',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `approved_at` datetime DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`transfer_id`),
  UNIQUE KEY `transfer_code` (`transfer_code`),
  KEY `requested_by` (`requested_by`),
  KEY `approved_by` (`approved_by`),
  KEY `idx_transfer_code` (`transfer_code`),
  KEY `idx_status` (`status`),
  KEY `idx_from_warehouse` (`from_warehouse_id`),
  KEY `idx_to_warehouse` (`to_warehouse_id`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `stock_transfer_ibfk_1` FOREIGN KEY (`from_warehouse_id`) REFERENCES `warehouse` (`warehouse_id`),
  CONSTRAINT `stock_transfer_ibfk_2` FOREIGN KEY (`to_warehouse_id`) REFERENCES `warehouse` (`warehouse_id`),
  CONSTRAINT `stock_transfer_ibfk_3` FOREIGN KEY (`requested_by`) REFERENCES `staff_profile` (`staff_id`),
  CONSTRAINT `stock_transfer_ibfk_4` FOREIGN KEY (`approved_by`) REFERENCES `staff_profile` (`staff_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Quản lý phiếu chuyển kho giữa các kho';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_transfer_details`
--

DROP TABLE IF EXISTS `stock_transfer_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_transfer_details` (
  `transfer_detail_id` int NOT NULL AUTO_INCREMENT,
  `transfer_id` int NOT NULL COMMENT 'Phiếu chuyển kho',
  `item_id` int NOT NULL COMMENT 'Sản phẩm',
  `quantity` int NOT NULL COMMENT 'Số lượng chuyển',
  `unit_price` decimal(12,2) DEFAULT NULL COMMENT 'Giá tại thời điểm chuyển',
  `notes` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Ghi chú cho item này',
  PRIMARY KEY (`transfer_detail_id`),
  KEY `idx_transfer` (`transfer_id`),
  KEY `idx_item` (`item_id`),
  CONSTRAINT `stock_transfer_details_ibfk_1` FOREIGN KEY (`transfer_id`) REFERENCES `stock_transfer` (`transfer_id`) ON DELETE CASCADE,
  CONSTRAINT `stock_transfer_details_ibfk_2` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Chi tiết các sản phẩm trong phiếu chuyển kho';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tax_rule`
--

DROP TABLE IF EXISTS `tax_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tax_rule` (
  `idtax_rule` int NOT NULL AUTO_INCREMENT,
  `tax_code` varchar(45) NOT NULL,
  `tax_name` varchar(100) DEFAULT NULL,
  `tax_rate` decimal(5,2) DEFAULT NULL,
  `item_type` varchar(45) DEFAULT NULL,
  `effective_from` date NOT NULL,
  `effective_to` date DEFAULT NULL,
  `is_active` tinyint DEFAULT '1',
  PRIMARY KEY (`idtax_rule`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ticket_custom_category`
--

DROP TABLE IF EXISTS `ticket_custom_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ticket_custom_category` (
  `id` int NOT NULL AUTO_INCREMENT,
  `inspection_id` int NOT NULL,
  `category_name` varchar(255) NOT NULL,
  `display_order` int DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (`id`),
  KEY `fk_tcc_inspection` (`inspection_id`),
  CONSTRAINT `fk_tcc_inspection` FOREIGN KEY (`inspection_id`) REFERENCES `safety_inspection` (`inspection_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `time_slot`
--

DROP TABLE IF EXISTS `time_slot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `time_slot` (
  `slot_id` int NOT NULL AUTO_INCREMENT,
  `start_time` time NOT NULL,
  `capacity` int NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `period` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reservation_id` int DEFAULT NULL,
  PRIMARY KEY (`slot_id`),
  UNIQUE KEY `start_time` (`start_time`),
  KEY `FKncate5ldoltsk7tstw3uapt9e` (`reservation_id`),
  CONSTRAINT `FKncate5ldoltsk7tstw3uapt9e` FOREIGN KEY (`reservation_id`) REFERENCES `booking_slot_reservation` (`reservation_id`)
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `vehicle`
--

DROP TABLE IF EXISTS `vehicle`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vehicle` (
  `vehicle_id` int NOT NULL AUTO_INCREMENT,
  `brand` varchar(255) DEFAULT NULL,
  `license_plate` varchar(255) NOT NULL,
  `manufacture_year` int DEFAULT NULL,
  `model` varchar(255) DEFAULT NULL,
  `customer_id` int DEFAULT NULL,
  `status` varchar(50) DEFAULT 'ACTIVE',
  PRIMARY KEY (`vehicle_id`),
  UNIQUE KEY `UKj5v3su3bdx4bvsk1t9dga4bsq` (`license_plate`),
  KEY `FKluxlvxmmogn3jsh9qn2hvbo46` (`customer_id`),
  CONSTRAINT `FKluxlvxmmogn3jsh9qn2hvbo46` FOREIGN KEY (`customer_id`) REFERENCES `customer_profile` (`customer_id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `vehicle_condition_photo`
--

DROP TABLE IF EXISTS `vehicle_condition_photo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vehicle_condition_photo` (
  `photo_id` int NOT NULL AUTO_INCREMENT,
  `service_ticket_id` int NOT NULL,
  `category` enum('FRONT','BACK','LEFT','RIGHT','OVERALL','DAMAGE','LICENSE_PLATE','ODOMETER') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `photo_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'URL ảnh từ Cloudinary',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'Mô tả (bắt buộc cho DAMAGE)',
  `uploaded_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời gian upload',
  `uploaded_by` int NOT NULL COMMENT 'Staff ID người upload',
  PRIMARY KEY (`photo_id`),
  KEY `idx_service_ticket_id` (`service_ticket_id`),
  KEY `idx_category` (`category`),
  KEY `idx_uploaded_at` (`uploaded_at`),
  KEY `fk_photo_staff` (`uploaded_by`),
  CONSTRAINT `fk_photo_service_ticket` FOREIGN KEY (`service_ticket_id`) REFERENCES `service_ticket` (`service_ticket_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_photo_staff` FOREIGN KEY (`uploaded_by`) REFERENCES `staff_profile` (`staff_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=105 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Ảnh tình trạng xe khi check-in';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `vehicle_specification`
--

DROP TABLE IF EXISTS `vehicle_specification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vehicle_specification` (
  `spec_id` int NOT NULL AUTO_INCREMENT,
  `vehicle_id` int NOT NULL,
  `spec_type` varchar(50) NOT NULL,
  `spec_value` varchar(100) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` int DEFAULT NULL,
  PRIMARY KEY (`spec_id`),
  KEY `fk_spec_vehicle` (`vehicle_id`),
  CONSTRAINT `fk_spec_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`vehicle_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `warehouse`
--

DROP TABLE IF EXISTS `warehouse`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `warehouse` (
  `warehouse_id` int NOT NULL AUTO_INCREMENT,
  `warehouse_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Mã kho (MASTER, CS_A, CS_B)',
  `warehouse_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Tên kho',
  `warehouse_type` enum('MASTER','BRANCH') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Loại kho: Master hoặc Chi nhánh',
  `parent_warehouse_id` int DEFAULT NULL COMMENT 'Kho cha (NULL nếu là MASTER)',
  `address` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'Địa chỉ kho',
  `manager_staff_id` int DEFAULT NULL COMMENT 'Người quản lý kho',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '1=Đang hoạt động, 0=Ngừng hoạt động',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`warehouse_id`),
  UNIQUE KEY `warehouse_code` (`warehouse_code`),
  KEY `idx_warehouse_type` (`warehouse_type`),
  KEY `idx_warehouse_active` (`is_active`),
  KEY `idx_parent` (`parent_warehouse_id`),
  KEY `fk_warehouse_manager` (`manager_staff_id`),
  CONSTRAINT `fk_warehouse_manager` FOREIGN KEY (`manager_staff_id`) REFERENCES `staff_profile` (`staff_id`),
  CONSTRAINT `fk_warehouse_parent` FOREIGN KEY (`parent_warehouse_id`) REFERENCES `warehouse` (`warehouse_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Quản lý kho tổng và kho chi nhánh';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `warehouse_attachment`
--

DROP TABLE IF EXISTS `warehouse_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `warehouse_attachment` (
  `attachment_id` int NOT NULL AUTO_INCREMENT,
  `ref_type` enum('STOCK_ENTRY','STOCK_ISSUE','RETURN_ENTRY_ITEM') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Loại chứng từ',
  `ref_id` int NOT NULL COMMENT 'ID tương ứng: entry_id / issue_id / return_item_id',
  `file_url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `uploaded_by` int NOT NULL,
  `uploaded_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`attachment_id`),
  KEY `idx_wa_ref` (`ref_type`,`ref_id`),
  KEY `idx_wa_uploader` (`uploaded_by`)
) ENGINE=InnoDB AUTO_INCREMENT=68 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Ảnh chứng từ kho — dùng chung cho phiếu nhập, xuất, hoàn hàng';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `warehouse_pricing`
--

DROP TABLE IF EXISTS `warehouse_pricing`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `warehouse_pricing` (
  `pricing_id` int NOT NULL AUTO_INCREMENT,
  `warehouse_id` int NOT NULL COMMENT 'Kho áp dụng giá',
  `item_id` int NOT NULL COMMENT 'Sản phẩm',
  `base_price` decimal(12,2) NOT NULL COMMENT 'Giá gốc (từ master)',
  `markup_multiplier` decimal(5,2) DEFAULT '1.00' COMMENT 'Hệ số nhân giá (x2, x10...)',
  `selling_price` decimal(12,2) NOT NULL COMMENT 'Giá bán = base_price × markup_multiplier',
  `effective_from` date NOT NULL COMMENT 'Ngày bắt đầu áp dụng',
  `effective_to` date DEFAULT NULL COMMENT 'Ngày hết hạn (NULL = vô thời hạn)',
  `is_active` tinyint(1) DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`pricing_id`),
  KEY `item_id` (`item_id`),
  KEY `idx_warehouse_item` (`warehouse_id`,`item_id`),
  KEY `idx_effective_dates` (`effective_from`,`effective_to`),
  KEY `idx_active` (`is_active`),
  CONSTRAINT `warehouse_pricing_ibfk_1` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse` (`warehouse_id`) ON DELETE CASCADE,
  CONSTRAINT `warehouse_pricing_ibfk_2` FOREIGN KEY (`item_id`) REFERENCES `catalog_item` (`item_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Định giá sản phẩm theo từng kho với hệ số markup';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `work_category`
--

DROP TABLE IF EXISTS `work_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `work_category` (
  `idwork_category` int NOT NULL AUTO_INCREMENT,
  `category_code` varchar(50) DEFAULT NULL,
  `category_name` varchar(100) DEFAULT NULL,
  `display_order` int DEFAULT NULL,
  `is_active` tinyint DEFAULT '0',
  `is_default` tinyint(1) DEFAULT '0',
  `tax_rule_idtax_rule` int NOT NULL,
  `category_type` enum('SERVICE','PART') DEFAULT NULL,
  PRIMARY KEY (`idwork_category`),
  KEY `fk_work_category_tax_rule1_idx` (`tax_rule_idtax_rule`),
  CONSTRAINT `fk_work_category_tax_rule1` FOREIGN KEY (`tax_rule_idtax_rule`) REFERENCES `tax_rule` (`idtax_rule`)
) ENGINE=InnoDB AUTO_INCREMENT=243 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `work_shift`
--

DROP TABLE IF EXISTS `work_shift`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `work_shift` (
  `shift_id` int NOT NULL AUTO_INCREMENT,
  `shift_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Ca Sáng, Ca Chiều, Ca Tối',
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  `is_active` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`shift_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `zalo_token`
--

DROP TABLE IF EXISTS `zalo_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `zalo_token` (
  `idzalo_token` bigint NOT NULL AUTO_INCREMENT,
  `access_token` text,
  `refresh_token` text,
  `expires_at` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `code_verifier` varchar(255) DEFAULT NULL,
  `state` varchar(40) DEFAULT NULL,
  PRIMARY KEY (`idzalo_token`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-25  2:14:39
