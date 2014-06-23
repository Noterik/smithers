package com.noterik.bart.fs.action.lisa;

import org.apache.log4j.Logger;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.tools.ServiceHelper;
import com.noterik.bart.marge.model.Service;
import com.noterik.springfield.tools.HttpHelper;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * Add an index to lisa
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.action.lisa
 * @access private
 * @version $Id: AddLisaIndexAction.java,v 1.11 2011-07-01 11:38:56 derk Exp $
 *
 */
public class AddLisaIndexAction  extends ActionAdapter{
	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(AddLisaIndexAction.class);
	
	@Override
	public String run() {
		System.out.println("ADDLISA ACTION BLOCKED CLUSTER 2.0 (Daniel)");
		if (1==1) return null;
		
		logger.debug("Lisa Add action: " + event.getUri() + "\n" + event.getRequestData());
		
		// construct lisa uri
		String domain = URIParser.getDomainFromUri(event.getUri());
		Service service = ServiceHelper.getService(domain, "searchmanager");
		
		// check service
		if(service==null) {
			logger.error("Could not find searchmanager for "+domain+". Service was null.");
			return null;
		}
		
		// final url
		String finalUrl = service.getUrl() + event.getUri();
		
		// do request
		String response = HttpHelper.sendRequest("PUT", finalUrl, event.getRequestData(),"text/xml");
		logger.debug("response of lisa2 was: " + response);
		
		return null;
	}
}