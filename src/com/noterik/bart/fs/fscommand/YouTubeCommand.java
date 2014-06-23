package com.noterik.bart.fs.fscommand;

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.XMLHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;
import com.noterik.springfield.tools.ftp.FtpHelper;

import com.google.gdata.data.youtube.*;
import com.google.gdata.data.media.*;
import com.google.gdata.data.media.mediarss.*;
import com.google.gdata.client.http.AuthSubUtil;
import com.google.gdata.client.youtube.YouTubeService;

public class YouTubeCommand implements Command {
	/** Logger */
	private static Logger logger = Logger.getLogger(YouTubeCommand.class);
	private static int MAX_FILESIZE = 1073741824;
	
	
	private String uri;
	private String xml;
	private String domainid;
	private String youtubeToken;
	
	public String execute(String url, String xmlContent) {	
		logger.debug("entering Youtube command");
		uri = url;
		xml = xmlContent;	
		
		domainid = URIParser.getDomainFromUri(uri);		
		return handlePresentation();
	}
	
	private String handlePresentation() {
		Document presentation = FSXMLRequestHandler.instance().getNodeProperties(uri,false);
		List<Node> videos = presentation.selectNodes("//video");
		
		String presentationTitle = presentation.selectSingleNode("//presentation/properties/title") == null ? "" :presentation.selectSingleNode("//presentation/properties/title").getText();
		String presentationDescription = presentation.selectSingleNode("//presentation/properties/description") == null ? "" : presentation.selectSingleNode("//presentation/properties/description").getText();

		logger.debug("title = "+presentationTitle+" description = "+presentationDescription);
		
		Element video;
		String response = "";
		
		// Loop all videos
		for(Iterator<Node> iter = videos.iterator(); iter.hasNext(); ) {
			video = (Element) iter.next();
			response = handleVideo(video, presentationTitle, presentationDescription);
		}
		return response;
	}
	
	public String handleVideo(Element v, String title, String description) {
		String refer = v.attributeValue("referid");
		Document video = FSXMLRequestHandler.instance().getNodeProperties(refer, false);
		Document ingestConfig = FSXMLRequestHandler.instance().getNodeProperties("/domain/"+domainid+"/config/ingest/setting/video",false);

		logger.debug("video = "+video.asXML());
		
		Node raw2 = video.selectSingleNode("//rawvideo[@id='2']/properties");
		
		if (raw2 == null) {
			//raw not yet created
			logger.debug("raw2 missing");
			return FSXMLBuilder.getErrorMessage("500","Source video not available", "Raw video 2 was not found", "http://teamelements.noterik.com/team");
		}
		
		String status = raw2.selectSingleNode("//status") == null ? "" : raw2.selectSingleNode("//status").getText();
		String mounts = raw2.selectSingleNode("//mount") == null ? "" : raw2.selectSingleNode("//mount").getText();
		Double duration = raw2.selectSingleNode("//duration") == null ? 0.0 : Double.parseDouble(raw2.selectSingleNode("//duration").getText());
		int filesize = raw2.selectSingleNode("//filesize") == null ? 0 : Integer.parseInt(raw2.selectSingleNode("//filesize").getText());		
		
		//not finished with transcoding
		if (!status.equals("done")) {
			logger.debug("raw2 not finished");
			return FSXMLBuilder.getErrorMessage("500","Source video not ready", "Raw video 2 was not finished", "http://teamelements.noterik.com/team");
		}		
		//too long
		if (duration > 900.0) {
			logger.debug("raw2 duration too long");
			return FSXMLBuilder.getErrorMessage("500","Source video duration too long", "Use a video with a maximum of 15 minutes", "http://teamelements.noterik.com/team");
		}
		//too big
		if (filesize > MAX_FILESIZE) {
			logger.debug("raw2 filesize too big:"+filesize);
			return FSXMLBuilder.getErrorMessage("500","Source video filesize too big", "Use a smaller video", "http://teamelements.noterik.com/team");
		}
		//no mount set
		if (mounts.equals("")) {
			logger.debug("raw2 no mount set");
			return FSXMLBuilder.getErrorMessage("500","Source video mount not available", "Raw video 2 has no mount set", "http://teamelements.noterik.com/team");
		}
		
		Document doc = XMLHelper.asDocument("<?xml version='1.0' encoding='UTF-8' standalone='no'?>"+xml);
		
		if(doc == null){
			logger.debug("could not make xml doc");
			return null;
		} else {
			youtubeToken = doc.selectSingleNode("//properties/token") == null ? "" : doc.selectSingleNode("//properties/token").getText();
		}
		
		logger.debug("youtube token = "+youtubeToken);
		
		String[] m = mounts.split(",");
		String mount = m[0];
		long timestamp = new Date().getTime();		
		String dir = "/tmp/"+String.valueOf(timestamp);
		boolean dirCreated = (new File(dir)).mkdir();
		if (dirCreated) {
			//get raw video 2 through ftp
			Boolean downloadedFile = FtpHelper.getFileWithFtp(refer+"/rawvideo/2/raw.mp4", dir, mount+".noterik.com", mount, mount);
		
			logger.debug("file downloaded from mount? "+downloadedFile);
		
			if (downloadedFile) {
				return uploadToYoutube(dir+"/raw.mp4", title, description);
			} else {
				return FSXMLBuilder.getErrorMessage("500","Video could not be downloaded", "Video could not be downloaded from filesystem", "http://teamelements.noterik.com/team");
			}
		} else {
			return FSXMLBuilder.getErrorMessage("500","Could not create tmp dir", "Could not create tmp dir", "http://teamelements.noterik.com/team");
		}
	}
	
	private String uploadToYoutube(String filename, String title, String description) {
		YouTubeService service = new YouTubeService("", "AI39si5ylY7YCbAylvTBLsmC5CxuNixlKBc6dFPlPFAmBx-8659OAbMFo8JCXwBm_eYYJo8wMbT28mxHdkfFGVPHjAX7fRTQEg");
		//String sessionToken = "";
		
		service.setUserToken(youtubeToken);
		
		/*try {
			sessionToken = AuthSubUtil.exchangeForSessionToken(youtubeToken, null);		
		} catch (Exception e) {
			return FSXMLBuilder.getErrorMessage("500","Error in youtube session token", e.toString(), "http://teamelements.noterik.com/team");
		}
		logger.debug("session token = "+sessionToken);
		
		service.setAuthSubToken(sessionToken,null);*/
		
		VideoEntry newEntry = new VideoEntry();
		
		YouTubeMediaGroup mg = newEntry.getOrCreateMediaGroup();
		mg.setTitle(new MediaTitle());
		mg.getTitle().setPlainTextContent(title);
		mg.addCategory(new MediaCategory(YouTubeNamespace.CATEGORY_SCHEME, "Tech"));
		/*mg.setKeywords(new MediaKeywords());
		mg.getKeywords().addKeyword("cars");
		mg.getKeywords().addKeyword("funny");*/
		mg.setDescription(new MediaDescription());
		mg.getDescription().setPlainTextContent(description);
		mg.setPrivate(false);
		mg.addCategory(new MediaCategory(YouTubeNamespace.DEVELOPER_TAG_SCHEME, "noterik"));
		
		MediaFileSource ms = new MediaFileSource(new File(filename), "video/mp4");
		newEntry.setMediaSource(ms);

		String uploadUrl = "http://uploads.gdata.youtube.com/feeds/api/users/default/uploads";

		VideoEntry createdEntry;
		
		try {
			createdEntry = service.insert(new URL(uploadUrl), newEntry);
		} catch (Exception e) {
			return FSXMLBuilder.getErrorMessage("500","Error in youtube upload", e.toString(), "http://teamelements.noterik.com/team");
		}
		
		//Successful upload
		FSXMLRequestHandler.instance().updateProperty(uri+"/properties/export", "export", "youtube", "PUT", false);

		//Put together status page
		String profileUrl = "http://gdata.youtube.com/feeds/api/users/default";
		String username = "";
		try {
			UserProfileEntry profileEntry = service.getEntry(new URL(profileUrl), UserProfileEntry.class);
			username = profileEntry.getUsername();
		} catch (Exception e) {	}

		String videoId = createdEntry.getId();
		videoId = videoId.substring(videoId.indexOf("video:")+6);
		logger.debug("video id "+videoId);
		String statusUri = "http://gdata.youtube.com/feeds/api/users/"+username+"/uploads/"+videoId;
		
		HashMap<String, String> properties = new HashMap<String,String>();
		properties.put("status", "uploaded");
		properties.put("url",statusUri);
		
		return FSXMLBuilder.wrapInFsxml("", properties);
	}
	
	public ManualEntry man() {
		return null;
	}
}
