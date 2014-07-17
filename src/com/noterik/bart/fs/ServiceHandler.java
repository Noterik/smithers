/* 
* ServiceHandler.java
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
package com.noterik.bart.fs;

import org.restlet.representation.Representation;
import org.springfield.mojo.interfaces.*;

import com.noterik.bart.fs.fscommand.CommandRequestHandler;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.bart.fs.script.FSScriptRequestHandler;
import com.noterik.bart.fs.tools.FSXMLHelper;
import com.noterik.bart.fs.type.MimeType;

public class ServiceHandler implements ServiceInterface{
	public String getName() {
		return "smithers";
	}
	
	public String get(String path,String fsxml,String mimetype) {
		if (path.endsWith("/")) path = path.substring(0,path.length()-1);
		Representation rep = FSXMLRequestHandler.instance().handleGET(path,fsxml);
		try {
			String body = rep.getText();
			return body;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public String put(String path,String value,String mimetype) {
		if (path.endsWith("/")) path = path.substring(0,path.length()-1);
		return (FSXMLRequestHandler.instance().handlePUT(path, value));
	}
	
	public String post(String path,String value,String mimetype) {
		if (path.endsWith("/")) path = path.substring(0,path.length()-1);
		System.out.println("Service Handeler = "+mimetype);
		if (mimetype==null || mimetype.equals("text/fsxml") || mimetype.equals("text/xml")) {
			return FSXMLRequestHandler.instance().handlePOST(path, value);
		} else if (mimetype.equals("application/fscommand")) {
			return CommandRequestHandler.instance().handlePOST(path, value);
		} else if (mimetype.equals("application/fsscript")) {
			return FSScriptRequestHandler.instance().handlePOST(path, value);
		}
		return null;
	}
	
	public String delete(String path,String value,String mimetype) {
		if (path.endsWith("/")) path = path.substring(0,path.length()-1);
		return (FSXMLRequestHandler.instance().handleDELETE(path, value));
	}
}
