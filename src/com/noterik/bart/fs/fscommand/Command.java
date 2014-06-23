/*
 * Created on Aug 27, 2008
 */
package com.noterik.bart.fs.fscommand;

/**
 * Command interface for filesystem commands
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2010
 * @package com.noterik.bart.fs.fscommand
 * @access private
 *
 */
public interface Command {

	/**
	 * Execute a command and returns an fsxml message
	 * 
	 * @param uri
	 * @param xml
	 * @return
	 */
	public String execute(String uri, String xml);

	/**
	 * Returns the manual entry for this command
	 * 
	 * @return
	 */
	public ManualEntry man();
}
