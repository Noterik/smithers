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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * Action to create all rawvideos when original is ingested
 * 
 * @author Pieter van Leeuwen <p.vanleeuwen@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2017
 * @package com.noterik.bart.fs.action
 * @access private
 */

public class CreateRawsMarinAction extends ActionAdapter {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(CreateRawsMarinAction.class);

	@Override
	public String run() {
	    String uri = event.getUri();
	    String requestBody = event.getRequestData();
	    String mount = null, original = null, filename = null;
	    Document originalDoc = null;
	
	    Pattern videoPattern = Pattern.compile(".*/video/[^/]*/");
	    Matcher videoMatcher = videoPattern.matcher(uri);
	    String videouri = "";
	    
	    if (videoMatcher.find()) {
		videouri = videoMatcher.group(0);
		videouri = videouri.substring(0, videouri.length()-1);
		logger.debug("Videouri = "+videouri);		
	    } else {
		return null;
	    }
	    
	    String videoName = videouri.substring(videouri.lastIndexOf("/")+1);
	    logger.debug("video name = "+videoName);
	    
	    //Check parent video to retrieve all rawvideos to check if MP4 already exists
	    Document videoDocument = FSXMLRequestHandler.instance().getNodeProperties(videouri, false);

	    List<Node> rawvideos = videoDocument.selectNodes("//rawvideo");
	    Node originalRaw = null;
	    boolean mp4Found = false;
	    
	    for (Iterator<Node> iter = rawvideos.iterator(); iter.hasNext(); ) {
		Node rawvideo = iter.next();
	
		String orig = rawvideo.selectSingleNode("properties/original") == null ? "" : rawvideo.selectSingleNode("properties/original").getText();
		String extension = rawvideo.selectSingleNode("properties/extension") == null ? "" : rawvideo.selectSingleNode("properties/extension").getText();

		if (extension.toLowerCase().contains("mp4")) {
		    mp4Found = true;
		}
		
		if (orig.toLowerCase().equals("true")) {
		    logger.debug("Original found");
		    originalRaw = rawvideo;
		}
	    }
	   
	    if (!mp4Found && originalRaw != null) {
		Node mountNode = originalRaw.selectSingleNode("properties/mount");
		Node filenameNode = originalRaw.selectSingleNode("properties/filename");

		if (mountNode != null && mountNode.getText() != null) {
		    mount = mountNode.getText();
		    logger.debug("mount = "+mount);
		    
		    //temp disable mp4 processing for non communication (mediamix) items
		    //this due to issue with vsync and interlaced videos
		    /*if (!mount.toLowerCase().equals("communications")) {
			logger.debug("No mp4 yet for mediamix items");
			return null;
		    }*/
		    
		} else {
		    logger.debug("no mount found");
		}
					
		if (filenameNode != null && filenameNode.getText() != null) {
		    filename = filenameNode.getText();
		}
	    } else {
		logger.debug("MP4 found = "+mp4Found+" or no original found");
		return null;
	    }
	    
	    String newFilename = "";
	    
	    if (filename.lastIndexOf(".") > 0) {
		newFilename = filename.substring(0, filename.lastIndexOf("."))+".mp4";
	    } else {
		newFilename = filename + ".mp4";
	    }
	    
	    //Does not apply for Communications workspace
	    if (mount.equals("marin")) {
		//transcoded files should always end up in a delivery folder
		if (newFilename.indexOf("/Raw/") > 0) {
		    newFilename = newFilename.replace("/Raw/", "/Delivery/");
		}
		
		//transcoded files should always end up in a transcode folder
		if (newFilename.indexOf("/transcode/") == -1) {
		    String pre = newFilename.substring(0, newFilename.lastIndexOf("/"));
		    String post = newFilename.substring(newFilename.lastIndexOf("/")+1);
		    newFilename = pre + "/transcode/" + post;
		}
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
		String rawUri = uri.substring(0, uri.lastIndexOf("/")) +"/"+ videoName+"_transcode/properties";
			
		if (FSXMLRequestHandler.instance().getPropertyValue(rawUri+"/reencode") == null && mount != null) {
		    //if (!FSXMLRequestHandler.instance().hasProperties(rawUri) && mount != null) {			
		    createRawProperties(rawUri, mount, profiles.get(id), newFilename);
		}			
	    }
	    return null;
	}

	private void createRawProperties(String rawUri, String mount, EncodingProfile ep, String filename) {
	    String domainid = URIParser.getDomainFromUri(rawUri);
	    String userid = URIParser.getUserFromUri(rawUri);
	    String rawVideoId = URIParser.getRawvideoIdFromUri(rawUri);
	    // get the xml for the rawvideo with the encoding profile
	    String xml = getRawXml(ep, mount, filename);
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

	private String getRawXml(EncodingProfile ep, String mount, String filename) {
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
		if (filename != null && !filename.equals("")) {
		    xml.append("<filename>"+filename+"</filename>");
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
