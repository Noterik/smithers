/* 
* html5.java
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
			addAudios(presentationXml);
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
					Element vid = (Element) videoXml.selectSingleNode("fsxml/video").detach();
					vid.addAttribute("fullid", refer);
					fsxml.add(vid);
				}
			}
		}
	}
	
	private void addAudios(Node presentationNode) {	
		List<Node> audios = presentationNode.selectNodes("//videoplaylist/audio");
		
		for(Iterator<Node> iter = audios.iterator(); iter.hasNext(); ) {
			Element audio = (Element) iter.next();
			
			String refer = audio.selectSingleNode("@referid") == null ? "" : audio.selectSingleNode("@referid").getText(); 
			if (refer != "") {
				Document audioXml = FSXMLRequestHandler.instance().getNodeProperties(refer, false);
				if (audioXml != null) {
					Element vid = (Element) audioXml.selectSingleNode("fsxml/audio").detach();
					vid.addAttribute("fullid", refer);
					fsxml.add(vid);
				}
			}
		}
	}
}
