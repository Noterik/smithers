/* 
* CreateUserIndexAction.java
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
package com.noterik.bart.fs.action.user;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Node;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.fs.URIParser;

public class CreateUserIndexAction extends ActionAdapter {
	/** the CreateUserIndexAction's log4j Logger */
	private static Logger logger = Logger.getLogger(CreateUserIndexAction.class);
	
	/** place where all the index settings are stored */
	private static final String GENERIC_CONFIG_URI = "/domain/{domain}/config/index/setting";
	
	/** place where indexes are configured per user */
	private static final String GENERIC_USER_CONFIG_URI = "/domain/{domain}/user/{user}/config/{index}";
	
	private String domain;
	private String user;
	
	@Override
	public String run() {
		logger.debug("starting run");
		
		// parse event data
		parseEventData();
		
		// check which indexes to create
		Map<String,Properties> indexes = getDomainIndexConfigurations();
		logger.debug("indexes: "+indexes);
		
		// create index if needed
		for(String index : indexes.keySet()) {
			String indexURI = GENERIC_USER_CONFIG_URI.replace("{domain}",domain).replace("{user}",user).replace("{index}",index);
			Properties indexProperties = indexes.get(index);
			if(!exists(indexURI)) {
				createUserIndexConfiguration(indexURI,indexProperties);
			}
		}
		
		return null;
	}

	/**
	 * Parses event data
	 */
	private void parseEventData() {
		String uri = event.getUri();
		domain = URIParser.getDomainFromUri(uri);
		user = URIParser.getUserFromUri(uri);
	}
	
	/**
	 * Returns the domains default indexes
	 * 
	 * @return
	 */
	private Map<String,Properties> getDomainIndexConfigurations() {
		Map<String,Properties> map = new HashMap<String, Properties>();
		
		// get list from filesystem
		String uri = GENERIC_CONFIG_URI.replace("{domain}", domain);
		Document doc = FSXMLRequestHandler.instance().getNodePropertiesByType(uri, 1, 0, -1);
		logger.debug("doc: "+doc.asXML());
		List<Node> nList = doc.selectNodes("//setting");
		for(Node node : nList) {
			String id = node.valueOf("@id");
			if(id!=null) {
				Properties properties = new Properties();
				List<Node> pList = node.selectNodes("properties/child::*");
				for(Node pNode : pList) {
					properties.put(pNode.getName(), pNode.getText());
				}
				map.put(id, properties);
			}
		}
		
		return map;
	}
	
	/**
	 * Create a new configuration of an index for a user
	 * 
	 * @param indexConfigurationURI
	 * @param indexProperties
	 */
	private void createUserIndexConfiguration(String indexConfigurationURI, Properties indexProperties) {
		logger.debug("creating new index configuration -- index: "+indexConfigurationURI+", properties: "+indexProperties);
		
		// update indexuri
		String indexURI = indexProperties.getProperty("indexuri");
		if(indexURI==null) {
			logger.error("no indexuri property was found");
			return;
		}
		indexURI = indexURI.replace("{domain}", domain).replace("{user}", user);
		indexProperties.put("indexuri", indexURI);
		
		
		// generate xml
		String xml = generateXML(indexProperties);
		
		// save xml
		FSXMLRequestHandler.instance().saveFsXml(indexConfigurationURI, xml, "PUT", true);		
	}
	
	/**
	 * Returns an FSXML string given Properties
	 * 
	 * @param properties
	 * @return
	 */
	private String generateXML(Properties properties) {
		StringBuffer buff = new StringBuffer();
		buff.append("<fsxml>");
		buff.append("<properties>");
		for(String key : properties.stringPropertyNames()) {
			String value = properties.getProperty(key);
			buff.append("<"+key+">"+value+"</"+key+">");
		}
		buff.append("</properties>");
		buff.append("</fsxml>");
		return buff.toString();
	}
	
	/**
	 * Checks if a resource exists in the filesystem
	 * 
	 * @param uri
	 * @return
	 */
	private boolean exists(String uri) {
		return FSXMLRequestHandler.instance().hasProperties(uri);
	}
}
