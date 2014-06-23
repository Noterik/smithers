package com.noterik.bart.fs.action.ou;

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.ftp.FtpHelper;


/**
 * copies the image files to the ftp servers
 *
 * @author Levi Pires <l.pires@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.action.ou
 * @project smithers2
 * @access private
 * @version $Id: OUimageFtpAction.java,v 1.10 2009-04-01 12:52:55 levi Exp $
 *
 */
public class OUimageFtpAction extends ActionAdapter{

	
	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(OUimageFtpAction.class);
	
	/**
	 * video config uri
	 */
	private static final String configUri = "/domain/ou/config/imageftpscript";
	
	
	/**
	 * will have the video name and will be stored in the properties
	 */
	private String timestamp = "";
	
		
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	
		
	@Override
	public String run() {		
		
		logger.debug("\n\n**************** Starting OU image FTP Action ***********");
		
		String rawUri = event.getUri();
		logger.debug("rawUri(from event) = " + rawUri);
		
		String extension = "";
	
		// get rawimage's properties from database (not from event body)
		Document doc = FSXMLRequestHandler.instance().getNodeProperties(rawUri, false);
	
		try {
			Node node = doc.selectSingleNode(".//properties/transferred");
						
			if(node != null && node.getText().toLowerCase().equals("true") ) {
												
				logger.debug("Transferred was true, will not tranfer the image file");
				return null;
			} 

			Node mtNode = doc.selectSingleNode(".//properties/mount");
			
			// if mount tag is still not set, the image has not been ingested yet
			if(mtNode == null){
				logger.debug("Mount not set, image has not been ingested yet.");
				return null;
			}
			
			Node extNode = doc.selectSingleNode(".//properties/extension");
			
			// if mount tag is still not set, the image has not been ingested yet
			if(extNode == null){
				logger.debug("Extension not set, image has not been ingested yet.");
				return null;
			}else{
				extension = extNode.getText();
			}
			
			if(rawUri.toLowerCase().indexOf("/rawimage/1") != -1){
				
				// do the ftp and set the property
				if(handleFtp(doc, rawUri, "1", extension)){
					logger.debug("Image file 1 was received by ftp");
					// set transferred to true
					String rawProp = rawUri + "/properties/transferred";					
					FSXMLRequestHandler.instance().updateProperty(rawProp, "transferred", "true", "PUT", false);
					
					// set the link in the properties
					String extUri = rawUri + "/properties/externaluri";
					// String extValue = "http://switch3.castup.net/cunet/gm.asp?ai=16&ar=" + timestamp + ".wmv&ak=null";
					String extValue = "http://vod.openu.ac.il/images" + rawUri + "/" + timestamp + "." + extension;
					FSXMLRequestHandler.instance().updateProperty(extUri, "externaluri", extValue, "PUT", false);	
				}
			}			
			
		} catch (Exception e) {
			logger.error("", e);
		}
		
		return null;
	}	
	
	
	/**
	 * gets the necessary parameters from the document and calls the ftp function
	 * 
	 * @param doc
	 * @param rawIndex
	 * @return
	 */
	private boolean handleFtp(Document doc, String rawUri, String rawIndex, String extension){
	
				
		// get config xml from database
		String uriToGET = configUri + "/ftp/1";				
		
		logger.debug("uri for config: " + uriToGET);
		
		Document confdoc = FSXMLRequestHandler.instance().getNodeProperties(uriToGET, false);
							
		logger.debug("confDoc = " + confdoc.asXML());
		
		Element serverEl = (Element)confdoc.selectSingleNode(".//server");
		Element userEl = (Element)confdoc.selectSingleNode(".//user");
		Element passEl = (Element)confdoc.selectSingleNode(".//pass");
		Element localFolderEl = (Element)confdoc.selectSingleNode(".//localfolder");
		

		// get mounts and extension
		if(serverEl != null && userEl != null && passEl != null && localFolderEl != null){
			
			String server = serverEl.getText();
			String user = userEl.getText();
			String pass = passEl.getText();
			String localFolder = localFolderEl.getText();
			String folder = localFolder + rawUri;
			
			
			logger.debug("rawUri = " + rawUri + ", server = " + server + ", user + " + user + ", pass + " + pass + ", folder + " + folder);
			
			if (checkPath(folder)){
				return ftpImage(rawUri, server, extension, user, pass, folder);	
			}else{
				logger.error("there was an error creating the destination folder");
				return false;
			}
		}			
		
		return false;
	}
	
	/**
	 * first gets the file to a temp folder and then sends it by ftp to the external ftp server
	 * 
	 * @param uri
	 * @param stream
	 * @param extension
	 * @param server
	 * @param user
	 * @param pass
	 * @return
	 */
	private boolean ftpImage(String rawUri, String server, String extension, String user, String pass, String folder){
		
		/** get the image to a temp folder */
		String rFilename = "raw." + extension;
		String lFilename = getTimestamp() + "." + extension;
		
		// get the video
		if(FtpHelper.commonsGetFile(server, user, pass, rawUri, folder, rFilename, lFilename)){
					
			return true;
		}		
		return false;		
	}
	
	
	private boolean checkPath(String path){
		File file = new File(path);
		if (!file.exists()){
			return file.mkdirs();
		}
		return true;
	}
	
	
	/**
	 * creates a timestamp string
	 * 
	 * @return
	 */
	private String getTimestamp(){
		
		Calendar cal = new GregorianCalendar();		
		String time = cal.getTimeInMillis() + "";
		
		setTimestamp(time);
		
		return time;
	}
		
	
	
}
