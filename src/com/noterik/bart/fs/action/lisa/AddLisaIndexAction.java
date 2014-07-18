/* 
* AddLisaIndexAction.java
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