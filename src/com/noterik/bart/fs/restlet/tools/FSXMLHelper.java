/*
 * Created on Aug 27, 2008
 */
package com.noterik.bart.fs.tools;

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