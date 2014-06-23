package com.noterik.bart.fs.fscommand;

import org.apache.log4j.Logger;

import com.noterik.bart.fs.tools.ServiceHelper;
import com.noterik.bart.marge.model.Service;
import com.noterik.springfield.tools.HttpHelper;
import com.noterik.springfield.tools.fs.URIParser;

public class MaggieCommand implements Command {
	/** Logger */
	private static Logger logger = Logger.getLogger(MaggieCommand.class);
	
	/**
	 * Execute the maggie command.
	 * This is not more than a simple pass through
	 */	
	public String execute(String uri, String xml) {	
		logger.debug("enter maggie command");
		String domain = URIParser.getDomainFromUri(uri);
		logger.debug("DOMAIN: " + domain);
		/*Service service = ServiceHelper.getService(domain, "indexmanager");
		logger.debug("SERVICE: " + service);
		if (service == null) {
			logger.error("MaggieCommand: Service was null");
			return null;
		}*/
		//String maggieUrl = service.getUrl()+uri;
		String maggieUrl = "http://localhost:8180/maggie"+uri;
		logger.debug("maggieURL = "+maggieUrl);
		String response = HttpHelper.sendRequest("POST", maggieUrl, xml, "text/xml");
		logger.debug("maggie response = "+response);
		
		return response;
	}
	
	public ManualEntry man() {
		return null;
	}
}
