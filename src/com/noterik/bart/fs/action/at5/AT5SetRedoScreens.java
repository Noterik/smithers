package com.noterik.bart.fs.action.at5;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.action.ActionException;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.HttpHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

public class AT5SetRedoScreens extends ActionAdapter {
	private static Logger logger = Logger.getLogger(AT5SetRedoScreens.class);
	public static final String RESPONSE_OK = "200";
	
	private String at5id;
	private String domain;
	private String ingestUri;
	
	@Override
	public String run() {
		
		logger.debug("**************************** starting AT5SetRedoScreensAction ************************");
		String requestBody = event.getRequestData();
		String uri = event.getUri();
		
		logger.debug("request body: " + requestBody);
		logger.debug("uri: " + uri);
		
		init();
		
		try {
			Document doc = DocumentHelper.parseText(requestBody);			
			Node stNode = doc.selectSingleNode("//properties/status");
			
			logger.debug("about to check for status");
			
			if(stNode != null && stNode.getText().toLowerCase().equals("done")){
			
				logger.debug("Status is done");
				// get uri of video
				String vidUri = uri.substring(0, uri.lastIndexOf("/rawvideo"));
				
				// check if screens tag already exists
				String scrUri = vidUri + "/screens/1";
				logger.debug("screens uri: " + scrUri);
				
				Document scrdoc = FSXMLRequestHandler.instance().getNodeProperties(scrUri, false);
				
				if(scrdoc != null){
					logger.debug("Screens tag already exists, no need to create screens.");
					logger.debug("screens props: " + scrdoc.asXML());
					return null;
				}
				
				logger.debug("redo will be set to true");
				// get uri of redo tag
				String screensUri = vidUri + "/screens/1/properties";
				
				logger.debug("redoUri: " + screensUri);
				
				// set the tag to true
				String screenProperties = "<fsxml>" +
											"<properties>" +
												"<size>320x240</size>" +
												"<interval>1</interval>" +
												"<redo>true</redo>" +
												"<useraw>2</useraw>" + 
											"</properties>" +
										"</fsxml>";
		
				logger.debug("redo properties: " + screenProperties);
				
				// change status to making screenshots
				setStatus("Extracting screenshots");
				doStatusCallback("screenshots");
				
				if(!FSXMLRequestHandler.instance().saveFsXml(screensUri, screenProperties, "PUT", true)) {
					// TODO: failed feedback
					throw new ActionException("Screens properties could not be set");
				}
			}else{
				logger.debug("Status was not done, redo will not be set to true");
			}
		} catch (Exception e) {
			logger.error("",e);
		}	
		return null;
	}
	
	public void init() {
		String eventUri = event.getUri();
		String videoOn = eventUri.substring(eventUri.indexOf("/video/") + 7, eventUri.length());			
		at5id = videoOn.substring(0, videoOn.indexOf("/"));
		String userid = URIParser.getUserFromUri(event.getUri());
		domain = URIParser.getDomainFromUri(event.getUri());
		ingestUri = "/domain/"+domain+"/user/"+userid+"/ingest/"+at5id;
	}
	
	/**
	 * Set a status message
	 * @param message
	 */
	public void setStatus(String message) {
		// make xml
		String statusXml = FSXMLBuilder.getFSXMLStatusMessage(message, "", "http://blackboots.noterik.com/team");
			
		// set status message
		String statusUri = ingestUri + "/status/1";
		FSXMLRequestHandler.instance().saveFsXml(statusUri, statusXml, "PUT", false);
	}
	
	public void doStatusCallback(String status) {
		String callback;
		if(domain.equals("at5.devel")) {
			callback = "http://api.at5.nl/ping/noterikbeta?id={id}&status={status}";
		} else {
			callback = "http://vdx.at5.net/ping/noterik?id={id}&status={status}";
		}
		String url = callback.replace("{id}", at5id).replace("{status}", status);
		logger.debug("doing status callback for: " + at5id + ", to: "+url);
		String response = HttpHelper.sendRequest("GET", url, null, null);
		
		logger.debug("response from at5 callback was: "+response);
		try {
			Document doc = DocumentHelper.parseText(response);
			String code = doc.valueOf("//code");
			if(code==null || !code.equals(RESPONSE_OK)) {
				logger.error("Callback was unsuccesful, code: "+code+", response: "+response);
			}
		} catch(Exception e) {
			logger.error("response from at5 callback was: "+response, e);
		}
	}
}
