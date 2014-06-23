package com.noterik.bart.fs.fsxml;

import java.io.Serializable;

/**
 * Child key container.
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2009
 * @package com.noterik.bart.fs.fsxml
 * @access private
 *
 */
public class FSXMLChildKey implements Serializable {
	
	private String uri;
	private String type;
	private String id;
	
	/**
	 * Constructor.
	 * 
	 * @param uri
	 * @param type
	 * @param id
	 */
	public FSXMLChildKey(String uri, String type, String id) {
		this.uri = uri;
		this.type = type;
		this.id = id;
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
	 * @return the uri
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * @param uri the uri to set
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "FSXMLChildKey [uri=" + uri + ", type=" + type + ", id=" + id + "]";
	}
	
}
