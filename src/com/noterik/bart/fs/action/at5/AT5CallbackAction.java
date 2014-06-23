package com.noterik.bart.fs.action.at5;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.action.ActionException;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.HttpHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;
/**
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @author Levi Pires <l.pires@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2009
 * @package com.noterik.bart.fs.action.at5
 * @project smithers2
 * @access private
 * @version $Id: AT5CallbackAction.java,v 1.15 2011-07-01 11:38:56 derk Exp $
 *
 */
public class AT5CallbackAction extends ActionAdapter {
	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(AT5CallbackAction.class);
	
	/**
	 * Default response
	 */
	public static final String RESPONSE_OK = "200";
	
	/**
	 * parameters
	 */
	private String ingestUri;
	private String callback = "http://vdx.at5.net/ping/noterik?id={id}&status=done";
	
	private String at5id;
	private String domain;
	
	@Override
	public String run() {
		logger.debug("running script CallBack");
		logger.debug("event uri: " + event.getUri());
		
		boolean raw2 = false;
		boolean raw3 = false;
		boolean screens = false;
				
		try {						
			// parse parameters		
			
			String eventUri = event.getUri();
			String videoOn = eventUri.substring(eventUri.indexOf("/video/") + 7, eventUri.length());			
			at5id = videoOn.substring(0, videoOn.indexOf("/"));
			domain = URIParser.getDomainFromUri(event.getUri());
			if(domain.equals("at5.devel")) {
				callback = "http://api.at5.nl/ping/noterikbeta?id={id}&status=done";
			}
			
			//String at5id = URIHelper.getCurrentUriPart(URIHelper.getPreviousUri(URIHelper.getPreviousUri(event.getUri())));
			String userid = URIParser.getUserFromUri(event.getUri());
			String domain = URIParser.getDomainFromUri(event.getUri());
			ingestUri = "/domain/"+domain+"/user/"+userid+"/ingest/"+at5id;
			
			String videoUri = "/domain/"+domain+"/user/"+userid+"/video/"+at5id;
			
			// check if callback has been done already
			Document vidDoc = FSXMLRequestHandler.instance().getNodeProperties(videoUri, false);
			Element callbackEl = (Element)vidDoc.selectSingleNode(".//callback");
			
			if(callbackEl != null && callbackEl.getText().toLowerCase().equals("done")){
				logger.debug("CallBack has been done already.");
				return null;
			}
			
			// check for raw2
			String raw2Uri = videoUri +"/rawvideo/2";			
			Document raw2Props = FSXMLRequestHandler.instance().getNodeProperties(raw2Uri, false);
			Element raw2StatusEl = (Element)raw2Props.selectSingleNode(".//status");
			
			if(raw2StatusEl != null){
				if(raw2StatusEl.getText().toLowerCase().equals("done")){
					raw2 = true;
				}else if(raw2StatusEl.getText().toLowerCase().equals("failed")){
					setError("transcoding failed","transcoding failed",event.getUri());
				}
				
			}
			
			
			// check for raw3
			String raw3Uri = videoUri +"/rawvideo/3";			
			Document raw3Props = FSXMLRequestHandler.instance().getNodeProperties(raw3Uri, false);
			Element raw3StatusEl = (Element)raw3Props.selectSingleNode(".//status");
			if(raw3StatusEl != null){
				if(raw3StatusEl.getText().toLowerCase().equals("done")){
					raw3 = true;
				}else if(raw3StatusEl.getText().toLowerCase().equals("failed")){
					setError("transcoding failed","transcoding failed",event.getUri());
				}
				
			}			
			
			// check for screens			
			String screensUri = videoUri + "/screens/1";
			Document screensProps = FSXMLRequestHandler.instance().getNodeProperties(screensUri, false);
			if(screensProps == null){
				logger.debug("screens are not done !");
				return null;
			}
			
			Element screensUriEl = (Element)screensProps.selectSingleNode(".//uri");
			
			if(screensUriEl != null && !screensUriEl.getText().equals("")){
				screens = true;
			}
												
			if (raw2 && raw3 && screens){
				logger.debug("all tests passed, doing callback");
				
				// callback
				setStatus("Doing callback");
				callback(callback.replace("{id}", at5id));
				
				// set callback tag to done
				String callbackUri = videoUri + "/properties/callback";
				FSXMLRequestHandler.instance().updateProperty(callbackUri, "callback", "done", "PUT", false);
				
				// set status to finished
				setStatus("finished");
			}else{
				logger.debug("raw2 is " + raw2);
				logger.debug("raw3 is " + raw3);
				logger.debug("screens is " + screens);
			}
		} catch (Exception e) {
			if(e instanceof ActionException) {
				setError("",e.getMessage());
				logger.error("",e);
			} else {
				setError("Internal system error",e.getMessage());
				logger.error("",e);
			}
			return null;
		}
		
		return null;
	}
	
	
	/**
	 * Callback function
	 * @param url
	 * @throws ActionException
	 */
	private void callback(String url) throws ActionException {
		logger.debug("callback to: " + url);
		
		// do callback
		String response = HttpHelper.sendRequest("GET", url, null, null);
		logger.debug("response from at5 callback was: "+response);
		try {
			Document doc = DocumentHelper.parseText(response);
			String code = doc.valueOf("//code");
			if(code==null || !code.equals(RESPONSE_OK)) {
				throw new ActionException("Callback was unsuccesful, code: "+code);
			}
		} catch(Exception e) {
			logger.error("response from at5 callback was: "+response, e);
			throw new ActionException("Callback was unsuccesful");
		}
	}
	
	/**
	 * Set an error message
	 * @param message
	 * @param details
	 */
	public void setError(String message, String details) {
		setError(message,details,"http://blackboots.noterik.com/team");
	}
	
	/**
	 * Set an error message
	 * @param message
	 * @param details
	 * @param uri
	 */
	public void setError(String message, String details, String uri) {
		// make xml
		String errorXml = FSXMLBuilder.getFSXMLErrorMessage("500",message,details,uri);
		
		// set error message 
		String errorUri = ingestUri + "/error/1";
		FSXMLRequestHandler.instance().saveFsXml(errorUri, errorXml, "PUT", false);
		
		doFailedCallback();
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
	
	public void doFailedCallback() {		
		String callback;
		if(domain.equals("at5.devel")) {
			callback = "http://api.at5.nl/ping/noterikbeta?id={id}&status=failed";
		} else {
			callback = "http://vdx.at5.net/ping/noterik?id={id}&status=failed";
		}
		String url = callback.replace("{id}", at5id);
		logger.debug("doing failed callback for: " + at5id + ", to: "+url);
		String response = HttpHelper.sendRequest("GET", url, null, null);
	}
}
