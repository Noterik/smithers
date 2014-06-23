package com.noterik.bart.fs.action.dance4life;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;

/**
 * Adds 1 to the viewcount of a presentation
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.action.dance4life
 * @access private
 * @version $Id: ViewCountAction.java,v 1.5 2009-02-11 10:34:34 jaap Exp $
 *
 */
public class ViewCountAction extends ActionAdapter {
	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(ViewCountAction.class);
	
	@Override
	public String run() {	
		if(true)return null;
		// parse request
		String requestBody = event.getRequestData();
		try {
			Document doc = DocumentHelper.parseText(requestBody);
			String presentationID = doc.valueOf("//properties/presentationid");
			String ip = doc.valueOf("//properties/ip");
			String domain = doc.valueOf("//properties/extdomain");
			
			logger.debug("id: " + presentationID + " -- ext domain: " + domain + " -- ip: " + ip);
			
			// add to the viewcount of this presentation
			addViewCount(domain,presentationID);
		} catch (Exception e) {
			logger.error(e);
		}
		
		return null;
	}
	
	/**
	 * Adds 1 the the viewcount specified by uri
	 * @param uri
	 */
	private void addViewCount(String domain, String uri) {
		String tViewcount = "", dViewcount = "";
		
		// get viewcounts
		try {
			tViewcount = FSXMLRequestHandler.instance().getPropertyValue(uri + "/properties/views_total");
			dViewcount = FSXMLRequestHandler.instance().getPropertyValue(uri + "/extdomain/"+domain+"/properties/views");
		} catch(Exception e) {
			logger.error(e);
		}
		
		// convert
		long tCount = 0, dCount = 0;
		try {
			tCount = Long.parseLong(tViewcount);
			dCount = Long.parseLong(dViewcount);
		} catch(Exception e) {
			logger.error(e);
		}
		
		// update
		tCount++;
		dCount++;
		
		// set viewcounts
		FSXMLRequestHandler.instance().updateProperty(uri + "/properties/views_total", "views_total", tCount+"", "PUT", true);
		FSXMLRequestHandler.instance().updateProperty(uri + "/extdomain/"+domain+"/properties/views", "views", dCount+"", "PUT", true);
	}
}
