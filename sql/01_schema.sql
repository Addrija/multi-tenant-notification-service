
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
DROP TABLE IF EXISTS `app_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `app_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role` enum('PLATFORM_ADMIN','TENANT_ADMIN') NOT NULL,
  `username` varchar(255) NOT NULL,
  `tenant_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK3k4cplvh82srueuttfkwnylq0` (`username`),
  KEY `FKsr53t6kfhi9d1k42hv4nlfkkd` (`tenant_id`),
  CONSTRAINT `FKsr53t6kfhi9d1k42hv4nlfkkd` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `channel_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `channel_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `channel_type` enum('EMAIL','IN_APP','PUSH','SMS') NOT NULL,
  `enabled` bit(1) NOT NULL,
  `sender_identity` varchar(255) DEFAULT NULL,
  `simulated_failure_rate` double NOT NULL,
  `tenant_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKcdk3h7wrfbplouiy7putle3sh` (`tenant_id`,`channel_type`),
  CONSTRAINT `FKcujhkbvxg0j2s2j4ty93wa27e` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `delivery_attempt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `delivery_attempt` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attempt_number` int NOT NULL,
  `attempted_at` datetime(6) NOT NULL,
  `channel_type` enum('EMAIL','IN_APP','PUSH','SMS') NOT NULL,
  `error_message` varchar(255) DEFAULT NULL,
  `outcome` enum('FAILURE','SUCCESS') NOT NULL,
  `notification_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKj3jmrdv479hmk4h50de528t1x` (`notification_id`),
  CONSTRAINT `FKj3jmrdv479hmk4h50de528t1x` FOREIGN KEY (`notification_id`) REFERENCES `notification` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification` (
  `id` binary(16) NOT NULL,
  `attempt_count` int NOT NULL,
  `channel_type` enum('EMAIL','IN_APP','PUSH','SMS') NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `next_retry_at` datetime(6) DEFAULT NULL,
  `recipient` varchar(255) NOT NULL,
  `rendered_content` longtext,
  `scheduled_at` datetime(6) DEFAULT NULL,
  `status` enum('FAILED','IN_PROGRESS','PENDING','RETRYING','SENT') NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `variables` longtext,
  `version` bigint NOT NULL,
  `template_id` bigint DEFAULT NULL,
  `tenant_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKiwlh3482klkkb02l15mhq4cf9` (`template_id`),
  KEY `FKai959be89sbcsngnm8pin3ju5` (`tenant_id`),
  CONSTRAINT `FKai959be89sbcsngnm8pin3ju5` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id`),
  CONSTRAINT `FKiwlh3482klkkb02l15mhq4cf9` FOREIGN KEY (`template_id`) REFERENCES `template` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `notification_status_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_status_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `changed_at` datetime(6) NOT NULL,
  `from_status` enum('FAILED','IN_PROGRESS','PENDING','RETRYING','SENT') DEFAULT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `to_status` enum('FAILED','IN_PROGRESS','PENDING','RETRYING','SENT') NOT NULL,
  `notification_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKc2x7rx5n42anj5ap6paidj2rf` (`notification_id`),
  CONSTRAINT `FKc2x7rx5n42anj5ap6paidj2rf` FOREIGN KEY (`notification_id`) REFERENCES `notification` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `rate_limit_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rate_limit_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `capacity` int NOT NULL,
  `refill_rate_per_sec` double NOT NULL,
  `tenant_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKanu5ryuh1mcudxu166jedei50` (`tenant_id`),
  CONSTRAINT `FK99indjj6rqfksdoqab0r5y7p` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `channel_type` enum('EMAIL','IN_APP','PUSH','SMS') NOT NULL,
  `content_template` longtext NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `name` varchar(255) NOT NULL,
  `tenant_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKct412v3iqwth9qs64fy15ri8q` (`tenant_id`),
  CONSTRAINT `FKct412v3iqwth9qs64fy15ri8q` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `tenant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

