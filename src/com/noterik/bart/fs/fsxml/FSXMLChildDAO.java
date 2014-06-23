package com.noterik.bart.fs.fsxml;

import java.util.List;

import com.noterik.bart.fs.dao.GenericDAO;

/**
 * FSXMLChild DAO interface.
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2009
 * @package com.noterik.bart.fs.fsxml
 * @access private
 *
 */
public interface FSXMLChildDAO extends GenericDAO<FSXMLChild, FSXMLChildKey> {
	/**
	 * Returns a list of all children of the given parent uri
	 * 
	 * @param uri	parent uri
	 * @return 		a list of all children of the given parent uri
	 */
	public List<FSXMLChild> getChildren(String uri);
	
	/**
	 * Returns a list of all children of the given parent uri, and of given child type
	 * 
	 * @param uri	parent uri
	 * @param type	child type
	 * @return 		a list of all children of the given parent uri, and of given child type
	 */
	public List<FSXMLChild> getChildrenByType(String uri, String type);
	
	/**
	 * Returns the amount children of the given parent uri, and of given child type
	 * 
	 * @param uri	parent uri
	 * @param type	child type
	 * @return 		the amount children of the given parent uri, and of given child type
	 */
	public int getChildrenByTypeCount(String uri, String type);
}
