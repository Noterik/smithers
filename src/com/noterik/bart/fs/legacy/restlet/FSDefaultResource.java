package com.noterik.bart.fs.legacy.restlet;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;

import com.noterik.bart.fs.legacy.properties.PropertyHandler;
import com.noterik.bart.fs.legacy.restlet.FSResource;

public class FSDefaultResource extends FSResource {
	private static final Logger logger = Logger.getLogger(FSDefaultResource.class);
	
	// allowed actions: POST, PUT, GET, DELETE 
	public boolean allowPut() {return true;}
	public boolean allowPost() {return true;}
	public boolean allowGet() {return true;}
	public boolean allowDelete() {return true;}
	
	/**
	 * GET  
	 */
	@Get
	public Representation doGet() {
		Representation result = null;
		
		if (getRequest().getEntity().getMediaType().equals(MediaType.TEXT_XML)) {
			// execute when the requested type equal XML
			
			String url = getRequestUrl();
			
			//String xmlField = PropertyHandler.getXMLfromProp(url);
			
			//System.out.println("\nProperties is: " + xmlField);
			
			// get the state
			String state = "";
				
			logger.info("i received a GET in default!!" );
			
			// return as string
			result = new StringRepresentation(state);
			
        }
        return result;
	}
	
	/**
	 * POST
	 */
	@Post
	public void doPost(Representation representation) {
		logger.info("I got a POST and i am in DefaultResource");
		String value = "";
		try {
			if (representation == null) {
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				getResponse().setEntity("<status>Error: the request data could not be read</status>",
						MediaType.TEXT_XML);
			} else {
				value = representation.getText();
			}
		} catch (IOException e2) {
			logger.error("",e2);
			return;
		}
		logger.debug("REPRESENTATION: " + value);
		if (representation != null && value != null) {
			logger.debug("xml was not null");
		}
				
	}
	
	/**
	 * PUT
	 */
	@Put
	public void doPut(Representation representation) {
		logger.info("I got a PUT");	
	}

	/**
	 * DELETE
	 */
	@Delete
	public void doDelete() {
		logger.info("I got a DELETE");
	}
}
