/* 
* MomarQueueAction.java
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

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * Action that puts certain rawvideo's in a queue for transcoding
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.action
 * @access private
 * @version $Id: MomarQueueAction.java,v 1.10 2011-07-08 08:42:08 derk Exp $
 *
 */
public class MomarQueueAction extends ActionAdapter {
	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(MomarQueueAction.class);
	
	/**
	 * queue uri
	 */
	private static final String QUEUE_URI = "/domain/{domainid}/service/momar/queue/{queueid}/job";
	
	/**
	 * queue overview uri
	 */
	private static final String QUEUE_OVERVIEW_URI = "/domain/{domainid}/service/momar/queue";
	
	/**
	 * do reencode / don't reencode
	 */
	private static final String DO_REENCODE = "true";
	private static final String DONT_REENCODE = "false";
	private static final String HIGH = "high";
	private static final String LOW = "low";
	
	/**
	 * domain id
	 */
	private String domainid;
	
	/**
	 * queueid
	 * TODO: variable queue id
	 */
	private String queueid = "default";
	
	@Override
	public String run() {
		// parse request
		String requestBody = event.getRequestData();
		try {
			Document doc = DocumentHelper.parseText(requestBody);
			Node node = doc.selectSingleNode("//properties/reencode");
			if(node==null || !node.getText().toLowerCase().equals(DO_REENCODE)) {
				logger.debug("Not reencoding");
				return null;
			} else {
				putInQueue();
			}
		} catch (Exception e) {
			logger.error(e);
		}
		
		return null;
	}
	
	/**
	 * Put the job in the queue
	 */
	private void putInQueue() {
		// TODO: get queueid
		// get domainid
		String uri = event.getUri();
		domainid = URIParser.getDomainFromUri(uri);
		
		// build jobs uri
		String queueUri = QUEUE_URI.replace("{domainid}", domainid).replace("{queueid}",queueid);
		
		// add raw 2 to high priority queue if available
		if (uri.substring(uri.lastIndexOf("/")+1).equals("2")) {
			logger.debug("raw 2");
			String queueOverviewUri = QUEUE_OVERVIEW_URI.replace("{domainid}", domainid);
			
			Document queue = FSXMLRequestHandler.instance().getNodePropertiesByType(queueOverviewUri);
			if (queue.selectSingleNode("//queue[@id='"+HIGH+"']") != null) {
				logger.debug("high priority queue available");
				queueUri = QUEUE_URI.replace("{domainid}", domainid).replace("{queueid}",HIGH);
			}			
		}
		
		// build xml
		StringBuffer xml = new StringBuffer("<fsxml>");
		xml.append("<properties/>");
		xml.append("<rawvideo id='1' referid='"+uri+"'/>");
		xml.append("</fsxml>");
		
		logger.debug("queue uri: " + queueUri);
		logger.debug("xml: " + xml.toString());
		
		// do request (internally)
		String response = FSXMLRequestHandler.instance().handlePOST(queueUri, xml.toString());
		logger.debug(response);
		
		// put job id into rawvideo
		try {
			Document doc = DocumentHelper.parseText(response);
			String jobUri = doc.valueOf("//properties/uri");
			FSXMLRequestHandler.instance().updateProperty(uri+"/properties/job", "job", jobUri, "PUT", false);
		} catch (DocumentException e) {
			logger.error("",e);
		}
	}
}
