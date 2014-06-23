--
-- Table structure for table `children`
--

DROP TABLE IF EXISTS `children`;
CREATE TABLE `children` (
  `c_uri` varchar(255) NOT NULL,
  `c_id` varchar(50) NOT NULL DEFAULT '',
  `c_type` varchar(30) NOT NULL,
  `c_refer_uri` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`c_uri`,`c_id`,`c_type`),
  KEY `c_refer_uri_index` (`c_refer_uri`)
) ENGINE=InnoDB DEFAULT CHARSET= utf8;

--
-- Table structure for table `properties`
--

DROP TABLE IF EXISTS `properties`;
CREATE TABLE `properties` (
  `p_uri` varchar(255) NOT NULL,
  `p_type` varchar(30) NOT NULL,
  `p_xml` mediumtext NOT NULL,
  `p_mimetype` varchar(20) DEFAULT 'text/fsxml',
  `p_refer_uri` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`p_uri`),
  KEY `p_refer_uri_index` (`p_refer_uri`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `tenum`
--

DROP TABLE IF EXISTS `tenum`;
CREATE TABLE `tenum` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uri` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`uri`),
  KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET= utf8;

--
-- Table structure for table `tid`
--

DROP TABLE IF EXISTS `tid`;
CREATE TABLE `tid` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `enum_id` int(11) NOT NULL,
  PRIMARY KEY (`enum_id`,`id`)
) ENGINE=MyISAM DEFAULT CHARSET= utf8;
