/* 
* DynamicCommandHandler.java
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


import org.apache.log4j.Logger;
import org.dom4j.Document;

import com.noterik.springfield.tools.XMLHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;

public class DynamicCommandHandler implements Command {
	/** Logger */
	private static Logger logger = Logger.getLogger(DynamicCommandHandler.class);
	
	/* POST	/domain/amersfoortbreedtv.devel/user/rutger/collection/default/presentation/14/
	 * 
	 * <fsxml mimetype="application/fscommand" id="dynamic">
	 * <properties>
	 * <handler>/presentation/playout/flash</handler>
	 * </properties>
	 * </fsxml>
	 */
	
	public String execute(String uri, String xml) {		
		System.out.println("url = "+uri);
		System.out.println("xml = "+xml);
		String handler = "";
		
		Document doc = XMLHelper.asDocument("<?xml version='1.0' encoding='UTF-8' standalone='no'?>"+xml);
		if(doc == null){
			return FSXMLBuilder.getErrorMessage("403", "The value you sent is not valid",
					"You have to POST a valid command XML", "http://teamelements.noterik.nl/team");
		}
		
		handler = doc.selectSingleNode("//properties/handler") == null ? "" : doc.selectSingleNode("//properties/handler").getText();	
		
		DynamicCommand obj = uri2object(handler);
		
		if (obj != null) {
			return obj.run(uri,xml);
		}
		
		return FSXMLBuilder.getErrorMessage("404", "Handler not found", "The dynamic handler you requested was not found", "http://teamelements.noterik.nl/team");
	}

	private DynamicCommand uri2object(String handler) {
		handler= handler.startsWith("/") ? handler.substring(1) : handler;
		
		String[] parts = handler.split("/");
		StringBuffer className = new StringBuffer(handler.length());
		className.append("com.noterik.bart.fs.fscommand");
		
		for (int i = 0; i < parts.length; i++) {
			className.append("."+parts[i]);
		}
		
		logger.debug("class = "+className.toString());
		
		try {
			Class c = Class.forName(className.toString());
			Object o = c.newInstance();
			if(o instanceof DynamicCommand){ 
				return (DynamicCommand) o;
			}
		} catch (ClassNotFoundException e) {
			logger.error("Class "+className.toString()+" not found");
		} catch (InstantiationException e) {
			logger.error("",e);
		} catch (IllegalAccessException e) {
			logger.error("",e);
		}
		
		return null;
	}
	
	public ManualEntry man() {
		return null;
	}
	
}
