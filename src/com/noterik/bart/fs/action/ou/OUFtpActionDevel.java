package com.noterik.bart.fs.action.ou;

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.ftp.FtpHelper;
import com.noterik.springfield.tools.fs.*;

/**
 * this action will transfer a file to an external server for the OU domain.
 * if the momar job failed, it will use the rawvideo/1, if the momar job
 * was successfull, it uses rawvideo/2
 *
 * @author Levi Pires <l.pires@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.action.ou
 * @project smithers2
 * @access private
 * @version $Id: OUFtpActionDevel.java,v 1.20 2009-06-12 15:11:29 derk Exp $
 *
 */
public class OUFtpActionDevel extends ActionAdapter{

	
	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(OUFtpActionDevel.class);
	
	/**
	 * video config uri
	 */
	//private static final String configUri = "/domain/ou.devel/config/video/rawvideo";
	private static final String configUri = "/domain/ou.devel/config/ftpscript";
	
		
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
		
		logger.debug("**************** Starting OU.devel FTP Action ***********");
		
		// parse request
		String requestBody = event.getRequestData();
		
		String rawUri = event.getUri();
		logger.debug("rawUri(from event) = " + rawUri);
	
		// get rawvideo's properties from database (not from event body)
		Document doc = FSXMLRequestHandler.instance().getNodeProperties(rawUri, false);
		
		logger.debug("\n\nrequest xml = " + doc.asXML());
		
		String user = URIParser.getUserFromUri(rawUri);
		String videoId = URIParser.getVideoIdFromUri(rawUri);
		
		try {
			
			Node stNode = doc.selectSingleNode(".//properties/status");
			Node trfNode = doc.selectSingleNode(".//properties/transferred");
			Node momarNode = doc.selectSingleNode(".//properties/momar");
					
			logger.debug("starting the if tests");
			// the transferred tag is still true
			if(trfNode != null && trfNode.getText().toLowerCase().equals("true")){
				logger.debug("transfer tag is true, no need to send by ftp");
				return null;
			}			
						
			// its rawvideo1
			if(rawUri.indexOf("/rawvideo/1") != -1){
				
				Node mtNode = doc.selectSingleNode(".//properties/mount");
				
				// if mount tag is still not set, the video has not been ingested yet
				if(mtNode == null){
					logger.debug("Mount not set, video has not been ingested yet.");
					return null;
				}
				logger.debug("its raw1 !! about to send it !!");
				
				// set transferred to true
				String transUri = rawUri + "/properties/transferred";
				FSXMLRequestHandler.instance().updateProperty(transUri, "transferred", "true", "PUT", false);
				Thread.sleep(1000);
				FSXMLRequestHandler.instance().updateProperty(transUri, "transferred", "true", "PUT", false);
				// TODO: test property setting
				
				// raw1 - if the ftp is successfull, set the property
				if(handleFtp(doc, "1", rawUri, rawUri)){
					logger.debug("Video (1) was sent by ftp (its raw1)");
					
					// set the link in the properties
					String extUri = rawUri + "/properties/externaluri";
					// String extValue = "http://switch3.castup.net/cunet/gm.asp?ai=16&ar=" + timestamp + ".wmv&ak=null";
					String extValue = "mms://cw2.noterik.com/stream2/domain/ou.devel/user/" + user + "/video/" + videoId + "/rawvideo/1/" +  getOriginalFilename();
					FSXMLRequestHandler.instance().updateProperty(extUri, "externaluri", extValue, "PUT", false);						

				}else{
					// set transferred to false
					FSXMLRequestHandler.instance().updateProperty(transUri, "transferred", "false", "PUT", false);
					// TODO: test property setting
				}
			}
			// its rawvideo2
			else if(rawUri.indexOf("/rawvideo/2") != -1){
								
				/**
				 *  old way (using momar)
				 */
				if(momarNode == null || momarNode.getText().toLowerCase().equals("true")){
				// there is no status message
					if(stNode == null){
						logger.debug("there is no status tag, no need to send by ftp");
						return null;
					}
					
					// job failed, get rawvideo 1
					if(stNode.getText().toLowerCase().equals("failed")){
						
						String origUri = rawUri.substring(0, rawUri.lastIndexOf("/")+1) + "1";
											
						// set transferred to true
						String transUri = rawUri + "/properties/transferred";
						FSXMLRequestHandler.instance().updateProperty(transUri, "transferred", "true", "PUT", false);
						Thread.sleep(1000);
						FSXMLRequestHandler.instance().updateProperty(transUri, "transferred", "true", "PUT", false);
						// TODO: test property setting
						
						// if the ftp is successfull, set the property
						if(handleFtp(doc, "1", origUri, rawUri)){
							logger.debug("Video (1) was sent by ftp (raw2)");
							
							// set the link in the properties
							String extUri = rawUri + "/properties/externaluri";
							// String extValue = "http://switch3.castup.net/cunet/gm.asp?ai=16&ar=" + timestamp + ".wmv&ak=null";
							String extValue = "mms://cw2.noterik.com/stream2/domain/ou.devel/user/" + user + "/video/" + videoId + "/rawvideo/2/" +  getOriginalFilename();
							FSXMLRequestHandler.instance().updateProperty(extUri, "externaluri", extValue, "PUT", false);						
						}
						else{
							// set transferred to false
							FSXMLRequestHandler.instance().updateProperty(transUri, "transferred", "false", "PUT", false);
							// TODO: test property setting
						}
					}
					// job was successfull, get rawvideo 2
					else if(stNode.getText().toLowerCase().equals("done")){
						logger.debug("Job done, getting rawvideo 2");
						
						// check if its rawvideo 2
						if(isRaw2(rawUri)){
							logger.debug("was rawvideo 2");
													
							// set transferred to true
							String transUri = rawUri + "/properties/transferred";
							FSXMLRequestHandler.instance().updateProperty(transUri, "transferred", "true", "PUT", false);
							Thread.sleep(1000);
							FSXMLRequestHandler.instance().updateProperty(transUri, "transferred", "true", "PUT", false);
							// TODO: test property setting
							
							// do the ftp and set the properties
							if(handleFtp(doc, "2", rawUri, rawUri)){
								logger.debug("Video (2) was sent by ftp");
								
								// set the link in the properties
								String extUri = rawUri + "/properties/externaluri";
								//String extValue = "http://switch3.castup.net/cunet/gm.asp?ai=16&ar=" + timestamp + ".wmv&ak=null";
								
								String extValue = "mms://cw2.noterik.com/stream2/domain/ou.devel/user/" + user + "/video/" + videoId + "/rawvideo/2/" +  getOriginalFilename();
								FSXMLRequestHandler.instance().updateProperty(extUri, "externaluri", extValue, "PUT", false);													
							}
							else{
								// set transferred to false
								FSXMLRequestHandler.instance().updateProperty(transUri, "transferred", "false", "PUT", false);
								// TODO: test property setting
							}
						}else{
							logger.debug("was not rawvideo2... exiting !");
						}
					}
				}
			
				/**
				 * new way: not using momar
				 */
				else{
					if(momarNode != null && momarNode.getText().toLowerCase().equals("false")){
						// check if its rawvideo 2
						if(isRaw2(rawUri)){
							logger.debug("was rawvideo 2");
													
							// set transferred to true
							String transUri = rawUri + "/properties/transferred";
							FSXMLRequestHandler.instance().updateProperty(transUri, "transferred", "true", "PUT", false);
							Thread.sleep(1000);
							FSXMLRequestHandler.instance().updateProperty(transUri, "transferred", "true", "PUT", false);
							// TODO: test property setting
							
							// do the ftp and set the properties
							if(handleFtp(doc, "2", rawUri, rawUri)){
								logger.debug("Video (2) was sent by ftp");
								
								// set the link in the properties
								String extUri = rawUri + "/properties/externaluri";
								//String extValue = "http://switch3.castup.net/cunet/gm.asp?ai=16&ar=" + timestamp + ".wmv&ak=null";
								
								String extValue = "mms://cw2.noterik.com/stream2/domain/ou.devel/user/" + user + "/video/" + videoId + "/rawvideo/2/" + getOriginalFilename();
								FSXMLRequestHandler.instance().updateProperty(extUri, "externaluri", extValue, "PUT", false);													
							}
							else{
								// set transferred to false
								FSXMLRequestHandler.instance().updateProperty(transUri, "transferred", "false", "PUT", false);
								// TODO: test property setting
							}
						}else{
							logger.debug("was not rawvideo2... exiting !");
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error(e);
		}		
		return null;
	}
	
	
	/**
	 * checks if the rawUri refers to the rawvideo 2
	 * 
	 * @param String
	 * @return
	 */
	private boolean isRaw2(String rawUri){
							
		// get raw index
		if(rawUri != null && !rawUri.equals("") && rawUri.indexOf("/rawvideo/") != -1){
			String rawIndex = rawUri.substring(rawUri.lastIndexOf("/rawvideo/") + 10);
			
			logger.debug("rawindex = " + rawIndex);
			
			// if its 2 return true
			if(rawIndex.equals("2")){
				return true;
			}
		}
				
		return false;
	}
	
	
	
	/**
	 * gets the necessary parameters from the document and calls the ftp function
	 * 
	 * @param doc
	 * @param rawIndex
	 * @return
	 */
	private boolean handleFtp(Document doc, String rawIndex, String fileUri, String rawUri){
			
		Element mountEl = (Element)doc.selectSingleNode("//mount");
		//Element extEl = (Element)doc.selectSingleNode("//extension");	

		// get mounts and extension
		if(mountEl != null){
			
			logger.debug("mounts and extension are set");
			
			String mounts = mountEl.getText();
			//String extension = extEl.getText();
			String extension = "wmv";
			
			// get stream name
			String stream = getStreamFromMounts(mounts);
			
			// get config xml from database
			String uriToGET = configUri + "/ftp/" + rawIndex;				
			
			logger.debug("uri for config: " + uriToGET);
			
			Document confdoc = FSXMLRequestHandler.instance().getNodeProperties(uriToGET, false);
								
			logger.debug("confDoc = " + confdoc.asXML());
			
			Element serverEl = (Element)confdoc.selectSingleNode(".//server");
			Element userEl = (Element)confdoc.selectSingleNode(".//user");
			Element passEl = (Element)confdoc.selectSingleNode(".//pass");
			
			String server = serverEl.getText();
			String user = userEl.getText();
			String pass = passEl.getText();
			
			logger.debug("rawUri = " + rawUri + 
						", mount = " + stream + 
						", extension = " + extension + 
						", server = " + server + 
						", user = " + user + 
						", pass = " + pass);
			
			return ftpVideo(fileUri, rawUri, stream, extension, server, user, pass);							
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
	private boolean ftpVideo(String fileUri, String rawUri, String stream, String extension, String server, String user, String pass){
		
		/** get the video to a temp folder */
		
		String ftpServer = stream + ".noterik.com";
		String rFilename = "raw." + extension;
		//String lFilename = getTimestamp() + "." + extension;
		
		// changed to original filename upon request of OU. NOT SAVE!
		String lFilename = getOriginalFilename();
		
		
		logger.debug("ftpServer = " + ftpServer + 
				", user = " + stream + 
				", pass = " + stream + 
				", remoteF = " + fileUri + 
				", localF = " + TMP_FOLDER +
				", remoteFile = " + rFilename +
				", localFile = " + lFilename);
		
		// get the video
		if(FtpHelper.commonsGetFile(ftpServer, stream, stream, fileUri, TMP_FOLDER, rFilename, lFilename)){
						
			logger.debug("video in the temp folder");
			/** send the video to the external ftp server */
			
			if(FtpHelper.commonsSendFile(server, user, pass, rawUri, TMP_FOLDER, lFilename)){
				
				//erase the temp file
				File file = new File(lFilename);
				file.delete();
				
				return true;
			}
		}		
		return false;		
	}
	
	/**
	 * Gets original filename from the rawvideo
	 * @return
	 */
	private String getOriginalFilename() {
		String ofn = null, original1 = null, original2=null;
		
		try {
			String rawVidUri = event.getUri();
			String vidUri = rawVidUri.substring(0,rawVidUri.indexOf("/rawvideo/"));
			original1 = FSXMLRequestHandler.instance().getPropertyValue(vidUri+"/rawvideo/1/properties/original-filename");
			original2 = FSXMLRequestHandler.instance().getPropertyValue(vidUri+"/rawvideo/2/properties/original-filename");
			
			logger.debug("original1: "+original1+", original2: "+original2);
		} catch(Exception e) {
			logger.debug("Caught Exception: "+e.getMessage());
		}
		
		if(original1==null) {
			ofn = original2;
		}
		else if(original2==null) {
			ofn = original1;
		} 

		// test for null
		if(ofn==null) {
			ofn = getTimestamp() + ".wmv";
		}
		
		return ofn;
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
	
		
	
}
