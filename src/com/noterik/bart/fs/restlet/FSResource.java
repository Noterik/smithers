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
package com.noterik.bart.fs.restlet;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;

import com.noterik.bart.fs.fscommand.CommandRequestHandler;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.bart.fs.fsxml.auth.DecisionEngine;
import com.noterik.bart.fs.fsxml.auth.TicketProxyDecisionEngine;
import com.noterik.bart.fs.script.FSScriptRequestHandler;
import com.noterik.bart.fs.tools.FSXMLHelper;
import com.noterik.bart.fs.type.MimeType;
import com.noterik.springfield.tools.XMLHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * This class is the first entry point of all the incoming requests.
 * The idea is to filter the requests by mimetype send them to the correct handler.
 *
 * At this point there are two different mimetypes supported:
 *
 * - text/fsxml
 * - application/fscommand
 *
 * The text/fsxml requests are forwared to the FSXMLRequestHandler
 *
 * The application/fscommand requests are forwarded to the CommandRequestHandler
 *
 * TODO (the part below)
 * Besides sending requests to the correct handler, the nodehandler for the current
 * node is fetched (by parsing the current uri). The node handler is responsible for
 * validating whether the current user is allowed to do the operation.
 *
 * @author Jaap Blom
 */

public class FSResource extends FSDefaultResource {	
	/** The FSResource's log4j Logger */
	private static Logger logger = Logger.getLogger(FSResource.class);
	
	/** the decision engine */
	private DecisionEngine decisionEngine;
	
	/**
	 * Called right after constructor of this resource (every request)
	 */
	@Override
	public void doInit() {
		super.doInit();
		decisionEngine = getDecisionEngine(getResourceUri());
	}
	
	// allowed actions: GET, PUT, POST, DELETE
	public boolean allowPut() {return true;}
	public boolean allowPost() {return true;}
	public boolean allowGet() {return true;}
	public boolean allowDelete() {return true;}
	private boolean httpblocked = true;

	/**
	 * Determine decision engine 
	 * 
	 * @param resourceUri
	 * @return
	 */
	private DecisionEngine getDecisionEngine(String resourceUri){
		DecisionEngine dEngine = null;
		String domain=URIParser.getDomainIdFromUri(resourceUri);
		String propUri="/domain/" + domain + "/config/authorization/properties/decisionengine";
		String engineName = FSXMLRequestHandler.instance().getPropertyValue(propUri);
		if (engineName==null) {
			dEngine =  new DecisionEngine();
		}else if (engineName.equals("TicketProxy")){
			dEngine = new TicketProxyDecisionEngine();
		}else{
			dEngine = new DecisionEngine();
		}
		return dEngine;
	}

	/**
	 * Get
	 */
	@Get
	public Representation doGet() {
		if (httpblocked) return null;
		logger.debug("GET: "+getResourceUri());
		Representation rep = null;
		if (!decisionEngine.decide(getRequest())){
			String error = FSXMLBuilder.getErrorMessage("403", "Permission denied", "Please log in","http://teamelements.noterik.com/team");
			rep = new StringRepresentation(error,MediaType.TEXT_XML);
		} else {
			String data = getRequestBodyData(getRequest().getEntity());	
			rep = FSXMLRequestHandler.instance().handleGET(getResourceUri(),data);
		}
		
		//set access control headers to allow cross domain communication
		Form responseHeaders = (Form) getResponse().getAttributes().get("org.restlet.http.headers");  
		if (responseHeaders == null)  
		{  
			responseHeaders = new Form();  
			getResponse().getAttributes().put("org.restlet.http.headers", responseHeaders);  
		}  
		
		responseHeaders.add("Access-Control-Allow-Origin", "*");  
		responseHeaders.add("Access-Control-Allow-Methods","GET, POST, PUT, DELETE, OPTIONS");
		responseHeaders.add("Access-Control-Allow-Headers", "Content-Type");
		return rep;
	}

	/**
	 * Post
	 */
	@Post
	public void doPost(Representation representation) {
		if (httpblocked) return;
		logger.debug("POST: "+getResourceUri());
		
		String data = getRequestBodyData(representation);
		String response = null;
		if (!decisionEngine.decide(getRequest())){
			String error = FSXMLBuilder.getErrorMessage("403", "Permission denied", "Please log in","http://teamelements.noterik.com/team");
			Representation rep = new StringRepresentation(error,MediaType.TEXT_XML);
			getResponse().setEntity(rep);
			return ;
		}
		if (data == null) {
			response = FSXMLBuilder.getErrorMessage("500", "The request data could not be found",
					"Please resend with the correct request data", "http://teamelements.noterik.com/team");
		} else {
			logger.debug("Post request: " + data);
			String uri = getResourceUri();
			MimeType mt = FSXMLHelper.getMimeTypeFromXml(data);
			if (mt == MimeType.MIMETYPE_FS_XML) {
				response = FSXMLRequestHandler.instance().handlePOST(uri, data);
			} else if (mt == MimeType.MIMETYPE_FS_COMMAND) {
				response = CommandRequestHandler.instance().handlePOST(uri, data);
			} else if (mt == MimeType.MIMETYPE_FS_SCRIPT) {
				response = FSScriptRequestHandler.instance().handlePOST(uri, data);
			} else {
				response = FSXMLBuilder.getErrorMessage("500", "Posting data of this mimetype is not supported",
						"Please resend with the correct mimetype (application/fscommand or text/fsxml)", "http://teamelements.noterik.com/team");
			}
		}
		logger.debug("returning response for POST request: " + response);
		
		// proper UTF-8 encoding of response
		DomRepresentation dr = null;
		try {
			dr = new DomRepresentation(MediaType.TEXT_XML);
			Document doc = DocumentHelper.parseText(response);
			dr.setDocument(XMLHelper.convert(doc));
		} catch (Exception e) {
			logger.error("",e);
			response = FSXMLBuilder.getErrorMessage("500", "Response data not valid",
					"", "http://teamelements.noterik.com/team");
			getResponse().setEntity(new StringRepresentation(response,MediaType.TEXT_XML));
		}
		getResponse().setEntity(dr);
		
		//set access control headers to allow cross domain communication
		Form responseHeaders = (Form) getResponse().getAttributes().get("org.restlet.http.headers");  
		if (responseHeaders == null)  
		{  
			responseHeaders = new Form();  
			getResponse().getAttributes().put("org.restlet.http.headers", responseHeaders);  
		}  
				
		responseHeaders.add("Access-Control-Allow-Origin", "*");  
		responseHeaders.add("Access-Control-Allow-Methods","GET, POST, PUT, DELETE, OPTIONS");
		responseHeaders.add("Access-Control-Allow-Headers", "Content-Type");
	}

	/**
	 * Put
	 */
	@Put
	public void doPut(Representation representation) {
		if (httpblocked) return;
		logger.debug("PUT: "+getResourceUri());
		
		String data = getRequestBodyData(representation);
		String response = null;
		if (!decisionEngine.decide(getRequest())){
			String error = FSXMLBuilder.getErrorMessage("403", "Permission denied", "Please log in","http://teamelements.noterik.com/team");
			Representation rep = new StringRepresentation(error,MediaType.TEXT_XML);
			getResponse().setEntity(rep);
			return;
		}
		if (data == null) {
			response = FSXMLBuilder.getErrorMessage("500", "The request data could not be found",
					"Please resend with the correct request data", "http://teamelements.noterik.com/team");
		} else {
			logger.debug("Put request: " + data);
			MimeType mt = FSXMLHelper.getMimeTypeFromXml(data);
			if (mt == MimeType.MIMETYPE_FS_XML) {
				response = FSXMLRequestHandler.instance().handlePUT(getResourceUri(), data);
			} else if (mt == MimeType.MIMETYPE_FS_SCRIPT) {
				response = FSScriptRequestHandler.instance().handlePUT(getResourceUri(), data);
			} else {
				response = FSXMLBuilder.getErrorMessage("403", "You can only PUT data of the mimetype text/fsxml",
						"Please resend with the correct mimetype", "http://teamelements.noterik.com/team");
			}
		}
		logger.debug("returning response for PUT request: " + response);
		
		// proper UTF-8 encoding of response
		DomRepresentation dr = null;
		try {
			dr = new DomRepresentation(MediaType.TEXT_XML);
			Document doc = DocumentHelper.parseText(response);
			dr.setDocument(XMLHelper.convert(doc));
		} catch (Exception e) {
			logger.error("",e);
			response = FSXMLBuilder.getErrorMessage("500", "Response data not valid",
					"", "http://teamelements.noterik.com/team");
			getResponse().setEntity(new StringRepresentation(response,MediaType.TEXT_XML));
		}
		getResponse().setEntity(dr);
		
		//set access control headers to allow cross domain communication
		Form responseHeaders = (Form) getResponse().getAttributes().get("org.restlet.http.headers");  
		if (responseHeaders == null)  
		{  
			responseHeaders = new Form();  
			getResponse().getAttributes().put("org.restlet.http.headers", responseHeaders);  
		}  
						
		responseHeaders.add("Access-Control-Allow-Origin", "*");  
		responseHeaders.add("Access-Control-Allow-Methods","GET, POST, PUT, DELETE, OPTIONS");
		responseHeaders.add("Access-Control-Allow-Headers", "Content-Type");
	}

	/**
	 * Delete
	 */
	@Delete
	public void doDelete() {
		if (httpblocked) return;
		
		logger.debug("DELETE: "+getResourceUri());
		
		if (!decisionEngine.decide(getRequest())){
			String error = FSXMLBuilder.getErrorMessage("403", "Permission denied", "Please log in","http://teamelements.noterik.com/team");
			Representation rep = new StringRepresentation(error,MediaType.TEXT_XML);
			getResponse().setEntity(rep);
			return ;
		}
		String data = getRequestBodyData(getRequest().getEntity());
		FSScriptRequestHandler.instance().handleDELETE(getResourceUri());
		String response = FSXMLRequestHandler.instance().handleDELETE(getResourceUri(),data);
		logger.debug("returning response for DELETE request: " + response);
		
		// proper UTF-8 encoding of response
		DomRepresentation dr = null;
		try {
			dr = new DomRepresentation(MediaType.TEXT_XML);
			Document doc = DocumentHelper.parseText(response);
			dr.setDocument(XMLHelper.convert(doc));
		} catch (Exception e) {
			logger.error("",e);
			response = FSXMLBuilder.getErrorMessage("500", "Response data not valid",
					"", "http://teamelements.noterik.com/team");
			getResponse().setEntity(new StringRepresentation(response,MediaType.TEXT_XML));
		}
		getResponse().setEntity(dr);
		
		//set access control headers to allow cross domain communication
		Form responseHeaders = (Form) getResponse().getAttributes().get("org.restlet.http.headers");  
		if (responseHeaders == null)  
		{  
			responseHeaders = new Form();  
			getResponse().getAttributes().put("org.restlet.http.headers", responseHeaders);  
		}  
						
		responseHeaders.add("Access-Control-Allow-Origin", "*");  
		responseHeaders.add("Access-Control-Allow-Methods","GET, POST, PUT, DELETE, OPTIONS");
		responseHeaders.add("Access-Control-Allow-Headers", "Content-Type");
	}

}