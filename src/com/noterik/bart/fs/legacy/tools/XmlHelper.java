/* 
* XmlHelper.java
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
package com.noterik.bart.fs.legacy.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.DOMWriter;
import org.restlet.data.MediaType;
import org.restlet.ext.xml.DomRepresentation;

public class XmlHelper {
	/** the XmlHelper's log4j logger */
	private static Logger logger = Logger.getLogger(XmlHelper.class);
	
	//assettype, source, collection, data, destination
	public static final String PROP_ASSET_TYPE = "assettype";
	public static final String PROP_SOURCE = "source";
	public static final String PROP_COLLECTION = "collection";
	public static final String PROP_DATA = "data";
	public static final String PROP_DESTINATION = "destination";
	public static final String PROP_SUB_NAME = "name";
	public static final String PROP_RAW_INDEX = "rawindex";

	/**
	 * This function sets the value to the properties file in the path passed as
	 * parameter
	 *
	 * @param xml
	 *            content of the properties file
	 * @param xpath
	 *            xpath of the property to set
	 * @param value
	 *            value to assign to the property
	 * @return xml String updated content of the properties file
	 */
	public static String setValueOfProperty(String xml, String path, String value) {
		String xpath = "/fsxml/" + path;
		Document propdoc = null;
		try {
			propdoc = DocumentHelper.parseText(xml);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		Element elem = (Element) propdoc.selectSingleNode(xpath);
		if (elem != null) {
			elem.setText(value);
		}
		Element props = (Element) propdoc.selectSingleNode("/fsxml");
		xml = props.asXML();
		return xml;
	}

	/**
	 * This function returns a Document with the value of the property set by
	 * path
	 *
	 * @param xml
	 *            String containing the property file
	 * @param path
	 *            The path for the desired value
	 * @return Document with the value of the property
	 */
	public static Document getValueOfProperty(String xml, String path) {
		String xpath = "/fsxml/" + path;
		Document doc = null;
		try {
			doc = DocumentHelper.parseText(xml);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		Element elem = (Element) doc.selectSingleNode(xpath).clone();
		Document newDoc = DocumentHelper.createDocument();
		Element root = DocumentHelper.createElement("fsxml");
		newDoc.setRootElement(root);
		Element fsxml = (Element) newDoc.selectSingleNode("/fsxml");
		fsxml.add(elem);
		return newDoc;
	}

	/**
	 * This function will add a property with the specified value to the xml
	 * passed as parameter
	 *
	 * @param xml
	 * @param property
	 * @param value
	 * @return
	 */
	public static String addPropertyValue(String xml, String property, String value) {
		Document doc = null;
		try {
			doc = DocumentHelper.parseText(xml);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		Element elem = (Element) doc.selectSingleNode("/fsxml/properties");
		elem.addElement(property).addText(value);
		Element fsxml = (Element) doc.selectSingleNode("/fsxml");
		xml = fsxml.asXML();
		return xml;
	}

	/**
	 * This function will check if a given property is defined
	 *
	 * @param xml
	 * @param property
	 * @return
	 */
	public static boolean propertyExists(String xml, String property) {
		boolean exists = false;
		Document doc = null;
		try {
			doc = DocumentHelper.parseText(xml);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		String xpath = "/fsxml/properties/" + property;
		Element elem = (Element) doc.selectSingleNode(xpath);
		if (elem != null) {
			exists = true;
		}
		return exists;
	}

	/**
	 * Converts dom4j Document to org.w3c Document
	 *
	 * @param doc1
	 * @return
	 */

	public static org.w3c.dom.Document convert(Document doc1) {
		if (doc1 == null) {
			return null;
		}
		DOMWriter writer = new DOMWriter();
		org.w3c.dom.Document doc2 = null;
		try {
			doc2 = writer.write(doc1);
		} catch (DocumentException e) {
		}
		return doc2;
	}

	public static String getPropertyValue(String xml, String propertyName) {
		String value = "";
		Document doc = null;
		try {
			doc = DocumentHelper.parseText(xml);			
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		Node propNode = null;
		propNode = doc.selectSingleNode("//" + propertyName);
		if(propNode != null) value = propNode.getText() ;
		return value;
	}

	/**
	 * This function will set the extension value (in the XML) obtained from the
	 * ingested file.
	 *
	 * @param xml
	 * @param ext
	 * @return
	 */
	public static String setExtensionProperty(String xml, String ext) {
		Document doc = null;
		try {
			doc = DocumentHelper.parseText(xml);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		if (doc.selectSingleNode("//extension") != null) {
			doc.selectSingleNode("//extension").setText(ext);
		} else {
			Node props = doc.selectSingleNode("//properties");
			if (props != null) {
				((Element) props).addElement("extension");
				doc.selectSingleNode("//extension").setText(ext);
			}
		}
		if (doc != null) {
			xml = doc.selectSingleNode("/fsxml").asXML();
		}
		return xml;
	}

	/**
	 * This function will set the mount value (in the XML)
	 *
	 * @param xml
	 * @param ext
	 * @return
	 */
	public static String setMountProperty(String xml, String mount) {
		Document doc = null;
		try {
			doc = DocumentHelper.parseText(xml);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		if (doc.selectSingleNode("//mount") != null) {
			doc.selectSingleNode("//mount").setText(mount);
		} else {
			Node props = doc.selectSingleNode("//properties");
			if (props != null) {
				((Element) props).addElement("mount");
				doc.selectSingleNode("//mount").setText(mount);
			}
		}
		if (doc != null) {
			xml = doc.selectSingleNode("/fsxml").asXML();
		}
		return xml;
	}

	public static String getClassName(String xml) {
		String dest = "";
		Document doc = null;
		try {
			doc = DocumentHelper.parseText(xml);
			dest = doc.selectSingleNode("//class").getText();
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return dest;
	}


	/**
	 * This function will extract the props part of the xml String
	 * passed as parameter and return it as fsxml compatible properties.
	 *
	 * If the node is not found, a default (empty) node will be returned
	 *
	 * @param xml
	 * @return
	 */
	public static String getPropsFromXml(String xml){
		String props = "<fsxml>" +
							"<properties>" +
							"</properties>" +
						"</fsxml>";

		Document doc = null;

		try {
			doc = DocumentHelper.parseText(xml);
		} catch (DocumentException e) {
			logger.error("",e);
		}

		Element properties = (Element) doc.selectSingleNode("/fsxml/ingest/props/properties").clone();

		if(properties != null){

			Document newDoc = DocumentHelper.createDocument();
			Element fsxml = (Element) newDoc.addElement("fsxml");
			fsxml.add(properties);

			props = fsxml.asXML();
		}
		return props;
	}

	/**
	 * this function will build a representation with the error message passed
	 * as argument. The result will be an xml file
	 *
	 * @param error
	 * @return
	 */
	public static DomRepresentation buildErrorRep(String error){
		DomRepresentation res = null;

		try {
			res = new DomRepresentation(MediaType.TEXT_XML);
			Document errDoc = DocumentHelper.parseText("<error>" + error + "</error>");
			res.setDocument(XmlHelper.convert(errDoc));
		} catch (IOException e) {
			logger.error("",e);
		} catch (DocumentException e) {
			logger.error("",e);
		}
		return res;
	}


	/**
	 * this function will return a boolean corresponding to the use or not of the FTP
	 * @param ingest
	 * @param type
	 * @return
	 */
	public static boolean useFtp(String ingest, String type){
		boolean use = false;

		Document doc = null;

		try {
			doc = DocumentHelper.parseText(ingest);
		} catch (DocumentException e) {
			logger.error("",e);
		}

		String xpath = "/fsxml/properties/" + type + "/ftp/enabled";
		Element isEnabled = (Element) doc.selectSingleNode(xpath);

		if(isEnabled != null){
			String enabled = isEnabled.getText();
			if(enabled.equals("true"))
				use = true;
		}
		return use;
	}
	
	
	/**
	 * this function will return a boolean corresponding to the use or not the backup
	 * server for FTP
	 * @param ingest
	 * @param type
	 * @return
	 */
	public static boolean useBackup(String ingest){
		boolean use = false;

		Document doc = null;

		try {
			doc = DocumentHelper.parseText(ingest);
		} catch (DocumentException e) {
			logger.error("",e);
		}

		String xpath = "/fsxml/properties/image/ftp/backup";
		Element isEnabled = (Element) doc.selectSingleNode(xpath);

		if(isEnabled != null){
			String enabled = isEnabled.getText();
			if(enabled.equals("true"))
				use = true;
		}
		return use;
	}

	/**
	 * this function will get the value in the given xpath
	 *
	 * @param ingest
	 * @param xpath
	 * @return
	 */
	public static String getValueFromIngest(String ingest, String xpath){
		Document doc = null;
		String value = "";

		try {
			doc = DocumentHelper.parseText(ingest);
		} catch (DocumentException e) {
			logger.error("",e);
		}

		Element elem = (Element) doc.selectSingleNode(xpath);

		if(elem != null)
			value = elem.getText();

		return value;
	}


	/**
	 * this function will return a list with the raw elements for this type
	 * @param ingest
	 * @param type
	 * @return
	 */
	public static List<Element> getRaws(String ingest, String type){
		List<Element> elems = new ArrayList<Element>();

		Document doc = null;

		logger.debug("ingest: " + ingest);
		logger.debug("type: " + type);
		
		try {
			doc = DocumentHelper.parseText(ingest);
		} catch (DocumentException e) {
			logger.error("",e);
		}
		String xpath = "/fsxml/properties/" + type + "/raw" + type;

		elems = doc.selectNodes(xpath);

		return elems;
	}

}