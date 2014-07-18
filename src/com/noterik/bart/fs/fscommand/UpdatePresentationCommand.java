/* 
* UpdatePresentationCommand.java
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

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.XMLHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;

public class UpdatePresentationCommand implements Command {
	/** Logger */
	private static Logger logger = Logger.getLogger(UpdatePresentationCommand.class);
	
	public String execute(String url, String xml) {	
		logger.debug("Updating properties of presentation "+url);
		logger.debug("Updating properties xml "+xml);
		
		Document doc = XMLHelper.asDocument(xml);
		List<Node> properties;
		
		//TODO: validate ticket
		
		//add every property in the xml supplied		
		properties = doc.selectNodes("//properties/*");
		
		for (Iterator<Node> it = properties.iterator(); it.hasNext(); ) {
			Node property = it.next();
			
			if (!property.getName().equals("ticket")) {			
				logger.debug("updating property "+property.getName()+" with value "+property.getText());
			
				FSXMLRequestHandler.instance().handlePUT(url+"/properties/"+property.getName(), property.getText());
			}
		}		
		return FSXMLBuilder.getFSXMLStatusMessage("The properties where successfully added", "", "");
	}
	
	public ManualEntry man() {
		return null;
	}
}
