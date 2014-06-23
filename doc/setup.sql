-- MySQL dump 10.13  Distrib 5.1.37, for apple-darwin8.11.1 (i386)
--
-- Host: localhost    Database: smithers
-- ------------------------------------------------------
-- Server version	5.1.37-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `children`
--

DROP TABLE IF EXISTS `children`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `children` (
  `c_uri` varchar(255) NOT NULL,
  `c_id` varchar(50) NOT NULL DEFAULT '',
  `c_type` varchar(30) NOT NULL,
  `c_refer_uri` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`c_uri`,`c_id`,`c_type`),
  KEY `c_refer_uri_index` (`c_refer_uri`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `properties`
--

DROP TABLE IF EXISTS `properties`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `properties` (
  `p_uri` varchar(255) NOT NULL,
  `p_type` varchar(30) NOT NULL,
  `p_xml` mediumtext NOT NULL,
  `p_mimetype` varchar(20) DEFAULT 'text/fsxml',
  `p_refer_uri` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`p_uri`),
  KEY `p_refer_uri_index` (`p_refer_uri`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tenum`
--

DROP TABLE IF EXISTS `tenum`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tenum` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uri` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`uri`),
  KEY `id` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=145391 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tid`
--

DROP TABLE IF EXISTS `tid`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tid` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `enum_id` int(11) NOT NULL,
  `uri` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`enum_id`,`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;