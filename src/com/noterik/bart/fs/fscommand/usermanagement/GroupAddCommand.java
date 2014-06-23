package com.noterik.bart.fs.fscommand.usermanagement;

import java.util.Properties;

import com.noterik.bart.fs.dao.DAOException;
import com.noterik.bart.fs.fscommand.ManualEntry;
import com.noterik.bart.fs.usermanager.Group;
import com.noterik.springfield.tools.PropertiesHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * Create a new group
 * 
 * SYNTAX
 * 
 * groupadd [options ... ] -gid GROUP
 * 
 * OPTIONS
 * 
 * -g, --gid GROUP
 * 		The ID of the group to create
 * 
 * -groupname GROUP_NAME
 * 		The group's name or description
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2010
 * @package com.noterik.bart.fs.fscommand.usermanagement
 * @access private
 *
 */
public class GroupAddCommand extends UserCommandAdapter {

	public String execute(String uri, String xml) {
		// get input parameters
		String domain = URIParser.getDomainFromUri(uri);
		Properties params = getInputParameters(xml);
		String gid = PropertiesHelper.getString(params, "g", null);
		gid = (gid==null) ? PropertiesHelper.getString(params, "gid", null) : gid;
		String groupname = PropertiesHelper.getString(params, "groupname", "N/A");
		
		// parameter check
		if(domain==null || gid==null) {
			return FSXMLBuilder.getErrorMessage("400", "Bad Request", "Parameters missing", "");
		}
		
		// create new group
		Group group = new Group();
		group.setDomain(domain);
		group.setId(gid);
		group.setGroupname(groupname);
		try {
			boolean created = gdao.create(group);
			if(!created) {
				return FSXMLBuilder.getErrorMessage("500", "Internal Server Error", "Unknown error", "");
			}
		} catch (DAOException e) {
			return FSXMLBuilder.getErrorMessage("500", "Internal Server Error", e.getMessage(), "");
		}
		return FSXMLBuilder.getStatusMessage("200", "Created", "Group was created successfully", "");
	}
	
	public ManualEntry man() {
		ManualEntry entry = new ManualEntry();
		entry.setDescription("Create a new group");
		entry.setSyntax("groupadd [options ... ] -gid GROUP");
		entry.addOption("g","gid","The ID of the group to create");
		entry.addOption("groupname","The group's name or description");
		return entry;
	}
	
}
