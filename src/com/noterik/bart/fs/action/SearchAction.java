/* 
* SearchAction.java
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
package com.noterik.bart.fs.action;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.bart.fs.restlet.tools.ServiceHelper;
import com.noterik.bart.marge.model.Service;
import com.noterik.springfield.tools.HttpHelper;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * Action that performs searches. 
 * 
 * Input requirements:
 * - uri 
 * - query (optional)
 * - pruning parameters; start, limit (optional)
 * - children true/false (optional)
 * - sorting parameters (optional)
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.action
 * @access private
 * @version $Id: SearchAction.java,v 1.8 2011-07-01 11:38:56 derk Exp $
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
	private Map<String,String> sProperties = new HashMap<String,String>();
	
	@Override
	public String run() {	
		// parse input
		Element element;
		String eId;
		List<Element> iElements = script.getInput().getInputElements();
		for(Iterator<Element> iter = iElements.iterator(); iter.hasNext(); ) {
			element = iter.next();
			eId = element.attributeValue("id");
			if(eId!=null && eId.equals("search")) {
				// parse properties
				Node pNode = element.selectSingleNode("properties");
				if(pNode!=null) {
					List<Node> children = pNode.selectNodes("child::*");
					Node child;
					for(Iterator<Node> cIter = children.iterator(); cIter.hasNext(); ) {
						child = cIter.next();
						sProperties.put(child.getName(), child.getText());
					}
				}
			}
		}
		
		// check on uri
		String uri = sProperties.get("uri");
		if(uri==null) {
			logger.error("SearchAction: Required input parameters not specified");
			return null;
		}
		
		// build search uri
		String sUri = "", key, value;
		for(Iterator<String> iter = sProperties.keySet().iterator(); iter.hasNext(); ) {
			key = iter.next();
			if(!key.equals("uri")) {
				value = sProperties.get(key); 
				sUri += "&" + key + "=" + value;
			}
		}
		sUri = sUri.replaceFirst("&", "?");
		
		// get service
		String domain = URIParser.getDomainFromUri(uri);
		Service service = ServiceHelper.getService(domain, "searchmanager");
		if(service==null) {
			logger.error("SearchAction: Service was null");
			return null;
		}
		
		// build final url
		String finalUrl = service.getUrl() + uri + sUri;
		
		logger.error("SearchAction: final url was " + finalUrl);
		
		// do request
		String response = HttpHelper.sendRequest("GET", finalUrl, null, null);
		
		// parse response
		String fsxml = parse2fsxml(response);
		
		logger.debug("result fsxml is " + fsxml);
		
		// delete old
		FSXMLRequestHandler.instance().handleDELETE(script.getID() + "/output/"+id+"/result", null);
		
		// save fsxml in output
		script.getOutput().setOutput(fsxml);
		
		return null;
	}
	
	/**
	 * Parses lisa response to fsxml
	 * @param xml
	 * @return
	 */
	private static String parse2fsxml(String xml) {
		String fsxml = "";
		
		// start with empty properties
		fsxml += "<properties />";
		
		try {
			// parse xml (only add result elements)
			Document doc = DocumentHelper.parseText(xml);
			List<Node> nList = doc.selectNodes("//result");
			Node node;
			for(Iterator<Node> iter = nList.iterator(); iter.hasNext(); ) {
				node = iter.next();
				if(node instanceof Element) {
					fsxml += node.asXML();
				}
			}
		} catch(Exception e) {
			logger.error("SearchAction: error parsing response from lisa",e);
		}
		
		// add fsxml
		fsxml = "<fsxml>"+fsxml+"</fsxml>";
		
		return fsxml;
	}
}
