package com.noterik.bart.fs.fscommand.dynamic.presentation.playout;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.fscommand.DynamicCommand;
import com.noterik.bart.fs.fscommand.DynamicCommandHandler;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

public class html5 implements DynamicCommand {
	/** Logger */
	private static Logger logger = Logger.getLogger(html5.class);
	
	Document returnXml = DocumentHelper.createDocument();
	Element fsxml = returnXml.addElement("fsxml");
	String domain = "";
	String user = "";
	String collection = "";
		
	public String run(String uri,String xml) {	
		fsxml.addElement("properties");
			
		domain = URIParser.getDomainFromUri(uri);			
		user = URIParser.getUserFromUri(uri);
		if (uri.indexOf("/collection/") == -1 || uri.indexOf("/presentation") == -1) {
			return FSXMLBuilder.getErrorMessage("403", "No collection presentation found",
					"You have to suppy a valid collection presentation", "http://teamelements.noterik.nl/team");
		}
		
		Document presentationProperties = FSXMLRequestHandler.instance().getNodeProperties(uri, false);
		if (presentationProperties == null) {
			return FSXMLBuilder.getErrorMessage("404", "Presentation not found",
					"You have to suppy an existing presentation", "http://teamelements.noterik.nl/team");
		}
		
		Node presentationXml = getPresentation(uri);			
		if (presentationXml != null) {
			fsxml.add(presentationXml);
			addVideos(presentationXml);
		}		
		
		return fsxml.asXML();
	}
	
	private Node getPresentation(String presentation) {
		Document pres = FSXMLRequestHandler.instance().getNodeProperties(presentation, false);
		
		if (pres != null) {
			logger.debug("presentation  "+pres.asXML());
			String refer = pres.selectSingleNode("fsxml/presentation/@referid") == null ? "" : pres.selectSingleNode("fsxml/presentation/@referid").getText();
			logger.debug("refer = "+refer);
			if (refer != "") {
				pres = FSXMLRequestHandler.instance().getNodeProperties(refer, false);
			}
		}
		
		if (pres != null) {
			return pres.selectSingleNode("fsxml/presentation").detach();
		}
		return null;
	}
	
	private void addVideos(Node presentationNode) {	
		List<Node> videos = presentationNode.selectNodes("//videoplaylist/video");
		
		for(Iterator<Node> iter = videos.iterator(); iter.hasNext(); ) {
			Element video = (Element) iter.next();
			
			String refer = video.selectSingleNode("@referid") == null ? "" : video.selectSingleNode("@referid").getText(); 
			if (refer != "") {
				Document videoXml = FSXMLRequestHandler.instance().getNodeProperties(refer, false);
				if (videoXml != null) {
					Node vid = videoXml.selectSingleNode("fsxml/video").detach();
					fsxml.add(vid);
				}
			}
		}
	}
}
