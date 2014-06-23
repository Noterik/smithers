package com.noterik.bart.fs.fscommand;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.dom4j.Document;

import com.google.gdata.client.GoogleAuthTokenFactory.UserToken;
import com.google.gdata.client.youtube.YouTubeService;
import com.google.gdata.util.AuthenticationException;
import com.noterik.springfield.tools.XMLHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;

public class YouTubeTokenCommand implements Command {
	/** Logger */
	private static Logger logger = Logger.getLogger(YouTubeTokenCommand.class);

	public String execute(String url, String xml) {
		Document doc = XMLHelper.asDocument("<?xml version='1.0' encoding='UTF-8' standalone='no'?>"+xml);
		String username = "";
		String password = "";
		
		if(doc == null){
			logger.debug("could not make xml doc");
			return FSXMLBuilder.getErrorMessage("500","No xml found", "Please provide xml", "http://teamelements.noterik.com/team");
		} else {
			username = doc.selectSingleNode("//properties/username") == null ? "" : doc.selectSingleNode("//properties/username").getText();
			password = doc.selectSingleNode("//properties/password") == null ? "" : doc.selectSingleNode("//properties/password").getText();
		}
		
		/**
		 *	http://code.google.com/apis/youtube/dashboard/gwt/index.html
		 *	user:admin@noterik.nl
		 *	password: ntk12345
		 */
		YouTubeService service = new YouTubeService("Smithers Youtube login", "AI39si47vCbOrz7ywAIg1KamuqhywzygKFKua8YpEeWs_wLb7gjAaT35Sm_Rr2Nna2GQKHZRuryyZEHEXGop4hon-UhWxeIwmA");
		try {
			//service.setUser
			service.setUserCredentials(username, password);
		} catch (AuthenticationException e) {
			//error in authentication
			for (int i = 0; i < e.getStackTrace().length; i++) {
				logger.debug(e.getStackTrace()[i]);
			}
			logger.debug("youtube authentication error "+e.getMessage()+" "+e.getResponseBody());
			return FSXMLBuilder.getErrorMessage("500","Authentication error", e.toString(), "http://teamelements.noterik.com/team");
		}
		UserToken authToken = (UserToken) service.getAuthTokenFactory().getAuthToken();
		String token = authToken.getValue();
		logger.debug("youtube token = "+token);
		
		HashMap<String,String> properties = new HashMap<String,String>();
		properties.put("token", token);
		
		return FSXMLBuilder.wrapInFsxml("", properties);
	}
	
	public ManualEntry man() {
		return null;
	}
	
}
