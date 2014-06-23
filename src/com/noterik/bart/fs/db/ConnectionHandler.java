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