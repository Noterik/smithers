/* 
* FSXMLHelper.java
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
package com.noterik.bart.fs.restlet.tools;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.type.MimeType;
import com.noterik.springfield.tools.XMLHelper;

public class FSXMLHelper {

	public static final String XML_PROPERTIES = "properties";
	public static final String XML_ATTRIBUTES = "attributes";

	public static MimeType getMimeTypeFromXml(String xml) {
		return getMimeTypeFromXml(XMLHelper.asDocument(xml));
	}

	public static MimeType getMimeTypeFromXml(Document doc) {
		if (doc != null) {
			Node n = doc.selectSingleNode("//fsxml");
			if (n != null && n instanceof Element) {
				String mt = ((Element) n).attributeValue("mimetype");
				if (mt != null) {
					if (mt.equals(MimeType.MIMETYPE_FS_SCRIPT.getName())) {
						return MimeType.MIMETYPE_FS_SCRIPT;
					} else if (mt.equals(MimeType.MIMETYPE_FS_XML.getName())) {
						return MimeType.MIMETYPE_FS_XML;
					} else if (mt.equals(MimeType.MIMETYPE_FS_COMMAND.getName())) {
						return MimeType.MIMETYPE_FS_COMMAND;
					}
				}
			}
		}
		return MimeType.MIMETYPE_FS_XML;
	}

	public static String getCommandIdFromXml(String xml) {
		return getCommandIdFromXml(XMLHelper.asDocument(xml));
	}

	public static String getCommandIdFromXml(Document doc) {
		if (doc != null) {
			Node n = doc.selectSingleNode("//fsxml");
			if (n != null && n instanceof Element) {
				return ((Element) n).attributeValue("id");
			}
		}
		return null;
	}

	public static String getTypeOfXmlContent(String xml) {
		Document doc = XMLHelper.asDocument(xml);
		if (doc != null) {
			Node node = doc.selectSingleNode("//attributes");
			if (node == null) {
				return XML_PROPERTIES;
			} else {
				return XML_ATTRIBUTES;
			}
		}
		return XML_PROPERTIES;
	}

	public static String getValueFromXml(String xml, String xpath) {
		String value = "";
		Document doc = XMLHelper.asDocument(xml);
		if (doc != null) {
			Element elem = (Element) doc.selectSingleNode(xpath);
			if (elem != null) {
				value = elem.getText();
			}
		}
		return value;
	}

}