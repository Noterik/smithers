package com.noterik.bart.fs.action;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.fs.URIParser;

public class MenuIndexAction extends ActionAdapter {
	/** 
	 * logger
	 */
	private static Logger logger = Logger.getLogger(MenuIndexAction.class);
	/**
	 * index uri
	 */
	private static final String INDEX_URI = "/domain/{domainid}/user/{user}/index/menu";
	/**
	 * queue uri
	 */
	private static final String QUEUE_URI = "/domain/{domainid}/service/smithers/queue/menuindex/job";
	/**
	 * domain id
	 */
	private String domainid;
	/**
	 * filled index uri
	 */
	private String indexUri;
	/**
	 * user
	 */
	private String user;
	/**
	 * Keep track of indexes already created
	 */
	private String[] indexesCreated;
	/**
	 * 
	 */
	private ArrayList<String> totalKeywords, totalLocations, totalDates, totalWitnesses, totalPersons, totalSpeakers;	
	
	private String collectionstatus;
	
	public String run() {
		new Thread() {
			@Override
			public void run() {
				String eventUri = event.getUri();
				domainid = URIParser.getDomainFromUri(eventUri);
				handleQueueItems();
			}
		}.start();
		return null;
	}
	
	/* Handle all jobs from the queue */
	private void handleQueueItems() {
		String queueUri = QUEUE_URI.replace("{domainid}", domainid);
		Document queue = FSXMLRequestHandler.instance().getNodePropertiesByType(queueUri);

		try {
			List<Node> jobs = queue.selectNodes("//job");
			indexesCreated = new String[jobs.size()];
			int i = 0;
			
			logger.debug("start to loop "+jobs.size());
			/* Loop over all jobs */
			for(Iterator<Node> iter = jobs.iterator(); iter.hasNext(); ) {
				Element job = (Element) iter.next();
				String uri = job.element("properties").elementText("presentation");
				
				user = URIParser.getUserFromUri(uri);
				indexUri = INDEX_URI.replace("{domainid}", domainid).replace("{user}", user);
				
				uri = getSubUri(uri, 5);
				
				if (!indexJustCreated(uri)) {
					logger.debug("create user index");
					indexesCreated[i] = uri;
					i++;
					logger.debug("added uri to not be updated after indexing");
					rebuildMenuIndex(uri);
				}
			}
			indexesCreated = null;
		} catch (Exception e) { }
		
	}
	
	private void rebuildMenuIndex(String uri) {		
		totalKeywords = new ArrayList<String>();
		totalLocations = new ArrayList<String>();
		totalDates = new ArrayList<String>();
		totalWitnesses = new ArrayList<String>();
		totalPersons = new ArrayList<String>();
		totalSpeakers = new ArrayList<String>();
		
		/* Delete current index */
		FSXMLRequestHandler.instance().deleteNodeProperties(indexUri, true);
		
		String collectionUri = getSubUri(uri, 4)+"/collection";
		
		/* Determine total number of collections */
		Document collections = FSXMLRequestHandler.instance().getNodePropertiesByType(collectionUri, 0, 0, 1);
		Node resultsAvailable = collections.selectSingleNode("//properties/totalResultsAvailable");
		int numCollections = Integer.parseInt(resultsAvailable.getText());
		
		logger.debug("num collections = "+numCollections);
		FSXMLRequestHandler.instance().handlePUT(indexUri+"/properties", "<fsxml><properties><lastupdate>"+String.valueOf(new Date().getTime())+"</lastupdate></properties></fsxml>");
		
		/* Loop over all collections */
		for (int i = 0; i < numCollections; i++) {
			logger.debug("collection "+i);
			Document collection = FSXMLRequestHandler.instance().getNodePropertiesByType(collectionUri, 10, i, 1);
			
			String cId = collection.selectSingleNode("//collection/@id").getText();
			List<Node> presentations = collection.selectNodes("//presentation");
			
			if (domainid.equals("webtv")) {
				Document coll = FSXMLRequestHandler.instance().getNodeProperties(collectionUri+"/"+cId, 1, false);
				collectionstatus = coll.selectSingleNode("//properties/publicationstatus") == null ? "" : coll.selectSingleNode("//properties/publicationstatus").getText();
				logger.debug("collectionstatus = "+collectionstatus);
			}
			
			/* loop over all presentations from collection */
			for(Iterator<Node> iter = presentations.iterator(); iter.hasNext(); ) {
				Element pres = (Element) iter.next();
				String collectionPresentationUri = collectionUri+"/"+cId+"/presentation/"+pres.attributeValue("id");
				String presentationUri = pres.attributeValue("referid");
				logger.debug("presentation uri = "+presentationUri);
				
				/* since getnodeproperties is not possible on an id node do a get and convert that to a document */
				//Document presentation = FSXMLRequestHandler.instance().getNodePropertiesByType(presentationUri, 10, 0, 1);
				String pr = null;
				try {
					//FSXMLRequestHandler.instance().getNodeProperties(presentationUri, false);
					pr = FSXMLRequestHandler.instance().handleGET(presentationUri, null).getText();
				} catch (Exception e) {	}
				
				Document presentation = null;
				try {
					presentation = DocumentHelper.parseText(pr);
				} catch (Exception e) { }
				
				String pId = presentation.selectSingleNode("//presentation/@id") == null ? "" : presentation.selectSingleNode("//presentation/@id").getText();
				
				/*JHM hack */
				if (domainid.equals("jhm")) {
					String lockmode = presentation.selectSingleNode("//presentation/properties/lockmode") == null ? "" : presentation.selectSingleNode("//presentation/properties/lockmode").getText();
					if (lockmode.equals("Finished / Approved")) {
						presentationResults(presentation);
					}
				} else if (!domainid.equals("lhwebtv") || pId.indexOf("p") > -1 || Integer.parseInt(pId) > 240) {
					presentationResults(presentation);	
				}
			}		
		}
		//add jhm keywords from config
		if (domainid.equals("jhm")) {
			Document document = DocumentHelper.createDocument();
		    Element fsxml = document.addElement("fsxml");		
			
		    Document db = FSXMLRequestHandler.instance().getNodePropertiesByType("/domain/jhm/config/presentation/filesystem/1/layer/4/database/1/keyword", 10, 0, 1000);
		    
		    logger.debug(db.asXML());
		    
		    List<Node> keywords = db.selectNodes("//keyword/properties/name");
		    
		    logger.debug("nr of keywords = "+keywords.size());
		    
		    /* loop over all keywords */
			for(Iterator<Node> iter = keywords.iterator(); iter.hasNext(); ) {		    
				Element e = (Element) iter.next();
				
				String keyword = e.getText();
				
				logger.debug("add "+keyword);
				
			    Element menuitem = fsxml.addElement("item");
				Element properties = menuitem.addElement("properties");
				properties.addElement("keyword").addText(keyword);	
				
				/* Unique chapter id */
				long timestamp = new Date().getTime();
				menuitem.addAttribute("id", String.valueOf(timestamp));
				try {
					Thread.sleep(1);
				} catch (InterruptedException exc) {
					logger.error("",exc);
				}
			}	   
			logger.debug("going to save "+document.asXML());
			FSXMLRequestHandler.instance().saveFsXml(indexUri, document.asXML(), "PUT", true);
		}
	}
	
	private void presentationResults(Document presentation) {
		List<Node> keywords = presentation.selectNodes("//keyword");
		if (!domainid.equals("jhm")) {
			putType(keywords, "keyword", new String[] {"name"});
		}
			
		List<Node> locations = presentation.selectNodes("//location");
		putType(locations, "location", new String[] {"name"});
		
		List<Node> dates = presentation.selectNodes("//date");
		putType(dates, "date", new String[] {"start", "end"});
		
		List<Node> persons = presentation.selectNodes("//person");
		putType(persons, "person", new String[] {"name"});
		
		List<Node> witness = presentation.selectNodes("//presentation");
		logger.debug("put witnesses");
		logger.debug("number of witnesses "+witness.size());
		putType(witness, "witness", new String[] {"title"});
		
		List<Node> speakers = presentation.selectNodes("//speakers");
		putType(speakers, "speakers", new String[] {"firstname"});
	}
	
	private void putType(List<Node> type, String t, String[] aNames) {
		Element e;
		boolean empty = true;
		Document document = DocumentHelper.createDocument();
	    Element fsxml = document.addElement("fsxml");		
	    
		/* Loop all elements */
		for (Iterator<Node> iter = type.iterator(); iter.hasNext(); ) {
			logger.debug("looping elements of type "+t);
			e = (Element) iter.next();

			for (int i = 0; i < aNames.length; i++) {
				String name = e.element("properties").elementText(aNames[i]) == null ? "" : e.element("properties").elementText(aNames[i]);	
				
				//logger.debug("got "+t+" with value "+name);
				
				if (!name.equalsIgnoreCase("datatype") && !name.equals("") && !elementExists(t, name)) {
					empty = false;	
					if (t.equals("keyword")) {
						totalKeywords.add(name.toLowerCase());
					} else if (t.equals("location")) {
						totalLocations.add(name.toLowerCase());
					} else if (t.equals("date")) {
						totalDates.add(name.toLowerCase());
					} else if (t.equals("person")) {
						totalPersons.add(name.toLowerCase());
					} else if (t.equals("witness")) {
						//remove number in front of title for jhm
						if (domainid.equals("jhm") && name.length() > 6) {
							name = name.substring(6);
						}
						totalWitnesses.add(name.toLowerCase());
					} else if (t.equals("speakers")) {
						name += e.element("properties").elementText("lastname") == null ? "" : " "+e.element("properties").elementText("lastname");
						//name += e.element("properties").elementText("organization") == null ? "" : e.element("properties").elementText("organization");
					}
					
					logger.debug("element exists? "+ elementExists(t,name));
					
					if (!t.equals("speakers") || !domainid.equals("webtv") || (collectionstatus.equals("published") && !elementExists(t, name))) {
						if (t.equals("speakers") && domainid.equals("webtv")) {
							totalSpeakers.add(name.toLowerCase());
						}
						
						Element menuitem = fsxml.addElement("item");
						Element properties = menuitem.addElement("properties");
						properties.addElement(t).addText(name);	

						/* Unique chapter id */
						long timestamp = new Date().getTime();
						menuitem.addAttribute("id", String.valueOf(timestamp));
						try {
							Thread.sleep(1);
						} catch (InterruptedException exc) {
							logger.error("",exc);
						}
					}
				}
			}
		}
		/* Add elements to index */
		if (!empty) {			
			FSXMLRequestHandler.instance().saveFsXml(indexUri, document.asXML(), "PUT", true);
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
		return subUri.substring(0, subUri.length()-1);
	}
	
	/* Check if index for user was created during current update process */
	private boolean indexJustCreated(String uri) {
		uri = getSubUri(uri,5);
		logger.debug("check for created uri = "+ uri);
		for (int i = 0; i < indexesCreated.length; i++) {
			if (indexesCreated[i] != null && indexesCreated[i].equalsIgnoreCase(uri)) {
				logger.debug("was created, skip it");
				return true;
			}
		}
		return false;
	}
	
	/* We don't want duplicate elements */
	private boolean elementExists(String element, String data) {
		if (element.equals("keyword")) {
			return totalKeywords.contains(data.toLowerCase());
		} else if (element.equals("location")) {
			return totalLocations.contains(data.toLowerCase());
		} else if (element.equals("date")) {
			return totalDates.contains(data.toLowerCase());
		} else if (element.equals("person")) {
			return totalPersons.contains(data.toLowerCase());
		} else if (element.equals("witness")) {
			return totalWitnesses.contains(data.toLowerCase());
		} else if (element.equals("speakers")) {
			return totalSpeakers.contains(data.toLowerCase());
		}
		return true;
	}
}
