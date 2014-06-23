package com.noterik.bart.fs.fscommand.usermanagement;

import java.util.Properties;

import com.noterik.bart.fs.dao.DAOException;
import com.noterik.bart.fs.fscommand.ManualEntry;
import com.noterik.bart.fs.usermanager.GroupKey;
import com.noterik.springfield.tools.PropertiesHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * Remove a group.
 * 
 * SYNTAX
 * 
 * groupdel [options ... ] -gid GROUP
 * 
 * OPTIONS
 * 
 * -g, --gid GROUP
 * 		The group's id.
 * 
 * -r, --remove (TODO)
 * 		Group's home directory will be removed, as well as all underlying directories 
 * 		and property nodes.
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2010
 * @package com.noterik.bart.fs.fscommand.usermanagement
 * @access private
 *
 */
public class GroupDelCommand extends UserCommandAdapter {

	public String execute(String uri, String xml) {
		// get input parameters
		String domain = URIParser.getDomainFromUri(uri);
		Properties params = getInputParameters(xml);
		String gid = PropertiesHelper.getString(params, "g", null);
		gid = (gid==null) ? PropertiesHelper.getString(params, "gid", null) : gid;
		boolean remove = PropertiesHelper.getBoolean(params, "r", true);
		remove &= PropertiesHelper.getBoolean(params, "remove", true);
		
		// check parameters
		if(domain==null || gid==null) {
			return FSXMLBuilder.getErrorMessage("400", "Bad Request", "Parameters missing", "");
		}
		
		// remove from usermanager
		try {
			boolean removed = gdao.delete(new GroupKey(domain, gid));
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
		
		return FSXMLBuilder.getStatusMessage("200", "OK", "Group was removed successfully", "");
	}
	
	public ManualEntry man() {
		ManualEntry entry = new ManualEntry();
		entry.setDescription("Remove a group.");
		entry.setSyntax("groupdel [options ... ] -gid GROUP");
		entry.addOption("g","gid","The group's id.");
		entry.addOption("r","remove","Group's home directory will be removed, as well as all underlying directories and property nodes.");
		return entry;
	}
}
