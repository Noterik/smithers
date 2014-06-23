package com.noterik.bart.fs.action;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.fs.FSXMLParser;
import com.noterik.springfield.tools.fs.URIParser;
import com.noterik.springfield.tools.fs.model.config.ingest.EncodingProfile;
import com.noterik.springfield.tools.fs.model.config.ingest.IngestConfig;
import com.noterik.springfield.tools.fs.model.config.ingest.VideoProfile;

/**
 * Action to create all rawvideos when rawvideo/1 is ingested
 * 
 * @author Pieter van Leeuwen <p.vanleeuwen@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2010
 * @package com.noterik.bart.fs.action
 * @access private
 * @version $Id: CreateRawsAction.java,v 1.11 2011-09-06 09:45:47 konstantin Exp $
 */

public class CreateRawsAction extends ActionAdapter {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(CreateRawsAction.class);

	@Override
	public String run() {
		logger.debug("Starting to create all required raws");
		String uri = event.getUri();
		String requestBody = event.getRequestData();
		logger.debug("\n\nRequest Data: " + requestBody);
		String mount = null, original = null;
		Document originalDoc = null;
		
		try {
			if (requestBody != null) {
				originalDoc = DocumentHelper.parseText(requestBody);
				Node mountNode = originalDoc.selectSingleNode("//properties/mount");
				Node originalNode = originalDoc.selectSingleNode("//properties/original");
				Node transcoderNode = originalDoc.selectSingleNode("//properties/transcoder");
				if(transcoderNode != null && transcoderNode.getText() != null && transcoderNode.getText().equals("apu")){
					logger.debug("The video was already transcoded by apu, skipping raw2 creation");
					return null;
				}
				
				if (mountNode != null && mountNode.getText() != null) {
					mount = mountNode.getText();
					logger.debug("\n\n\n\n\nFOUND MOUNT: " + mount);
				} else {
					logger.debug("\n\n\n\n\nNO MOUNT FOUND IN PROPERTIES");
				}
				
				if (originalNode != null && originalNode.getText() != null) {
					original = originalNode.getText();
					logger.debug("\n\n\n\n\nFOUND ORIGINAL: " + original);
				} else {
					logger.debug("\n\n\n\n\nNO ORIGINAL FOUND IN PROPERTIES, WHICH MEANS APU UPLOAD, CREATE ORIGINAL ANYWAYS ;)");
					createOriginalTagAfterApuUpload();
					return null;
				}
			}
		} catch (DocumentException e) {
			logger.error("",e);
		}
		
		//get config and number of raws to work on
		Map<String, EncodingProfile> profiles = getConfigs(uri);
		String[] ids = new String[profiles.size()];
		logger.debug("number of profiles = "+profiles.size());
		int h = 0; 
		
		for(Iterator i = profiles.keySet().iterator();i.hasNext();) {
			ids[h] = (String) i.next();
			h++;
		}
		//sorting using natural order!! so 11 < 2
		Arrays.sort(ids);
		
		for (int j = 0; j < ids.length; j++) {		
			String id = ids[j];
			logger.debug("found video config "+id);			
			String rawUri = uri.substring(0, uri.lastIndexOf("/")) +"/"+ id+"/properties";
			
			if (FSXMLRequestHandler.instance().getPropertyValue(rawUri+"/reencode") == null && mount != null) {
			//if (!FSXMLRequestHandler.instance().hasProperties(rawUri) && mount != null) {			
				createRawProperties(rawUri, mount, profiles.get(id));
			}			
		}
		return null;
	}

	private void createRawProperties(String rawUri, String mount, EncodingProfile ep) {
		String domainid = URIParser.getDomainFromUri(rawUri);
		String userid = URIParser.getUserFromUri(rawUri);
		String rawVideoId = URIParser.getRawvideoIdFromUri(rawUri);
		// get the xml for the rawvideo with the encoding profile
		String xml = getRawXml(ep, mount);
		// set the xml to the rawvideo
		String response = FSXMLRequestHandler.instance().handlePUT(rawUri, xml.toString());
		logger.debug(response);
		//Get sponsor from encoding profile
		String videoUri = getSubUri(rawUri,6);
		String sponsor = "", presentationURI = "", sponsorURI = "";
		sponsor = ep.getSponsor();
		
		List<String> refers = FSXMLRequestHandler.instance().getReferParents(videoUri);
		int timeout = 0;
		while(refers.size()==0) {
			refers = FSXMLRequestHandler.instance().getReferParents(videoUri);
			try {
				Thread.currentThread().sleep(500);
			} catch (InterruptedException e) {
				logger.error("",e);
			}
			timeout++;
			if(timeout==5) break;
		}
		if(refers.size()>0) {
			for (int i = 0; i < refers.size(); i++) {
				if (refers.get(i).contains("/presentation/")) {
					presentationURI = refers.get(i);
					break;
				}
			}
			
			presentationURI = getSubUri(presentationURI,6);
			logger.debug("Presentation URI: " + presentationURI);
			sponsorURI = presentationURI + "/properties/sponsor";
			String sponsor_presentation = FSXMLRequestHandler.instance().getPropertyValue(sponsorURI);
			if(sponsor_presentation != null && !sponsor_presentation.equals("")) sponsor = sponsor_presentation;
		}
		
		logger.debug("sponsor = "+sponsor);
		logger.debug("rawvideoId = "+rawVideoId);
		
		//Create the sponsor reference only for the first rawvideo being created (in this case 2)
		if(sponsor != null && !sponsor.equals("") && rawVideoId.equals("2")) { //There is a sponsor set for this user
			//Check if the sponsor user exists
			Boolean exists = FSXMLRequestHandler.instance().hasProperties(sponsor);
			logger.debug("exists ? "+exists.toString());
			if(exists) {
				//Sponsor exists so just add a link to this video
				String ref_videoUri = videoUri;
				logger.debug(ref_videoUri);
				
				StringBuffer sponsorVideo = new StringBuffer("<fsxml><attributes>");
				sponsorVideo.append("<referid>"+ref_videoUri+"</referid>");						  
				sponsorVideo.append("</attributes></fsxml>");
				Boolean hasVideos = FSXMLRequestHandler.instance().hasProperties(sponsor + "/sponsor/1");
				if(!hasVideos) {
					//Create the <sponsor> tag for the user
					response = FSXMLRequestHandler.instance().handlePUT(sponsor + "/sponsor/1/properties", "<fsxml><properties/></fsxml>");
					logger.debug("User: " + sponsor + " does not have sponsoring videos - create the sponsor tag");
				}
				//Create the reference to the video in the sponsor
				response = FSXMLRequestHandler.instance().handlePOST(sponsor + "/sponsor/1/video", sponsorVideo.toString());
				logger.debug(response);
			}
		}
		
		logger.debug(response);
	}
	
	private Map<String, EncodingProfile> getConfigs(String uri) {
		// get the ingest config
		String domainid = URIParser.getDomainFromUri(uri);
		String userid = URIParser.getUserFromUri(uri);
		Document doc;
		
		// Check for ingest configuration in this order: collection, user, domain, sn domain (for historical reasons)
		String videoUri = getSubUri(uri,6);
		// Get the correct collection
		String collectionUri = getCollection(videoUri, userid);
		
		Document collectionConfig = FSXMLRequestHandler.instance().getNodeProperties(collectionUri+"config/ingest", false);
		if (collectionConfig != null) {
			doc = collectionConfig;
			logger.debug("use collection config "+collectionUri+"config/ingest");
		} else {		
			Document userConfig = FSXMLRequestHandler.instance().getNodeProperties("/domain/"+domainid+"/user/"+userid+"/config/ingest", false);
			if (userConfig != null) {
				doc = userConfig;
				logger.debug("use user config /domain/"+domainid+"/user/"+userid+"/config/ingest");
			} else {
				Document domainConfig = FSXMLRequestHandler.instance().getNodeProperties("/domain/"+domainid+"/config/ingest", false);
				if (domainConfig != null) {
					doc = domainConfig;
					logger.debug("use domain config /domain/"+domainid+"/config/ingest");
				} else {		
					doc = FSXMLRequestHandler.instance().getNodeProperties("/domain/sn/config/ingest", false);
					logger.debug("use sn config");
				}
			}
		}
		
		HashMap<String, EncodingProfile> profiles = new HashMap<String, EncodingProfile>();
		
		if (doc != null) {
			String confXml = doc.asXML();
			IngestConfig ic = FSXMLParser.getIngestConfigFromXml(confXml);
			
			for(Iterator i = ic.getVideoSettings().getRawVideos().keySet().iterator();i.hasNext();) {
				String next = (String) i.next();
				EncodingProfile ep = ic.getVideoSettings().getRawVideos().get(next).getEncodingProfile();
				
				profiles.put(next, ep);				
			}
		}		
		return profiles;
	}
	
	private void createOriginalTagAfterApuUpload() {
		String eventURI = event.getUri();
		if(eventURI.contains("/rawvideo/1")) {
			FSXMLRequestHandler.instance().updateProperty(eventURI + "/properties/original", "original", "true", "PUT", true);
		} else {
			logger.debug("you shouldn't see me");
		}
	}

	private String getRawXml(EncodingProfile ep, String mount) {
		StringBuffer xml = new StringBuffer("<fsxml><properties>");
		xml.append("<reencode>true</reencode>");
		xml.append("<mount>"+mount+"</mount>");
		xml.append("<format>" + ep.getFormat() + "</format>");
		xml.append("<extension>" + ep.getExtension() + "</extension>");
		xml.append("<wantedwidth>" + ep.getWidth() + "</wantedwidth>");
		xml.append("<wantedheight>" + ep.getHeight() + "</wantedheight>");
		xml.append("<wantedbitrate>" + ep.getBitRate() + "</wantedbitrate>");
		xml.append("<wantedframerate>" + ep.getFrameRate() + "</wantedframerate>");
		xml.append("<wantedkeyframerate>" + ep.getKeyFrameRate() + "</wantedkeyframerate>");
		xml.append("<wantedaudiobitrate>" + ep.getAudioBitRate() + "</wantedaudiobitrate>");
		if (ep.getBatchFile() != null) {
			xml.append("<batchfile>"+ep.getBatchFile()+"</batchfile>");
		}
		xml.append("</properties></fsxml>");
		return xml.toString();
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
	
	/* Get collection for video */
	private String getCollection(String vUri, String userid) {
		List<String> refers = FSXMLRequestHandler.instance().getReferParents(vUri);
		String refer = "";
		
		logger.debug("look for refer from "+vUri);
		
		for (Iterator<String> iter = refers.iterator(); iter.hasNext(); ) {
			refer = iter.next();			
			logger.debug("refer = "+refer);
			
			//Get refer from same user and make sure it's a collection
			if(refer.startsWith("/")) {
				refer = refer.substring(1);
			}
			if(refer.endsWith("/")) {
				refer = refer.substring(0,refer.length()-1);
			}			
			String[] parts = refer.split("/");
			
			if (parts[3].equals(userid) && parts[4].equals("collection")) {
				return getSubUri(refer, 6)+"/";
			}
		}
		return "";
	}	
}
