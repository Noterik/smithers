package com.noterik.bart.fs.fscommand.usermanagement;

import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.noterik.bart.fs.dao.DAOException;
import com.noterik.bart.fs.fscommand.ManualEntry;
import com.noterik.bart.fs.usermanager.Group;
import com.noterik.bart.fs.usermanager.UserKey;
import com.noterik.bart.fs.usermanager.UserManagerPOJOConverter;
import com.noterik.springfield.tools.PropertiesHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * List current groups, and user groups.
 * 
 * SYNTAX
 * 
 * groups [options ... ]
 * 
 * OPTIONS
 * 
 * -u, --uid LOGIN
 * 		The user id, or LOGIN of the user 
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2010
 * @package com.noterik.bart.fs.fscommand.usermanagement
 * @access private
 *
 */
public class GroupsCommand extends UserCommandAdapter {
	/** The GroupsCommand's log4j Logger */
	private static Logger logger = Logger.getLogger(GroupsCommand.class);
	
	public String execute(String uri, String xml) {
		// get input parameters
		String domain = URIParser.getDomainFromUri(uri);
		Properties params = getInputParameters(xml);
		String uid = PropertiesHelper.getString(params, "u", null);
		uid = (uid==null) ? PropertiesHelper.getString(params, "uid", null) : uid;
		
		// get groups
		List<Group> groups = null;
		try {
			if(uid == null) {
				groups = gdao.getGroupList(domain);
			} else {
				groups = gdao.getUserGroupList(new UserKey(domain,uid));
			}
		} catch(DAOException e) {
			return FSXMLBuilder.getErrorMessage("500", e.getMessage(), e.getMessage(), "");
		}
		logger.debug("group list: "+groups);
		return UserManagerPOJOConverter.grouplist2xml(groups);
	}

	public ManualEntry man() {
		ManualEntry entry = new ManualEntry();
		entry.setDescription("List current groups, and user groups.");
		entry.setSyntax("groups [options ... ]");
		entry.addOption("u","uid","The user id, or LOGIN of the user");
		return entry;
	}
}
