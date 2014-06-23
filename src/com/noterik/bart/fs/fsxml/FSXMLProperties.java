package com.noterik.bart.fs.fsxml;

import java.io.Serializable;

/**
 * Container and transfer object for FSXML properties node.
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2009
 * @package com.noterik.bart.fs.fsxml
 * @access private
 * @version $Id: FSXMLProperties.java,v 1.4 2009-12-09 15:36:58 derk Exp $
 *
 */
public class FSXMLProperties implements Serializable {	
	
	private static final long serialVersionUID = 21L;
	
	/** database fields */
	private String uri;
	private String referUri;
	private String type;
	private String mimetype;
	private String xml;
	
	/** filed for concurrent updates (to be removed) */
	private String oldXml;
	
	/**
	 * @param uri
	 * @param xml
	 * @param type
	 * @param mimetype
	 * @param referUri
	 */
	public FSXMLProperties(String uri, String referUri, String type,
			String mimetype, String xml) {
		this.uri = uri;
		this.referUri = referUri;
		this.type = type;
		this.mimetype = mimetype;
		this.xml = xml;
	}

	/**
	 * @return the oldXml
	 */
	public String getOldXml() {
		return oldXml;
	}

	/**
	 * @param oldXml the oldXml to set
	 */
	public void setOldXml(String oldXml) {
		this.oldXml = oldXml;
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
	 * @return the xml
	 */
	public String getXml() {
		return xml;
	}

	/**
	 * @param xml the xml to set
	 */
	public void setXml(String xml) {
		this.xml = xml;
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
	 * @return the mimetype
	 */
	public String getMimetype() {
		return mimetype;
	}

	/**
	 * @param mimetype the mimetype to set
	 */
	public void setMimetype(String mimetype) {
		this.mimetype = mimetype;
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
		return "FSXMLProperties [uri=" + uri + ", referUri=" + referUri + ", type=" + type + ", mimetype=" + mimetype + ", xml=" + xml + ", oldXml=" + oldXml + "]";
	}
	
}
