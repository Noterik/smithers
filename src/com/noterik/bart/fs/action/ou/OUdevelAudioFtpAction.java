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
 * copies the audio files to the ftp servers
 *
 * @author Levi Pires <l.pires@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.action.ou
 * @project smithers2
 * @access private
 * @version $Id: OUdevelAudioFtpAction.java,v 1.8 2010-02-16 13:54:15 pieter Exp $
 *
 */
public class OUdevelAudioFtpAction extends ActionAdapter{

	
	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(OUdevelAudioFtpAction.class);
	
	/**
	 * video config uri
	 */
	private static final String configUri = "/domain/ou.devel/config/audioftpscript";
	
	
	/**
	 * temp folder to store video
	 */
	private static final String TMP_FOLDER = "/tmp/ou";
	
	/**
	 * will have the video name and will be stored in the properties
	 */
	private String timestamp = "";
	
		
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	
		
	@Override
	public String run() {		
		
		logger.debug("**************** Starting OU.devel Audio FTP Action ***********");
		
		String rawUri = event.getUri();
		logger.debug("rawUri(from event) = " + rawUri);
		
		String rawIndex = getRawIndex(rawUri);
		logger.debug("RawIndex = " + rawIndex);
	
		// get rawvideo's properties from database (not from event body)
		Document doc = FSXMLRequestHandler.instance().getNodeProperties(rawUri, false);
	
		try {
			Node node = doc.selectSingleNode(".//properties/transferred");
						
			if(node != null && node.getText().toLowerCase().equals("true") ) {
												
				logger.debug("Transferred was true, will not tranfer the audio file");
				return null;
			} 

			Node mtNode = doc.selectSingleNode(".//properties/mount");
			
			// if mount tag is still not set, the video has not been ingested yet
			if(mtNode == null){
				logger.debug("Mount not set, audio has not been ingested yet.");
				return null;
			}
			
			
			// its rawaudio 2 - transfer and set external uri
			if(rawUri.toLowerCase().indexOf("/rawaudio/2") != -1){
				//removed origUri, why was rawaudio/2 rawaudio/1??
				//String origUri = rawUri.substring(0, rawUri.lastIndexOf("/")+1) + "1";
				
				// do the ftp and set the property
				//replaced origUri with rawUri, just like with rawaudio/1
				if(handleFtp(doc, rawUri, rawUri, rawIndex)){
					logger.debug("Audio file 2 was sent by ftp");
					
					// set transferred to true
					String rawProp = rawUri + "/properties/transferred";					
					FSXMLRequestHandler.instance().updateProperty(rawProp, "transferred", "true", "PUT", false);					
					
					// set the link in the properties
					String extUri = rawUri + "/properties/externaluri";
					// String extValue = "http://switch3.castup.net/cunet/gm.asp?ai=16&ar=" + timestamp + ".wmv&ak=null";
					String extValue = "mms://cw2.noterik.com/stream2" + rawUri + "/" + timestamp + ".wma";
					FSXMLRequestHandler.instance().updateProperty(extUri, "externaluri", extValue, "PUT", false);		
				}
			}
			// its rawaudio 1 - just set the external uri
			else if(rawUri.toLowerCase().indexOf("/rawaudio/1") != -1){
				
				// do the ftp and set the property
				if(handleFtp(doc, rawUri, rawUri, rawIndex)){
					logger.debug("Audio file 1 was sent by ftp");
					// set transferred to true
					String rawProp = rawUri + "/properties/transferred";					
					FSXMLRequestHandler.instance().updateProperty(rawProp, "transferred", "true", "PUT", false);
					
					// set the link in the properties
					String extUri = rawUri + "/properties/externaluri";
					// String extValue = "http://switch3.castup.net/cunet/gm.asp?ai=16&ar=" + timestamp + ".wmv&ak=null";
					String extValue = "mms://cw2.noterik.com/stream2" + rawUri + "/" + timestamp + ".wma";
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
	private boolean handleFtp(Document doc, String rawUri, String origUri, String rawIndex){
	
				
		// parse the doc		
		Element mountEl = (Element)doc.selectSingleNode("//mount");	

		// get mounts and extension
		if(mountEl != null ){
			String mounts = mountEl.getText();
			
			// get stream name
			String stream = getStreamFromMounts(mounts);
			
			// get config xml from database
			String uriToGET = configUri + "/ftp/" + rawIndex ;				
			
			logger.debug("uri for config: " + uriToGET);
			
			Document confdoc = FSXMLRequestHandler.instance().getNodeProperties(uriToGET, false);
								
			logger.debug("confDoc = " + confdoc.asXML());
			
			Element serverEl = (Element)confdoc.selectSingleNode(".//server");
			Element userEl = (Element)confdoc.selectSingleNode(".//user");
			Element passEl = (Element)confdoc.selectSingleNode(".//pass");
			
			String server = serverEl.getText();
			String user = userEl.getText();
			String pass = passEl.getText();
			
			logger.debug("rawUri = " + rawUri + ", origUri = " + origUri + ", mount: " + stream + ", server = " + server + ", user + " + user + ", pass + " + pass);
			
			return ftpAudio(rawUri, origUri, stream, "wma", server, user, pass);							
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
	private boolean ftpAudio(String uri, String origUri, String stream, String extension, String server, String user, String pass){
		
		/** get the audio to a temp folder */
		
		String ftpServer = stream + ".noterik.com";
		String rFilename = "raw." + extension;
		String lFilename = getTimestamp() + "." + extension;
		
		// get the video
		if(FtpHelper.commonsGetFile(ftpServer, stream, stream, origUri, TMP_FOLDER, rFilename, lFilename)){
						
			logger.debug("audio in the temp folder");
			/** send the audio to the external ftp server */
			
			if(FtpHelper.commonsSendFile(server, user, pass, uri, TMP_FOLDER, lFilename)){
				
				//erase the temp file
				File file = new File(lFilename);
				file.delete();
				
				return true;
			}
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
	
		
	
	/**
	 * gets the index of the rawaudio from the uri
	 * 
	 * @param rawUri
	 * @return
	 */
	private String getRawIndex(String rawUri){		
		String index = "";
		
	 	try{
	 		index = rawUri.substring(rawUri.lastIndexOf("/") + 1, rawUri.length());
	 	}catch(Exception e){
	 		logger.error("", e);
	 	}
 	
	 	return index;
	}
	
}
