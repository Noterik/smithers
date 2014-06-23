package com.noterik.bart.fs.fscommand.usermanagement;

import java.rmi.activation.ActivateFailedException;
import java.util.Properties;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;

import com.noterik.bart.fs.dao.DAOException;
import com.noterik.bart.fs.fscommand.ManualEntry;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.bart.fs.tools.MD5;
import com.noterik.bart.fs.tools.ServiceHelper;
import com.noterik.bart.fs.usermanager.GroupKey;
import com.noterik.bart.fs.usermanager.User;
import com.noterik.bart.fs.usermanager.UserKey;
import com.noterik.bart.marge.model.Service;
import com.noterik.springfield.tools.HttpHelper;
import com.noterik.springfield.tools.PropertiesHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * Activates a user, e.g. adds a user to the active group
 * 
 * SYNTAX
 * 
 * useract [options ... ] -uid LOGIN
 * 
 * OPTIONS
 * 
 * -u, --uid LOGIN
 * 		The user's id, or LOGIN
 * 
 * -uuid UUID
 * 		The uuid supplied with the activation email
 * 
 * 
 * This command is used in combination with the {@link UserRegistrationCommand}
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2011
 * @package com.noterik.bart.fs.fscommand.usermanagement
 * @access private
 *
 */
public class UserActivationCommand extends UserCommandAdapter {
	/** the active group name */
	private static final String ACTIVE_GROUP = "active";
	private static final String ACTIVATION_URI = "/domain/{domainid}/user/{userid}/activate";
	
	public String execute(String uri, String xml) {
		// get input parameters
		String domain = URIParser.getDomainFromUri(uri);
		Properties params = getInputParameters(xml);
		String uid = PropertiesHelper.getString(params, "u", null);
		uid = (uid==null) ? PropertiesHelper.getString(params, "uid", null) : uid;
		String uuid = PropertiesHelper.getString(params, "uuid", null);
		
		// parameter check
		if(domain==null || uid==null || uuid==null) {
			return FSXMLBuilder.getErrorMessage("400", "Bad Request", "Parameters missing", "");
		}
		
		// check if user exists
		User user = null; 
		try {
			user = udao.read(new UserKey(domain, uid));
			if(user == null) {
				return FSXMLBuilder.getErrorMessage("404", "Not Found", "Could not find user", "");
			}
		} catch (DAOException e) {
			return FSXMLBuilder.getErrorMessage("500", "Internal System Error", e.getMessage(), "");
		}
		
		// check if uuid is correct
		String userUri = user.getURI();
		String uuidInFS = FSXMLRequestHandler.instance().getPropertyValue(userUri+"/properties/activation-uuid");
		if(uuidInFS == null) {
			return FSXMLBuilder.getErrorMessage("404", "Not Found", "UUID not found", "");
		}
		if(!uuidInFS.trim().equals(uuid)) {
			return FSXMLBuilder.getErrorMessage("400", "Bad Request", "UUID was incorrect", "");
		}
		
		// add user to group
		try {
			gdao.addUserToGroup(new UserKey(domain,uid), new GroupKey(domain,ACTIVE_GROUP));
		} catch (DAOException e) {
			return FSXMLBuilder.getErrorMessage("500", "Internal System Error", e.getMessage(), "");
		}
		
		// remove uuid from filesystem
		FSXMLRequestHandler.instance().deletePropertyValue(userUri+"/properties/activation-uuid", "activation-uuid");
		
		Service service = ServiceHelper.getService(domain, "usermanager");
		
		if (service == null) {
			return FSXMLBuilder.getErrorMessage("500", "Usermanager service was null", "", "");
		}
		String barneyUrl = service.getUrl() + ACTIVATION_URI.replace("{domainid}", domain).replace("{userid}", uid);
		String method = "POST";
		String body = "hash="+MD5.getHashValue(uid);
		String contentType = "application/x-www-form-urlencoded";
		
		String response = HttpHelper.sendRequest(method, barneyUrl, body, contentType);
		String ticket = "";
		try {
			Document doc = DocumentHelper.parseText(response);
			ticket = doc.valueOf("//ticket");
			if(ticket==null || ticket.equals("")) {
				ticket = "";
			}
		} catch(Exception e) {
			return FSXMLBuilder.getErrorMessage("500", "Internal System Error", e.getMessage(), "");
		}
		
		// done
		StringBuffer xmlResponse = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		xmlResponse.append("<status id=\"200\">");
		xmlResponse.append("<properties>");
		xmlResponse.append("<message>OK</message>");
		xmlResponse.append("<details>User was activated successfully</details>");
		xmlResponse.append("<ticket>"+ticket+"</ticket>");
		xmlResponse.append("<uri></uri>");
		xmlResponse.append("</properties>");
		xmlResponse.append("</status>");
		return xmlResponse.toString();
	}

	/**
	 * Manual description
	 */
	public ManualEntry man() {
		ManualEntry entry = new ManualEntry();
		entry.setDescription("Activate a user");
		entry.setSyntax("useract [options ... ] -uid LOGIN");
		entry.addOption("u","uid","The user's id, or LOGIN.");
		entry.addOption("uuid","The UUID supplied with the activation email.");
		return entry;
	}

}
