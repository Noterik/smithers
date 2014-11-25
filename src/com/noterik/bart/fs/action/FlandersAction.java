/* 
* FlandersAction.java
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.LazyHomer;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.HttpHelper;

/**
 * Action that sends request to flanders when the status
 * of a rawvideo is set to 'done'.
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2009
 * @package com.noterik.bart.fs.action
 * @access private
 * @version $Id: FlandersAction.java,v 1.3 2009-02-20 14:32:36 derk Exp $
 *
 */
public class FlandersAction extends ActionAdapter {
	/**	serialVersionUID */
	private static final long serialVersionUID = 1L;
	
	private static final String DONE = "done";

	/** the FlandersAction's log4j Logger */
	private static Logger logger = Logger.getLogger(FlandersAction.class);
	
	@Override
	public String run() {
		// parse request
		String requestBody = event.getRequestData();
		String requestUri = event.getUri();
		
		try {
			Document doc = DocumentHelper.parseText(requestBody);
			Node statusNode = doc.selectSingleNode("//properties/status");
			Node flandersNode = doc.selectSingleNode("//properties/metadata_file"); // created by flanders
			
			// check if status was done
			if(statusNode==null || !statusNode.getText().toLowerCase().equals(DONE) || flandersNode!=null) {
				logger.debug("Not sending to flanders");
				return null;
			} else {
				logger.debug("Sending request to flanders ("+requestUri+")");
				String newProperties = processRaw(requestUri, requestBody);
				FSXMLRequestHandler.instance().saveFsXml(requestUri, newProperties, "PUT", true);
			}
		} catch (Exception e) {
			logger.error("Could not parse request data");
		}
		
		return null;
	}
	
	private String processRaw(String uri, String xml) {
		// parse document
		Document doc = null;
		try {
			doc = DocumentHelper.parseText(xml);
		} catch (DocumentException e) {
			logger.error("Could not parse xml",e);
			return null;
		}
				
		// check mount property
		Node mountNode = doc.selectSingleNode("//mount");
		if(mountNode == null) {
			logger.error("No mount property was set");
			return null;
		}
				
		// extract single mount
		String mount = null;
		String mounts = mountNode.getText();
		if(mounts != null && !mounts.equals("")){
			mount = mounts.indexOf(",")!= -1 ? mounts.substring(0, mounts.indexOf(",")): mounts;
		}
		
		// determine external or local stream
		String flandersXml = null;
		if(mount.toLowerCase().startsWith("rtmp")) {
			logger.debug("External stream");
			Node filenameNode = doc.selectSingleNode("//filename");
			if(filenameNode != null) {
				String filename = filenameNode.getText();
				flandersXml = getXmlFromFlandersExternal(filename, mount);
			}
		} else if (mount.toLowerCase().indexOf("drm://") != -1) {
			logger.debug("DRM stream");
			Node filenameNode = doc.selectSingleNode("//filename");
			if(filenameNode != null) {
				String filename = filenameNode.getText();
				flandersXml = getXmlFromFlandersBgDrm(filename, mount);
			}
		} else {
			logger.debug("Local stream");
			Node extNode = doc.selectSingleNode("//extension");
			Node filenameNode = doc.selectSingleNode("//filename");
			if (filenameNode != null) {
				String filename = filenameNode.getText();
				flandersXml = getXmlFromFlandersLocal(filename, mount);
			} else if(extNode != null) {
				String extension = extNode.getText();
				String filename = uri + "/raw." + extension;
				flandersXml = getXmlFromFlandersLocal(filename, mount);
			} else {
				logger.error("Extension property was not set");
				return null;
			}
		}
				
		logger.debug("FLANDERS XML: " + flandersXml);
		xml = processXml(xml, flandersXml);
		return xml;
	}
	
	private String processXml(String original, String flanders){
		String xml = "";
		
		Map<String, String> values = new HashMap<String, String>();
		
		Document origdoc = null;
		Document flandoc = null;
		
		try {
			origdoc = DocumentHelper.parseText(original);
			flandoc = DocumentHelper.parseText(flanders);
		} catch (DocumentException e) {
			logger.error("",e);
		}
				
		Element origProp = (Element)origdoc.selectSingleNode("//properties");
        Iterator i = origProp.elementIterator(); 
        
        while(i.hasNext()) {
            Element prop = (Element) i.next();
            String name = prop.getName();
            String value = prop.getText();
            values.put(name, value);
        }
        
        logger.debug("\n flandProp = " + flandoc.asXML());
        
        Element flandProp = (Element)flandoc.selectSingleNode("/meta-data");
        Iterator j = flandProp.elementIterator(); 
       
        while(j.hasNext()) {
            Element prop = (Element) j.next();
            String name = prop.getName();
            String value = prop.getText();
            
            //For marin there metadata is leading
            if (values.containsKey("mount") && values.get("mount").toLowerCase().equals("marin")) {
            	if (!values.containsKey(name)) {
            		values.put(name, value);
            	}
            } else {
            	values.put(name, value);
            }
        }
        
        Element finalEl = DocumentHelper.createElement("fsxml");
        Element propsEl = finalEl.addElement("properties");

        Iterator<String> it = values.keySet().iterator();
        while(it.hasNext()){
        	String name = it.next();
        	String value = values.get(name);        	
        	propsEl.addElement(name).addText(value);
        }             
		
        xml = finalEl.asXML();
        
		return xml;
	}
	
	private  String getXmlFromFlandersLocal(String fileUri, String mount) {
		String address = LazyHomer.getActiveService("flanders");
		
		if (address == null) {
			logger.error("Could not get active flanders from cluster");
			return null;
		}
		
		String flandersUrl = "http://" + address + "/flanders/restlet/extract";
		logger.debug("FLANDERS URL: " + flandersUrl);		
		String attach = "<root><mount>" + mount + "</mount><source>" + fileUri + "</source></root>";		
		logger.debug("FLANDERS REQUEST XML: " + attach);
		return HttpHelper.sendRequest("POST", flandersUrl, attach, "text/plain");
	}
	
	private String getXmlFromFlandersBgDrm(String filename, String mount) {
		String eurl = mount.toLowerCase() + filename;
		
		// check for missing protocol part
		int pos = eurl.indexOf("//");
		if (pos==-1) {
			logger.error("Missing protocol part in external uri");
			return null;
		}
		
		eurl = eurl.substring(pos);
		eurl = "http:"+eurl; 
		eurl = decodeASCII8(eurl);
		eurl = eurl+"&mode=object";
		String body = HttpHelper.sendRequest("GET", eurl, null, null);
		
		// check for streamer parameter
		pos = body.indexOf("streamer=");
		if (pos==-1) {
			logger.error("Cannot find streamer parameter in html");
			return null;
		}
		
		// check if DRM file is an mp4 file
		String result = body.substring(pos+9);
		pos = result.indexOf(".mp4");
		if (pos==-1) {
			logger.error("DRM file is not mp4");
			return null;
		}
		
		// check for ftmp file parameter 
		result = result.substring(0,pos+4);
		pos = result.indexOf("&file=");
		if(pos==-1) {
			logger.error("Cannot find rtmp file parameter");
			return null;
		}
		
		// get file using rtmp dump
		String server = result.substring(0, pos) + "/";
		String rFilename = result.substring(pos+6);
		return getXmlFromFlandersExternal(rFilename, server);
	}
	
	private String getXmlFromFlandersExternal(String filename, String server){
		String address = LazyHomer.getActiveService("flanders");
		
		if (address == null) {
			logger.error("Could not get active flanders from cluster");
			return null;
		}
		
		String flandersUrl = "http://" + address + "/flanders/restlet/extract";
		logger.debug("FLANDERS URL: " + flandersUrl);		
		String stream = server + filename;
		String attach = "<root><stream>"+stream+"</stream><file>"+filename+"</file></root>";
		logger.debug("FLANDERS REQUEST XML: " + attach);
		return HttpHelper.sendRequest("POST", flandersUrl, attach, "text/plain");
	}
	
	private String decodeASCII8(String input) {
		if (input.indexOf("\\")!=-1) {
			int pos = input.indexOf("\\");
			String output = "";
			while (pos!=-1) {
				output+=input.substring(0,pos);
				int code=Integer.parseInt(input.substring(pos+1,pos+4));
				output+=(char)code;
				input = input.substring(pos+4);
				pos = input.indexOf("\\");
			}
			output+=input;
			return output;
		} else {
			return input;
		}
	}
}
