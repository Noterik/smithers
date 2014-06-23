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
		String jdbcString="jdbc:mysql://" + config.getDatabaseHost() + "/" + config.getDatabaseName();
		String jdbcDriver="com.mysql.jdbc.Driver";
		
		System.out.println("CONNECTING TO DATABASE = "+jdbcString);
		// pooled database connections
		cpds = new ComboPooledDataSource(); 
		try {
			cpds.setDriverClass(jdbcDriver); 
		} catch (PropertyVetoException e) {
			logger.error("Could not set driver class.",e);
		} 
		
		// set database info
		cpds.setJdbcUrl(jdbcString); 
		cpds.setUser(config.getDatabaseUser()); 
		cpds.setPassword(config.getDatabasePassword()); 
		System.out.println("USER="+config.getDatabaseUser()+" pass="+config.getDatabasePassword());
		
		// the settings below are optional -- c3p0 can work with defaults 
		cpds.setMinPoolSize(5); 
		cpds.setAcquireIncrement(5);
		cpds.setMaxIdleTimeExcessConnections(2*60*60*1000);
		cpds.setMaxPoolSize(GlobalConfig.instance().getMaxNumDbConnections());
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
