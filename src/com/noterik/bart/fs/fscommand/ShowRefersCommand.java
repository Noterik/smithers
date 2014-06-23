/*
 * Created on Feb 13, 2009
 */
package com.noterik.bart.fs.fscommand;

import java.util.List;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;

public class ShowRefersCommand implements Command {
	
	public String execute(String uri, String xml) {
		List<String> refPars = FSXMLRequestHandler.instance().getReferParents(uri);
		StringBuffer fsxml = new StringBuffer();
		fsxml.append("<fsxml>");
		for(String s : refPars){
			fsxml.append("<parent>" + s +"</parent>");
		}
		fsxml.append("</fsxml>");
		return fsxml.toString();
	}
	
	public ManualEntry man() {
		return null;
	}
}
