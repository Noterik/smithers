package com.noterik.bart.fs.action.ou;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.fs.FSXMLParser;
import com.noterik.springfield.tools.fs.model.config.ingest.EncodingProfile;
import com.noterik.springfield.tools.fs.model.config.ingest.IngestConfig;

/**
 * Action for OU that adds the rawvideo/2 when the rawvideo/1 is ingested
 * 
 * @author Jaap Blom <j.blom@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.action
 * @access private
 * @version $Id: OUCreateMomarRawAction.java,v 1.11 2011-06-24 08:00:06 derk Exp $
 * 
 */
public class OUCreateMomarRawAction extends ActionAdapter {

	private static Logger logger = Logger.getLogger(OUCreateMomarRawAction.class);

	@Override
	public String run() {
		
		logger.debug("\n\n ######### starting create momar raw action ######\n\n");
		String requestBody = event.getRequestData();
		logger.debug("\n\nRequest Data: " + requestBody);
		String mount = null;
		Document doc = null;
		try {
			if (requestBody != null) {
				doc = DocumentHelper.parseText(requestBody);
				Node node = doc.selectSingleNode("//properties/mount");
				Node useMomar = doc.selectSingleNode("//properties/momar");
				
				if(useMomar != null && useMomar.getText().toLowerCase().equals("false")){
					logger.debug("\n\n Momar tag is false ! Will not create Momar Job !!\n");
					return null;
				}
				
				if (node != null) {
					mount = node.getText();
					logger.debug("\n\n\n\n\nFOUND MOUNT: " + mount);
				} else {
					logger.debug("\n\n\n\n\nNO MOUNT FOUND IN PROPERTIES");
				}
			}
		} catch (DocumentException e) {
			logger.error("",e);
		}
		String uri = event.getUri();
		logger.debug("OU CREATE RAW 2 FOR MOMAR ACTION:\nURI: " + uri);
		String raw2Uri = uri.substring(0, uri.lastIndexOf("/")) + "/2";
		if (!FSXMLRequestHandler.instance().hasProperties(raw2Uri) && mount != null) {			
			createRaw2Properties(raw2Uri, mount);
		}
		return null;
	}

	private void createRaw2Properties(String raw2Uri, String mount) {
		// get the ingest config
		Document doc = FSXMLRequestHandler.instance().getNodeProperties("/domain/ou/config/ingest", false);
		if (doc != null) {
			String confXml = doc.asXML();
			IngestConfig ic = FSXMLParser.getIngestConfigFromXml(confXml);
			// get encoding profile #2 from the config --> raw index = 2
			EncodingProfile ep = ic.getVideoSettings().getRawVideos().get("2").getEncodingProfile();
			// get the xml for the rawvideo #2 with the encoding profile
			String xml = getRaw2Xml(ep, mount);
			// set the xml to the rawvideo 2
			String response = FSXMLRequestHandler.instance().handlePUT(raw2Uri + "/properties", xml.toString());
			logger.debug(response);
		} else {
			logger.debug("No ingest config defined for OU!");
		}
	}

	private String getRaw2Xml(EncodingProfile ep, String mount) {
		StringBuffer xml = new StringBuffer("<fsxml><properties>");
		xml.append("<reencode>true</reencode>");
		xml.append("<mount>"+mount+"</mount>");
		xml.append("<format>" + ep.getFormat() + "</format>");
		xml.append("<extension>" + ep.getExtension() + "</extension>");
		xml.append("<wantedwidth>" + ep.getWidth() + "</wantedwidth>");
		xml.append("<wantedheight>" + ep.getHeight() + "</wantedheight>");
		xml.append("<wantedbitrate>" + ep.getBitRate() + "</wantedbitrate>");
		xml.append("<wantedframerate>" + ep.getFrameRate() + "</wantedframerate>");
		xml.append("<wantedkeyframerate>" + ep.getKeyFrameRate() + "</wantedkeyframerate>");
		xml.append("<wantedaudiobitrate>" + ep.getAudioBitRate() + "</wantedaudiobitrate>");		
		xml.append("</properties></fsxml>");
		return xml.toString();
	}

}