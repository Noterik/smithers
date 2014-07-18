/* 
* ConnectionHandler.java
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
package com.noterik.bart.fs.db;

import java.sql.Connection;

import com.noterik.bart.fs.GlobalConfig;

/**
 * Abstract class for handling connections
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.db
 * @access private
 * @version $Id: ConnectionHandler.java,v 1.4 2009-12-10 10:03:27 derk Exp $
 *
 */
public abstract class ConnectionHandler {
	/**
	 * Configuration
	 */
	protected GlobalConfig config;
	
	/**
	 * instance
	 */
	private static ConnectionHandler instance;
	
	/**
	 * Default constructor
	 */
	public ConnectionHandler() {
		config = GlobalConfig.instance();
	}
	
	/**
	 * get instance based on gonfiguration
	 */
	public static ConnectionHandler instance() {
		if (instance == null) {
			if(GlobalConfig.instance().getConnectionHandlerType()!=null) {
				if(GlobalConfig.instance().getConnectionHandlerType().equals("C3PO")) {
					// C3PO pooling connection handler
					instance = new C3P0ConnectionHandler();
				} 
			} 
			
			if(instance == null) {
				// default
				instance = new VerySimpleConnectionHandler();
			}
		}
		return instance;
	}

	/**
	 * Get connection from connection handler
	 * 
	 * @return conn 	database connection
	 */
	public abstract Connection getConnection();

	/**
	 * Let connection handler close the connection
	 * 
	 * @param conn		database connection
	 */
	public abstract void closeConnection(Connection conn);
	
	/**
	 * Get the number of connections currently used throughout the system
	 * 
	 * @return numberOfConnections		number of open connections
	 */
	public abstract int getNumberOfOpenConnections();
	
	/**
	 * Cleans up connection handler
	 */
	public abstract void destroy();
}