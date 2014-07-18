/* 
* Statistics.java
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
