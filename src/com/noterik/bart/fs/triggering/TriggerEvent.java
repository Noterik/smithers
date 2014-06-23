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