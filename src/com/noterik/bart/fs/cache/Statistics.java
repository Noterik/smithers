package com.noterik.bart.fs.cache;

/**
 * Caching statistics.
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2009
 * @package com.noterik.bart.fs.cache
 * @access private
 * @version $Id: Statistics.java,v 1.2 2009-03-11 13:29:30 derk Exp $
 *
 */
public interface Statistics {
	/**
	 * Returns the number of times a requested item was found in the cache.
	 * 
	 * @return The number of times a requested item was found in the cache.
	 */
	public long getCacheHits();
	
	/**
	 * Returns the number of times a requested item was not found in the cache.
	 * 
	 * @return The number of times a requested item was not found in the cache.
	 */
	public long getCacheMisses();
}
