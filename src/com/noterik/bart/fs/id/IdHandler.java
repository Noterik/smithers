package com.noterik.bart.fs.id;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.noterik.bart.fs.db.ConnectionHandler;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * Provides unique id's for new assets
 *
 * The class will group strings into seperate groups of similar strings. Each string
 * group will be provided with unique id's.
 *
 * So, insert('a'), insert('b'), insert('a'), will lead to
 * 'a' -> 1
 * 'b' -> 1
 * 'a' -> 2
 *
 *
 *	SQL:
 *	DROP TABLE IF EXISTS tenum;
 *		CREATE TABLE tenum (
 *		id int not null auto_increment,
 *		uri varchar(255),
 *		primary key(uri),
 *		key(id)
 *	);
 *
 *	DROP TABLE IF EXISTS tid;
 *		create table tid (
 *		id int not null auto_increment,
 *		enum_id int not null,
 *		primary key(enum_id,id)   --> IMPORTANT: don't change the order of these keys
 *	)type=myisam;
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.lisa.server.database
 * @access private
 * @version $Id: IdHandler.java,v 1.20 2011-07-01 11:57:41 derk Exp $
 *
 */
public class IdHandler {
	/** IdHandler's log4j Logger */
	private static Logger logger = Logger.getLogger(IdHandler.class);
	
	private static IdHandler instance;
	/**
	 * table which stores the string groups
	 */
	private static final String ENUM_TABLE = "tenum";

	/**
	 * table which creates the id's
	 */
	private static final String ID_TABLE = "tid";

	private IdHandler(){}

	public static IdHandler instance(){
		if(instance == null){
			instance = new IdHandler();
		}
		return instance;
	}

	/**
	 * Check if uri exists in enum_table
	 *
	 * @param uri
	 * @return
	 */
	private int getEnumId(Connection conn, String uri) {
		int enum_id = -1;
		String sql = "SELECT * FROM " + ENUM_TABLE + " WHERE uri=?";
		try {
			PreparedStatement pst = conn.prepareStatement(sql);
			pst.setString(1, uri);
			ResultSet rs = pst.executeQuery();
			if(rs.next()) {
				enum_id = rs.getInt("id");
			}

			rs.close();
			pst.close();
		} catch (Exception e) {
			logger.error("",e);
		} 
		return enum_id;
	}

	/**
	 * Create a new field in enum table
	 *
	 * @param uri
	 * @return
	 */
	private void createEnum(Connection conn, String uri) {		
		String sql = "INSERT INTO " + ENUM_TABLE + " (uri) VALUES (?)";
		try {
			PreparedStatement pst = conn.prepareStatement(sql);
			pst.setString(1, uri);
			pst.executeUpdate(); // duplicate calls are harmless, since uri is primary key
			pst.close();
		} catch (Exception e) {
			logger.error("",e);
		} 
	}

	/**
	 * Create new unique id
	 *
	 * @param uri
	 * @return
	 */
	public int insert(String uri) {
		// get connection
		Connection conn = ConnectionHandler.instance().getConnection();
		
		int id = -1;
		try {
			int enum_id = getEnumId(conn, uri);
	
			// check if it exists in the enum table
			if(enum_id == -1) {
				// create enum
				createEnum(conn,uri);
				enum_id = getEnumId(conn,uri);
			}
	
			id = insertUri(conn, uri, enum_id);
		} finally {
			// close connection
			ConnectionHandler.instance().closeConnection(conn);
		}
		return id;
	}

	/**
	 * Create new unique id using the enum_id 
	 * 
	 * @param conn
	 * @param uri
	 * @param enum_id
	 * @return
	 */
	private int insertUri(Connection conn, String uri, int enum_id) {
		int id  = -1;

		String sql = "INSERT INTO " + ID_TABLE + " (enum_id) VALUES (?)";
		String deleteSql = "DELETE FROM " + ID_TABLE + " WHERE enum_id=? AND id < ?";
		try {
			PreparedStatement pst = conn.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
			pst.setInt(1, enum_id);
			pst.executeUpdate();
			ResultSet rs = pst.getGeneratedKeys();
			if(rs.next()) {
				id = rs.getInt(1);				
			}

			// remove from table
			PreparedStatement deletePst = conn.prepareStatement(deleteSql);
			deletePst.setInt(1, enum_id);
			deletePst.setInt(2, id);
			deletePst.execute();
			deletePst.close();

			rs.close();
			pst.close();
		} catch (SQLException e) {
			logger.error("",e);
		}

		return id;
	}

	/**
	 * Check to see if currently inserted item, 
	 * that already has an id, is larger than the
	 * one in the tid table.
	 * (needed when client supplies id)
	 * 
	 * @param conn
	 * @param uri
	 */
	public  void checkUriId(String uri){
		// get connection
		Connection conn = ConnectionHandler.instance().getConnection();
		
		int id = 0;
		try {
			try{
				id = new Integer(URIParser.getCurrentUriPart(uri)).intValue();
			} catch(NumberFormatException e){
				logger.debug("ID is not an integer");
				return;
			}
			uri = URIParser.getPreviousUri(uri);
			
			// check if there are larger id's in table
			String sql = "SELECT tid.id AS count , enum_id FROM tid JOIN tenum ON enum_id = tenum.id WHERE tenum.uri = ? AND tid.id < ?";
			PreparedStatement pst = null;
			ResultSet rs = null;
			int dbId = 0;
			try {
				pst = conn.prepareStatement(sql);
				pst.setString(1, uri);
				pst.setInt(2, id);
				rs = pst.executeQuery();			
				if(rs.next()){
					// passed id is bigger than db id				
					setNewId(conn, rs.getInt("enum_id"), id);
				} 
			} catch(SQLException e){
				logger.error("",e);
			} finally {
				try {
					rs.close();
					pst.close();
				} catch (SQLException e) {
					logger.error("",e);
				}
			}
			
			// check if there are any id's in table
			sql = "SELECT tid.id AS count , enum_id FROM tid JOIN tenum ON enum_id = tenum.id WHERE tenum.uri = ?";
			try {
				pst = conn.prepareStatement(sql);
				pst.setString(1, uri);
				rs = pst.executeQuery();			
				if(!rs.next()){
					// passed id is bigger than db id				
					createEnum(conn,uri);
					int enum_id = getEnumId(conn,uri);
					insertUri(conn, uri, enum_id);
					setNewId(conn, enum_id, id);
				}
			} catch(SQLException e){
				logger.error("",e);
			} finally {
				try {
					rs.close();
					pst.close();
				} catch (SQLException e) {
					logger.error("",e);
				}
			}
		} finally {
			// close connection
			ConnectionHandler.instance().closeConnection(conn);
		}
	}

	/**
	 * Set the highest id in the tid table for a given a enum_id.
	 * 
	 * @param conn
	 * @param uriId enum id
	 * @param count
	 */
	private void setNewId(Connection conn, int uriId, int count){
		PreparedStatement pst = null;		
		String sql = "UPDATE tid SET id=? WHERE enum_id=?";
		try {
			pst = conn.prepareStatement(sql);
			pst.setInt(1, count);
			pst.setInt(2, uriId);
			pst.execute();
		} catch(SQLException e){
			logger.error("",e);
		} finally {
			try {
				pst.close();
			} catch (SQLException e) {
				logger.error("",e);
			}
		}
	}

}