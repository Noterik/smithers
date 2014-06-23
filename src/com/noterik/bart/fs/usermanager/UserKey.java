package com.noterik.bart.fs.usermanager;

import java.io.Serializable;

/**
 * UserKey for DAO interaction
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2010
 * @package com.noterik.bart.fs.usermanager
 * @access private
 *
 */
public class UserKey implements Serializable {
	private String domain;
	private String uid;
	
	/**
	 * @param domain
	 * @param uid
	 */
	public UserKey(String domain, String uid) {
		this.domain = domain;
		this.uid = uid;
	}
	
	/**
	 * @return the domain
	 */
	public String getDomain() {
		return domain;
	}
	
	/**
	 * @return the uid
	 */
	public String getUid() {
		return uid;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "UserKey [domain=" + domain + ", uid=" + uid + "]";
	}
	
}
