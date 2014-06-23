package com.noterik.bart.fs.action;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.io.*;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.fs.URIParser;

public class PresentationIndexQueueAction extends ActionAdapter {
	
	private static final long serialVersionUID = 1L;
	
	/** log4j logger */
	private static Logger logger = Logger.getLogger(PresentationIndexQueueAction.class);
	
	/** queue uri */
	private static final String QUEUE_URI = "/domain/{domainid}/service/smithers/queue/presentationindex/job";
	
	/** filled queue uri */
	private String queueUri;
	
	/** domain id */
	private String domainid;
	
	private String[] topPriority = new String[3];	
	private List<String> referedPresentations = null;
	private List<String> referedJobs = null;
	private String type;
	private String collectiontype;
	
	public String run() {			
		String eventUri = event.getUri();
		domainid = URIParser.getDomainIdFromUri(eventUri);		
		String presentationUri = getSubUri(eventUri, 6);		
		
		logger.debug("event uri = "+eventUri+" domain = "+domainid+" presentation = "+presentationUri);
		
		/* Top prio are properties of a presentation itself */
		topPriority[0] = "/domain/"+domainid+"/user/[^/]+/presentation/[^/]+$";
		topPriority[1] = "/domain/"+domainid+"/user/[^/]+/collection/[^/]+$";
		topPriority[2] = "/domain/"+domainid+"/user/[^/]+/collection/[^/]+/presentation/*";
		
		int priority = getPriority(eventUri);
		
		//presentation added/updated/deleted
		if (type.equals("presentation")) {
			getRefers(presentationUri);
			putInQueue(eventUri, priority, presentationUri);
		} //collection presentation 
		else if (type.equals("collection") && collectiontype.equals("presentation")) {
			//presentation deleted from collection
			if (event.getMethod().equals("DELETE")) {
				//get refers to this collectionpresentation
				List<String> collectionRefers = FSXMLRequestHandler.instance().getReferParents(getSubUri(eventUri, 8));
				Iterator<String> referIterator = collectionRefers.iterator();
				String presentation = "";
					
				referedPresentations = new ArrayList<String>(collectionRefers.size());
				referedJobs = new ArrayList<String>(0);
					
				while (presentation.equals("") && referIterator.hasNext()) {
					String refer = referIterator.next();
					String[] parts = refer.substring(1).split("/");
					
					if (parts[4].equals("index")) {
						String indexPresentation = getSubUri(refer,8)+"/presentation/1";
						Document indexPresentationContent = FSXMLRequestHandler.instance().getNodeProperties(indexPresentation,false);
						
						 presentation = indexPresentationContent.selectSingleNode("//presentation/@referid") == null ? null : indexPresentationContent.selectSingleNode("//presentation/@referid").getText();
						 logger.debug("presentation removed from collection = "+presentation);
					}
				}
				
				if (!presentation.equals("")) {
					List<String> jobRefers = FSXMLRequestHandler.instance().getReferParents(presentation);
				
					boolean jobExists = false;
					
					 for (int i = 0; i < jobRefers.size(); i++) {
						String jobRefer = jobRefers.get(i);
						if (jobRefer != null) {
							logger.debug("job refer = "+jobRefer);
							domainid = URIParser.getDomainIdFromUri(eventUri);
						
							String[] parts = jobRefer.substring(1).split("/");
							if (parts[1].equals(domainid) && parts[5].equals("presentationindex") && parts[6].equals("job")) {
								String job = getSubUri(jobRefer, 8);
								String jobContent = null;
									
								logger.debug("job uri = "+job);
									
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
					if (!jobExists && !presentation.equals("")) {
						String d = URIParser.getDomainIdFromUri(presentation);
						queueUri = QUEUE_URI.replace("{domainid}", d);
						addJob(presentation, eventUri);
					}				
				}			
			} else {
				//presentation added/updated to collection
				String collectionPresentationUri = getSubUri(eventUri, 8);
				Document refercontent = FSXMLRequestHandler.instance().getNodeProperties(collectionPresentationUri, false);
				//logger.debug("refercontent = "+refercontent.asXML());
				String refer = refercontent.selectSingleNode("//presentation/@referid") == null ? "null" : refercontent.selectSingleNode("//presentation/@referid").getText();
				
				//get only jobs referring to this presentation (refer), but discard other presentation refers
				if (refer != "null") {
					getRefers(refer);
					referedPresentations = new ArrayList<String>(1);
					referedPresentations.add(collectionPresentationUri);
					putInQueue(eventUri, priority, refer);
				}
			}
		} //collection properties added/updated/deleted, all presentations will be updated 
		else if (type.equals("collection")) {
			String collectionUri = presentationUri;
			getPresentations(collectionUri, priority);
		} //video screenshots 
		else if(type.equals("video")) {
			//get all refer that are presentation to this video
			List<String> videoRefers = FSXMLRequestHandler.instance().getReferParents(presentationUri);
			
			for (Iterator<String> referIterator = videoRefers.iterator(); referIterator.hasNext(); ) {
				String refer = referIterator.next();
				
				String[] parts = refer.substring(1).split("/");
				if (parts[4].equals("presentation") && parts[6].equals("videoplaylist")) {
					getRefers(getSubUri(refer,6));
					putInQueue(getSubUri(refer,6), 1, getSubUri(refer,6));
				}
			}
		} else {
			//exception
		}
		return null;
	}
		
	private void putInQueue(String eventUri, int priority, String presentationUri) {
		logger.debug("putInQueue eventuri = "+eventUri+" priority = "+priority+" presentationuri = "+presentationUri);
		
		/* Loop all collections where presentation is refered and add to queues */
		for (int i = 0; i < referedPresentations.size(); i++) {
			String collectionPresentation = referedPresentations.get(i) == null ? null : referedPresentations.get(i);
			if (collectionPresentation != null) {
				//get domain from collection presentation and add to queue in domain
			}
			
			if (referedPresentations.get(i) != null) {
				domainid = URIParser.getDomainIdFromUri(eventUri);
				queueUri = QUEUE_URI.replace("{domainid}", domainid);
				
				logger.debug("test");
				
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
					addJob(presentationUri, eventUri);
				}
				
				/* check for double entries */
				getRefers(eventUri);
				int jobCounter = 0;
				for (int k = 0; k < referedJobs.size(); k++) {
					String refer = referedJobs.get(k);
					if (refer != null) {
						String[] parts = refer.substring(1).split("/");
						if (parts[1].equals(domainid) && parts[5].equals("presentationindex") && parts[6].equals("job")) {
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
	
	/* Put presentation in queue for reindexing */
	private void addJob(String presentationUri, String eventUri) {
		/* only add valid presentations */
		if (presentationUri.startsWith("/")) {		
			long timestamp = new Date().getTime();
			
			Document xml = DocumentFactory.getInstance().createDocument();
			Element fsxml = xml.addElement("fsxml");
			Element properties = fsxml.addElement("properties");
			properties.addElement("presentation").addText(presentationUri);
			properties.addElement("priority").addText(String.valueOf(getPriority(eventUri)));
			properties.addElement("timestamp").addText(String.valueOf(timestamp));
			Element presentation = fsxml.addElement("presentation");
			presentation.addAttribute("id", "1");
			presentation.addAttribute("referid", presentationUri);
			
			logger.debug(xml.asXML());
			logger.debug(queueUri);
			
			FSXMLRequestHandler.instance().handlePOST(queueUri, xml.asXML());
		}
	}
	
	/* update priority that is already in the queue */
	private void updatePriority(int priority, String id) {
		if (!id.equals("")) {
			FSXMLRequestHandler.instance().handlePUT("/domain/"+domainid+"/service/smithers/queue/presentationindex/job/"+id+"/properties/priority", String.valueOf(priority));
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
		if (type.equals("collection")) {
			collectiontype = parts.length < 7 ? "" : parts[6];
		}
		
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
		
		referedPresentations = new ArrayList<String>(refers.size());
		referedJobs = new ArrayList<String>(refers.size());
		
		for (Iterator<String> iter = refers.iterator(); iter.hasNext(); ) {
			refer = iter.next();
			logger.debug("refer = "+refer);
			String[] parts = refer.substring(1).split("/");			
			
			// get presentations from collection
			if ((parts[4].equals("collection") && parts[6].equals("presentation"))) {
				referedPresentations.add(refer);				
				logger.debug("added refer to presentationlist");
			} else if (parts[5].equals("presentationindex") && parts[6].equals("job")) {
				referedJobs.add(refer);
				logger.debug("added refer to jobslist");
			} else {
				//logger.debug("not added refer to any list "+parts[4]+" "+parts[6]);			
			}
		}
	}
	
	/* Get all presentations from a collection */
	@SuppressWarnings("unchecked")
	private void getPresentations(String eventUri, int priority) {
		logger.debug("get presentations in this collection");
		Document content = FSXMLRequestHandler.instance().getNodePropertiesByType(eventUri+"/presentation", 2, 0, -1);
		logger.debug(content.asXML());
		
		List<Node> presentations = content.selectNodes("//presentation/@referid");
		for (Iterator<Node> i = presentations.iterator(); i.hasNext(); ) {
			String pUri = i.next().getText();
			logger.debug("pUri = "+pUri);
			getRefers(pUri);
			putInQueue(eventUri, priority, pUri);
		}
	}
}
