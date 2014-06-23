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

public class GenericIndexAction extends ActionAdapter {
	
	private static final long delay = 300000L;			/* (ms) delay for low priority queue items */			
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = Logger.getLogger(GenericIndexAction.class);
	private static final String QUEUE_URI = "/domain/{domainid}/service/smithers/queue/genericindex/job";
	private static final String CONFIG_URI = "/domain/{domainid}/user/{user}/config/{type}";
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
				LOG.debug("Starting GenericIndexAction run");
				
				// create separate configuration object
				String domain = URIParser.getDomainIdFromUri(eventUri);
				String queueUri = QUEUE_URI.replace("{domainid}", domain);
				Config config = new Config();
				config.setDomain(domain);
				config.setQueueUri(queueUri);
				
				// start
				handleQueue(config);
				
				LOG.debug("Finished GenericIndexAction run");
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
		if (job.getIndexObject() != null) {
			String jobId = job.getId();
			String objectUri = job.getIndexObject();
			String type = job.getType();
			String domain = config.getDomain();
			String user = URIParser.getUserIdFromUri(objectUri); 
			
			//check if we got a valid job
			if (user == null || domain == null || user.equals("") || domain.equals("")) {
				return;
			}
			
			// load index configuration
			String configUri = CONFIG_URI.replace("{domainid}", domain).replace("{user}", user).replace("{type}", type);
			IndexConfig iConfig = loadIndexConfig(configUri);
			
			// create or update index
			/* 
			 * TODO: don't use addIndexCreated or isIndexCreated, because it will screw up in 
			 * index jobs running on multiple machines
			 */
			LOG.debug("index config: "+ iConfig);
			if (!FSXMLRequestHandler.instance().hasChildren(iConfig.getIndexUri(), iConfig.getIndexType())) {
				createObjectIndex(objectUri, iConfig);
				config.addIndexCreated(getSubUri(objectUri, 5));
			} else  if(!config.isIndexCreated(getSubUri(iConfig.getIndexUri(),5))) {
				updateObjectIndex(objectUri, iConfig);
			}
			
			// check for second index
			String configUri2 = CONFIG2_URI.replace("{domainid}", domain).replace("{user}", user);
			IndexConfig iConfig2 = loadIndexConfig(configUri2);
			if (!iConfig2.isDefaultQueue()) {  // don't do a default one				
				// create or update index
				if (!FSXMLRequestHandler.instance().hasChildren(iConfig.getIndexUri(), iConfig.getIndexType())) {
					createObjectIndex(objectUri, iConfig2);
					config.addIndexCreated(getSubUri(objectUri, 5));
				} else  if(!config.isIndexCreated(getSubUri(iConfig.getIndexUri(),5))) {
					updateObjectIndex(objectUri, iConfig2);
				}			
			}	
			
			// remove job from filesystem
			FSXMLRequestHandler.instance().deleteNodeProperties(config.getQueueUri()+"/"+jobId,true);
		}
	}

	/**
	 * Update the index
	 * 
	 * @param objectUri
	 * @param iConfig
	 */
	private static void updateObjectIndex(String uri, IndexConfig iConfig) {
		LOG.debug("updateObjectIndex: "+uri+", index config: "+iConfig);
		
		// get object and collection details
		Map<String, Object> objectDetails = getObjectDetails(uri, iConfig);
		Map<String, Object> collectionDetails = getCollectionDetails(uri, iConfig);
		
		// remove indexes of specific type from the object
		deleteTypes(uri, iConfig);
		
		// 
		if (objectDetails != null && collectionDetails != null) {		
			loopTypes(uri, objectDetails, collectionDetails, iConfig);
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void createObjectIndex(String uri, IndexConfig iConfig) {	
		LOG.debug("createObjectIndex: "+uri+", index config: "+iConfig);
		
		createIndex(iConfig);
		
		String indexObject = iConfig.getIndexObject();
		
		String collectionUri = getSubUri(uri, 4)+"/collection";		
		int numCollections = getNumberOfCollections(collectionUri);
		
		/* Loop over all collections */
		for (int i = 0; i < numCollections; i++) {
			Document collection = FSXMLRequestHandler.instance().getNodePropertiesByType(collectionUri, 10, i, 1);
			
			Map<String, Object> collectionDetails = getCollectionDetails(collection, collectionUri, iConfig);			
			List<Node> objects = collection.selectNodes("//"+indexObject);
			LOG.debug("found "+objects.size()+" for node type "+indexObject+" in collection");
			
			/* loop over all objects from a collection */
			for(Iterator<Node> iter = objects.iterator(); iter.hasNext(); ) {
				Element object = (Element) iter.next();				
				collectionDetails.put("object", (String) collectionDetails.get("collection")+indexObject+"/"+object.attributeValue("id")+"/");
				String objectUri = object.attributeValue("referid");
				
				if (objectUri != null && objectUri.startsWith("/domain/")) {
					Map<String, Object> objectDetails = getObjectDetails(objectUri, iConfig);
					if (objectDetails != null) {
						loopTypes(objectUri, objectDetails, collectionDetails, iConfig);
					}
				}
			}			
		}
	}
	
	/**
	 * Create XML for an index
	 * 
	 * @param objectUri
	 * @param objectDetails
	 * @param collectionDetails
	 * @param iConfig
	 */
	private static void makeTypeFromObject(String objectUri, Map<String, Object> objectDetails, Map<String, Object> collectionDetails, IndexConfig iConfig) {
		Document typeDocument = DocumentHelper.createDocument();
		Element fsxml = typeDocument.addElement("fsxml");
		Element type = fsxml.addElement(iConfig.getIndexType());
		
		String objectType = iConfig.getIndexObject();
		
		// add refer to original object
		Element referObject = type.addElement(objectType);
		referObject.addAttribute("id", "1");
		referObject.addAttribute("referid", objectUri);
		
		//add refer to original collection, remove trailing slash, otherwise refer cannot be found!
		Element referCollectionObject = type.addElement("collection"+objectType);
		referCollectionObject.addAttribute("id", "1");		
		String collectionObject = (String) collectionDetails.get("object");
		collectionObject = collectionObject.substring(0, collectionObject.length()-1);
		referCollectionObject.addAttribute("referid", collectionObject);
		
		Element properties = type.addElement("properties");
		//add standard properties		
		properties.addElement(objectType).addText((String) collectionDetails.get("object"));
		properties.addElement(objectType+"uri").addText(objectUri+"/");		
		properties.addElement(objectType+"title").addText((String) objectDetails.get(objectType+"title"));
		properties.addElement(objectType+"description").addText((String) objectDetails.get(objectType+"description"));
		properties.addElement(objectType+"screenshot").addText((String) objectDetails.get(objectType+"screenshot"));
		//properties.addElement(objectType+"type").addText((String) objectDetails.get(objectType+"type"));
		//properties.addElement(objectType+"author").addText((String) objectDetails.get(objectType+"author"));
		//properties.addElement(objectType+"copyright").addText((String) objectDetails.get(objectType+"copyright"));
		//properties.addElement(objectType+"website").addText((String) objectDetails.get(objectType+"website"));
		
		properties.addElement("collection").addText((String) collectionDetails.get("collection"));
		
		//add user configured properties
		Map<String, String> items = iConfig.getProperties();
		for (String item : items.keySet()) {
			if (item.equals("collectiontitle") || item.equals("collectiondescription") || item.equals("collectionstatus")) {
				properties.addElement(item).addText((String) collectionDetails.get(item));
			} else if (item.equals("lockmode") || item.equals("date_created") || item.equals(objectType+"duration") || item.equals(objectType+"theme") || item.equals(objectType+"location") || item.equals(objectType+"date") || item.equals(objectType+"date_original") || item.equals(objectType+"public") || item.equals("sponsor")) {
				properties.addElement(item).addText((String) objectDetails.get(item));
			} else if (item.equals("title") || item.equals("name")) {
				properties.addElement(item).addText((String) objectDetails.get(objectType+"title"));
			} else if (item.equals("description")) {
				properties.addElement(item).addText((String) objectDetails.get(objectType+"description"));
			} else if (item.equals("screenshot")) {
				properties.addElement(item).addText((String) objectDetails.get(objectType+"screenshot"));
			} else if (item.equals("rank")) {
				properties.addElement(item).addText("5");
			} else if (item.equals("peercomments")) {
				properties.addElement(item).addText(getType((Document) objectDetails.get(objectType), "peercomments", 0.0, 86400000.0, new String[] {"comment"}));
			} else if (item.equals("bookmark")) {
				properties.addElement(item).addText(getType((Document) objectDetails.get(objectType), "bookmark", 0.0, 86400000.0, new String[] {"title", "description", "creator"}));
			} else if (item.equals("webtv_item_id") || item.equals(objectType+"livestate")) {
				properties.addElement(item).addText((String) objectDetails.get(item));
			} else if (item.equals(objectType+"status") || item.equals(objectType+"theme") || item.equals(objectType+"time")) {
				properties.addElement(item).addText((String) objectDetails.get(item));
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
	 * @param objectUri
	 * @param objectDetails
	 * @param collectionDetails
	 * @param iConfig
	 */
	private static void makeType(Element typeContent, String objectUri, Map<String, Object> objectDetails, Map<String, Object> collectionDetails, IndexConfig iConfig) {
		// create new type
	    Document typeDocument = DocumentHelper.createDocument();
	    Element fsxml = typeDocument.addElement("fsxml");
		Element type = fsxml.addElement(iConfig.getIndexType());
		
		String objectType = iConfig.getIndexObject();
		
		 // add refer to original object
		Element referObject = type.addElement(objectType);
		referObject.addAttribute("id", "1");
		referObject.addAttribute("referid", objectUri);
		
		// add refer to original collection
 		Element referCollectionObject = type.addElement("collection"+objectType);
		referCollectionObject.addAttribute("id", "1");
		String collectionObject = (String) collectionDetails.get("object");
		collectionObject = collectionObject.substring(0, collectionObject.length()-1);
		referCollectionObject.addAttribute("referid", (String) collectionObject);
		
		 Element properties = type.addElement("properties");
		//add standard properties
		properties.addElement(objectType).addText((String) collectionDetails.get("object"));
		properties.addElement(objectType+"uri").addText(objectUri+"/");
		properties.addElement("collection").addText((String) collectionDetails.get("collection"));
		properties.addElement(objectType+"title").addText((String) objectDetails.get(objectType+"title"));
		properties.addElement(objectType+"description").addText((String) objectDetails.get(objectType+"description"));
		properties.addElement(objectType+"screenshot").addText((String) objectDetails.get(objectType+"screenshot"));
		//properties.addElement(objectType+"type").addText((String) objectDetails.get(objectType+"type"));
		//properties.addElement(objectType+"author").addText((String) objectDetails.get(objectType+"author"));
		//properties.addElement(objectType+"copyright").addText((String) objectDetails.get(objectType+"copyright"));
		//properties.addElement(objectType+"website").addText((String) objectDetails.get(objectType+"website"));
		 
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
			} else if (item.equals("lockmode") || item.equals("link") || item.equals("date_created") || item.equals(objectType+"duration") || item.equals(objectType+"theme") || item.equals(objectType+"location") || item.equals(objectType+"date") || item.equals(objectType+"date_original") || item.equals(objectType+"public") || item.equals("sponsor")) {
				properties.addElement(item).addText((String) objectDetails.get(item));
			} else if (item.equals("title") || item.equals("description") || item.equals("name")) {
				String value = typeProperties.elementText(item) == null ? "" : typeProperties.elementText(item);
				properties.addElement(item).addText(value);
			} else if (item.equals("firstnamelastname")) {
				String firstname = typeProperties.elementText("firstname") == null ? "" : typeProperties.elementText("firstname");
				String lastname = typeProperties.elementText("lastname") == null ? "" : typeProperties.elementText("lastname");
				properties.addElement("name").addText(firstname+" "+lastname);
			} else if (item.equals("screenshot")) {
				//former chapterscreenshot
				properties.addElement(item).addText(getTypeScreenshot((Document) objectDetails.get(objectType), start, (String) objectDetails.get(objectType+"screenshot")));
			} else if (item.equals("rank")) {
				properties.addElement(item).addText(String.valueOf(getRankBasedOnLockmode((String) objectDetails.get("lockmode"))));
			} else if (item.equals("start")) {
				properties.addElement(item).addText(String.format(Locale.US, "%f", start));
			} else if (item.equals("duration")) {
				properties.addElement(item).addText(String.format(Locale.US, "%f", duration));
			} else if (item.equals("locations")) {
				properties.addElement(item).addText(getType((Document) objectDetails.get(objectType), "location", start, duration, new String[] {"name"}));
			} else if (item.equals("dates")) {
				properties.addElement(item).addText(getType((Document) objectDetails.get(objectType), "date", start, duration, new String[] {"start", "end"}));
			} else if (item.equals("keywords")) {
				properties.addElement(item).addText(getType((Document) objectDetails.get(objectType), "keyword", start, duration, new String[] {"name"}));
			} else if (item.equals("persons")) {
				properties.addElement(item).addText(getType((Document) objectDetails.get(objectType), "person", start, duration, new String[] {"name"}));
			} else if (item.equals("periods")) {
				properties.addElement(item).addText(getType((Document) objectDetails.get(objectType), "period", start, duration, new String[] {"name"}));
			} else if (item.equals("speakers")) { 
				properties.addElement(item).addText(getType((Document) objectDetails.get(objectType), "speakers", start, duration, new String[] {"firstname", "lastname", "organization"}));
			} else if (item.equals("topics")) {
				properties.addElement(item).addText(getType((Document) objectDetails.get(objectType), "topics", start, duration, new String[] {"name"}));
			} else if (item.equals("peercomments")) {
				properties.addElement(item).addText(getType((Document) objectDetails.get(objectType), "peercomments", 0.0, 86400000.0, new String[] {"comment"}));
			} else if (item.equals("voiceindex")) {
				String voiceindex = typeProperties.elementText("voiceindex") == null ? "" : typeProperties.elementText("voiceindex");
				properties.addElement(item).addText(voiceindex);
			} else if (item.equals(objectType+iConfig.getIndexType())) {
				properties.addElement(item).addText(getTypeUri(typeContent, objectUri, iConfig));
			} else if (item.equals("bookmark")) {
				properties.addElement(item).addText(getType((Document) objectDetails.get(objectType), "bookmark", 0.0, 86400000.0, new String[] {"title", "description", "creator"}));
			} else if (item.equals("webtv_item_id") || item.equals(objectType+"livestate")) {
				properties.addElement(item).addText((String) objectDetails.get(item));
			}  else if (item.equals(objectType+"status") || item.equals(objectType+"theme") || item.equals(objectType+"time") || item.equals(objectType+"type")) {
				properties.addElement(item).addText((String) objectDetails.get(item));
			}
		}
		long timestamp = new Date().getTime();
		type.addAttribute("id", String.valueOf(timestamp));
		
		// Add directly to fs so maggie get's updated with first content faster
		FSXMLRequestHandler.instance().saveFsXml(iConfig.getIndexUri(), typeDocument.asXML(), "PUT", true);
	}
	
	// Delete all index types from the current object
	private static void deleteTypes(String objectUri, IndexConfig iConfig) {
		LOG.debug("deleting "+ iConfig.getIndexType() +" with objectUri "+objectUri);
		// get index types refering to object
		List<String> refers = FSXMLRequestHandler.instance().getReferParents(objectUri);

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
	
	// Get screenshot for the object from first videoplaylist that contains a video with screenshots
	@SuppressWarnings("unchecked")
	private static String getObjectScreenshot(Document object, String objectUri) {
		int seconds = -1;
		String domain = URIParser.getDomainIdFromUri(objectUri);
		//check if screenshottime property of the object is set
		Double screenshottime = object.selectSingleNode("//properties/screenshottime") == null ? -1.0 : Double.parseDouble(object.selectSingleNode("//properties/screenshottime").getText());

		if (screenshottime != -1.0) {
			seconds = screenshottime.intValue();
		}
		
		Element video;		
		List<Node> videos = object.selectNodes("//video");
		
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
						seconds = 7;
						return getVideoScreenshot(refer)+secondsToImageUri(seconds,false);
					} else {
						Double duration = rawVideo.selectSingleNode("//properties/duration") == null ? 10000.0 : Double.parseDouble(rawVideo.selectSingleNode("//properties/duration").getText())*1000;
						//set duration
						seconds = seconds == -1 ? (int) (duration/2000) : seconds;
						if ((domain.equals("lhwebtv") || domain.equals("jhm")) && seconds < 150 && duration > 150000) {
							seconds = seconds == -1 ? 150 : seconds;
						}				
						return getVideoScreenshot(refer)+secondsToImageUri(seconds,false);
					}
				}
				LOG.debug("no rawvideo found "+video.asXML());
				return "";
			} else {
				Double starttime = Double.parseDouble(video.selectSingleNode("//video/properties/starttime").getText());
				seconds = seconds == -1 ? (int) (starttime/1000) : ((int) (starttime/1000)) + seconds;
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
				LOG.debug("starttime objectscreenshot = "+seconds);
				return getVideoScreenshot(refer)+secondsToImageUri(seconds,false);
			}
		}
		LOG.debug("no video found in object");
		
		//Try if we can find any node that ends with screenshot in the current document, return that since we didn't find anything else
		//String finalScreenshot = object.selectSingleNode("//*[ends-with(name(), 'screenshot')]") == null ? "" : object.selectSingleNode("//*[ends-with(name(), 'screenshot')]").getText();
		//if (finalScreenshot != "") {
		//	return finalScreenshot;
		//}
		return "";
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
	private static String getTypeScreenshot(Document object, double typeStart, String objectScreenshot) {
		Element video;
		double videoDuration, videoStart, totalDuration = 0.0;
		List<Node> videos = object.selectNodes("//video");
		
		//check if special ordering is required
		if (object.selectSingleNode("//video/properties/position") != null) {
			videos = orderVideos(videos);
		}
		// Loop all videos
		for(Iterator<Node> iter = videos.iterator(); iter.hasNext(); ) {
			video = (Element) iter.next();
			videoDuration = video.element("properties").elementText("duration") == null ? 0.0 : Double.parseDouble(video.element("properties").elementText("duration"));
			videoStart = video.element("properties").elementText("starttime") == null ? 0.0 : Double.parseDouble(video.element("properties").elementText("starttime"));
			
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
		return objectScreenshot;
	}
	
	// Get for a specified type all attributes that fit in the time range
	@SuppressWarnings("unchecked")
	private static String getType(Document object, String type, double cStart, double cDuration, String[] aNames) {
		Element e;
		double eStart, eDuration;
		List<Node> typeList = object.selectNodes("//"+type);
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
	
	// Get collection object is in
	private static String getCollection(String objectUri) {
		List<String> refers = FSXMLRequestHandler.instance().getReferParents(objectUri);
		String refer = "";
		int size = refers.size();
		String domain = URIParser.getDomainIdFromUri(objectUri);
		String user = URIParser.getUserIdFromUri(objectUri);
		
		LOG.debug("found "+size+" refers for "+objectUri);
		
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
		//object not referred (anymore) in a collection for this domain & user
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
			String type = jElem.valueOf("type") == null ? "presentation" : jElem.valueOf("type");
			String indexObject = jElem.valueOf(type);
			long tt = -1;
			try {
				tt = Long.parseLong(timestamp);
			} catch(Exception e) {/* ignored */}
			job = new IndexJob();
			job.setId(id);
			job.setPriority(priority);
			job.setType(type);
			job.setIndexObject(indexObject);
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
				} else if (property.getName().equals("indexobject")) {
					iConfig.setIndexObject(propertyValue);
				}
				else if (property.getName().equals("presentationdate")) {
					iConfig.setProperty(propertyName,propertyValue);
				} else {
					iConfig.setProperty(propertyName,"enabled");
				}
			}
		}
		return iConfig;
	}
	
	private static Map<String, Object> getObjectDetails(String objectUri, IndexConfig iConfig) {
		Map<String, Object> objectDetails = new HashMap<String, Object>();
		
		Document object = FSXMLRequestHandler.instance().getNodeProperties(objectUri, false);
		String domainId = URIParser.getDomainIdFromUri(objectUri);
		if (object == null) {
			LOG.debug("object does not exists, skip");
			return null;
		}
		
		String indexObject = iConfig.getIndexObject();		
		String objectId = object.selectSingleNode("//"+indexObject+"/@id") == null ? "" : object.selectSingleNode("//"+indexObject+"/@id").getText();
		
		objectDetails.put(indexObject, object);
		objectDetails.put(indexObject+"Id", objectId);
		objectDetails.put(indexObject+"title", getObjectTitle(object, objectId, objectUri, indexObject));
		objectDetails.put(indexObject+"description", getObjectDescription(object, objectId, objectUri, indexObject));
		objectDetails.put(indexObject+"screenshot", getObjectScreenshot(object, objectUri));
		objectDetails.put(indexObject+"duration", getObjectDuration(object));
		
		String type = object.selectSingleNode("//"+indexObject+"/properties/type") == null ? "" : object.selectSingleNode("//"+indexObject+"/properties/type").getText();
		
		objectDetails.put(indexObject+"type", type);
		
		Map<String, String> items = iConfig.getProperties();
		for (String item : items.keySet()) {
			if (item.equals(indexObject+"theme")) {
				String theme = object.selectSingleNode("//"+indexObject+"/properties/theme") == null ? "" : object.selectSingleNode("//"+indexObject+"/properties/theme").getText();
				objectDetails.put(item,theme);
			} else if (item.equals(indexObject+"status")) {
				String status = object.selectSingleNode("//"+indexObject+"/properties/status") == null ? "" : object.selectSingleNode("//"+indexObject+"/properties/status").getText();
				objectDetails.put(item,status);
			} else if (item.equals(indexObject+"time")) {
				String time = object.selectSingleNode("//"+indexObject+"/properties/time") == null ? "" : object.selectSingleNode("//"+indexObject+"/properties/time").getText();
				objectDetails.put(item,time);
			}
		}
		
		return objectDetails;
	}
	
	//Collection details
	private static Map<String, Object> getCollectionDetails(String objectUri, IndexConfig iConfig) {
		Map<String, Object> collectionDetails = new HashMap<String, Object>();
		
		String collectionObjectUri = getCollection(objectUri);
		if (collectionObjectUri == null) {
			LOG.error("object not refered in any collection for the same user & domain");
			return null;
		}
		String collectionUri = getSubUri(collectionObjectUri, 6)+"/";
		
		collectionDetails.put("object", collectionObjectUri);
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
	
	private static String getObjectTitle(Document object, String objectId, String objectUri, String objectType) {
		String objectTitle = "";
		String domainId = URIParser.getDomainIdFromUri(objectUri);
		objectTitle = object.selectSingleNode("//"+objectType+"/properties/title") == null ? "" :object.selectSingleNode("//"+objectType+"/properties/title").getText();
		//jhm: remove number in front of title
		if (domainId.equals("jhm") && objectTitle.length() > 6) {
			objectTitle = objectTitle.substring(6);
		}
		return objectTitle;
	}
	
	private static String getObjectDescription(Document object, String objectId, String objectUri, String objectType) {
		String objectDescription = "";
		String domainId = URIParser.getDomainIdFromUri(objectUri);
		if (domainId.equals("lhwebtv") && objectId.indexOf("p") > -1) {
			objectDescription = object.selectSingleNode("//videoplaylist/video/properties/description") == null ? "" : object.selectSingleNode("//videoplaylist/video/properties/description").getText();
		} else {
			objectDescription = object.selectSingleNode("//"+objectType+"/properties/description") == null ? "" : object.selectSingleNode("//"+objectType+"/properties/description").getText();
		}		
		return objectDescription;
	}
	
	//Loop over type
	@SuppressWarnings("unchecked")
	private static void loopTypes(String objectUri, Map<String, Object> objectDetails, Map<String, Object> collectionDetails, IndexConfig iConfig) {
		LOG.debug("loop types");
		
		Document object = (Document) objectDetails.get(iConfig.getIndexObject());
		String objectId = (String) objectDetails.get(iConfig.getIndexObject()+"Id");
		String domainId = URIParser.getDomainIdFromUri(objectUri);
		
		List<Node> types = object.selectNodes("//"+iConfig.getIndexType());
		LOG.debug("number of "+iConfig.getIndexType()+" = "+types.size());
		
		if (types.isEmpty()) {
			if (domainId.equals("lhwebtv") && object.selectSingleNode("//videoplaylist/video/properties/title") != null && (objectId.indexOf("p") > -1 || Integer.parseInt(objectId) > 240)) {
				makeTypeFromObject(objectUri, objectDetails, collectionDetails, iConfig);
			} else {
				makeTypeFromObject(objectUri, objectDetails, collectionDetails, iConfig);
			}			
		} 
		
		//loop over type
		if (!domainId.equals("lhwebtv") || objectId.indexOf("p") > -1 || Integer.parseInt(objectId) > 240) {
			for(Iterator<Node> iter = types.iterator(); iter.hasNext(); ) {
				Element type = (Element) iter.next();
				makeType(type, objectUri, objectDetails, collectionDetails, iConfig);
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					LOG.error("",e);
				}
			}
		}
	}
	
	// Get the type uri
	private static String getTypeUri(Element typeContent, String objectUri, IndexConfig iConfig) {
		//return objectUri+"/videoplaylist/"+typeContent.getParent().attributeValue("id")+"/"+iConfig.getIndexType()+"/"+typeContent.attributeValue("id");
		return objectUri+"/"+typeContent.getParent().attributeValue("id")+"/"+iConfig.getIndexType()+"/"+typeContent.attributeValue("id");
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
	
	// Get the duration of the object (videos)
	@SuppressWarnings("unchecked")
	private static String getObjectDuration(Document object) {
		Element video;
		double videoDuration, totalDuration = 0.0;
		List<Node> videos = object.selectNodes("//video");
		
		//check if special ordering is required
		if (object.selectSingleNode("//video/properties/position") != null) {
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
		private String indexObject = "presentation";
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
		 * @ return the object to index
		 */
		public String getIndexObject() {
			return indexObject;
		}
		/**
		 * @param indexObject the object to index
		 */
		public void setIndexObject(String indexObject) {
			this.indexObject = indexObject;
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
					+ indexType + ", defaultQueue=" + defaultQueue + ", indexObject="+ indexObject +"]";
		}
	}
	
	private static class IndexJob {
		private String id;
		private String priority;
		private String type;
		private String indexObject;
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
		 * @return the type
		 */
		public String getType() {
			return type;
		}
		/**
		 * @param type the type to set
		 */
		public void setType(String type) {
			this.type = type;
		}		
		/**
		 * @return the index object
		 */
		public String getIndexObject() {
			return indexObject;
		}
		/**
		 * @param indexObject the index object to set
		 */
		public void setIndexObject(String indexObject) {
			this.indexObject = indexObject;
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
					+ priority + ", indexObject=" + indexObject
					+ ", timestamp=" + timestamp + ", type="+ type +"]";
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