package com.noterik.bart.fs.fscommand;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.XMLHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;

public class UpdatePresentationCommand implements Command {
	/** Logger */
	private static Logger logger = Logger.getLogger(UpdatePresentationCommand.class);
	
	public String execute(String url, String xml) {	
		logger.debug("Updating properties of presentation "+url);
		logger.debug("Updating properties xml "+xml);
		
		Document doc = XMLHelper.asDocument(xml);
		List<Node> properties;
		
		//TODO: validate ticket
		
		//add every property in the xml supplied		
		properties = doc.selectNodes("//properties/*");
		
		for (Iterator<Node> it = properties.iterator(); it.hasNext(); ) {
			Node property = it.next();
			
			if (!property.getName().equals("ticket")) {			
				logger.debug("updating property "+property.getName()+" with value "+property.getText());
			
				FSXMLRequestHandler.instance().handlePUT(url+"/properties/"+property.getName(), property.getText());
			}
		}		
		return FSXMLBuilder.getFSXMLStatusMessage("The properties where successfully added", "", "");
	}
	
	public ManualEntry man() {
		return null;
	}
}
