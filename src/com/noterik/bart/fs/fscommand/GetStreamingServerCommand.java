package com.noterik.bart.fs.fscommand;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.fs.FSXMLBuilder;

public class GetStreamingServerCommand implements Command {
	
	private static final String COLLECTION_PRESENTATION = "/domain/[^/]+/user/[^/]+/collection/[^/]+/presentation/.*";
	private static final String PRESENTATION = "/domain/[^/]+/user/[^/]+/presentation/.*";
	private static final String VIDEO = "/domain/[^/]+/user/[^/]+/video/.*";
	
	/** The ShiftCommand's log4j Logger */
	private static Logger logger = Logger.getLogger(GetStreamingServerCommand.class);
	
	private String rawId = null;
	
	public String execute(String uri, String xml) {
		logger.debug("get streaming server command");
		
		//get raw id from xml
		try {
			Document properties = DocumentHelper.parseText(xml);			
			rawId = properties.selectSingleNode("//properties/raw") == null ? "1" : properties.selectSingleNode("//properties/raw").getText();
		} catch (DocumentException e) {
			logger.error("could not parse streaming server command xml :"+xml);
		}		
		
		Pattern collectionPresentationPattern = Pattern.compile(COLLECTION_PRESENTATION);
		Matcher collectionPresentationMatcher = collectionPresentationPattern.matcher(uri);
		
		if (collectionPresentationMatcher.matches()) {
			logger.debug("collectionpresentation "+uri);
			//get refer presentation
			Document doc = FSXMLRequestHandler.instance().getNodeProperties(uri, false);
			uri = doc.selectSingleNode("//presentation/@referid") == null ? "" : doc.selectSingleNode("//presentation/@referid").getText();
		}
		
		Pattern presentationPattern = Pattern.compile(PRESENTATION);
		Matcher presentationMatcher = presentationPattern.matcher(uri);
		
		if (presentationMatcher.matches()) {
			logger.debug("presentation "+uri);
			//get refer video
			Document doc = FSXMLRequestHandler.instance().getNodeProperties(uri, false);
			uri = doc.selectSingleNode("//presentation/videoplaylist/video/@referid") == null ? "" : doc.selectSingleNode("//presentation/videoplaylist/video/@referid").getText();
		}
		
		Pattern videoPattern = Pattern.compile(VIDEO);
		Matcher videoMatcher = videoPattern.matcher(uri);
		
		if (videoMatcher.matches()) {
			logger.debug("video "+uri);
			Document doc = FSXMLRequestHandler.instance().getNodeProperties(uri, false);
			String mount = doc.selectSingleNode("//video/rawvideo[@id='"+rawId+"']/properties/mount") == null ? "" : doc.selectSingleNode("//video/rawvideo[@id='"+rawId+"']/properties/mount").getText();
			String extension = doc.selectSingleNode("//video/rawvideo[@id='"+rawId+"']/properties/extension") == null ? "" : doc.selectSingleNode("//video/rawvideo[@id='"+rawId+"']/properties/extension").getText();
			String status = doc.selectSingleNode("//video/rawvideo[@id='"+rawId+"']/properties/status") == null ? "" : doc.selectSingleNode("//video/rawvideo[@id='"+rawId+"']/properties/status").getText();
			String original = doc.selectSingleNode("//video/rawvideo[@id='"+rawId+"']/properties/original") == null ? "" : doc.selectSingleNode("//video/rawvideo[@id='"+rawId+"']/properties/original").getText();
			
			logger.debug("mount "+mount);
			logger.debug("extension "+extension);
			logger.debug("status "+status);
			logger.debug("original "+original);
			
			if (mount != "" && (status.toUpperCase().equals("DONE") || original.toUpperCase().equals("TRUE"))) {
				StringBuffer output = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
				output.append("<fsxml><properties/>");
				
				String[] mounts = mount.split(",");
				logger.debug("number of mounts "+mounts.length);
				
				for (int i = 0; i < mounts.length; i++) {
					String streamingServer = mounts[i]+".noterik.com/"+mounts[i];
					String file = uri+"/rawvideo/"+rawId+"/raw."+extension;
					output.append(serverToXml(streamingServer, i+1, file));					
				}				
				output.append("</fsxml>");
				return output.toString();
			} else {
				return FSXMLBuilder.getErrorMessage("404", "Video not found", "The specified video was not found", "http://teamelements.noterik.com/team");
			}
		}		
		logger.error("Could not convert uri to video "+uri);
		return FSXMLBuilder.getErrorMessage("500", "Could not get stream for this id", "Please try with a correct id", "http://teamelements.noterik.com/team");
	}
	
	private String serverToXml(String server, int id, String file) {
		StringBuffer output = new StringBuffer();
		output.append("<server id=\""+id+"\">");
		output.append("<properties>");
		output.append("<streamingserver>"+server+"</streamingserver>");
		output.append("<file>"+file+"</file>");
		output.append("</properties>");
		output.append("</server>");	
		
		return output.toString();
	}	
	
	public ManualEntry man() {
		return null;
	}
}
