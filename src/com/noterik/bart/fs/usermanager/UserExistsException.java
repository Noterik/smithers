package com.noterik.bart.fs.usermanager;

import com.noterik.bart.fs.dao.DAOException;

/**
 * Exception when a user already exists
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2011
 * @package com.noterik.bart.fs.usermanager
 * @access private
 *
 */
public class UserExistsException extends DAOException {
	public UserExistsException() {
		this("User exists");
	}
	public UserExistsException(String message) {
		super(message);
	}
}
