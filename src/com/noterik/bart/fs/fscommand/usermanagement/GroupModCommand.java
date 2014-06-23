package com.noterik.bart.fs.fscommand.usermanagement;

import java.util.Properties;

import com.noterik.bart.fs.dao.DAOException;
import com.noterik.bart.fs.fscommand.ManualEntry;
import com.noterik.bart.fs.usermanager.Group;
import com.noterik.bart.fs.usermanager.GroupKey;
import com.noterik.springfield.tools.PropertiesHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * Modify a group.
 * 
 * SYNTAX
 * 
 * groupmod [options ... ] -gid GROUP
 * 
 * OPTIONS
 * 
 * -g, --gid GROUP
 * 		The ID of the group to modify
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
public class GroupModCommand extends UserCommandAdapter {

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
		
		// modify group
		try {
			Group group = gdao.read(new GroupKey(domain, gid));
			if(groupname!=null) 
				group.setGroupname(groupname);
			boolean updated = gdao.update(group);
			if(!updated) {
				return FSXMLBuilder.getErrorMessage("500", "Internal Server Error", "Unknown error", "");
			}
		} catch (DAOException e) {
			return FSXMLBuilder.getErrorMessage("500", "Internal Server Error", e.getMessage(), "");
		}
		return FSXMLBuilder.getStatusMessage("200", "OK", "Group was updated successfully", "");
	}

	public ManualEntry man() {
		ManualEntry entry = new ManualEntry();
		entry.setDescription("Modify a group.");
		entry.setSyntax("groupmod [options ... ] -gid GROUP");
		entry.addOption("g","gid","The ID of the group to modify");
		entry.addOption("groupname","The group's name or description");
		return entry;
	}
}
