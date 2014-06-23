/*
 * Created on Aug 27, 2008
 */
package com.noterik.bart.fs.fscommand;

import org.dom4j.Document;

import com.noterik.bart.fs.tools.FSXMLHelper;
import com.noterik.springfield.tools.XMLHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;

public class CommandRequestHandler {

	private static CommandRequestHandler instance;

	private CommandRequestHandler() {

	}

	public static CommandRequestHandler instance() {
		if (instance == null) {
			instance = new CommandRequestHandler();
		}
		return instance;
	}

	public String handlePOST(String uri, String value) {
		Document doc = XMLHelper.asDocument(value);
		if (doc != null) {
			String id = FSXMLHelper.getCommandIdFromXml(doc);
			if(id == null){
				return FSXMLBuilder.getErrorMessage("403", "No command id found",
						"You have to supply a valid command id", "http://teamelements.noterik.nl/team");
			} else {
				String result = CommandHandler.instance().executeCommand(id, uri, value);
				if(result == null){
					return FSXMLBuilder.getErrorMessage("500", "Command not found",
							"The command you tried to execute does not exist", "http://teamelements.noterik.nl/team");
				} else {
					return result;
				}
			}
		} else {
			return FSXMLBuilder.getErrorMessage("403", "The value you sent is not valid",
					"You have to POST a valid command XML", "http://teamelements.noterik.nl/team");
		}
	}

}