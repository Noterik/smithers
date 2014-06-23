package com.noterik.bart.fs.usermanager;

import java.util.List;

import com.noterik.bart.fs.dao.DAOException;
import com.noterik.bart.fs.dao.GenericDAO;

/**
 * UserDAO interface
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2010
 * @package com.noterik.bart.fs.usermanager
 * @access private
 *
 */
public interface UserDAO extends GenericDAO<User, UserKey> {
	/**
	 * Get all users from a certain domain
	 * 
	 * @param domain
	 * @return
	 */
	public List<User> getUserList(String domain) throws DAOException;
	
	/**
	 * Get users from a certain domain, and limit the results
	 * 
	 * @param domain
	 * @param start
	 * @param limit
	 * @return
	 */
	public List<User> getUserList(String domain, int start, int limit) throws DAOException;
	
	/**
	 * Get users from a certain domain, and sort them
	 * 
	 * @param domain
	 * @param sortField
	 * @param sortDirection
	 * @return
	 * @throws DAOException
	 */
	public List<User> getUserList(String domain, String sortField, String sortDirection) throws DAOException;
	
	/**
	 * Get users from a certain domain, limit the results and sort the results
	 * 
	 * @param domain
	 * @param start
	 * @param limit
	 * @param sortField
	 * @param sortDirection
	 * @return
	 */
	public List<User> getUserList(String domain, int start, int limit, String sortField, String sortDirection) throws DAOException;
}
