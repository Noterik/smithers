package com.noterik.bart.fs.fsxml;

/**
 * Mysql implementation of the DAOFactory.
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2009
 * @package com.noterik.bart.fs.fsxml
 * @access private
 * @version $Id: DAOFactoryMySQL.java,v 1.2 2009-12-09 15:36:58 derk Exp $
 *
 */
public class DAOFactoryMySQL extends DAOFactory {

	public DAOFactoryMySQL() {}
	
	public FSXMLChildDAO getFSXMLChildDAO() {
		return FSXMLChildDAOMySQL.instance();
	}

	public FSXMLPropertiesDAO getFSXMLPropertiesDAO() {
		return FSXMLPropertiesDAOMySQL.instance();
	}

}
