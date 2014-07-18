/* 
* DANSMapping.java
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
 * generate a md5 hash of the filename and divide this up in portion
 * to get an evenly balanced distribution.
 * Downside is that it's not very human readable friendly
 * 
 * @author Pieter van Leeuwen <p.vanleeuwen@noterik.nl>
 *
 */

public class DANSMapping implements DNSMapping {
	private static char[] SEPERATORS = {':', '-'};
	private static int MIN_NUM_OCCURENCES = 2;
	private static String uriStart ="/domain/dans/dns";
	/** The DANSMapping log4j Logger */
	private static Logger logger = Logger.getLogger(DANSMapping.class);
	
	public DANSMapping() {
		//constructor
	}
	
	public Representation updateMapping(String uri, String file) {
		Representation rep = null;
		
		if (countOccurrences(file, SEPERATORS[1]) < MIN_NUM_OCCURENCES) {
			//could not provide DANS mapping
			String error = FSXMLBuilder.getErrorMessage("500", "Could not provide DANS mapping", "Please provide a longer identifier","");
			rep = new StringRepresentation(error,MediaType.TEXT_XML);
			return rep;
		}
		
		String fileMapping = mapToIdentifier(file).toLowerCase();
		if (fileMapping.equals("")) {
			String error = FSXMLBuilder.getErrorMessage("500", "Could not provide DANS mapping", "Please provide a longer identifier","");
			rep = new StringRepresentation(error,MediaType.TEXT_XML);
			return rep;
		}
		String fsUri = uriStart+fileMapping;
		
		//write mapping at the place in the FS
		FSXMLRequestHandler.instance().handlePUT(fsUri+"/attributes", "<fsxml><attributes><referid>"+uri+"</referid></attributes></fsxml>");
		
		String result = FSXMLBuilder.getStatusMessage("400", "Updated", "The mapping was successfully updated", "");
		rep = new StringRepresentation(result,MediaType.TEXT_XML);
		return rep;		
	}
	
	public Representation getMapping(String identifier) {
		Representation rep = null;

		String fileMapping = mapToIdentifier(identifier);
		if (fileMapping.equals("")) {
			String error = FSXMLBuilder.getErrorMessage("500", "Could not provide DANS mapping", "Please provide a longer identifier","");
			rep = new StringRepresentation(error,MediaType.TEXT_XML);
			return rep;
		}
		logger.debug("fileMapping = "+fileMapping);
		String fsUri = uriStart+fileMapping;
		
		//get file from FS
		String refer = FSXMLRequestHandler.instance().getRefer(fsUri);
		if (refer == null) {
			String error = FSXMLBuilder.getErrorMessage("500", "Could not provide DANS mapping", "Identifier unknown","");
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
		
		String fileMapping = mapToIdentifier(identifier);
		if (fileMapping.equals("")) {
			String error = FSXMLBuilder.getErrorMessage("500", "Could not provide DANS mapping", "Please provide a longer identifier","");
			rep = new StringRepresentation(error,MediaType.TEXT_XML);
			return rep;
		}
		logger.debug("fileMapping = "+fileMapping);		
		String fsUri = uriStart+fileMapping;
		
		//write mapping at the place in the FS
		FSXMLRequestHandler.instance().handleDELETE(fsUri,"");				
		
		String result = FSXMLBuilder.getStatusMessage("400", "Deleted", "The mapping was succesfully deleted", "");
		rep = new StringRepresentation(result,MediaType.TEXT_XML);
		return rep;	
	}
	
	private static int countOccurrences(String haystack, char needle)
	{
	    int count = 0;
	    for (int i=0; i < haystack.length(); i++)
	    {
	        if (haystack.charAt(i) == needle)
	        {
	             count++;
	        }
	    }
	    return count;
	}
		
	private String mapToIdentifier(String file) {
		StringBuffer mapping = new StringBuffer("/");
		
		int pos = file.lastIndexOf(SEPERATORS[0])+1;
		for (int i = 0; i < 3; i++) {
			int position = i < 2 ? file.indexOf(SEPERATORS[1], pos) : file.lastIndexOf(SEPERATORS[1])+1;
			logger.debug("pos = "+pos+" position = "+position);
			if (position == -1 || position < pos) {
				//too short to get a good mapping :-(
				logger.debug("not enough data in iteration "+i);				
				return "";
			} else if(pos == position) {
				//last item
				mapping.append(file.substring(pos));
			} else {
				mapping.append(file.substring(pos, Math.min(pos+3,position))+"/");
			}
			pos = i < 1 ? position+1 : file.lastIndexOf(SEPERATORS[1])+1;			
		}
		logger.debug("maptoidentifier = "+mapping.toString());
		return mapping.toString();
	}
}
