/*
 * Created on Aug 7, 2008
 */
package com.noterik.bart.fs.type;

public enum MimeType {

	MIMETYPE_FS_XML ("text/fsxml"),
	MIMETYPE_FS_SCRIPT ("application/fsscript"),
	MIMETYPE_FS_COMMAND ("application/fscommand");

	private String name;

	private MimeType (String name){
		this.name = name;
	}

	public String getName(){
		return name;
	}

}