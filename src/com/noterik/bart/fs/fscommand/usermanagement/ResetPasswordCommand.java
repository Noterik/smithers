package com.noterik.bart.fs.fscommand.usermanagement;

import java.util.Properties;

import org.apache.log4j.Logger;

import com.noterik.bart.fs.dao.DAOException;
import com.noterik.bart.fs.fscommand.ManualEntry;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.bart.fs.usermanager.User;
import com.noterik.bart.fs.usermanager.UserKey;
import com.noterik.springfield.tools.PropertiesHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * Reset a password
 * 
 * SYNTAX
 * 
 * resetpw [options ... ] -uid LOGIN
 * 
 * OPTIONS
 * 
 * -p, --password PASSWORD
 * 		The users password.
 * 
 * -u, --uid LOGIN
 * 		The user's id, or LOGIN
 * 
 * -uuid 
 * 		The uuid supplied with the email
 * 
 * 
 * This command is used in combination with the {@link ForgotPasswordCommand}
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2011
 * @package com.noterik.bart.fs.fscommand.usermanagement
 * @access private
 *
 */
public class ResetPasswordCommand extends UserCommandAdapter {
	/** the ResetPasswordCommand's log4j Logger */
	private static final Logger logger = Logger.getLogger(ResetPasswordCommand.class);
	
	public String execute(String uri, String xml) {
		// get input parameters
		String domain = URIParser.getDomainFromUri(uri);
		Properties params = getInputParameters(xml);
		String uid = PropertiesHelper.getString(params, "u", null);
		uid = (uid==null) ? PropertiesHelper.getString(params, "uid", null) : uid;
		String password = PropertiesHelper.getString(params, "p", null);
		password = (password==null) ? PropertiesHelper.getString(params, "password", null) : password;
		String uuid = PropertiesHelper.getString(params, "uuid", null);
		
		// parameter check
		if(domain==null || uid==null || password==null || uuid==null) {
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
		String uuidInFS = FSXMLRequestHandler.instance().getPropertyValue(userUri+"/properties/forgotpw-uuid");
		if(uuidInFS == null) {
			return FSXMLBuilder.getErrorMessage("404", "Not Found", "UUID not found", "");
		}
		if(!uuidInFS.trim().equals(uuid)) {
			return FSXMLBuilder.getErrorMessage("400", "Bad Request", "UUID was incorrect", "");
		}
		
		// reset password
		try {
			user.setPassword(password);
			boolean updated = udao.update(user);
			if(!updated) {
				return FSXMLBuilder.getErrorMessage("500", "Internal Server Error", "Unknown error", "");
			}
		} catch (DAOException e) {
			return FSXMLBuilder.getErrorMessage("500", "Internal System Error", e.getMessage(), "");
		}
		
		// remove uuid from filesystem
		FSXMLRequestHandler.instance().deletePropertyValue(userUri+"/properties/forgotpw-uuid", "forgotpw-uuid");
		
		// done
		return FSXMLBuilder.getStatusMessage("200", "OK", "User password was reset successfully", "");
	}

	/**
	 * Manual description
	 */
	public ManualEntry man() {
		ManualEntry entry = new ManualEntry();
		entry.setDescription("Reset a password");
		entry.setSyntax("resetpw [options ... ] -uid LOGIN");
		entry.addOption("p","password","The users password.");
		entry.addOption("u","uid","The user's id, or LOGIN.");
		entry.addOption("uuid","The uuid supplied with the email.");
		return entry;
	}
	
}