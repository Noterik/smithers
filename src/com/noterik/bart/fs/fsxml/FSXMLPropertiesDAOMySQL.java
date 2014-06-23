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
 * MySQL implementation of the FSXMLProperties DAO.
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2009
 * @package com.noterik.bart.fs.fsxml
 * @access private
 *
 */
public class FSXMLPropertiesDAOMySQL implements FSXMLPropertiesDAO {
	/** The FSXMLPropertiesDAOMySQL's log4j Logger */
	private static Logger logger = Logger.getLogger(FSXMLPropertiesDAOMySQL.class);
	
	/** Singleton instance of the FSXMLPropertiesDAOMySQL */
	private static FSXMLPropertiesDAOMySQL instance = new FSXMLPropertiesDAOMySQL();
	
	/**
	 * Constructor.
	 */
	private FSXMLPropertiesDAOMySQL() {}
	
	/**
	 * Returns the singleton instance of this class.
	 * 
	 * @return The singleton instance of this class.
	 */
	public static FSXMLPropertiesDAOMySQL instance() {
		return instance;
	}

	/**
	 * Create properties
	 */
	public boolean create(FSXMLProperties properties) {
		logger.debug("About to create properties: "+properties);
		
		String sql = "INSERT INTO properties (p_uri, p_refer_uri, p_type, p_mimetype, p_xml) VALUES (?, ?, ?, ?, ?)";
		Connection conn = ConnectionHandler.instance().getConnection();
		PreparedStatement pstmt = null;
		try {
			// save
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, properties.getUri());
			pstmt.setString(2, properties.getReferUri());
			pstmt.setString(3, properties.getType());
			pstmt.setString(4, properties.getMimetype());
			pstmt.setString(5, properties.getXml());
			logger.debug(pstmt);
			
			pstmt.execute();
		} catch(Exception e) {
			logger.debug("Unable to create properties",e);
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
	 * Read properties
	 */
	public FSXMLProperties read(String uri) {
		logger.debug("About to read properties: "+uri);
		
		// properties to return
		FSXMLProperties properties = null;
		
		String sql = "SELECT * FROM properties WHERE p_uri=?";
		Connection conn = ConnectionHandler.instance().getConnection();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			// read
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, uri);
			logger.debug(pstmt);
			
			rs = pstmt.executeQuery();
			if(rs.next()) {
				// read to FSXMLProperties object
				properties = readSingleRow(rs);
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
		logger.debug("read properties from "+uri);
		
		return properties;
	}

	/**
	 * Update properties
	 */
	public boolean update(FSXMLProperties properties) {
		logger.debug("About to update properties: "+properties);
		
		// normal update
		if(properties.getOldXml() == null) {
			return updateNormal(properties);
		}
		// concurrent update
		return updateConcurrent(properties);
	}
	
	/**
	 * Normal update, which override previous
	 * 
	 * @return success
	 */
	private boolean updateNormal(FSXMLProperties properties) {
		logger.debug("normal update");
		
		String sql = "UPDATE properties SET p_refer_uri=?, p_type=?, p_mimetype=?, p_xml=? WHERE p_uri=?";
		Connection conn = ConnectionHandler.instance().getConnection();
		PreparedStatement pstmt = null;
		try {
			// save
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, properties.getReferUri());
			pstmt.setString(2, properties.getType());
			pstmt.setString(3, properties.getMimetype());
			pstmt.setString(4, properties.getXml());
			pstmt.setString(5, properties.getUri());
			logger.debug(pstmt);
			
			pstmt.execute();
		} catch(Exception e) {
			logger.debug("Unable to update properties",e);
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
	 * Concurrent update, which override previous only when the xml is the same
	 * 
	 * @return success
	 */
	private boolean updateConcurrent(FSXMLProperties properties) {
		logger.debug("concurrent update");
		
		String sql = "UPDATE properties SET p_refer_uri=?, p_type=?, p_mimetype=?, p_xml=? WHERE p_uri=? AND p_xml=?";
		Connection conn = ConnectionHandler.instance().getConnection();
		PreparedStatement pstmt = null;
		int numberOfRowsAffected = 0;
		try {
			// save
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, properties.getReferUri());
			pstmt.setString(2, properties.getType());
			pstmt.setString(3, properties.getMimetype());
			pstmt.setString(4, properties.getXml());
			pstmt.setString(5, properties.getUri());
			pstmt.setString(6, properties.getOldXml());
			logger.debug(pstmt);
			
			numberOfRowsAffected = pstmt.executeUpdate();
		} catch(Exception e) {
			logger.debug("Unable to update properties",e);
			return false;
		} finally {
			try {
				ConnectionHandler.instance().closeConnection(conn);
			} catch (Exception e) {
				logger.error("Unable to close connection",e);
			}
		}		
		return (numberOfRowsAffected>0);
	}
	
	/**
	 * Delete properties
	 */
	public boolean delete(String uri) {
		logger.debug("About to delete properties: "+uri);
		
		String sql = "DELETE FROM properties WHERE p_uri=?";
		Connection conn = ConnectionHandler.instance().getConnection();
		PreparedStatement pstmt = null;
		try {
			// delete
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, uri);
			logger.debug(pstmt);
			
			pstmt.execute();
		} catch(Exception e) {
			logger.error("Unable to delete properties",e);
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
	 * Read referrer list
	 */
	public List<FSXMLProperties> getReferredProperties(String referUri) {
		logger.debug("About to read referrer properties list: "+referUri);
		
		// list to return
		List<FSXMLProperties> pList = new ArrayList<FSXMLProperties>();
		
		String sql = "SELECT * FROM properties WHERE p_refer_uri=?";
		Connection conn = ConnectionHandler.instance().getConnection();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			// read
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, referUri);
			logger.debug(pstmt);
			
			rs = pstmt.executeQuery();
			while(rs.next()) {
				// read to FSXMLProperties object
				FSXMLProperties properties = readSingleRow(rs);
				
				// add to list
				pList.add(properties);
			}
		} catch(Exception e) {
			logger.debug("Unable to read referrer properties list",e);
		} finally {
			try {
				ConnectionHandler.instance().closeConnection(conn);
			} catch (Exception e) {
				logger.error("Unable to close connection",e);
			}
		}
		
		return pList;
	}
	
	/**
	 * Read type list
	 */
	public List<FSXMLProperties> getPropertiesByType(String type) {
		logger.debug("About to read properties list for type: "+type);
		
		// list to return
		List<FSXMLProperties> pList = new ArrayList<FSXMLProperties>();
		
		String sql = "SELECT * FROM properties WHERE p_type=?";
		Connection conn = ConnectionHandler.instance().getConnection();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			// read
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, type);
			logger.debug(pstmt);
			
			rs = pstmt.executeQuery();
			while(rs.next()) {
				// read to FSXMLProperties object
				FSXMLProperties properties = readSingleRow(rs);
				
				// add to list
				pList.add(properties);
			}
		} catch(Exception e) {
			logger.debug("Unable to read properties list of type",e);
		} finally {
			try {
				ConnectionHandler.instance().closeConnection(conn);
			} catch (Exception e) {
				logger.error("Unable to close connection",e);
			}
		}
		
		return pList;
	}
	
	/**
	 * Returns an FSXMLProperties object stored in the current row of this ResultSet.
	 * 
	 * @param rs	ResultSet containing properties data
	 * @return		an FSXMLProperties object stored in the current row of this ResultSet
	 * @throws SQLException 
	 */
	private FSXMLProperties readSingleRow(ResultSet rs) throws SQLException {
		FSXMLProperties properties = null;
		
		// read to FSXMLProperties object
		String uri = rs.getString("p_uri");
		String referUri = rs.getString("p_refer_uri");
		String type = rs.getString("p_type");
		String mimetype = rs.getString("p_mimetype");
		String xml = rs.getString("p_xml");		
		properties = new FSXMLProperties(uri, referUri, type, mimetype, xml);
		
		return properties;
	}
	
}
