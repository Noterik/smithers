package com.noterik.bart.fs.usermanager;

import java.util.List;

import com.noterik.bart.fs.dao.DAOException;
import com.noterik.bart.fs.dao.GenericDAO;

public interface GroupDAO extends GenericDAO<Group, GroupKey> {
	/**
	 * Adds a user to a group
	 * 
	 * @param uKey
	 * @param gKey
	 * @return
	 * @throws DAOException
	 */
	public boolean addUserToGroup(UserKey uKey, GroupKey gKey) throws DAOException;
	
	/**
	 * Removes a user from a group
	 * 
	 * @param uKey
	 * @param gKey
	 * @return
	 * @throws DAOException
	 */
	public boolean removeUserFromGroup(UserKey uKey, GroupKey gKey) throws DAOException;
	
	/**
	 * Get all groups from a certain domain
	 * 
	 * @param domain
	 * @return
	 */
	public List<Group> getGroupList(String domain) throws DAOException;
	
	/**
	 * Get all groups of certain user.
	 * 
	 * @param key
	 * @return
	 */
	public List<Group> getUserGroupList(UserKey key) throws DAOException;
}
