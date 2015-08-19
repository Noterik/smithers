/* 
* MoveCommand.java
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
package com.noterik.bart.fs.fscommand;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.bart.fs.restlet.tools.FSXMLHelper;
import com.noterik.springfield.tools.XMLHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.FSXMLParser;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * Responsible for moving resources.
 * 
 * <fsxml mimetype="application/fscommand" id="move">
 * 		<properties>
 * 			<source> source uri </source>
 * 			<destination> destination uri </destination>
 * 			<params></params>
 * 		</properties>
 * </fsxml>
 * 
 * parameters:
 * - o override TODO
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2009
 * @package com.noterik.bart.fs.fscommand
 * @access private
 * @version $Id: MoveCommand.java,v 1.7 2011-08-15 12:49:19 derk Exp $
 *
 */
public class MoveCommand implements Command {
	
	/** the CopyingCommand's log4j Logger */
	private static Logger logger = Logger.getLogger(MoveCommand.class);
	
	/** request handler */
	private static FSXMLRequestHandler rHandler = FSXMLRequestHandler.instance();
	
	/**
	 * Execute the copying command.
	 */
	public String execute(String uri, String xml) {	
		logger.debug("input xml (move): " + xml);
		
		// get input parameters and run command
		Properties input = getInputParameters(xml);
		if(input != null) {
			return move(input, uri);
		}
		
		// error message
		return FSXMLBuilder.getErrorMessage("500", "No input found", "Please call this command as follows: mv [OPTION] SOURCE DESTINATION", "http://teamelements.noterik.com/team");
	}
	
	/**
	 * Move resource specified by the input parameters.
	 * 
	 * @param input		input parameters
	 * @param uri		local directory
	 * @return			status/error message
	 */
	private String move(Properties input, String uri) {
		String res = null;
		
		// get input parameters
		String source = input.getProperty("source");
		String destination = input.getProperty("destination");
		String params = input.getProperty("params");
		logger.debug("source: "+source+", destination: "+destination+", params: "+params);
		
		
		// determine optional parameters
		boolean override=false;
		if(params!=null) {
			override = params.contains("-o");
		}
		
		// check input parameters
		if( source == null || destination == null){
			return FSXMLBuilder.getErrorMessage("500", "No source or destination", "Please provide the SOURCE and DESTINATION input parameters", "http://teamelements.noterik.com/team");
		}
		
		// resolve uris
		String sourceUri = URIParser.resolveLocalUri(source, uri);
		if (sourceUri.lastIndexOf("/") == sourceUri.length() - 1) {
			sourceUri = sourceUri.substring(0, sourceUri.lastIndexOf("/"));
		}
		String destinationUri = URIParser.resolveLocalUri(destination, uri);
		if (destinationUri.lastIndexOf("/") == destinationUri.length() - 1) {
			destinationUri = destinationUri.substring(0, destinationUri.lastIndexOf("/"));
		}
		logger.debug("ABSOLUTE URIS -- source: " + sourceUri + ", destination: " + destinationUri);
		
		// check if uri is an uri of type id
		String typeSource = URIParser.getResourceTypeFromUri(sourceUri);
		String cpDest = URIParser.getCurrentUriPart(destinationUri);
		if(!URIParser.isResourceId(sourceUri)) {
			return FSXMLBuilder.getErrorMessage("500", "Invalid source specified, only id nodes permitted.", "Invalid source specified, only id nodes permitted.", "http://teamelements.noterik.com/team");
		}
		
		String docStr = null;
		String refer = null;
		
		// get properties of source	
		Document doc = getPropertiesOfUri(sourceUri,-1);
		logger.debug("document being created: "+doc.asXML());
		
		// exception for first node
		Element root = doc.getRootElement();
		Node first = root.selectSingleNode("//"+typeSource);
		refer = first.valueOf("@referid");
		List<Node> children = first.selectNodes("child::*");
		for(Iterator<Node> iter = children.iterator(); iter.hasNext(); ) {
			Node node = iter.next();
			docStr += node.asXML();
		}
		docStr = "<fsxml>"+docStr+"</fsxml>";
		logger.debug("document being created after first node exception: "+docStr);
		
		// check if dest ends with 'properties'
		String newResourceUri=null;
		if (cpDest.equals(FSXMLHelper.XML_PROPERTIES)) {
			logger.debug("putting to "+destinationUri);
			res = rHandler.handlePUT(destinationUri , docStr);
			newResourceUri = URIParser.getParentUri(destinationUri);
		} else if(URIParser.isResourceId(destinationUri)) { 
			destinationUri += "/"+FSXMLHelper.XML_PROPERTIES;
			logger.debug("putting to "+destinationUri);
			res = rHandler.handlePUT(destinationUri , docStr);
			newResourceUri = URIParser.getParentUri(destinationUri);
		} else {
			logger.debug("posting to "+destinationUri);
			res = rHandler.handlePOST(destinationUri , docStr);
			try {
				if(FSXMLParser.getErrorMessageFromXml(res)==null) { 
					Document respDoc = DocumentHelper.parseText(res);
					newResourceUri = respDoc.valueOf("//uri");
				} 
			} catch(Exception e) {
				logger.error("",e);
			}
		}
		
		// add refer of first node
		if(refer!=null && !refer.equals("")) {
			logger.debug("adding refer for first node (" + refer + ")");
			boolean ok = rHandler.saveAttributes(newResourceUri, "<fsxml><attributes><referid>"+refer+"</referid></attributes></fsxml>", "PUT");
			logger.debug("attributes added: "+ok);
		} else {
			logger.debug("not need for adding refer id");
		}
		
		// check if copy was successful and remove original
	    String vError = FSXMLParser.getErrorMessageFromXml(res);
	    if(vError!=null) {
	    	logger.error("copy before moving was unsuccessful.");
	    } else {
	    	logger.debug("copy before moving was successful.");
	    	FSXMLRequestHandler.instance().deleteNodeProperties(sourceUri, true);
	    }
		
		logger.debug("response: " + res);
		return res;
	}
	
	/**
	 * Returns the input parameters.
	 * 
	 * @param xml	The xml specifying the commands parameters.
	 * @return		The input parameters.
	 */
	private Properties getInputParameters(String xml){
		Properties props = new Properties();
		Document doc = XMLHelper.asDocument(xml);
		if(doc == null){
			return null;
		} else {
			Node n = doc.selectSingleNode("./fsxml/properties/source");			
			if(n != null && n instanceof Element){
				props.put("source", ((Element)n).getText());				
			}
			n = doc.selectSingleNode("./fsxml/properties/destination");
			if(n != null && n instanceof Element){
				props.put("destination", ((Element)n).getText());				
			}
			n = doc.selectSingleNode("./fsxml/properties/params");
			if(n != null && n instanceof Element){
				props.put("params", ((Element)n).getText());				
			}			
		}		
		logger.debug(props.toString());
		return props;
	}

	/**
	 * 
	 * @param uri
	 * @return
	 */
	private Document getPropertiesOfUri(String uri, int depth) {		
		// refactor uri
		String cp = URIParser.getCurrentUriPart(uri);
		if(cp.equals(FSXMLHelper.XML_PROPERTIES)) {
			uri = URIParser.getParentUri(uri);
		}
		
		// get complete
		Document pDoc = null;
		if(depth==-1) {
			pDoc = rHandler.getNodeProperties(uri, true);
		} else {
			pDoc = rHandler.getNodeProperties(uri, depth, true);
		}
		
		// loop through xml and check referid's
		List<Node> rNodes = pDoc.selectNodes("//@referid");
		logger.debug("rNodes: " + rNodes);
		for(Iterator<Node> iter = rNodes.iterator(); iter.hasNext(); ) {
			// get referid attribute and parent node
			Node node = iter.next();
			String referid = node.getText();
			Element parent = node.getParent();			
			logger.debug("parent: "+parent.asXML()+", refer: "  + referid);
			
			// get properties of referid
			Document rDoc = rHandler.getNodeProperties(referid, 0, false);
			logger.debug("rDoc: "+rDoc.asXML());
			Node properties = rDoc.selectSingleNode("//properties");
			List<Node> pNodes = properties.selectNodes("child::*");
			logger.debug("pNodes: "+pNodes);
			for(Iterator<Node> iter2 = pNodes.iterator(); iter2.hasNext(); ) {
				// select the same property elements that are in parent element and refer properties
				Node prop = iter2.next();
				List<Node> parentPNodes = parent.selectNodes("properties/child::*");
				for(Node parentPropNode : parentPNodes) {
					if(parentPropNode.getName().equals(prop.getName()) && parentPropNode.getText().equals(prop.getText())) {
						logger.debug("removing: " + parentPropNode.asXML());
						parentPropNode.detach();
					}
				}
			}
		}
		
		return pDoc;
	}
	
	public ManualEntry man() {
		return null;
	}
}