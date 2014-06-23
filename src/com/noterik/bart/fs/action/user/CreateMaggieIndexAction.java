package com.noterik.bart.fs.action.user;

import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.fs.URIParser;

public class CreateMaggieIndexAction extends ActionAdapter {
	/** the CreateMaggieIndexAction's log4j Logger */
	private static Logger logger = Logger.getLogger(CreateMaggieIndexAction.class);
	
	/** place where all the index settings are stored */
	private static final String GENERIC_MAGGIE_CONFIG_URI = "/domain/{domain}/config/maggie";
	
	/** generic pattern user in the maggie configuration */
	private static final String GENERIC_PATTERN = "<![CDATA[<{indexsavetype} (.*?)</{indexsavetype}>]]>"; 
	
	private String domain;
	private String user;
	private String index;
	private Properties eProperties;
	
	@Override
	public String run() {
		logger.debug("starting run");
		
		// parse event data
		parseEventData();
		
		// create user index if needed
		String indexURI = eProperties.getProperty("indexuri");
		if(!existsInMaggieConfiguration(indexURI)) {
			createMaggieConfiguration(indexURI);
		}
		
		return null;
	}
	
	/**
	 * Creates an index in the maggie configuration
	 * 
	 * @param indexURI
	 */
	private void createMaggieConfiguration(String indexURI) {
		logger.debug("creating maggie configuration index");
		
		// determine configuration properties
		String indextype = eProperties.getProperty("indextype");
		String indexsavetype = eProperties.containsKey("indexsavetype") ? eProperties.getProperty("indexsavetype") : indextype;
		String pattern = GENERIC_PATTERN.replace("{indexsavetype}", indexsavetype);
		
		// insert configuration properties
		Properties cProperties = new Properties();
		cProperties.put("uri",indexURI);
		cProperties.put("pattern",pattern);
		cProperties.put("item",indexsavetype);
		
		// generate xml and URI
		String uri = GENERIC_MAGGIE_CONFIG_URI.replace("{domain}", domain) + "/index";
		String xml = generateXML(cProperties);
		logger.debug("creating maggie configuration -- uri: "+uri+", properties: "+cProperties);
		
		// save
		FSXMLRequestHandler.instance().handlePOST(uri, xml);
	}

	/**
	 * Parses event data
	 */
	private void parseEventData() {
		logger.debug("parsing event data for event: "+event);
		
		String uri = event.getUri();
		domain = URIParser.getDomainFromUri(uri);
		user = URIParser.getUserFromUri(uri);
		index = URIParser.getCurrentUriPart(uri);
		
		String data = event.getRequestData();
		eProperties = new Properties();
		try {
			Document doc = DocumentHelper.parseText(data);
			List<Node> pList = doc.selectNodes("//properties/child::*");
			for(Node pNode : pList) {
				eProperties.put(pNode.getName(), pNode.getText());
			}
		} catch (DocumentException e) {
			logger.error("",e);
		}
	}
	
	/**
	 * Checks if a resource exists in the filesystem
	 * 
	 * @param uri
	 * @return
	 */
	private boolean existsInMaggieConfiguration(String uri) {
		String maggieConfigurationURI = GENERIC_MAGGIE_CONFIG_URI.replace("{domain}", domain);
		Document doc = FSXMLRequestHandler.instance().getNodeProperties(maggieConfigurationURI, false);
		List<Node> nList = doc.selectNodes("//index/properties/uri");
		for(Node node : nList) {
			String indexURI = node.getText();
			if(indexURI.trim().equals(uri)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns an FSXML string given Properties
	 * 
	 * @param properties
	 * @return
	 */
	private String generateXML(Properties properties) {
		StringBuffer buff = new StringBuffer();
		buff.append("<fsxml>");
		buff.append("<properties>");
		for(String key : properties.stringPropertyNames()) {
			String value = properties.getProperty(key);
			buff.append("<"+key+">"+value+"</"+key+">");
		}
		buff.append("</properties>");
		buff.append("</fsxml>");
		return buff.toString();
	}
}