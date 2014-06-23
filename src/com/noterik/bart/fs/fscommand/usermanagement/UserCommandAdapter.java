package com.noterik.bart.fs.fscommand.usermanagement;

import com.noterik.bart.fs.fscommand.CommandAdapter;
import com.noterik.bart.fs.usermanager.GroupDAO;
import com.noterik.bart.fs.usermanager.UserDAO;
import com.noterik.bart.fs.usermanager.UserManagerDAOFactory;

public abstract class UserCommandAdapter extends CommandAdapter {
	/**
	 * DAO Factory 
	 * TODO: move to configuration? 
	 */
	private static final UserManagerDAOFactory umdaof =  new UserManagerDAOFactory(UserManagerDAOFactory.BARNEY);
	protected static final UserDAO udao = umdaof.getUserDAO();
	protected static final GroupDAO gdao = umdaof.getGroupDAO();
}
