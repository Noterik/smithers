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
 * @version $Id: OUdevelImageFtpAction.java,v 1.5 2009-04-01 12:52:55 levi Exp $
 *
 */
public class OUdevelImageFtpAction extends ActionAdapter{

	
	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(OUdevelImageFtpAction.class);
	
	/**
	 * local folder on images server
	 */
	private static final String localFolder = "/mount/images1";
	
	
	
	/**
	 * will have the video name and will be stored in the properties
	 */
	private String timestamp = "";
	
		
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	
		
	@Override
	public String run() {		
		
		logger.debug("\n\n**************** Starting OU.devel image FTP Action ***********");
		
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
					// String extValue = "mms://cw2.noterik.com/stream2" + rawUri + "/" + timestamp + "." + extension;
					String extValue = "http://images1.noterik.com" + rawUri + "/" + timestamp + "." + extension;
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
	
	
		// get the server from the mounts
		Node mtNode = doc.selectSingleNode(".//properties/mount");
		String mounts = mtNode.getText();
		String mount = getStreamFromMounts(mounts);
				

		String server = mount + ".noterik.com";
		String user = mount;
		String pass = mount;
		String folder = localFolder + rawUri;
		
		
		logger.debug("\nrawUri = " + rawUri + "\n server = " + server + "\n user " + user + "\n pass + " + pass + "\n folder " + folder);
		
		if (checkPath(folder)){
			logger.debug("path was verified. ");
			return ftpImage(rawUri, server, extension, user, pass, folder);	
		}else{
			logger.error("there was an error creating the destination folder");
			return false;
		}
	}
	
	/**
	 * gets the image by ftp from the streaming server
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
		
		logger.debug("about to get the image to the images server.");
		// get the video
		if(FtpHelper.commonsGetFile(server, user, pass, rawUri, folder, rFilename, lFilename)){
					
			return true;
		}		
		return false;		
	}
	
	
	/**
	 * gets one of the stream names from the mounts
	 * 
	 * @param mounts
	 * @return
	 */
	public String getStreamFromMounts(String mounts){
		
		// if there is only one, return it
		if(mounts.indexOf(",") == -1){
			return mounts;
		}
		// if there are 2, return the first one
		else{
			String stream = mounts.substring(0,mounts.indexOf(","));
			return stream;
		}
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
