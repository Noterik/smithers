package com.noterik.bart.fs.action;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.XMLHelper;
import com.noterik.springfield.tools.fs.URIParser;

public class SetRedoScreensAction extends ActionAdapter {
	/** the SetRedoScreensAction's log4j Logger */
	private static Logger logger = Logger.getLogger(SetRedoScreensAction.class);

	/** constants */
	private static final String SCREENS_CONFIG_URI = "/domain/{domain}/config/ingest/setting/screens";
	private static final String DEFAULT_SCREENS_PROPERTIES = "<fsxml><properties><size>320x240</size><interval>1</interval><redo>true</redo></properties></fsxml>";
	
	@Override
	public String run() {
		
		logger.debug("**************************** starting SetRedoScreensAction ************************");
		String requestBody = event.getRequestData();
		String uri = event.getUri();
		String domain = URIParser.getDomainFromUri(uri);
		
		logger.debug("request body: " + requestBody);
		logger.debug("uri: " + uri);
		
		try {
			Document doc = DocumentHelper.parseText(requestBody);			
			Node mtNode = doc.selectSingleNode("//properties/mount");
			
			if(mtNode == null){
				logger.debug("Mounts are not set, redo will not be set to true");
				return null;
			}
			
			// get uri and properties of video
			String vidUri = uri.substring(0, uri.lastIndexOf("/rawvideo"));
			
			String transcoder = doc.selectSingleNode("//properties/transcoder") == null ? "" : doc.selectSingleNode("//properties/transcoder").getText();

			// work around so we don't need to fix APU
			if ( !transcoder.equals("apu") ) {
				// check is screens are already done
				Document vidDoc = FSXMLRequestHandler.instance().getNodeProperties(vidUri, false);
				boolean hasScreens = vidDoc.selectSingleNode(".//screens") != null;
				if(hasScreens){
					logger.debug("Screens are already present. No need to set redo to true.");
					return null;
				}				
			}			
			
			logger.debug("Screens properties will be created");
			// get uri of redo tag
			String screensUri = vidUri + "/screens/1/properties";
			logger.debug("screensUri: " + screensUri);
			
			// check for domain preferences
			String screenProperties = DEFAULT_SCREENS_PROPERTIES;
			String configURI = SCREENS_CONFIG_URI.replace("{domain}", domain);
			Document configDoc = FSXMLRequestHandler.instance().getNodeProperties(configURI, false);
			if(configDoc != null) {
				logger.debug("Custom screenshots profile");
				Node configProperties = configDoc.selectSingleNode("//screensprofile/properties");
				screenProperties = "<fsxml>"+configProperties.asXML()+"</fsxml>";
			}
			
			//if apu most likely only 1 raw available, always use raw 1 otherwise job might never get executed
			if (transcoder.equals("apu")) {	
				Document sProperties = XMLHelper.asDocument(screenProperties);
				Node useraw = sProperties.selectSingleNode("//useraw");
				if (useraw != null) {
					sProperties.selectSingleNode("//useraw").setText("1");
				}
				screenProperties = sProperties.asXML();
			}
	
			logger.debug("redo properties: " + screenProperties);
			if(!FSXMLRequestHandler.instance().saveFsXml(screensUri, screenProperties, "PUT", true)) {
				throw new ActionException("Screens properties could not be set");
			}else{
				logger.debug("screens properties were created successfully");
			}
		} catch (Exception e) {
			logger.error("",e);
		}	
		return null;
	}
}
