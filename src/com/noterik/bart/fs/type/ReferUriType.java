/*
 * Created on Aug 11, 2008
 */
package com.noterik.bart.fs.type;

public enum ReferUriType {

	JAVA_URI ("java://"),
	HTTP_URI ("http://"),
	FS_URI ("");

	private String protocol;

	private ReferUriType(String protocol){
		this.protocol = protocol;
	}

	public String getProtocol(){
		return protocol;
	}

}