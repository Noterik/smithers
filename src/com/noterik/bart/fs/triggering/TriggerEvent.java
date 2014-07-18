/* 
* TriggerEvent.java
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
package com.noterik.bart.fs.triggering;

import java.io.Serializable;

/**
 * Event container
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.triggering
 * @access private
 * @version $Id: TriggerEvent.java,v 1.4 2010-07-27 11:05:06 derk Exp $
 *
 */
public class TriggerEvent implements Serializable {

	private static final long serialVersionUID = 1929292L;
	private String uri;
	private String method;
	private String mimeType;
	private String requestData;

	public TriggerEvent(String uri, String method, String mimeType, String requestData) {
		this.uri = uri;
		this.method = method;
		this.mimeType = mimeType;
		this.requestData = requestData;
	}

	public String getMethod() {
		return method;
	}

	public String getRequestData() {
		return requestData;
	}

	public void setRequestData(String requestData) {
		this.requestData = requestData;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "TriggerEvent [method=" + method + ", uri=" + uri  + ", mimeType=" + mimeType
				+ ", requestData=" + requestData+ "]";
	}

}