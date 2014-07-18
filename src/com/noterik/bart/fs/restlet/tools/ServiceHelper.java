/* 
* ServiceHelper.java
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
package com.noterik.bart.fs.tools;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.noterik.bart.marge.model.Service;
import com.noterik.bart.marge.server.MargeServer;

public class ServiceHelper {
	
	/** the ServiceHelper's log4j Logger */
	private static Logger logger = Logger.getLogger(ServiceHelper.class);
	
	public static Service getService(String domain, String serviceName) {
		MargeServer marge = MargeServer.getInstance();
		HashMap<Integer, Service> map = marge.getServices(serviceName, domain);
		Service service =  null;
		
		// get current host
    	String hostIP = "UNKNOWN";
    	try {
    		hostIP = java.net.InetAddress.getLocalHost().getHostAddress(); // get this ip
		} catch (UnknownHostException e) {}
		
		// determine user manager on the same host
		Integer key;
		String serviceHost = null;
		String serviceHostIP = null;
		if(map!=null) {
			for(Iterator iter = map.keySet().iterator(); iter.hasNext(); ) {
				key = (Integer)iter.next();
				service = map.get(key);
				try {
					serviceHost = service.app_host();
					serviceHostIP = java.net.InetAddress.getByName(serviceHost).getHostAddress();
				} catch (UnknownHostException e) {
					logger.error("Could not determine host address",e);
				}
				
				// preferably this server
				if(serviceHostIP!=null) {
					if(serviceHostIP.equals(hostIP)) {
						break;
					} 
				}
			}
		} 
		
		// check if service not is null
		if(service == null) {
			service = marge.getService(serviceName, domain);
		}
		
		return service;
	}
}
