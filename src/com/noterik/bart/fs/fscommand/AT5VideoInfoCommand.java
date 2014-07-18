/* 
* AT5VideoInfoCommand.java
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
package com.noterik.bart.fs.fscommand;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;

/**
 * AT5VideoInfoCommand
 * 
 * Command to send video information to AT5, including the streaming servers and paths
 * 
 * @author Pieter van Leeuwen
 * @Copyright: Noterik B.V. 2011
 */

public class AT5VideoInfoCommand implements Command {
	/** Logger */
	private static Logger logger = Logger.getLogger(AT5VideoInfoCommand.class);

	public String execute(String url, String xml) {
		Element raw;
		Document video = FSXMLRequestHandler.instance().getNodeProperties(url,false);
		List<Node> raws = video.selectNodes("//rawvideo");
		
		logger.debug("# of rawvideos = "+raws.size());

		// Loop all videos
		for(Iterator<Node> iter = raws.iterator(); iter.hasNext(); ) {
			raw = (Element) iter.next();
			
			int filesize = raw.selectSingleNode("properties/filesize") == null ? 0 : Integer.parseInt(raw.selectSingleNode("properties/filesize").getText());
			logger.debug("filesize = "+filesize);
			if (filesize == 0) {
				logger.debug("detached rawvideo with filesize == 0");
				raw.detach();
			} else {				
				String id = raw.attribute("id") == null ? "" : raw.attribute("id").getText();
				logger.debug("id of this raw "+id);
				
				Boolean original = raw.selectSingleNode("properties/original") == null ? false : Boolean.parseBoolean(raw.selectSingleNode("properties/original").getText());
				
				if (!original) {
					Element properties = (Element) raw.selectSingleNode("properties");
					properties.addElement("filename").addText("mp4:"+url+"/rawvideo/"+id+"/raw.mp4");
		
					Node mountNode = raw.selectSingleNode("properties/mount");
					if (mountNode != null) {
						String mount = raw.selectSingleNode("properties/mount").getText();
						//mountNode.detach();
						if (!mount.equals("")) {
							String[] mounts = mount.split(",");
							
							for (int i = 0; i < mounts.length; i++) {
								String fullPath = "rtmp://"+mounts[i]+".noterik.com/"+mounts[i];
								properties.addElement("server").addText(fullPath);
							}
						}					
					}
				} else {
					logger.debug("detached original");
					raw.detach();
				}
			}
		}		
		logger.debug(video.asXML());		
		return video.asXML();
	}
	
	public ManualEntry man() {
		return null;
	}
}
