package com.noterik.bart.fs.usermanager;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import com.noterik.bart.fs.dao.DAOException;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.bart.marge.model.Service;
import com.noterik.bart.marge.server.MargeServer;
import com.noterik.springfield.tools.HttpHelper;

/**
 * UserDAO implementation for the usermanager (barney)
 * 
 * TODO: move filesystem code in an other UserDAO
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2010
 * @package com.noterik.bart.fs.usermanager
 * @access private
 *
 */
public class UserDAOBarney implements UserDAO {
	/** */
	private static Logger logger = Logger.getLogger(UserDAOBarney.class);
	
	/** service type of barney */
	private static final String SERVICE_TYPE = "usermanager";
	
	/** search defaults */
	private static final int DEFAULT_START = 0;
	private static final int DEFAULT_LIMIT = -1;
	private static final String DEFAULT_SORT_FIELD = "username";
	private static final String DEFAULT_SORT_DIRECTION = "ASC";
	
	/** some URIs to call the usermanager on */
	private static final String USER_URI_TEMPLATE = "/domain/{domain}/user/{user}";
	private static final String USERS_URI_TEMPLATE = "/domain/{domain}/user";
	
	/** filesystem constants */
	public static final String EMPTY_PROPERTIES = "<fsxml><properties/></fsxml>";
	
	/** tmp ugly hack untill we have a marge replacement **/
	private static final String barneyURL = "http://localhost:8080/barney/restlet";

	
	/**
	 * Create a user
	 * @throws UserExistsException 
	 */
	public boolean create(User user) throws DAOException {
		logger.debug("trying to create user "+user);
		boolean created;
		
		// get usermanagement service from homer
		/*Service service = MargeServer.getInstance().getService(SERVICE_TYPE, user.getDomain());
		if(service == null) {
			throw new DAOException("service not found for domain '"+user.getDomain()+"'");
		}*/
		
		// check if user already exists
		if( userExists(user) ) {
			throw new UserExistsException("User already exists");
		}
		
		// create user in barney
		//String url = service.getUrl() + USER_URI_TEMPLATE.replace("{domain}", user.getDomain()).replace("{user}", user.getId());
		String url = barneyURL + USER_URI_TEMPLATE.replace("{domain}", user.getDomain()).replace("{user}", user.getId());
		String body = "firstName="+user.getFirstname()+"&lastName="+user.getLastname()+"&email="+user.getEmail()+"&password="+user.getPassword()+"&telephone=N/A";
		String response = HttpHelper.sendRequest("POST", url, body, "text/plain");
		try {
			Document doc = DocumentHelper.parseText(response);
			Node nNode = doc.selectSingleNode("//notification");
			Node eNode = doc.selectSingleNode("//error");
			// error response
			if(eNode != null) { 
				created = false;
			} 
			// no response
			else if(nNode == null) {
				throw new DAOException("no response from barney");
			}
			else { 
				created = nNode.getText().indexOf("creation:false") == -1;
			}
		} catch (DocumentException e) {
			throw new DAOException("could not parse response from barney");
		}
		
		// create user in filesystem		
		String uri = USER_URI_TEMPLATE.replace("{domain}", user.getDomain()).replace("{user}", user.getId());
		logger.debug("creating user in filesystem: "+uri);
		boolean success = FSXMLRequestHandler.instance().saveFsXml(uri, EMPTY_PROPERTIES, "PUT", true);
		if(!success) {
			logger.error("failed to create user in filesystem");
			created = false;
		}
		
		return created;
	}

	/**
	 * Read a user
	 */
	public User read(UserKey key) throws DAOException {
		logger.debug("trying to read user "+key);
		User user = null;
		
		// get usermanagement service from homer
		/*Service service = MargeServer.getInstance().getService(SERVICE_TYPE, key.getDomain());
		if(service == null) {
			throw new DAOException("service not found for domain '"+key.getDomain()+"'");
		}*/
		
		// read user from barney
		//String url = service.getUrl() + USER_URI_TEMPLATE.replace("{domain}", key.getDomain()).replace("{user}", key.getUid());
		String url = barneyURL + USER_URI_TEMPLATE.replace("{domain}", key.getDomain()).replace("{user}", key.getUid());
		logger.debug("url: "+url);
		String response = HttpHelper.sendRequest("GET", url, null, null);
		try {
			Document doc = DocumentHelper.parseText(response);
			Node node = doc.selectSingleNode("//user");
			if(node!=null) {
				// be careful umxml2user does not include the domain
				user = UserManagerPOJOConverter.umxml2user(node.asXML());
				user.setDomain(key.getDomain());
			}
		} catch (DocumentException e) {
			throw new DAOException("could not parse response from barney");
		} catch (Exception e) {
			logger.error("",e);
		}
		return user;
	}

	/**
	 * Update a user
	 */
	public boolean update(User user) throws DAOException {
		logger.debug("trying to update user "+user);
		boolean updated;
		
		// get usermanagement service from homer
		/*Service service = MargeServer.getInstance().getService(SERVICE_TYPE, user.getDomain());
		if(service == null) {
			throw new DAOException("service not found for domain '"+user.getDomain()+"'");
		}*/
		
		// update user in barney
		//String url = service.getUrl() + USER_URI_TEMPLATE.replace("{domain}", user.getDomain()).replace("{user}", user.getId());
		String url = barneyURL + USER_URI_TEMPLATE.replace("{domain}", user.getDomain()).replace("{user}", user.getId());
		logger.debug("url: "+url);
		String body = "firstName="+user.getFirstname()+"&lastName="+user.getLastname()+"&email="+user.getEmail()+"&password="+user.getPassword()+"&telephone=N/A";
		String response = HttpHelper.sendRequest("PUT", url, body, "text/plain");
		try {
			Document doc = DocumentHelper.parseText(response);
			Node nNode = doc.selectSingleNode("//notification");
			Node eNode = doc.selectSingleNode("//error");
			// error response
			if(eNode != null) { 
				updated = false;
			} 
			// no response
			else if(nNode == null) {
				throw new DAOException("no response from barney");
			}
			else { 
				updated = nNode.getText().indexOf("updated:true") != -1;
			}
		} catch (DocumentException e) {
			throw new DAOException("could not parse response from barney");
		}
		
		// TODO: update user in filesystem?
		
		return updated;
	}
	
	/**
	 * Remove a user
	 */
	public boolean delete(UserKey key) throws DAOException {
		logger.debug("trying to delete user "+key);
		boolean removed;
		
		// get usermanagement service from homer
		/*Service service = MargeServer.getInstance().getService(SERVICE_TYPE, key.getDomain());
		if(service == null) {
			throw new DAOException("service not found for domain '"+key.getDomain()+"'");
		}*/
		
		// remove group from barney
		//String url = service.getUrl() + USER_URI_TEMPLATE.replace("{domain}", key.getDomain()).replace("{user}", key.getUid());
		String url = barneyURL + USER_URI_TEMPLATE.replace("{domain}", key.getDomain()).replace("{user}", key.getUid());
		logger.debug("url: "+url);
		String response = HttpHelper.sendRequest("DELETE", url, null, null);
		try {
			Document doc = DocumentHelper.parseText(response);
			Node nNode = doc.selectSingleNode("//notification");
			Node eNode = doc.selectSingleNode("//error");
			// error response
			if(eNode != null) { 
				removed = false;
			} 
			// no response
			else if(nNode == null) {
				throw new DAOException("no response from barney");
			}
			else { 
				removed = nNode.getText().indexOf("delete:false") == -1;
			}
		} catch (DocumentException e) {
			throw new DAOException("could not parse response from barney");
		}
		return removed;
	}
	/**
	 * Get all users from a certain domain
	 */
	public List<User> getUserList(String domain) throws DAOException {
		return getUserList(domain, DEFAULT_START, DEFAULT_LIMIT);
	}

	/**
	 * Get users from a certain domain, and limit the results
	 */
	public List<User> getUserList(String domain, int start, int limit) throws DAOException {
		return getUserList(domain, start, limit, DEFAULT_SORT_FIELD, DEFAULT_SORT_DIRECTION);
	}
	
	/**
	 * Get users from a certain domain, and sort them
	 */
	public List<User> getUserList(String domain, String sortField, String sortDirection) throws DAOException {
		return getUserList(domain, DEFAULT_START, DEFAULT_LIMIT, sortField, sortDirection);
	}

	/**
	 * Get users from a certain domain, limit the results and sort the results
	 */
	public List<User> getUserList(String domain, int start, int limit, String sortField, String sortDirection) throws DAOException {
		logger.debug("trying to get user list for domain "+domain);
		List<User> userList = new ArrayList<User>();
		
		// get usermanagement service from homer
		/*Service service = MargeServer.getInstance().getService(SERVICE_TYPE, domain);
		if(service == null) {
			throw new DAOException("service not found for domain '"+domain+"'");
		}*/
		
		// get xml
		//String url = service.getUrl() + USERS_URI_TEMPLATE.replace("{domain}", domain) + "?start="+start+"&limit="+limit+"&sort="+sortField+","+sortDirection;
		String url = barneyURL + USERS_URI_TEMPLATE.replace("{domain}", domain) + "?start="+start+"&limit="+limit+"&sort="+sortField+","+sortDirection;
		logger.debug("url: "+url);
		String response = HttpHelper.sendRequest("GET", url, null, null);
		try {
			Document doc = DocumentHelper.parseText(response);
			Node eNode = doc.selectSingleNode("//error");
			if(eNode != null) {
				throw new DAOException(eNode.getText());
			}
			
			// parse all groups
			List<Node> nList = doc.selectNodes("//user");
			for(Node node : nList) {
				User user = UserManagerPOJOConverter.umxml2user(node.asXML());
				if(user!=null) {
					// be careful umxml2user does not include the domain
					user.setDomain(domain);
					userList.add(user);
				}
			}
		} catch (DocumentException e) {
			throw new DAOException("could not parse response from barney");
		}
		return userList;
	}

	/**
	 * Test if a user exists
	 * 
	 * @param user
	 * @return
	 */
	private boolean userExists(User user) {
		User u = null;
		String domain = user.getDomain();
		String uid = user.getId();
		try {
			// try to read user to see if it exists
			u = read( new UserKey(domain,uid) );
		} catch (DAOException e) {
			return false;
		}
		return (u != null);
	}
}
