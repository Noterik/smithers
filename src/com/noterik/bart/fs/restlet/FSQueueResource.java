/* 
* FSQueueResource.java
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
package com.noterik.bart.fs.restlet;

import org.restlet.data.Form;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import com.noterik.bart.fs.triggering.TriggerEvent;
import com.noterik.bart.fs.triggering.TriggerSystem;
import com.noterik.bart.fs.triggering.TriggerSystemManager;
import com.noterik.bart.fs.triggering.TriggerSystemQueue;

/**
 * The FSQueueResource is used to monitor the trigger system
 * 
 * Resource uri:
 * 		/queue
 * 
 * Agruments:
 *  	domain				The domain name of the trigger system queues you want to check
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2009
 * @package com.noterik.bart.fs.restlet
 * @access private
 *
 */
public class FSQueueResource extends ServerResource {
	
	// allowed actions: GET 
	public boolean allowPut() {return false;}
	public boolean allowPost() {return false;}
	public boolean allowGet() {return true;}
	public boolean allowDelete() {return false;}
	
	/**
	 * GET
	 */
	@Get
    public Representation doGet() {
		String responseBody = "";
		
		// get parameters
		Form qForm = getRequest().getResourceRef().getQueryAsForm();
		String domain = qForm.getFirstValue("domain",null);
		String queue = qForm.getFirstValue("queue",null);
		if(domain != null && queue != null) {
			responseBody = getQueueOverview(domain, queue);
		} else if(domain!=null) {
			responseBody = getDomainOverview(domain);
		} else {
			responseBody = "please provide domain name";
		}
		
		// return
		Representation entity = new StringRepresentation(responseBody);
        return entity;
	}

	/**
	 * Gives a string representation of the trigger system of a domain
	 * 
	 * @param domain
	 * @return
	 */
	private String getDomainOverview(String domain) {
		String domainOverview = "DOMAIN: "+domain+"\n\n";
		
		TriggerSystem ts = TriggerSystemManager.getInstance().getDomainTS(domain);
		domainOverview += "Get Queue Size: "+ts.getGetQueue().size()+"\n";
		domainOverview += "Put Queue Size: "+ts.getPutQueue().size()+"\n";
		domainOverview += "Post Queue Size: "+ts.getPostQueue().size()+"\n";
		domainOverview += "Delete Queue Size: "+ts.getDeleteQueue().size()+"\n";
		
		return domainOverview;
	}
	
	/**
	 * Gives a string representation of the trigger system of a specific queue
	 * 
	 * @param domain
	 * @param queue
	 * @return
	 */
	private String getQueueOverview(String domain, String queue) {
		StringBuffer domainOverview = new StringBuffer(); 
		domainOverview.append("DOMAIN: "+domain+"\n");
		domainOverview.append("QUEUE: "+queue+"\n\n");
		
		// get queue
		TriggerSystem ts = TriggerSystemManager.getInstance().getDomainTS(domain);
		TriggerSystemQueue tq = null;
		if(queue.equalsIgnoreCase("get")) {
			tq = ts.getGetQueue();
		} else if(queue.equalsIgnoreCase("put")) {
			tq = ts.getPutQueue();
		} else if(queue.equalsIgnoreCase("post")) {
			tq = ts.getPostQueue();
		} else if(queue.equalsIgnoreCase("delete")) {
			tq = ts.getDeleteQueue();
		}
		
		// check
		if(tq == null) {
			domainOverview.append("Please sepcify queue type: {GET, PUT, POST, DELETE}");
			return domainOverview.toString();
		}
		
		// get snapshot
		TriggerEvent[] events = tq.toArray();
		for(TriggerEvent event : events) {
			domainOverview.append(event + "\n");
		}
		
		return domainOverview.toString();
	}
}
