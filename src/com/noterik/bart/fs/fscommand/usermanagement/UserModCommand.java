package com.noterik.bart.fs.fscommand.usermanagement;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.noterik.bart.fs.dao.DAOException;
import com.noterik.bart.fs.fscommand.ManualEntry;
import com.noterik.bart.fs.usermanager.GroupKey;
import com.noterik.bart.fs.usermanager.User;
import com.noterik.bart.fs.usermanager.UserKey;
import com.noterik.springfield.tools.PropertiesHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * Modify a user
 * 
 * SYNTAX
 * 
 * usermod [options ... ] -uid LOGIN
 * 
 * OPTIONS
 * 
 * -a, --append
 * 		Add the user to the supplemental group(s). Use only with -G option.
 * 
 * -g, --gid GROUP
 * 		The group name of the initial login group.
 * 
 * -G, --groups GROUP1[,GROUP2,GROUP3]
 * 		A list of groups which the user is also a member of. Each group is 
 * 		separated from the next by a comma. If the user is currently a member
 * 		of a group which is not listed, the user will be removed from the 
 * 		group. This behaviour can be changed via -a option, which appends the
 * 		current supplementary group list.
 * 
 * -p, --password PASSWORD
 * 		The users password.
 * 
 * -u, --uid LOGIN
 * 		The user's id, or LOGIN.
 * 
 * -firstname FIRSTNAME
 * 		The user's first name.
 * 
 * -lastname LASTNAME
 * 		The user's last name.
 * 
 * -email EMAIL
 * 		The user's email address.
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2010
 * @package com.noterik.bart.fs.fscommand.usermanagement
 * @access private
 *
 */
public class UserModCommand extends UserCommandAdapter {
	/** The UserModCommand's log4j Logger */
	private static Logger logger = Logger.getLogger(UserModCommand.class);
	
	public String execute(String uri, String xml) {
		boolean updated;
		
		// get input parameters
		String domain = URIParser.getDomainFromUri(uri);
		Properties params = getInputParameters(xml);
		boolean append = PropertiesHelper.getBoolean(params, "a", true);
		append &= PropertiesHelper.getBoolean(params, "append", true);
		String gid = PropertiesHelper.getString(params, "g", null);
		gid = (gid==null) ? PropertiesHelper.getString(params, "gid", null) : gid;
		String groups = PropertiesHelper.getString(params, "G", null);
		groups = (groups==null) ? PropertiesHelper.getString(params, "groups", null) : groups;
		String uid = PropertiesHelper.getString(params, "u", null);
		uid = (uid==null) ? PropertiesHelper.getString(params, "uid", null) : uid;
		String firstname = PropertiesHelper.getString(params, "firstname", null);
		String lastname = PropertiesHelper.getString(params, "lastname", null);
		String email = PropertiesHelper.getString(params, "email", null);
		String password = PropertiesHelper.getString(params, "p", null);
		password = (password==null) ? PropertiesHelper.getString(params, "password", null) : password;
		
		// parameter check
		if(domain==null || uid==null) {
			return FSXMLBuilder.getErrorMessage("400", "Bad Request", "Parameters missing", "");
		}
		
		// update the user
		try {
			User user = udao.read(new UserKey(domain, uid));
			if(firstname!=null)
				user.setFirstname(firstname);
			if(lastname!=null) 
				user.setLastname(lastname);
			if(email!=null)
				user.setEmail(email);
			if(password!=null) 
				user.setPassword(password);
			updated = udao.update(user);
			if(!updated) {
				return FSXMLBuilder.getErrorMessage("500", "Internal Server Error", "Unknown error", "");
			}
		} catch (DAOException e) {
			return FSXMLBuilder.getErrorMessage("500", "Internal Server Error", e.getMessage(), "");
		}
		
		// check for append parameter
		if(!append && (gid!=null || groups!=null)) {
			// TODO: remove user from all groups it is currently in
		}
		
		// add user to groups
		List<String> groupList = new ArrayList<String>();
		if(gid!=null) {
			groupList.add(gid);
		}
		if(groups!=null) {
			String[] parts = groups.split(",");
			for(int i=0; i<parts.length; i++) {
				String part = parts[i].trim();
				if(!part.equals(""))
					groupList.add(part);
			}
		}
		for(String group : groupList) {
			try {
				gdao.addUserToGroup(new UserKey(domain,uid), new GroupKey(domain,group));
			} catch (DAOException e) {
				logger.debug("",e);
			}
		}
		
		return FSXMLBuilder.getStatusMessage("200", "OK", "User was updated successfully", "");
	}
	
	public ManualEntry man() {
		ManualEntry entry = new ManualEntry();
		entry.setDescription("Modify a user");
		entry.setSyntax("usermod [options ... ] -uid LOGIN");
		entry.addOption("a","append","Add the user to the supplemental group(s). Use only with -G option.");
		entry.addOption("g","gid","The group name of the initial login group.");
		entry.addOption("G","groups","A list of groups which the user is also a member of. Each group is  separated from the next by a comma. If the user is currently a member of a group which is not listed, the user will be removed from the group. This behaviour can be changed via -a option, which appends the current supplementary group list.");
		entry.addOption("p","password","The users password.");
		entry.addOption("u","uid","The user's id, or LOGIN.");
		entry.addOption("firstname","The user's first name.");
		entry.addOption("lastname","The user's last name.");
		entry.addOption("email","The user's email address.");
		return entry;
	}
}
