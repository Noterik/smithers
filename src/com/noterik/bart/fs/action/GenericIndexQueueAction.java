/* 
* GenericIndexQueueAction.java
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
package com.noterik.bart.fs.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.fs.URIParser;

public class GenericIndexQueueAction extends ActionAdapter {
private static final long serialVersionUID = 1L;
	
	/** log4j logger */
	private static Logger logger = Logger.getLogger(GenericIndexQueueAction.class);
	
	/** queue uri */
	private static final String QUEUE_URI = "/domain/{domainid}/service/smithers/queue/genericindex/job";
	
	/** filled queue uri */
	private String queueUri;
	
	/** domain id */
	private String domainid;
	
	private String[] topPriority = new String[2];	
	private List<String> referedObjects = null;
	private List<String> referedJobs = null;
	private String type;
	
	public String run() {			
		String eventUri = event.getUri();
		domainid = URIParser.getDomainIdFromUri(eventUri);		
		String objectUri = getSubUri(eventUri, 6);		
		
		logger.debug("event uri = "+eventUri+" domain = "+domainid+" object = "+objectUri);
		
		/* Top prio are properties of a object itself */
		topPriority[0] = "/domain/"+domainid+"/user/[^/]+/project/[^/]+$";
		topPriority[1] = "/domain/"+domainid+"/user/[^/]+/project/properties/[^/]+$";
		
		int priority = getPriority(eventUri);
		
		//object added/updated/deleted
		if (type.equals("project")) {
			getRefers(objectUri);
			putInQueue(eventUri, priority, objectUri);
		} else {
			//exception
		}
		return null;
	}
		
	private void putInQueue(String eventUri, int priority, String objectUri) {
		logger.debug("putInQueue eventuri = "+eventUri+" priority = "+priority+" objecturi = "+objectUri);
		
		/* Loop all collections where object is refered and add to queues */
		for (int i = 0; i < referedObjects.size(); i++) {
			String collectionObject = referedObjects.get(i) == null ? null : referedObjects.get(i);
			if (collectionObject != null) {
				//get domain from collection object and add to queue in domain
			}
			
			if (referedObjects.get(i) != null) {
				domainid = URIParser.getDomainIdFromUri(eventUri);
				queueUri = QUEUE_URI.replace("{domainid}", domainid);
				
				boolean jobExists = false;
				
				/* check if job is already in proper queue */
				for (int j = 0; j < referedJobs.size(); j++) {
					String refer = referedJobs.get(j);
					if (refer != null) {
						String[] parts = refer.substring(1).split("/");
						if (parts[1].equals(domainid)) {
							//select this job to check priority
							jobExists = true;
							String job = getSubUri(refer, 8);
							String jobContent = null;
							
							try {
								jobContent = FSXMLRequestHandler.instance().handleGET(job, null).getText();
							} catch (IOException e) { }
							Document jobProperties = null;
							try {
								jobProperties = DocumentHelper.parseText(jobContent);
							} catch (Exception e) { }
							
							String jobId = job.substring(job.lastIndexOf("/")+1);
							int currentPriority = jobProperties.selectSingleNode("//priority") == null ? 2 : Integer.parseInt(jobProperties.selectSingleNode("//priority").getText());
							logger.debug("current prio "+currentPriority);
							if (currentPriority > priority) {
								updatePriority(priority, jobId);
							}
						}
					}
				}
				/* Otherwise add as a new job */
				if (!jobExists) {
					logger.debug("call add job");
					addJob(objectUri, eventUri);
				}
				
				/* check for double entries */
				getRefers(eventUri);
				int jobCounter = 0;
				for (int k = 0; k < referedJobs.size(); k++) {
					String refer = referedJobs.get(k);
					if (refer != null) {
						String[] parts = refer.substring(1).split("/");
						if (parts[1].equals(domainid) && parts[5].equals("genericindex") && parts[6].equals("job")) {
							jobCounter++;
							if (jobCounter > 1) {
								logger.debug("remove double entry from queue");
								FSXMLRequestHandler.instance().deleteNodeProperties(refer, false);
							}
						}
					}
				}
			}
		}		
	}
	
	/* Put object in queue for reindexing */
	private void addJob(String objectUri, String eventUri) {
		/* only add valid object */
		if (objectUri.startsWith("/")) {		
			long timestamp = new Date().getTime();
			
			Document xml = DocumentFactory.getInstance().createDocument();
			Element fsxml = xml.addElement("fsxml");
			Element properties = fsxml.addElement("properties");
			properties.addElement(type).addText(objectUri);
			properties.addElement("priority").addText(String.valueOf(getPriority(eventUri)));
			properties.addElement("timestamp").addText(String.valueOf(timestamp));
			properties.addElement("type").addText(type);
			Element object = fsxml.addElement(type);
			object.addAttribute("id", "1");
			object.addAttribute("referid", objectUri);
			
			logger.debug(xml.asXML());
			logger.debug(queueUri);
			
			FSXMLRequestHandler.instance().handlePOST(queueUri, xml.asXML());
		}
	}
	
	/* update priority that is already in the queue */
	private void updatePriority(int priority, String id) {
		if (!id.equals("")) {
			FSXMLRequestHandler.instance().handlePUT("/domain/"+domainid+"/service/smithers/queue/genericindex/job/"+id+"/properties/priority", String.valueOf(priority));
		}
	}
	
	/* Get a sub part of the uri */
	private String getSubUri(String uri, int length) {		
		if(uri.startsWith("/")) {
			uri = uri.substring(1);
		}
		if(uri.endsWith("/")) {
			uri = uri.substring(0,uri.length()-1);
		}
		
		String[] parts = uri.split("/");
		if (parts.length < length) {
			return uri;
		}
		
		String subUri = "/";
		for (int i = 0; i < length; i++) {
			subUri += parts[i]+"/";
		}
		type = parts[4];
		
		return subUri.substring(0, subUri.length()-1);
	}
	
	private int getPriority(String eventUri) {		
		for (int i = 0; i < topPriority.length; i++) {
			Pattern p = Pattern.compile(topPriority[i]);
			Matcher m = p.matcher(eventUri);
			if (m.find()) {
				return 1;
			}
		}		
		return 2;
	}
	
	/* Get all parents for item */
	private void getRefers(String eventUri) {	
		String refer = "";
		List<String> refers = FSXMLRequestHandler.instance().getReferParents(eventUri);
		
		referedObjects = new ArrayList<String>(refers.size());
		referedJobs = new ArrayList<String>(refers.size());
		
		for (Iterator<String> iter = refers.iterator(); iter.hasNext(); ) {
			refer = iter.next();
			logger.debug("refer = "+refer);
			String[] parts = refer.substring(1).split("/");			
			
			// get objects from collection
			if ((parts[4].equals("collection") && parts[6].equals(type))) {
				referedObjects.add(refer);				
				logger.debug("added refer to list");
			} else if (parts[5].equals("genericindex") && parts[6].equals("job")) {
				referedJobs.add(refer);
				logger.debug("added refer to jobslist");
			} else {
				//logger.debug("not added refer to any list "+parts[4]+" "+parts[6]);			
			}
		}
	}
}
