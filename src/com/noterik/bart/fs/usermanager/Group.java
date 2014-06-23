package com.noterik.bart.fs.usermanager;

/**
 * Container for group data
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2010
 * @package com.noterik.bart.fs.fscommand.usermanagement
 * @access private
 *
 */
public class Group {
	/** URI template */
	public static final String GROUP_URI_TEMPLATE = "/domain/{domain}/group/{id}";
	
	private String domain;
	private String id;
	private String groupname;
	
	public Group(){}
	
	/**
	 * @return the domain
	 */
	public String getDomain() {
		return domain;
	}
	
	/**
	 * @param domain the domain to set
	 */
	public void setDomain(String domain) {
		this.domain = domain;
	}
	
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * @return the groupname
	 */
	public String getGroupname() {
		return groupname;
	}


	/**
	 * @param groupname the groupname to set
	 */
	public void setGroupname(String groupname) {
		this.groupname = groupname;
	}
	
	/**
	 * @return the URI
	 */
	public String getURI() {
		if(domain == null || id == null) {
			return null;
		}
		return GROUP_URI_TEMPLATE.replace("{domain}",domain).replace("{id}", id);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Group [domain=" + domain + ", id=" + id + ", groupname="
				+ groupname + "]";
	}
	
}
