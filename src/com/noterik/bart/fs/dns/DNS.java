/* 
* FSResource.java
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
package com.noterik.bart.fs.dns;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;

import com.noterik.bart.fs.restlet.FSResource;
import com.noterik.springfield.tools.XMLHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;

public class DNS extends FSResource {
	/** The DNS log4j Logger */
	private static Logger logger = Logger.getLogger(DNS.class);
	private static String CLASS_BASE = "com.noterik.bart.fs.dns.mapping.";

	// allowed actions: GET,PUT,DELETE
	public boolean allowGet() {return true;}
	public boolean allowPut() {return true;}
	public boolean allowDelete() {return true;}
	public boolean allowPost() {return false;}
		
	/**
	 * Get
	 */
	@Get
	public Representation doGet() {
		logger.debug("received get");
		Representation rep = null;
		
		Object domainObj =  getRequest().getAttributes().get("domain");
		Object identifierObj = getRequest().getAttributes().get("identifier");
		String domain = domainObj == null ? null : domainObj.toString().toLowerCase();
		String identifier = identifierObj == null ? null : identifierObj.toString();
		if (domain == null) {
			String error = FSXMLBuilder.getErrorMessage("500", "Could not determine filesystem service", "Could not determine filesystem service. Please check if the domain is configured correctly.","http://blackboots.noterik.com/team");
			rep = new StringRepresentation(error,MediaType.TEXT_XML);
			return rep;
		}
		if (identifier == null) {
			String error = FSXMLBuilder.getErrorMessage("500", "Mapping identifier not found", "Please provide an identifier","");
			rep = new StringRepresentation(error,MediaType.TEXT_XML);
			return rep;
		}
		
		logger.debug("domain = "+domain+" identifier = "+identifier);

		String className = Character.toUpperCase(domain.charAt(0))+domain.substring(1)+"Mapping";
		try {
			Class c = Class.forName(CLASS_BASE+className);
			Object o = c.newInstance();
			if(o instanceof DNSMapping){
				return ((DNSMapping) o).getMapping(identifier);
			}
		} catch (ClassNotFoundException e) {
			//mapping selector not found
		} catch (IllegalAccessException e) {
			//mapping selector not found
		} catch (InstantiationException e) {
			//mapping selector not found
		}

		//error representation
		String error = FSXMLBuilder.getErrorMessage("500", "Could not determine filesystem service", "Could not determine filesystem service. Please check if the domain is configured correctly.","http://blackboots.noterik.com/team");
		rep = new StringRepresentation(error,MediaType.TEXT_XML);
		return rep;
	}
	
	/** 
	 * Put
	 */
	@Put
	public void doPut(Representation representation) {
		logger.debug("received put");
		Representation rep = null;		
		
		Object domainObj =  getRequest().getAttributes().get("domain");
		Object identifierObj = getRequest().getAttributes().get("identifier");
		String domain = domainObj == null ? null : domainObj.toString().toLowerCase();
		String identifier = identifierObj == null ? null : identifierObj.toString();
		
		if (domain == null) {
			String error = FSXMLBuilder.getErrorMessage("500", "Could not determine filesystem service", "Could not determine filesystem service. Please check if the domain is configured correctly.","http://blackboots.noterik.com/team");
			rep = new StringRepresentation(error,MediaType.TEXT_XML);
			getResponse().setEntity(rep);
			return;
		}
		if (identifier == null) {
			String error = FSXMLBuilder.getErrorMessage("500", "Mapping identifier not found", "Please provide an identifier","");
			rep = new StringRepresentation(error,MediaType.TEXT_XML);
			getResponse().setEntity(rep);
			return;
		}
		
		String data = getRequest().getEntityAsText();

		if (data == null) {
			String error = FSXMLBuilder.getErrorMessage("500", "Request data not found", "Please resend with the correct request data","");
			rep = new StringRepresentation(error,MediaType.TEXT_XML);
			getResponse().setEntity(rep);
			return;
		}
		
		Document fsxml = XMLHelper.asDocument(data);
		String refer = fsxml.selectSingleNode("/fsxml/properties/refer") == null ? null : fsxml.selectSingleNode("/fsxml/properties/refer").getText();
		if (refer == null) {
			String error = FSXMLBuilder.getErrorMessage("500", "Request data not found", "Please resend with the correct request data","");
			rep = new StringRepresentation(error,MediaType.TEXT_XML);
			getResponse().setEntity(rep);
			return;
		}
		
		String className = Character.toUpperCase(domain.charAt(0))+domain.substring(1)+"Mapping";
		try {
			Class c = Class.forName(CLASS_BASE+className);
			Object o = c.newInstance();
			if(o instanceof DNSMapping){
				getResponse().setEntity(((DNSMapping) o).updateMapping(refer, identifier));
				return;
			}
		} catch (ClassNotFoundException e) {
			//mapping selector not found
		} catch (IllegalAccessException e) {
			//mapping selector not found
		} catch (InstantiationException e) {
			//mapping selector not found
		}

		//error representation
		String error = FSXMLBuilder.getErrorMessage("500", "Could not determine filesystem service", "Could not determine filesystem service. Please check if the domain is configured correctly.","http://blackboots.noterik.com/team");
		rep = new StringRepresentation(error,MediaType.TEXT_XML);
		getResponse().setEntity(rep);
		return;
	}
	
	/**
	 * Delete
	 */
	@Delete
	public void doDelete(Representation representation) {
		logger.debug("received delete");
		Representation rep = null;		
		
		Object domainObj =  getRequest().getAttributes().get("domain");
		Object identifierObj = getRequest().getAttributes().get("identifier");
		String domain = domainObj == null ? null : domainObj.toString().toLowerCase();
		String identifier = identifierObj == null ? null : identifierObj.toString();
		
		if (domain == null) {
			String error = FSXMLBuilder.getErrorMessage("500", "Could not determine filesystem service", "Could not determine filesystem service. Please check if the domain is configured correctly.","http://blackboots.noterik.com/team");
			rep = new StringRepresentation(error,MediaType.TEXT_XML);
			getResponse().setEntity(rep);
			return;
		}
		if (identifier == null) {
			String error = FSXMLBuilder.getErrorMessage("500", "Mapping identifier not found", "Please provide an identifier","");
			rep = new StringRepresentation(error,MediaType.TEXT_XML);
			getResponse().setEntity(rep);
			return;
		}
		
		String className = Character.toUpperCase(domain.charAt(0))+domain.substring(1)+"Mapping";
		try {
			Class c = Class.forName(CLASS_BASE+className);
			Object o = c.newInstance();
			if(o instanceof DNSMapping){
				getResponse().setEntity(((DNSMapping) o).deleteMapping(identifier));
				return;
			}
		} catch (ClassNotFoundException e) {
			//mapping selector not found
		} catch (IllegalAccessException e) {
			//mapping selector not found
		} catch (InstantiationException e) {
			//mapping selector not found
		}

		//error representation
		String error = FSXMLBuilder.getErrorMessage("500", "Could not determine filesystem service", "Could not determine filesystem service. Please check if the domain is configured correctly.","http://blackboots.noterik.com/team");
		rep = new StringRepresentation(error,MediaType.TEXT_XML);
		getResponse().setEntity(rep);
		return;
	}
}
