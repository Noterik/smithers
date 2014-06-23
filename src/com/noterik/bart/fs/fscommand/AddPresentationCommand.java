package com.noterik.bart.fs.fscommand;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.dom4j.Document;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.XMLHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

public class AddPresentationCommand implements Command {
	/** Logger */
	private static Logger logger = Logger.getLogger(AddPresentationCommand.class);
	
	public String execute(String url, String xml) {		
		logger.debug("Adding properties of presentation "+url);
		logger.debug("Adding properties xml "+xml);
		
		Document doc = XMLHelper.asDocument(xml);
		String ticket = "";
		String title = "";
		String description = "";
		
		if(doc == null){
			logger.error("Could not parse xml");
			return FSXMLBuilder.getErrorMessage("500","No xml found", "Please provide xml", "");
		} else {
			ticket = doc.selectSingleNode("//properties/ticket") == null ? "" : doc.selectSingleNode("//properties/ticket").getText();
			title = doc.selectSingleNode("//properties/title") == null ? "" : doc.selectSingleNode("//properties/title").getText();
			description = doc.selectSingleNode("//properties/description") == null ? "" : doc.selectSingleNode("//properties/description").getText();
		}
		
		logger.debug("ticket = "+ticket+" title = "+title+" description = "+description);
		
		if (title.equals("") || description.equals("")) {
			return FSXMLBuilder.getErrorMessage("500","Not all properties found", "Please provide ticket, title, description", "");
		}
		
		String domain = URIParser.getDomainIdFromUri(url);
		String user = URIParser.getUserIdFromUri(url);
		
		logger.debug("domain = "+domain);
		logger.debug("user = "+user);	
		
		//TODO: validate ticket
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		String current = sdf.format(new Date());		
		String pXml = "<fsxml><properties><title>"+title+"</title><description>"+description+"</description>";
		pXml += "<date_created>"+current+"</date_created><livestate>preview</livestate></properties></fsxml>";
		
		//create presentation
		String pResponse = FSXMLRequestHandler.instance().handlePOST(url, pXml);
		
		logger.debug("response = "+pResponse);
		
		Document resp = XMLHelper.asDocument(pResponse);
		if (resp == null) {
			logger.error("Could not read xml response from creating presentation");
			return FSXMLBuilder.getErrorMessage("500","Error creating presentation", "Could not create presentation", "");
		}
		
		String pRefer = resp.selectSingleNode("//properties/uri") == null ? "" : resp.selectSingleNode("//properties/uri").getText();
		logger.debug("refer = "+pRefer);
		String cXml = "<fsxml><attributes><referid>"+pRefer+"</referid></attributes></fsxml>";

		//create collection presentation
		String cResponse = FSXMLRequestHandler.instance().handlePOST("/domain/"+domain+"/user/"+user+"/collection/default/presentation", cXml);
		logger.debug("response = "+cResponse);
		
		return pResponse;
	}
	
	public ManualEntry man() {
		return null;
	}
}
