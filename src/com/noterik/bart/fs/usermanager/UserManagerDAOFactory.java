package com.noterik.bart.fs.usermanager;

/**
 * Factory class for user management DAOs
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2010
 * @package com.noterik.bart.fs.usermanager
 * @access private
 *
 */
public class UserManagerDAOFactory {
	/** List of DAO types supported by the factory */
	public static final int BARNEY = 1;
	
	/**
	 * The DAO factories type
	 */
	private int type;
	
	/**
	 * Constructor
	 * 
	 * @param type
	 */
	public UserManagerDAOFactory(int type) {
		this.type = type;
	}
	
	/**
	 * Returns a UserDAO
	 * 
	 * @return
	 */
	public UserDAO getUserDAO() {
		switch(type) {
		case BARNEY:
			return new UserDAOBarney();
		default :
			return null;
		}
	}
	
	/**
	 * Returns a GroupDAO
	 * 
	 * @return
	 */
	public GroupDAO getGroupDAO() {
		switch(type) {
		case BARNEY:
			return new GroupDAOBarney();
		default :
			return null;
		}
	}
}
