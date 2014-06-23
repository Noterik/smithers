package com.noterik.bart.fs.fscommand.usermanagement;

import java.util.Properties;

import com.noterik.bart.fs.dao.DAOException;
import com.noterik.bart.fs.fscommand.ManualEntry;
import com.noterik.bart.fs.usermanager.User;
import com.noterik.bart.fs.usermanager.UserKey;
import com.noterik.springfield.tools.PropertiesHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * Update a user's password
 * 
 * SYNTAX
 * 
 * passwd -p PASSWORD -uid LOGIN
 * 
 * OPTIONS
 * 
 * -p, --password PASSWORD
 * 		The user's new PASSWORD.
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
public class PasswdCommand extends UserCommandAdapter {

	public String execute(String uri, String xml) {
		boolean updated;
		
		// get input parameters
		String domain = URIParser.getDomainFromUri(uri);
		Properties params = getInputParameters(xml);
		String uid = PropertiesHelper.getString(params, "u", null);
		uid = (uid==null) ? PropertiesHelper.getString(params, "uid", null) : uid;
		String password = PropertiesHelper.getString(params, "p", null);
		password = (password==null) ? PropertiesHelper.getString(params, "password", null) : password;
		
		// parameter check
		if(domain==null || uid==null || password == null) {
			return FSXMLBuilder.getErrorMessage("400", "Bad Request", "Parameters missing", "");
		}
		
		// update the user's password
		try {
			User user = udao.read(new UserKey(domain, uid));
			user.setPassword(password);
			updated = udao.update(user);
			if(!updated) {
				return FSXMLBuilder.getErrorMessage("500", "Internal Server Error", "Unknown error", "");
			}
		} catch (DAOException e) {
			return FSXMLBuilder.getErrorMessage("500", "Internal Server Error", e.getMessage(), "");
		}
		return FSXMLBuilder.getStatusMessage("200", "OK", "User password was updated successfully", "");
	}
	
	public ManualEntry man() {
		ManualEntry entry = new ManualEntry();
		entry.setDescription("Update a user's password");
		entry.setSyntax("passwd -p PASSWORD -uid LOGIN");
		entry.addOption("p","password","The user's new PASSWORD.");
		entry.addOption("u","uid","The user's id, or LOGIN");
		return entry;
	}
}
