package com.noterik.bart.fs.legacy.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.springfield.tools.HttpHelper;

/**
 * Sends request to Flanders, in order to determine the file information
 */
public class FlandersHelper {
	/** The FlandersHelper's lgo4j logger */
	private static final Logger LOG = Logger.getLogger(FlandersHelper.class);
	
	/** list of streaming machines */
	private static final List<String> streams = new ArrayList<String>();
	static {
		streams.add("stream4");
		streams.add("stream5");
		streams.add("stream6");
		streams.add("stream7");
		streams.add("stream8");
		streams.add("stream9");
		streams.add("stream10");
		streams.add("stream11");
		streams.add("stream12");
		streams.add("stream13");
		streams.add("stream14");
		streams.add("stream15");
	}
	
	public static String processRaw(String uri, String xml){
		// parse document
		Document doc = null;
		try {
			doc = DocumentHelper.parseText(xml);
		} catch (DocumentException e) {
			LOG.error("Could not parse xml",e);
			return null;
		}
		
		// check mount property
		Node mountNode = doc.selectSingleNode("//mount");
		if(mountNode == null) {
			LOG.error("No mount property was set");
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
			LOG.debug("External stream");
			Node filenameNode = doc.selectSingleNode("//filename");
			if(filenameNode != null) {
				String filename = filenameNode.getText();
				flandersXml = getXmlFromFlandersExternal(filename, mount);
			}
		} else if (mount.toLowerCase().indexOf("drm://") != -1) {
			LOG.debug("DRM stream");
			Node filenameNode = doc.selectSingleNode("//filename");
			if(filenameNode != null) {
				String filename = filenameNode.getText();
				flandersXml = getXmlFromFlandersBgDrm(filename, mount);
			}
		} else {
			LOG.debug("Local stream");
			Node extNode = doc.selectSingleNode("//extension");
			if(extNode != null) {
				String extension = extNode.getText();
				String filename = uri + "/raw." + extension;
				flandersXml = getXmlFromFlandersLocal(filename, mount);
			} else {
				LOG.error("Extension property was not set");
				return null;
			}
		}
		
		LOG.debug("FLANDERS XML: " + flandersXml);
		xml = processXml(xml, flandersXml);
		return xml;
	}	
	
	private static String processXml(String original, String flanders){
		String xml = "";
		
		Map<String, String> values = new HashMap<String, String>();
		
		Document origdoc = null;
		Document flandoc = null;
		
		try {
			origdoc = DocumentHelper.parseText(original);
			flandoc = DocumentHelper.parseText(flanders);
		} catch (DocumentException e) {
			LOG.error("",e);
		}
				
		Element origProp = (Element)origdoc.selectSingleNode("//properties");
        Iterator i = origProp.elementIterator(); 
        
        while(i.hasNext()) {
            Element prop = (Element) i.next();
            String name = prop.getName();
            String value = prop.getText();
            values.put(name, value);
        }
        
        LOG.debug("\n flandProp = " + flandoc.asXML());
        
        Element flandProp = (Element)flandoc.selectSingleNode("/meta-data");
        Iterator j = flandProp.elementIterator(); 
       
        while(j.hasNext()) {
            Element prop = (Element) j.next();
            String name = prop.getName();
            String value = prop.getText();
            values.put(name, value);
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

	private static String getXmlFromFlandersLocal(String fileUri, String mount){
		String flandersUrl = "http://" + mount + ".noterik.com:8080/flanders/restlet/extract";
		LOG.debug("FLANDERS URL: " + flandersUrl);		
		String location = getLocation(mount);	
		String fileLocation = location + fileUri;		
		String attach = "<root><source>" + fileLocation + "</source></root>";		
		LOG.debug("FLANDERS REQUEST XML: " + attach);
		return HttpHelper.sendRequest("POST", flandersUrl, attach, "text/plain");
	}
	
	private static String getXmlFromFlandersBgDrm(String filename, String mount) {
		String eurl = mount.toLowerCase() + filename;
		
		// check for missing protocol part
		int pos = eurl.indexOf("//");
		if (pos==-1) {
			LOG.error("Missing protocol part in external uri");
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
			LOG.error("Cannot find streamer parameter in html");
			return null;
		}
		
		// check if DRM file is an mp4 file
		String result = body.substring(pos+9);
		pos = result.indexOf(".mp4");
		if (pos==-1) {
			LOG.error("DRM file is not mp4");
			return null;
		}
		
		// check for ftmp file parameter 
		result = result.substring(0,pos+4);
		pos = result.indexOf("&file=");
		if(pos==-1) {
			LOG.error("Cannot find rtmp file parameter");
			return null;
		}
		
		// get file using rtmp dump
		String server = result.substring(0, pos) + "/";
		String rFilename = result.substring(pos+6);
		return getXmlFromFlandersExternal(rFilename, server);
	}
	
	private static String getXmlFromFlandersExternal(String filename, String server){
		String mount = getRandomMount();
		String flandersUrl = "http://" + mount + ".noterik.com:8080/flanders/restlet/extract";
		LOG.debug("FLANDERS URL: " + flandersUrl);		
		String stream = server + filename;
		String attach = "<root><stream>"+stream+"</stream><file>"+filename+"</file></root>";
		LOG.debug("FLANDERS REQUEST XML: " + attach);
		return HttpHelper.sendRequest("POST", flandersUrl, attach, "text/plain");
	}
	
	
	private static String getRandomMount() {
		Random generator = new Random();
		int rand = generator.nextInt(streams.size());
		return streams.get(rand);
	}

	private static boolean isLocal(String mount) {
		String stream = mount.toLowerCase();
		return streams.contains(stream);
	}
	
	private static String getLocation(String stream){
		String location = null;
				
		if (stream.toLowerCase().equals("stream4")){
			location = "d:" + File.separatorChar + "stream4";
		}else if (stream.toLowerCase().equals("stream5")){
			location = "e:" + File.separatorChar + "stream5";
		}else if (stream.toLowerCase().equals("stream6")){
			location = "h:" + File.separatorChar + "stream6";
		}else if (stream.toLowerCase().equals("stream7")){
			location = "f:" + File.separatorChar + "stream7";
		}else if (stream.toLowerCase().equals("stream8")){
			location = "g:" + File.separatorChar + "stream8";
		}else if (stream.toLowerCase().equals("stream9")){
			location = "h:" + File.separatorChar + "stream9";
		}else if (stream.toLowerCase().equals("stream10")){
			location = "f:" + File.separatorChar + "stream10";
		}else if (stream.toLowerCase().equals("stream11")){
			location = "g:" + File.separatorChar + "stream11";
		}else if (stream.toLowerCase().equals("stream12")){
			location = "h:" + File.separatorChar + "stream12";
		}else if (stream.toLowerCase().equals("stream13")){
			location = "f:" + File.separatorChar + "stream13";
		}else if (stream.toLowerCase().equals("stream14")){
			location = "g:" + File.separatorChar + "stream14";
		}else if (stream.toLowerCase().equals("stream15")){
			location = "h:" + File.separatorChar + "stream15";
		}
		
		return location;
	}
	
	/**
	 * TODO: comment
	 * 
	 * @param input
	 * @return
	 */
	private static String decodeASCII8(String input) {
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