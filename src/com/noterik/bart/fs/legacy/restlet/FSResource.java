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

/*
 *
 * All resources of the Presentation Manager project extend this class
 * and therefore inherit its functions. The setup of this class is to enforce all resources
 * to have both an xml and an html representation.
 */
package com.noterik.bart.fs.legacy.restlet;

import java.util.List;
import org.restlet.resource.ServerResource;

public abstract class FSResource extends ServerResource {

	protected final int URI_TYPE_OTHER = 0;
	protected final int URI_TYPE_ALL_PROPERTIES = 1;
	protected final int URI_TYPE_PROPERTY = 2;
	protected final int URI_TYPE_PROPERTIES = 3;

	protected final String getDomainNameFromUrl() {
		return getRequest().getResourceRef().getSegments().get(3);
	}

	/**
	 * This function returns the last url segment from the url
	 *
	 * @return
	 */

	protected final String getCurrentUrlPart() {
		List<String> parts = getRequest().getResourceRef().getSegments();
		if (parts.isEmpty()) {
			return null;
		}
		return parts.get(parts.size() - 2);
	}

	/**
	 * This function returns the next to last url part
	 *
	 * @return
	 */

	protected final String getParentUrlPart() {
		List<String> parts = getRequest().getResourceRef().getSegments();
		if (parts.isEmpty()) {
			return null;
		}
		return parts.get(parts.size() - 3);
	}

	/**
	 * This function gets the url of the request eg
	 * domain/noterik/user/levi/video/1
	 *
	 * @return
	 */
	protected final String getRequestUrl() {
		String url = "";
		String uri = getRequest().getResourceRef().getPath();
		String uri2 = uri.substring(2);
		url = uri2.substring(uri2.indexOf("/"));

		// if the url ends in a /, remove it
		if (url.lastIndexOf("/") == url.length() - 1) {
			url = url.substring(0, url.lastIndexOf("/"));
		}

		return url;
	}

	/**
	 * This function returns the type from the 4th position to the end of the
	 * uri
	 *
	 * @return
	 */
	protected final String getTypeFromUrl4() {
		String type = "";
		List<String> parts = getRequest().getResourceRef().getSegments();
		type = parts.get(parts.size() - 4);
		return type;
	}

	/**
	 * This function returns the type from the 3rd position to the end of the
	 * uri
	 *
	 * @return
	 */
	protected final String getTypeFromUrl3() {
		String type = "";
		List<String> parts = getRequest().getResourceRef().getSegments();
		type = parts.get(parts.size() - 3);
		return type;
	}

	/**
	 * This function will test where the properties word is located in the uri
	 * and set a type to it accordingly
	 *
	 * @return
	 */
	protected final int getTypeOfUri() {
		int type = 0;
		List<String> parts = getRequest().getResourceRef().getSegments();
		if (!parts.contains("properties")) {
			// uri does not contain properties
			type = URI_TYPE_ALL_PROPERTIES;
		} else if (parts.get(parts.size() - 2).equals("properties")) {
			// uri is of type: .../properties/{prop}
			type = URI_TYPE_PROPERTY;
		} else if (parts.get(parts.size() - 1).equals("properties")) {
			// uri is of type: .../properties
			type = URI_TYPE_PROPERTIES;
		}
		return type;
	}

	protected final String getDomainOfUri(String uri) {
		String domain = "";
		String[] parts = uri.split("/");
		domain = parts[2];
		return domain;
	}

}