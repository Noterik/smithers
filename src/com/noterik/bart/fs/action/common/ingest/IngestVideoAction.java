/*
 * Created on Jan 8, 2009
 */
package com.noterik.bart.fs.action.common.ingest;

import java.io.File;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.bart.fs.legacy.tools.FlandersHelper;
import com.noterik.springfield.tools.FileHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.FSXMLParser;
import com.noterik.springfield.tools.fs.URIParser;
import com.noterik.springfield.tools.fs.model.FTPConfig;
import com.noterik.springfield.tools.fs.model.RawVideo;
import com.noterik.springfield.tools.fs.model.Screens;
import com.noterik.springfield.tools.fs.model.config.video.VideoConfig;
import com.noterik.springfield.tools.ftp.FtpHelper;

/**
 * Script offering the possibilities for ingesting video files in either the cluster
 * machines or on an external ftp server. It will also update properties of the triggered
 * rawvideo.
 * 
 * The ingest function works like this:
 * 
 * - it is triggered by listening to all rawvideo properties
 * - it will react to the following properties:
 * 		- inputlocation (current location of the video to be ingested)
 * 		- collectionid (used for determining the streaming server to copy to -> for cluster only)
 * 		- ingested (true or false)
 * - it uses the /domain/{domain}/config/video to get the FTP settings needed
 * - it uses FTP to send the file according to the FTP settings
 * - in case of saving to the cluster it uses FTP to send the file to a backup server
 * - it copies the rawvideo properties from the /domain/{domain}/config/video config to the current raw
 * - it will add the mount property to the rawvideo
 * - it will call flanders to extract the meta-data and adds this info to the properties of the rawvideo
 * - it will delete the ingest folder where the uploaded file was
 * 
 * @author Jaap
 *
 */

public class IngestVideoAction extends ActionAdapter {

	private static Logger logger = Logger.getLogger(IngestVideoAction.class);

	@Override
	public String run() {
		logger.debug("IN THE VIDEO INGEST SCRIPT");
		String requestBody = event.getRequestData();
		logger.debug("XML: " + requestBody);
		String uri = event.getUri();
		logger.debug("URI" + uri);
		Document doc = null;
		String response = null;
		try {
			if (requestBody != null) {
				doc = DocumentHelper.parseText(requestBody);
				Node in = doc.selectSingleNode("//properties/ingested");
				Node iln = doc.selectSingleNode("//properties/inputlocation");
				Node cn = doc.selectSingleNode("//properties/collectionid");
				if (in != null && iln != null) {
					String i = in.getText();
					logger.debug("I" + i);
					if (i != null && i.equals("false")) {
						String il = iln.getText();
						String domain = URIParser.getDomainFromUri(uri);
						String user = URIParser.getUserFromUri(uri);
						String videoId = URIParser.getVideoIdFromUri(uri);
						String rawId = URIParser.getRawIdFromUri(uri, "video");
						String vcUri = "/domain/" + domain + "/config/video";
						logger.debug("LOCATION" + il);
						logger.debug("DOMAIN " + domain);
						logger.debug("USER " + user);
						logger.debug("VIDEO ID " + videoId);
						logger.debug("RAW ID " + rawId);						
						Document vcDoc = FSXMLRequestHandler.instance().getNodeProperties(vcUri, false);
						if (vcDoc != null) {							
							VideoConfig vc = FSXMLParser.getVideoConfigFromXml(vcDoc.asXML());							
							response = ingestVideo(vc, il, domain, user, videoId, rawId, cn == null ? (Math.random() * 100) + "" : cn.getText());
						}
					}
				}
			}
		} catch (DocumentException e) {
			logger.error("",e);
		}
		return response;
	}

	private String ingestVideo(VideoConfig vc, String location, String domain, String user, String videoId,
			String rawId, String collectionId) {
		logger.debug("INGESTING A VIDEO ");
		String response = null;
		// get the correct profile from the config (by using the rawindex)
		RawVideo rv = null;		
		rv = vc.getRawVideos().get(rawId);		
		String destinationUri = null;
		destinationUri = "/domain/" + domain + "/user/" + user + "/video/" + videoId + "/rawvideo/" + rawId;
		//FIXME this was for saving the screens properties in the video
		//String videoUri = "/domain/" + domain + "/user/" + user + "/video/" + videoId;
		logger.debug("DESTINATION URI " + destinationUri);
		if (rv == null || destinationUri == null) {
			// TODO do this differently
			return FSXMLBuilder.getErrorMessage("500", "Incorrect input parameters",
					"Please provide either a destination uri or a profile id", "http://teamelements.noterik.com/team");
		}
		// get the ftp config from the profile
		FTPConfig ftpc = rv.getFtpConfig();
		logger.debug(ftpc);
		if (ftpc.isEnabled()) {
			logger.debug("IS PAST THE ENABLED CHECK");
			// get the id of the cluster
			int sid = -1;
			if(ftpc.getServer() == null){
				sid = getFTPServerIdForCollection(destinationUri, collectionId, ftpc.getServers(), ftpc.getOffset(),
					"video");
			} 
			String ftpServer = null;
			String ftpUser = null;
			String ftpPassword = null;
			String mount = null;
			String backupFtpServer = null;
			String backupMount = null;
			boolean backup = false;
			boolean saveInCluster = sid != -1;
			if (saveInCluster) {
				// the ftp config indicates saving the file in the cluster
				ftpServer = ftpc.getPrefix() + sid + ftpc.getSuffix();
				ftpUser = ftpc.getPrefix() + sid;
				ftpPassword = ftpc.getPrefix() + sid;
				backup = true;
			} else {
				// the ftp config indicates saving the file on a foreign server
				ftpServer = ftpc.getServer();
				ftpUser = ftpc.getUser();
				ftpPassword = ftpc.getPassword();
			}
			logger.debug("FTP SERVER: " + ftpServer);
			logger.debug("INPUT FILE: " + location);
			logger.debug("DESTINATION: " + destinationUri);
			if (FtpHelper.sendFileWithFtp(location, destinationUri, ftpServer, ftpUser, ftpPassword, false)) {
				if (saveInCluster) {
					mount = ftpc.getPrefix() + sid;
				} else {
					mount = ftpc.getServer();
				}
			}
			// backup is only true if the file will be saved in the cluster
			if (backup) {
				// get the backup server id
				int bid = getBackupFTPServerId(sid, ftpc.getServers(), ftpc.getOffset());
				backupFtpServer = ftpc.getPrefix() + bid + ftpc.getSuffix();
				logger.debug("BACKUP FTP SERVER: " + backupFtpServer);
				if (FtpHelper.sendFileWithFtp(location, destinationUri, backupFtpServer, ftpc.getPrefix() + bid, ftpc
						.getPrefix()
						+ bid, true)) {
					backupMount = ftpc.getPrefix() + bid;
				}
			}
			if (saveInCluster && mount == null && backupMount == null) {
				// the file could not be saved in the cluster
				response = FSXMLBuilder.getErrorMessage("500", "Could not transfer the ingest file to the FS",
						"The file: " + location + " could not be sent to either: " + ftpServer + " or "
								+ backupFtpServer, "http://teamelements.noterik.com/team");
			} else if (!saveInCluster && mount == null) {
				// the file could not be saved on the foreign ftp server
				response = FSXMLBuilder.getErrorMessage("500", "Could not transfer the ingest file to" + ftpServer,
						"The file: " + location + " could not be sent to: " + ftpServer,
						"http://teamelements.noterik.com/team");
			} else {
				// set the mount properties in the raw video properties
				String url = destinationUri + "/properties/mount";
				String mounts = null;
				if (saveInCluster) {
					mounts = mount == null ? "" : mount;
					mounts += backupMount == null ? "" : "," + backupMount;
				} else {
					mounts = mount;
				}
				logger.debug("SETTING THE MOUNT PROPERTY:\n" + url + "\nmounts: " + mounts);
				rv.setMount(mounts);
				rv.setIngested(true);
				rv.setInputLocation(null);
				rv.setCollectionId(collectionId);				
				setRawVideoProperties(rv, destinationUri);
				if(saveInCluster){
					// get the updated raw video properties and use flanders to add technical meta-data
					logger.debug("Getting the properties for: " + destinationUri);
					Document doc = FSXMLRequestHandler.instance().getNodeProperties(destinationUri, false);
					if (doc != null) {
						String properties = doc.asXML();
						logger.debug("Properties are: " + properties);
						properties = FlandersHelper.processRaw(destinationUri, properties);
						logger.debug("Properties are now after flanders: " + properties);
						FSXMLRequestHandler.instance().handlePUT(destinationUri + "/properties", properties);
					}
				}
				// FIXME this was for saving the screens properties in the video
				//setVideoProperties(vc, videoUri);
				deleteIngestFolder(location);
				response = FSXMLBuilder.getStatusMessage("The file has been succesfully ingested", "The file "
						+ FileHelper.getFileNameFromPath(location) + " has been succesfully ingested", destinationUri);
			}
		}
		return response;
	}
	
	/**
	 * Uses mainly the collection id + uri as input to calculate which server it should reside on.
	 * 
	 * The idea is that all videos of the same collection uri should reside on the same server.
	 * This function's algorithm makes sure this is the case. 
	 * 
	 * (this function is only relevant when saving files in the cluster)
	 * 
	 * @param uri
	 * @param collection
	 * @param servers
	 * @param offset
	 * @param type
	 * @return
	 */

	private int getFTPServerIdForCollection(String uri, String collection, int servers, int offset, String type) {
		logger.debug("Get FTP serverid : " + uri + " c: " + collection + " s: " + servers + " o: " + offset + " t: "
				+ type);
		if (servers == -1 && offset == -1) {
			return -1;
		}
		if (uri.indexOf("/" + type) == -1) {
			return -1;
		}
		uri = uri.substring(0, uri.indexOf("/" + type));
		logger.debug("HASH FOR URI(2): " + uri + " AND COLLECTION: " + collection);
		uri += collection + "";
		int sid = uri.hashCode() % servers;
		sid = sid < 0 ? sid * -1 : sid;
		sid += offset;
		sid = sid == 0 ? 1 : sid;
		return sid;
	}
	
	/**
	 * Returns the id of the backup server. This function is only relevant when ingesting into the cluster.
	 * (the idea is that all videos ingested in the cluster are also saved on a backup server)
	 * @param sid
	 * @param servers
	 * @param offset
	 * @return
	 */

	private int getBackupFTPServerId(int sid, int servers, int offset) {
		int bid = sid;
		int max = servers + offset - 1;
		bid += 3;
		bid = bid > max ? (bid - servers) + offset : bid;
		return bid;
	}
	
	private boolean setVideoProperties(VideoConfig vc, String uri){
		logger.debug("Setting video properties (ingest) ");
		Screens s = vc.getScreens();
		String vidXml = "<fsxml>" + s.asXML(false) + "</fsxml>";
		logger.debug("XML\n" + vidXml);
		logger.debug("URI\n" + uri);
		String response = FSXMLRequestHandler.instance().handlePUT(uri + "/screens/1/properties", vidXml);
		logger.debug(response);
		return false;
	}
	
	/**
	 * Saves the properties of the RawVideo object into the rawvideo in the filessytem
	 * the RawVideo object is the rawvideo indicated in the config:
	 * 
	 * /domain/{domain}/config/video
	 * 
	 * @param rv
	 * @param uri
	 * @return
	 */

	private boolean setRawVideoProperties(RawVideo rv, String uri) {
		String rawXml = "<fsxml>" + rv.asXML(false) + "</fsxml>";
		logger.debug("XML\n" + rawXml);
		logger.debug("URI\n" + uri);
		String response = FSXMLRequestHandler.instance().handlePUT(uri + "/properties", rawXml);		
		logger.debug(response);
		return false;
	}
	
	private boolean deleteIngestFolder(String location){
		File f = new File(location);
		logger.debug(f.getParent());
		if(f.getParent() != null && f.getParent().endsWith("input")){
			f = f.getParentFile();
			if(f.getParentFile() != null){
				logger.debug("DELETING DIR: " + f.getParentFile().getAbsolutePath());
				FileHelper.deleteDir(f.getParentFile());
			}
		}
		return false;
	}

}