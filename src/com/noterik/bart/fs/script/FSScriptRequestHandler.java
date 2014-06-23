package com.noterik.bart.fs.script;

import org.dom4j.Document;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.bart.fs.triggering.TriggerSystemManager;
import com.noterik.springfield.tools.HttpHelper;
import com.noterik.springfield.tools.XMLHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;

/**
 * Handler for requests with mimemetype application/fsscript
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.script
 * @access private
 * @version $Id: FSScriptRequestHandler.java,v 1.4 2011-07-01 11:38:56 derk Exp $
 *
 */
public class FSScriptRequestHandler {
	/**
	 * instance
	 */
	private static FSScriptRequestHandler instance;
	public static FSScriptRequestHandler instance() {
		if (instance == null) {
			instance = new FSScriptRequestHandler();
		}
		return instance;
	}
	
	/**
	 * Default constructor
	 */
	private FSScriptRequestHandler() {}
	
	public String handlePOST(String uri, String value) {
		return handle(uri,value,"POST");
	}
	
	public String handlePUT(String uri, String value) {
		return handle(uri,value,"PUT");
	}
	
	// TODO: check (recursively) if resource was script, and remove it from the tirggering system
	public String handleDELETE(String uri) {
		TriggerSystemManager.getInstance().removeScript(uri);
		return FSXMLBuilder.getStatusMessage("script was successfully deleted", "", "");
	}
	
	private String handle(String uri, String value, String method) {
		// send to filessystem
		String fsResponse = "";
		if(method.equals("PUT")) {
			fsResponse = FSXMLRequestHandler.instance().handlePUT(uri,value);
		} else {
			fsResponse = FSXMLRequestHandler.instance().handlePOST(uri,value);
		}
		Document fsDoc = XMLHelper.asDocument(fsResponse);
		Node fsUriNode = fsDoc.selectSingleNode("//status/properties/uri");
		
		// initialize fsscript
		if(fsUriNode!=null) {
			String scriptUri = fsUriNode.getText();
			Document doc = XMLHelper.asDocument(value);
			if (doc != null) {
				FSScript script = new FSScript(scriptUri,doc);
				TriggerSystemManager.getInstance().addScript(scriptUri, script);
				return FSXMLBuilder.getStatusMessage("The script was successfully added", "", scriptUri);
			} else {
				return FSXMLBuilder.getErrorMessage("403", "The value you sent is not valid",
						"You have to POST a valid command XML", "http://teamelements.noterik.nl/team");
			}
		} else {
			return fsResponse;
		}
	}
}
