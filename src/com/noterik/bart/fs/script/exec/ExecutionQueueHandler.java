/*
 * Created on Feb 4, 2009
 */
package com.noterik.bart.fs.script.exec;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.noterik.bart.fs.GlobalConfig;
import com.noterik.bart.fs.db.ConnectionHandler;
import com.noterik.bart.fs.script.ActionSet;
import com.thoughtworks.xstream.XStream;

public class ExecutionQueueHandler{

	/** the ExecutionQueueHandler's log4j Logger */
	private static final Logger logger = Logger.getLogger(ExecutionQueueHandler.class);
	
	private static ExecutionQueueHandler instance;
	private static final String WRITE_OBJECT_SQL = "INSERT INTO actions (ac_object, ac_smithers_host, ac_method) VALUES (?, ?, ?)";
	private static final String READ_OBJECT_SQL = "SELECT ac_id, ac_object, ac_method FROM actions WHERE ac_smithers_host=? ORDER BY ac_id";
	private static final String DEL_OBJECT_SQL = "DELETE FROM actions WHERE ac_id=?";
	private ExecutionQueue PUTQueue;
	private ExecutionQueue DELETEQueue;
	private ExecutionQueue GETQueue;
	private ExecutionQueue POSTQueue;

	private ExecutionQueueHandler() {

	}

	public static ExecutionQueueHandler instance() {
		if (instance == null) {
			instance = new ExecutionQueueHandler();
		}
		return instance;
	}

	public ExecutionQueue getQueue(String method) {
		if ("DELETE".equals(method)) {
			return getDELETEQueue();
		} else if ("GET".equals(method)) {
			return getGETQueue();
		} else if ("POST".equals(method)) {
			return getPOSTQueue();
		} else if ("PUT".equals(method)) {
			return getPUTQueue();
		}
		return null;
	}

	private ExecutionQueue getDELETEQueue() {
		if (DELETEQueue == null) {
			DELETEQueue = new ExecutionQueue("DELETE");
		}
		return DELETEQueue;
	}

	private ExecutionQueue getGETQueue() {
		if (GETQueue == null) {
			GETQueue = new ExecutionQueue("GET");
		}
		return GETQueue;
	}

	private ExecutionQueue getPOSTQueue() {
		if (POSTQueue == null) {
			POSTQueue = new ExecutionQueue("POST");
		}
		return POSTQueue;
	}

	private ExecutionQueue getPUTQueue() {
		if (PUTQueue == null) {
			PUTQueue = new ExecutionQueue("PUT");
		}
		return PUTQueue;
	}	

	public int serializeActionInDb(ActionSet as, String method) {
		Connection conn = ConnectionHandler.instance().getConnection();
		int id = 0;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement(WRITE_OBJECT_SQL);
			logger.debug(as);
			XStream xs = new XStream();
			String asxml = xs.toXML(as);
			pstmt.setString(1, asxml);
			pstmt.setString(2, GlobalConfig.instance().getHostName());
			pstmt.setString(3, method);
			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();
		    id = -1;
		    if (rs.next()) {
		      id = rs.getInt(1);
		    }
		} catch (SQLException e) {
			logger.error("",e);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (pstmt != null) {
					pstmt.close();
				}
				ConnectionHandler.instance().closeConnection(conn);
			} catch (SQLException e) {
				logger.error("",e);
			}
		}
		return id;
	}
	
	public void deleteSerializedActionFromDb(ActionSet as){
		Connection conn = ConnectionHandler.instance().getConnection();
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(DEL_OBJECT_SQL);
			pstmt.setInt(1, as.getDbId());
			pstmt.execute();
		} catch (SQLException e) {		
			logger.error("",e);
		} finally{
			try {			
				if (pstmt != null) {
					pstmt.close();
				}
				ConnectionHandler.instance().closeConnection(conn);
			} catch (SQLException e) {
				logger.error("",e);
			}
		}
	}
	
	/**
	 * This function is called on startup and reads the persisted execution
	 * queues to recover them. The queues are serialized in files in the
	 * directory WebContent/recovery. For each http method there is a different
	 * file stored.
	 * 
	 */

	public void recoverQueues() {
		Connection conn = ConnectionHandler.instance().getConnection();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement(READ_OBJECT_SQL);
			pstmt.setString(1, GlobalConfig.instance().getHostName());
			rs = pstmt.executeQuery();
			XStream xs = new XStream();
			while (rs.next()) {
				ActionSet as = (ActionSet) xs.fromXML(rs.getString("ac_object"));
				as.setDbId(rs.getInt("ac_id"));
				logger.debug("recovered action set: " + as);
				getQueue(rs.getString("ac_method")).addToQueue(as);
			}
		} catch (SQLException e) {
			logger.error("",e);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (pstmt != null) {
					pstmt.close();
				}
				ConnectionHandler.instance().closeConnection(conn);
			} catch (SQLException e) {
				logger.error("",e);
			}
		}

	}
	
	

}