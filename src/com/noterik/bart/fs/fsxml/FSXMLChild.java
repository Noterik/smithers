package com.noterik.bart.fs.fsxml;

import java.io.Serializable;

/**
 * Container and transfer object for FSXML child arcs.
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2009
 * @package com.noterik.bart.fs.fsxml
 * @access private
 *
 */
public class FSXMLChild implements Serializable {
	
	private static final long serialVersionUID = 22L;
	
	/** database fields */
	private String id;
	private String uri;
	private String referUri;
	private String type;
	
	/**
	 * Constructor.
	 * 
	 * @param id
	 * @param uri
	 * @param referUri
	 * @param type
	 */
	public FSXMLChild(String id, String uri, String referUri, String type) {
		this.uri = uri;
		this.id = id;
		this.type = type;
		this.referUri = referUri;
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

	/**
	 * @return the referUri
	 */
	public String getReferUri() {
		return referUri;
	}

	/**
	 * @param referUri the referUri to set
	 */
	public void setReferUri(String referUri) {
		this.referUri = referUri;
	}

	@Override
	public String toString() {
		return "FSXMLChild [id=" + id + ", uri=" + uri + ", referUri=" + referUri + ", type="+ type + "]";
	}
	
}
