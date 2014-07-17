/* 
* PresentationIndexAction.java
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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
 
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.fs.URIParser;

public class PresentationIndexAction extends ActionAdapter {
	
	private static final long delay = 300000L;			/* (ms) delay for low priority queue items */			
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = Logger.getLogger(PresentationIndexAction.class);
	private static final String QUEUE_URI = "/domain/{domainid}/service/smithers/queue/presentationindex/job";
	private static final String CONFIG_URI = "/domain/{domainid}/user/{user}/config/index";
	private static final String CONFIG2_URI = "/domain/{domainid}/user/{user}/config/index2";
	private static final String DEFAULT_INDEX_TYPE = "chapter";
	private static final String DEFAULT_INDEX_URI = "/domain/{domainid}/user/{user}/index/chaptersearch";
	private static final DateFormat SORTABLE_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
	private static final Map<String, WorkQueue> workQueueMap = new HashMap<String, WorkQueue>();
	
	/* 
	 * Run in own thread, but make sure only 1 instance is running at the time, per domain 
	 */
	public String run() {
		final String eventUri = event.getUri();
		Runnable r = new Runnable() {
			public void run() {
				LOG.debug("Starting PresentationIndexAction run");
				
				// create separate configuration object
				String domain = URIParser.getDomainIdFromUri(eventUri);
				String queueUri = QUEUE_URI.replace("{domainid}", domain);
				Config config = new Config();
				config.setDomain(domain);
				config.setQueueUri(queueUri);
				
				// start
				handleQueue(config);
				
				LOG.debug("Finished PresentationIndexAction run");
			}
		};
		WorkQueue wQueue = null;
		synchronized (workQueueMap) {
			String domain = URIParser.getDomainIdFromUri(eventUri);
			wQueue = workQueueMap.get(domain);
			if(wQueue == null) {
				wQueue = new WorkQueue(1);
				workQueueMap.put(domain, wQueue);
			}
		}
		wQueue.execute(r);
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private static void handleQueue(Config config) {
		Document queue = FSXMLRequestHandler.instance().getNodePropertiesByType(config.getQueueUri());
		List<Node> highPriorityJobs = queue.selectNodes("//job/properties[priority=1]");
		List<Node> lowPriorityJobs = queue.selectNodes("//job/properties[priority!=1]");
		
		LOG.debug("Number of high priority jobs: "+highPriorityJobs.size());
		LOG.debug("Number of low priority jobs: "+lowPriorityJobs.size());
		
		handleHighPriorityJobs(highPriorityJobs, config);
		handleLowPriorityJobs(lowPriorityJobs, config);
	}
	
	private static void handleHighPriorityJobs(List<Node> highPriorityJobs, Config config) {
		for (Iterator<Node> i = highPriorityJobs.iterator(); i.hasNext(); ) {
			Element jElem = (Element) i.next();
			IndexJob job = loadIndexJob(jElem);
			handleJob(job, config);
		}
	}
	
	private static void handleLowPriorityJobs(List<Node> lowPriorityJobs, Config config) {
		long currentTime = new Date().getTime();
		
		for (Iterator<Node> i = lowPriorityJobs.iterator(); i.hasNext(); ) {
			Element jElem = (Element) i.next();
			IndexJob job = loadIndexJob(jElem);
			long timestamp = job.getTimestamp();
			
			// low priority job
			if ((timestamp+delay) < currentTime) {
				handleJob(job, config);
			}
		}
	}
	
	/**
	 * Handle a single job
	 * 
	 * @param job
	 * @param config
	 */
	private static void handleJob(IndexJob job, Config config) {
		LOG.debug("Handling job: "+job+", with config: "+config);
		if (job.getPresentation() != null) {
			String jobId = job.getId();
			String presentationUri = job.getPresentation();
			String domain = config.getDomain();
			String user = URIParser.getUserIdFromUri(presentationUri); 
			
			//check if we got a valid job
			if (user == null || domain == null || user.equals("") || domain.equals("")) {
				return;
			}
			
			// load index configuration
			String configUri = CONFIG_URI.replace("{domainid}", domain).replace("{user}", user);
			IndexConfig iConfig = loadIndexConfig(configUri);
			
			// create or update index
			/* 
			 * TODO: don't use addIndexCreated or isIndexCreated, because it will screw up in 
			 * index jobs running on multiple machines
			 */
			LOG.debug("index config: "+ iConfig);
			if (!FSXMLRequestHandler.instance().hasChildren(iConfig.getIndexUri(), iConfig.getIndexType())) {
				createPresentationIndex(presentationUri, iConfig);
				config.addIndexCreated(getSubUri(presentationUri, 5));
			} else  if(!config.isIndexCreated(getSubUri(iConfig.getIndexUri(),5))) {
				updatePresentationIndex(presentationUri, iConfig);
			}
			
			// check for second index
			String configUri2 = CONFIG2_URI.replace("{domainid}", domain).replace("{user}", user);
			IndexConfig iConfig2 = loadIndexConfig(configUri2);
			if (!iConfig2.isDefaultQueue()) {  // don't do a default one				
				// create or update index
				if (!FSXMLRequestHandler.instance().hasChildren(iConfig.getIndexUri(), iConfig.getIndexType())) {
					createPresentationIndex(presentationUri, iConfig2);
					config.addIndexCreated(getSubUri(presentationUri, 5));
				} else  if(!config.isIndexCreated(getSubUri(iConfig.getIndexUri(),5))) {
					updatePresentationIndex(presentationUri, iConfig2);
				}			
			}	
			
			// remove job from filesystem
			FSXMLRequestHandler.instance().deleteNodeProperties(config.getQueueUri()+"/"+jobId,true);
		}
	}

	/**
	 * Update the presentation index
	 * 
	 * @param presentationUri
	 * @param iConfig
	 */
	private static void updatePresentationIndex(String presentationUri, IndexConfig iConfig) {
		LOG.debug("updatePresentationIndex: "+presentationUri+", index config: "+iConfig);
		
		// get presentation and collection details
		Map<String, Object> presentationDetails = getPresentationDetails(presentationUri, iConfig);
		Map<String, Object> collectionDetails = getCollectionDetails(presentationUri, iConfig);
		
		// remove indexes of specific type from the presentation
		deleteTypes(presentationUri, iConfig);
		
		// 
		if (presentationDetails != null && collectionDetails != null) {		
			loopTypes(presentationUri, presentationDetails, collectionDetails, iConfig);
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void createPresentationIndex(String uri, IndexConfig iConfig) {	
		LOG.debug("createPresentationIndex: "+uri+", index config: "+iConfig);
		
		createIndex(iConfig);
			
		String collectionUri = getSubUri(uri, 4)+"/collection";		
		int numCollections = getNumberOfCollections(collectionUri);
		
		/* Loop over all collections */
		for (int i = 0; i < numCollections; i++) {
			Document collection = FSXMLRequestHandler.instance().getNodePropertiesByType(collectionUri, 10, i, 1);
			
			Map<String, Object> collectionDetails = getCollectionDetails(collection, collectionUri, iConfig);			
			List<Node> presentations = collection.selectNodes("//presentation");
			
			/* loop over all presentations from a collection */
			for(Iterator<Node> iter = presentations.iterator(); iter.hasNext(); ) {
				Element presentation = (Element) iter.next();				
				collectionDetails.put("presentation", (String) collectionDetails.get("collection")+"presentation/"+presentation.attributeValue("id")+"/");
				String presentationUri = presentation.attributeValue("referid");
				
				if (presentationUri != null && presentationUri.startsWith("/domain/")) {
					Map<String, Object> presentationDetails = getPresentationDetails(presentationUri, iConfig);
					if (presentationDetails != null) {
						loopTypes(presentationUri, presentationDetails, collectionDetails, iConfig);
					}
				}
			}			
		}
	}
	
	/**
	 * Create XML for an index
	 * 
	 * @param presentationUri
	 * @param presentationDetails
	 * @param collectionDetails
	 * @param iConfig
	 */
	private static void makeTypeFromPresentation(String presentationUri, Map<String, Object> presentationDetails, Map<String, Object> collectionDetails, IndexConfig iConfig) {
		Document typeDocument = DocumentHelper.createDocument();
		Element fsxml = typeDocument.addElement("fsxml");
		Element type = fsxml.addElement(iConfig.getIndexType());
		
		// add refer to original presentation
		Element referPresentation = type.addElement("presentation");
		referPresentation.addAttribute("id", "1");
		referPresentation.addAttribute("referid", presentationUri);
		
		//add refer to original collection, remove trailing slash, otherwise refer cannot be found!
		Element referCollectionPresentation = type.addElement("collectionpresentation");
		referCollectionPresentation.addAttribute("id", "1");		
		String collectionPresentation = (String) collectionDetails.get("presentation");
		collectionPresentation = collectionPresentation.substring(0, collectionPresentation.length()-1);
		referCollectionPresentation.addAttribute("referid", collectionPresentation);
		
		Element properties = type.addElement("properties");
		//add standard properties
		properties.addElement("presentation").addText((String) collectionDetails.get("presentation"));
		properties.addElement("presentationuri").addText(presentationUri+"/");
		properties.addElement("collection").addText((String) collectionDetails.get("collection"));
		properties.addElement("presentationtitle").addText((String) presentationDetails.get("presentationtitle"));
		properties.addElement("presentationdescription").addText((String) presentationDetails.get("presentationdescription"));
		properties.addElement("presentationscreenshot").addText((String) presentationDetails.get("presentationscreenshot"));
		properties.addElement("presentationtype").addText((String) presentationDetails.get("presentationtype"));
		properties.addElement("presentationauthor").addText((String) presentationDetails.get("presentationauthor"));
		properties.addElement("presentationcopyright").addText((String) presentationDetails.get("presentationcopyright"));
		properties.addElement("presentationwebsite").addText((String) presentationDetails.get("presentationwebsite"));
		
		//add user configured properties
		Map<String, String> items = iConfig.getProperties();
		for (String item : items.keySet()) {
			if (item.equals("collectiontitle") || item.equals("collectiondescription") || item.equals("collectionstatus")) {
				properties.addElement(item).addText((String) collectionDetails.get(item));
			} else if (item.equals("lockmode") || item.equals("date_created") || item.equals("presentationduration") || item.equals("presentationtheme") || item.equals("presentationlocation") || item.equals("presentationdate") || item.equals("presentationdate_original") || item.equals("presentationpublic") || item.equals("sponsor")) {
				properties.addElement(item).addText((String) presentationDetails.get(item));
			} else if (item.equals("title") || item.equals("name")) {
				properties.addElement(item).addText((String) presentationDetails.get("presentationtitle"));
			} else if (item.equals("description")) {
				properties.addElement(item).addText((String) presentationDetails.get("presentationdescription"));
			} else if (item.equals("screenshot")) {
				properties.addElement(item).addText((String) presentationDetails.get("presentationscreenshot"));
			} else if (item.equals("rank")) {
				properties.addElement(item).addText("5");
			} else if (item.equals("peercomments")) {
				properties.addElement(item).addText(getType((Document) presentationDetails.get("presentation"), "peercomments", 0.0, 86400000.0, new String[] {"comment"}));
			} else if (item.equals("bookmark")) {
				properties.addElement(item).addText(getType((Document) presentationDetails.get("presentation"), "bookmark", 0.0, 86400000.0, new String[] {"title", "description", "creator"}));
			} else if (item.equals("webtv_item_id") || item.equals("presentationlivestate")) {
				properties.addElement(item).addText((String) presentationDetails.get(item));
			}
		}

		long timestamp = new Date().getTime();
		type.addAttribute("id", String.valueOf(timestamp));
		
		// Add directly to fs so maggie get's updated with first content faster
		FSXMLRequestHandler.instance().saveFsXml(iConfig.getIndexUri(), typeDocument.asXML(), "PUT", true);
	}
	
	/**
	 * Create XML for an index
	 * 
	 * @param typeContent
	 * @param presentationUri
	 * @param presentationDetails
	 * @param collectionDetails
	 * @param iConfig
	 */
	private static void makeType(Element typeContent, String presentationUri, Map<String, Object> presentationDetails, Map<String, Object> collectionDetails, IndexConfig iConfig) {
		// create new type
	    Document typeDocument = DocumentHelper.createDocument();
	    Element fsxml = typeDocument.addElement("fsxml");
		Element type = fsxml.addElement(iConfig.getIndexType());
		
		 // add refer to original presentation
		Element referPresentation = type.addElement("presentation");
		referPresentation.addAttribute("id", "1");
		referPresentation.addAttribute("referid", presentationUri);
		
		// add refer to original collection
 		Element referCollectionPresentation = type.addElement("collectionpresentation");
		referCollectionPresentation.addAttribute("id", "1");
		String collectionPresentation = (String) collectionDetails.get("presentation");
		collectionPresentation = collectionPresentation.substring(0, collectionPresentation.length()-1);
		referCollectionPresentation.addAttribute("referid", (String) collectionPresentation);
		
		 Element properties = type.addElement("properties");
		//add standard properties
		properties.addElement("presentation").addText((String) collectionDetails.get("presentation"));
		properties.addElement("presentationuri").addText(presentationUri+"/");
		properties.addElement("collection").addText((String) collectionDetails.get("collection"));
		properties.addElement("presentationtitle").addText((String) presentationDetails.get("presentationtitle"));
		properties.addElement("presentationdescription").addText((String) presentationDetails.get("presentationdescription"));
		properties.addElement("presentationscreenshot").addText((String) presentationDetails.get("presentationscreenshot"));
		properties.addElement("presentationtype").addText((String) presentationDetails.get("presentationtype"));
		properties.addElement("presentationauthor").addText((String) presentationDetails.get("presentationauthor"));
		properties.addElement("presentationcopyright").addText((String) presentationDetails.get("presentationcopyright"));
		properties.addElement("presentationwebsite").addText((String) presentationDetails.get("presentationwebsite"));
		 
		//add user configured properties
		Map<String, String> items = iConfig.getProperties();
		 
		Element typeProperties = typeContent.element("properties");
		
		double start = typeProperties.elementText("starttime") == null ? 0.0 : Double.parseDouble(typeProperties.elementText("starttime"));
		double duration = typeProperties.elementText("duration") == null ? 0.0 : Double.parseDouble(typeProperties.elementText("duration"));
		if (duration == 0.0) {
			duration = typeProperties.elementText("length") == null ? 0.0 : Double.parseDouble(typeProperties.elementText("length"));
		}
		
		for (String item : items.keySet()) {
			if (item.equals("collectiontitle") || item.equals("collectiondescription") || item.equals("collectionstatus")) {
				properties.addElement(item).addText((String) collectionDetails.get(item));
			} else if (item.equals("lockmode") || item.equals("link") || item.equals("date_created") || item.equals("presentationduration") || item.equals("presentationtheme") || item.equals("presentationlocation") || item.equals("presentationdate") || item.equals("presentationdate_original") || item.equals("presentationpublic") || item.equals("sponsor")) {
				properties.addElement(item).addText((String) presentationDetails.get(item));
			} else if (item.equals("title") || item.equals("description") || item.equals("name")) {
				String value = typeProperties.elementText(item) == null ? "" : typeProperties.elementText(item);
				properties.addElement(item).addText(value);
			} else if (item.equals("firstnamelastname")) {
				String firstname = typeProperties.elementText("firstname") == null ? "" : typeProperties.elementText("firstname");
				String lastname = typeProperties.elementText("lastname") == null ? "" : typeProperties.elementText("lastname");
				properties.addElement("name").addText(firstname+" "+lastname);
			} else if (item.equals("screenshot")) {
				//former chapterscreenshot
				properties.addElement(item).addText(getTypeScreenshot((Document) presentationDetails.get("presentation"), start, (String) presentationDetails.get("presentationscreenshot")));
			} else if (item.equals("rank")) {
				properties.addElement(item).addText(String.valueOf(getRankBasedOnLockmode((String) presentationDetails.get("lockmode"))));
			} else if (item.equals("start")) {
				properties.addElement(item).addText(String.format(Locale.US, "%f", start));
			} else if (item.equals("duration")) {
				properties.addElement(item).addText(String.format(Locale.US, "%f", duration));
			} else if (item.equals("locations")) {
				properties.addElement(item).addText(getType((Document) presentationDetails.get("presentation"), "location", start, duration, new String[] {"name"}));
			} else if (item.equals("dates")) {
				properties.addElement(item).addText(getType((Document) presentationDetails.get("presentation"), "date", start, duration, new String[] {"start", "end"}));
			} else if (item.equals("keywords")) {
				properties.addElement(item).addText(getType((Document) presentationDetails.get("presentation"), "keyword", start, duration, new String[] {"name"}));
			} else if (item.equals("persons")) {
				properties.addElement(item).addText(getType((Document) presentationDetails.get("presentation"), "person", start, duration, new String[] {"name"}));
			} else if (item.equals("periods")) {
				properties.addElement(item).addText(getType((Document) presentationDetails.get("presentation"), "period", start, duration, new String[] {"name"}));
			} else if (item.equals("speakers")) { 
				properties.addElement(item).addText(getType((Document) presentationDetails.get("presentation"), "speakers", start, duration, new String[] {"firstname", "lastname", "organization"}));
			} else if (item.equals("topics")) {
				properties.addElement(item).addText(getType((Document) presentationDetails.get("presentation"), "topics", start, duration, new String[] {"name"}));
			} else if (item.equals("peercomments")) {
				properties.addElement(item).addText(getType((Document) presentationDetails.get("presentation"), "peercomments", 0.0, 86400000.0, new String[] {"comment"}));
			} else if (item.equals("voiceindex")) {
				String voiceindex = typeProperties.elementText("voiceindex") == null ? "" : typeProperties.elementText("voiceindex");
				properties.addElement(item).addText(voiceindex);
			} else if (item.equals("presentation"+iConfig.getIndexType())) {
				properties.addElement(item).addText(getTypeUri(typeContent, presentationUri, iConfig));
			} else if (item.equals("bookmark")) {
				properties.addElement(item).addText(getType((Document) presentationDetails.get("presentation"), "bookmark", 0.0, 86400000.0, new String[] {"title", "description", "creator"}));
			} else if (item.equals("webtv_item_id") || item.equals("presentationlivestate")) {
				properties.addElement(item).addText((String) presentationDetails.get(item));
			}
		}
		long timestamp = new Date().getTime();
		type.addAttribute("id", String.valueOf(timestamp));
		
		// Add directly to fs so maggie get's updated with first content faster
		FSXMLRequestHandler.instance().saveFsXml(iConfig.getIndexUri(), typeDocument.asXML(), "PUT", true);
	}
	
	// Delete all index types from the current presentation
	private static void deleteTypes(String presentationUri, IndexConfig iConfig) {
		LOG.debug("deleting "+ iConfig.getIndexType() +" with presentationUri "+presentationUri);
		// get index types refering to presentation
		List<String> refers = FSXMLRequestHandler.instance().getReferParents(presentationUri);

		// Delete all refers that are of type index type in the current index & same domain
		for (Iterator<String> iter = refers.iterator(); iter.hasNext(); ) {
			String refer = iter.next();

			if(refer.startsWith("/")) {
				refer = refer.substring(1);
			}
			String[] parts = refer.split("/");
			
			// only remove if the refer is an index of given type
			if (parts[4].equals("index") && parts[6].equals(iConfig.getIndexType())) {
				String cid = parts[7];
				LOG.debug("delete index type "+cid);
				FSXMLRequestHandler.instance().deleteNodeProperties(iConfig.getIndexUri()+"/"+iConfig.getIndexType()+"/"+cid, true);
			}
		}
	}

	// Get a sub part of the uri
	private static String getSubUri(String uri, int length) {		
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
	
	// Get screenshot for the presentation from first videoplaylist that contains a video with screenshots
	@SuppressWarnings("unchecked")
	private static String getPresentationScreenshot(Document presentation, String presentationUri) {
		int seconds = -1;
		String domain = URIParser.getDomainIdFromUri(presentationUri);
		//check if screenshottime property of the presentation is set
		Double screenshottime = presentation.selectSingleNode("//properties/screenshottime") == null ? -1.0 : Double.parseDouble(presentation.selectSingleNode("//properties/screenshottime").getText());

		if (screenshottime != -1.0) {
			seconds = screenshottime.intValue();
		}
		
		//check for screens block in the videoplaylist
		Element screenshotBlock;
		List<Node> screenshotBlocks = presentation.selectNodes("//videoplaylist[@id='1']/screenshot");
	
		for (Iterator<Node> ite = screenshotBlocks.iterator(); ite.hasNext();) {
			screenshotBlock = (Element) ite.next();
			String stype = screenshotBlock.selectSingleNode("//properties/stype") == null ? "" : screenshotBlock.selectSingleNode("//properties/stype").getText();
			if (stype.equals("video")) {
				Double starttime = screenshotBlock.selectSingleNode("//properties/starttime") == null ? -1.0 : Double.parseDouble(screenshotBlock.selectSingleNode("//properties/starttime").getText());
				seconds = (int) Math.round(starttime/1000)+1;
				break;
			}
		}
		
		Element video;		
		List<Node> videos = presentation.selectNodes("//video");
		
		// loop videos
		for(Iterator<Node> iter = videos.iterator(); iter.hasNext(); ) {
			video = (Element) iter.next();
			String refer = video.attributeValue("referid");
			
			// get starttime from first video, if not available take duration / 2
			if (video.selectSingleNode("//video/properties/starttime") == null) {
				Document rawVideo = FSXMLRequestHandler.instance().getNodeProperties(refer+"/rawvideo/1", false);
				if (rawVideo == null || rawVideo.selectSingleNode("//properties/duration") == null) {
					if (rawVideo != null && rawVideo.selectSingleNode("//properties/filename") != null) {
						LOG.debug("external video detected");
						seconds = seconds == -1 ? 20 : seconds;
						//set duration
						return getVideoScreenshot(refer)+secondsToImageUri(seconds,false);
					} else {
						rawVideo = FSXMLRequestHandler.instance().getNodeProperties(refer+"/rawvideo/2", false);
					}
				}				
				if (rawVideo != null) {
					//logger.debug(rawVideo.asXML());
					if (rawVideo.selectSingleNode("//properties/filename") != null) {
						//external video's with a duration, not correct for euscreen due to incorrect values in NTUA
						seconds =  seconds == -1 ? 7 : seconds;
						return getVideoScreenshot(refer)+secondsToImageUri(seconds,false);
					} else {
						Double duration = rawVideo.selectSingleNode("//properties/duration") == null ? 10000.0 : Double.parseDouble(rawVideo.selectSingleNode("//properties/duration").getText())*1000;
						//set duration
						seconds = seconds == -1 ? (int) Math.round(duration/2000) : seconds;
						if ((domain.equals("lhwebtv") || domain.equals("jhm")) && seconds < 150 && duration > 150000) {
							seconds = seconds == -1 ? 150 : seconds;
						}				
						return getVideoScreenshot(refer)+secondsToImageUri(seconds,false);
					}
				}
				LOG.debug("no rawvideo found "+video.asXML());
				return presentation.selectSingleNode("//properties/backupthumbnail") == null ? "" : presentation.selectSingleNode("//properties/backupthumbnail").getText();
			} else {
				Double starttime = video.selectSingleNode("//video/properties/starttime").getText().equalsIgnoreCase("") ? 0.0 : Double.parseDouble(video.selectSingleNode("//video/properties/starttime").getText());
				seconds = seconds == -1 ? (int) Math.round(starttime/1000) : ((int) Math.round(starttime/1000)) + seconds;
				if ((domain.equals("lhwebtv") || domain.equals("jhm")) && seconds < 150) {
					//Check if duration is longer then 150
					Document rawVideo = FSXMLRequestHandler.instance().getNodeProperties(refer+"/rawvideo/1", false);
					if (rawVideo == null) {
						rawVideo = FSXMLRequestHandler.instance().getNodeProperties(refer+"/rawvideo/2", false);
					}				
					Double duration = rawVideo.selectSingleNode("//properties/duration") == null ? 10.0 : Double.parseDouble(rawVideo.selectSingleNode("//properties/duration").getText())*1000;
					
					if (duration > 150000) {					
						seconds = seconds == -1 ? 150 : seconds;
					}
				}
				LOG.debug("starttime presentationscreenshot = "+seconds);
				return getVideoScreenshot(refer)+secondsToImageUri(seconds,false);
			}
		}
		LOG.debug("no video found in presentation");
		//backup thumbnail only serves as a backup, when the video screenshots are not available
		return presentation.selectSingleNode("//properties/backupthumbnail") == null ? "" : presentation.selectSingleNode("//properties/backupthumbnail").getText();
	}

	// Get screenshot from a video
	@SuppressWarnings("unchecked")
	private static String getVideoScreenshot(String vUri) {
		Document screens = FSXMLRequestHandler.instance().getNodePropertiesByType(vUri+"/screens");
		Element elem;
		String uri = "";
		for(Iterator<Element> i = screens.getRootElement().elementIterator(); i.hasNext(); ) {
			elem = i.next();
			if (elem.getName().equals("screens")) {
				try {
					uri = elem.selectSingleNode("//properties/uri").getText();
					break;
				} catch (Exception e) {/* ignored */}
			}
		}
		return uri;
	}
	
	// Get screenshot for a type
	@SuppressWarnings("unchecked")
	private static String getTypeScreenshot(Document presentation, double typeStart, String presentationScreenshot) {
		Element video;
		double videoDuration = 0.0, videoStart = 0.0, totalDuration = 0.0;
		List<Node> videos = presentation.selectNodes("//video");
		
		//check if special ordering is required
		if (presentation.selectSingleNode("//video/properties/position") != null) {
			videos = orderVideos(videos);
		}
		// Loop all videos
		for(Iterator<Node> iter = videos.iterator(); iter.hasNext(); ) {
			video = (Element) iter.next();
			
			if (video.element("properties").elementText("duration") != null && !video.element("properties").elementText("duration").equals("")) {
				videoDuration = Double.parseDouble(video.element("properties").elementText("duration"));
			}
			if (video.element("properties").elementText("starttime") != null && !video.element("properties").elementText("starttime").equals("")) {
				videoStart = Double.parseDouble(video.element("properties").elementText("starttime"));
			}
				
			// if video does not contain duration it's not editted, take refers duration
			if (videoDuration == 0.0) {
				//get refer rawvideo 2 for duration in seconds
				Document videoNode = FSXMLRequestHandler.instance().getNodeProperties(video.attributeValue("referid"), false);
				if (videoNode != null) {
					videoDuration = videoNode.selectSingleNode("//rawvideo[@id='2']/properties/duration") == null ? 0.0 : Double.parseDouble(videoNode.selectSingleNode("//rawvideo[@id='2']/properties/duration").getText())*1000;
					// no rawvideo 2, so take rawvideo 1, most of the time broadcasts
					if (videoDuration == 0.0) {
						videoDuration = videoNode.selectSingleNode("//rawvideo[@id='1']/properties/duration") == null ? 0.0 : Double.parseDouble(videoNode.selectSingleNode("//rawvideo[@id='1']/properties/duration").getText())*1000;
					}
					LOG.debug("video duration has become "+videoDuration);
				}
			}
			
			// Get correct video
			if (typeStart >= totalDuration && typeStart <= totalDuration+videoDuration) {
				int seconds = (int) ((typeStart-totalDuration+videoStart)/1000);
				return getVideoScreenshot(video.attributeValue("referid"))+secondsToImageUri(seconds,true);
			}
			totalDuration += videoDuration;
		}		
		return presentationScreenshot;
	}
	
	// Get for a specified type all attributes that fit in the time range
	@SuppressWarnings("unchecked")
	private static String getType(Document presentation, String type, double cStart, double cDuration, String[] aNames) {
		Element e;
		double eStart, eDuration;
		List<Node> typeList = presentation.selectNodes("//"+type);
		Set<String> results = new HashSet<String>();
		
		// Loop all elements
		for (Iterator<Node> iter = typeList.iterator(); iter.hasNext(); ) {
			e = (Element) iter.next();
			//prevent hanging on keyword/date/location without properties
			if (e.element("properties") != null) { 
				eStart = e.element("properties").elementText("starttime") == null ? 0.0 : Double.parseDouble(e.element("properties").elementText("starttime").replace(" ", ""));
				eDuration = e.element("properties").elementText("duration") == null ? 0.0 : Double.parseDouble(e.element("properties").elementText("duration").replace(" ", ""));
				
				/* Same interval */
				if ((eStart >= cStart && eStart <= cStart+cDuration) || (eStart+eDuration >= cStart && eStart+eDuration <= cStart+cDuration) || (eStart <= cStart && eStart+eDuration >= cStart+cDuration)) {
					String result = "";
					for (int j = 0; j < aNames.length; j++) {
						String name = e.element("properties").elementText(aNames[j]) == null ? "" : e.element("properties").elementText(aNames[j]);
						if (!name.equals("datatype")) {
							result += name+" ";
						}
					}
					if (!result.equals("")) {
						results.add(result);
					}
				}
			}
		}		
		String temp = "";
		
		for (Iterator<String> it = results.iterator(); it.hasNext(); ) {
			temp += it.next()+" , ";
		}
		LOG.debug("got for type "+type+": "+temp);
		return temp;
	}
	
	// Seconds to img uri format
	private static String secondsToImageUri(int seconds, boolean correct) {
		// add 5 seconds to correct for chapters starting early but only for clips of at least 10 seconds, otherwise we go past the duration!
		if (correct && seconds > 8) {
			seconds += 5;
		}
		
		
		int hours = (int) (seconds/3600);
		int minutes = (int) ((seconds%3600)/60);
		int sec = (int) (seconds%60);		
		return "/h/"+hours+"/m/"+minutes+"/sec"+sec+".jpg";
	}
	
	// Get collection presentation is in
	private static String getCollection(String pUri) {
		List<String> refers = FSXMLRequestHandler.instance().getReferParents(pUri);
		String refer = "";
		int size = refers.size();
		String domain = URIParser.getDomainIdFromUri(pUri);
		String user = URIParser.getUserIdFromUri(pUri);
		
		LOG.debug("found "+size+" refers for "+pUri);
		
		for (Iterator<String> iter = refers.iterator(); iter.hasNext(); ) {
			refer = iter.next();
			LOG.debug("refer = "+refer);
			
			//Get refer from same user and make sure it's a collection
			if(refer.startsWith("/")) {
				refer = refer.substring(1);
			}
			if(refer.endsWith("/")) {
				refer = refer.substring(0,refer.length()-1);
			}			
			String[] parts = refer.split("/");
			
			if (parts[1].equals(domain) && parts[3].equals(user) && parts[4].equals("collection")) {
				return "/"+refer+"/";
			}
		}
		//presentation not referred (anymore) in a collection for this domain & user
		return null;
	}
	
	private static int getRankBasedOnLockmode(String lockmode) {
		if (lockmode.equals("Finished / Approved")) {
			return 1;
		} else if (lockmode.equals("Ready for review")) {
			return 2;
		} else if (lockmode.equals("Working on")) {
			return 3;
		} else if (lockmode.equals("New")) {
			return 4;
		}
		//unknown or missing lockmode give lowest rank
		return 5;
	}
	
	// return videos in the order specified by the position property
	// videos without position will be added to the end of the list
	private static List<Node> orderVideos(List<Node> videos) {
		return quicksortVideoOrder(new ArrayList<Node>(videos));
	}
	
	private static ArrayList<Node> quicksortVideoOrder(ArrayList<Node> videos) {
		if (videos.size() <= 1) {
			return videos;
		}
		int pivot = videos.size() / 2;
		ArrayList<Node> lesser = new ArrayList<Node>();
		ArrayList<Node> equal = new ArrayList<Node>();
		ArrayList<Node> greater = new ArrayList<Node>();
		for (Node video : videos) {
			Element v1 = (Element) video;
			Element v2 = (Element) videos.get(pivot);
			
			int p1 = v1.element("properties").elementText("position") == null ? Integer.MAX_VALUE : Integer.parseInt(v1.element("properties").elementText("position"));
			int p2 = v2.element("properties").elementText("position") == null ? Integer.MAX_VALUE : Integer.parseInt(v2.element("properties").elementText("position"));
			LOG.debug("Compare "+p1+" vs "+p2);
			
			if (p1 > p2) {
				greater.add(video);
			} else if (p1 < p2) {
				lesser.add(video);
			} else {
				equal.add(video);
			}
		}
		lesser = quicksortVideoOrder(lesser);
		for (Node video : equal) {
			lesser.add(video);
		}
		greater = quicksortVideoOrder(greater);
		ArrayList<Node> sorted = new ArrayList<Node>();
		for (Node video : lesser) {
			sorted.add(video);
		}
		for (Node video : greater) {
			sorted.add(video);
		}		
		return sorted;
	}
	
	private static IndexJob loadIndexJob(Element jElem) {
		IndexJob job = null;
		try {
			String id = jElem.getParent().valueOf("@id");
			String timestamp = jElem.valueOf("timestamp");
			String priority = jElem.valueOf("priority");
			String presentation = jElem.valueOf("presentation");
			long tt = -1;
			try {
				tt = Long.parseLong(timestamp);
			} catch(Exception e) {/* ignored */}
			job = new IndexJob();
			job.setId(id);
			job.setPriority(priority);
			job.setPresentation(presentation);
			job.setTimestamp(tt);
		} catch(Exception e) {
			LOG.error("Could not load index job",e);
		}
		return job;
	}
	
	// load user configuration what field are needed for the index
	// TODO: caching
	@SuppressWarnings("unchecked")
	private static IndexConfig loadIndexConfig(String configUri) {
		IndexConfig iConfig = new IndexConfig();
		String domain = URIParser.getDomainIdFromUri(configUri);
		String user = URIParser.getUserIdFromUri(configUri);
		Document cDoc = FSXMLRequestHandler.instance().getNodeProperties(configUri, false);
		if(cDoc == null) {
			// load default index configuration
			LOG.debug("Could not load config from "+configUri+" ... loading default");
			String indexType = DEFAULT_INDEX_TYPE;
			String indexUri = DEFAULT_INDEX_URI.replace("{domainid}",domain).replace("{user}",user);
			iConfig.setIndexType(indexType);
			iConfig.setIndexUri(indexUri);
			iConfig.setDefaultQueue(true);
		} else {
			// load config from filesystem
			LOG.debug("loaded config for "+configUri);
			List<Node> properties = cDoc.selectNodes("//properties/*");
			for (Iterator<Node> i = properties.iterator(); i.hasNext(); ) {
				Element property = (Element) i.next();
				String propertyName = property.getName();
				String propertyValue =  property.getTextTrim();
				LOG.debug("config property: "+property);
				if(propertyName.equals("indextype")) {
					iConfig.setIndexType(propertyValue);
				} else if (property.getName().equals("indexuri")) {
					String inderUri = propertyValue.replace("{domainid}",domain).replace("{user}",user);
					iConfig.setIndexUri(inderUri);
				} else if (property.getName().equals("presentationdate")) {
					iConfig.setProperty(propertyName,propertyValue);
				} else {
					iConfig.setProperty(propertyName,"enabled");
				}
			}
		}
		return iConfig;
	}
	
	//Presentation details
	private static Map<String, Object> getPresentationDetails(String presentationUri, IndexConfig iConfig) {
		Map<String, Object> presentationDetails = new HashMap<String, Object>();
		Document presentation = FSXMLRequestHandler.instance().getNodeProperties(presentationUri, false);
		String domainId = URIParser.getDomainIdFromUri(presentationUri);
		if (presentation == null) {
			LOG.debug("presentation does not exists, skip");
			return null;
		}
		
		String presentationId = presentation.selectSingleNode("//presentation/@id") == null ? "" : presentation.selectSingleNode("//presentation/@id").getText();
		
		// don't accept old lh presentations since they are duplicates
		if (presentationId == "" || (domainId.equals("lhwebtv") && presentationId.indexOf("p") == -1 && Integer.parseInt(presentationId) < 240)) {
			return null;
		}
		
		presentationDetails.put("presentation", presentation);
		presentationDetails.put("presentationId", presentationId);
		presentationDetails.put("presentationtitle", getPresentationTitle(presentation, presentationId, presentationUri));
		presentationDetails.put("presentationdescription", getPresentationDescription(presentation, presentationId, presentationUri));
		presentationDetails.put("presentationscreenshot", getPresentationScreenshot(presentation, presentationUri));
		presentationDetails.put("presentationduration", getPresentationDuration(presentation));
		
		String type = presentation.selectSingleNode("//presentation/properties/type") == null ? "" : presentation.selectSingleNode("//presentation/properties/type").getText();
		String author = presentation.selectSingleNode("//presentation/properties/author") == null ? "" : presentation.selectSingleNode("//presentation/properties/author").getText();
		String copyright = presentation.selectSingleNode("//presentation/properties/copyright") == null ? "" : presentation.selectSingleNode("//presentation/properties/copyright").getText();
		String website = presentation.selectSingleNode("//presentation/properties/website") == null ? "" : presentation.selectSingleNode("//presentation/properties/website").getText();
		
		presentationDetails.put("presentationtype", type);
		presentationDetails.put("presentationauthor", author);
		presentationDetails.put("presentationcopyright", copyright);
		presentationDetails.put("presentationwebsite", website);
		
		Map<String, String> items = iConfig.getProperties();
		for (String item : items.keySet()) {
			if (item.equals("lockmode")) {
				 String lockmode = presentation.selectSingleNode("//presentation/properties/lockmode") == null ? "null" : presentation.selectSingleNode("//presentation/properties/lockmode").getText();
				 presentationDetails.put(item, lockmode);
			} else if (item.equals("link")) {
				String link = presentation.selectSingleNode("//presentation/properties/link") == null ? "null" : presentation.selectSingleNode("//presentation/properties/link").getText();
				presentationDetails.put(item,link);
			} else if (item.equals("date_created")) {
				String date_created = presentation.selectSingleNode("//presentation/properties/date_created") == null ? "0" : presentation.selectSingleNode("//presentation/properties/date_created").getText();
				presentationDetails.put(item,date_created);
			} else if (item.equals("presentationtheme")) {
				String theme = presentation.selectSingleNode("//presentation/properties/theme") == null ? "" : presentation.selectSingleNode("//presentation/properties/theme").getText();
				presentationDetails.put(item,theme);
			} else if (item.equals("presentationlocation")) {
				String location = presentation.selectSingleNode("//presentation/properties/location") == null ? "" : presentation.selectSingleNode("//presentation/properties/location").getText();
				presentationDetails.put(item,location);
			} else if (item.equals("presentationdate")) {
				String date = presentation.selectSingleNode("//presentation/properties/date") == null ? "" : presentation.selectSingleNode("//presentation/properties/date").getText();
				date = convertDate(date, iConfig);
				presentationDetails.put(item,date);
			} else if (item.equals("presentationdate_original")) {
				String date = presentation.selectSingleNode("//presentation/properties/date") == null ? "" : presentation.selectSingleNode("//presentation/properties/date").getText();
				presentationDetails.put(item,date);
			} else if (item.equals("presentationpublic")) {
				String pub = presentation.selectSingleNode("//presentation/properties/public") == null ? "" : presentation.selectSingleNode("//presentation/properties/public").getText();
				presentationDetails.put(item, pub);
			} else if (item.equals("sponsor")) {
				String sponsor = presentation.selectSingleNode("//presentation/properties/sponsor") == null ? "" : presentation.selectSingleNode("//presentation/properties/sponsor").getText();
				presentationDetails.put(item,sponsor);
			} else if (item.equals("webtv_item_id")) {
				String webtv_item_id = presentation.selectSingleNode("//presentation/properties/webtv_item_id") == null ? "" : presentation.selectSingleNode("//presentation/properties/webtv_item_id").getText();
				presentationDetails.put(item,webtv_item_id);
			} else if (item.equals("presentationlivestate")) {
				String livestate = presentation.selectSingleNode("//presentation/properties/livestate") == null ? "" : presentation.selectSingleNode("//presentation/properties/livestate").getText();
				presentationDetails.put(item, livestate);
			}
		}		
		return presentationDetails;
	}
	
	//Collection details
	private static Map<String, Object> getCollectionDetails(String presentationUri, IndexConfig iConfig) {
		Map<String, Object> collectionDetails = new HashMap<String, Object>();
		
		String collectionPresentationUri = getCollection(presentationUri);
		if (collectionPresentationUri == null) {
			LOG.debug("presentation not refered in any collection for the same user & domain");
			return null;
		}
		String collectionUri = getSubUri(collectionPresentationUri, 6)+"/";
		
		collectionDetails.put("presentation", collectionPresentationUri);
		collectionDetails.put("collection", collectionUri);
		
		Map<String, String> items = iConfig.getProperties();
		
		Document collection = null;
		
		for (String item : items.keySet()) {
			if (item.equals("collectiontitle")) {
				collection = collection == null ? FSXMLRequestHandler.instance().getNodeProperties(collectionUri, 0, false) : collection;
				String collectionTitle = collection.selectSingleNode("//collection/properties/title") == null ? "" : collection.selectSingleNode("//collection/properties/title").getText();
				collectionDetails.put(item, collectionTitle);
			} else if (item.equals("collectiondescription")) {
				collection = collection == null ? FSXMLRequestHandler.instance().getNodeProperties(collectionUri, 0, false) : collection;
				String collectionDesc = collection.selectSingleNode("//collection/properties/description") == null ? "" : collection.selectSingleNode("//collection/properties/description").getText();
				collectionDetails.put(item, collectionDesc);
			} else if (item.equals("collectionstatus")) {
				collection = collection == null ? FSXMLRequestHandler.instance().getNodeProperties(collectionUri, 0, false) : collection;
				String collectionStatus = collection.selectSingleNode("//collection/properties/publicationstatus") == null ? "" : collection.selectSingleNode("//collection/properties/publicationstatus").getText();
				collectionDetails.put(item, collectionStatus);
			}
		}	
		return collectionDetails;
	}
	
	//Collection details
	private static Map<String, Object> getCollectionDetails(Document collection, String uri, IndexConfig iConfig) {
		Map<String, Object> collectionDetails = new HashMap<String, Object>();
		
		String collectionUri = uri +"/"+ collection.selectSingleNode("//collection/@id").getText() +"/";
		collectionDetails.put("collection", collectionUri);
		
		Map<String, String> items = iConfig.getProperties();
		for (String item : items.keySet()) {
			if (item.equals("collectiontitle")) {
				String collectionTitle = collection.selectSingleNode("//collection/properties/title") == null ? "" : collection.selectSingleNode("//collection/properties/title").getText();
				collectionDetails.put(item, collectionTitle);
			} else if (item.equals("collectiondescription")) {
				String collectionDesc = collection.selectSingleNode("//collection/properties/description") == null ? "" : collection.selectSingleNode("//collection/properties/description").getText();
				collectionDetails.put(item, collectionDesc);
			} else if (item.equals("collectionstatus")) {
				collection = collection == null ? FSXMLRequestHandler.instance().getNodeProperties(collectionUri, 0, false) : collection;
				String collectionStatus = collection.selectSingleNode("//collection/properties/publicationstatus") == null ? "" : collection.selectSingleNode("//collection/properties/publicationstatus").getText();
				collectionDetails.put(item, collectionStatus);
			}
		}
		return collectionDetails;
	}
	
	private static String getPresentationTitle(Document presentation, String presentationId, String presentationUri) {
		String presentationTitle = "";
		String domainId = URIParser.getDomainIdFromUri(presentationUri);
		presentationTitle = presentation.selectSingleNode("//presentation/properties/title") == null ? "" :presentation.selectSingleNode("//presentation/properties/title").getText();
		//jhm: remove number in front of title
		if (domainId.equals("jhm") && presentationTitle.length() > 6) {
			presentationTitle = presentationTitle.substring(6);
		}
		return presentationTitle;
	}
	
	private static String getPresentationDescription(Document presentation, String presentationId, String presentationUri) {
		String presentationDescription = "";
		String domainId = URIParser.getDomainIdFromUri(presentationUri);
		if (domainId.equals("lhwebtv") && presentationId.indexOf("p") > -1) {
			presentationDescription = presentation.selectSingleNode("//videoplaylist/video/properties/description") == null ? "" : presentation.selectSingleNode("//videoplaylist/video/properties/description").getText();
		} else {
			presentationDescription = presentation.selectSingleNode("//presentation/properties/description") == null ? "" : presentation.selectSingleNode("//presentation/properties/description").getText();
		}		
		return presentationDescription;
	}
	
	//Loop over type
	@SuppressWarnings("unchecked")
	private static void loopTypes(String presentationUri, Map<String, Object> presentationDetails, Map<String, Object> collectionDetails, IndexConfig iConfig) {
		LOG.debug("loop types");
		
		Document presentation = (Document) presentationDetails.get("presentation");
		String presentationId = (String) presentationDetails.get("presentationId");
		String domainId = URIParser.getDomainIdFromUri(presentationUri);
		
		List<Node> types = presentation.selectNodes("//videoplaylist/"+iConfig.getIndexType());
		LOG.debug("number of "+iConfig.getIndexType()+" = "+types.size());
		
		if (types.isEmpty()) {
			if (domainId.equals("lhwebtv") && presentation.selectSingleNode("//videoplaylist/video/properties/title") != null && (presentationId.indexOf("p") > -1 || Integer.parseInt(presentationId) > 240)) {
				makeTypeFromPresentation(presentationUri, presentationDetails, collectionDetails, iConfig);
			} else {
				makeTypeFromPresentation(presentationUri, presentationDetails, collectionDetails, iConfig);
			}			
		} 
		
		//loop over type
		if (!domainId.equals("lhwebtv") || presentationId.indexOf("p") > -1 || Integer.parseInt(presentationId) > 240) {
			for(Iterator<Node> iter = types.iterator(); iter.hasNext(); ) {
				Element type = (Element) iter.next();
				makeType(type, presentationUri, presentationDetails, collectionDetails, iConfig);
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					LOG.error("",e);
				}
			}
		}
	}
	
	// Get the type uri
	private static String getTypeUri(Element typeContent, String presentationUri, IndexConfig iConfig) {
		return presentationUri+"/videoplaylist/"+typeContent.getParent().attributeValue("id")+"/"+iConfig.getIndexType()+"/"+typeContent.attributeValue("id");
	}
	
	// Create an empty index
	private static void createIndex(IndexConfig iConfig) {
		FSXMLRequestHandler.instance().handlePUT(iConfig.getIndexUri()+"/properties", "<fsxml><properties><lastupdate>"+String.valueOf(new Date().getTime())+"</lastupdate></properties></fsxml>");
	}
	
	// Get the number of collections for this uri
	private static int getNumberOfCollections(String collectionUri) {
		Document collections = FSXMLRequestHandler.instance().getNodePropertiesByType(collectionUri, 0, 0, 1);
		Node resultsAvailable = collections.selectSingleNode("//properties/totalResultsAvailable");
		return Integer.parseInt(resultsAvailable.getText());
	}
	
	// Get the duration of the presentation (videos)
	@SuppressWarnings("unchecked")
	private static String getPresentationDuration(Document presentation) {
		Element video;
		double videoDuration, totalDuration = 0.0;
		List<Node> videos = presentation.selectNodes("//video");
		
		//check if special ordering is required
		if (presentation.selectSingleNode("//video/properties/position") != null) {
			videos = orderVideos(videos);
		}
		// Loop all videos
		for(Iterator<Node> iter = videos.iterator(); iter.hasNext(); ) {
			video = (Element) iter.next();
			//videos without refer can occur, don't have properties
			if (video.element("properties") == null) {
				return "0";
			}
			videoDuration = video.element("properties").elementText("duration") == null ? 0.0 : Double.parseDouble(video.element("properties").elementText("duration"));
			
			// if video does not contain duration it's not editted, take refers duration
			if (videoDuration == 0.0) {
				//get refer rawvideo 2 for duration in seconds
				Document videoNode = FSXMLRequestHandler.instance().getNodeProperties(video.attributeValue("referid"), false);
				if (videoNode != null) {
					List<Node> rawvideos = videoNode.selectNodes("//rawvideo"); 
					for(Node rawvideoNode : rawvideos) {
						String durationStr = rawvideoNode.valueOf("properties/duration");
						try {
							videoDuration = Double.parseDouble(durationStr);
						} catch(Exception e) {/* ignored */}
						
						// check if video duration was set 
						if(videoDuration != 0.0) {
							break;
						}
					}
				}
			}
			// TODO: take in/out points into account on duration
			totalDuration += videoDuration;
		}		
		int duration = (int) Math.round(totalDuration);
		
		return Integer.toString(duration);
	}
	
	/**
	 * Converts the date string given to a format which can be sorted alphabetically
	 * 
	 * @param date
	 * @return
	 */
	private static String convertDate(String dateStr, IndexConfig iConfig) { 
		// determine conversion format
		Map<String, String> items = iConfig.getProperties();
		
		// check if 'presentationdate' contains a format string
		if(items!=null && items.get("presentationdate")!=null && !items.get("presentationdate").equals("")) {
			String inFormatStr = items.get("presentationdate");
			try {
				SimpleDateFormat inFormat = new SimpleDateFormat(inFormatStr);
				Date date = inFormat.parse(dateStr);
				dateStr = SORTABLE_DATE_FORMAT.format(date);
			} catch(IllegalArgumentException iae) {
				LOG.error("Could not parse input format -- "+inFormatStr);
			} catch(ParseException pe) {
				LOG.error("Could not parse input date string -- "+dateStr);
			}
		}
		
		return dateStr;
	}
	
	private static class Config {
		private String domain;
		private String queueUri;
		private List<String> indexesCreated = new ArrayList<String>();
		
		public void addIndexCreated(String subUri) {
			indexesCreated.add(subUri);
		}
		
		public boolean isIndexCreated(String subUri) {
			return indexesCreated.contains(subUri);
		}
		
		/**
		 * @return the domain
		 */
		public String getDomain() {
			return domain;
		}
		/**
		 * @param domain the domain to set
		 */
		public void setDomain(String domain) {
			this.domain = domain;
		}
		/**
		 * @return the queueUri
		 */
		public String getQueueUri() {
			return queueUri;
		}
		/**
		 * @param queueUri the queueUri to set
		 */
		public void setQueueUri(String queueUri) {
			this.queueUri = queueUri;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "Config [domain=" + domain + ", queueUri=" + queueUri
					+ ", indexesCreated=" + indexesCreated + "]";
		}
	}
	
	private static class IndexConfig {
		private String indexUri;
		private String indexType;
		private Map<String,String> properties;
		private boolean defaultQueue;
		
		/**
		 * Constructor
		 */
		public IndexConfig() {
			properties = new HashMap<String,String>();
			defaultQueue = false;
		}
		
		/**
		 * Add property
		 * 
		 * @param propertyName
		 * @param propertyValue
		 */
		public void setProperty(String propertyName, String propertyValue) {
			properties.put(propertyName, propertyValue);
		}
		
		/**
		 * Get property
		 * 
		 * @param propertyName
		 * @return
		 */
		public String getProperty(String propertyName) {
			return properties.get(propertyName);
		}
		
		/**
		 * @return the properties
		 */
		public Map<String, String> getProperties() {
			return properties;
		}
		/**
		 * @return the indexType
		 */
		public String getIndexType() {
			return indexType;
		}
		/**
		 * @param indexType the indexType to set
		 */
		public void setIndexType(String indexType) {
			this.indexType = indexType;
		}
		/**
		 * @return the indexUri
		 */
		public String getIndexUri() {
			return indexUri;
		}
		/**
		 * @param indexUri the indexUri to set
		 */
		public void setIndexUri(String indexUri) {
			this.indexUri = indexUri;
		}
		/**
		 * @return the defaultQueue
		 */
		public boolean isDefaultQueue() {
			return defaultQueue;
		}
		/**
		 * @param defaultQueue the defaultQueue to set
		 */
		public void setDefaultQueue(boolean defaultQueue) {
			this.defaultQueue = defaultQueue;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "IndexConfig [indexUri=" + indexUri + ", indexType="
					+ indexType + ", defaultQueue=" + defaultQueue + "]";
		}
	}
	
	private static class IndexJob {
		private String id;
		private String priority;
		private String presentation;
		private long timestamp;
		/**
		 * @return the id
		 */
		public String getId() {
			return id;
		}
		/**
		 * @param id the id to set
		 */
		public void setId(String id) {
			this.id = id;
		}
		/**
		 * @return the priority
		 */
		public String getPriority() {
			return priority;
		}
		/**
		 * @param priority the priority to set
		 */
		public void setPriority(String priority) {
			this.priority = priority;
		}
		/**
		 * @return the presentation
		 */
		public String getPresentation() {
			return presentation;
		}
		/**
		 * @param presentation the presentation to set
		 */
		public void setPresentation(String presentation) {
			this.presentation = presentation;
		}
		/**
		 * @return the timestamp
		 */
		public long getTimestamp() {
			return timestamp;
		}
		/**
		 * @param timestamp the timestamp to set
		 */
		public void setTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "IndexJob [id=" + id + ", priority="
					+ priority + ", presentation=" + presentation
					+ ", timestamp=" + timestamp + "]";
		}
	}
	
	/**
	 * Pool for thread execution
	 */
	public static class WorkQueue {
	    private final int nThreads;
	    private final PoolWorker[] threads;
	    private final LinkedList<Runnable> queue;

	    /**
	     * Constructor
	     * 
	     * @param nThreads
	     */
	    public WorkQueue(int nThreads) {
	        this.nThreads = nThreads;
	        queue = new LinkedList<Runnable>();
	        threads = new PoolWorker[this.nThreads];

	        for (int i=0; i<nThreads; i++) {
	            threads[i] = new PoolWorker();
	            threads[i].setDaemon(true);
	            threads[i].start();
	        }
	    }

	    /**
	     * Add runnable to execution queue
	     * 
	     * @param r
	     */
	    public void execute(Runnable r) {
	        synchronized(queue) {
	            queue.addLast(r);
	            queue.notify();
	        }
	    }
	    
	    /**
	     *  wait for the queue to be empty
	     */
	    public void join() {
	    	while (!queue.isEmpty()) {
	    		try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {/* do nothing */}
	    	}
	    }

	    /**
	     * Worker thread
	     */
	    private class PoolWorker extends Thread {
	    	
	    	/**
	    	 * Run
	    	 */
	    	public void run() {
	            Runnable r;

	            while (true) {
	                synchronized(queue) {
	                    while (queue.isEmpty()) {
	                        try {
	                            queue.wait();
	                        } catch (InterruptedException e) {/* do nothing */}
	                    }
	                    r = (Runnable) queue.removeFirst();
	                }

	                // If we don't catch RuntimeException, 
	                // the pool could leak threads
	                try {
	                    r.run();
	                }
	                catch (RuntimeException e) {
	                    LOG.error("",e);
	                }
	            }
	        }
	    	
	    }
	}
}