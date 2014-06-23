package com.noterik.bart.fs.action.dance4life;

import org.apache.log4j.Logger;
import org.dom4j.Document;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;

public class NumberOfUsersAction extends ActionAdapter {
	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(EmailAction.class);
	
	@Override
	public String run() {	
		try {
			logger.debug("NumberOfUsersAction: generating total number of users");
			
			// get number of users
			String uri = "/domain/dance4life/user";
			Document doc = FSXMLRequestHandler.instance().getNodePropertiesByType(uri, 0, 0, 0);
			String usersTotal = doc.valueOf("//totalResultsAvailable");
			
			// create fsxml
			String fsxml = "<fsxml><properties><users_total>"+usersTotal+"</users_total></properties></fsxml>";
			
			// set output
			script.getOutput().setOutput(fsxml);
		} catch (Exception e) {
			logger.debug("NumberOfUsersAction: could not generate number of users total: " + e.getMessage());
			logger.error(e);
		}
		
		return null;
	}
}
