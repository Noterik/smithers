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
import com.noterik.springfield.tools.fs.URIParser;
import com.noterik.springfield.tools.ftp.FtpHelper;


/**
 * copies the audio file (rawaudio 2) to the ftp server
 *
 * @author Levi Pires <l.pires@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.action.ou
 * @project smithers2
 * @access private
 * @version $Id: OUAudioFtpAction.java,v 1.21 2009-04-23 10:30:25 levi Exp $
 *
 */
public class OUAudioFtpAction extends ActionAdapter{

	
	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(OUAudioFtpAction.class);
	
	/**
	 * video config uri
	 */
	private static final String configUri = "/domain/ou/config/audioftpscript";
	
	
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
		
		logger.debug("**************** Starting OU Audio FTP Action ***********");
		
		String rawUri = event.getUri();
		logger.debug("rawUri(from event) = " + rawUri);
		
		String rawIndex = getRawIndex(rawUri);
		logger.debug("RawIndex = " + rawIndex);
	
		// get rawvideo's properties from database (not from event body)
		Document doc = FSXMLRequestHandler.instance().getNodeProperties(rawUri, false);
	
		try {
			Node node = doc.selectSingleNode(".//properties/transferred");
			Node momarNode = doc.selectSingleNode(".//properties/momar");
						
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
				String origUri = rawUri.substring(0, rawUri.lastIndexOf("/")+1) + "1";
				logger.debug("its rawaudio 2, about to start ftp method");
				
				logger.debug("doc: " + doc.asXML() + 
						"\n rawUri: " + rawUri + 
						"\n origUri: " + origUri + 
						"\n rawIndex: " + rawIndex);
				
				// momar is true or non existent (send raw 1)
				if(momarNode == null || momarNode.getText().equals("true")){
				
				
					// do the ftp and set the property
					if(handleFtp(doc, rawUri, origUri, rawIndex)){
						logger.debug("Audio file 1 was sent by ftp (as raw2)");
						
						// set transferred to true
						String rawProp = rawUri + "/properties/transferred";					
						FSXMLRequestHandler.instance().updateProperty(rawProp, "transferred", "true", "PUT", false);					
						
						// set the link in the properties
						String extUri = rawUri + "/properties/externaluri";
						// String extValue = "http://switch3.castup.net/cunet/gm.asp?ai=16&ar=" + timestamp + ".wma&ak=null";
						
						String extValue = "http://telem.openu.ac.il/cgi-bin/telem/tools/open_video/fetch_media.pl?ar=" + timestamp + "&cusi=1";
						
						// String extValue = "mms://cw2.noterik.com/stream2" + rawUri + "/" + timestamp + ".wma";
						FSXMLRequestHandler.instance().updateProperty(extUri, "externaluri", extValue, "PUT", false);		
					}
				}
				// momar is false (send raw 2)
				else{
					// do the ftp and set the property
					if(handleFtp(doc, rawUri, rawUri, rawIndex)){
						logger.debug("Audio file 2 was sent by ftp");
						
						// set transferred to true
						String rawProp = rawUri + "/properties/transferred";					
						FSXMLRequestHandler.instance().updateProperty(rawProp, "transferred", "true", "PUT", false);					
						
						// set the link in the properties
						String extUri = rawUri + "/properties/externaluri";
						// String extValue = "http://switch3.castup.net/cunet/gm.asp?ai=16&ar=" + timestamp + ".wma&ak=null";
						
						String extValue = "http://telem.openu.ac.il/cgi-bin/telem/tools/open_video/fetch_media.pl?ar=" + timestamp + "&cusi=1";
						
						// String extValue = "mms://cw2.noterik.com/stream2" + rawUri + "/" + timestamp + ".wma";
						FSXMLRequestHandler.instance().updateProperty(extUri, "externaluri", extValue, "PUT", false);		
					}
				}
			}
			// its rawaudio 1 - just set the external uri
			else if( rawUri.toLowerCase().indexOf("/rawaudio/1") != -1 ){
				
				Node external = doc.selectSingleNode(".//properties/externaluri");
				
				// if node exists, there's no need to set it again
				if( external == null || !external.getText().equals("") ){
					
					// set the link in the properties
					String extUri = rawUri + "/properties/externaluri";
					// String extValue = "http://switch3.castup.net/cunet/gm.asp?ai=16&ar=" + timestamp + ".wma&ak=null";
					String extValue = "mms://mms1.openu.local/stream1" + rawUri + "/raw.wma";
					FSXMLRequestHandler.instance().updateProperty(extUri, "externaluri", extValue, "PUT", false);	
				}else{
					logger.debug("external uri is already set, no need to set it again");
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

		logger.debug("about to check for mount.");
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
			
			logger.debug("rawUri = " + rawUri + "\n origUri = " + origUri + "\n mount: " + stream + "\n server = " + server + "\n user + " + user + "\n pass + " + pass);
			
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
		
		String ftpServer = stream + ".vod.openu.ac.il";
		String rFilename = "raw." + extension;
		String lFilename = getTimestamp() + "." + extension;
		
		logger.debug("ftpServer = " + ftpServer + 
				"\n user = " + stream + 
				"\n pass = " + stream + 
				"\n remoteF = " + origUri + 
				"\n localF = " + TMP_FOLDER +
				"\n remoteFile = " + rFilename +
				"\n localFile = " + lFilename);
		
		// get the audio file
		if(FtpHelper.commonsGetFile(ftpServer, stream, stream, origUri, TMP_FOLDER, rFilename, lFilename)){
						
			logger.debug("audio in the temp folder");
			/** send the audio to the external ftp server */
						
			if(FtpHelper.commonsSendFile(server, user, pass, "", TMP_FOLDER, lFilename, true)){
				
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
