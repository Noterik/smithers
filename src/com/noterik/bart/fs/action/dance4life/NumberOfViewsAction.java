package com.noterik.bart.fs.action.dance4life;

import org.apache.log4j.Logger;
import org.dom4j.Document;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;

public class NumberOfViewsAction extends ActionAdapter {
	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(EmailAction.class);
	
	@Override
	public String run() {	
		try {
			logger.debug("NumberOfViewsAction: generating total number of views");
			
			// get number of users
			String uri = "/domain/dance4life/user/anonymous/view";
			Document doc = FSXMLRequestHandler.instance().getNodePropertiesByType(uri, 0, 0, 0);
			String usersTotal = doc.valueOf("//totalResultsAvailable");
			
			// create fsxml
			String fsxml = "<fsxml><properties><views_total>"+usersTotal+"</views_total></properties></fsxml>";
			
			// set output
			script.getOutput().setOutput(fsxml);
		} catch (Exception e) {
			logger.debug("NumberOfViewsAction: could not generate number of views total: " + e.getMessage());
			logger.error(e);
		}
		
		return null;
	}
}
