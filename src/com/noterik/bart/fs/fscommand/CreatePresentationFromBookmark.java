package com.noterik.bart.fs.fscommand;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.XMLHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

public class CreatePresentationFromBookmark implements Command {
	/** Logger */
	private static Logger logger = Logger.getLogger(CreatePresentationFromBookmark.class);
	
	private String uri;
	private String xml;
	private String domainid;
	private String user;
	
	public String execute(String url, String xmlContent) {
		uri = url;
		xml = xmlContent;
		
		return handlePresentation();
	}
	
	private String handlePresentation() {
		//get collection where to put presentation in
		Document config = XMLHelper.asDocument(xml);
		String collectionUri = config.selectSingleNode("//collection") == null ? "" : config.selectSingleNode("//collection").getText();
		
		if (collectionUri.equals("")) {
			logger.error("no collection supplied");
			return FSXMLBuilder.getErrorMessage("500","No collection supplied", "Please provide a collection", "http://teamelements.noterik.com/team");
		}
		
		domainid = URIParser.getDomainIdFromUri(collectionUri);
		user = URIParser.getUserIdFromUri(collectionUri);
		
		if (domainid.equals("")) {
			logger.error("got no domain from uri");
			return FSXMLBuilder.getErrorMessage("500","No domain found", "Please provide a collection with domain", "http://teamelements.noterik.com/team");
		}
		if (user.equals("")) {
			logger.error("got no user from uri");
			return FSXMLBuilder.getErrorMessage("500","No user found", "Please provide a collection with user", "http://teamelements.noterik.com/team");
		}
		
		//get collection presentation
		Document collectionPresentation = FSXMLRequestHandler.instance().getNodeProperties(uri,false);
		String presentationUri = collectionPresentation.selectSingleNode("//presentation/@referid") == null ? "" : collectionPresentation.selectSingleNode("//presentation/@referid").getText();
		
		if (presentationUri.equals("")) {
			logger.error("no presentation refered");
			return FSXMLBuilder.getErrorMessage("500","No presentation refered", "", "http://teamelements.noterik.com/team");
		}
		
		Document presentation = FSXMLRequestHandler.instance().getNodeProperties(presentationUri,false);
		
		//remove statistics from original presentation
		Element statistics = (Element) presentation.selectSingleNode("//statistics");
		if (statistics != null) {
			statistics.detach();
		}
		
		StringBuffer presentationXml = new StringBuffer();
		
		// exception for first node
		Element root = presentation.getRootElement();
		Node first = root.selectSingleNode("//presentation");
		List<Node> children = first.selectNodes("child::*");
		for(Iterator<Node> iter = children.iterator(); iter.hasNext(); ) {
			Node node = iter.next();
			presentationXml.append(node.asXML());
		}
		String pXml = "<fsxml>"+presentationXml.toString()+"</fsxml>";
		String response = FSXMLRequestHandler.instance().handlePOST("/domain/"+domainid+"/user/"+user+"/presentation", pXml);
		
		//response contains new presentation uri we have to refer to in collection
		Document resp = XMLHelper.asDocument(response);
		
		logger.debug("response = "+resp.asXML());
		String newPresentationUri = resp.selectSingleNode("//status[@id='400']/properties/uri") == null ? "" : resp.selectSingleNode("//status[@id='400']/properties/uri").getText();
		
		if (newPresentationUri.equals("")) {
			logger.error("could not create presentation");
			return FSXMLBuilder.getErrorMessage("500","Could not create presentation", "Try again later", "http://teamelements.noterik.com/team");
		}
		
		//refer new presentation in the collection
		String response2 = FSXMLRequestHandler.instance().handlePOST(collectionUri+"/presentation", "<fsxml><attributes><referid>"+newPresentationUri+"</referid></attributes></fsxml>");
		
		Document resp2 = XMLHelper.asDocument(response2);
		logger.debug("response 2 = "+resp2.asXML());
		
		String newCollectionPresentationUri = resp2.selectSingleNode("//status[@id='400']/properties/uri") == null ? "" : resp2.selectSingleNode("//status[@id='400']/properties/uri").getText();
		
		if (newCollectionPresentationUri.equals("")) {
			logger.error("could not create refer presentation in collection");
			return FSXMLBuilder.getErrorMessage("500","Could not create presentation in collection", "Try again later", "http://teamelements.noterik.com/team");
		}
		
		return resp2.asXML();
	}
	
	public ManualEntry man() {
		return null;
	}
}
