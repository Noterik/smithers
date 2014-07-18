/* 
* Playout.java
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
package com.noterik.bart.fs.fscommand.dynamic.playlist;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.fscommand.DynamicCommand;
import com.noterik.bart.fs.fscommand.DynamicCommandHandler;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * 
 * @author Daniel Ockeloen
 * 
 * Wrapper class if frontend asks for a dynamic playlist directly. Normally
 * its called from quickpresentation start but when a update is needed a frontend
 * can request it directly 
 *
 */
public class Playout implements DynamicCommand {
	/** Logger */
	private static Logger logger = Logger.getLogger(Playout.class);
	
	// clearly a dummy class for now until i finish the quickpresentation start.	
	public String run(String uri,String xml) {	
		logger.error("HELLO WORLD !");
		return "";
	}
}
