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
