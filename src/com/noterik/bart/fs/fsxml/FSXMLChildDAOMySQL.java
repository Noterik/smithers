package com.noterik.bart.fs.fsxml;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.noterik.bart.fs.db.ConnectionHandler;

/**
 * MySQL implementation of the FSXMLChild DAO.
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2009
 * @package com.noterik.bart.fs.fsxml
 * @access private
 */
public class FSXMLChildDAOMySQL implements FSXMLChildDAO {
	/** The FSXMLChildDAOMySQL's log4j Logger */
	private static Logger logger = Logger.getLogger(FSXMLChildDAOMySQL.class);
	
	/** Singleton instance of the FSXMLChildDAOMySQL */
	private static FSXMLChildDAO instance = new FSXMLChildDAOMySQL();
	
	/**
	 * Limit value when all childs should be displayed
	 */
	private static final int LIMIT_ALL = -1;
	
	/**
	 * Constructor.
	 */
	private FSXMLChildDAOMySQL() {}
	
	/**
	 * Returns the singleton instance of this class.
	 * 
	 * @return The singleton instance of this class.
	 */
	public static FSXMLChildDAO instance() {
		return instance;
	}

	/**
	 * Create child
	 */
	public boolean create(FSXMLChild child) {
		logger.debug("About to create child: "+child);
		
		String sql = "INSERT INTO children (c_uri, c_type, c_id, c_refer_uri) VALUES (?, ?, ?, ?)";
		Connection conn = ConnectionHandler.instance().getConnection();
		PreparedStatement pstmt = null;
		try {
			// save
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, child.getUri());
			pstmt.setString(2, child.getType());
			pstmt.setString(3, child.getId());
			pstmt.setString(4, child.getReferUri());
			logger.debug(pstmt);
			
			pstmt.execute();
		} catch(Exception e) {
			logger.debug("Unable to create child",e);
			return false;
		} finally {
			try {
				ConnectionHandler.instance().closeConnection(conn);
			} catch (Exception e) {
				logger.error("Unable to close connection",e);
			}
		}		
		return true;
	}

	/**
	 * Read child
	 */
	public FSXMLChild read(FSXMLChildKey key) {
		logger.debug("About to read child: "+key);
		
		// properties to return
		FSXMLChild child = null;
		
		String sql = "SELECT * FROM children WHERE c_uri=? AND c_type=? AND c_id=?";
		Connection conn = ConnectionHandler.instance().getConnection();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			// read
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, key.getUri());
			pstmt.setString(2, key.getType());
			pstmt.setString(3, key.getId());
			logger.debug(pstmt);
			
			rs = pstmt.executeQuery();
			if(rs.next()) {
				// read to FSXMLChild object
				child = readSingleRow(rs);
			}
		} catch(Exception e) {
			logger.debug("Unable to read properties",e);
		} finally {
			try {
				ConnectionHandler.instance().closeConnection(conn);
			} catch (Exception e) {
				logger.error("Unable to close connection",e);
			}
		}
		
		return child;
	}

	/**
	 * Update child
	 */
	public boolean update(FSXMLChild child) {
		logger.debug("About to update child: "+child);
		
		String sql = "UPDATE children SET c_refer_uri=? WHERE c_uri=? AND c_type=? AND c_id=?";
		Connection conn = ConnectionHandler.instance().getConnection();
		PreparedStatement pstmt = null;
		try {
			// save
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, child.getReferUri());
			pstmt.setString(2, child.getUri());
			pstmt.setString(3, child.getType());
			pstmt.setString(4, child.getId());
			logger.debug(pstmt);
			
			pstmt.execute();
		} catch(Exception e) {
			logger.debug("Unable to create child",e);
			return false;
		} finally {
			try {
				ConnectionHandler.instance().closeConnection(conn);
			} catch (Exception e) {
				logger.error("Unable to close connection",e);
			}
		}		
		return true;
	}
	
	/**
	 * Delete child
	 */
	public boolean delete(FSXMLChildKey key) {
		logger.debug("About to delete child: "+key);
		
		String sql = "DELETE FROM children WHERE c_uri=? AND c_type=? AND c_id=?";
		Connection conn = ConnectionHandler.instance().getConnection();
		PreparedStatement pstmt = null;
		try {
			// save
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, key.getUri());
			pstmt.setString(2, key.getType());
			pstmt.setString(3, key.getId());
			logger.debug(pstmt);
			
			pstmt.execute();
		} catch(Exception e) {
			logger.debug("Unable to delete child",e);
			return false;
		} finally {
			try {
				ConnectionHandler.instance().closeConnection(conn);
			} catch (Exception e) {
				logger.error("Unable to close connection",e);
			}
		}		
		return true;
	}
	
	/**
	 * Read children list
	 */
	public List<FSXMLChild> getChildren(String parentUri) {
		logger.debug("About to get child list of parent uri: "+parentUri);
		
		// list to return
		List<FSXMLChild> cList = new ArrayList<FSXMLChild>();
		
		String sql = "SELECT * FROM children WHERE c_uri=? ORDER BY c_type, IF(ABS(c_id) = 0, 99999 + ASCII(c_id), c_id) +0";	
		Connection conn = ConnectionHandler.instance().getConnection();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			// read
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, parentUri);
			logger.debug(pstmt);
			
			rs = pstmt.executeQuery();
			while(rs.next()) {
				// read to FSXMLChild object
				FSXMLChild child = readSingleRow(rs);
				
				// add to list
				cList.add(child);
			}
		} catch(Exception e) {
			logger.debug("Unable to list children",e);
		} finally {
			try {
				ConnectionHandler.instance().closeConnection(conn);
			} catch (Exception e) {
				logger.error("Unable to close connection",e);
			}
		}
		
		return cList;
	}
	
	/**
	 * Read children list by type
	 */
	public List<FSXMLChild> getChildrenByType(String parentUri, String childType) {
		logger.debug("About to get children by type for parent uri: "+parentUri+", child type: "+childType);
		
		// list to return
		List<FSXMLChild> cList = new ArrayList<FSXMLChild>();
		
		String sql = "SELECT * FROM children WHERE c_uri=? AND c_type=? ORDER BY c_type, IF(ABS(c_id) = 0, 99999 + ASCII(c_id), c_id) +0";	
		Connection conn = ConnectionHandler.instance().getConnection();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			// read
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, parentUri);
			pstmt.setString(2, childType);
			logger.debug(pstmt);
			
			rs = pstmt.executeQuery();
			while(rs.next()) {
				// read to FSXMLChild object
				FSXMLChild child = readSingleRow(rs);
				
				// add to list
				cList.add(child);
			}
		} catch(Exception e) {
			logger.debug("Unable to list children",e);
		} finally {
			try {
				ConnectionHandler.instance().closeConnection(conn);
			} catch (Exception e) {
				logger.error("Unable to close connection",e);
			}
		}
		
		return cList;
	}
	
	/**
	 * Count
	 */
	public int getChildrenByTypeCount(String parentUri, String childType) {
		logger.debug("About to get amount of children by type for parent uri: "+parentUri+", child type: "+childType);
		
		String sql = "SELECT count(*) as cnt FROM children WHERE c_uri = ? and c_type=?";
		Connection conn = ConnectionHandler.instance().getConnection();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		int count = 0;
		try {
			// read
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, parentUri);
			pstmt.setString(2, childType);
			logger.debug(pstmt);
			
			rs = pstmt.executeQuery();
			if(rs.next()) {
				count = rs.getInt("cnt");
			}
		} catch(Exception e) {
			logger.debug("Unable to get amount of children",e);
		} finally {
			try {
				ConnectionHandler.instance().closeConnection(conn);
			} catch (Exception e) {
				logger.error("Unable to close connection",e);
			}
		}
		
		return count;
	}
	
	/**
	 * Returns an FSXMLChild object stored in the current row of this ResultSet.
	 * 
	 * @param rs	ResultSet containing child data
	 * @return		an FSXMLProperties object stored in the current row of this ResultSet
	 * @throws SQLException 
	 */
	private FSXMLChild readSingleRow(ResultSet rs) throws SQLException {
		FSXMLChild child = null;
		
		// read to FSXMLChild object
		String id = rs.getString("c_id");
		String uri = rs.getString("c_uri");
		String referUri = rs.getString("c_refer_uri");
		String type = rs.getString("c_type");		
		child = new FSXMLChild(id, uri, referUri, type);
		
		return child;
	}

}
