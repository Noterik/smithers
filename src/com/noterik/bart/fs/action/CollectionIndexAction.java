package com.noterik.bart.fs.action;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.fs.URIParser;

public class CollectionIndexAction extends ActionAdapter {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(CollectionIndexAction.class);
	private static final String QUEUE_URI = "/domain/{domainid}/service/smithers/queue/collectionindex/job";
	private static final String CONFIG_URI = "/domain/{domainid}/user/{user}/config/collectionindex";
	private static final String DEFAULT_INDEX_TYPE = "item";
	private static final String DEFAULT_INDEX_URI = "/domain/{domainid}/user/{user}/index/collectionindex";
	
	private String domainid;
	private String userid;
	private String indexUri;	
	private String indexType;
	private String indexSaveType = null;
	
	Document typeDocument = DocumentHelper.createDocument();
	
	/* To avoid too many requests keep user configs in memory */
	private Map<String, Map<String, String>> userConfigMemory;
	
	public String run() {
		new Thread() {
			@Override
			public void run() {
				logger.debug("run collectionindex");
				String eventUri = event.getUri();
				logger.debug("eventUri = "+eventUri);
				domainid = URIParser.getDomainFromUri(eventUri);
				String collectionUri = getSubUri(eventUri, 6)+"/";
				handle(collectionUri);
			}
		}.start();
		return null;
	}
	
	private void handle(String collectionUri) {
		userid = URIParser.getUserFromUri(collectionUri);
		logger.debug("collectionuri = "+collectionUri+" load config");
		loadIndexConfig();
		
		logger.debug("check if "+indexUri+" has childs of type "+indexSaveType);
		
		if (!FSXMLRequestHandler.instance().hasChildren(indexUri, indexSaveType)) {
			createCollectionIndex(collectionUri);
		} else {
			updateCollectionIndex(collectionUri);
		}
	}
	
	// load user configuration what field are needed for the index
	private void loadIndexConfig() {
		if (userConfigMemory == null) {
			userConfigMemory = new HashMap<String, Map<String, String>>();
		}
		if (!userConfigMemory.containsKey(domainid+":"+userid)) {
			logger.debug("config not yet in memory for "+domainid+" "+userid);
			Map<String, String> propertiesMap = new HashMap<String, String>();
			
			String configUri = CONFIG_URI.replace("{domainid}", domainid).replace("{user}", userid);
			Document configuration = FSXMLRequestHandler.instance().getNodeProperties(configUri, false);
			
			if (configuration != null) {
				logger.debug("configuration "+configuration.asXML());
				
				List<Node> properties = configuration.selectNodes("//properties/*");
				for (Iterator<Node> i = properties.iterator(); i.hasNext(); ) {
					Element property = (Element) i.next();				
					if(property.getName().equals("indextype")) {
						indexType = property.getText();
					} else if (property.getName().equals("indexuri")) {
						indexUri = property.getText().replace("{domainid}", domainid).replace("{user}", userid);
					} else if (property.getName().equals("indexsavetype")) {
						indexSaveType = property.getText();
					} else {
						propertiesMap.put(property.getName(),"enabled");
					}
				}
			} else {
				logger.debug("get default config variables");
				//default indextype & uri when configuration is not available
				indexType = DEFAULT_INDEX_TYPE;
				indexSaveType = DEFAULT_INDEX_TYPE;
				indexUri = DEFAULT_INDEX_URI.replace("{domainid}", domainid).replace("{user}", userid);
			}
			if (indexSaveType == null) {
				indexSaveType = indexType;
			}
			userConfigMemory.put(domainid+":"+userid, propertiesMap);
		} else {
			logger.debug("config found in memory for "+domainid+":"+userid);
		}
	}
	
	private void updateCollectionIndex(String collectionUri) {
		Map<String, Object> collectionDetails = getCollectionDetails(collectionUri);
		
		deleteTypes(collectionUri);
		
		if (collectionDetails != null) {		
			loopTypes(collectionUri, collectionDetails);
		}
	}
	
	private void createCollectionIndex(String uri) {	
		logger.debug("create index");
		createIndex();
			
		String collectionUri = getSubUri(uri, 5);		
		int numCollections = getNumberOfCollections(collectionUri);
		
		logger.debug("loop over all collections = "+numCollections);
		/* Loop over all collections */
		for (int i = 0; i < numCollections; i++) {
			Document collection = FSXMLRequestHandler.instance().getNodePropertiesByType(collectionUri, 10, i, 1);
			Map<String, Object> collectionDetails = getCollectionDetails(collection, collectionUri);			
	
			if (collectionDetails != null) {
					loopTypes(collectionUri, collectionDetails);
			}			
		}
	}
	
	private void makeTypeFromCollection(Map<String, Object> collectionDetails) {
		typeDocument.clearContent();
		Element fsxml = typeDocument.addElement("fsxml");
		Element type = fsxml.addElement(indexSaveType);
		
		//add refer to original collection
		Element referCollectionPresentation = type.addElement("collectionrefer");
		referCollectionPresentation.addAttribute("id", "1");
		referCollectionPresentation.addAttribute("referid", (String) collectionDetails.get("collectionUri"));
		
		Element properties = type.addElement("properties");
		//add standard properties
		properties.addElement("collection").addText((String) collectionDetails.get("collectionUri"));

		//add user configured properties
		Map<String, String> items = userConfigMemory.get(domainid+":"+userid);
		
		for (String item : items.keySet()) {
			if (item.equals("collectiontitle") || item.equals("collectiondescription") || item.equals("collectionstatus")) {
				properties.addElement(item).addText((String) collectionDetails.get(item));
			}
		}

		long timestamp = new Date().getTime();
		type.addAttribute("id", String.valueOf(timestamp));
		
		// Add directly to fs so maggie get's updated with first content faster
		FSXMLRequestHandler.instance().saveFsXml(indexUri, typeDocument.asXML(), "PUT", true);
	}
	
	private void makeType(Element typeContent, Map<String, Object> collectionDetails) {
		// create new type
	    typeDocument.clearContent();
	    Element fsxml = typeDocument.addElement("fsxml");
		Element type = fsxml.addElement(indexSaveType);
		
		// add refer to original collection
 		Element referCollectionPresentation = type.addElement("collectionrefer");
		referCollectionPresentation.addAttribute("id", "1");
		referCollectionPresentation.addAttribute("referid", (String) collectionDetails.get("collectionUri"));
		
		 Element properties = type.addElement("properties");
		//add standard properties
		properties.addElement("collection").addText((String) collectionDetails.get("collectionUri"));
		 
		//add user configured properties
		Map<String, String> items = userConfigMemory.get(domainid+":"+userid);

		for (String item : items.keySet()) {
			if (item.equals("collectiontitle") || item.equals("collectiondescription") || item.equals("collectionstatus")) {
				properties.addElement(item).addText((String) collectionDetails.get(item));
			}
		}
		long timestamp = new Date().getTime();
		type.addAttribute("id", String.valueOf(timestamp));
		
		logger.debug("about to PUT to uri "+indexUri+" this xml: "+typeDocument.asXML());
		
		// Add directly to fs so maggie get's updated with first content faster
		FSXMLRequestHandler.instance().saveFsXml(indexUri, typeDocument.asXML(), "PUT", true);
	}
	
	// Get a sub part of the uri
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
	
	//Collection details
	private Map<String, Object> getCollectionDetails(String collectionUri) {
		Map<String, Object> collectionDetails = new HashMap<String, Object>();
		
		Document collection = null;
		collection = FSXMLRequestHandler.instance().getNodeProperties(collectionUri, 0, false);
		
		if (collection == null) {
			return null;
		}
		
		collectionDetails.put("collection", collection);
		collectionDetails.put("collectionUri", collectionUri);
		
		Map<String, String> items = userConfigMemory.get(domainid+":"+userid);

		for (String item : items.keySet()) {
			if (item.equals("collectiontitle")) {
				String collectionTitle = collection.selectSingleNode("//collection/properties/title") == null ? "" : collection.selectSingleNode("//collection/properties/title").getText();
				collectionDetails.put(item, collectionTitle);
			} else if (item.equals("collectiondescription")) {
				String collectionDesc = collection.selectSingleNode("//collection/properties/description") == null ? "" : collection.selectSingleNode("//collection/properties/description").getText();
				collectionDetails.put(item, collectionDesc);
			} else if (item.equals("collectionstatus")) {
				String collectionStatus = collection.selectSingleNode("//collection/properties/publicationstatus") == null ? "" : collection.selectSingleNode("//collection/properties/publicationstatus").getText();
				collectionDetails.put(item, collectionStatus);
			}
		}	
		return collectionDetails;
	}
	
	//Collection details
	private Map<String, Object> getCollectionDetails(Document collection, String uri) {
		Map<String, Object> collectionDetails = new HashMap<String, Object>();
		
		String collectionUri = uri +"/"+ collection.selectSingleNode("//collection/@id").getText() +"/";
		
		collectionDetails.put("collection", collection);
		collectionDetails.put("collectionUri", collectionUri);
		
		Map<String, String> items = userConfigMemory.get(domainid+":"+userid);
		
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
	
	// Delete the old collection in the index
	private void deleteTypes(String collectionUri) {
		logger.debug("deleting "+ indexSaveType +" with collectionUri "+collectionUri);
		// get index types refering to collection
		List<String> refers = FSXMLRequestHandler.instance().getReferParents(collectionUri);

		// Delete all refers that are of type index type in the current index & same domain
		for (Iterator<String> iter = refers.iterator(); iter.hasNext(); ) {
			String refer = iter.next();

			if(refer.startsWith("/")) {
				refer = refer.substring(1);
			}
			String[] parts = refer.split("/");
			
			if (parts[1].equals(domainid) && parts[3].equals(userid) && parts[4].equals("index") && parts[6].equals(indexSaveType)) {
				String cid = parts[7];
				//logger.debug("delete index type "+cid);
				logger.debug("deleting "+indexUri+"/"+indexSaveType+"/"+cid);
				FSXMLRequestHandler.instance().deleteNodeProperties(indexUri+"/"+indexSaveType+"/"+cid, true);
			}
		}
	}
	
	//Loop over type
	private void loopTypes(String collectionUri, Map<String, Object> collectionDetails) {
		logger.debug("loop types");
		
		Document collection = (Document) collectionDetails.get("collection");
		
		List<Node> types = collection.selectNodes("//"+indexType);
		logger.debug("number of "+indexType+" = "+types.size());
		
		if (types.isEmpty()) {
			makeTypeFromCollection(collectionDetails);		
		} 
		
		//loop over type
		for(Iterator<Node> iter = types.iterator(); iter.hasNext(); ) {
			Element type = (Element) iter.next();
			makeType(type, collectionDetails);
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				logger.error("",e);
			}
		}
	}
	
	// Create an empty index
	private void createIndex() {
		FSXMLRequestHandler.instance().handlePUT(indexUri+"/properties", "<fsxml><properties><lastupdate>"+String.valueOf(new Date().getTime())+"</lastupdate></properties></fsxml>");
	}
	
	// Get the number of collections for this uri
	private int getNumberOfCollections(String collectionUri) {
		Document collections = FSXMLRequestHandler.instance().getNodePropertiesByType(collectionUri, 0, 0, 1);
		Node resultsAvailable = collections.selectSingleNode("//properties/totalResultsAvailable");
		return Integer.parseInt(resultsAvailable.getText());
	}	
}
