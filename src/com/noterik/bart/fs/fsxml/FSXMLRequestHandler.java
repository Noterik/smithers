/**
 * The default node handler, all other node handlers extend this class.
 *
 * This node handler implements the basic functions for processing requests related to
 * basic CRUD actions on smithers.
 *
 * @author Jaap Blom <j.blom@noterik.nl>
 * @author Levi Pires <l.pires@noterik.nl>
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.restlet.fs
 * @access private
 * @version $Id: FSXMLRequestHandler.java,v 1.49 2012-08-13 11:31:35 daniel Exp $
 *	
 * 
 * This class is the sole entry point for interaction with the smithers database.
 * For this purpose both the FSXMLHandler class and the AttributeHandler class were 
 * relocated to this class.
 * 
 * All actions (located in the com.noterik.bart.fs.action package) should call this class for 
 * performing any database actions (on the smithers db).
 * 
 */
package com.noterik.bart.fs.fsxml;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.restlet.data.MediaType;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;

import com.noterik.bart.fs.GlobalConfig;
import com.noterik.bart.fs.cache.CacheHandler;
import com.noterik.bart.fs.dao.DAOException;
import com.noterik.bart.fs.fscommand.dynamic.presentation.playout.cache;
import com.noterik.bart.fs.id.IdHandler;
import com.noterik.bart.fs.script.FSScript;
import com.noterik.bart.fs.tools.FSXMLHelper;
import com.noterik.bart.fs.tools.URIHelper;
import com.noterik.bart.fs.triggering.TriggerSystemManager;
import com.noterik.bart.fs.type.MimeType;
import com.noterik.bart.fs.type.ReferUriType;
import com.noterik.bart.fs.type.ResourceNodeType;
import com.noterik.springfield.tools.XMLHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;
import com.noterik.springfield.tools.net.Message;

public class FSXMLRequestHandler {
	
	private static Logger logger = Logger.getLogger(FSXMLRequestHandler.class);
	private static FSXMLRequestHandler instance;
	private FSXMLHandler fsxmlHandler;
	private AttributeHandler attributeHandler;
	private static final String EXEC_PARAM = "exec";
	public static String debuglevel = "off";
	
	/**
	 * Empty properties xml
	 */
	private static final String DEFAULT_PROPERTIES = "<fsxml><properties></properties></fsxml>";
	
	/* dao instances */
	private static FSXMLPropertiesDAO pdao;
	private static FSXMLChildDAO cdao;
	static {
		// TODO: move to global config 
		
		// initialize cached properties dao 
		FSXMLPropertiesDAO pdao_uncached = GlobalConfig.instance().getDAOFactory().getFSXMLPropertiesDAO();
		//CacheHandler pcHandler = GlobalConfig.instance().getPropertyCacheHandler();
		pdao = new CachingDecoratorPropertiesDAO(pdao_uncached, null);
		
		// initialize cached children dao 
		FSXMLChildDAO cdao_uncached = GlobalConfig.instance().getDAOFactory().getFSXMLChildDAO();
		//CacheHandler ccHandler = GlobalConfig.instance().getChildCacheHandler();
		cdao = new CachingDecoratorChildDAO(cdao_uncached, null);
	}

	private FSXMLRequestHandler() {
		fsxmlHandler = new FSXMLHandler();
		attributeHandler = new AttributeHandler();
	}

	public static FSXMLRequestHandler instance() {
		if (instance == null) {
			instance = new FSXMLRequestHandler();
		}
		return instance;
	}

	/*
	 * THE FUNCTIONS BELOW ARE ALL CALLED INTERNALLY (MOSTLY FROM SCRIPTS)
	 * THE FUNCTIONS BELOW ARE ALL CALLED INTERNALLY (MOSTLY FROM SCRIPTS)
	 * THE FUNCTIONS BELOW ARE ALL CALLED INTERNALLY (MOSTLY FROM SCRIPTS)
	 * THE FUNCTIONS BELOW ARE ALL CALLED INTERNALLY (MOSTLY FROM SCRIPTS)
	 */

	public Document getNodeProperties(String uri, boolean sendEvent) {
		long timer_start = new Date().getTime();
		Document doc = fsxmlHandler.getNodeProperties(uri, sendEvent);
		long timer_end = new Date().getTime();
		if (debuglevel.equals("high")) System.out.println("LOADTIME="+(timer_end-timer_start)+" uri="+uri);
		return doc;
	}

	public MimeType getMimeTypeOfResource(String uri) {
		return fsxmlHandler.getMimeTypeOfResource(uri);
	}
	
	public Document getNodeProperties(String uri, int depth, boolean sendEvent) {
		long timer_start = new Date().getTime();
		Document doc = fsxmlHandler.getNodeProperties(uri, depth, sendEvent);
		long timer_end = new Date().getTime();
		return doc;
	}

	public boolean saveFsXml(String uri, String xml, String method, boolean sendEvent) {		
		boolean succes = false;
		try {
			succes = fsxmlHandler.saveFsXml(uri, xml, method, sendEvent);
		} catch(Exception e) {
			logger.error("",e);
		} 		
		return succes;
	}
	
	public boolean deleteNodeProperties(String uri, boolean sendEvent) {
		boolean success = false;
		try {
			fsxmlHandler.deleteAllPropertiesOfUriTop(uri,sendEvent);
			success = true;
		} catch(Exception e) {
			logger.error("",e);
		} 		
		return success;
	}

	public void updateProperty(String uri, String property, String value, String method, boolean sendEvent) {
		try {
			saveProperty(uri, value, method, property, sendEvent);
		} catch(Exception e) {
			logger.error("",e);
		} 
	}

	public String getPropertyValue(String uri) {
		String value = fsxmlHandler.getPropertyValue(uri);
		return value;
	}
	
	public void deletePropertyValue(String uri, String property) {
		fsxmlHandler.deleteProperty(uri, property, true);
	}

	public Document getNodePropertiesByType(String uri) {
		Document doc = fsxmlHandler.getNodePropertiesByType(uri);
		return doc;
	}

	public Document getNodePropertiesByType(String uri, int depth, int start, int limit) {
		Document doc = fsxmlHandler.getNodePropertiesByType(uri, depth, start, limit);
		return doc;
	}

	public boolean hasProperties(String uri) {
		boolean hp = fsxmlHandler.hasProperties(uri);
		return hp;
	}

	public boolean hasChildren(String uri, String type) {
		boolean hc = fsxmlHandler.hasChildren(uri, type);
		return hc;
	}

	public void addUriToChildrenOfParentResource(String uri, String type) {
		try {		
			fsxmlHandler.addUriToChildrenOfParentResource(uri, type);
		} catch(Exception e) {
			logger.error("",e);
		} 
	}

	public boolean saveAttributes(String uri, String xml, String method) {
		boolean succes = false;
		try {		
			succes = attributeHandler.saveAttributes(uri, xml, method);
		} catch(Exception e) {
			logger.error("",e);
		}
		return succes;
	}
	
	/**
	 * Returns a list of resources that refer to the specified uri.
	 * 
	 * @param uri
	 * @return
	 */
	public List<String> getReferParents(String uri){
		List<String> refPars = attributeHandler.getReferParents(uri);
		return refPars;
	}

	/**
	 * Returns only the refer this node is referring to without getting it's content, otherwise return null
	 */
	public String getRefer(String uri) {
		// get properties
		FSXMLProperties pfsxml = fsxmlHandler.getProperties(uri);
		if (pfsxml.getReferUri() != null) {
			return pfsxml.getReferUri();
		}
		return null;
	}
	
	/*
	 * END END END END
	 */
	
	public Representation handleGET(String uri, String value, Map<String, String> GETParams){
		// see if we need to execute a script
		//System.out.println("DO A GET");
		if(GETParams != null && GETParams.containsKey(EXEC_PARAM)){
			if(GETParams.get(EXEC_PARAM) != null && GETParams.get(EXEC_PARAM).equals("true")){
				// check if this is actually a script
				FSScript fss = getScript(uri);
				String response = null;
				logger.debug("executing script");
				if (fss != null) {
					response = fss.execute();
				} else {
					response = FSXMLBuilder.getErrorMessage("500", "Could not find script",
							"The script you tried to access does not exist", "http://teamelements.noterik.com/team");					
				}
				return new StringRepresentation(response, MediaType.TEXT_XML);
			}
		}
		return handleGET(uri, value);
	}

	/**
	 * Handles a GET request
	 * 
	 * @param uri
	 *            resource uri
	 * @param value
	 *            request body
	 * @return
	 */
	public Representation handleGET(String uri, String value) {		
		// get parameters from request body
		long timer_start = new Date().getTime();


		Map<String, String> params = getParameters(value);
		//System.out.println("REQ="+uri+" P="+params.toString()+" V="+value);
		String curi = uri;
		if (params.size()>0) {
			curi = uri + params.toString();
		}
		
		// hack daniel. lets hook it into our temp cache
		Document doc = cache.get(curi);
		if (doc!=null && curi.indexOf("properties")==-1 && curi.indexOf("/euscreen/")!=-1) {
			DomRepresentation dr = null;
			try {
				dr = new DomRepresentation(MediaType.TEXT_XML);
			} catch (IOException e) {
				logger.error("",e);
			}
			dr.setDocument(XMLHelper.convert(doc));	
			long timer_end = new Date().getTime();
			System.out.println("HIT LOADTIME_URL="+(timer_end-timer_start)+" uri="+curi+" CP="+cache.getPerformance()+" CS="+cache.getCacheSize());
			return dr;
		} // end of cache check 
		
		
		//Document doc = null;
		String cp = URIParser.getCurrentUriPart(uri);
		String pp = URIParser.getParentUriPart(uri);
		if (cp.equals(FSXMLHelper.XML_PROPERTIES)) {
			String correctedUri = URIParser.getPreviousUri(uri);
			doc = fsxmlHandler.getNodeProperties(correctedUri, 0);
		} else if (cp.equals(FSXMLHelper.XML_ATTRIBUTES)) {
			String attrs = attributeHandler.getAttributes(uri);
			if (attrs != null) {
				doc = XMLHelper.asDocument(attrs);
			}
		} else if (pp.equals(FSXMLHelper.XML_PROPERTIES)) {
			String property = fsxmlHandler.getPropertyValue(uri);
			if (property != null) {
				doc = XMLHelper.asDocument("<" + cp + ">" + property + "</" + cp + ">");
			}
		} else if (pp.equals(FSXMLHelper.XML_ATTRIBUTES)) {
			String attr = attributeHandler.getAttributeValue(uri);
			if (attr != null) {
				doc = XMLHelper.asDocument("<" + cp + ">" + attr + "</" + cp + ">");
			}
		} else if (URIParser.isResourceId(uri)) {
			// try cache (including depth)
			String depth = params.get("depth");
			if(doc==null) {
				doc = fsxmlHandler.getNodeProperties(uri, params);
			}
		} else {
			// uri does not end with an id, so the request is for all children
			// of the given type
			String t = URIParser.getCurrentUriPart(uri);
			if (t.equals("domain")) {
				doc = fsxmlHandler.getAllDomains();
			} else {
				doc = fsxmlHandler.getNodePropertiesByType(uri, params);
			}
		}

		if (doc != null) {
			DomRepresentation dr = null;
			try {
				dr = new DomRepresentation(MediaType.TEXT_XML);
			} catch (IOException e) {
				logger.error("",e);
			}
			dr.setDocument(XMLHelper.convert(doc));						
			// TODO jaap: after we decided on the way to go with the HEAD info,
			// probably uncomment this
			// dr.setSize(doc.asXML().length());
			
			long timer_end = new Date().getTime();
			
			if ((timer_end-timer_start)>30 && curi.indexOf("properties")==-1 && curi.indexOf("/euscreen/")!=-1) {
				if (params.size()==0) {
					cache.put(curi,doc);
				} else {
					cache.putParams(uri,doc,params.toString());
				}
				System.out.println("MIS LOADTIME_URL="+(timer_end-timer_start)+" uri="+curi+" CP="+cache.getPerformance()+" CS="+cache.getCacheSize());
			}
			return dr;
		} else {
			String response = FSXMLBuilder.getErrorMessage("404", "No data available",
					"No properties were found for this resource", "http://teamelements.noterik.com/team");
			return new StringRepresentation(response, MediaType.TEXT_XML);
		}
	}

	
	public Document handleDocGET(String uri, String value) {		
		// get parameters from request body
		long timer_start = new Date().getTime();


		Map<String, String> params = getParameters(value);
		//System.out.println("REQ="+uri+" P="+params.toString());
		String curi = uri;
		if (params.size()>0) {
			curi = uri + params.toString();
		}
		
		
		Document doc = null;
		String cp = URIParser.getCurrentUriPart(uri);
		String pp = URIParser.getParentUriPart(uri);
		if (cp.equals(FSXMLHelper.XML_PROPERTIES)) {
			String correctedUri = URIParser.getPreviousUri(uri);
			doc = fsxmlHandler.getNodeProperties(correctedUri, 0);
		} else if (cp.equals(FSXMLHelper.XML_ATTRIBUTES)) {
			String attrs = attributeHandler.getAttributes(uri);
			if (attrs != null) {
				doc = XMLHelper.asDocument(attrs);
			}
		} else if (pp.equals(FSXMLHelper.XML_PROPERTIES)) {
			String property = fsxmlHandler.getPropertyValue(uri);
			if (property != null) {
				doc = XMLHelper.asDocument("<" + cp + ">" + property + "</" + cp + ">");
			}
		} else if (pp.equals(FSXMLHelper.XML_ATTRIBUTES)) {
			String attr = attributeHandler.getAttributeValue(uri);
			if (attr != null) {
				doc = XMLHelper.asDocument("<" + cp + ">" + attr + "</" + cp + ">");
			}
		} else if (URIParser.isResourceId(uri)) {
			// try cache (including depth)
			String depth = params.get("depth");
			if(doc==null) {
				doc = fsxmlHandler.getNodeProperties(uri, params);
			}
		} else {
			// uri does not end with an id, so the request is for all children
			// of the given type
			String t = URIParser.getCurrentUriPart(uri);
			if (t.equals("domain")) {
				doc = fsxmlHandler.getAllDomains();
			} else {
				doc = fsxmlHandler.getNodePropertiesByType(uri, params);
			}
		}

		if (doc != null) {
			// TODO jaap: after we decided on the way to go with the HEAD info,
			// probably uncomment this
			// dr.setSize(doc.asXML().length());			
			long timer_end = new Date().getTime();
			if ((timer_end-timer_start)>30 && curi.indexOf("properties")==-1 && curi.indexOf("/euscreen/")!=-1) {
				if (params.size()==0) {
					cache.put(curi,doc);
				} else {
					cache.putParams(uri,doc,params.toString());
				}
				System.out.println("MIS LOADTIME_URL="+(timer_end-timer_start)+" uri="+curi);
			}
			return doc;
		} else {
			return null;
		}
	}

	
	private FSScript getScript(String uri) {
		MimeType mt = fsxmlHandler.getMimeTypeOfResource(uri);		
		logger.debug("FOUND MIME TYPE: " + mt.getName() + "(" + uri + ")");
		if (mt == MimeType.MIMETYPE_FS_SCRIPT) {			
			FSScript fss = TriggerSystemManager.getInstance().getScriptOfUri(uri);
			return fss;
		} else {
			return null;
		}
	}

	/**
	 * Handle a PUT request.
	 * 
	 * @param uri
	 *            resource uri
	 * @param value
	 *            request body
	 * @return
	 */
	public String handlePUT(String uri, String value) {
		value = value.replaceAll("\n", "");
		logger.debug("xml in PUT: " + value);
		String cp = URIParser.getCurrentUriPart(uri);
		String pp = URIParser.getParentUriPart(uri);
		if (cp.equals(FSXMLHelper.XML_ATTRIBUTES)) {
			// this means it is possible to post/put attributes to the selected
			// resource
			return saveAttributesAndGetHttpResponse(URIParser.getPreviousUri(uri), value, "PUT");
		} else if (cp.equals(FSXMLHelper.XML_PROPERTIES)) {
			// the current uri points to the properties of a resource
			// PropertyHandler.instance().saveFsXml(uri, value, "PUT");
			return savePropertiesAndGetHttpResponse(URIParser.getPreviousUri(uri), value, "PUT");
		} else if (pp.equals(FSXMLHelper.XML_PROPERTIES)) {
			// the current uri points to a single property
			return saveProperty(uri, value, "PUT", cp);
		} else {
			return FSXMLBuilder.getErrorMessage("403", "You are not allowed to send a PUT request to this resource",
					"Please resend to either a properties uri or a single property uri",
					"http://teamelements.noterik.com/team");
		}
	}

	/**
	 * Handle a POST request
	 * 
	 * @param uri
	 *            resource uri
	 * @param value
	 *            request body
	 * @return
	 */
	public String handlePOST(String uri, String value) {
		value = value.replaceAll("\n", "");
		if (URIParser.getCurrentUriPart(uri).equals(ResourceNodeType.PROPERTIES.getName())) {
			// current node is a properties node
			return FSXMLBuilder.getErrorMessage("400", "You cannot POST to a properties node",
					"In order to update properties, send a PUT to the correct properties node",
					"http://teamelements.noterik.com/team");
		} else if (!URIParser.isResourceId(uri)) {
			// current node is the conceptual node
			if (!URIParser.getCurrentUriPart(uri).equals(FSXMLHelper.XML_ATTRIBUTES)) {
				// when pointing to a concept node, auto create an id
				logger.debug("Creating new id for: " + uri);
				int id = IdHandler.instance().insert(uri);
				logger.debug("Newly created id is: "+id);
				// add the id to the uri, so the properties can be properly
				// saved
				uri += "/" + id;
				if (FSXMLHelper.getTypeOfXmlContent(value).equals(FSXMLHelper.XML_PROPERTIES)) {
					return savePropertiesAndGetHttpResponse(uri, value, "POST");
				} else {
					return saveAttributesAndGetHttpResponse(uri, value, "POST");
				}
			} else {
				return FSXMLBuilder.getErrorMessage("403", "You are not allowed to POST to an attributes node",
						"In order to POST new attributes, please resend data to an id node",
						"http://teamelements.noterik.com/team");
			}
		} else {
			if (URIParser.getParentUriPart(uri).equals(FSXMLHelper.XML_PROPERTIES)) {
				// when pointing to a single property node, try to add it
				return saveProperty(uri, value, "POST", URIParser.getCurrentUriPart(uri));
			}
			// current node is the id node
			return FSXMLBuilder.getErrorMessage("403", "You are not allowed to POST to an id node",
					"In order to update properties, send a PUT to the correct properties node",
					"http://teamelements.noterik.com/team");
		}
	}

	/**
	 * Handle a DELETE call
	 * 
	 * @param uri
	 *            resource uri
	 * @param value
	 *            request body
	 * @return
	 */
	public String handleDELETE(String uri, String value) {
		String cp = URIParser.getCurrentUriPart(uri);
		String pp = URIParser.getParentUriPart(uri);
		if(pp.equals(FSXMLHelper.XML_PROPERTIES)) {
			// delete single property
			fsxmlHandler.deleteProperty(uri, cp, true);
			return FSXMLBuilder.getStatusMessage("The resource property has been successfully deleted", "", uri);
		}
		
		if (fsxmlHandler.isAuthorized(uri)) {
			if (URIParser.isResourceId(uri)) {
				if (fsxmlHandler.hasProperties(uri)) {
					fsxmlHandler.deleteAllPropertiesOfUriTop(uri);
					return FSXMLBuilder.getStatusMessage("The resource has been successfully deleted", "", uri);
				} else {
					return FSXMLBuilder.getErrorMessage("403", "The requested resource does not exist "
							+ "or has no properties ", "Review your uri.", "http://teamelements.noterik.com/team");
				}
			} else {
				Map<String,String> childs = fsxmlHandler.getChildrenOfUri(URIParser.getPreviousUri(uri));
				String type = URIParser.getCurrentUriPart(uri);
				logger.debug("type to remove is: " + type);
				
				Boolean success = true;
				String current = "";
				String currentType = "";
				for (Iterator<String> i = childs.keySet().iterator(); i.hasNext();) {
					current = i.next();
					currentType = URIParser.getParentUriPart(current);
					logger.debug("currentType: " + currentType);
					
					// only remove nodes of certain type
					if(type.equals(currentType)) {
						if (fsxmlHandler.hasProperties(current)) {
							fsxmlHandler.deleteAllPropertiesOfUriTop(current);
						} else {
							success = false;
						}
					}
				}
				if (success) {
					return FSXMLBuilder.getStatusMessage("The resource has been successfully deleted", "", uri);
				} else {
					return FSXMLBuilder.getStatusMessage("Some/all of the uri's childs could not be removed", "", uri);
				}
			}
		} else {
			return FSXMLBuilder.getErrorMessage("403", "For security reasons you are not "
					+ "allowed to do a DELETE on such a high level uri.", "Try a uri with 5 " + "or more slashes.",
					"http://teamelements.noterik.com/team");
		}
	}

	private String saveAttributesAndGetHttpResponse(String uri, String value, String method) {
		if (XMLHelper.isXml(value)) {
			if (attributeHandler.hasReferId(uri) && method.equals("POST")) {
				return FSXMLBuilder.getErrorMessage("403", "This resource already exists",
						"If you want to update the attributes, please use a PUT request",
						"http://teamelements.noterik.com/team");
			}
			if (FSXMLHelper.getTypeOfXmlContent(value).equals(FSXMLHelper.XML_ATTRIBUTES)) {
				if (fsxmlHandler.hasProperties(URIParser.getParentUri(uri))) {
					if (attributeHandler.saveAttributes(uri, value, method)) {
						return FSXMLBuilder.getStatusMessage("The attributes were successfully saved", "", uri);
					} else {
						return FSXMLBuilder
								.getErrorMessage("500", "Error during the saving of attributes",
										"An error occurred while saving the attributes",
										"http://teamelements.noterik.com/team");
					}
				} else {
					return FSXMLBuilder.getErrorMessage("403",
							"The parent of the resource you try to access does not exist",
							"First make sure the parent resources exist before sending requests to this one",
							"http://teamelements.noterik.com/team");
				}
			} else {
				return FSXMLBuilder.getErrorMessage("400", "The XML you sent is invalid",
						"Please send a valid attribute fsxml", "http://teamelements.noterik.com/team");
			}
		} else {
			return FSXMLBuilder.getErrorMessage("400", "You are not allowed to send non-fsxml data to this resource",
					"This resource reflects the attributes of a node, please resend with fsxml attribute data",
					"http://teamelements.noterik.com/team");
		}
	}

	private String savePropertiesAndGetHttpResponse(String uri, String value, String method) {
		if (!XMLHelper.isXml(value)) {
			return FSXMLBuilder.getErrorMessage("400", "You are not allowed to send non-fsxml data to this resource",
					"This resource reflects all properties of a node, please resend with fsxml data",
					"http://teamelements.noterik.com/team");
		} else {
			String parUri = URIParser.getParentUri(uri);
			logger.debug("parent URI: " + parUri + "(" + URIParser.getParentUriPart(uri) + ")");
			if (fsxmlHandler.hasProperties(parUri) || URIParser.getParentUriPart(uri).equals("domain")) {
				boolean isOk = fsxmlHandler.saveFsXml(uri, value, method);
				if (isOk) {
					if (method.equals("POST")) {
						return FSXMLBuilder.getStatusMessage("The properties were successfully added", "", uri);
					} else {
						return FSXMLBuilder.getStatusMessage("The properties were successfully updated", "", uri);
					}
				} else {
					return FSXMLBuilder.getErrorMessage("500", "Error during the saving of properties",
							"An error occurred while saving the properties", "http://teamelements.noterik.com/team");
				}
			} else {
				return FSXMLBuilder.getErrorMessage("403", "The parent of the resource you try to access does not exist",
						"First make sure the parent resources exist before sending requests to this one",
						"http://teamelements.noterik.com/team");
			}
		}
	}

	protected String saveProperty(String uri, String value, String method, String property) {
		return saveProperty(uri, value, method, property, true);
	}
	protected String saveProperty(String uri, String value, String method, String property, boolean sendEvent) {
		if (XMLHelper.isXml(value)) {
			return FSXMLBuilder.getErrorMessage("400", "You are not allowed to send fsxml data to this resource",
					"This is a single property resource, please just send a single value",
					"http://teamelements.noterik.com/team");
		} else {
			if (method.equals("POST")) {
				if (fsxmlHandler.hasProperty(uri)) {
					return FSXMLBuilder.getErrorMessage("403", "The property you tried to add already exists",
							"If you want to change the property use a PUT request",
							"http://teamelements.noterik.com/team");
				} else {
					fsxmlHandler.addProperty(uri, property, value, method, sendEvent);
					return FSXMLBuilder.getStatusMessage("The property was added successfully", "Added property: "
							+ value + " (" + property + ")", uri);
				}
			} else {
				fsxmlHandler.updateProperty(uri, property, value, method, sendEvent);
				return FSXMLBuilder.getStatusMessage("The property was successfully updated", "Updated property: "
						+ value + " (" + property + ")", uri);
			}

		}
	}

	/**
	 * Get pruning parameters from request body
	 * 
	 * @param value
	 * @return
	 */
	private Map<String, String> getParameters(String value) {
		Map<String, String> params = new HashMap<String, String>();

		// parse request body
		Node start = null, limit = null, depth = null;
		try {
			Document doc = DocumentHelper.parseText(value);
			start = doc.selectSingleNode("//properties/start");
			limit = doc.selectSingleNode("//properties/limit");
			depth = doc.selectSingleNode("//properties/depth");
		} catch (Exception e) { /*
								 * request body empty or request body could not
								 * be parsed
								 */
		}

		// add to params
		if (start != null) {
			params.put("start", start.getText());
		}
		if (limit != null) {
			params.put("limit", limit.getText());
		}
		if (depth != null) {
			params.put("depth", depth.getText());
		}

		return params;
	}

	/*
	 * **************************************** THE FSXML HANDLER CLASS
	 * ****************************************
	 * **************************************** THE FSXML HANDLER CLASS
	 * ****************************************
	 * **************************************** THE FSXML HANDLER CLASS
	 * ****************************************
	 * **************************************** THE FSXML HANDLER CLASS
	 * ****************************************
	 */

	private class FSXMLHandler {

		/**
		 * logger
		 */
		private Logger logger = Logger.getLogger(FSXMLHandler.class);

		/**
		 * Default depth used for tree pruning
		 */
		private static final int DEFAULT_DEPTH = 10;
		
		/**
		 * Limit value when all childs should be displayed
		 */
		private static final int LIMIT_ALL = -1;

		/**
		 * Default limit for tree pruning
		 */
		private static final int DEFAULT_LIMIT = LIMIT_ALL;

		/**
		 * Default start for tree pruning
		 */
		private static final int DEFAULT_START = 0;

		/**
		 * Default constructor
		 */
		private FSXMLHandler() {

		}

		/**
		 * Function that removes the outer fsxml tags from a document 
		 * 
		 * @param xml
		 * @return
		 */
		private String unwrapXml(String xml) {
			StringBuffer xmlB = new StringBuffer();

			try {
				// parse xml
				Document doc = DocumentHelper.parseText(xml);
				Element root = doc.getRootElement();
				Element elem;
				for (Iterator<Element> iter = root.elementIterator(); iter.hasNext();) {
					elem = iter.next();
					xmlB.append(elem.asXML());
				}
			} catch (Exception e) {
				logger.error("Could not unwrap xml: "+xml,e);
			}

			return xmlB.toString();
		}

		/**
		 * Function that puts fsxml tags around an xml document
		 * 
		 * @param xml
		 * @return
		 */
		private String wrapXml(String xml) {
			if (xml.indexOf("<fsxml>") == -1) {
				return "<fsxml>" + xml + "</fsxml>";
			}
			return xml;
		}

		/**
		 * Get the properties of this uri and send a trigger Only uris which end
		 * with an id are allowed
		 * 
		 * @param uri
		 * @return
		 */
		private Document getNodeProperties(String uri) {
			return getNodeProperties(uri, true);
		}
		
		/**
		 * 
		 * @param uri
		 * @param params
		 * @return
		 */
		public Document getNodeProperties(String uri, Map<String, String> params) {
			int depth = DEFAULT_DEPTH, start = DEFAULT_START, limit = DEFAULT_LIMIT;

			// convert params
			try {
				depth = Integer.parseInt(params.get("depth"));
			} catch (Exception e) { /* no problem, will revert to default */
			}
			try {
				start = Integer.parseInt(params.get("start"));
			} catch (Exception e) { /* no problem, will revert to default */
			}
			try {
				limit = Integer.parseInt(params.get("limit"));
			} catch (Exception e) { /* no problem, will revert to default */
			}

			logger.debug("start: " + start + ", limit: " + limit + ", depth: " + depth);
			
			// only depth for now
			return getNodeProperties(uri, depth);
		}

		/**
		 * Get the properties of this uri and specify if a trigger should be
		 * send
		 * 
		 * @param uri
		 * @param sendEvent
		 *            trigger an event
		 * @return
		 */
		private Document getNodeProperties(String uri, boolean sendEvent) {
			return getNodeProperties(uri, DEFAULT_DEPTH, sendEvent);
		}

		/**
		 * Get the properties of this uri, send a trigger and get the children
		 * to a specified depth.
		 * 
		 * @param uri
		 * @param depth
		 *            the depth to which the ancestors are shown
		 * @return
		 */
		private Document getNodeProperties(String uri, int depth) {
			return getNodeProperties(uri, depth, true);
		}

		/**
		 * Get the properties of this uri, specify if a trigger should be send,
		 * and get the children to a specified depth.
		 * 
		 * @param uri
		 * @param depth
		 *            the depth to which the ancestors are shown
		 * @param sendEvent
		 *            trigger an event
		 * @return
		 */
		private Document getNodeProperties(String uri, int depth, boolean sendEvent) {
			String id = uri.substring(uri.lastIndexOf("/") + 1);
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("fsxml");
			Element current = root.addElement(URIParser.getResourceTypeFromUri(uri)).addAttribute("id", id);
			// call by reference !!!
			if (fsxmlHandler.getNodeProperties(uri, current, depth, 0, sendEvent) != null) {

			} else {
				doc = null;
			}
			return doc;
		}

		/**
		 * This function returns the properties of a uri. The depth parameter
		 * indicates how many levels of the node's childrens' properties have to
		 * be retrieved.
		 * 
		 * @param uri
		 * @param current
		 * @param depth
		 * @param currentDepth
		 * @return
		 */
		private Element getNodeProperties(String uri, Element current, int depth, int currentDepth,
				boolean sendEvent) {
			// trigger event
			if (sendEvent) {
				TriggerSystemManager.getInstance().getDomainTS(URIParser.getDomainFromUri(uri)).eventHappened(uri,
						"GET", "", "");
				
				// send message
				try {
					// daniel test
					cache.signal(InetAddress.getLocalHost().toString(),"GET",uri);
					
					// 27apr GlobalConfig.instance().getDispatcher().send(new Message(InetAddress.getLocalHost(),"GET",uri));
				} catch (Exception e) {
					logger.error("Could not send message",e);
				}
			}
			
			logger.debug("Get node properties");
			// stop condition
			if (currentDepth <= depth) {
				boolean hasOwnProperties = false;
				
				// get properties
				FSXMLProperties pfsxml = getProperties(uri);

				if(pfsxml!=null) {
					// build document
					String xml = pfsxml.getXml();
					if (xml != null && !xml.equals("")) {
						Element elem = null;
						xml = unwrapXml(xml);
						try {
							elem = (Element) DocumentHelper.parseText(xml).getRootElement().clone();
						} catch (DocumentException e) {
							logger.error("Could not clone xml element.",e);
						}
						hasOwnProperties = true;
						// check if there is also a referid
						if (pfsxml.getReferUri() != null) {
							addReferProperties(elem, pfsxml.getReferUri());
							current.addAttribute("referid", pfsxml.getReferUri());
						}
						current.add(elem);
						// add the child elements to the current element (will
						// recurse)
						String childUri = null;
						String childId = null;
						String childType = null;
						Map<String, String> childs = getChildrenOfUri(uri);
						Iterator<String> it = childs.keySet().iterator();
						while (it.hasNext()) {
							childUri = it.next();
							childId = childUri.substring(childUri.lastIndexOf('/') + 1, childUri.length());
							childType = childs.get(childUri);
							Element newElem = current.addElement(childType).addAttribute("id", childId);
							getNodeProperties(childUri, newElem, depth, currentDepth + 1, sendEvent);
						}
					}
					// if it does not have its own properties see if it has a refer
					// id
					if (pfsxml.getReferUri() != null && !hasOwnProperties) {
						current.addAttribute("referid", pfsxml.getReferUri());
						ReferUriType rut = URIHelper.getReferUriType(pfsxml.getReferUri());
						if (rut.equals(ReferUriType.FS_URI)) {
							String referXml = getReferredProperties(pfsxml.getReferUri());
							logger.debug("refer xml is "+referXml);
							if (referXml != null) {
								referXml = unwrapXml(referXml);
							} else {
								referXml = "<properties><error>Dead link!</error><referid>" + pfsxml.getReferUri()
										+ "</referid></properties>";
							}
							Document refDoc = XMLHelper.asDocument(referXml);
							if (refDoc != null) {
								current.add(refDoc.getRootElement());
							}
						}
					}
					if (currentDepth == 0 && !hasOwnProperties && pfsxml.getReferUri() == null) {
						return null;
					}
				} else {
					return null;
				}
			}
			logger.debug("got node properties");
			return current;
		}

		private void addReferProperties(Element elem, String referUri) {
			logger.debug("NAME OF THE ELEMENT: " + elem.getName());
			ReferUriType rut = URIHelper.getReferUriType(referUri);
			if (rut.equals(ReferUriType.FS_URI)) {
				String referXml = getReferredProperties(referUri);
				logger.debug("refer xml is "+referXml);
				if (referXml != null && !referXml.equals("")) {
					//System.out.println("REF="+referXml);
					referXml = unwrapXml(referXml);
				} else {
					referXml = "<properties><error>Dead link!</error><referid>" + referUri + "</referid></properties>";
				}
				Document refDoc = XMLHelper.asDocument(referXml);
				if (refDoc != null) {
					List<Node> nodes = refDoc.selectNodes("properties/*");
					// Element propertiesNode =
					// (Element)elem.selectSingleNode("properties");
					for (Node n : nodes) {
						logger.debug("NAME OF THE NODE: " + n.getName());
						n.detach();
						elem.add(n);
					}
				}
			}
		}

		/**
		 * Basic function that gets the properties of a single URI from the
		 * database, or cache.
		 * 
		 * @param uri
		 * @return
		 */

		private FSXMLProperties getProperties(String uri) {
			// check uri
			if (uri.endsWith("/")) {
				uri = uri.substring(0, uri.length() - 1);
			}
			
			FSXMLProperties pfsxml = null;
			try {
				pfsxml = pdao.read(uri);
			} catch (DAOException e) {
				logger.error("",e);
			}
			return pfsxml;
		}

		/**
		 * Get a single property value
		 * 
		 * @param uri
		 * @return
		 */
		private String getPropertyValue(String uri) {
			logger.debug("getPropertyValue: "+uri);
			
			String propXml = null;
			String parentUri = URIParser.getParentUri(uri);
			FSXMLProperties pfsxml = getProperties(parentUri);
			if(pfsxml==null) {
				return null;
			}
			
			if (pfsxml.getXml() != null) {
				propXml = pfsxml.getXml();
			} else {
				return null;
			}
			Document propdoc = null;
			try {
				propdoc = DocumentHelper.parseText(propXml);
			} catch (DocumentException e) {
				logger.error("",e);
				return null;
			}
			Element elem = (Element) propdoc.selectSingleNode("//" + URIParser.getCurrentUriPart(uri));
			if (elem != null) {
				return elem.getText();
			}
			return null;
		}

		/**
		 * This function gets properties referred to by a refer id.
		 * 
		 * @param uri
		 * @return
		 */
		private String getReferredProperties(String uri) {
			FSXMLProperties pfsxml = getProperties(uri);
			if(pfsxml!=null) {
				if (pfsxml.getXml() != null) {
					return pfsxml.getXml();
				} else if (pfsxml.getReferUri() != null) {
					return getProperties(pfsxml.getReferUri()).getXml();
				}
			}
			return null;
		}

		/**
		 * Basic function that gets the URI's of the children of the specified
		 * URI/node
		 * 
		 * @param uri
		 * @deprecated		use FSXMLChildDAO in stead
		 * @return
		 */
		private Map<String, String> getChildrenOfUri(String uri) {
			// map to return
			Map<String, String> childrenMap = new LinkedHashMap<String, String>();
			
			List<FSXMLChild> children = cdao.getChildren(uri);
			for(FSXMLChild child : children) {
				childrenMap.put(child.getReferUri(), child.getType());
			}
			
			return childrenMap;
		}

		/**
		 * Update a single property
		 * 
		 * @param uri
		 * @param property
		 * @param value
		 * @param method
		 * @param sendEvent
		 */
		private synchronized void updateProperty(String uri, String property, String value, String method,
				boolean sendEvent) {
			String newXml = "", oldXml = "", referid;
			String parentUri = uri.substring(0, uri.lastIndexOf("/properties"));
			//System.out.println("UPDATE URL="+uri);
			// retry concurrency updates. if numRetries == -1, retries will be
			// indefinitely (-1 gives database errors)
			int numRetries = 10;
			boolean updated = false;
			int tries = 0;
			while (tries != numRetries) {
				//System.out.println("TRY "+tries+" URL="+uri);
				// delay
				try {
					Thread.sleep(10*tries);
				} catch (InterruptedException e) { /* do nothing */
				}
				FSXMLProperties properties = getProperties(parentUri);
				oldXml = null;
				referid = null;
				if(properties!=null) {
					oldXml=properties.getXml();
					referid=properties.getReferUri();
				}
				if (oldXml == null) {
					oldXml = DEFAULT_PROPERTIES;
					newXml = addPropertyValue(oldXml, property, value);

					// try insert
					updated = insertConcurrentProperties(parentUri, newXml);
				} else {
					if (hasProperty(uri)) {
						newXml = setPropertyValue(oldXml, property, value);
					} else {
						// not sure why this can happen. probably when a video is created using only a referid
						String tmpXml = oldXml;
						if(tmpXml.equals("")) {
							tmpXml = DEFAULT_PROPERTIES;
						}						
						newXml = addPropertyValue(tmpXml, property, value);
					}

					// try update
					updated = updateConcurrentProperties(parentUri, referid, newXml, oldXml);
				}

				// check if update was a succes. if so, break for loop.
				if (updated) {
					logger.debug("Updating single property successful for " + parentUri);
					break;
				} else {
					logger
							.debug("Updating single property failed for " + parentUri + ", retrying (" + tries
									+ ") ... ");
				}
	
				// update number of tries
				tries++;
			}

			// send event to triggersystem
			if (sendEvent && updated) {
				MimeType mt = FSXMLHelper.getMimeTypeFromXml(newXml);
				TriggerSystemManager.getInstance().getDomainTS(URIParser.getDomainFromUri(uri)).eventHappened(
						parentUri, method, mt.getName(), newXml);
						
				// send message
				try {
					// daniel 27apr2014
					cache.signal(InetAddress.getLocalHost().toString(),method,parentUri);
				} catch (Exception e) {
					logger.error("Could not send message",e);
				}
			}
			
			// catch updates that didn't succeed.
			if(!updated) {
				logger.error("Updating single property failed for " + parentUri);
			}
		}

		/**
		 * Inserting properties. Could go wrong if another process or thread
		 * already inserted properties at the same time.
		 * 
		 * @param uri
		 *            resource uri
		 * @param xml
		 *            properties xml
		 * @return success insert successful or not
		 */
		private synchronized boolean insertConcurrentProperties(String uri, String xml) {
			// create properties
			String type = URIParser.getResourceTypeFromUri(uri);
			MimeType mt = FSXMLHelper.getMimeTypeFromXml(xml);
			FSXMLProperties properties = new FSXMLProperties(uri, null, type, mt.getName(), xml);
			
			// save
			boolean success = false;
			try {
				success = pdao.create(properties);
			} catch (DAOException e) {
				logger.error("",e);
			}
			
			return success;
		}

		/**
		 * Updating properties. Could go wrong if another process or thread
		 * already updated existing properties at the same time.
		 * 
		 * @param uri
		 *            resource uri
		 * @param newXml
		 *            new properties xml
		 * @param oldXml
		 *            old properties xml
		 * @return success update successful or not
		 */
		private synchronized boolean updateConcurrentProperties(String uri, String referid, String newXml, String oldXml) {
			// create properties
			String type = URIParser.getResourceTypeFromUri(uri);
			MimeType mt = FSXMLHelper.getMimeTypeFromXml(newXml);
			FSXMLProperties properties = new FSXMLProperties(uri, referid, type, mt.getName(), newXml);
			properties.setOldXml(oldXml);
			
			// save
			boolean success = false;
			try {
				success = pdao.update(properties);
			} catch (DAOException e) {
				logger.error("",e);
			}
			return success;
		}

		/**
		 * Adds a single property
		 * 
		 * @param uri
		 * @param property
		 * @param value
		 * @param method
		 * @param sendEvent
		 */
		private synchronized void addProperty(String uri, String property, String value, String method, boolean sendEvent) {
			String newXml = "";
			String parentUri = uri.substring(0, uri.lastIndexOf("/properties"));
			FSXMLProperties properties = getProperties(parentUri);
			String xml = properties.getXml();
			if (xml == null) {
				xml = DEFAULT_PROPERTIES;
			}
			newXml = addPropertyValue(xml, property, value);
			newXml = newXml.replace("\n", "");
			saveProperties(parentUri, properties.getReferUri(), newXml, method, sendEvent);
		}
		
		/**
		 * Deletes a single property
		 * 
		 * @param uri
		 * @param property
		 * @param sendEvent
		 */
		private synchronized void deleteProperty(String uri, String property,
				boolean sendEvent) {
			String newXml = "", oldXml = "", referid;
			String parentUri = uri.substring(0, uri.lastIndexOf("/properties"));
			// retry concurrency updates. if numRetries == -1, retries will be
			// indefinitely
			int numRetries = 10;
			boolean updated = false;
			int tries = 0;
			while (tries != numRetries) {
				FSXMLProperties properties = getProperties(parentUri);
				oldXml = null;
				referid = null;
				if(properties!=null) {
					oldXml=properties.getXml();
					referid=properties.getReferUri();
				}
				if (oldXml == null) {
					return;
				} else {
					// remove property from xml
					newXml = removePropertyValue(oldXml, property);

					// try update
					updated = updateConcurrentProperties(parentUri, referid, newXml, oldXml);
				}

				// check if update was a succes. if so, break for loop.
				if (updated) {
					logger.debug("Updating single property successful for " + parentUri);					
					break;
				} else {
					logger.error("Updating single property failed for " + parentUri + ", retrying (" + tries + ") ... ");
				}

				// update number of tries
				tries++;
			}

			// send event to triggersystem
			if (sendEvent && updated) {
				MimeType mt = FSXMLHelper.getMimeTypeFromXml(newXml);
				TriggerSystemManager.getInstance().getDomainTS(URIParser.getDomainFromUri(uri)).eventHappened(
						parentUri, "DELETE", mt.getName(), newXml);
				
				// send message
				try {
					// daniel test
					cache.signal(InetAddress.getLocalHost().toString(),"DELETE",parentUri);
					
					// 27apr GlobalConfig.instance().getDispatcher().send(new Message(InetAddress.getLocalHost(),"DELETE",parentUri));
				} catch (Exception e) {
					logger.error("Could not send message",e);
				}
			}
		}

		/**
		 * Update single property value in a xml file
		 * 
		 * @param xml
		 * @param property
		 * @param value
		 * @return
		 */
		private String setPropertyValue(String xml, String property, String value) {
			Document doc = null;
			try {
				doc = DocumentHelper.parseText(xml);
				Element elem = (Element) doc.selectSingleNode("/fsxml/properties/"+property);
				if(elem!=null) {
					// set property value
					elem.setText(value);
				}
				
				// create new xml
				Element fsxml = (Element) doc.selectSingleNode("/fsxml");
				xml = fsxml.asXML();
			} catch (DocumentException e) {
				logger.error("Could not parse xml",e);
			}
			return xml;
		}

		/**
		 * Add a single property value to an xml file
		 * 
		 * @param xml
		 * @param property
		 * @param value
		 * @return
		 */
		private String addPropertyValue(String xml, String property, String value) {
			Document doc = null;
			try {
				doc = DocumentHelper.parseText(xml);
			} catch (DocumentException e) {
				logger.error("",e);
			}
			Element elem = (Element) doc.selectSingleNode("/fsxml/properties");
			elem.addElement(property).addText(value);
			Element fsxml = (Element) doc.selectSingleNode("/fsxml");
			xml = fsxml.asXML();
			return xml;
		}
		
		/**
		 * Remove a single property value to an xml file
		 * 
		 * @param xml
		 * @param property
		 * @return
		 */
		private String removePropertyValue(String xml, String property) {
			Document doc = null;
			try {
				doc = DocumentHelper.parseText(xml);
				Element elem = (Element) doc.selectSingleNode("/fsxml/properties/"+property);
				if(elem!=null) {
					// remove node
					elem.detach();
				}
				
				// create new xml
				Element fsxml = (Element) doc.selectSingleNode("/fsxml");
				xml = fsxml.asXML();
			} catch (DocumentException e) {
				logger.error("Could not parse xml",e);
			}
			return xml;
		}

		/**
		 * Save fsxml on a specified uri, and sends an event
		 * 
		 * @param uri
		 * @param xml
		 * @param method
		 * @return
		 */
		private boolean saveFsXml(String uri, String xml, String method) {
			return saveFsXml(uri, xml, method, true);
		}

		/**
		 * Save fsxml on a specified uri and
		 * 
		 * @param uri
		 * @param xml
		 * @param method
		 * @param sendEvent
		 * @return
		 */
		private boolean saveFsXml(String uri, String xml, String method, boolean sendEvent) {
			logger.debug("**************************** Saving fsxml ( " + uri + ") **************");
			boolean isOk = false;
			Document doc = XMLHelper.asDocument(xml);
			if (doc != null) {
				// TODO get properties string from a central variable
				Node node = doc.selectSingleNode("//fsxml");
				if (node != null) {
					Element curElm = (Element) node;
					isOk = saveFsXmlNodes(curElm, uri, method, sendEvent);
				}
			}
			return isOk;
		}

		/**
		 * Save fsxml nodes and specify if a trigger should be send
		 * 
		 * @param curElm
		 * @param curUri
		 * @param method
		 * @param sendEvent
		 * @return
		 */
		private boolean saveFsXmlNodes(Element curElm, String curUri, String method, boolean sendEvent) {
			logger.debug("CURRENT URI: " + curUri + " (" + curElm.getName() + ")");
			if (URIParser.getCurrentUriPart(curUri).equals("properties")) {
				logger.debug("Saving properties...");
				logger.debug(curElm.asXML());

				// TODO: copy attributes from parent (nicely)
				if (curElm.getParent() != null && curElm.getParent().getName().equals("fsxml")
						&& curElm.getParent().attributeValue("mimetype") != null
						&& !curElm.getParent().attributeValue("mimetype").equals("")) {
					saveProperties(URIParser.getPreviousUri(curUri), null, "<fsxml mimetype='"+ curElm.getParent().attributeValue("mimetype") + "'>" + curElm.asXML() + "</fsxml>",method, sendEvent);
				} else {
					saveProperties(URIParser.getPreviousUri(curUri), null, wrapXml(curElm.asXML()), method, sendEvent);
				}
			} else {
				Node n = null;
				Element e = null;
				String childUri = null, childTypeUri = null;
				for (Iterator i = curElm.nodeIterator(); i.hasNext();) {
					n = (Node) i.next();
					if (n instanceof Element) {
						e = (Element) n;
						if (!e.getName().equals("properties")) {
							String id = e.attributeValue("id");
							String referId = e.attributeValue("referid");
							if (id == null) {
								childTypeUri = curUri + "/" + e.getName();
								logger.debug("Creating new id for: " + childTypeUri); 
								// TODO:
								// error
								// when
								// putting
								// on
								// /asset/id/properties
								// (WRONG
								// URI)
								id = IdHandler.instance().insert(childTypeUri) + "";
							} else {
								logger.debug("Id supplied for: " + curUri);
							}
							childUri = curUri + "/" + e.getName() + "/" + id;
							if (referId != null) {
								logger.debug("Saving referid (" + referId + ") for: " + childUri);
								attributeHandler.saveReferId(childUri, referId, method);
							}
						} else {
							childUri = curUri + "/" + e.getName();
						}
						saveFsXmlNodes(e, childUri, method, sendEvent);
					}
				}
			}
			return true;
		}

		/**
		 * Save properties
		 * 
		 * @param uri
		 *            resource uri
		 * @param xml
		 *            properties xml
		 * @param method
		 *            request method
		 * @param sendEvent
		 *            send a trigger event
		 * @return
		 */
		private synchronized boolean saveProperties(String uri, String referid, String xml, String method, boolean sendEvent) {			
			// TODO: authorization
			String type = URIParser.getResourceTypeFromUri(uri);
			MimeType mt = FSXMLHelper.getMimeTypeFromXml(xml);
			//System.out.println("MIME1="+mt.getName());
			FSXMLProperties properties = null;
			try {
				properties = pdao.read(uri);
			} catch (DAOException e) {
				logger.error("",e);
			}
			boolean success = false;
			if(properties == null) {
				// create
				properties = new FSXMLProperties(uri, referid, type, mt.getName(), xml);
				try {
					success = pdao.create(properties);
				} catch (DAOException e) {
					logger.error("",e);
				}
			} else {
				// update
				properties.setType(type);
				//System.out.println("MIME2="+mt.getName());
				properties.setMimetype(mt.getName());
				properties.setXml(xml);
				try {
					success = pdao.update(properties);
				} catch (DAOException e) {
					logger.error("",e);
				}
			}
			
			// save children
			if(success) {
				// make sure the ids are set correctly
				IdHandler.instance().checkUriId(uri);
				
				// make sure the URI is linked to its parent
				addUriToChildrenOfParentResource(uri, type);
			}
			
			// send event to triggersystem
			if (sendEvent) {
				TriggerSystemManager.getInstance().getDomainTS(URIParser.getDomainFromUri(uri)).eventHappened(uri,
						method, mt.getName(), xml);
				
				// send message
				try {
					// daniel test
					cache.signal(InetAddress.getLocalHost().toString(),method,uri);
					
					// 27apr GlobalConfig.instance().getDispatcher().send(new Message(InetAddress.getLocalHost(),method,uri));
				} catch (Exception e) {
					logger.error("Could not send message",e);
				}
			}
			return success;
		}

		/**
		 * this function inserts a relation parent/child in the children table
		 * of the data base
		 * 
		 * @param uri
		 * @param type
		 */
		private synchronized void addUriToChildrenOfParentResource(String uri, String type) {
			String id = URIParser.getCurrentUriPart(uri);
			String pUri = URIParser.getParentUri(uri);

			// remove from database
			if (!pUri.equals("") && !pUri.equals("/")) {
				FSXMLChild child = new FSXMLChild(id, pUri, uri, type);
				boolean success = false;
				try {
					success = cdao.create(child);
				} catch (DAOException e) {
					logger.error("",e);
				}
				if(!success) {
					try {
						success = cdao.update(child);
					} catch (DAOException e) {
						logger.error("",e);
					}
				}
				
				logger.debug("addUriToChildrenOfParentResource success: "+success);
			}
		}

		private boolean hasProperties(String uri) {
			boolean has = false;
			FSXMLProperties properties = null;
			try {
				properties = pdao.read(uri);
			} catch (DAOException e) {
				logger.error("",e);
			}
			has = properties!=null;
			return has;
		}

		private boolean hasProperty(String uri) {
			if (!hasProperties(URIParser.getParentUri(uri))) {
				return false;
			}
			String xml = getProperties(URIParser.getParentUri(uri)).getXml();
			String property = uri.substring(uri.lastIndexOf("/") + 1);
			Document doc = null;
			try {
				doc = DocumentHelper.parseText(xml);
			} catch (DocumentException e) {
				logger.error("",e);
				return false;
			}
			String xpath = "/fsxml/properties/" + property;
			Element elem = (Element) doc.selectSingleNode(xpath);
			if (elem != null) {
				return true;
			}
			return false;
		}
		
		/**
		 * Delete call to top node (caching).
		 * 
		 * @param uri
		 */
		private void deleteAllPropertiesOfUriTop(String uri) {
			deleteAllPropertiesOfUriTop(uri,true);
		}
		
		/**
		 * Delete call to top node (caching).
		 * 
		 * @param uri
		 */
		private void deleteAllPropertiesOfUriTop(String uri, boolean sendEvent) {
			 deleteAllPropertiesOfUri(uri, sendEvent);
			
			 // remove from cache
			 String parentUri = URIParser.getParentUri(URIParser.getParentUri(uri));
			 //CacheHandler childCacheHandler = GlobalConfig.instance().getChildCacheHandler();
			 //childCacheHandler.delete(parentUri);
		}

		/**
		 * this function will remove the properties of uri from the properties
		 * table and the occurences of this uri as a children in childrens table
		 * 
		 * @param uri
		 */
		private void deleteAllPropertiesOfUri(String uri, boolean sendEvent) {
			// depth first removal of nodes
			Map<String, String> children = getChildrenOfUri(uri);
			String childUri = null;
			for (Iterator i = children.keySet().iterator(); i.hasNext();) {
				childUri = i.next().toString();
				deleteAllPropertiesOfUri(childUri, sendEvent);
			}
			
			// remove properties and children links
			deletePropertiesOfUri(uri, sendEvent);
			deleteChildrenOfUri(uri);
		}

		
		private synchronized void deletePropertiesOfUri(String uri, boolean sendEvent) {			
			// get mimetype
			MimeType mt = getMimeTypeOfResource(uri);
			
			String parentUri = uri;
			
			if (uri.lastIndexOf("/properties") > -1) {
				parentUri = uri.substring(0, uri.lastIndexOf("/properties"));
			}
			FSXMLProperties properties = getProperties(parentUri);
			String referId = null;
			if (properties.getReferUri() != null) {
				referId = properties.getReferUri();
			}
			
			// delete
			try {
				pdao.delete(uri);
			} catch (DAOException e) {
				logger.error("",e);
			}
			
			if (sendEvent) {
				TriggerSystemManager.getInstance().getDomainTS(URIParser.getDomainFromUri(uri)).eventHappened(uri,
						"DELETE", mt.getName(), null);
				
				// send message
				try {
					cache.signal(InetAddress.getLocalHost().toString(),"DELETE",uri);					
					// 27apr GlobalConfig.instance().getDispatcher().send(new Message(InetAddress.getLocalHost(),"DELETE",uri));
				
					if (referId != null) {
						cache.signal(InetAddress.getLocalHost().toString(),"LINK",referId+" DELETE "+uri);
					}
				} catch (Exception e) {
					logger.error("Could not send message",e);
				}
			}
			
			/* 
			 * clear referred cache, since item that is deleted might
			 * refer to another item
			 * TODO: more efficient removal of referred items 
			 */
			//CacheHandler referredCacheHandler = GlobalConfig.instance().getReferredCacheHandler();
			//referredCacheHandler.deleteAll();
		}

		/**
		 * Does not actually delete the children of this uri. It deletes this child from its parent children.
		 * @param uri
		 */
		private synchronized void deleteChildrenOfUri(String uri) {
			String id = URIParser.getCurrentUriPart(uri);
			String type = URIParser.getParentUriPart(uri);
			String pUri = URIParser.getParentUri(uri);
			try {
				cdao.delete(new FSXMLChildKey(pUri, type, id));
			} catch (DAOException e) {
				logger.error("",e);
			}
		}

		/**
		 * Get the node properties of a uri that does not end with an id Default
		 * values for start and limit (0,-1).
		 * 
		 * @param uri
		 * @return
		 */
		private Document getNodePropertiesByType(String uri) {
			return getNodePropertiesByType(uri, DEFAULT_DEPTH, DEFAULT_START, DEFAULT_LIMIT);
		}

		/**
		 * Get the node properties of a uri that does not end with an id
		 * 
		 * @param uri
		 *            resource uri
		 * @param params
		 *            pruning parameters, such as depth, start and limit
		 * @return
		 */
		private Document getNodePropertiesByType(String uri, Map<String, String> params) {
			int depth = DEFAULT_DEPTH, start = DEFAULT_START, limit = DEFAULT_LIMIT;

			// convert params
			try {
				depth = Integer.parseInt(params.get("depth"));
			} catch (Exception e) { /* no problem, will revert to default */
			}
			try {
				start = Integer.parseInt(params.get("start"));
			} catch (Exception e) { /* no problem, will revert to default */
			}
			try {
				limit = Integer.parseInt(params.get("limit"));
			} catch (Exception e) { /* no problem, will revert to default */
			}

			logger.debug("Start: " + start + ", limit: " + limit + ", depth: " + depth);

			return getNodePropertiesByType(uri, depth, start, limit);
		}

		/**
		 * Get the node properties of a uri that does not end with an id
		 * 
		 * @param uri
		 * @param depth
		 * @param start
		 * @param limit
		 * @return
		 */
		private Document getNodePropertiesByType(String uri, int depth, int start, int limit) {
			Document doc = null;

			// check start and limit
			start = start < DEFAULT_START ? DEFAULT_START : start;
			limit = limit < LIMIT_ALL ? LIMIT_ALL : limit;
			
			// get type of asset
			String type = URIParser.getCurrentUriPart(uri);
			uri = URIParser.getPreviousUri(uri);

			// debug
			logger.debug("Getting nodes of type for: " + uri + "(" + type + ")");
			
			// get children and count
			List<FSXMLChild> children = cdao.getChildrenByType(uri, type);
			int totalResultsAvailable = children.size();
			
			// create xml
			StringBuffer xml = new StringBuffer();
			int pNow = 0, totalResultsReturned = 0; // pointers for position of resultset and number of childs unpruned.
			for(FSXMLChild child : children) {				
				if (pNow >= start && (totalResultsReturned < limit || limit == LIMIT_ALL)) {
					// add complete child and update total pointer
					Document childDoc = getNodeProperties(child.getReferUri(),depth-1);
					if(childDoc!=null) {
						xml.append( unwrapXml(childDoc.asXML()) );
					}
					
					// update pointer
					totalResultsReturned++;
				} else if (totalResultsReturned > limit && limit != LIMIT_ALL) {
					// stop loop
					break;
				}
				
				// update pointer
				pNow++;
			}

			// add information such as start, limit, depth,
			// totalResultsAvailable
			// and totalResultsReturned
			StringBuffer rsProperties = new StringBuffer();
			rsProperties.append("<properties>");
			rsProperties.append("<depth>" + depth + "</depth>");
			rsProperties.append("<start>" + start + "</start>");
			rsProperties.append("<limit>" + limit + "</limit>");
			rsProperties.append("<totalResultsAvailable>" + totalResultsAvailable + "</totalResultsAvailable>");
			rsProperties.append("<totalResultsReturned>" + totalResultsReturned + "</totalResultsReturned>");
			rsProperties.append("</properties>");

			// convert to document
			try {
				doc = DocumentHelper.parseText(wrapXml(rsProperties.toString() + xml.toString()));
			} catch (DocumentException e) {
				logger.error("",e);
			}

			// return
			return doc;
		}

		/**
		 * Get the different domains available
		 * 
		 * @return
		 */
		private Document getAllDomains() {
			List<FSXMLProperties> pList = pdao.getPropertiesByType("domain");
			
			Document doc = null;
			StringBuffer xml = new StringBuffer();
			for(FSXMLProperties properties : pList) {
				xml.append("<domain id=\"" + URIParser.getCurrentUriPart(properties.getUri()) + "\">");
				xml.append(unwrapXml(properties.getXml()));
				xml.append("</domain>");
			}
			
			try {
				doc = DocumentHelper.parseText(wrapXml(xml.toString()));
			} catch (DocumentException e) {
				logger.error("",e);
			}
			return doc;
		}

		/**
		 * Get the mimetype of a uri
		 * 
		 * @param uri
		 * @return
		 */
		private MimeType getMimeTypeOfResource(String uri) {
			FSXMLProperties properties = null;
			try {
				properties = pdao.read(uri);
			} catch (DAOException e) {
				logger.error("",e);
			}
			if(properties!=null && properties.getMimetype()!=null && properties.getMimetype().equals(MimeType.MIMETYPE_FS_SCRIPT.getName())) {
				return MimeType.MIMETYPE_FS_SCRIPT;
			}
			return MimeType.MIMETYPE_FS_XML;
		}

		private boolean hasChildren(String uri, String type) {
			int count = cdao.getChildrenByTypeCount(uri, type);
			if(count==0) {
				return false;
			}
			return true;			
		}

		private boolean isAuthorized(String uri) {
			boolean is = (uri.split("/").length > 5);
			return is;
		}

	}

	/*
	 * **************************************** THE ATTRIBUTE HANDLER CLASS
	 * ****************************************
	 * **************************************** THE ATTRIBUTE HANDLER CLASS
	 * ****************************************
	 * **************************************** THE ATTRIBUTE HANDLER CLASS
	 * ****************************************
	 * **************************************** THE ATTRIBUTE HANDLER CLASS
	 * ****************************************
	 */

	private class AttributeHandler {

		private static final String ATTRIBUTES_TAG = "attributes";

		private boolean saveAttributes(String uri, String xml, String method) {
			Document doc = XMLHelper.asDocument(xml);
			if (doc != null) {
				Element atrs = (Element) doc.selectSingleNode("//" + ATTRIBUTES_TAG);
				if (atrs != null) {
					Element child = null;
					for (Iterator<Element> i = atrs.elementIterator(); i.hasNext();) {
						child = (Element) i.next();
						if (child.getName() != null) {
							if (child.getName().equals("referid")) {
								return saveReferId(uri, child.getText(), method);
							}
						}
					}
				}
			}
			return false;
		}

		private synchronized boolean saveReferId(String uri, String referId, String method) {
			// get type
			String type = URIParser.getResourceTypeFromUri(uri);
			
			// read old properties, update and save back
			FSXMLProperties properties = null;
			try {
				properties = pdao.read(uri);
			} catch (DAOException e) {
				logger.error("",e);
			}
			boolean success = false;
			try {
				if(properties==null) {
					properties = new FSXMLProperties(uri, referId, type, MimeType.MIMETYPE_FS_XML.getName(), DEFAULT_PROPERTIES);
					success = pdao.create(properties);
				} else {
					properties.setReferUri(referId);
					success = pdao.update(properties);
				}
			} catch(DAOException e) {
				logger.error("",e);
			}
			
			// make proper xml for event
			String attributesXml = "<fsxml><attributes><referid>"+referId+"</referid></attributes></fsxml>";
			// make sure the URI is linked to its parent
			FSXMLRequestHandler.instance().addUriToChildrenOfParentResource(uri, type);
			TriggerSystemManager.getInstance().getDomainTS(URIParser.getDomainFromUri(uri)).eventHappened(uri, method,
					MimeType.MIMETYPE_FS_XML.getName(), attributesXml);
			
			// send message
			try {
				cache.signal(InetAddress.getLocalHost().toString(),method,uri);				
				// 27apr GlobalConfig.instance().getDispatcher().send(new Message(InetAddress.getLocalHost(),method,uri));
				
				cache.signal(InetAddress.getLocalHost().toString(),"LINK",referId+" "+method+" "+uri);				
			} catch (Exception e) {
				logger.error("Could not send message",e);
			}
			
			// clear referred cache
			//CacheHandler referredCacheHandler = GlobalConfig.instance().getReferredCacheHandler();
			//referredCacheHandler.delete(referId);
			
			return success;
		}
		
		/**
		 * Returns a list of resources that refer to the specified uri.
		 * 
		 * @param uri		resource uri.
		 * @return			A list of resources that refer to the specified uri.
		 */
		public List<String> getReferParents(String uri){
			List<String> refPars = null; 
			
			// check cache
			//CacheHandler referredCacheHandler = GlobalConfig.instance().getReferredCacheHandler();
			//refPars = (List<String>)referredCacheHandler.get(uri);

			if(refPars==null) {
				// create list
				refPars = new ArrayList<String>();
				
				// get referrer properties
				List<FSXMLProperties> pList = pdao.getReferredProperties(uri);
				for(FSXMLProperties properties : pList) {
					refPars.add(properties.getUri());
				}
				
				// store in cache
				//referredCacheHandler.put(uri, refPars);
			}
			
			// return
			return refPars;
		}

		private boolean hasReferId(String uri) {
			// get properties
			FSXMLProperties properties = null;
			try {
				properties = pdao.read(uri);
			} catch (DAOException e) {
				logger.error("",e);
			}
			if(properties==null) {
				return false;
			}
			return  properties.getReferUri()!=null;
		}

		/**
		 * This function returns all of the resources attributes.
		 * 
		 * TODO the db structure should be changed (when more attributes are
		 * needed), so it actually has a separate table for attributes
		 * 
		 * @param uri
		 * @return
		 */

		private String getAttributes(String uri) {
			// get properties
			FSXMLProperties properties = null;
			try {
				properties = pdao.read(uri);
			} catch (DAOException e) {
				logger.error("",e);
			}
			
			// TODO: xml transformer class
			// create xml
			String attributes = "<fsxml><attributes>";
			attributes += "<id>" + URIParser.getParentUriPart(uri) + "</id>";
			if(properties.getReferUri()!=null) {
				attributes += "<referid>" + properties.getReferUri() + "</referid>";
			}
			attributes += "</attributes></fsxml>";
			return attributes;
		}

		private String getAttributeValue(String uri) {
			String parentUri = URIParser.getPreviousUri(uri);
			String xml = getAttributes(parentUri);
			
			// check attributes
			if (xml == null) {
				return null;
			}
			
			// parse
			Document doc = null;
			try {
				doc = DocumentHelper.parseText(xml);
			} catch (DocumentException e) {
				logger.error("",e);
			}
			Element elem = (Element) doc.selectSingleNode("//" + URIParser.getCurrentUriPart(uri));
			if (elem != null) {
				return elem.getText();
			}
			return null;
		}

	}

}