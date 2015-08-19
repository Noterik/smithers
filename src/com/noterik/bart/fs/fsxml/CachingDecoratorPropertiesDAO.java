/* 
* CachingDecoratorPropertiesDAO.java
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

import java.util.List;

import com.noterik.bart.fs.cache.CacheHandler;
import com.noterik.bart.fs.fscommand.dao.DAOException;

/**
 * Decorator for FSXMLPropertiesDAO, that adds a caching functionality
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2009
 * @package com.noterik.bart.fs.fsxml
 * @access private
 *
 */
public class CachingDecoratorPropertiesDAO implements FSXMLPropertiesDAO {

	/** dao to cache */
	private FSXMLPropertiesDAO pdao;
	
	
	/**
	 * Default constructor.
	 * 
	 * @param pdao	FSXMLPropertiesDAO to cache
	 */
	public CachingDecoratorPropertiesDAO(FSXMLPropertiesDAO pdao, CacheHandler cHandler) {
		this.pdao = pdao;
		//this.cHandler = cHandler;
	}

	/**
	 * Create properties
	 */
	public boolean create(FSXMLProperties properties) throws DAOException {
		// create using dao
		boolean success = pdao.create(properties);
		
		// clear cache
		//cHandler.delete(properties.getUri());
		
		return success;
	}

	/**
	 * Read properties
	 */
	public FSXMLProperties read(String key) throws DAOException {
		return pdao.read(key);
	}

	/**
	 * Update properties
	 */
	public boolean update(FSXMLProperties properties) throws DAOException {
		boolean success = pdao.update(properties);
		return success;
	}

	/**
	 * Delete properties
	 */
	public boolean delete(String key) throws DAOException {
		// delete using dao
		boolean success = pdao.delete(key);
		return success;
	}

	/**
	 * Read referrer list
	 * 
	 * TODO: caching
	 */
	public List<FSXMLProperties> getReferredProperties(String referUri) {
		// get list using dao
		List<FSXMLProperties> pList = pdao.getReferredProperties(referUri);
		return pList;
	}
	
	/**
	 * Read type list
	 * 
	 * TODO: caching
	 */
	public List<FSXMLProperties> getPropertiesByType(String type) {
		// get list using dao
		List<FSXMLProperties> pList = pdao.getPropertiesByType(type);		
		return pList;
	}
}
