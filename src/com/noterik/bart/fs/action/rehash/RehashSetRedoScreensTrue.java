package com.noterik.bart.fs.action.rehash;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;

/**
 * Action for Rehash that will set the redo tag to true in the screens properties
 * 
 * @author Levi Pires <l.pires@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.action
 * @access private
 * @version $Id: RehashSetRedoScreensTrue.java,v 1.4 2011-06-24 08:00:06 derk Exp $
 * 
 */
public class RehashSetRedoScreensTrue extends ActionAdapter {

	private static Logger logger = Logger.getLogger(RehashSetRedoScreensTrue.class);

	@Override
	public String run() {
		
		logger.debug("**************************** starting RehashSetRedoScreensTrue ************************");
		String requestBody = event.getRequestData();
		String uri = event.getUri();
		
		logger.debug("request body: " + requestBody);
		logger.debug("uri: " + uri);
		
		try {
			Document doc = DocumentHelper.parseText(requestBody);			
			Node mtNode = doc.selectSingleNode("//properties/mount");
			
			logger.debug("about to check for mount");
			
			if(mtNode != null){
			
				logger.debug("Mounts are set, redo will be set to true");
				// get uri of video
				String vidUri = uri.substring(0, uri.lastIndexOf("/rawvideo"));
				
				// get uri of redo tag
				String redoUri = vidUri + "/screens/1/properties/redo";
				
				logger.debug("redoUri: " + redoUri);
				
				// set the tag to true
				FSXMLRequestHandler.instance().updateProperty(redoUri, "redo", "true", "PUT", true);
			}else{
				logger.debug("Mounts are not set, redo will not be set to true");
			}
		} catch (Exception e) {
			logger.error("",e);
		}	
		return null;
	}

	
	
}