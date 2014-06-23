package com.noterik.bart.fs.action;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * Action that puts a new job in the Nelson queue
 * This job can be the creation of thumbnails of an image,
 * or taking screenshots of a video.
 *
 * @author Levi Pires <l.pires@noterik.nl>
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.action
 * @access private
 * @version $Id: NelsonQueueAction.java,v 1.10 2011-07-01 11:38:56 derk Exp $
 *
 */
public class NelsonQueueAction extends ActionAdapter {
	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(NelsonQueueAction.class);
	
	/**
	 * queue uri
	 */
	private static final String QUEUE_URI = "/domain/{domainid}/service/nelson/queue/{queueid}/job";
	
	/**
	 * redo / don't redo the job
	 */
	private static final String REDO = "true";
	private static final String DONT_REDO = "false";
	
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
		
		logger.debug("starting action");
		
		// parse request
		String requestBody = event.getRequestData();
		try {
			Document doc = DocumentHelper.parseText(requestBody);
			Node node = doc.selectSingleNode("//properties/redo");
			if(node==null || !node.getText().toLowerCase().equals(REDO)) {
				logger.debug("Not redoing the job");
				return null;
			} else {
				logger.debug("about to put in queue");
				putInQueue();
			}
		} catch (Exception e) {
			logger.error("",e);
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
		
		// build xml
		StringBuffer xml = new StringBuffer("<fsxml>");
		xml.append("<properties/>");
		xml.append("<nelsonjob id='1' referid='"+uri+"'/>");
		xml.append("</fsxml>");
		
		logger.debug("queue uri: " + queueUri);
		logger.debug("xml: " + xml.toString());
		
		// do request (internally)
		String response = FSXMLRequestHandler.instance().handlePOST(queueUri, xml.toString());
		logger.debug(response);
	}
}
