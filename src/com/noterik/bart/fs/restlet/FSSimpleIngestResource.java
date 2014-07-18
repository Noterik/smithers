/* 
* FSSimpleIngestResource.java
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
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.bart.fs.ingest.IngestInputData;
import com.noterik.bart.fs.ingest.SimpleIngestHandler;
import com.noterik.springfield.tools.XMLHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.FSXMLParser;
import com.noterik.springfield.tools.fs.URIParser;
import com.noterik.springfield.tools.fs.model.config.FileIngestConfig;
import com.noterik.springfield.tools.fs.model.config.ingest.IngestConfig;

public class FSSimpleIngestResource extends FSDefaultResource {

	/** the FSSimpleIngestResource's log4j Logger */
	private static Logger logger = Logger.getLogger(FSSimpleIngestResource.class);

	// allowed actions: GET, POST
	public boolean allowPut() {return false;}
	public boolean allowPost() {return true;}
	public boolean allowGet() {return true;}
	public boolean allowDelete() {return false;}

	/**
	 * Get
	 */
	@Get
	public Representation doGet() {
		return new StringRepresentation("You are in the ingest resource");
	}

	/**
	 * Post
	 */
	@Post
	public void doPost(Representation representation) {
		logger.debug("Ingest script");
		logger.debug("URI: " + getResourceUri());
		String domain = URIParser.getDomainFromUri(getResourceUri());
		String confUri = "/domain/" + domain + "/config/ingest";
		logger.debug("CONF URI: " + confUri);
		Document doc = FSXMLRequestHandler.instance().getNodeProperties(confUri, false);
		String response = null;
		if (doc != null) {
			logger.debug("****************************************************\n" + doc.asXML());
			logger.debug("****************************************************\n");
			FileIngestConfig conf = FSXMLParser.getFileIngestConfigFromXml(doc.asXML());
			IngestConfig ic = FSXMLParser.getIngestConfigFromXml(doc.asXML());
			if (conf == null && ic == null) {
				logger.error("ERROR IN CONFIG");
			}
			String xml = this.getRequestBodyData(representation);
			logger.debug("----------------------------------------------------\n" + xml);
			logger.debug("----------------------------------------------------\n");
			Document input = XMLHelper.asDocument(xml);
			if (input == null) {
				logger.error("ERROR IN REQUEST");
			} else {
				logger.debug("GOING TO PROCESS INGEST");
				if (conf.getVideoSettings().getFtpConfig() != null) {
					logger.debug("--> OLD INGEST");
					response = processIngest(conf, input);
				} else {
					logger.debug("--> NEW INGEST");
					response = processIngestWithNewConfig(ic, input, domain);
				}
			}
		}
		if (response == null) {
			response = FSXMLBuilder.getErrorMessage("500", "The input XML was empty",
					"No input XML found, please resend with an XML", "http://teamelements.noterik.com/team");
		}
		logger.debug("RESPONSE FROM INGEST: " + response);
		getResponse().setEntity(new StringRepresentation(response));
	}

	private String processIngest(FileIngestConfig conf, Document doc) {
		IngestInputData iid = SimpleIngestHandler.instance().getInputVariables(doc);
		if (iid == null) {
			logger.debug("THE INPUT VARIABLES WERE INVALID");
			return null;
		}
		return SimpleIngestHandler.instance().ingestFile(conf, iid);
	}

	private String processIngestWithNewConfig(IngestConfig conf, Document doc, String domain) {
		IngestInputData iid = SimpleIngestHandler.instance().getInputVariables(doc);
		if (iid == null) {
			logger.error("THE INPUT VARIABLES WERE INVALID (new conf)");
			return null;
		}
		return SimpleIngestHandler.instance().ingestFileWithNewConfig(conf, iid, domain);
	}

}