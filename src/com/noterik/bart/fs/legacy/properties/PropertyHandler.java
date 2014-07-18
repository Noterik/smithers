/* 
* PropertyHandler.java
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
package com.noterik.bart.fs.legacy.properties;

import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.noterik.bart.fs.GlobalConfig;
import com.noterik.bart.fs.cache.CacheHandler;
import com.noterik.bart.fs.db.ConnectionHandler;
import com.noterik.bart.fs.legacy.tools.XmlHelper;

public class PropertyHandler {
	
	private static Logger logger = Logger.getLogger(PropertyHandler.class);

	/**
	 * this function creates the Document where the properties will be saved in
	 * and the first element to put in it. Then it calls the loopInIt.
	 *
	 * @param uri
	 * @return
	 */
	public static String getProperties(String uri) {
		int count = 0;
		String xmlProps = "";
		if (hasProperties(uri)) {
			List<String> uris = new ArrayList<String>();
			uris.add(uri);
			String id = uri.substring(uri.lastIndexOf("/") + 1);
			Document doc = DocumentHelper.createDocument();
			String type = getTypefromProp(uri);
			Element root = doc.addElement("fsxml");
			Element current = root.addElement(type).addAttribute("id", id);
			doc = loopInIt(doc, current, uri, count, uris);
			xmlProps = doc.asXML();
		}
		return xmlProps;
	}

	/**
	 * This function will walk trough all children of a given uri and add it's
	 * properties to the Document passed as parameter. The counter sets the
	 * number of levels it should go into
	 *
	 * @param doc
	 * @param current
	 * @param uri
	 * @param count
	 * @return
	 */
	public static Document loopInIt(Document doc, Element current, String uri, int count, List uris) {
		// the depth level it should go into
		if (count < 10) {
			count++;
			String xml = getXMLfromProp(uri);
			if (!xml.equals("")) {
				xml = unwrapXml(xml);
			}
			Element elem = null;
			// if xml is not empty add the node
			if (!xml.equals("")) {
				try {
					elem = (Element) DocumentHelper.parseText(xml).getRootElement().clone();
				} catch (DocumentException e) {
					e.printStackTrace();
				}
				current.add(elem);
			} else {
				// if it is empty, just add an empty node
				current.addElement("properties");
			}
			String childUri = "";
			String childId = "";
			String childType = "";
			// get the children for this uri (refer_uris)
			Map<String, Props> childs = getChildrenOfUri(uri);
			Iterator<String> it = childs.keySet().iterator();
			// for each children do the loop
			while (it.hasNext()) {
				childUri = it.next();
				// check if this uri was already processed
				if (!uris.contains(childUri)) {
					uris.add(childUri);
					childId = childs.get(childUri).getId();
					childType = childs.get(childUri).getType();
					Element newElem = current.addElement(childType).addAttribute("id", childId);
					doc = loopInIt(doc, newElem, childUri, count, uris);
				}
			}
		}
		return doc;
	}

	/**
	 * This function will get the properties of a uri only, and not of it's
	 * childs
	 *
	 * @param uri
	 * @return
	 */
	public static Document getPropsNoChilds(String uri) {
		String url = uri.substring(0, uri.lastIndexOf("/"));
		logger.debug("url is:" + url);
		Document doc = DocumentHelper.createDocument();
		String id = url.substring(url.lastIndexOf("/") + 1);
		String type = getTypefromProp(url);
		Element root = doc.addElement("fsxml");
		Element current = root.addElement(type).addAttribute("id", id);
		String xml = getXMLfromProp(url);
		Element elem = null;
		// if xml is not empty, unwrapp it and add the node
		if (!xml.equals("")) {
			xml = unwrapXml(xml);
			try {
				elem = (Element) DocumentHelper.parseText(xml).getRootElement().clone();
			} catch (DocumentException e) {
				e.printStackTrace();
			}
			current.add(elem);
		} else {
			// if it is empty, just add an empty node
			current.addElement("properties");
		}
		return doc;
	}

	/**
	 * This function returns a Map with all the children(refer_uri) of a uri
	 *
	 * @param uri
	 * @return
	 */
	public static Map<String, Props> getChildrenOfUri(String uri) {
		Map<String, Props> childs = new LinkedHashMap<String, Props>();
		String cid = "";
		String curi = "";
		String ctype = "";
		//CacheHandler childHandlerCache = GlobalConfig.instance().getChildCacheHandler();
		//Cache cache = manager.getCache("childCache");
		//if (childHandlerCache.isKeyInCache(uri) && childHandlerCache.get(uri) != null) {
		//	childs = (Map) childHandlerCache.get(uri);
		if (1==2) {
		} else {
			Connection conn = ConnectionHandler.instance().getConnection();
			PreparedStatement selStmt = null;
			ResultSet rs = null;
			String selSql = "SELECT c_refer_uri, c_id, c_type FROM children WHERE c_uri=? ORDER BY c_type, c_id";
			try {
				selStmt = conn.prepareStatement(selSql);
				selStmt.setString(1, uri);
				logger.debug("Query for children is: " + selStmt.toString());
				selStmt.execute();
				rs = selStmt.executeQuery();
				while (rs.next()) {
					curi = rs.getString("c_refer_uri");
					cid = rs.getString("c_id");
					ctype = rs.getString("c_type");
					Props props = new Props(cid, ctype);
					childs.put(curi, props);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			//childHandlerCache.put(uri, childs);
		}
		return childs;
	}

	/**
	 * This function returns the properties xml for a given uri from the
	 * properties table. If the given uri has no properties, it will look
	 * recursively in it's parents
	 *
	 * @param uri
	 * @return
	 */
	public static String getXMLfromProp(String uri) {
		String xml = "";
		logger.debug("Will get xml for: " + uri);
		//CacheHandler propsHandlerCache = GlobalConfig.instance().getPropertyCacheHandler();
		// if the values are in cache get them from there
		//if (propsHandlerCache.isKeyInCache(uri) && propsHandlerCache.get(uri) != null) {
		//	xml = (String) propsHandlerCache.get(uri);
		if (1==2) {
		} else {
			// if not in cache get them from data base
			Connection conn = ConnectionHandler.instance().getConnection();
			PreparedStatement selStmt = null;
			ResultSet rs = null;
			String selSql = "SELECT p_xml FROM properties WHERE p_uri=?";
			try {
				selStmt = conn.prepareStatement(selSql);
				selStmt.setString(1, uri);
				logger.debug("Query is: " + selStmt.toString());
				selStmt.execute();
				rs = selStmt.executeQuery();
				if (rs.next()) {
					xml = rs.getString("p_xml");
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			// if the result is empty, loop in the parents
			if (xml.equals("")) {
				String url = uri.substring(0, uri.lastIndexOf("/"));
				// stop looping when getting into /domain
				if (url.equals("/domain")) {
					return xml;
				} else {
					return getXMLfromProp(url);
				}
			}
			/*
			if(xml != null && !xml.equals("")){
				propsHandlerCache.put(uri, xml);
			}
			*/
		}
		return xml;
	}

	/**
	 * This function takes the wrapping <fsxml> tag from the xml properties
	 * string
	 *
	 * @param xml
	 * @return
	 */
	public static String unwrapXml(String xml) {
		logger.debug("\nXML IS: " + xml);
		try {
			Document doc = DocumentHelper.parseText(xml);
			Element props = (Element) doc.selectSingleNode("/fsxml/properties");
			xml = props.asXML();
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return xml;
	}

	/**
	 * This function returns the type of a given uri from the properties table
	 *
	 * @param uri
	 * @return
	 */
	public static String getTypefromProp(String uri) {
		String type = "";
		Connection conn = ConnectionHandler.instance().getConnection();
		PreparedStatement selStmt = null;
		ResultSet rs = null;
		String selSql = "SELECT p_type FROM properties WHERE p_uri=?";
		try {
			selStmt = conn.prepareStatement(selSql);
			selStmt.setString(1, uri);
			logger.debug("Query is: " + selStmt.toString());

			selStmt.execute();
			rs = selStmt.executeQuery();
			if (rs.next()) {
				type = rs.getString("p_type");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (type.equals("")) {
			String[] parts = uri.split("/");
			type = parts[parts.length - 2];
		}
		return type;
	}

	/**
	 * This function will get the value of the property defined in uri, by
	 * getting and xpath from it and grabbing the correspondent value
	 *
	 * @param url
	 * @return
	 */
	public static Document getPropertyValue(String url) {
		String uri = url.substring(0, url.lastIndexOf("/properties"));
		String xpath = url.substring(url.lastIndexOf("/properties"));
		String xml = getXMLfromProp(uri);
		Document doc = XmlHelper.getValueOfProperty(xml, xpath);
		return doc;
	}

	/**
	 * This function sets the value of a property
	 *
	 * @param url
	 * @param type
	 * @param value
	 */
	public static void setPropertyValue(String url, String type, String value) {
		String newXml = "";
		String uri = url.substring(0, url.lastIndexOf("/properties"));
		String xpath = url.substring(url.lastIndexOf("/properties"));
		String xml = getXMLfromProp(uri);
		logger.debug("previous xml = " + xml);
		newXml = XmlHelper.setValueOfProperty(xml, xpath, value);
		logger.debug("new xml = " + newXml);
		newXml = newXml.replace("\n", "");
		saveProperties(uri, type, newXml);
	}

	/**
	 * This function will add a new property value to the property file
	 *
	 * @param url
	 * @param type
	 * @param property
	 * @param value
	 */
	public static void addValueToProperty(String url, String type, String property, String value) {
		String newXml = "";
		String xml = getXMLfromProp(url);
		logger.debug("previous xml = " + xml);
		newXml = XmlHelper.addPropertyValue(xml, property, value);
		logger.debug("new xml = " + newXml);
		newXml = newXml.replace("\n", "");
		saveProperties(url, type, newXml);
	}

	/**
	 * This function creates a default property with the specified value
	 *
	 * @param url
	 * @param type
	 * @param value
	 */
	public static void createPropertyWithValue(String url, String type, String value) {
		String uri = url.substring(0, url.lastIndexOf("/properties"));
		String property = url.substring(url.lastIndexOf("/properties") + 12);
		Document doc = DocumentHelper.createDocument();
		doc.addElement("fsxml").addElement("properties").addElement(property).addText(value);
		Element fsxml = (Element) doc.selectSingleNode("/fsxml");
		String xml = fsxml.asXML();
		saveProperties(uri, type, xml);
	}

	/**
	 * this function sets the entire properties file in the DB
	 *
	 * @param uri
	 * @param type
	 * @param prop
	 */
	public static void saveProperties(String uri, String type, String prop) {
		//CacheHandler propsHandlerCache = GlobalConfig.instance().getPropertyCacheHandler();
		
		logger.debug("\nbefore connection");
		Connection conn = ConnectionHandler.instance().getConnection();
		PreparedStatement setStmt = null;
		String setSql = "REPLACE INTO properties (p_uri, p_type, p_xml)" + " values(?, ?, ?)";
		prop = prop.replace("\n", "");
		try {
			setStmt = conn.prepareStatement(setSql);
			setStmt.setString(1, uri);
			setStmt.setString(2, type);
			setStmt.setString(3, prop);
			logger.debug("Query is: " + setStmt.toString());
			setStmt.execute();
			setStmt.executeQuery();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		// clear the cache
		//propsHandlerCache.delete(uri);
		logger.debug("Following uri was deleted from props cache: " + uri);

		// set the parent/child relation
		setChild(uri, type);
		// check if parent has properties
		checkParent(uri);
	}

	/**
	 * This function checks if the parent of the uri passed as parameter has its
	 * properties defined in the database. If not a default node will be
	 * created.
	 */
	public static void checkParent(String uri) {
		String tmp = uri.substring(0, uri.lastIndexOf("/"));
		String parent = tmp.substring(0, tmp.lastIndexOf("/"));
		logger.debug("tmp is:" + tmp);
		logger.debug("parent is:" + parent);
		if (!parent.equals("/domain") && !tmp.equals("/domain")) {
			if (!hasProperties(parent)) {
				logger.debug("parent had no properties");
				String[] parts = uri.split("/");
				String type = parts[parts.length - 4];
				String prop = "<fsxml><properties></properties></fsxml>";
				saveProperties(parent, type, prop);
			}
			// checkParent(parent);
		}
	}

	/**
	 * this function checks if a uri has properties defined in the data base
	 *
	 * @param uri
	 * @return
	 */
	public static boolean hasProperties(String uri) {
		logger.debug("Checking if it has properties....");
		boolean has = false;
		//CacheHandler propsHandlerCache = GlobalConfig.instance().getPropertyCacheHandler();
		//if (propsHandlerCache.isKeyInCache(uri) && propsHandlerCache.get(uri) != null) {
		//	has = true;
		if (1==2) {
		} else {
			// if not in cache get them form data base
			Connection conn = ConnectionHandler.instance().getConnection();
			PreparedStatement selStmt = null;
			ResultSet rs = null;
			String selSql = "SELECT p_xml FROM properties WHERE p_uri=?";
			try {
				selStmt = conn.prepareStatement(selSql);
				selStmt.setString(1, uri);
				logger.debug("Query for has is: " + selStmt.toString());
				selStmt.execute();
				rs = selStmt.executeQuery();
				if (rs.next()) {
					has = true;
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		logger.debug("Has properties: " + has);
		return has;
	}

	/**
	 * this function inserts a relation parent/child in the children table of
	 * the data base
	 *
	 * @param uri
	 * @param type
	 */
	public static void setChild(String uri, String type) {

		//CacheHandler childHandlerCache = GlobalConfig.instance().getChildCacheHandler();

		String id = "";
		String pUri = "";
		String[] parts = uri.split("/");
		id = parts[parts.length - 1];
		String tmp = uri.substring(0, uri.lastIndexOf("/"));
		pUri = tmp.substring(0, tmp.lastIndexOf("/"));
		if (!pUri.equals("") && !pUri.equals("/")) {
			Connection conn = ConnectionHandler.instance().getConnection();
			PreparedStatement setStmt = null;
			String setSql = "REPLACE INTO children (c_uri, c_id, c_type, c_refer_uri)" + " values(?, ?, ?, ?)";
			try {
				setStmt = conn.prepareStatement(setSql);
				setStmt.setString(1, pUri);
				setStmt.setString(2, id);
				setStmt.setString(3, type);
				setStmt.setString(4, uri);
				logger.debug("Query is: " + setStmt.toString());
				setStmt.execute();
				setStmt.executeQuery();
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			//childHandlerCache.delete(pUri);
			logger.debug("Following uri was deleted from childs cache: " + pUri);
		}
	}

	/**
	 * this function will remove the properties of uri from the properties table
	 * and the occurences of this uri as a children in childrens table
	 *
	 * @param uri
	 */
	public static void removeProperty(String uri) {
		//CacheHandler propsHandlerCache = GlobalConfig.instance().getPropertyCacheHandler();

		Connection conn = ConnectionHandler.instance().getConnection();
		PreparedStatement delPropStmt = null;
		PreparedStatement delChildStmt = null;
		String delPropSql = "DELETE FROM properties WHERE p_uri=?";
		String delChilSql = "DELETE FROM children WHERE c_refer_uri=?";
		try {
			delPropStmt = conn.prepareStatement(delPropSql);
			delPropStmt.setString(1, uri);
			logger.debug("Query for delete props is: " + delPropStmt.toString());
			delChildStmt = conn.prepareStatement(delChilSql);
			delChildStmt.setString(1, uri);
			logger.debug("Query for delete childs is: " + delChildStmt.toString());
			delPropStmt.execute();
			delChildStmt.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		
		// cache.remove(uri);
		//propsHandlerCache.delete(uri);
		logger.debug("Following uri was deleted from props cache: " + uri);
	}

}