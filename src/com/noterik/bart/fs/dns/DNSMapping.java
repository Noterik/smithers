package com.noterik.bart.fs.dns;

import org.restlet.representation.Representation;

public interface DNSMapping {
	/**
	 * get mapping and returns a fsxml message
	 * 
	 * @param uri
	 * @return
	 */
	public Representation getMapping(String uri);
	
	/**
	 * update mapping and returns a fsxml message
	 * @param uri
	 * @param xml
	 * @return
	 */
	public Representation updateMapping(String uri, String xml);
	
	/**
	 * delete mapping and returns a fsxml message
	 * @param uri
	 * @return
	 */
	public Representation deleteMapping(String uri);
}
