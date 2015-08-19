/* 
* CommandRequestHandler.java
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
package com.noterik.bart.fs.fscommand;

import org.dom4j.Document;

import com.noterik.bart.fs.restlet.tools.FSXMLHelper;
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