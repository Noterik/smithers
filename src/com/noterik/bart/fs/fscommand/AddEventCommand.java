/* 
* AddEventCommand.java
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.XMLHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;

public class AddEventCommand implements Command {
	/** Logger */
	private static Logger logger = Logger.getLogger(AddEventCommand.class);
	
	private static final SimpleDateFormat PRESENTATION_STARTTIME_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
	private static final SimpleDateFormat PRESENTATION_LIVETIME_FORMAT = new SimpleDateFormat("EEE MMM dd yyyy hh:mm:ss a");
	private static final SimpleDateFormat EVENT_STARTTIME_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
	private static final SimpleDateFormat LASTUPDATE_FORMAT = new SimpleDateFormat("EEE MMM dd yyyy hh:mm:ss a");
	private static final String EMPTY_PROPERTIES = "<fsxml><properties/></fsxml>";
	
	public String execute(String uri, String xml) {
		logger.debug("Add event of presentation "+uri);
		logger.debug("Add event xml "+xml);
		
		Document doc = XMLHelper.asDocument(xml);
		List<Node> events;
		String ticket = "";
		String user = "";
		
		if(doc == null){
			logger.error("Could not parse xml");
			return FSXMLBuilder.getErrorMessage("500","No xml found", "Please provide xml", "");
		} else {
			events = doc.selectNodes("/fsxml/*");
			user = doc.selectSingleNode("//properties/user") == null ? "" : doc.selectSingleNode("//properties/user").getText();
			user = user.toLowerCase();
			ticket = doc.selectSingleNode("//properties/ticket") == null ? "" : doc.selectSingleNode("//properties/ticket").getText();
		}
		
		//TODO: validate ticket
		
		// create playlist if it does not exist
		createPlaylistIfNotExists(uri);
		
		for (Iterator<Node> it = events.iterator(); it.hasNext(); ) {
			Node event = it.next();
			
			if (!event.getName().equals("properties")) {
				logger.debug("received "+event.asXML());
				
				String starttime = event.selectSingleNode("properties/starttime") == null ? "" : event.selectSingleNode("properties/starttime").getText();
				String duration = event.selectSingleNode("properties/duration") == null ? "" : event.selectSingleNode("properties/duration").getText();
				String reason = event.selectSingleNode("properties/reason") == null ? "" : event.selectSingleNode("properties/reason").getText();
				
				logger.debug("starttime = "+starttime+" duration = "+duration +" reason = "+reason);
				
				// determine starttime relative to beginning of video
				long stSpringfield = determineCorrectStarttime(uri,starttime);

				if (starttime.equals("") || duration.equals("") || reason.equals("")) {
					logger.error("Not all mandatory fields where provided");
					return FSXMLBuilder.getErrorMessage("500","No all mandatory fields available", "Please provide all mandatory fields in your request", "");
				}
				
				// create new 'bookmark'
				StringBuffer bmXml = new StringBuffer(); 
				bmXml.append("<fsxml>");
				bmXml.append("<properties>");
				bmXml.append("<title><![CDATA[");
				bmXml.append(reason);
				bmXml.append("]]></title>");
				bmXml.append("<description><![CDATA[");
				bmXml.append(reason);
				bmXml.append("]]></description>");
				bmXml.append("<starttime>");
				bmXml.append(stSpringfield);
				bmXml.append("</starttime>");
				bmXml.append("<duration>");
				bmXml.append(duration);
				bmXml.append("</duration>");
				bmXml.append("<creator>");
				bmXml.append(user);
				bmXml.append("</creator>");
				bmXml.append("<changedby>");
				bmXml.append(user);
				bmXml.append("</changedby>");
				bmXml.append("<lastupdate>");
				bmXml.append( LASTUPDATE_FORMAT.format(new Date()) );
				bmXml.append("</lastupdate>");
				bmXml.append("</properties>");
				bmXml.append("</fsxml>");
				String bmUri = uri + "/videoplaylist/1/bookmark";
				String resp = FSXMLRequestHandler.instance().handlePOST(bmUri, bmXml.toString());
				logger.debug(resp);
			}
		}	

		return FSXMLBuilder.getFSXMLStatusMessage("The properties where successfully added", "", "");
	}
	
	/**
	 * Determines the starttime relative to the beginning of a video, given a presentation URI and a date string
	 * 
	 * @param uri			presentation URI
	 * @param starttime		date string supplied by Zaphyrion
	 * @return
	 */
	private long determineCorrectStarttime(String uri, String eDateStr) {
		// default value
		long starttime = 0;
		long eDate = 0;
		long pDate = 0;
		
		// parse event date string
		try {
			eDate = EVENT_STARTTIME_FORMAT.parse(eDateStr).getTime();
		} catch(Exception e) {
			logger.error("Could not parse event date string (" + eDateStr + ")");
			return starttime;
		}
		
		// TODO: get presentation property
		String pDateStr = FSXMLRequestHandler.instance().getPropertyValue(uri + "/properties/livetime");
		
		// parse presentation date string
		try {
			pDate = PRESENTATION_LIVETIME_FORMAT.parse(pDateStr).getTime();
		} catch(Exception e) {
			logger.error("Could not parse presentation date string (" + pDateStr + ")");
			return starttime;
		}	
		
		starttime = eDate - pDate;
		if(starttime < 0) {
			starttime = 0;
		}
		return starttime;
	}

	/**
	 * Creates a playlist if necessary 
	 * @param uri
	 */
	private void createPlaylistIfNotExists(String uri) {
		String plUri = uri + "/videoplaylist/1";
		boolean exists = FSXMLRequestHandler.instance().hasProperties(plUri);;
		if(!exists) {
			FSXMLRequestHandler.instance().saveFsXml(plUri, EMPTY_PROPERTIES, "PUT", true);
		}
	}
	
	public ManualEntry man() {
		return null;
	}
}
