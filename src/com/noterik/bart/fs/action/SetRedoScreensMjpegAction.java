/* 
* SetRedoScreensMjpegAction.java
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

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.XMLHelper;
import com.noterik.springfield.tools.fs.URIParser;

public class SetRedoScreensMjpegAction extends ActionAdapter {
	/**	serialVersionUID */
	private static final long serialVersionUID = 1L;
	
	/** the SetRedoScreensMjpegAction's log4j Logger */
	private static Logger logger = Logger.getLogger(SetRedoScreensMjpegAction.class);
	
	/** constants */
	private static final String SCREENS_CONFIG_URI = "/domain/{domain}/config/ingest/setting/screens";
	private static final String DEFAULT_SCREENS_PROPERTIES = "<fsxml><properties><format>file/mjpeg</format><redo>true</redo><useraw>original</useraw></properties></fsxml>";
	
	@Override
	public String run() {		
		logger.debug("**************************** starting SetRedoScreensMjpegAction ************************");
		String requestBody = event.getRequestData();
		String uri = event.getUri();
		String domain = URIParser.getDomainFromUri(uri);
		
		logger.debug("request body: " + requestBody);
		logger.debug("uri: " + uri);
		
		try {
			Document doc = DocumentHelper.parseText(requestBody);			
			Node mtNode = doc.selectSingleNode("//properties/mount");
			
			if(mtNode == null){
				logger.debug("Mounts are not set, redo will not be set to true");
				return null;
			}
			
			// get uri and properties of video
			String vidUri = uri.substring(0, uri.lastIndexOf("/rawvideo"));
			
			Document vidDoc = FSXMLRequestHandler.instance().getNodeProperties(vidUri, false);

			boolean hasScreens = vidDoc.selectSingleNode(".//screens") != null;
			if(hasScreens){
				logger.debug("Screens are already present. No need to set redo to true.");
				return null;
			}							
			
			logger.debug("Screens properties will be created");
			// get uri of redo tag
			String screensUri = vidUri + "/screens/1/properties";
			logger.debug("screensUri: " + screensUri);
			
			// check for domain preferences
			String screenProperties = DEFAULT_SCREENS_PROPERTIES;
			String configURI = SCREENS_CONFIG_URI.replace("{domain}", domain);
			Document configDoc = FSXMLRequestHandler.instance().getNodeProperties(configURI, false);
			if(configDoc != null) {
				logger.debug("Custom screens profile");
				Node configProperties = configDoc.selectSingleNode("//screensprofile/properties");
				screenProperties = "<fsxml>"+configProperties.asXML()+"</fsxml>";
			}

			logger.debug("redo properties: " + screenProperties);
			if(!FSXMLRequestHandler.instance().saveFsXml(screensUri, screenProperties, "PUT", true)) {
				throw new ActionException("Screens properties could not be set");
			}else{
				logger.debug("screens properties were created successfully");
			}
		} catch (Exception e) {
			logger.error("",e);
		}	
		return null;
	}	
}
