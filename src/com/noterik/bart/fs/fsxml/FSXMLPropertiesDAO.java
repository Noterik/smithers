package com.noterik.bart.fs.fsxml;

import java.util.List;

import com.noterik.bart.fs.dao.GenericDAO;

/**
 * FSXMLProperties DAO interface.
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2009
 * @package com.noterik.bart.fs.fsxml
 * @access private
 * @version $Id: FSXMLPropertiesDAO.java,v 1.2 2009-12-09 15:36:58 derk Exp $
 *
 */
public interface FSXMLPropertiesDAO extends GenericDAO<FSXMLProperties, String>{
	/**
	 * Returns a list of properties that refer to the specified uri
	 * 
	 * @param referUri	uri
	 * @return			A list of properties that refer to the specified uri
	 */
	public List<FSXMLProperties> getReferredProperties(String referUri);
	
	/**
	 * Returns a list of properties of a certain type
	 * 
	 * @param type	type of property
	 * @return		a list of properties of a certain type
	 */
	public List<FSXMLProperties> getPropertiesByType(String type);
}
