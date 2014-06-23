package com.noterik.bart.fs.restlet;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.restlet.representation.Representation;
import org.restlet.resource.ServerResource;


public abstract class FSDefaultResource extends ServerResource {

	/** The FSDefaultResource's log4j Logger */
	private static Logger logger = Logger.getLogger(FSDefaultResource.class);

	protected final String getResourceUri() {
		String path = getRequest().getResourceRef().getPath();
		String uri2 = path.substring(2);
		String uri = uri2.substring(uri2.indexOf("/"));
		if (uri.lastIndexOf("/") == uri.length() - 1) {
			uri = uri.substring(0, uri.lastIndexOf("/"));
		}
		return uri;
	}

	protected String getRequestBodyData(Representation representation) {
		String data = null;
		try {
			if (representation != null) {
				data = representation.getText();
			}
		} catch (IOException e2) {
			logger.error("Could not get request body data",e2);
			return null;
		}
		return data;
	}
}