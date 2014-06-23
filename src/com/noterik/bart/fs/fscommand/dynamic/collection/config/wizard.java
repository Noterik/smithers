package com.noterik.bart.fs.fscommand.dynamic.collection.config;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.fscommand.DynamicCommand;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.XMLHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;

public class wizard implements DynamicCommand {
	/** Logger */
	private static Logger logger = Logger.getLogger(wizard.class);
	
	public String run(String uri,String xml) {	
		logger.debug("start dynamic/collection/config/wizard");
		
		Document doc = XMLHelper.asDocument(xml);
		
		if(doc == null){
			return FSXMLBuilder.getErrorMessage("403", "The value you sent is not valid",
					"You have to POST a valid command XML", "http://teamelements.noterik.nl/team");
		}
		
		String user = doc.selectSingleNode("//properties/user") == null ? "" : doc.selectSingleNode("//properties/user").getText();	
		String ticket = doc.selectSingleNode("//properties/ticket") == null ? "" : doc.selectSingleNode("//properties/ticket").getText();
		
		//TODO: validate user/ticket

		//get collection
		Document collection = FSXMLRequestHandler.instance().getNodeProperties(uri, false);
		logger.debug(collection.asXML());
		
		//get roles
		Element wizard;		
		List<Node> wizards = collection.selectNodes("//config[@id='1']/wizard");
		Node userRole = collection.selectSingleNode("//config[@id='roles']/user[@id='"+user+"']/properties/wizard");
		
		if (userRole == null) {			
			logger.debug("user not found --> default wizard");
			// Loop all wizards
			for(Iterator<Node> iter = wizards.iterator(); iter.hasNext(); ) {
				wizard = (Element) iter.next();
				
				String wizardId = wizard.attribute("id") == null ? "" : wizard.attribute("id").getText();
				if (!wizardId.equals("1")) {
					logger.debug("detach wizard with id "+wizardId);
					wizard.detach();
				}
			}			
		} else {
			logger.debug("user found "+userRole.asXML());
			String roles = userRole.getText();
			String[] results = roles.split(",");			
			
			// Loop all wizards
			for(Iterator<Node> iter = wizards.iterator(); iter.hasNext(); ) {
				wizard = (Element) iter.next();
							
				String wizardId = wizard.attribute("id") == null ? "" : wizard.attribute("id").getText();
				if (!inArray(results,wizardId)) {
					logger.debug("detach wizard with id "+wizardId);
					wizard.detach();
				}
			}
		}
		
		//detach config roles
		Node configRoles = collection.selectSingleNode("//config[@id='roles']");
		configRoles.detach();

		return collection.asXML();
	}
	
	private boolean inArray(String[] haystack,String needle) {
		for (int i = 0; i < haystack.length;i++) {
			if (haystack[i].equals(needle)) {
				return true;
			}
		}
		return false;
	}
}
