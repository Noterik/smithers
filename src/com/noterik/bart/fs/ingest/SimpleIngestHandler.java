/* 
* SimpleIngestHandler.java
* 
* Copyright (c) 2012 Noterik B.V.
* 
* This file is part of smithers, related to the Noterik Springfield project.
*
* Smithers is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Smithers is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Smithers.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.noterik.bart.fs.ingest;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.xml.sax.DocumentHandler;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.bart.fs.id.IdHandler;
import com.noterik.bart.fs.legacy.tools.FlandersHelper;

import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;
import com.noterik.springfield.tools.fs.model.config.FileIngestConfig;
import com.noterik.springfield.tools.fs.model.config.ingest.EncodingProfile;
import com.noterik.springfield.tools.fs.model.FTPConfig;
import com.noterik.springfield.tools.fs.model.config.ingest.ImageProfile;
import com.noterik.springfield.tools.fs.model.config.ingest.IngestConfig;
import com.noterik.springfield.tools.fs.model.config.ingest.AudioProfile;
import com.noterik.springfield.tools.fs.model.config.ingest.VideoProfile;
import com.noterik.springfield.tools.ftp.FtpHelper;
import com.noterik.springfield.tools.FileHelper;

/**
 * TODO this class is now a big mess because of the changing of configs.
 * 
 * FIXME The following should be done:
 * 1. update springfield tools so it will read the final config.
 * 2. update apu to use the new config
 * 3. remove the backward compatibility for clients sending the old configuration (apu should be the
 * only one)
 * 4. in the ingest function(s) make sure to remove the response strings and move this to the
 * SimpleIngestRequestHandler
 * 5. make one type of function for ingesting all kinds of files
 * 
 * @author Jaap Blom
 */


public class SimpleIngestHandler {

	private static Logger logger = Logger.getLogger(SimpleIngestHandler.class);
	private static SimpleIngestHandler instance;

	private SimpleIngestHandler() {

	}

	public static SimpleIngestHandler instance() {
		if (instance == null) {
			instance = new SimpleIngestHandler();
		}
		return instance;
	}

	public String ingestFile(FileIngestConfig conf, IngestInputData iid) {
		String type = FileHelper.getFileType(iid.getSource());
		if (type == null) {
			return null;
		} else if (type.equals(FileHelper.VIDEO_FILE)) {
			return ingestVideo(conf, iid);
		} else if (type.equals(FileHelper.IMAGE_FILE)) {
			// TODO this function is not yet thought through well enough (what
			// about thumbs??)
			return ingestImage(conf, iid);
		} 
		return null;
	}

	
	/**
	 * only for OU !!! don't mess it up !!
	 * 
	 * @param conf
	 * @param iid
	 * @param domain
	 * @return
	 */
	public String ingestFileWithNewConfig(IngestConfig conf, IngestInputData iid, String domain) {
		String type = FileHelper.getFileType(iid.getSource());
		if (type == null) {
			return null;
		} else if (type.equals(FileHelper.VIDEO_FILE)) {
			return ingestVideoWithNewConfig(conf, iid, domain);
		} else if (type.equals(FileHelper.IMAGE_FILE)) {
			return ingestImageWithNewConfig(conf, iid, domain);
		} else if (type.equals(FileHelper.AUDIO_FILE)) {
			// TODO add check if audio
			return ingestAudioWithNewConfig(conf, iid, domain);
		}
		return null;
	}

	private String ingestAudioWithNewConfig(IngestConfig conf, IngestInputData iid, String domain) {		
		logger.debug("INGESTING AN AUDIO FILE");
		String response = null;
		// get the correct profile from the config (by using the rawindex)
		AudioProfile ra = null;
		String destinationUri = null;
		if (iid.isSmart()) {
			// in this case the profile will be chosen based on the raw id in
			// the destination uri
			ra = conf.getAudioSettings().getRawAudios().get(getRawIndexFromDestinationUri(iid.getDestinationUri()));
			destinationUri = iid.getDestinationUri();
		} else {
			//logger.debug("CONF: " + conf);
			//logger.debug("IID profile: " + iid.getProfile());
			//logger.debug("VS: " + conf.getVideoSettings());
			//logger.debug("VS PROF: " + conf.getVideoSettings().getRawVideos());
			ra = conf.getAudioSettings().getRawAudios().get(iid.getProfile());
			destinationUri = "/domain/" + domain + "/user/" + iid.getUser() + "/audio/1/rawaudio/" + ra.getRawIndex();
		}
		if (ra == null || destinationUri == null) {
			// TODO all the responses should move to the requesthandler
			return FSXMLBuilder.getErrorMessage("500", "Incorrect input parameters",
					"Please provide either a destination uri or a profile id", "http://teamelements.noterik.com/team");
		}
		// get the ftp config from the profile
		FTPConfig ftpc = ra.getFtpConfig();
		logger.debug(ftpc);
		if (ftpc.isEnabled()) {
			// get the id of the cluster
			String preferred = iid.getPreferred();
			int sid;
			if(preferred==null) {
				sid = getFTPServerIdForCollection(destinationUri, iid.getCollectionId(), ftpc.getServers(), ftpc
					.getOffset(), "audio");
			} else {
				sid = Integer.parseInt(preferred.trim());
			}
			String ftpServer = null;
			String user = null;
			String password = null;
			String mount = null;
			String backupFtpServer = null;
			String backupMount = null;
			boolean backup = false;
			boolean saveInCluster = sid != -1;
			boolean useMomar = iid.getMomar();
			if (saveInCluster) {
				// the ftp config indicates saving the file in the cluster
				ftpServer = ftpc.getPrefix() + sid + ftpc.getSuffix();
				user = ftpc.getPrefix() + sid;
				password = ftpc.getPrefix() + sid;
				backup = true;
			} else {
				// the ftp config indicates saving the file on a foreign server
				ftpServer = ftpc.getServer();
				user = ftpc.getUser();
				password = ftpc.getPassword();
			}
			if (!iid.isSmart()) {
				destinationUri = saveAudioProperties(iid, domain, ra.getRawIndex());
			}
			logger.debug("FTP SERVER: " + ftpServer);
			logger.debug("INPUT FILE: " + iid.getSource());
			logger.debug("DESTINATION: " + destinationUri);
			if (FtpHelper.sendFileWithFtp(iid.getSource(), destinationUri, ftpServer, user, password, false)) {
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
				if (FtpHelper.sendFileWithFtp(iid.getSource(), destinationUri, backupFtpServer, ftpc.getPrefix() + bid,
						ftpc.getPrefix() + bid, true)) {
					backupMount = ftpc.getPrefix() + bid;
				}
			}
			if (saveInCluster && mount == null && backupMount == null) {
				// the file could not be saved in the cluster
				response = FSXMLBuilder.getErrorMessage("500", "Could not transfer the ingest file to the FS",
						"The file: " + iid.getSource() + " could not be sent to either: " + ftpServer + " or "
								+ backupFtpServer, "http://teamelements.noterik.com/team");
			} else if (!saveInCluster && mount == null) {
				// the file could not be saved on the foreign ftp server
				response = FSXMLBuilder.getErrorMessage("500", "Could not transfer the ingest file to" + ftpServer,
						"The file: " + iid.getSource() + " could not be sent to: " + ftpServer,
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
				
				if(!useMomar){
					logger.debug("SETTING THE MOUNT PROPERTY:\n" + url + "\nmounts: " + mounts);
					FSXMLRequestHandler.instance().updateProperty(url, "mount", mounts, "PUT", false);
					String momarUri = destinationUri + "/properties/momar";
					logger.debug("SETTING THE MOMAR PROPERTY:\n" + momarUri + "\nmomar: " + useMomar);
					FSXMLRequestHandler.instance().updateProperty(momarUri, "momar", useMomar + "", "PUT", true);
				}else{
					logger.debug("SETTING THE MOUNT PROPERTY:\n" + url + "\nmounts: " + mounts);
					FSXMLRequestHandler.instance().updateProperty(url, "mount", mounts, "PUT", true);
				}
				response = FSXMLBuilder.getStatusMessage("The file has been succesfully ingested", "The file "
						+ FileHelper.getFileNameFromPath(iid.getSource()) + " has been succesfully ingested",
						destinationUri);
			}
		}
		return response;
	}

	/**
	 * Only used in OU !!!
	 *  
	 * @param conf
	 * @param iid
	 * @return
	 */

	private String ingestVideoWithNewConfig(IngestConfig conf, IngestInputData iid, String domain) {
		logger.debug("INGESTING A VIDEO ");
		String response = null;
		// get the correct profile from the config (by using the rawindex)
		VideoProfile rv = null;
		String destinationUri = null;
		if (iid.isSmart()) {
			// in this case the profile will be chosen based on the raw id in
			// the destination uri
			rv = conf.getVideoSettings().getRawVideos().get(getRawIndexFromDestinationUri(iid.getDestinationUri()));
			destinationUri = iid.getDestinationUri();
		} else {
			//logger.debug("CONF: " + conf);
			//logger.debug("IID profile: " + iid.getProfile());
			//logger.debug("VS: " + conf.getVideoSettings());
			//logger.debug("VS PROF: " + conf.getVideoSettings().getRawVideos());
			rv = conf.getVideoSettings().getRawVideos().get(iid.getProfile());
			destinationUri = "/domain/" + domain + "/user/" + iid.getUser() + "/video/1/rawvideo/" + rv.getRawIndex();
		}
		if (rv == null || destinationUri == null) {
			// TODO all the responses should move to the requesthandler
			return FSXMLBuilder.getErrorMessage("500", "Incorrect input parameters",
					"Please provide either a destination uri or a profile id", "http://teamelements.noterik.com/team");
		}
		// get the ftp config from the profile
		FTPConfig ftpc = rv.getFtpConfig();
		//logger.debug(ftpc);
		if (ftpc.isEnabled()) {
			// get the id of the cluster
			String preferred = iid.getPreferred();
			int sid;
			if(preferred==null) {
				sid = getFTPServerIdForCollection(destinationUri, iid.getCollectionId(), ftpc.getServers(), ftpc
					.getOffset(), "video");
			} else {
				sid = Integer.parseInt(preferred.trim());
			}
			String ftpServer = null;
			String user = null;
			String password = null;
			String mount = null;
			String backupFtpServer = null;
			String backupMount = null;
			boolean backup = false;
			boolean saveInCluster = sid != -1;
			boolean useMomar = iid.getMomar();
			if (saveInCluster) {
				// the ftp config indicates saving the file in the cluster
				ftpServer = ftpc.getPrefix() + sid + ftpc.getSuffix();
				user = ftpc.getPrefix() + sid;
				password = ftpc.getPrefix() + sid;
				backup = true;
			} else {
				// the ftp config indicates saving the file on a foreign server
				ftpServer = ftpc.getServer();
				user = ftpc.getUser();
				password = ftpc.getPassword();
			}
			if (!iid.isSmart()) {
				destinationUri = saveVideoProperties(rv.getEncodingProfile(), iid, domain, rv.getRawIndex());
			}
			logger.info("FTP SERVER: " + ftpServer);
			logger.info("INPUT FILE: " + iid.getSource());
			logger.info("DESTINATION: " + iid.getDestinationUri());
			logger.info("SIZE: " + new File(iid.getSource()).length() );
			if (FtpHelper.sendFileWithFtp(iid.getSource(), destinationUri, ftpServer, user, password, false)) {
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
				if (FtpHelper.sendFileWithFtp(iid.getSource(), destinationUri, backupFtpServer, ftpc.getPrefix() + bid,
						ftpc.getPrefix() + bid, true)) {
					backupMount = ftpc.getPrefix() + bid;
				}
			}
			if (saveInCluster && mount == null && backupMount == null) {
				// the file could not be saved in the cluster
				response = FSXMLBuilder.getErrorMessage("500", "Could not transfer the ingest file to the FS",
						"The file: " + iid.getSource() + " could not be sent to either: " + ftpServer + " or "
								+ backupFtpServer, "http://teamelements.noterik.com/team");
			} else if (!saveInCluster && mount == null) {
				// the file could not be saved on the foreign ftp server
				response = FSXMLBuilder.getErrorMessage("500", "Could not transfer the ingest file to" + ftpServer,
						"The file: " + iid.getSource() + " could not be sent to: " + ftpServer,
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
				
				if(!useMomar){
					logger.debug("SETTING THE MOUNT PROPERTY:\n" + url + "\nmounts: " + mounts);
					FSXMLRequestHandler.instance().updateProperty(url, "mount", mounts, "PUT", false);
					String momarUri = destinationUri + "/properties/momar";
					logger.debug("SETTING THE MOMAR PROPERTY:\n" + momarUri + "\nmomar: " + useMomar);
					FSXMLRequestHandler.instance().updateProperty(momarUri, "momar", useMomar + "", "PUT", true);
				}else{
					logger.debug("SETTING THE MOUNT PROPERTY:\n" + url + "\nmounts: " + mounts);
					FSXMLRequestHandler.instance().updateProperty(url, "mount", mounts, "PUT", true);
				}
				
				if(domain.equals("ou") || domain.equals("ou.devel")) {
					FSXMLRequestHandler.instance().updateProperty(destinationUri+"/properties/extension", "extension", "wmv", "PUT", true);
				}
				// get the updated raw video properties and use flanders to add
				// technical meta-data
				//logger.debug("Getting the properties for: " + destinationUri);
				//Document doc = FSXMLHandler.instance().getNodeProperties(destinationUri, false);
				//if (doc != null) {
					//String properties = doc.asXML();
					//logger.debug("Properties JAAPSCODEISREALLYSTUPIT are: " + properties);
					
					// properties = FlandersHelper.processRaw(destinationUri, properties);
					// logger.debug("Properties are now after flanders: " + properties);
					// FSXMLHandler.instance().saveFsXml(destinationUri, properties, "PUT");
				//}
				response = FSXMLBuilder.getStatusMessage("The file has been succesfully ingested", "The file "
						+ FileHelper.getFileNameFromPath(iid.getSource()) + " has been succesfully ingested",
						destinationUri);
			}
		}
		return response;
	}

	public String ingestVideo(FileIngestConfig conf, IngestInputData iid) {
		logger.debug("INGESTING A VIDEO");
		String response = null;
		FTPConfig ftpc = conf.getVideoSettings().getFtpConfig();
		logger.debug(ftpc);
		if (ftpc.isEnabled()) {
			String preferred = iid.getPreferred();
			int sid;
			if(preferred==null) {
				sid = getFTPServerIdForCollection(iid.getDestinationUri(), iid.getCollectionId(), ftpc.getServers(),
					ftpc.getOffset(), "video");
			} else {
				sid = Integer.parseInt(preferred.trim());
			}
			String ftpServer = null;
			String user = null;
			String password = null;
			String mount = null;
			String backupFtpServer = null;
			String backupMount = null;
			boolean backup = false;
			if (sid != -1) {
				ftpServer = ftpc.getPrefix() + sid + ftpc.getSuffix();
				user = ftpc.getPrefix() + sid;
				password = ftpc.getPrefix() + sid;
				backup = true;
			} else {
				ftpServer = ftpc.getPrefix() + ftpc.getSuffix();
				user = ftpc.getUser();
				password = ftpc.getPassword();
			}
			logger.info("FTP SERVER: " + ftpServer);
			logger.info("INPUT FILE: " + iid.getSource());
			logger.info("DESTINATION: " + iid.getDestinationUri());
			logger.info("SIZE: " + new File(iid.getSource()).length() );
			if (FtpHelper.sendFileWithFtp(iid.getSource(), iid.getDestinationUri(), ftpServer, user, password, false)) {
				mount = ftpc.getPrefix() + sid;
			}
			if (backup) {
				int bid = getBackupFTPServerId(sid, ftpc.getServers(), ftpc.getOffset());
				backupFtpServer = ftpc.getPrefix() + bid + ftpc.getSuffix();
				logger.debug("BACKUP FTP SERVER: " + backupFtpServer);
				if (FtpHelper.sendFileWithFtp(iid.getSource(), iid.getDestinationUri(), backupFtpServer, ftpc
						.getPrefix()
						+ bid, ftpc.getPrefix() + bid, true)) {
					backupMount = ftpc.getPrefix() + bid;
				}
			}
			if (mount == null && backupMount == null) {
				response = FSXMLBuilder.getErrorMessage("500", "Could not transfer the ingest file to the FS",
						"The file: " + iid.getSource() + " could not be sent to either: " + ftpServer + " or "
								+ backupFtpServer, "http://teamelements.noterik.com/team");
			} else {
				String url = iid.getDestinationUri() + "/properties/mount";
				String mounts = mount + "," + backupMount;
				logger.debug("SETTING THE MOUNT PROPERTY:\n" + url + "\nmounts: " + mounts);
				FSXMLRequestHandler.instance().updateProperty(url, "mount", mounts, "PUT", true);
				logger.debug("Getting the properties for: " + iid.getDestinationUri());
				Document doc = FSXMLRequestHandler.instance().getNodeProperties(iid.getDestinationUri(), false);
				if (doc != null) {
					String properties = doc.asXML();
					logger.debug("Properties are: " + properties);
					properties = FlandersHelper.processRaw(iid.getDestinationUri(), properties);
					logger.debug("Properties are now after flanders: " + properties);
					if(properties!=null) {
						// only save when flanders gave meaningful result
						FSXMLRequestHandler.instance().saveFsXml(iid.getDestinationUri(), properties, "PUT", true);
						// set status stag to done
						FSXMLRequestHandler.instance().updateProperty(iid.getDestinationUri()+"/properties/status", "status", "done", "PUT", true);
					}
				}
				response = FSXMLBuilder.getStatusMessage("The file has been succesfully ingested", "The file "
						+ FileHelper.getFileNameFromPath(iid.getSource()) + " has been succesfully ingested", iid
						.getDestinationUri());
			}
		}
		return response;
	}

	
	public String ingestImage(FileIngestConfig conf, IngestInputData iid) {
		logger.debug("INGESTING AN IMAGE");
		String response = null;
		FTPConfig ftpc = conf.getImageSettings().getFtpConfig();
		logger.debug(ftpc);
		if (ftpc.isEnabled()) {
			int sid = getFTPServerIdForCollection(iid.getDestinationUri(), iid.getCollectionId(), ftpc.getServers(),
					ftpc.getOffset(), "image");
			String ftpServer = null;
			String user = null;
			String password = null;
			String mount = null;
			String backupFtpServer = null;
			String backupMount = null;
			boolean backup = false;
			if (sid != -1) {
				ftpServer = ftpc.getPrefix() + sid + ftpc.getSuffix();
				user = ftpc.getPrefix() + sid;
				password = ftpc.getPrefix() + sid;
				backup = true;
			} else {
				ftpServer = ftpc.getPrefix() + ftpc.getSuffix();
				user = ftpc.getUser();
				password = ftpc.getPassword();
			}
			logger.debug("FTP SERVER: " + ftpServer);
			logger.debug("INPUT FILE: " + iid.getSource());
			logger.debug("DESTINATION: " + iid.getDestinationUri());
			String destDir = null;
			if (backup) {
				destDir = ftpc.getPrefix() + sid + iid.getDestinationUri();
			} else {
				destDir = iid.getDestinationUri();
			}
			logger.debug("FTP DATA: " + iid.getSource() + " destDir: " + destDir + " ftps " + ftpServer + " user "
					+ user + " pw " + password);
			if (FtpHelper.sendFileWithFtp(iid.getSource(), destDir, ftpServer, user, password, false)) {
				mount = ftpc.getPrefix() + sid;
			}
			if (backup) {
				int bid = getBackupFTPServerId(sid, ftpc.getServers(), ftpc.getOffset());
				backupFtpServer = ftpc.getPrefix() + bid + ftpc.getSuffix();
				logger.debug("BACKUP FTP SERVER: " + backupFtpServer);
				destDir = ftpc.getPrefix() + bid + iid.getDestinationUri();
				if (FtpHelper.sendFileWithFtp(iid.getSource(), destDir, backupFtpServer, ftpc.getPrefix() + bid, ftpc
						.getPrefix()
						+ bid, true)) {
					backupMount = ftpc.getPrefix() + bid;
				}
			}
			if (mount == null && backupMount == null) {
				response = FSXMLBuilder.getErrorMessage("500", "Could not transfer the ingest file to the FS",
						"The file: " + iid.getSource() + " could not be sent to either: " + ftpServer + " or "
								+ backupFtpServer, "http://teamelements.noterik.com/team");
			} else {
				String url = iid.getDestinationUri() + "/properties/mount";
				String mounts = mount + "," + backupMount;
				logger.debug("SETTING THE MOUNT PROPERTY:\n" + url + "\nmounts: " + mounts);
				FSXMLRequestHandler.instance().updateProperty(url, "mount", mounts, "PUT", true);
				response = FSXMLBuilder.getStatusMessage("The file has been succesfully ingested", "The file "
						+ FileHelper.getFileNameFromPath(iid.getSource()) + " has been succesfully ingested", iid
						.getDestinationUri());
			}
		}
		return response;
	}

	/**
	 * Only used in OU !!!
	 *  
	 * @param conf
	 * @param iid
	 * @return
	 */

	private String ingestImageWithNewConfig(IngestConfig conf, IngestInputData iid, String domain) {
		logger.debug("INGESTING AN IMAGE ");
		String response = null;
		// get the correct profile from the config (by using the rawindex)
		ImageProfile rv = null;
		String destinationUri = null;
		if (iid.isSmart()) {
			// in this case the profile will be chosen based on the raw id in
			// the destination uri
			rv = conf.getImageSettings().getRawImages().get(getRawIndexFromDestinationUri(iid.getDestinationUri()));
			destinationUri = iid.getDestinationUri();
		} else {
			//logger.debug("CONF: " + conf);
			//logger.debug("IID profile: " + iid.getProfile());
			//logger.debug("VS: " + conf.getVideoSettings());
			//logger.debug("VS PROF: " + conf.getVideoSettings().getRawVideos());
			rv = conf.getImageSettings().getRawImages().get(iid.getProfile());
			destinationUri = "/domain/" + domain + "/user/" + iid.getUser() + "/image/1/rawimage/" + rv.getRawIndex();
		}
		if (rv == null || destinationUri == null) {
			// TODO all the responses should move to the requesthandler
			return FSXMLBuilder.getErrorMessage("500", "Incorrect input parameters",
					"Please provide either a destination uri or a profile id", "http://teamelements.noterik.com/team");
		}
		// get the ftp config from the profile
		FTPConfig ftpc = rv.getFtpConfig();
		//logger.debug(ftpc);
		if (ftpc.isEnabled()) {
			// get the id of the cluster
			String preferred = iid.getPreferred();
			int sid;
			if(preferred==null) {
				sid = getFTPServerIdForCollection(destinationUri, iid.getCollectionId(), ftpc.getServers(), ftpc
					.getOffset(), "image");
			} else {
				sid = Integer.parseInt(preferred.trim());
			}
			String ftpServer = null;
			String user = null;
			String password = null;
			String mount = null;
			String backupFtpServer = null;
			String backupMount = null;
			boolean backup = false;
			boolean saveInCluster = sid != -1;
			boolean useMomar = iid.getMomar();
			if (saveInCluster) {
				// the ftp config indicates saving the file in the cluster
				ftpServer = ftpc.getPrefix() + sid + ftpc.getSuffix();
				user = ftpc.getPrefix() + sid;
				password = ftpc.getPrefix() + sid;
				backup = true;
			} else {
				// the ftp config indicates saving the file on a foreign server
				ftpServer = ftpc.getServer();
				user = ftpc.getUser();
				password = ftpc.getPassword();
			}
			
			logger.debug("FTP SERVER: " + ftpServer);
			logger.debug("INPUT FILE: " + iid.getSource());
			logger.debug("DESTINATION: " + destinationUri);
			if (FtpHelper.sendFileWithFtp(iid.getSource(), destinationUri, ftpServer, user, password, false)) {
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
				if (FtpHelper.sendFileWithFtp(iid.getSource(), destinationUri, backupFtpServer, ftpc.getPrefix() + bid,
						ftpc.getPrefix() + bid, true)) {
					backupMount = ftpc.getPrefix() + bid;
				}
			}
			if (saveInCluster && mount == null && backupMount == null) {
				// the file could not be saved in the cluster
				response = FSXMLBuilder.getErrorMessage("500", "Could not transfer the ingest file to the FS",
						"The file: " + iid.getSource() + " could not be sent to either: " + ftpServer + " or "
								+ backupFtpServer, "http://teamelements.noterik.com/team");
			} else if (!saveInCluster && mount == null) {
				// the file could not be saved on the foreign ftp server
				response = FSXMLBuilder.getErrorMessage("500", "Could not transfer the ingest file to" + ftpServer,
						"The file: " + iid.getSource() + " could not be sent to: " + ftpServer,
						"http://teamelements.noterik.com/team");
			} else {
				// set the mount properties in the raw image properties
				String url = destinationUri + "/properties/mount";
				String mounts = null;
				if (saveInCluster) {
					mounts = mount == null ? "" : mount;
					mounts += backupMount == null ? "" : "," + backupMount;
				} else {
					mounts = mount;
				}
				
				// set the extension				
				String fileUri = iid.getSource();
				String extension = fileUri.substring(fileUri.lastIndexOf(".") + 1);
				String extUrl = destinationUri + "/properties/extension";
				FSXMLRequestHandler.instance().updateProperty(extUrl, "extension", extension, "PUT", false);
			
				logger.debug("SETTING THE MOUNT PROPERTY:\n" + url + "\nmounts: " + mounts);
				FSXMLRequestHandler.instance().updateProperty(url, "mount", mounts, "PUT", true);
						
				// create the raw fot the thumbnails
				// createImageRawsForOU(destinationUri);
				
				response = FSXMLBuilder.getStatusMessage("The file has been succesfully ingested", "The file "
						+ FileHelper.getFileNameFromPath(iid.getSource()) + " has been succesfully ingested",
						destinationUri);
			}
		}
		return response;
	}
	
	/**
	 * only used in OU
	 * creates the raw images for the nelson jobs (thumbnails)
	 * @param uri
	 */
	
	// not used anymore !!
	public void createImageRawsForOU(String uri){
		
		String domain = URIParser.getDomainFromUri(uri);
		
		String configUri = "/domain/" + domain + "/config/image";
		Document doc = FSXMLRequestHandler.instance().getNodeProperties(configUri, false);
		
		List<Element> raws = (List<Element>)doc.selectNodes("//rawimage");
		logger.debug("OU image config\n" + doc.asXML());
		// go through all elements
		for (Element raw : raws){
			String id = raw.valueOf("@id");
			String rawUri = uri.substring(0, uri.lastIndexOf("/") + 1) + id + "/properties";						
			Element props = (Element)raw.selectSingleNode("./properties");
			StringBuffer fsxml = null;			
			// create the raw
			if(props != null) {
				fsxml = new StringBuffer("<fsxml><properties>");												
				Node child = null;
				for (Iterator i = ((Element) props).nodeIterator(); i.hasNext();) {
					child = (Node)i.next();
					fsxml.append("<" +child.getName() + ">" + child.getText() + "</" +child.getName() + ">");
				}				
				fsxml.append("</properties></fsxml>");
				logger.debug("ou uri: " + rawUri + " xml\n" + fsxml.toString());
				FSXMLRequestHandler.instance().saveFsXml(rawUri, fsxml.toString(), "PUT", true);
			}
		}
	}

	/**
	 * This function returns the mount which stores the video files of the
	 * specified collection
	 * 
	 * @param collection
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

	private int getBackupFTPServerId(int sid, int servers, int offset) {
		int bid = sid;
		int max = servers + offset - 1;
		bid += 3;
		bid = bid > max ? (bid - servers) + offset : bid;
		return bid;
	}

	/**
	 * This function creates a human readable object from the ingest input XML
	 * and returns it.
	 * 
	 * @param input
	 * @return
	 */

	public IngestInputData getInputVariables(Document input) {
		IngestInputData iid = new IngestInputData();
		// set the destination uri OR the user and profile id
		Node vp = input.selectSingleNode("/fsxml/properties/destinationuri");
		if (vp == null) {
			Node u = input.selectSingleNode("/fsxml/properties/user");
			if (u == null) {
				return null;
			} else {
				iid.setUser(u.getText());
				Node p = input.selectSingleNode("/fsxml/properties/profile");
				if (p == null) {
					return null;
				} else {
					iid.setProfile(p.getText());
				}
			}
		} else {
			iid.setDestinationUri(vp.getText());
		}
		// set the source
		Node fn = input.selectSingleNode("/fsxml/properties/source");
		if (fn == null) {
			return null;
		}
		iid.setSource(fn.getText());
		// set the collection id 
		Node cid = input.selectSingleNode("/fsxml/properties/collection");
		if (cid == null) {
			return null;
		}
		iid.setCollectionId(cid.getText());
		// set smart default to true otherwise look at the tag
		Node s = input.selectSingleNode("/fsxml/properties/smart");
		if (s == null) {
			iid.setSmart(true);
		} else {
			iid.setSmart(s.getText().equals("true"));
		}
		Node vid = input.selectSingleNode("/fsxml/properties/video");
		if (vid != null) {
			iid.setVideoId(vid.getText());
		}
		// set preferred mount
		Node pm = input.selectSingleNode("/fsxml/properties/preferred");
		if(pm!=null) {
			iid.setPrefferredMount(pm.getText());
		}		
		// set momar job
		Node m = input.selectSingleNode("/fsxml/properties/momar");
		if (m == null) {
			iid.setMomar(true);
		} else {
			iid.setMomar(m.getText().equals("true"));
		}
		return iid;
	}

	private String getRawIndexFromDestinationUri(String uri) {
		if (uri == null || uri.indexOf("/") == -1) {
			return null;
		}
		return uri.substring(uri.lastIndexOf("/") + 1);
	}
	
	private String saveAudioProperties(IngestInputData iid, String domain, String rawIndex){
		String destUri = null;
		String vidUri = "/domain/" + domain + "/user/" + iid.getUser() + "/audio";
		if (iid.getVideoId() == null) {
			String vidXml = "<fsxml><properties></properties></fsxml>";
			logger.debug("ADD(" + vidUri + ") " + vidXml);
			int id = IdHandler.instance().insert(vidUri);
			vidUri += "/" + id;
			logger.debug("VID URI AFTER: " + vidUri);		
			FSXMLRequestHandler.instance().saveFsXml(vidUri, vidXml, "POST", true);
		} else {
			vidUri += "/" + iid.getVideoId();
		}
		destUri = vidUri + "/rawaudio/" + rawIndex;
		logger.debug("DEST URI BECOMES: " + destUri);
		String rawXml = "<fsxml><properties/></fsxml>";
		logger.debug("ADD(" + rawXml + ") " + destUri);
		FSXMLRequestHandler.instance().saveFsXml(destUri + "/properties", rawXml, "PUT", true);
		return destUri;
	}

	private String saveVideoProperties(EncodingProfile ep, IngestInputData iid, String domain, String rawIndex) {
		String destUri = null;
		String vidUri = "/domain/" + domain + "/user/" + iid.getUser() + "/video";
		if (iid.getVideoId() == null) {
			String vidXml = "<fsxml><properties></properties></fsxml>";	
			logger.debug("ADD(" + vidUri + ") " + vidXml);
			int id = IdHandler.instance().insert(vidUri);
			vidUri += "/" + id;
			logger.debug("VID URI AFTER: " + vidUri);
			FSXMLRequestHandler.instance().saveFsXml(vidUri, vidXml, "POST", true);
		} else {
			vidUri += "/" + iid.getVideoId();
		}
		destUri = vidUri + "/rawvideo/" + rawIndex;
		logger.debug("DEST URI BECOMES: " + destUri);
		String rawXml = FSXMLBuilder.getVideoPropertiesFromProfile(ep);
		
		// add original filename
		try {
			String originalFilename = iid.getSource();
			originalFilename = originalFilename.substring(originalFilename.lastIndexOf("/")+1);
			Document doc = DocumentHelper.parseText(rawXml);
			Element properties = (Element)doc.selectSingleNode("//properties");
			Element oName = properties.addElement("original-filename");
			oName.setText(originalFilename);
			rawXml = doc.asXML();
		} catch(Exception e) {
			logger.error("Caught Exception: "+e.getMessage());
		}
		
		logger.debug("ADD(" + rawXml + ") " + destUri);
		FSXMLRequestHandler.instance().saveFsXml(destUri + "/properties", rawXml, "PUT", true);
		return destUri;
	}

}