/* 
* flash.java
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
package com.noterik.bart.fs.fscommand.dynamic.config;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.fscommand.DynamicCommand;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.XMLHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

public class flash implements DynamicCommand {
	/** Logger */
	private static Logger logger = Logger.getLogger(flash.class);

	Document returnXml = DocumentHelper.createDocument();
	Element fsxml = returnXml.addElement("fsxml");
	String domain = "";
	String user = "";
	String collection = "";
	
	public String run(String uri,String xml) {	
		logger.debug("start dynamic/config/flash");
		fsxml.addElement("properties");
		
		domain = URIParser.getDomainIdFromUri(uri);			
		user = URIParser.getUserIdFromUri(uri);
		collection = URIParser.getCollectionIdFromUri(uri);

		if (domain == null){
			return FSXMLBuilder.getErrorMessage("404", "Not found",
					"You have to supply an existing uri", "http://teamelements.noterik.nl/team");
		}
		
		Node config = getConfig(uri, xml);
		
		if (config != null) {
			fsxml.add(config);
			addPlayer(config);
		}

		return fsxml.asXML();
	}
	
	private Node getConfig(String uri, String xml) {		
		Document conf = null;
		Document tmpConf = null;		
		
		// domain conf
		conf = FSXMLRequestHandler.instance().getNodeProperties("/domain/"+domain+"/config/presentation/filesystem/1", false);
		//check if override is allowed
		Boolean allowReplace = conf.selectSingleNode("/fsxml/filesystem[@id='1']/properties/allow_replace") == null ? false : Boolean.valueOf(conf.selectSingleNode("/fsxml/filesystem[@id='1']/properties/allow_replace").getText());
		
		//check if there is a sponsor, if so take his config
		Document doc = XMLHelper.asDocument(xml);
		String  sponsor = doc.selectSingleNode("//sponsor") == null ? null : doc.selectSingleNode("//sponsor").getText();
		
		if (sponsor != null) {
			// sponsor conf
			tmpConf = FSXMLRequestHandler.instance().getNodeProperties(sponsor+"/config/presentation/filesystem/1", false);	
			
			if (tmpConf != null && (conf == null || !allowReplace)) {
				conf = tmpConf;
				allowReplace = conf.selectSingleNode("/fsxml/filesystem[@id='1']/properties/allow_replace") == null ? false : Boolean.valueOf(conf.selectSingleNode("/fsxml/filesystem[@id='1']/properties/allow_replace").getText());
			} else if (tmpConf != null && allowReplace) {
				handleIncludeExcludeNodes(conf, tmpConf);
				allowReplace = tmpConf.selectSingleNode("/fsxml/filesystem[@id='1']/properties/allow_replace") == null ? false : Boolean.valueOf(tmpConf.selectSingleNode("/fsxml/filesystem[@id='1']/properties/allow_replace").getText());
				tmpConf = null;
			}			
		}
		
		// user conf
		if (user != null) {	
			tmpConf = FSXMLRequestHandler.instance().getNodeProperties("/domain/"+domain+"/user/"+user+"/config/presentation/filesystem/1", false);		
			if (tmpConf != null && (conf == null || !allowReplace)) {
				conf = tmpConf;
				allowReplace = conf.selectSingleNode("/fsxml/filesystem[@id='1']/properties/allow_replace") == null ? false : Boolean.valueOf(conf.selectSingleNode("/fsxml/filesystem[@id='1']/properties/allow_replace").getText());
			} else if (tmpConf != null && allowReplace) {			
				handleIncludeExcludeNodes(conf, tmpConf);
				allowReplace = tmpConf.selectSingleNode("/fsxml/filesystem[@id='1']/properties/allow_replace") == null ? false : Boolean.valueOf(tmpConf.selectSingleNode("/fsxml/filesystem[@id='1']/properties/allow_replace").getText());
				tmpConf = null;
			}
		}
		
		// user collection conf
		if (user != null && collection != null) {
			tmpConf = FSXMLRequestHandler.instance().getNodeProperties("/domain/"+domain+"/user/"+user+"/collection/"+collection+"/config/presentation/filesystem/1", false);
			if (tmpConf != null && (conf == null || !allowReplace)) {
				conf = tmpConf;
			} else if (tmpConf != null && allowReplace) {
				handleIncludeExcludeNodes(conf, tmpConf);
			}
		}
		
		if (conf != null) {
			return conf.selectSingleNode("fsxml/filesystem[@id='1']").detach();
		}	
		return null;
	}
	
	private void handleIncludeExcludeNodes(Document conf, Document tmpConf) {
		List<Node> includeNodes = tmpConf.selectNodes("/fsxml/filesystem[@id='1']/*[@id and not(ends-with(@id,'_exclude'))]");
		List<Node> excludeNodes = tmpConf.selectNodes("/fsxml/filesystem[@id='1']/*[ends-with(@id,'_exclude')]");
		
		logger.debug("number of includeNodes = "+includeNodes.size());
		for (int j = 0; j < includeNodes.size(); j++) {
			logger.debug(j+" = "+includeNodes.get(j).toString());
		}
		logger.debug("number of excludeNodes = "+excludeNodes.size());
		for (int j = 0; j < excludeNodes.size(); j++) {
			logger.debug(j+" = "+excludeNodes.get(j).toString());
		}
		
		Element base = (Element) conf.selectSingleNode("/fsxml/filesystem[@id='1']");
		
		if (includeNodes != null) {
			for (int i = 0; i < includeNodes.size(); i++) {
				String nodename = includeNodes.get(i).getName();					
				String nodeid = includeNodes.get(i).valueOf("@id");
				
				logger.debug("check if node exists "+nodename+" id "+nodeid);
				
				Node existingNode = base.selectSingleNode(nodename+"[@id='"+nodeid+"']");
				if (existingNode != null) {
					logger.debug("node exists, replace");
					List contentOfBase = base.content();
					int index = contentOfBase.indexOf(existingNode);
					contentOfBase.set(index, includeNodes.get(i).detach());
				} else {
					base.add(includeNodes.get(i).detach());
				}
			}
		}
		
		if (excludeNodes != null) {
			logger.debug("handling exclude nodes for user");
			for (int i = 0; i < excludeNodes.size(); i++) {
				logger.debug("handling exclude node nr "+i);
				String nodename = excludeNodes.get(i).getName();					
				String nodeid = excludeNodes.get(i).valueOf("@id");					
				nodeid = nodeid.substring(0, nodeid.lastIndexOf("_exclude"));
				
				logger.debug("about to exclude "+nodename+" with id "+nodeid);
				
				Node remove = base.selectSingleNode(nodename+"[@id='"+nodeid+"']");
				if (remove != null) {
					logger.debug("node to exclude found, detach");
					remove.detach();
				}
			}
		}
	}
	
	private void addPlayer(Node configNode) {
		List<Node> players = configNode.selectNodes("//player");
		
		for(Iterator<Node> iter = players.iterator(); iter.hasNext(); ) {
			Element player = (Element) iter.next();
		
			String refer = player.selectSingleNode("@referid") == null ? "" : player.selectSingleNode("@referid").getText();
			if (!refer.equals("")) {
				Document playerXml = FSXMLRequestHandler.instance().getNodeProperties(refer, false);
				if (playerXml != null) {
					Element p = (Element) playerXml.selectSingleNode("fsxml/player").detach();
					p.addAttribute("fullid", refer);
					fsxml.add(p);
				}				
			}
		}
	}
	
}
