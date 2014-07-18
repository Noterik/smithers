/* 
* VerySimpleConnectionHandler.java
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
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.log4j.Logger;

/**
 * Very simple connection handler
 * 
 * Server a new connection every time
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.db
 * @access private
 * @version $Id: VerySimpleConnectionHandler.java,v 1.5 2009-12-10 10:03:27 derk Exp $
 *
 */
public class VerySimpleConnectionHandler extends ConnectionHandler {
	/**
	 * logger
	 */
	private Logger logger = Logger.getLogger(VerySimpleConnectionHandler.class);
	
	/**
	 * Number of active connections
	 */
	private int numConnections = 0;
	
	/**
	 * Synchronization lock
	 */
	private Object lock = new Object();
	
	/**
	 * Default constructor
	 */
	public VerySimpleConnectionHandler() {
		super();
	}
	
	@Override
	public void closeConnection(Connection conn) {
		try {
			synchronized (lock) {
				numConnections--;
			}
			conn.close();
		} catch (Exception e) {
			logger.error("Could not close connection",e);
		}
	}

	@Override
	public Connection getConnection() {
		// database info
		String jdbcString="jdbc:mysql://" + config.getDatabaseHost() + "/" + config.getDatabaseName();
		String jdbcDriver="com.mysql.jdbc.Driver";
		//String jdbcDriver="org.gjt.mm.mysql.Driver";
		
		
		Connection conn = null;
		try{
			Class.forName(jdbcDriver).newInstance();
		}
		catch(ClassNotFoundException cnfe){			
			logger.error("Could not find jdbc driver: " + jdbcDriver, cnfe);
		}
		catch(InstantiationException ie){			
			logger.error("Could not instantiate jdbc driver", ie);
		}
		catch(IllegalAccessException iae){			
			logger.error("Error while instantiating jdbc driver", iae);
		}
		
		try{
			conn = DriverManager.getConnection(jdbcString,config.getDatabaseUser(),config.getDatabasePassword());
			synchronized (lock) {
				numConnections++;
			}
		}
		catch(SQLException sqle){
			logger.error("Error while connecting to '" + jdbcString+"':" + sqle.getMessage(), sqle);
		}
		return conn;
	}
	
	@Override
	public int getNumberOfOpenConnections() {
		return numConnections;
	}

	@Override
	public void destroy() {
		// does nothing
	}
}
