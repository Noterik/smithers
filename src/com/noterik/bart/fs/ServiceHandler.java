package com.noterik.bart.fs;

import org.restlet.representation.Representation;
import org.springfield.mojo.interfaces.*;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;

public class ServiceHandler implements ServiceInterface{
	public String getName() {
		return "smithers";
	}
	
	public String get(String path,String fsxml,String mimetype) {
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
		return (FSXMLRequestHandler.instance().handlePUT(path, value));
	}
}
