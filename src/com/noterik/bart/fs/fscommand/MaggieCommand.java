/* 
* MaggieCommand.java
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
