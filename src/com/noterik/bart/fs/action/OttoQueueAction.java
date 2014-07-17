/* 
* OttoQueueAction.java
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

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.fs.URIParser;

public class OttoQueueAction extends ActionAdapter {
	
	private static final long serialVersionUID = 1L;
	
	/* logger */
	private static Logger logger = Logger.getLogger(OttoQueueAction.class);
	
	/* queue uri */
	private static final String QUEUE_URI = "/domain/{domainid}/service/otto/queue/{queueid}/job";

	/* domain id */
	private String domainid;
	
	/* queue id */
	private String queueid = "default";
	
	@Override
	public String run() {
		putInQueue();
		
		return null;
	}
	
	private void putInQueue() {	
		String uri = event.getUri();
		domainid = URIParser.getDomainFromUri(uri);
		
		String queueUri = QUEUE_URI.replace("{domainid}", domainid).replace("{queueid}", queueid);
		
		logger.debug("queue ="+queueUri);
		
		StringBuffer xml = new StringBuffer("<fsxml>");
		xml.append("<properties/>");
		xml.append("<export id='1' referid='"+uri+"'/>");
		xml.append("</fsxml>");
		
		logger.debug("xml = "+xml.toString());
		
		String response = FSXMLRequestHandler.instance().handlePOST(queueUri, xml.toString());
		logger.debug(response);
		
		
	}
}
