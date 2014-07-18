/* 
* DAOFactory.java
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

/**
 * Abstract factory for creating concrete DAO factories.
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2009
 * @package com.noterik.bart.fs.fsxml
 * @access private
 * @version $Id: DAOFactory.java,v 1.2 2009-12-09 15:36:58 derk Exp $
 *
 */
public abstract class DAOFactory {
	/** List of DAO types supported by the factory */
	public static final int MYSQL = 1;
	
	/**
	 * Returns the indicated DAOFactory.
	 * 
	 * @param type	Type of DAOFactory. 
	 * @return The indicated DAOFactory.
	 */
	public static DAOFactory getDAOFactory(int type) {
		switch(type) {
			case MYSQL:
					return new DAOFactoryMySQL();
			default:
				return null;
		}
	}
	
	/**
	 * Returns the FSXMLPropertiesDAO for this client.
	 * 
	 * @return The FSXMLPropertiesDAO for this client.
	 */
	public abstract FSXMLPropertiesDAO getFSXMLPropertiesDAO();
	
	/**
	 * Returns the FSXMLChildDAO for this client.
	 * 
	 * @return The FSXMLChildDAO for this client.
	 */
	public abstract FSXMLChildDAO getFSXMLChildDAO();
}
