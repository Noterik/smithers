package com.noterik.bart.fs.fscommand.usermanagement;

import java.util.List;
import java.util.Properties;

import com.noterik.bart.fs.dao.DAOException;
import com.noterik.bart.fs.fscommand.ManualEntry;
import com.noterik.bart.fs.usermanager.User;
import com.noterik.bart.fs.usermanager.UserManagerPOJOConverter;
import com.noterik.springfield.tools.PropertiesHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * List current users
 * 
 * SYNTAX
 * 
 * users [options ... ]
 * 
 * OPTIONS
 * 
 * -sort SORT
 * 		Sort by a property SORT defined for all users. SORT_ORDER is the order by which
 * 		the users are sorted (ASC, DESC).
 * 
 * -sort-order SORT_ORDER
 * 		Sort by a property SORT defined for all users. SORT_ORDER is the order by which
 * 		the users are sorted (ASC, DESC), default ASC.
 * 
 * -start START
 * 		Limit the amount of results that are returned.
 * 
 * -limit LIMIT
 * 		Limit the amount of results that are returned.
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2010
 * @package com.noterik.bart.fs.fscommand.usermanagement
 * @access private
 *
 */
public class UsersCommand extends UserCommandAdapter {

	public String execute(String uri, String xml) {
		// get input parameters
		String domain = URIParser.getDomainFromUri(uri);
		Properties params = getInputParameters(xml);
		String sort = PropertiesHelper.getString(params, "sort", null);
		String sortOrder = PropertiesHelper.getString(params, "sort-order", null);
		int start = PropertiesHelper.getInteger(params, "start", 0);
		int limit = PropertiesHelper.getInteger(params, "limit", -1);
		
		// get user list
		List<User> users = null;
		try {
			if(start!=0 && limit!=-1) {
				if(sort!=null) {
					users = udao.getUserList(domain, start, limit, sort, sortOrder);
				} else {
					users = udao.getUserList(domain, start, limit);
				}
			} else if(sort!=null) {
				users = udao.getUserList(domain, sort, sortOrder);
			} else {
				users = udao.getUserList(domain);
			}
		} catch(DAOException e) {
			return FSXMLBuilder.getErrorMessage("500", e.getMessage(), e.getMessage(), "");
		}
		return UserManagerPOJOConverter.userlist2xml(users);
	}
	
	public ManualEntry man() {
		ManualEntry entry = new ManualEntry();
		entry.setDescription("List current users");
		entry.setSyntax("users [options ... ]");
		entry.addOption("sort","Sort by a property SORT defined for all users. SORT_ORDER is the order by which the users are sorted (ASC, DESC).");
		entry.addOption("sort-order","Sort by a property SORT defined for all users. SORT_ORDER is the order by which the users are sorted (ASC, DESC), default ASC.");
		entry.addOption("start","Limit the amount of results that are returned.");
		entry.addOption("limit","Limit the amount of results that are returned.");
		return entry;
	}
}
