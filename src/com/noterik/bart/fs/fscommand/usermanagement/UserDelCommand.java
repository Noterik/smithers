package com.noterik.bart.fs.fscommand.usermanagement;

import java.util.Properties;

import com.noterik.bart.fs.dao.DAOException;
import com.noterik.bart.fs.fscommand.ManualEntry;
import com.noterik.bart.fs.usermanager.UserKey;
import com.noterik.springfield.tools.PropertiesHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * Remove a user
 * 
 * SYNTAX
 * 
 * userdel [options ... ] -uid LOGIN
 * 
 * OPTIONS
 * 
 * -u, --uid LOGIN
 * 		The user id, or LOGIN of the user to remove 
 * 
 * -r, --remove (TODO)
 * 		User's home directory will be removed, as well as all underlying directories 
 * 		and property nodes.
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2010
 * @package com.noterik.bart.fs.fscommand.usermanagement
 * @access private
 *
 */
public class UserDelCommand extends UserCommandAdapter {
	
	public String execute(String uri, String xml) {
		// get input parameters
		String domain = URIParser.getDomainFromUri(uri);
		Properties params = getInputParameters(xml);
		String uid = PropertiesHelper.getString(params, "u", null);
		uid = (uid==null) ? PropertiesHelper.getString(params, "uid", null) : uid;
		boolean remove = PropertiesHelper.getBoolean(params, "r", true);
		remove &= PropertiesHelper.getBoolean(params, "remove", true);
		
		// check parameters
		if(domain==null || uid==null) {
			return FSXMLBuilder.getErrorMessage("400", "Bad Request", "Parameters missing", "");
		}
		
		// remove from usermanager
		try {
			boolean removed = udao.delete(new UserKey(domain,uid));
			if(!removed) {
				return FSXMLBuilder.getErrorMessage("500", "Internal Server Error", "Unknown error", "");
			}
		} catch (DAOException e) {
			return FSXMLBuilder.getErrorMessage("500", "Internal Server Error", e.getMessage(), "");
		}
		
		// remove from filsystem
		if(remove) {
			// TODO
		}
		
		return FSXMLBuilder.getStatusMessage("200", "OK", "User was removed successfully", "");
	}
	
	public ManualEntry man() {
		ManualEntry entry = new ManualEntry();
		entry.setDescription("Remove a user");
		entry.setSyntax("userdel [options ... ] -uid LOGIN");
		entry.addOption("u","uid","The user id, or LOGIN of the user to remove");
		entry.addOption("r","remove","User's home directory will be removed, as well as all underlying directories and property nodes.");
		return entry;
	}
	
}
