/* 
* C3P0ConnectionHandler.java
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

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;
import com.noterik.bart.fs.GlobalConfig;

/**
 * Connection handler using C3PO
 * 
 * Connection handler using the C3PO pooling capabilities
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.db
 * @access private
 * @version $Id: C3P0ConnectionHandler.java,v 1.4 2009-12-10 10:03:27 derk Exp $
 *
 */
public class C3P0ConnectionHandler extends ConnectionHandler {
	/**
	 * Logger
	 */
	private static Logger logger = Logger.getLogger(C3P0ConnectionHandler.class);
	
	private static int counter = 0;
	
	/**
	 * Connection pool
	 */
	private ComboPooledDataSource cpds;
	
	public C3P0ConnectionHandler() {
		super();
		
		// database info
		String jdbcString="jdbc:mysql://" + config.getDatabaseHost() + "/" + config.getDatabaseName() + "?serverTimezone=UTC";
		//String jdbcDriver="com.mysql.cj.jdbc.Driver";
		
		System.out.println("CONNECTING TO DATABASE = "+jdbcString);
		// pooled database connections
		cpds = new ComboPooledDataSource(); 
		/*try {
			cpds.setDriverClass(jdbcDriver); 
		} catch (PropertyVetoException e) {
			logger.error("Could not set driver class.",e);
		}*/
		
		// set database info
		cpds.setJdbcUrl(jdbcString); 
		cpds.setUser(config.getDatabaseUser()); 
		cpds.setPassword(config.getDatabasePassword()); 
		System.out.println("USER="+config.getDatabaseUser()+" pass="+config.getDatabasePassword());
		
		// the settings below are optional -- c3p0 can work with defaults 
		cpds.setMinPoolSize(5); 
		cpds.setAcquireIncrement(5);
		cpds.setMaxIdleTimeExcessConnections(2*60*60*1000);	// 2 hours
		cpds.setMaxPoolSize(GlobalConfig.instance().getMaxNumDbConnections());
		cpds.setUnreturnedConnectionTimeout(60*1000); // 1 minute
		cpds.setDebugUnreturnedConnectionStackTraces(true);
		cpds.setCheckoutTimeout(15000); //15 seconds
		cpds.setMaxConnectionAge(90); //30 seconds
	}

	@Override
	public void closeConnection(Connection conn) {
		try {
			conn.close();
		} catch (Exception e) {
			logger.error("Could not close connection.",e);
		}
	}

	@Override
	public Connection getConnection() {
		Connection conn = null;
		try {
			conn = cpds.getConnection();
			//System.out.println("GOT CONNECTION ("+(counter++)+")");
		} catch (SQLException e) {
			logger.error("Could not get a new connection.",e);
			System.out.println("Could not get a new connection.");
		}
		return conn;
	}

	@Override
	public int getNumberOfOpenConnections() {
		int numConnections = -1;
		try {
			numConnections = cpds.getNumBusyConnectionsDefaultUser();
		} catch (Exception e) {
			logger.error("Could not determine number of open connections.",e);
		}
		return numConnections;
	}

	@Override
	public void destroy() {
		try {
			DataSources.destroy( cpds );
		} catch (SQLException e) {
			logger.error("unable to destroy pooled datasource");
		}
	}

	
}
