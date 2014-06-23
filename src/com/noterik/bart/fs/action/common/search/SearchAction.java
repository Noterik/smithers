package com.noterik.bart.fs.action.common.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.bart.fs.search.SearchParams;
import com.noterik.bart.fs.search.SearchQueueManager;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * Action that performs searches (referred to in: /xml/search.xml). This class
 * will send the XML read from the input of this script and pass it to the
 * proper search queue (each domain has a search queue).
 * 
 * Input requirements: - uri - query (optional) - pruning parameters; start,
 * limit (optional) - children true/false (optional) - sorting parameters
 * (optional)
 * 
 * @author Jaap Blom <j.blom@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * 
 */
public class SearchAction extends ActionAdapter {
	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(SearchAction.class);

	/**
	 * search properties
	 */
	private Map<String, String> sProperties = new HashMap<String, String>();

	@Override
	public String run() {
		// parse input
		Element element = null;
		String eId = null;
		SearchParams sp = null;
		logger.debug(":::::: start of search script ::::::");
		List<Element> iElements = getInputElements(event.getUri());
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
							}
						}
						sProperties.put(n, t);
					}
				}
			}
		}
		if (sp == null) {
			logger.error("SearchAction: No input parameters were specified");
			return null;
		}
		sp.setEventUri(event.getUri());
		// check on uri
		if (sp.getUri() == null) {
			logger.error("SearchAction: Required input parameters not specified");
			return null;
		}
		SearchQueueManager.instance().getSearchQueueOfDomain(URIParser.getDomainFromUri(event.getUri())).addToQueue(sp);
		logger.debug("::::: end of search action ::::::");
		return null;
	}

	private List<Element> getInputElements(String uri) {
		Document doc = FSXMLRequestHandler.instance().getNodeProperties(uri, false);
		logger.debug(doc.asXML());
		List<Node> nodes = doc.selectNodes("//input");
		List<Element> elements = new ArrayList<Element>();
		for (Iterator<Node> iter = nodes.iterator(); iter.hasNext();) {
			Node node = iter.next();
			logger.debug("NODE: " + node.getText());
			if (node instanceof Element) {
				elements.add((Element) node);
			}
		}
		return elements;
	}

}