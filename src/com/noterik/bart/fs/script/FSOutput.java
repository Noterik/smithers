/* 
* FSOutput.java
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
package com.noterik.bart.fs.script;

import java.io.Serializable;

import org.dom4j.Document;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;

public class FSOutput implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1222L;
	/**
	 * pointer to parent script 
	 */
	private FSScript script;
	
	public FSOutput(FSScript script) {
		this.script = script;
	}
	
	/**
	 * Set the output element of the script
	 * @param doc
	 */
	public void setOutput(Document doc) {
		setOutput(doc.asXML());
	}
	
	/**
	 * Set the output element of the script
	 * @param xml
	 */
	public void setOutput(String xml) {
		setOutput("1",xml);
	}
	
	/**
	 * Set the output element of the script
	 * @param id
	 * @param xml
	 */
	public void setOutput(String id, String xml) {
		String uri = script.getID() + "/output/"+id;
		FSXMLRequestHandler.instance().saveFsXml(uri, xml, "PUT", false);  // do not send trigger events
	}
}