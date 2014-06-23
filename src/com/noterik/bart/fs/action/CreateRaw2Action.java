package com.noterik.bart.fs.action;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.action.CreateRaw2Action;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.fs.FSXMLParser;
import com.noterik.springfield.tools.fs.URIParser;
import com.noterik.springfield.tools.fs.model.config.ingest.EncodingProfile;
import com.noterik.springfield.tools.fs.model.config.ingest.IngestConfig;

/**
 * Action that adds the rawvideo/2 when the rawvideo/1 is ingested
 * 
 * @author Jaap Blom <j.blom@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.action
 * @access private
 * @version $Id: CreateRaw2Action.java,v 1.11 2011-09-06 06:38:33 konstantin Exp $
 * 
 */
public class CreateRaw2Action extends ActionAdapter {

	private static Logger logger = Logger.getLogger(CreateRaw2Action.class);

	@Override
	public String run() {
		logger.debug("\n\n ######### starting create momar raw action ######\n\n");
		String requestBody = event.getRequestData();
		logger.debug("\n\nRequest Data: " + requestBody);
		String mount = null, original = null, batchFile = null;
		Document doc = null;
		
		try {
			if (requestBody != null) {
				doc = DocumentHelper.parseText(requestBody);
				Node node1 = doc.selectSingleNode("//properties/mount");
				Node node2 = doc.selectSingleNode("//properties/original");
				Node node3 = doc.selectSingleNode("//properties/transcoder");
				Node node4 = doc.selectSingleNode("//properties/batchfile");
				if(node3 != null && node3.getText() != null && node3.getText().equals("apu")){
					logger.debug("The video was already transcoded by apu, skipping raw2 creation");
					return null;
				}
				if (node4 != null && node4.getText() != null) {
					//encoding using specified batchfile
					batchFile = node4.getText();
				}
				
				//Node useMomar = doc.selectSingleNode("//properties/momar");
				
				//if(useMomar != null && useMomar.getText().toLowerCase().equals("false")){
				//	logger.debug("\n\n Momar tag is false ! Will not create Momar Job !!\n");
				//	return null;
				//}
				
				if (node1 != null) {
					mount = node1.getText();
					logger.debug("\n\n\n\n\nFOUND MOUNT: " + mount);
				} else {
					logger.debug("\n\n\n\n\nNO MOUNT FOUND IN PROPERTIES");
				}
				
				if (node2 != null) {
					original = node2.getText();
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
		String uri = event.getUri();
		logger.debug("CREATE RAW 2 ACTION:\nURI: " + uri);
		String raw2Uri = uri.substring(0, uri.lastIndexOf("/")) + "/2";
		if (!FSXMLRequestHandler.instance().hasProperties(raw2Uri) && mount != null) {			
			createRaw2Properties(raw2Uri, mount, batchFile);
		}
		return null;
	}

	private void createRaw2Properties(String raw2Uri, String mount, String batchFile) {
		// get the ingest config
		String domainid = URIParser.getDomainFromUri(raw2Uri);
		String userid = URIParser.getUserFromUri(raw2Uri);
		String presentationURI = "";
		String sponsorURI = "";
		String sponsor = "";
		Document doc;
		Boolean hasSponsor = false;
		
		// Check for ingest configuration in this order: collection, user, domain, sn domain (for historical reasons)
		String videoUri = getSubUri(raw2Uri,6);
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

		if (doc != null) {
			String confXml = doc.asXML();
			IngestConfig ic = FSXMLParser.getIngestConfigFromXml(confXml);
			// get encoding profile #2 from the config --> raw index = 2
			EncodingProfile ep = ic.getVideoSettings().getRawVideos().get("2").getEncodingProfile();
			if (batchFile != null) {
				ep.setBatchFile(batchFile);
			}
			// get the xml for the rawvideo #2 with the encoding profile
			String xml = getRaw2Xml(ep, mount);
			// set the xml to the rawvideo 2
			String response = FSXMLRequestHandler.instance().handlePUT(raw2Uri + "/properties", xml.toString());
			logger.debug(response);
			
			//Get sponsor from encoding profile
			sponsor = ep.getSponsor();
			
		} else {
			logger.debug("No ingest config defined for "+domainid);
		}
		
		// Check for sponsor in the presentation (it has highest priority)
		//logger.debug("video refer uri = "+videoUri + "?method=POST&mimetype=application/fscommand&id=showrefs");
		
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
		
		if(sponsor != null && !sponsor.equals("")) { //There is a sponsor set for this user
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
				String response = "";
				if(!hasVideos) {
					//Create the <sponsor> tag for the user
					response = FSXMLRequestHandler.instance().handlePUT(sponsor + "/sponsor/1/properties", "<fsxml><properties/></fsxml>");
					logger.debug("User: " + sponsor +" does not have sponsoring videos - create the sponsor tag");
				}
				//Create the reference to the video in the sponsor
				response = FSXMLRequestHandler.instance().handlePOST(sponsor + "/sponsor/1/video", sponsorVideo.toString());
				logger.debug(response);
			}
		}
	}
	
	private void createOriginalTagAfterApuUpload() {
		String eventURI = event.getUri();
		if(eventURI.contains("/rawvideo/1")) {
			FSXMLRequestHandler.instance().updateProperty(eventURI + "/properties/original", "original", "true", "PUT", true);
		} else {
			logger.debug("you shouldn't see me");
		}
	}

	private String getRaw2Xml(EncodingProfile ep, String mount) {
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