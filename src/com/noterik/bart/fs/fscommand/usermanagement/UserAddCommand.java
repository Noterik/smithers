package com.noterik.bart.fs.fscommand.usermanagement;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.noterik.bart.fs.dao.DAOException;
import com.noterik.bart.fs.fscommand.ManualEntry;
import com.noterik.bart.fs.usermanager.GroupKey;
import com.noterik.bart.fs.usermanager.User;
import com.noterik.bart.fs.usermanager.UserExistsException;
import com.noterik.bart.fs.usermanager.UserKey;
import com.noterik.springfield.tools.PropertiesHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * Create a new user
 * 
 * SYNTAX
 * 
 * useradd [options ... ] -uid LOGIN
 * 
 * OPTIONS
 * 
 * -g, --gid GROUP
 * 		The group name of the initial login group.
 * 
 * -G, --groups GROUP1[,GROUP2,GROUP3]
 * 		A list of groups which the user is also a member of. Each group is 
 * 		separated from the next by a comma.
 * 
 * -M
 * 		The user's home directory will not be created (TODO)
 * 
 * -m, --create-home
 * 		The user's home directory will be created if it does not exist (TODO)
 * 
 * -p, --password PASSWORD
 * 		The users password.
 * 
 * -u, --uid LOGIN
 * 		The user's id, or LOGIN
 * 
 * -firstname FIRSTNAME
 * 		The user's first name
 * 
 * -lastname LASTNAME
 * 		The user's last name
 * 
 * -email EMAIL
 * 		The user's email address
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2010
 * @package com.noterik.bart.fs.fscommand.usermanagement
 * @access private
 *
 */
public class UserAddCommand extends UserCommandAdapter {
	/** The UserAddCommand's log4j Logger */
	private static Logger logger = Logger.getLogger(UserAddCommand.class);
	
	public String execute(String uri, String xml) {
		// get input parameters
		String domain = URIParser.getDomainFromUri(uri);
		Properties params = getInputParameters(xml);
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
		if(domain==null || uid==null || firstname==null || lastname==null || email==null || password==null) {
			return FSXMLBuilder.getErrorMessage("400", "Bad Request", "Parameters missing", "");
		}
		
		// create a user
		User user = new User();
		user.setDomain(domain);
		user.setId(uid);
		user.setFirstname(firstname);
		user.setLastname(lastname);
		user.setEmail(email);
		user.setPassword(password);
		try {
			boolean created = udao.create(user);
			if(!created) {
				return FSXMLBuilder.getErrorMessage("500", "Internal Server Error", "Unknown error", "");
			}
		} catch (DAOException e) {
			if(e instanceof UserExistsException) {
				return FSXMLBuilder.getErrorMessage("409", "Conflict", e.getMessage(), "");
			}
			return FSXMLBuilder.getErrorMessage("500", "Internal Server Error", e.getMessage(), "");
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
		
		return FSXMLBuilder.getStatusMessage("200", "Created", "User was created successfully", "");
	}
	
	public ManualEntry man() {
		ManualEntry entry = new ManualEntry();
		entry.setDescription("Create a new user");
		entry.setSyntax("useradd [options ... ] -uid LOGIN");
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
