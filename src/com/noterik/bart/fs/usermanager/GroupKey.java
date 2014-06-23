package com.noterik.bart.fs.usermanager;

import java.io.Serializable;

/**
 * GroupKey for DAO interaction
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2010
 * @package com.noterik.bart.fs.usermanager
 * @access private
 *
 */
public class GroupKey implements Serializable {
	private String domain;
	private String gid;
	
	/**
	 * @param domain
	 * @param gid
	 */
	public GroupKey(String domain, String gid) {
		this.domain = domain;
		this.gid = gid;
	}
	
	/**
	 * @return the domain
	 */
	public String getDomain() {
		return domain;
	}
	
	/**
	 * @return the gid
	 */
	public String getGid() {
		return gid;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "GroupKey [domain=" + domain + ", gid=" + gid + "]";
	}
	
}
