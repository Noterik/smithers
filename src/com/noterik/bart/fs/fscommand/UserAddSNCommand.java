/* 
* UserAddSNCommand.java
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
package com.noterik.bart.fs.fscommand;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.bart.marge.model.Service;
import com.noterik.bart.marge.server.MargeServer;
import com.noterik.springfield.tools.XMLHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

public class UserAddSNCommand implements Command {
	/** the GenerateUserCommand's log4j logger */
	private static Logger logger = Logger.getLogger(UserAddSNCommand.class);
	
	/** parameter constants */
	public static final String USER = "username";
	public static final String PASS = "password";
	
	/** user manager constants */
	public static final String GENERIC_USER_URI = "/domain/{domain}/user/{user}";
	public static final String GENERIC_COLLECTION_URI = "/domain/{domain}/user/{user}/collection/default";
	public static final String GENERIC_GROUP_URI = "/domain/{domain}/group/{group}";
	public static final String EMPTY = "N/A";
	
	/** filesystem constants */
	public static final String EMPTY_PROPERTIES = "<fsxml><properties/></fsxml>";
	public static final String DEFAULT_COLLECTION_PROPERTIES = "<fsxml><properties><title>default collection</title><description>collection automatically created upon user creation</description></properties></fsxml>";
	
	public String execute(String uri, String xml) {
		// get parameters
		Properties parameters = getInputParameters(xml);
		
		// get domain
		String domain = URIParser.getDomainFromUri(uri);
		
		// get user and pass
		String user = parameters.containsKey(USER) ? parameters.getProperty(USER) : null;
		String pass = parameters.containsKey(PASS) ? parameters.getProperty(PASS) : null;
		
		// default password
		if(user!=null && pass==null) {
			pass = user + "12";
		}
		
		// check parameters
		if( parameters==null || domain==null || user==null || pass==null ) {
			FSXMLBuilder.getErrorMessage("500", "No input found", "Please call this command as follows: useradd NAME PASSWORD", "http://teamelements.noterik.com/team");
		}
		
		// create user in UM
		addUserToUM(domain,user,pass);
		
		// create user in FS
		addUserToFS(domain,user);
		
		// create default collection
		addDefaultCollectionToFS(domain, user);
		
		return FSXMLBuilder.getStatusMessage("Successfully created user", "Successfully created user", uri);
	}

	/**
	 * Creates a user in the user manager
	 * 
	 * @param domain	domain of user
	 * @param username	name of user
	 * @param password	password of user
	 */
	private void addUserToUM(String domain, String username, String password) {
		logger.debug("adding user to UM -- domain: "+domain+", user: "+username);
		
		// get usermanager service
		MargeServer marge = new MargeServer();
		Service service = marge.getService("usermanager", domain);
		if(service==null) {
			logger.error("usermanager service was null");
			return;
		}
		
		// build user request
		String url = service.getUrl() + GENERIC_USER_URI.replace("{domain}", domain).replace("{user}", username);
		PostMethod method = new PostMethod(url);
		method.setParameter("firstName", EMPTY);
		method.setParameter("lastName", EMPTY);
		method.setParameter("password", password);
		method.setParameter("email", EMPTY);
		method.setParameter("birthdate", EMPTY);
		method.setParameter("telephone", EMPTY);
		logger.debug("about to send request to "+url);
		
		// handle request
		try {
			new HttpClient().executeMethod(method);
		} catch (HttpException e) {
			logger.error("",e);
		} catch (IOException e) {
			logger.error("",e);
		}
		
		// get response
		String response = null;
		try {
			response = method.getResponseBodyAsString();
		} catch (IOException e) {
			logger.error("",e);
		}
		
		// check response
		if(response!=null && response.equals("")) { /** weird enough this means is was successful*/
			logger.debug("user was created successfully");
			return;
		}
		logger.error("failed to create user, response was: >>>"+response+"<<<");
	}
	
	/**
	 * Add user to a group
	 * 
	 * @param domain		domain of user
	 * @param username		name of user	
	 * @param groupname		name of group
	 */
	public void addUserToGroup(String domain, String username, String groupname) {
		logger.debug("adding user to group UM -- domain: "+domain+", user: "+username+", group: "+groupname);
		
		// get usermanager service
		MargeServer marge = new MargeServer();
		Service service = marge.getService("usermanager", domain);
		if(service==null) {
			logger.error("usermanager service was null");
			return;
		}
		
		// build user request
		String url = service.getUrl() + GENERIC_GROUP_URI.replace("{domain}", domain).replace("{group}", groupname);
		PostMethod method = new PostMethod(url);
		method.setParameter("userID", username);
		logger.debug("about to send request to "+url);
		
		// handle request
		try {
			new HttpClient().executeMethod(method);
		} catch (HttpException e) {
			logger.error("",e);
		} catch (IOException e) {
			logger.error("",e);
		}
		
		// get response
		String response = null;
		try {
			response = method.getResponseBodyAsString();
		} catch (IOException e) {
			logger.error("",e);
		}
		
		// check response
		if(response!=null && response.equals("")) { /** weird enough this means is was successful*/
			logger.debug("user was added to group successfully");
			return;
		}
		logger.error("failed to add user to group, response was: >>>"+response+"<<<");
	}
	
	/**
	 * Creates a user in the filesystem
	 * 
	 * @param domain		domain of user
	 * @param username		name of user
	 */
	public void addUserToFS(String domain, String username) {
		logger.debug("adding user to FS -- domain: "+domain+", user: "+username);
		
		// final uri
		String uri = GENERIC_USER_URI.replace("{domain}", domain).replace("{user}", username);
		
		// check resource existence
		if(exists(uri)) {
			return;
		}
		
		// create resource
		boolean success = FSXMLRequestHandler.instance().saveFsXml(uri, EMPTY_PROPERTIES, "PUT", true);
		if(!success) {
			logger.error("failed to create user in filesystem");
		}
	}
	
	/**
	 * Creates a default collection for a user in the filesystem
	 * 
	 * @param domain		domain of user
	 * @param username		name of user
	 */
	public void addDefaultCollectionToFS(String domain, String username) {
		logger.debug("creating default collection for user -- domain: "+domain+", user: "+username);
		
		// final uri
		String uri = GENERIC_COLLECTION_URI.replace("{domain}", domain).replace("{user}", username);
		
		// check resource existence
		if(exists(uri)) {
			return;
		}
		
		// create resource
		boolean success = FSXMLRequestHandler.instance().saveFsXml(uri, DEFAULT_COLLECTION_PROPERTIES, "PUT", true);
		if(!success) {
			logger.error("failed to create default collection in filesystem");
		}
	}

	/**
	 * Returns the input parameters.
	 * 
	 * @param xml	The xml specifying the commands parameters.
	 * @return		The input parameters.
	 */
	private Properties getInputParameters(String xml){
		Properties props = new Properties();
		Document doc = XMLHelper.asDocument(xml);
		if(doc == null){
			return null;
		} else {
			Node propertiesNode = doc.selectSingleNode("./fsxml/properties");
			if(propertiesNode instanceof Element) {
				Element propertiesElement = (Element) propertiesNode;
				List<Element> elements = propertiesElement.elements();
				for(Element elem : elements) {
					props.put(elem.getName(), elem.getText());
				}
			}
		}		
		logger.debug(props.toString());
		return props;
	}
	
	/**
	 * Check if resource exists
	 * 
	 * @param uri
	 * @return
	 */
	public boolean exists(String uri) {
		return FSXMLRequestHandler.instance().hasProperties(uri);
	}
	
	public ManualEntry man() {
		return null;
	}
}
