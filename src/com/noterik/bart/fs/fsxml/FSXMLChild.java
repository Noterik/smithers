/* 
* FSXMLChild.java
* 
* Copyright (c) 2012 Noterik B.V.
* 
* This file is part of smithers, related to the Noterik Springfield project.
*
* Smithers is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Smithers is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Smithers.  If not, see <http://www.gnu.org/licenses/>.
*/
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
