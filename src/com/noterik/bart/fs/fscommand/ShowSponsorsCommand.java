package com.noterik.bart.fs.fscommand;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.fs.URIParser;

public class ShowSponsorsCommand implements Command {

	/** Logger */
	private static Logger logger = Logger.getLogger(ShowSponsorsCommand.class);

	public String execute(String uri, String xml) {
		int numSponsors = 0;
		Element user;
		Document typeDocument = DocumentHelper.createDocument();
		
		Element fsxml = typeDocument.addElement("fsxml");
		Element properties = fsxml.addElement("properties");
		
		String domain = URIParser.getDomainFromUri(uri);		
		String userUri = "/domain/"+domain+"/user";
		
		Document userDoc = FSXMLRequestHandler.instance().getNodePropertiesByType(userUri, 0, 0, 999999);
		List<Node> users = userDoc.selectNodes("//user");
		
		logger.debug("# of users = "+users.size());
		
		//loop over all users of domain
		for(Iterator<Node> iter = users.iterator(); iter.hasNext(); ) {
			user = (Element) iter.next();
			String userid = user.selectSingleNode("@id") == null ? "" : user.selectSingleNode("@id").getText();
			
			logger.debug("user = "+userid);			
			String sponsorUri = "/domain/"+domain+"/user/"+userid+"/sponsor";
			Document sponsor = FSXMLRequestHandler.instance().getNodePropertiesByType(sponsorUri, 0, 0, 1);
			int sponsorItems = sponsor.selectSingleNode("//totalResultsAvailable") == null ? 0 : Integer.parseInt(sponsor.selectSingleNode("//totalResultsAvailable").getText());
		
			if (sponsorItems > 0) {
				logger.debug(userid+ " sponsors "+sponsorItems);
				numSponsors++;
				fsxml.addElement("sponsor").addAttribute("id", userid);
			}
		} 
		properties.addElement("totalResultsAvailable").addText(Integer.toString(numSponsors));		
		return typeDocument.asXML();
	}
	
	public ManualEntry man() {
		return null;
	}
}
