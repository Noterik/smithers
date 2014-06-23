package com.noterik.bart.fs.fsxml;

import java.util.List;

import com.noterik.bart.fs.cache.CacheHandler;
import com.noterik.bart.fs.dao.DAOException;

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
