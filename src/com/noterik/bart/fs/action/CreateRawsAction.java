/* 
* CreateRawsAction.java
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
 * Action to create all rawitems when raw<item>/1 is ingested
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
		String mount = null, original = null, filename = null;
		Document originalDoc = null;
		
		try {
			if (requestBody != null) {
				originalDoc = DocumentHelper.parseText(requestBody);
				Node mountNode = originalDoc.selectSingleNode("//properties/mount");
				Node originalNode = originalDoc.selectSingleNode("//properties/original");
				Node filenameNode = originalDoc.selectSingleNode("//properties/filename");
				Node transcoderNode = originalDoc.selectSingleNode("//properties/transcoder");
				if(transcoderNode != null && transcoderNode.getText() != null && transcoderNode.getText().equals("apu")){
					logger.debug("The item was already transcoded by apu, skipping raw2 creation");
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
				
				if (filenameNode != null && filenameNode.getText() != null) {
					filename = filenameNode.getText();
				}
			}
		} catch (DocumentException e) {
			logger.error("",e);
		}
		
		String type = "video";
		if (uri.indexOf("/audio/") > -1) {
		    type = "audio";
		}
		
		//get config and number of raws to work on
		Map<String, EncodingProfile> profiles = getConfigs(uri, type);
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
			logger.debug("found item config "+id);			
			String rawUri = uri.substring(0, uri.lastIndexOf("/")) +"/"+ id+"/properties";
			
			if (FSXMLRequestHandler.instance().getPropertyValue(rawUri+"/reencode") == null && mount != null) {
			//if (!FSXMLRequestHandler.instance().hasProperties(rawUri) && mount != null) {			
				createRawProperties(rawUri, mount, profiles.get(id), filename, type);
			}			
		}
		return null;
	}

	private void createRawProperties(String rawUri, String mount, EncodingProfile ep, String filename, String type) {
		String domainid = URIParser.getDomainFromUri(rawUri);
		String userid = URIParser.getUserFromUri(rawUri);
		String rawItemId = URIParser.getRawvideoIdFromUri(rawUri);
		if (type.equals("audio")) {
		    rawItemId = URIParser.getRawaudioIdFromUri(rawUri);
		}
		
		// get the xml for the raw item with the encoding profile
		String xml = getRawXml(ep, mount, filename);
		// set the xml to the raw item
		String response = FSXMLRequestHandler.instance().handlePUT(rawUri, xml.toString());
		logger.debug(response);
		//Get sponsor from encoding profile
		String itemUri = getSubUri(rawUri,6);
		String sponsor = "", presentationURI = "", sponsorURI = "";
		sponsor = ep.getSponsor();
		
		List<String> refers = FSXMLRequestHandler.instance().getReferParents(itemUri);
		int timeout = 0;
		while(refers.size()==0) {
			refers = FSXMLRequestHandler.instance().getReferParents(itemUri);
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
		logger.debug("raw item Id = "+rawItemId);
		
		//Create the sponsor reference only for the first raw item being created (in this case 2)
		if(sponsor != null && !sponsor.equals("") && rawItemId.equals("2")) { //There is a sponsor set for this user
			//Check if the sponsor user exists
			Boolean exists = FSXMLRequestHandler.instance().hasProperties(sponsor);
			logger.debug("exists ? "+exists.toString());
			if(exists) {
				//Sponsor exists so just add a link to this item
				String ref_itemUri = itemUri;
				logger.debug(ref_itemUri);
				
				StringBuffer sponsorItem = new StringBuffer("<fsxml><attributes>");
				sponsorItem.append("<referid>"+ref_itemUri+"</referid>");						  
				sponsorItem.append("</attributes></fsxml>");
				Boolean hasItems = FSXMLRequestHandler.instance().hasProperties(sponsor + "/sponsor/1");
				if(!hasItems) {
					//Create the <sponsor> tag for the user
					response = FSXMLRequestHandler.instance().handlePUT(sponsor + "/sponsor/1/properties", "<fsxml><properties/></fsxml>");
					logger.debug("User: " + sponsor + " does not have sponsoring items - create the sponsor tag");
				}
				//Create the reference to the item in the sponsor
				response = FSXMLRequestHandler.instance().handlePOST(sponsor + "/sponsor/1/"+type, sponsorItem.toString());
				logger.debug(response);
			}
		}
		
		logger.debug(response);
	}
	
	private Map<String, EncodingProfile> getConfigs(String uri, String type) {
		// get the ingest config
		String domainid = URIParser.getDomainFromUri(uri);
		String userid = URIParser.getUserFromUri(uri);
		Document doc;
		
		// Check for ingest configuration in this order: collection, user, domain, sn domain (for historical reasons)
		String itemUri = getSubUri(uri,6);
		// Get the correct collection
		String collectionUri = getCollection(itemUri, userid);
		
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
			
			if (type.equals("audio")) {			    
			    for(Iterator i = ic.getAudioSettings().getRawAudios().keySet().iterator();i.hasNext();) {
        			String next = (String) i.next();
        			EncodingProfile ep = ic.getAudioSettings().getRawAudios().get(next).getEncodingProfile();
        			
        			profiles.put(next, ep);				
			    }			    
			} else {			
			    for(Iterator i = ic.getVideoSettings().getRawVideos().keySet().iterator();i.hasNext();) {
        			String next = (String) i.next();
        			EncodingProfile ep = ic.getVideoSettings().getRawVideos().get(next).getEncodingProfile();
        			
        			profiles.put(next, ep);				
			    }
			}
		}		
		return profiles;
	}
	
	private void createOriginalTagAfterApuUpload() {
		String eventURI = event.getUri();
		if(eventURI.contains("/rawvideo/1") || eventURI.contains("/rawaudio/1")) {
			FSXMLRequestHandler.instance().updateProperty(eventURI + "/properties/original", "original", "true", "PUT", true);
		} else {
			logger.debug("you shouldn't see me");
		}
	}

	private String getRawXml(EncodingProfile ep, String mount, String filename) {
		StringBuffer xml = new StringBuffer("<fsxml><properties>");
		xml.append("<reencode>true</reencode>");
		xml.append("<mount>"+mount+"</mount>");
		xml.append("<format>" + ep.getFormat() + "</format>");
		xml.append("<extension>" + ep.getExtension() + "</extension>");
		xml.append("<wantedbitrate>" + ep.getBitRate() + "</wantedbitrate>");
		if (ep.getWidth() != null) {
		    xml.append("<wantedwidth>" + ep.getWidth() + "</wantedwidth>");
		}
		if (ep.getHeight() != null) {
		    xml.append("<wantedheight>" + ep.getHeight() + "</wantedheight>");
		}		
		if (ep.getFrameRate() != null) {
		    xml.append("<wantedframerate>" + ep.getFrameRate() + "</wantedframerate>");
		}
		if (ep.getKeyFrameRate() != null) {
		    xml.append("<wantedkeyframerate>" + ep.getKeyFrameRate() + "</wantedkeyframerate>");
		}
		if (ep.getAudioBitRate() != null) {
		    xml.append("<wantedaudiobitrate>" + ep.getAudioBitRate() + "</wantedaudiobitrate>");
		}
		if (ep.getBatchFile() != null) {
			xml.append("<batchfile>"+ep.getBatchFile()+"</batchfile>");
		}
		if (ep.getFilename() != null) {
			if (ep.getFilename().equals("original")) {
			    	if (filename.indexOf(".") > -1) {
			    	    //replace extension
			    	    filename = filename.substring(0, filename.lastIndexOf("."));
			    	}
			    	filename = filename +"."+ep.getExtension();
			    	
				xml.append("<filename>"+filename+"</filename>");
			} else {
				xml.append("<filename>"+ep.getFilename()+"</filename>");
			}
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
	
	/* Get collection for item */
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
