package com.noterik.bart.fs.dns.mapping;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;

import com.noterik.bart.fs.dns.DNSMapping;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.fs.FSXMLBuilder;

/**
 * DANS mapping for DANS file names
 * 
 * @author Pieter van Leeuwen <p.vanleeuwen@noterik.nl>
 *
 */

public class DANSMapping implements DNSMapping {
	private static String uriStart ="/domain/dans/dns/";
	/** The DansMapping log4j Logger */
	private static Logger logger = Logger.getLogger(DANSMapping.class);
	
	public DANSMapping() {
		//constructor
	}
	
	public Representation updateMapping(String uri, String identifier) {
		Representation rep = null;

		String fsUri = uriStart+identifier;
		
		//write mapping at the place in the FS
		FSXMLRequestHandler.instance().handlePUT(fsUri+"/attributes", "<fsxml><attributes><referid>"+uri+"</referid></attributes></fsxml>");
		
		String result = FSXMLBuilder.getStatusMessage("400", "Updated", "The mapping was successfully updated", "");
		rep = new StringRepresentation(result,MediaType.TEXT_XML);
		return rep;		
	}
	
	public Representation getMapping(String identifier) {
		Representation rep = null;

		String fsUri = uriStart+identifier;
		
		//get file from FS
		String refer = FSXMLRequestHandler.instance().getRefer(fsUri);
		if (refer == null) {
			String error = FSXMLBuilder.getErrorMessage("500", "Could not provide mapping", "Identifier unknown","");
			rep = new StringRepresentation(error,MediaType.TEXT_XML);
			return rep;
		}
		
		HashMap<String, String> properties = new HashMap<String,String>();
		properties.put("refer", refer);
		
		rep = new StringRepresentation(FSXMLBuilder.wrapInFsxml("", properties), MediaType.TEXT_XML);
		return rep;
	}
	
	public Representation deleteMapping(String identifier) {
		Representation rep = null;
		
		String fsUri = uriStart+identifier;
		
		//delete mapping at the place in the FS
		FSXMLRequestHandler.instance().handleDELETE(fsUri,"");				
		
		String result = FSXMLBuilder.getStatusMessage("400", "Deleted", "The mapping was succesfully deleted", "");
		rep = new StringRepresentation(result,MediaType.TEXT_XML);
		return rep;	
	}
}
