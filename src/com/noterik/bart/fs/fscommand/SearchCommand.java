/*
 * Created on Feb 11, 2009
 */
package com.noterik.bart.fs.fscommand;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.InvalidXPathException;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.bart.fs.search.SearchParams;
import com.noterik.bart.fs.tools.ServiceHelper;
import com.noterik.bart.marge.model.Service;
import com.noterik.springfield.tools.HttpHelper;
import com.noterik.springfield.tools.XMLHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * Command that performs searches. (referred to in: /xml/search.xml). This
 * command wraps the client's input XML into a request suited for the
 * search engine it currently uses. (the search engine is configured in the config.xml)
 * 
 * At this point there is one search engine, which is lisa.
 * 
 * Input requirements:
 * - uri
 * - query (optional)
 * - pruning parameters; start, limit (optional)
 * - children true/false (optional)
 * - sorting parameters (optional)
 * - fullresults (default = false)
 * - searchengine (default as in config.xml)
 *
 * @author Jaap Blom <j.blom@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 *
 */

public class SearchCommand implements Command {

	private static Logger logger = Logger.getLogger(SearchCommand.class);

	
	/**
	 * This function wraps the input XML of the client into a SearchParams object and 
	 * passes it to the function that actually performs the search. The results of the
	 * search are returned.
	 * @param uri  not needed?
	 * @param xml  the input XML coming from the client
	 * @return  the results of ths search in XML
	 */
	
	public String execute(String uri, String xml) {
		// parse input
		Element element = null;
		String eId = null;
		SearchParams sp = null;
		logger.debug(":::::: start of search script ::::::");
		List<Element> iElements = getInputElements(xml);
		for (Iterator<Element> iter = iElements.iterator(); iter.hasNext();) {
			element = iter.next();
			eId = element.attributeValue("id");
			logger.debug("EID: " + eId);
			if (eId != null) {
				// parse properties
				Node pNode = element.selectSingleNode("properties");
				if (pNode != null) {
					List<Node> children = pNode.selectNodes("child::*");
					Node child;
					sp = new SearchParams();
					for (Iterator<Node> cIter = children.iterator(); cIter.hasNext();) {
						child = cIter.next();
						String n = child.getName();
						String t = child.getText();
						if (t != null && !t.equals("")) {
							if (n.equals("uri")) {
								sp.setUri(t);
							} else if (n.equals("query")) {
								sp.setQuery(t);
							} else if (n.equals("start")) {
								sp.setStart(t);
							} else if (n.equals("limit")) {
								sp.setLimit(t);
							} else if (n.equals("sort")) {
								sp.setSort(t);
							} else if (n.equals("childs")) {
								sp.setChilds(t.equals("true"));
							} else if (n.equals("searchengine")) {
								sp.setSearchEngine(t);
							} else if (n.equals("fullresults")) {
								sp.setFullResults(t.equals("true"));
							} else if (n.equals("propertiesonly")) {
								sp.setPropertiesOnly(t.equals("true"));
							} else if (n.equals("headonly")) {
								sp.setHeadOnly(t.equals("true"));
							}
						}
					}
				}
			}
		}
		if (sp == null) {
			logger.error("No input parameters were specified");
			return null;
		}
		sp.setEventUri(uri);
		// check on uri
		if (sp.getUri() == null) {
			logger.error("Required input parameters not specified");
			return null;
		}
		return doLisaSearch(sp);
	}
	
	/**
	 * Uses the search parameters to create a search URL for lisa.
	 * The correct lisa host is obtained by using marge.
	 * @param sp
	 * @return   the XML results of the query
	 */

	private String doLisaSearch(SearchParams sp) {
		String sUri = "";
		if (sp.getQuery() != null && !sp.getQuery().equals("")) {
			try {
				sUri = "&query=" + URLEncoder.encode(sp.getQuery(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				sUri = null;
				logger.error("",e);
			}
			logger.debug("Custom query: " + sp.getQuery());
		}
		if (sp.getStart() != null) {
			sUri += "&start=" + sp.getStart();
		}
		if (sp.getLimit() != null) {
			sUri += "&limit=" + sp.getLimit();
		}
		if (sp.getSort() != null) {
			sUri += "&sort=" + sp.getSort();
		}
		if (sp.isChilds()) {
			sUri += "&childs=true";
		}
		if (sUri != null) {
			sUri = sUri.replaceFirst("&", "?");
			logger.debug("RETURN HEAD ONLY: " + sp.isHeadOnly());
			logger.debug("Event URI: " + sp.getEventUri());
			// get service
			String domain = URIParser.getDomainFromUri(sp.getEventUri());
			logger.debug("DOMAIN: " + domain);
			Service service = ServiceHelper.getService(domain, "searchmanager");
			logger.debug("SERVICE: " + service);
			if (service == null) {
				logger.error("SearchAction: Service was null");
				return null;
			}
			logger.debug("SERVICE URL: " + service.getUrl());
			logger.debug("SEARCH URI: " + sp.getUri());
			logger.debug("SURI: " + sUri);
			// build final url
			String finalUrl = service.getUrl() + sp.getUri() + sUri;
			logger.debug("FINAL LISA URI: " + finalUrl);
			String response = HttpHelper.sendRequest("GET", finalUrl, null, null);
			logger.debug("RESPONSE\n\t " + response);
			String fsxml = parseLisaResponse(response, sp.isFullResults(), sp.isPropertiesOnly(), sp.isHeadOnly());
			logger.debug("LISA RESPONSE\n\t " + fsxml);
			return fsxml;
		}
		return FSXMLBuilder.getErrorMessage("500", "Malformed query",
				"The query you sent is not valid: " + sp.getQuery(), "http://teamelements.noterik.com/team");
	}
	
	/**
	 * Transforms the lisa response in a fsxml compliant XML.
	 * @param xml
	 * @return
	 */

	private static String parseLisaResponse(String xml, boolean fullResults, boolean propsOnly, boolean headOnly) {
		StringBuffer fsxml = new StringBuffer("<fsxml>");	
		try {
			// parse xml (only add result elements)
			Document doc = DocumentHelper.parseText(xml);
			Node ta = doc.selectSingleNode("//totalResultsAvailable");
			Node tr = doc.selectSingleNode("//totalResultsReturned");
			if(ta != null && tr != null){
				fsxml.append("<properties>");
				fsxml.append(ta.asXML());
				fsxml.append(tr.asXML());
				fsxml.append("</properties>");
			} else {
				fsxml.append("<properties/>");
			}
			if(headOnly){
				fsxml.append("</fsxml>");
				return fsxml.toString();
			}
			logger.debug("PASSED THE HEAD ONLY");
			List<Node> nList = doc.selectNodes("//result");
			Node node;			
			for (Iterator<Node> iter = nList.iterator(); iter.hasNext();) {
				node = iter.next();				
				if (node instanceof Element) {
					logger.debug("FULLRESULTS: " + fullResults);
					if(fullResults){
						String referuri = ((Element)node).attributeValue("referid");
						if(referuri.endsWith("/")){
							referuri = referuri.substring(0, referuri.length() -1);
						}
						logger.debug("REFERID: " + referuri);
						if(referuri != null){
							Document props = null;
							logger.debug("PROPSONLY: " + propsOnly);
							if(propsOnly){
								props = FSXMLRequestHandler.instance().getNodeProperties(referuri, 0, false);
							} else {
								props = FSXMLRequestHandler.instance().getNodeProperties(referuri, false);
							}
							logger.debug("PROPS: " + props);
							if(props != null){
								logger.debug("RESULT PROPERTIES: " + props.asXML());
								try {
									Node n = props.selectSingleNode("//fsxml/" + URIParser.getParentUriPart(referuri));
									fsxml.append("<result id=\""+((Element)node).attributeValue("id")+"\" ");
									fsxml.append("referid=\""+((Element)node).attributeValue("referid")+"\">");
									fsxml.append(n.asXML());
									fsxml.append("</result>");
								} catch(InvalidXPathException e){
									logger.error("Invalid XPATH expression: check the smithers db! error=\n" + e.getMessage() );
								}
							}
						}
					} else {
						fsxml.append(node.asXML());
					}
				}
			}
		} catch (Exception e) {
			logger.error("SearchAction: error parsing response from lisa",e);
		}

		// add fsxml
		fsxml.append("</fsxml>");
		return fsxml.toString();
	}
	
	/**
	 * Returns the client's input XML as a list of elements
	 * @param xml
	 * @return
	 */

	private List<Element> getInputElements(String xml) {
		logger.debug("XML: " + xml);
		Document doc = XMLHelper.asDocument(xml);
		List<Element> elements = new ArrayList<Element>();
		if (doc != null) {
			logger.debug(doc.asXML());
			List<Node> nodes = doc.selectNodes("//fsxml");
			for (Iterator<Node> iter = nodes.iterator(); iter.hasNext();) {
				Node node = iter.next();
				logger.debug("NODE: " + node.getText());
				if (node instanceof Element) {
					elements.add((Element) node);
				}
			}
		}
		return elements;
	}

	public ManualEntry man() {
		return null;
	}
}