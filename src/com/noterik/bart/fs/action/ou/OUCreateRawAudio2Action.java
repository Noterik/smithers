package com.noterik.bart.fs.action.ou;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;

/**
 * Action for OU that adds the rawaudio/2 when the rawaudio/1 is ingested
 * 
 * @author Jaap Blom <j.blom@noterik.nl>
 * @author Levi Pires <l.pires@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.action
 * @access private
 * @version $Id: OUCreateRawAudio2Action.java,v 1.7 2011-06-24 08:00:06 derk Exp $
 * 
 */
public class OUCreateRawAudio2Action extends ActionAdapter {

	private static Logger logger = Logger.getLogger(OUCreateRawAudio2Action.class);

	@Override
	public String run() {
		String requestBody = event.getRequestData();
		String mount = null;
		Document doc = null;
		
		logger.debug("********************* starting OUCreateRawAudio2Action***************");
		logger.debug("request data: " + requestBody);
		try {
			if (requestBody != null) {
				doc = DocumentHelper.parseText(requestBody);
				Node node = doc.selectSingleNode("//properties/mount");
				Node momarNode = doc.selectSingleNode("//properties/momar");
				
				/*
				if(momarNode != null && momarNode.getText().toLowerCase().equals("false")){
					logger.debug("Momar tag is false, will not create rawaudio2 for momar !!");
					return null;
				}
				*/
				
				if (node != null) {
					mount = node.getText();
					logger.debug("FOUND MOUNT: " + mount);
				} else {
					logger.debug("NO MOUNT FOUND IN PROPERTIES");
				}
			}
		} catch (DocumentException e) {
			logger.error("",e);
		}
		String uri = event.getUri();
		logger.debug("OU CREATE RAW AUDIO 2 FOR MOMAR ACTION:\nURI: " + uri);
		String raw2Uri = uri.substring(0, uri.lastIndexOf("/")) + "/2";
		if (!FSXMLRequestHandler.instance().hasProperties(raw2Uri) && mount != null) {			
			createRaw2Properties(raw2Uri, mount);
		}
		return null;
	}

	private void createRaw2Properties(String raw2Uri, String mount) {
		// get the ingest config
		String xml = getRaw2Xml(mount);
		// set the xml to the rawaudio 2
		String response = FSXMLRequestHandler.instance().handlePUT(raw2Uri + "/properties", xml.toString());
		logger.debug(response);
	}

	private String getRaw2Xml(String mount) {
		StringBuffer xml = new StringBuffer("<fsxml><properties>");		
		xml.append("<mount>"+mount+"</mount>");
		xml.append("<transferred>false</transferred>");
		xml.append("</properties></fsxml>");
		return xml.toString();
	}

}