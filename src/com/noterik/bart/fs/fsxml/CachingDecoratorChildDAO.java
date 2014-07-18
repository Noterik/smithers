/* 
* CachingDecoratorChildDAO.java
* 
* Copyright (c) 2012 Noterik B.V.
* 
* This file is part of smithers, related to the Noterik Springfield project.
*
* Smithers is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Smithers is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Smithers.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.noterik.bart.fs.fsxml;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.noterik.bart.fs.cache.CacheHandler;
import com.noterik.bart.fs.dao.DAOException;

/**
 * Decorator for FSXMLChildDAO, that adds a caching functionality. 
 * 
 * Only lists are cached, not single Child objects. So the caching 
 * key is the child.uri (parent uri of list).
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2009
 * @package com.noterik.bart.fs.fsxml
 * @access private
 *
 */
public class CachingDecoratorChildDAO implements FSXMLChildDAO {
	/** CachingDecoratorChildDAO's log4j Logger */
	private static Logger logger = Logger.getLogger(CachingDecoratorChildDAO.class);
	
	/** dao to cache */
	private FSXMLChildDAO cdao;
	
	/** cache handler to use */
	//private CacheHandler cHandler;
	
	/**
	 * Default constructor.
	 * 
	 * @param cdao	FSXMLChildDAO to cache
	 */
	public CachingDecoratorChildDAO(FSXMLChildDAO cdao, CacheHandler cHandler) {
		this.cdao = cdao;
	//	this.cHandler = cHandler;
	}

	/**
	 * Create child
	 */
	public boolean create(FSXMLChild child) throws DAOException {
		logger.debug("About to create child: "+child);
		
		// create using dao
		boolean success = cdao.create(child);
		
		// clear cache
	//	cHandler.delete(child.getUri());
		
		return success;
	}

	/**
	 * Read child
	 * 
	 * TODO: not used
	 */
	public FSXMLChild read(FSXMLChildKey key) throws DAOException {
		logger.debug("About to read child: "+key);
		
		// read using dao
		FSXMLChild child = cdao.read(key);
		return child;
	}

	/**
	 * Update child
	 */
	public boolean update(FSXMLChild child) throws DAOException {
		logger.debug("About to update child: "+child);
		
		// update using dao
		boolean success = cdao.update(child);
		
		// TODO: only update this single child from the list in the cache (optimization)
		// clear cache
		//cHandler.delete(child.getUri());
		
		return success;
	}
	
	/**
	 * Delete child
	 */
	public boolean delete(FSXMLChildKey key) throws DAOException {
		logger.debug("About to delete child: "+key);
		
		// update using dao
		boolean success = cdao.delete(key);
		
		// TODO: only delete this single child from the list in the cache (optimization)
		// clear cache
		//cHandler.delete(key.getUri());
		
		return success;
	}

	/**
	 * Read children list
	 */
	public List<FSXMLChild> getChildren(String uri) {
		logger.debug("About to get child list of parent uri: "+uri);
		
		// try cache
		List<FSXMLChild> cList = null;
		/*
		Object clObject = cHandler.get(uri);
		try {
			cList = (List<FSXMLChild>) clObject;
		} catch(ClassCastException e) {
			logger.error("Wrong Object type in cache");
		}
		*/
		
		// try dao
		if(cList==null) {
			cList = cdao.getChildren(uri);
			
			// store in cache
		//	if(cList!=null) {
		//	cHandler.put(uri, cList);
		//	}
		}
		
		return cList;
	}
	
	/**
	 * Read children list by type
	 */
	public List<FSXMLChild> getChildrenByType(String uri, String type) {
		logger.debug("About to get children by type for parent uri: "+uri+", child type: "+type);
		
		// read all children (cached) 
		List<FSXMLChild> allList = getChildren(uri);
		
		// create subset of allList
		List<FSXMLChild> cList = new ArrayList<FSXMLChild>();
		for(FSXMLChild child : allList) {
			if(child.getType().equals(type)) {
				cList.add(child);
			}
		}
		
		return cList;
	}
	
	/**
	 * Count
	 */
	public int getChildrenByTypeCount(String uri, String type) {
		// determine using dao
		int count = cdao.getChildrenByTypeCount(uri, type);
		return count;
	}
}
