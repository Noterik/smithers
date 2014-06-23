/*
 * Created on Jan 9, 2009
 */
package com.noterik.bart.fs.action.common.ingest;

import java.util.Iterator;
import java.util.Map;

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
import com.noterik.springfield.tools.fs.model.RawImage;
import com.noterik.springfield.tools.fs.model.config.image.ImageConfig;
import com.noterik.springfield.tools.ftp.FtpHelper;

/**
 * Script offering the possibilities for ingesting image files in either the cluster
 * machines or on an external ftp server. It will also update properties of the triggered
 * rawimage and also create the properties for all the other rawimages mentioned in the 
 * config: /domain/{domain}/config/image
 * 
 * Another script will read the other rawimages and check if it has the <redo> tag for
 * nelson. If so nelson will create thumbnails from this originally ingested image.
 * 
 * TODO make sure this script checks for the <original> tag
 * 
 * 
 * @author Jaap
 *
 */

public class IngestImageAction extends ActionAdapter {

	private static Logger logger = Logger.getLogger(IngestImageAction.class);

	@Override
	public String run() {
		logger.debug("IN THE IMAGE INGEST SCRIPT");
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
						String imageId = URIParser.getImageIdFromUri(uri);
						String rawId = URIParser.getRawIdFromUri(uri, "image");
						String vcUri = "/domain/" + domain + "/config/image";
						logger.debug("IL" + il);
						logger.debug("DOMAIN " + domain);
						logger.debug("USER " + user);
						logger.debug("IMAGE ID " + imageId);
						logger.debug("RAW ID " + rawId);
						Document icDoc = FSXMLRequestHandler.instance().getNodeProperties(vcUri, false);
						if (icDoc != null) {
							ImageConfig ic = FSXMLParser.getImageConfigFromXml(icDoc.asXML());
							response = ingestImage(ic, il, domain, user, imageId, rawId);
						}
					}
				}
			}
		} catch (DocumentException e) {
			logger.error("",e);
		}
		return response;
	}

	/**
	 * This ingest function will have a look at the
	 * /domain/{domain}/config/image and will send the uploaded rawimage to the
	 * ftpconfig defined in the current rawimage. After that, for all the
	 * rawimages other than the origal, the function will copy the properties
	 * and add the redo tag, so nelson will create the proper thumb for in on
	 * the image server.
	 * 
	 * @param ic
	 * @param location
	 * @param domain
	 * @param user
	 * @param imageId
	 * @param rawId
	 * @return
	 */

	private String ingestImage(ImageConfig ic, String location, String domain, String user, String imageId, String rawId) {
		RawImage ri = ic.getRawImages().get(rawId);
		logger.debug("INGESTING AN IMAGE");
		String response = null;
		FTPConfig ftpc = ri.getFtpConfig();
		logger.debug(ftpc);
		String destinationUri = "/domain/" + domain + "/user/" + user + "/image/" + imageId + "/rawimage/" + rawId;
		if (ftpc.isEnabled()) {
			int sid = -1;
			if (ftpc.getServer() == null) {
				sid = getFTPServerIdForCollection(destinationUri, "1", ftpc.getServers(), ftpc.getOffset(), "image");
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
			logger.debug("FTP DATA: " + location + " destDir: " + destDir + " ftps " + ftpServer + " user " + ftpUser
					+ " pw " + ftpPassword);
			if (FtpHelper.sendFileWithFtp(location, destDir, ftpServer, ftpUser, ftpPassword, false)) {
				if (saveInCluster) {
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
				setRawImageProperties(ri, destinationUri);
				if (saveInCluster) {
					// TODO test this
					createOtherRawImages(ic, domain, user, imageId);
				}
				response = FSXMLBuilder.getStatusMessage("The file has been succesfully ingested", "The file "
						+ FileHelper.getFileNameFromPath(location) + " has been succesfully ingested", destinationUri);
			}
		}
		return response;
	}

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

	private boolean setRawImageProperties(RawImage ri, String uri) {
		String rawXml = "<fsxml>" + ri.asXML(false) + "</fsxml>";
		logger.debug("XML\n" + rawXml);
		logger.debug("URI\n" + uri);
		String response = FSXMLRequestHandler.instance().handlePUT(uri + "/properties", rawXml);
		logger.debug(response);
		return false;
	}

	private void createOtherRawImages(ImageConfig ic, String domain, String user, String imageId) {
		String baseUri = "/domain/" + domain + "/user/" + user + "/image/" + imageId + "/rawimage/";
		RawImage ri = null;
		String riUri = null;
		Map<String, RawImage> ris = ic.getRawImages();
		// TODO ok add the properties to the raws and then add referid's to
		// these jobs
		for (Iterator i = ris.keySet().iterator(); i.hasNext();) {
			ri = ris.get(i.next());
			if (!ri.isOriginal()) {
				riUri = baseUri + ri.getId();
				// set the raw properties
				String riXml = "<fsxml>" + ri.asXML(false) + "</fsxml>";
				logger.debug("---> Setting redo for nelson thumb jobs: " + riXml.toString());
				FSXMLRequestHandler.instance().handlePUT(riUri + "/properties", riXml.toString());
			}
		}
	}
}