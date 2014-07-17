/* 
* CreatePresentationAction.java
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
package com.noterik.bart.fs.action;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;

public class CreatePresentationAction extends ActionAdapter {
	private static Logger logger = Logger.getLogger(CreatePresentationAction.class);

	@Override
	public String run() {
		
		logger.debug("**************************** starting CreatePresentationAction ************************");

		String requestBody = event.getRequestData();
		logger.debug("request body: " + requestBody);
		
		String vidUri = "";
		
		try {
			Document doc = DocumentHelper.parseText(requestBody);			
			Node vidNode = doc.selectSingleNode("//referid");
			
			if (vidNode != null) {
				vidUri = vidNode.getText();
			}
		} catch (Exception e) {
			logger.error("",e);
		}
				
		String vidId = vidUri.substring(vidUri.lastIndexOf("/") + 1);
		String userUri = vidUri.substring(0, vidUri.lastIndexOf("/video"));
				
		
		logger.debug("video uri: " + vidUri);
		logger.debug("video id: " + vidId);
		logger.debug("user uri: " + userUri);
		
		
		// check if presentation already exists
		String presUri = userUri + "/presentation/p" + vidId;
		logger.debug("presentation uri: " + presUri);
		
		Boolean has = FSXMLRequestHandler.instance().hasProperties(presUri);
		if(has){
			logger.debug("presentation already exists, no need to create.");
			return null;
		}		
		
		String title = "";
		// get video properties
		Document vidDoc = FSXMLRequestHandler.instance().getNodeProperties(vidUri, false);
		Node titleNode = vidDoc.selectSingleNode(".//title");
			
		if(titleNode != null){
			title = titleNode.getText();
		}
						
			
		String presxml = buildPresXML(vidUri, title);
		logger.debug("xml to create presentation: " + presxml);
		
		// create the presentation
		String presPropsUri = presUri + "/properties";
		
		logger.debug("about to set xml for presentation: " + presxml + " into url: " + presPropsUri); 
		FSXMLRequestHandler.instance().handlePUT(presPropsUri, presxml);
		
		// create link in collection
		List<String> uris = FSXMLRequestHandler.instance().getReferParents(vidUri);
					
		Iterator<String> i = uris.iterator();
		while(i.hasNext()){
			String uri = i.next();
			
			if(uri.indexOf(userUri) != -1 && uri.indexOf("collection") != -1){
				
				String colUri = uri.substring(0, uri.lastIndexOf("/video"));
				// insert presentation in collection
				String colxml = buildColXML(presUri);
				String colAttrUri = colUri + "/presentation/p" + vidId + "/attributes";
				
				logger.debug("about to set xml for collection: " + colxml + " into url: " + colAttrUri); 
				FSXMLRequestHandler.instance().handlePUT(colAttrUri, colxml);
				
				return null;
			}
			logger.error("None of the uris was either a collection or belonged to this user.");
			return null;
		}
		logger.error("there was no uri refering to the requested video.");
		return null;
								
	}	
	
	
	private String buildPresXML(String vidUri, String title){
		
		StringBuffer xml = new StringBuffer("<fsxml>");
		xml.append("<properties>");
		xml.append("<title>" + title + "</title>");
		xml.append("<type>filesystem</type>");
		xml.append("<description/>");
		xml.append("</properties>");
		xml.append("<videoplaylist id='1'>");
		xml.append("<properties/>");
		xml.append("<video id='1' referid='" + vidUri + "'/>");
		xml.append("</videoplaylist>");
		xml.append("</fsxml>");
		return xml.toString();
		
	}
	
	
	private String buildColXML(String presUri){
		
		StringBuffer xml = new StringBuffer("<fsxml>");
		xml.append("<attributes>");
		xml.append("<referid>" + presUri + "</referid>");
		xml.append("</attributes>");
		xml.append("</fsxml>");
		
		return xml.toString();
	}
	
	
}
