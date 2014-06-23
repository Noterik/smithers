package com.noterik.bart.fs.fscommand.usermanagement;

import java.util.Properties;

import com.noterik.bart.fs.dao.DAOException;
import com.noterik.bart.fs.fscommand.ManualEntry;
import com.noterik.bart.fs.usermanager.User;
import com.noterik.bart.fs.usermanager.UserKey;
import com.noterik.bart.fs.usermanager.UserManagerPOJOConverter;
import com.noterik.springfield.tools.PropertiesHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * Show the information of a single user
 * 
 * SYNTAX
 * 
 * userinfo [options ...] -uid LOGIN
 * 
 * OPTIONS
 * 
 * -u, --uid LOGIN
 * 		The user's id, or LOGIN
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2010
 * @package com.noterik.bart.fs.fscommand.usermanagement
 * @access private
 *
 */
public class UserInfoCommand extends UserCommandAdapter {

	public String execute(String uri, String xml) {
		// get input parameters
		String domain = URIParser.getDomainFromUri(uri);
		Properties params = getInputParameters(xml);
		String uid = PropertiesHelper.getString(params, "u", null);
		uid = (uid==null) ? PropertiesHelper.getString(params, "uid", null) : uid;
		
		// load user
		User user = null;
		try {
			user = udao.read(new UserKey(domain, uid));
		} catch (DAOException e) {
			return FSXMLBuilder.getErrorMessage("500", "Internal Server Error", e.getMessage(), "");
		}
		
		// check if user exists
		if(user == null) {
			return FSXMLBuilder.getErrorMessage("404", "Not Found", "User not found", "");
		}
		return UserManagerPOJOConverter.user2xml(user);
	}

	public ManualEntry man() {
		ManualEntry entry = new ManualEntry();
		entry.setDescription("Show the information of a single user");
		entry.setSyntax("userinfo [options ...] -uid LOGIN");
		entry.addOption("u","uid","The user's id, or LOGIN.");
		return entry;
	}

}
