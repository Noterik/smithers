package com.noterik.bart.fs.fscommand.usermanagement;

import com.noterik.bart.fs.fscommand.ManualEntry;
import com.noterik.springfield.tools.fs.FSXMLBuilder;

public class ChownCommand extends UserCommandAdapter {
	
	public String execute(String uri, String xml) {
		return FSXMLBuilder.getErrorMessage("501", "Not Implemented", "Command not implemented", "");
	}
	
	public ManualEntry man() {
		return null;
	}
}
