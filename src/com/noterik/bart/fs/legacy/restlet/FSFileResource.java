package com.noterik.bart.fs.legacy.restlet;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;

import com.noterik.bart.fs.ingest.IngestHandler;
import com.noterik.bart.fs.legacy.properties.PropertyHandler;
import com.noterik.bart.fs.legacy.restlet.FSResource;
import com.noterik.bart.fs.legacy.tools.XmlHelper;

public class FSFileResource extends FSResource {
	
	private static Logger logger = Logger.getLogger(FSFileResource.class);

	// allowed actions: POST, PUT, GET, DELETE
	public boolean allowPut() {
		return true;
	}

	public boolean allowPost() {
		return true;
	}

	public boolean allowGet() {
		return true;
	}

	public boolean allowDelete() {
		return true;
	}

	/**
	 * GET
	 */
	@Get
	public Representation doGet() {
		Representation result = null;
		logger.debug("i just received a GET in files!!");
		getResponse().setStatus(Status.CLIENT_ERROR_NOT_ACCEPTABLE, "Invalid method for this uri");
		return result;
	}

	/**
	 * POST
	 */
	@Post
	public void doPost(Representation representation) {
		logger.debug("Received a POST in FSFileResource");
		String xml = "";
		try {
			if (representation == null) {
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				getResponse().setEntity("<status>Error: the request data could not be read</status>",
						MediaType.TEXT_XML);
			} else {
				xml = representation.getText();
			}
		} catch (IOException e2) {
			e2.printStackTrace();
			return;
		}
		logger.debug("REPRESENTATION: " + xml);
		if (representation != null && xml != null) {
			String url = "";
			url = getRequestUrl();
			String domain = getDomainOfUri(url);
			logger.debug("url = " + url);
			// only create if it has not been created already
			if (!PropertyHandler.hasProperties(url)) {
				String ingestType = XmlHelper.getPropertyValue(xml, XmlHelper.PROP_ASSET_TYPE);
				if (ingestType.equals("video")) {					
					logger.debug("\ndomain is: " + domain);
					String response = "";
					response = IngestHandler.instance().ingestVideo(url, domain, xml);					
					getResponse().setEntity(new StringRepresentation(response));
				} else {
					if (ingestType.equals("image")) {						
						logger.debug("\ndomain is: " + domain);
						IngestHandler.instance().ingestImage(url, domain, xml);
						getResponse().setStatus(Status.SUCCESS_OK, "Image was successfully ingested");
					}
					 else {
						if (ingestType.equals("audio")) {							
							logger.debug("\ndomain is: " + domain);
							IngestHandler.instance().ingestAudio(url, domain, xml);
							getResponse().setStatus(Status.SUCCESS_OK, "Audio file was successfully ingested");
						}
						 else {
							 if(ingestType.equals("swf")){
								logger.debug("\ndomain is: " + domain);
								IngestHandler.instance().ingestBanner(url, domain, xml);
								getResponse().setStatus(Status.SUCCESS_OK, "Banner was successfully ingested");
							 }
						 }
					}
				}
			} else {
				getResponse().setStatus(Status.CLIENT_ERROR_NOT_ACCEPTABLE, "This file has already been ingested");
			}

		} else {
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_ACCEPTABLE, "Unsupported input format");
		}

	}

	/**
	 * PUT
	 */
	@Put
	public void doPut(Representation representation) {
		logger.debug("I got a PUT in files resource");
		getResponse().setStatus(Status.CLIENT_ERROR_NOT_ACCEPTABLE, "Invalid method for this uri");

	}

	/**
	 * DELETE
	 */
	@Delete
	public void doDelete() {
		logger.debug("I got a DELETE in files resource");
		getResponse().setStatus(Status.CLIENT_ERROR_NOT_ACCEPTABLE, "Invalid method for this uri");

	}

}