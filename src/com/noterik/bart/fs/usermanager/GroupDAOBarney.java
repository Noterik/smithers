package com.noterik.bart.fs.usermanager;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import com.noterik.bart.fs.dao.DAOException;
import com.noterik.bart.marge.model.Service;
import com.noterik.bart.marge.server.MargeServer;
import com.noterik.springfield.tools.HttpHelper;

/**
 * Interface to the usermanagement service called barney
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2010
 * @package com.noterik.bart.fs.usermanager
 * @access private
 * 
 * TODO: create groups in filesystem?
 *
 */
public class GroupDAOBarney implements GroupDAO {
	/** The GroupDAOBarney's log4j Logger */
	private static Logger logger = Logger.getLogger(GroupDAOBarney.class);
	
	/** service type of barney */
	private static final String SERVICE_TYPE = "usermanager";
	
	/** Some URIs to call the usermanager on */
	private static final String GROUP_URI_TEMPLATE = "/domain/{domain}/group/{group}/";
	private static final String GROUPS_URI_TEMPLATE = "/domain/{domain}/group/";
	private static final String USER_GROUPS_URI_TEMPLATE = "/domain/{domain}/user/{user}/";
	private static final String GROUP_MEMBER_URI_TEMPLATE = "/domain/{domain}/group/{group}/member/{member}/";
	
	/** tmp ugly hack untill we have a marge replacement **/
	private static final String barneyURL = "http://localhost:8080/barney/restlet";
	
	/**
	 * Create a group
	 */
	public boolean create(Group group) throws DAOException {
		logger.debug("trying to create group "+group);
		boolean created;
		
		// get usermanagement service from homer
		/*Service service = MargeServer.getInstance().getService(SERVICE_TYPE, group.getDomain());
		if(service == null) {
			throw new DAOException("service not found for domain '"+group.getDomain()+"'");
		}*/
		
		// create group in barney
		//String url = service.getUrl() + GROUP_URI_TEMPLATE.replace("{domain}", group.getDomain()).replace("{group}", group.getId());
		String url = barneyURL + GROUP_URI_TEMPLATE.replace("{domain}", group.getDomain()).replace("{group}", group.getId());
		logger.debug("url: "+url);
		String body = "name="+group.getGroupname();
		String response = HttpHelper.sendRequest("PUT", url, body, "text/plain");
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
		
		return created;
	}

	/**
	 * Read a group
	 */
	public Group read(GroupKey key) throws DAOException {
		logger.debug("trying to read group "+key);
		Group group = null;
		
		// get usermanagement service from homer
		/*Service service = MargeServer.getInstance().getService(SERVICE_TYPE, key.getDomain());
		if(service == null) {
			throw new DAOException("service not found for domain '"+key.getDomain()+"'");
		}*/
		
		// read group from barney
		//String url = service.getUrl() + GROUP_URI_TEMPLATE.replace("{domain}", key.getDomain()).replace("{group}", key.getGid());
		String url = barneyURL + GROUP_URI_TEMPLATE.replace("{domain}", key.getDomain()).replace("{group}", key.getGid());
		logger.debug("url: "+url);
		String response = HttpHelper.sendRequest("GET", url, null, null);
		try {
			Document doc = DocumentHelper.parseText(response);
			Node node = doc.selectSingleNode("//group");
			group = UserManagerPOJOConverter.umxml2group(node.asXML());
		} catch (DocumentException e) {
			throw new DAOException("could not parse response from barney");
		} catch (Exception e) {
			logger.error("",e);
		}
		
		return group;
	}

	/**
	 * Operation not supported
	 */
	public boolean update(Group group) throws DAOException {
		return false;
	}
	
	/**
	 * Remove a group
	 */
	public boolean delete(GroupKey key) throws DAOException {
		logger.debug("trying to delete group "+key);
		boolean removed;
		
		// get usermanagement service from homer
		/*Service service = MargeServer.getInstance().getService(SERVICE_TYPE, key.getDomain());
		if(service == null) {
			throw new DAOException("service not found for domain '"+key.getDomain()+"'");
		}*/
		
		// remove group from barney
		//String url = service.getUrl() + GROUP_URI_TEMPLATE.replace("{domain}", key.getDomain()).replace("{group}", key.getGid());
		String url = barneyURL + GROUP_URI_TEMPLATE.replace("{domain}", key.getDomain()).replace("{group}", key.getGid());
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
	 * List all groups of a domain
	 */
	public List<Group> getGroupList(String domain) throws DAOException {
		List<Group> groupList = new ArrayList<Group>();
		
		// get usermanagement service from homer
		/*Service service = MargeServer.getInstance().getService(SERVICE_TYPE, domain);
		if(service == null) {
			throw new DAOException("service not found for domain '"+domain+"'");
		}*/
		
		// get xml
		//String url = service.getUrl() + GROUPS_URI_TEMPLATE.replace("{domain}", domain);
		String url = barneyURL + GROUPS_URI_TEMPLATE.replace("{domain}", domain);
		logger.debug("url: "+url);
		String response = HttpHelper.sendRequest("GET", url, null, null);
		try {
			Document doc = DocumentHelper.parseText(response);
			Node eNode = doc.selectSingleNode("//error");
			if(eNode != null) {
				throw new DAOException(eNode.getText());
			}
			
			// parse all groups
			List<Node> nList = doc.selectNodes("//group");
			for(Node node : nList) {
				Group group = UserManagerPOJOConverter.umxml2group(node.asXML());
				if(group!=null) {
					// be careful umxml2group does not include the domain
					group.setDomain(domain);
					groupList.add(group);
				}
			}
		} catch (DocumentException e) {
			throw new DAOException("could not parse response from barney");
		}
		return groupList;
	}

	/**
	 * List all groups of a specific user
	 */
	public List<Group> getUserGroupList(UserKey key) throws DAOException {
		List<Group> groupList = new ArrayList<Group>();
		
		// get usermanagement service from homer
		/*Service service = MargeServer.getInstance().getService(SERVICE_TYPE, key.getDomain());
		if(service == null) {
			throw new DAOException("service not found for domain '"+key.getDomain()+"'");
		}*/
		
		// get xml
		//String url = service.getUrl() + USER_GROUPS_URI_TEMPLATE.replace("{domain}", key.getDomain()).replace("{user}", key.getUid());
		String url = barneyURL + USER_GROUPS_URI_TEMPLATE.replace("{domain}", key.getDomain()).replace("{user}", key.getUid());
		logger.debug("url: "+url);
		String response = HttpHelper.sendRequest("GET", url, null, null);
		try {
			Document doc = DocumentHelper.parseText(response);
			Node eNode = doc.selectSingleNode("//error");
			if(eNode != null) {
				throw new DAOException(eNode.getText());
			}
			
			// parse all groups
			List<Node> nList = doc.selectNodes("//group");
			for(Node node : nList) {
				Group group = UserManagerPOJOConverter.umxml2group(node.asXML());
				if(group!=null) {
					// be careful umxml2group does not include the domain
					group.setDomain(key.getDomain());
					groupList.add(group);
				}
			}
		} catch (DocumentException e) {
			throw new DAOException("could not parse response from barney");
		}
		return groupList;
	}

	/**
	 * Adds a user to a group
	 */
	public boolean addUserToGroup(UserKey uKey, GroupKey gKey) throws DAOException {
		boolean added;
		
		// parameter check
		if(!uKey.getDomain().equals(gKey.getDomain())) {
			throw new DAOException("the domains for the user and group specified do not match");
		}
		
		// get usermanagement service from homer
		/*Service service = MargeServer.getInstance().getService(SERVICE_TYPE, gKey.getDomain());
		if(service == null) {
			throw new DAOException("service not found for domain '"+gKey.getDomain()+"'");
		}*/
		
		// add user to group in barney
		//String url = service.getUrl() + GROUP_URI_TEMPLATE.replace("{domain}", gKey.getDomain()).replace("{group}", gKey.getGid());
		String url = barneyURL + GROUP_URI_TEMPLATE.replace("{domain}", gKey.getDomain()).replace("{group}", gKey.getGid());
		logger.debug("url: "+url);
		String body = "userID="+uKey.getUid();
		String response = HttpHelper.sendRequest("POST", url, body, "text/plain");
		try {
			Document doc = DocumentHelper.parseText(response);
			Node nNode = doc.selectSingleNode("//notification");
			Node eNode = doc.selectSingleNode("//error");
			// error response
			if(eNode != null) { 
				added = false;
			} 
			// no response
			else if(nNode == null) {
				throw new DAOException("no response from barney");
			}
			else { 
				added = nNode.getText().indexOf("creation:false") == -1;
			}
		} catch (DocumentException e) {
			throw new DAOException("could not parse response from barney");
		}
		
		return added;
	}

	/**
	 * Removes a user from a group
	 */
	public boolean removeUserFromGroup(UserKey uKey, GroupKey gKey)	throws DAOException {
		boolean removed;
		
		// parameter check
		if(!uKey.getDomain().equals(gKey.getDomain())) {
			throw new DAOException("the domains for the user and group specified do not match");
		}
		
		// get usermanagement service from homer
		/*Service service = MargeServer.getInstance().getService(SERVICE_TYPE, gKey.getDomain());
		if(service == null) {
			throw new DAOException("service not found for domain '"+gKey.getDomain()+"'");
		}*/
		
		// add user to group in barney
		//String url = service.getUrl() + GROUP_URI_TEMPLATE.replace("{domain}", gKey.getDomain()).replace("{group}", gKey.getGid()).replace("{member}", uKey.getUid());
		String url = barneyURL + GROUP_URI_TEMPLATE.replace("{domain}", gKey.getDomain()).replace("{group}", gKey.getGid()).replace("{member}", uKey.getUid());
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

}
