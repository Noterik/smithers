package com.noterik.bart.fs.fscommand;

/**
 * Custom exception for commands
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2011
 * @package com.noterik.bart.fs.fscommand
 * @access private
 *
 */
public class CommandException extends Exception {
	private static final long serialVersionUID = 1L;
	public CommandException(String message) {
		super(message);
	}
}
