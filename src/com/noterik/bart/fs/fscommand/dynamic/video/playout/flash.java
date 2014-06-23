package com.noterik.bart.fs.fscommand.dynamic.video.playout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.fscommand.DynamicCommand;
import com.noterik.bart.fs.fscommand.dynamic.presentation.playout.cache;
import com.noterik.bart.fs.fscommand.dynamic.presentation.playout.flash.config;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

public class flash implements DynamicCommand {
	/** Logger */
	private static Logger logger = Logger.getLogger(flash.class);
	
	public synchronized String run(String uri,String xml) {
		logger.debug("start dynamic/video/playout/flash");
		
		Document returnXml = DocumentHelper.createDocument();
		Element fsxml = returnXml.addElement("fsxml");
		fsxml.addElement("properties");
			
		String domain = URIParser.getDomainIdFromUri(uri);			
		String user = URIParser.getUserIdFromUri(uri);
		String selectedplaylist = "";
		Element handlerparams = null;
		
		config conf = new config(domain, user, selectedplaylist, handlerparams);
		
		Document videoProperties = cache.get(uri);
		if (videoProperties==null) {
			videoProperties = FSXMLRequestHandler.instance().getNodeProperties(uri, false);
			if (videoProperties == null) {
				return FSXMLBuilder.getErrorMessage("404", "Video not found",
					"You have to supply an existing video", "http://teamelements.noterik.nl/team");
			}
			cache.put(uri, videoProperties);
		}
		
		Node videoXml = getVideo(uri);		
		fsxml.add(videoXml);
		Node videoConfig = getVideoConfig(uri, videoXml, conf);
		
		//add fake presentation as placeholder for the video
		Element presentation = addPlaceholderPresentation(uri);
		fsxml.add(presentation);
		
		if (videoConfig != null) {
			fsxml.add(videoConfig);
			logger.debug("past video config");
			
			List<Element> players = addPlayer(videoConfig);			
			for (int j = 0; j < players.size(); j++) {
				fsxml.add(players.get(j));
			}
		}			
		logger.debug("past adding player(s)");	
		
		
		
		return fsxml.asXML();		
	}
	
	private static Node getVideo(String video) {
		Document vid = cache.get(video);
		if (vid==null) {
			vid = FSXMLRequestHandler.instance().getNodeProperties(video, false);
			cache.put(video, vid);
		}
		
		String refer = "";
		
		if (vid != null) {
			refer = vid.selectSingleNode("fsxml/video/@referid") == null ? "" : vid.selectSingleNode("fsxml/video/@referid").getText();
			if (refer != "") {
				vid = cache.get(refer);
				if (vid==null) {
					vid = FSXMLRequestHandler.instance().getNodeProperties(refer, false);
					cache.put(refer, vid);
				}
			}
		}
		if (vid != null) {
			Element v = (Element) vid.selectSingleNode("fsxml/video").detach();
			if (!refer.equals("")) {
				v.addAttribute("fullid", refer);
			} else {
				v.addAttribute("fullid", video);
			}
			return v;
		}
		return null;
	}
	
	private static Node getVideoConfig(String video, Node videoXml, config c) {		
		Document tmpConf = null;		

		// domain conf
		logger.debug("before domain conf");
		String url = "/domain/"+c.getDomain()+"/config/presentation/filesystem/1";
		Document conf = cache.get(url);
		if (conf==null) {
			conf = FSXMLRequestHandler.instance().getNodeProperties(url, false);
			cache.put(url, conf);
		}
		logger.debug("after domain conf");
		Boolean allowReplace = conf.selectSingleNode("/fsxml/filesystem[@id='1']/properties/allow_replace") == null ? false : Boolean.valueOf(conf.selectSingleNode("/fsxml/filesystem[@id='1']/properties/allow_replace").getText());
			
		// user conf
		logger.debug("before user conf");
		tmpConf = cache.get("/domain/"+c.getDomain()+"/user/"+c.getUser()+"/config/presentation/filesystem/1");
		if (tmpConf==null && !cache.isEmpty("/domain/"+c.getDomain()+"/user/"+c.getUser()+"/config/presentation/filesystem/1")) {
			tmpConf = FSXMLRequestHandler.instance().getNodeProperties("/domain/"+c.getDomain()+"/user/"+c.getUser()+"/config/presentation/filesystem/1", false);	
			cache.put("/domain/"+c.getDomain()+"/user/"+c.getUser()+"/config/presentation/filesystem/1", tmpConf);
		}	
		logger.debug("after sponsor conf");
		
		if (tmpConf != null && (conf == null || !allowReplace)) {
			conf = tmpConf;
			allowReplace = conf.selectSingleNode("/fsxml/filesystem[@id='1']/properties/allow_replace") == null ? false : Boolean.valueOf(conf.selectSingleNode("/fsxml/filesystem[@id='1']/properties/allow_replace").getText());
		} else if (tmpConf != null && allowReplace) {
			handleIncludeExcludeNodes(conf, tmpConf);
			allowReplace = tmpConf.selectSingleNode("/fsxml/filesystem[@id='1']/properties/allow_replace") == null ? false : Boolean.valueOf(tmpConf.selectSingleNode("/fsxml/filesystem[@id='1']/properties/allow_replace").getText());
			tmpConf = null;
		}
		
		if (tmpConf != null) {
			String refer = tmpConf.selectSingleNode("/fsxml/filesystem/@referid") == null ? "" : tmpConf.selectSingleNode("/fsxml/filesystem/@referid").getText();
			if (!refer.equals("")) {
				tmpConf = FSXMLRequestHandler.instance().getNodeProperties(refer, false);				
			}
		}
		
		if (tmpConf != null && (conf == null || !allowReplace)) {
			conf = tmpConf;
		} else if (tmpConf != null && allowReplace) {
			handleIncludeExcludeNodes(conf, tmpConf);
		}
		
		if (conf != null) {
			return conf.selectSingleNode("fsxml/filesystem[@id='1']").detach();
		}	
		return null;
	}
	
	private static void handleIncludeExcludeNodes(Document conf, Document tmpConf) {
		List<Node> includeNodes = tmpConf.selectNodes("/fsxml/filesystem[@id='1']/*[@id and not(ends-with(@id,'_exclude'))]");
		List<Node> excludeNodes = tmpConf.selectNodes("/fsxml/filesystem[@id='1']/*[ends-with(@id,'_exclude')]");
		
		logger.debug("number of includeNodes = "+includeNodes.size());
		for (int j = 0; j < includeNodes.size(); j++) {
			logger.debug(j+" = "+includeNodes.get(j).toString());
		}
		logger.debug("number of excludeNodes = "+excludeNodes.size());
		for (int j = 0; j < excludeNodes.size(); j++) {
			logger.debug(j+" = "+excludeNodes.get(j).toString());
		}
		
		Element base = (Element) conf.selectSingleNode("/fsxml/filesystem[@id='1']");
		
		if (includeNodes != null) {
			for (int i = 0; i < includeNodes.size(); i++) {
				String nodename = includeNodes.get(i).getName();					
				String nodeid = includeNodes.get(i).valueOf("@id");
				
				logger.debug("check if node exists "+nodename+" id "+nodeid);
				
				Node existingNode = base.selectSingleNode(nodename+"[@id='"+nodeid+"']");
				if (existingNode != null) {
					logger.debug("node exists, replace");
					List contentOfBase = base.content();
					int index = contentOfBase.indexOf(existingNode);
					contentOfBase.set(index, includeNodes.get(i).detach());
				} else {
					base.add(includeNodes.get(i).detach());
				}
			}
		}
		
		if (excludeNodes != null) {
			logger.debug("handling exclude nodes for user");
			for (int i = 0; i < excludeNodes.size(); i++) {
				logger.debug("handling exclude node nr "+i);
				String nodename = excludeNodes.get(i).getName();					
				String nodeid = excludeNodes.get(i).valueOf("@id");					
				nodeid = nodeid.substring(0, nodeid.lastIndexOf("_exclude"));
				
				logger.debug("about to exclude "+nodename+" with id "+nodeid);
				
				Node remove = base.selectSingleNode(nodename+"[@id='"+nodeid+"']");
				if (remove != null) {
					logger.debug("node to exclude found, detach");
					remove.detach();
				}
			}
		}
	}
	
	private static List<Element> addPlayer(Node configNode) {
		List<Node> players = configNode.selectNodes("//player");
		List<Element> result = new ArrayList<Element>();
		
		for(Iterator<Node> iter = players.iterator(); iter.hasNext(); ) {
			Element player = (Element) iter.next();
		
			String refer = player.selectSingleNode("@referid") == null ? "" : player.selectSingleNode("@referid").getText();
			if (!refer.equals("")) {
				Document playerXml = cache.get(refer);
				if (playerXml==null && !cache.isEmpty(refer)) {
					playerXml = FSXMLRequestHandler.instance().getNodeProperties(refer, false);
// daniel
					cache.put(refer,playerXml);
				}
				if (playerXml != null) {
					Element p = (Element) playerXml.selectSingleNode("fsxml/player").detach();
					p.addAttribute("fullid", refer);
					result.add(p);
				}				
			}
		}
		return result;
	}
	
	private static Element addPlaceholderPresentation(String uri) {
		Document xml = DocumentHelper.createDocument();
		
		Element presentation = xml.addElement("presentation");
		presentation.addAttribute("id", "1");
		String baseUri = uri.substring(0, uri.lastIndexOf("/video/"));
		presentation.addAttribute("fullid", baseUri+"/presentation/1");
		Element properties = presentation.addElement("properties");
		properties.addEntity("title", "test title");
		properties.addEntity("description", "test description");
		Element videoplaylist = presentation.addElement("videoplaylist");
		videoplaylist.addAttribute("id", "1");
		videoplaylist.addElement("properties");
		Element vid = videoplaylist.addElement("video");
		vid.addAttribute("id", "1");
		vid.addAttribute("referid", uri);
		
		return presentation;
	}
}
