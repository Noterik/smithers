package com.noterik.bart.fs.fscommand;

public interface DynamicCommand {

	/**
	 * Runs a command and returns an fsxml message
	 * 
	 * @param xml
	 * @return
	 */
	public String run(String uri,String xml);
	
}
