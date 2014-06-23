/*
 * Created on Jan 9, 2009
 */
package com.noterik.bart.fs.action.common.ingest;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.FileHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.FSXMLParser;
import com.noterik.springfield.tools.fs.URIParser;
import com.noterik.springfield.tools.fs.model.FTPConfig;
import com.noterik.springfield.tools.fs.model.RawAudio;
import com.noterik.springfield.tools.fs.model.config.audio.AudioConfig;
import com.noterik.springfield.tools.ftp.FtpHelper;

/**
 * Script offering the possibilities for ingesting audio files in either the cluster
 * machines or on an external ftp server. It will also update properties of the triggered
 * rawaudio.
 * 
 * The ingest function works like this:
 * 
 * - it is triggered by listening to all rawaudio properties
 * - it will react to the following properties:
 * 		- inputlocation (current location of the video to be ingested)
 * 		- ingested (true or false)
 * - it uses the /domain/{domain}/config/audio to get the FTP settings needed
 * - it uses FTP to send the file according to the FTP settings
 * - in case of saving to the cluster it uses FTP to send the file to a backup server
 * - it copies the rawaudio properties from the /domain/{domain}/config/audio config to the current raw
 * - it will add the mount property to the rawaudio
 * - TODO it will delete the ingest folder where the uploaded file was
 */

public class IngestAudioAction extends ActionAdapter{
	
	private static Logger logger = Logger.getLogger(IngestAudioAction.class);
	
	@Override
	public String run() {
		logger.debug("IN THE AUDIO INGEST SCRIPT");
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
				if (in != null && iln != null) {
					String i = in.getText();
					logger.debug("I" + i);
					if (i != null && i.equals("false")) {
						String il = iln.getText();
						String domain = URIParser.getDomainFromUri(uri);
						String user = URIParser.getUserFromUri(uri);
						String audioId = URIParser.getAudioIdFromUri(uri);
						String rawId = URIParser.getRawIdFromUri(uri, "audio");
						String vcUri = "/domain/" + domain + "/config/audio";
						logger.debug("IL" + il);
						logger.debug("DOMAIN " + domain);
						logger.debug("USER " + user);
						logger.debug("AUDIO ID " + audioId);
						logger.debug("RAW ID " + rawId);
						Document icDoc = FSXMLRequestHandler.instance().getNodeProperties(vcUri, false);
						if (icDoc != null) {
							AudioConfig ac = FSXMLParser.getAudioConfigFromXml(icDoc.asXML());
							response = ingestAudio(ac, il, domain, user, audioId, rawId);
						}
					}
				}
			}
		} catch (DocumentException e) {
			logger.error("",e);
		}
		return response;
	}
	
	private String ingestAudio(AudioConfig ac, String location, String domain, String user, String audioId, String rawId) {
		RawAudio ri = ac.getRawAudios().get(rawId);
		logger.debug("INGESTING AN AUDIO");
		String response = null;
		FTPConfig ftpc = ri.getFtpConfig();
		logger.debug(ftpc);
		String destinationUri = "/domain/" + domain + "/user/" + user + "/audio/" + audioId + "/rawaudio/" + rawId;
		if (ftpc.isEnabled()) {
			int sid = -1;
			if (ftpc.getServer() == null) {
				sid = getFTPServerIdForCollection(destinationUri, "1", ftpc.getServers(), ftpc.getOffset(), "audio");
			}
			String ftpServer = null;
			String ftpUser = null;
			String ftpPassword = null;
			String mount = null;
			String backupFtpServer = null;
			String backupMount = null;
			boolean backup = false;
			boolean saveInCluster = sid != -1;
			if (sid != -1) {
				ftpServer = ftpc.getPrefix() + sid + ftpc.getSuffix();
				ftpUser = ftpc.getPrefix() + sid;
				ftpPassword = ftpc.getPrefix() + sid;
				backup = true;
			} else {
				ftpServer = ftpc.getServer();
				ftpUser = ftpc.getUser();
				ftpPassword = ftpc.getPassword();
			}
			logger.debug("FTP SERVER: " + ftpServer);
			logger.debug("INPUT FILE: " + location);
			logger.debug("DESTINATION: " + destinationUri);
			String destDir = null;
			if (saveInCluster) {
				destDir = ftpc.getPrefix() + sid + destinationUri;
			} else {
				destDir = destinationUri;
			}
			logger.debug("FTP DATA: " + location + " destDir: " + destDir + " ftps " + ftpServer + " user "
					+ ftpUser + " pw " + ftpPassword);
			if (FtpHelper.sendFileWithFtp(location, destDir, ftpServer, ftpUser, ftpPassword, false)) {
				if(saveInCluster){
					mount = ftpc.getPrefix() + sid;
				} else {
					mount = ftpc.getServer();
				}
			}
			if (backup) {
				int bid = getBackupFTPServerId(sid, ftpc.getServers(), ftpc.getOffset());
				backupFtpServer = ftpc.getPrefix() + bid + ftpc.getSuffix();
				logger.debug("BACKUP FTP SERVER: " + backupFtpServer);
				destDir = ftpc.getPrefix() + bid + destinationUri;
				if (FtpHelper.sendFileWithFtp(location, destDir, backupFtpServer, ftpc.getPrefix() + bid, ftpc
						.getPrefix()
						+ bid, true)) {
					backupMount = ftpc.getPrefix() + bid;
				}
			}
			if (mount == null && backupMount == null) {
				response = FSXMLBuilder.getErrorMessage("500", "Could not transfer the ingest file to the FS",
						"The file: " + location + " could not be sent to either: " + ftpServer + " or "
								+ backupFtpServer, "http://teamelements.noterik.com/team");
			} else {
				String mounts = null;
				if (saveInCluster) {
					mounts = mount == null ? "" : mount;
					mounts += backupMount == null ? "" : "," + backupMount;
				} else {
					mounts = mount;
				}
				ri.setMount(mounts);
				ri.setIngested(true);
				ri.setInputLocation(null);
				setRawAudioProperties(ri, destinationUri);
				response = FSXMLBuilder.getStatusMessage("The file has been succesfully ingested", "The file "
						+ FileHelper.getFileNameFromPath(location) + " has been succesfully ingested", destinationUri);
			}
		}
		return response;
	}
	
	private int getFTPServerIdForCollection(String uri, String collection, int servers, int offset, String type) {
		logger.debug("Get FTP serverid : " + uri + " c: " + collection + " s: " + servers + " o: " + offset
				+ " t: " + type);
		if (servers == -1 && offset == -1) {
			return -1;
		}
		if (uri.indexOf("/" + type) == -1) {
			return -1;
		}
		uri = uri.substring(0, uri.indexOf("/" + type));
		logger.debug("HASH FOR URI: " + uri + " AND COLLECTION: " + collection);
		uri += collection + "";
		int sid = uri.hashCode() % servers;
		sid = sid < 0 ? sid * -1 : sid;
		sid += offset;
		sid = sid == 0 ? 1 : sid;
		return sid;
	}

	private int getBackupFTPServerId(int sid, int servers, int offset) {
		int bid = sid;
		int max = servers + offset - 1;
		bid += 3;
		bid = bid > max ? (bid - servers) + offset : bid;
		return bid;
	}

	private boolean setRawAudioProperties(RawAudio ra, String uri) {
		String rawXml = "<fsxml>" + ra.asXML(false) + "</fsxml>";
		logger.debug("XML\n" + rawXml);
		logger.debug("URI\n" + uri);
		String response = FSXMLRequestHandler.instance().handlePUT(uri + "/properties", rawXml);
		logger.debug(response);
		return false;
	}

}