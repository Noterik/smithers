package com.noterik.bart.fs.ingest;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.noterik.bart.fs.GlobalConfig;
import com.noterik.bart.fs.legacy.properties.PropertyHandler;
import com.noterik.bart.fs.legacy.tools.FlandersHelper;
import com.noterik.bart.fs.legacy.tools.XmlHelper;
import com.noterik.springfield.tools.FileHelper;
import com.noterik.springfield.tools.HttpHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.ftp.FtpHelper;


public class IngestHandler {

	private static Logger logger = Logger.getLogger(IngestHandler.class);
	private static IngestHandler instance;

	private IngestHandler() {

	}

	public static IngestHandler instance() {
		if (instance == null) {
			instance = new IngestHandler();
		}
		return instance;
	}

	public String ingestVideo(String uri, String domain, String xml) {
		String response = null;
		String origin = "";
		String destination = "";
		// get the settings file to see what to do with this video ingest
		String domIngest = "/domain/" + domain + "/settings/ingest";
		String ingest = PropertyHandler.getXMLfromProp(domIngest);
		// determine the path to the video file
		String fileName = XmlHelper.getPropertyValue(xml, XmlHelper.PROP_DATA);
		String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
		origin = XmlHelper.getValueFromIngest(ingest, "/fsxml/properties/video/basedir") + uri + "/" + fileName;		
		// where to store the video in the file system
		String url = XmlHelper.getPropertyValue(xml, XmlHelper.PROP_DESTINATION);
		String rawIndex = XmlHelper.getPropertyValue(xml, XmlHelper.PROP_RAW_INDEX);
		String dest = url + "/rawvideo/1";
		// whether to store in the old FS (b10) or the new one (use ftp)
		if (XmlHelper.useFtp(ingest, "video")) {
			String backupMount = null;
			String mount = null;

			String collection = XmlHelper.getPropertyValue(xml, XmlHelper.PROP_COLLECTION);

			String offPath = "/fsxml/properties/video/ftp/offset";
			String servPath = "/fsxml/properties/video/ftp/servers";
			String prefPath = "/fsxml/properties/video/ftp/prefix";
			String sufPath = "/fsxml/properties/video/ftp/suffix";
			String mtnPath = "/fsxml/properties/video/ftp/mount";

			String offset = XmlHelper.getValueFromIngest(ingest, offPath);
			String servers = XmlHelper.getValueFromIngest(ingest, servPath);
			String prefix = XmlHelper.getValueFromIngest(ingest, prefPath);
			String suffix = XmlHelper.getValueFromIngest(ingest, sufPath);
			String mountprf = XmlHelper.getValueFromIngest(ingest, mtnPath);
			int servs = Integer.valueOf(servers);
			int offs = Integer.valueOf(offset);

			if (extension.toLowerCase().equals("flv")) {
				dest = url + "/rawvideo/1";
			} else {
				dest = url + "/rawvideo/2";
			}
			// if the rawindex is specified in the request xml, put the video at
			// this rawvideo index
			if(rawIndex != null){
				dest = rawIndex.equals("") ? dest : url + "/rawvideo/" + rawIndex;
			}
			logger.debug("Copying file to new FS (using FTP) into: " + dest + " rawindex(" + rawIndex + ")");
			int sid = getFTPServerIdForCollectionVideo(url, collection, servs, offs);
			if (sid != -1) {
				int bid = getBackupFTPServerId(sid, servs, offs);
				String sHostBase = prefix + sid;
				String bHostBase = prefix + bid;
				String ftpServer = sHostBase + suffix;
				String backupFtpServer = bHostBase + suffix;

				logger.debug("FTP SERVER: " + ftpServer);
				logger.debug("BACKUP FTP SERVER: " + backupFtpServer);
				logger.debug("INPUT FILE: " + origin);
				logger.debug("DESTINATION: " + dest);
				if (FtpHelper.sendFileWithFtp(origin, dest, ftpServer, sHostBase, sHostBase, false)) {
					mount = mountprf + sid;
				}
				if (FtpHelper.sendFileWithFtp(origin, dest, backupFtpServer, bHostBase, bHostBase, true)) {
					backupMount = mountprf + bid;
				}
				if (mount == null && backupMount == null) {
					response = FSXMLBuilder.getErrorMessage("500", "Could not transfer the ingest file to the FS",
							"The file: " + origin + " could not be sent to either: " + sHostBase + " or " + bHostBase,
							"http://teamelements.noterik.com/team");
				}
			} else {
				response = FSXMLBuilder.getErrorMessage("500", "Invalid destination URL parameter",
						"The destination URL: " + url + " is invalid", "http://teamelements.noterik.com/team");
			}
			if (response == null) {
				logger.debug("response was null, properties will be set now !!");

				String properties = XmlHelper.getPropsFromXml(xml);
				String type = "rawvideo";
				if (mount != null || backupMount != null) {
					String m = mount;
					if (backupMount != null) {
						m = m == null ? backupMount : m + "," + backupMount;
					}
					properties = XmlHelper.setMountProperty(properties, m);
				}			
				logger.debug("xml before Flanders is: " + properties);
				// update the properties with information from flanders
				properties = FlandersHelper.processRaw(dest, properties);
				logger.debug("xml after Flanders is: " + properties);
				// add the properties to data base
				PropertyHandler.saveProperties(dest, type, properties);
				response = FSXMLBuilder.getStatusMessage("The file was succesfully ingested into the FS", "", "");
			} else {
				logger.error(response);
			}

		} else {
			String transPath = "/fsxml/properties/transcode";
			String transcode = XmlHelper.getValueFromIngest(ingest, transPath);

			logger.debug("Copying file to FS on b10 (old file system)");
			destination = GlobalConfig.instance().getIngestBaseDir() + dest + "/raw." + extension;
			if (!FileHelper.copyFile(origin, destination)) {
				response = FSXMLBuilder.getErrorMessage("500", "Could not transfer the ingest file to the old (b10) FS",
						"The file: " + origin + " could not be sent to the old (b10) FS",
						"http://teamelements.noterik.com/team");
			} else {
				// file was copied successfully, now setting properties
				String type = "rawvideo";

				String properties = XmlHelper.getPropsFromXml(xml);
				logger.debug("xml before Flanders is: " + properties);
				// update the properties with information from flanders
				properties = FlandersHelper.processRaw(dest, properties);
				logger.debug("xml after Flanders is: " + properties);
				PropertyHandler.saveProperties(dest, type, properties);
			}
			// creating unhappy files
			if (transcode.equals("true")) {
				String type = "rawvideo";
				List<Element> elems = XmlHelper.getRaws(ingest, "video");
				if (elems.size() != 0) {
					Iterator<Element> it = elems.iterator();
					Element elem = null;
					while (it.hasNext()) {
						elem = it.next();
						String id = elem.attributeValue("id");
						Element propsElem = (Element) elem.selectSingleNode("/properties");
						Element fsxml = DocumentHelper.createElement("fsxml");
						fsxml.add((Element) propsElem.clone());
						String props = fsxml.asXML();
						url = uri + "/rawvideo/" + id;

						PropertyHandler.saveProperties(url, type, props);
					}
				}
				String newUrl = url.substring(1);
				String TEFurl = "http://cw2.noterik.com:10001/domain/emails/fsorders";
				String finalUrl = TEFurl + "?res=" + newUrl;
				HttpHelper.sendRequest("GET", finalUrl, null,null);
			}
		}

		return response;
	}

	/**
	 * This function returns the mount which stores the video files of the
	 * specified collection
	 *
	 * @param collection
	 * @return
	 */

	private int getFTPServerIdForCollectionVideo(String uri, String collection, int servers, int offset) {
		logger.debug("CALCULATING HASH FOR URI(1): " + uri + " AND COLLECTION: " + collection);
		if (uri.indexOf("/video") == -1) {
			return -1;
		}
		uri = uri.substring(0, uri.indexOf("/video"));
		logger.debug("CALCULATING HASH FOR URI(2): " + uri + " AND COLLECTION: " + collection);
		uri += collection + "";
		int sid = uri.hashCode() % servers;
		sid = sid < 0 ? sid * -1 : sid;
		sid += offset;
		return sid;
	}

	/**
	 * This function returns the mount which stores the audio files of the
	 * specified collection
	 *
	 * @param collection
	 * @return
	 */

	private int getFTPServerIdForCollectionAudio(String uri, String collection, int servers, int offset) {
		logger.debug("CALCULATING HASH FOR URI(1): " + uri + " AND COLLECTION: " + collection);
		if (uri.indexOf("/audio") == -1) {
			return -1;
		}
		uri = uri.substring(0, uri.indexOf("/audio"));
		logger.debug("CALCULATING HASH FOR URI(2): " + uri + " AND COLLECTION: " + collection);
		uri += collection + "";
		int sid = uri.hashCode() % servers;
		sid = sid < 0 ? sid * -1 : sid;
		sid += offset;
		return sid;
	}

	/**
	 * This function returns the id of the backup server. The backup server id
	 * is simply 3 server id 'farther' than the original server.
	 *
	 * @param sid
	 * @return
	 */

	private int getBackupFTPServerId(int sid, int servers, int offset) {
		int bid = sid;
		// int offset = GlobalConfig.instance().getServerRangeOffset();
		// int servers = GlobalConfig.instance().getNumberOfServers();
		int max = servers + offset - 1;
		bid += 3;
		bid = bid > max ? (bid - servers) + offset : bid;
		return bid;
	}

	/**
	 * this function will create the 4 "unhappy files" in the database
	 *
	 * the default properties MUST be in the database
	 *
	 * @param uri
	 * @param domain
	 */
	public void processUnhappyOnes(String uri, String domain) {
		String type = "rawvideo";
		String props = "";
		String url = "";
		logger.debug("starting to process unhappy ones");
		String defDom = "/domain/" + domain + "/settings/tef/default/";
		String flvhigh = defDom + "flashhigh";
		String flvlow = defDom + "flashlow";
		String mp4high = defDom + "mp4high";
		String mp4low = defDom + "mp4low";
		// create first unhappy (mp4 low)
		props = PropertyHandler.getXMLfromProp(mp4low);
		url = uri + "/rawvideo/2";
		PropertyHandler.saveProperties(url, type, props);
		// create second unhappy (mp4 high)
		props = PropertyHandler.getXMLfromProp(mp4high);
		url = uri + "/rawvideo/3";
		PropertyHandler.saveProperties(url, type, props);
		// create third unhappy (flash low)
		props = PropertyHandler.getXMLfromProp(flvlow);
		url = uri + "/rawvideo/4";
		PropertyHandler.saveProperties(url, type, props);
		// create fourth unhappy (flash high)
		props = PropertyHandler.getXMLfromProp(flvhigh);
		url = uri + "/rawvideo/5";
		PropertyHandler.saveProperties(url, type, props);
	}




	public String ingestImage(String uri, String domain, String xml) {
		logger.debug("Processing image ingest...");

		String domIngest = "/domain/" + domain + "/settings/ingest";
		String ingest = PropertyHandler.getXMLfromProp(domIngest);
		String origin = "";
		String fileName = XmlHelper.getPropertyValue(xml, XmlHelper.PROP_DATA);
		String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
		origin = XmlHelper.getValueFromIngest(ingest, "/fsxml/properties/image/basedir") + uri + "/" + fileName;
		String url = XmlHelper.getPropertyValue(xml, XmlHelper.PROP_DESTINATION);
		String dest = url + "/rawimage/1";

		String response = null;
		String type = "rawimage";

		String imagePath = "";
		String prefix = "";
		String suffix = "";
		String user = "";
		String pass = "";
		String server = "";
		String backImagePath = "";
		String backServer = "";
		String backUser = "";
		String backPass = "";
		String mount = null;
		String backMount = null;

		boolean useFtp = XmlHelper.useFtp(ingest, "image");

		if (useFtp) {
			String prefixPath = "/fsxml/properties/image/ftp/prefix";
			String suffixPath = "/fsxml/properties/image/ftp/suffix";
			String userPath = "/fsxml/properties/image/ftp/user";
			String passPath = "/fsxml/properties/image/ftp/pass";

			prefix = XmlHelper.getValueFromIngest(ingest, prefixPath);
			suffix = XmlHelper.getValueFromIngest(ingest, suffixPath);
			user = XmlHelper.getValueFromIngest(ingest, userPath) + "1";
			pass = XmlHelper.getValueFromIngest(ingest, passPath) + "1";
			imagePath = XmlHelper.getValueFromIngest(ingest, "/fsxml/properties/image/ftp/path") + "1";

			server = prefix + "1" + suffix;
			backServer = prefix + "2" + suffix;

			backUser = XmlHelper.getValueFromIngest(ingest, userPath) + "2";
			backPass = XmlHelper.getValueFromIngest(ingest, passPath) + "2";
			backImagePath = XmlHelper.getValueFromIngest(ingest, "/fsxml/properties/image/ftp/path") + "2";



		} else {
			imagePath = XmlHelper.getValueFromIngest(ingest, "/fsxml/properties/image/basedir");
		}
		String newDest = imagePath + dest;
		String backNewDest = backImagePath + dest;

		List<Element> elems = XmlHelper.getRaws(ingest, "image");
		logger.debug("NUMBER OF RAWS: " + elems.size());
		if (elems.size() != 0) {
			Iterator<Element> it = elems.iterator();
			while (it.hasNext()) {
				Element elem = (Element)it.next().clone();
				String id = elem.attributeValue("id");
				String thumbDest = url + "/rawimage/" + id;
				logger.debug("ELEMENT FOUND:\n\n" + elem.asXML());
				Element propsElem = (Element)elem.selectSingleNode("./properties");
				logger.debug("PROPS ELEMENT:\n" + propsElem.asXML());
				if (propsElem != null) {
					String width = propsElem.selectSingleNode("./width").getText();
					String heigth = propsElem.selectSingleNode("./height").getText();
					int tWidth = Integer.valueOf(width);
					int tHeight = Integer.valueOf(heigth);
					String onlyName = fileName.substring(0, fileName.indexOf("."));
					String thumbLocation = XmlHelper.getValueFromIngest(ingest, "/fsxml/properties/image/basedir")
							+ uri + "/thumb" + onlyName + ".jpg";
					if (createThumb(tWidth, tHeight, origin, thumbLocation)) {
						Element fsxml = DocumentHelper.createElement("fsxml");
						fsxml.add((Element) propsElem.clone());
						if (useFtp) {

							//send to images1

							String thumbNewDest = imagePath + thumbDest;
							logger.debug("FTP HOST: " + server);
							logger.debug("INPUT FILE: " + thumbLocation);
							logger.debug("DESTINATION: " + thumbNewDest);
							if(FtpHelper.sendFileWithFtp(thumbLocation, thumbNewDest, server, user, pass, false)){
								mount = "images1";
							}

							//send to images2
							String backThumbDest = backImagePath + thumbDest;
							logger.debug("FTP HOST: " + backServer);
							logger.debug("INPUT FILE: " + thumbLocation);
							logger.debug("DESTINATION: " + backThumbDest);
							if(FtpHelper.sendFileWithFtp(thumbLocation, backThumbDest, backServer, backUser, backPass, true)){
								backMount = "images2";
							}
							String props = fsxml.asXML();
							if (mount != null || backMount != null) {
								String m = mount;
								if (backMount != null) {
									m = m == null ? backMount : m + "," + backMount;
								}
								props = XmlHelper.setMountProperty(props, m);
							}
							PropertyHandler.saveProperties(thumbDest, type, props);
						} else {
							String thumbNewDest = imagePath + thumbDest;
							if (!FileHelper.copyFile(thumbLocation, thumbNewDest)) {
								response = FSXMLBuilder.getErrorMessage("500",
										"Could not transfer the ingest file to the old (b10) FS", "The file: " + origin
												+ " could not be sent to the old (b10) FS",
										"http://teamelements.noterik.com/team");
							} else {
								String props = fsxml.asXML();
								PropertyHandler.saveProperties(thumbDest, type, props);
							}
						}
					} else {
						logger.error("There was an error creating thumbnail " + id);
					}
				} else {
					logger.error("The ingest settings xml is not properly configured");
				}
			}
		}
		if (useFtp) {
			//send to images1
			logger.debug("FTP HOST: " + server);
			logger.debug("INPUT FILE: " + origin);
			logger.debug("DESTINATION: " + newDest);
			if(FtpHelper.sendFileWithFtp(origin, newDest, server, user, pass, false)){
				mount = "images1";
			}

			//send to images2
			logger.debug("FTP HOST: " + backServer);
			logger.debug("INPUT FILE: " + origin);
			logger.debug("DESTINATION: " + backNewDest);
			if(FtpHelper.sendFileWithFtp(origin, backNewDest, backServer, backUser, backPass, true)){
				backMount = "images2";
			}
			String props = "<fsxml>" + "<properties>" + "<extension>" + extension + "</extension></properties>"
			+ "</fsxml>";
			if (mount != null || backMount != null) {
				String m = mount;
				if (backMount != null) {
					m = m == null ? backMount : m + "," + backMount;
				}
				props = XmlHelper.setMountProperty(props, m);
			}
			PropertyHandler.saveProperties(dest, type, props);

		} else {
			if (!FileHelper.copyFile(origin, newDest)) {
				response = FSXMLBuilder.getErrorMessage("500", "Could not transfer the ingest file to the old (b10) FS",
						"The file: " + origin + " could not be sent to the old (b10) FS",
						"http://teamelements.noterik.com/team");
			} else {
				String prop = "<fsxml>" + "<properties>" + "<extension>" + extension + "</extension>" + "</properties>"
						+ "</fsxml>";
				PropertyHandler.saveProperties(dest, type, prop);
			}

		}

		return response;
	}



	public String ingestBanner(String uri, String domain, String xml) {
		logger.debug("Processing swf ingest...");

		String domIngest = "/domain/" + domain + "/settings/ingest";
		String ingest = PropertyHandler.getXMLfromProp(domIngest);
		String origin = "";
		String fileName = XmlHelper.getPropertyValue(xml, XmlHelper.PROP_DATA);
		String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
		origin = XmlHelper.getValueFromIngest(ingest, "/fsxml/properties/image/basedir") + uri + "/" + fileName;
		String url = XmlHelper.getPropertyValue(xml, XmlHelper.PROP_DESTINATION);
		String dest = url + "/rawswf/1";

		String response = null;
		String type = "rawswf";

		String imagePath = "";
		String prefix = "";
		String suffix = "";
		String user = "";
		String pass = "";
		String server = "";
		String backImagePath = "";
		String backServer = "";
		String backUser = "";
		String backPass = "";
		String mount = null;
		String backMount = null;

		boolean useFtp = XmlHelper.useFtp(ingest, "image");

		if (useFtp) {
			String prefixPath = "/fsxml/properties/image/ftp/prefix";
			String suffixPath = "/fsxml/properties/image/ftp/suffix";
			String userPath = "/fsxml/properties/image/ftp/user";
			String passPath = "/fsxml/properties/image/ftp/pass";

			prefix = XmlHelper.getValueFromIngest(ingest, prefixPath);
			suffix = XmlHelper.getValueFromIngest(ingest, suffixPath);
			user = XmlHelper.getValueFromIngest(ingest, userPath) + "1";
			pass = XmlHelper.getValueFromIngest(ingest, passPath) + "1";
			imagePath = XmlHelper.getValueFromIngest(ingest, "/fsxml/properties/image/ftp/path") + "1";

			server = prefix + "1" + suffix;
			backServer = prefix + "2" + suffix;

			backUser = XmlHelper.getValueFromIngest(ingest, userPath) + "2";
			backPass = XmlHelper.getValueFromIngest(ingest, passPath) + "2";
			backImagePath = XmlHelper.getValueFromIngest(ingest, "/fsxml/properties/image/ftp/path") + "2";


		} else {
			imagePath = XmlHelper.getValueFromIngest(ingest, "/fsxml/properties/image/basedir");
		}
		String newDest = imagePath + dest;
		String backNewDest = backImagePath + dest;

		if (useFtp) {
			//send to images1
			logger.debug("FTP HOST: " + server);
			logger.debug("INPUT FILE: " + origin);
			logger.debug("DESTINATION: " + newDest);
			if(FtpHelper.sendFileWithFtp(origin, newDest, server, user, pass, false)){
				mount = "images1";
			}

			//send to images2
			logger.debug("FTP HOST: " + backServer);
			logger.debug("INPUT FILE: " + origin);
			logger.debug("DESTINATION: " + backNewDest);
			if(FtpHelper.sendFileWithFtp(origin, backNewDest, backServer, backUser, backPass, true)){
				backMount = "images2";
			}
			String props = "<fsxml>" + "<properties>" + "<extension>" + extension + "</extension></properties>"
			+ "</fsxml>";
			if (mount != null || backMount != null) {
				String m = mount;
				if (backMount != null) {
					m = m == null ? backMount : m + "," + backMount;
				}
				props = XmlHelper.setMountProperty(props, m);
			}
			PropertyHandler.saveProperties(dest, type, props);

		} else {
			if (!FileHelper.copyFile(origin, newDest)) {
				response = FSXMLBuilder.getErrorMessage("500", "Could not transfer the ingest file to the old (b10) FS",
						"The file: " + origin + " could not be sent to the old (b10) FS",
						"http://teamelements.noterik.com/team");
			} else {
				String prop = "<fsxml>" + "<properties>" + "<extension>" + extension + "</extension>" + "</properties>"
						+ "</fsxml>";
				PropertyHandler.saveProperties(dest, type, prop);
			}

		}

		return response;
	}


	/**
	 * this function takes care of an ingested image. Basically copies the image
	 * to the designated folder and creates the empty properties for it
	 *
	 * @param uri
	 * @param domain
	 * @param xml
	 */
	private String processIngestedImage(String uri, String domain, String xml) {
		logger.debug("Processing image ingest");
		String origin = "";
		String destination = "";
		String fileName = XmlHelper.getPropertyValue(xml, XmlHelper.PROP_DATA);
		String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
		origin = GlobalConfig.instance().getIngestBaseDir() + uri + "/" + fileName;
		String url = XmlHelper.getPropertyValue(xml, XmlHelper.PROP_DESTINATION);
		String dest = url + "/rawimage/1";
		String thumb1Dest = url + "/rawimage/2";
		String thumb2Dest = url + "/rawimage/3";
		String thumb3Dest = url + "/rawimage/4";
		String imagePath = GlobalConfig.instance().getFtpImagePath();
		String newDest = imagePath + dest;
		String collection = XmlHelper.getPropertyValue(xml, XmlHelper.PROP_COLLECTION);
		destination = GlobalConfig.instance().getIngestBaseDir() + dest + "/raw." + extension;
		String response = null;
		String imageUser = GlobalConfig.instance().getFtpImageUser();
		String imagePass = GlobalConfig.instance().getFtpImagePass();

		String prop = "<fsxml>" + "<properties>" + "<extension>" + extension + "</extension>" + "</properties>"
				+ "</fsxml>";
		String type = "rawimage";
		// add the properties to data base
		PropertyHandler.saveProperties(dest, type, prop);

		String thumb1Width = "";
		String thumb1Height = "";

		String thumb2Width = "";
		String thumb2Height = "";

		String thumb3Width = "";
		String thumb3Height = "";

		int t1Width = Integer.valueOf(thumb1Width);
		int t1Height = Integer.valueOf(thumb1Height);

		int t2Width = Integer.valueOf(thumb2Width);
		int t2Height = Integer.valueOf(thumb2Height);

		int t3Width = Integer.valueOf(thumb3Width);
		int t3Height = Integer.valueOf(thumb3Height);

		String onlyName = fileName.substring(0, fileName.indexOf("."));
		String thumb1Location = GlobalConfig.instance().getIngestBaseDir() + uri + "/thumb1" + onlyName + ".jpg";
		String thumb2Location = GlobalConfig.instance().getIngestBaseDir() + uri + "/thumb2" + onlyName + ".jpg";
		String thumb3Location = GlobalConfig.instance().getIngestBaseDir() + uri + "/thumb3" + onlyName + ".jpg";

		if (collection == null) {
			logger.debug("Copying file to FS on b10 (old file system)");

			if (!FileHelper.copyFile(origin, destination)) {
				response = FSXMLBuilder.getErrorMessage("500", "Could not transfer the ingest file to the old (b10) FS",
						"The file: " + origin + " could not be sent to the old (b10) FS",
						"http://teamelements.noterik.com/team");
			}
		} else {
			logger.debug("Copying file to new FS (using FTP)");

			String ftpHost = GlobalConfig.instance().getFtpImage();

			// create the thumbs, send them through FTP and set the properties
			if (createThumb(t1Width, t1Height, origin, thumb1Location)) {

				String thumb1NewDest = imagePath + thumb1Dest;

				logger.debug("FTP HOST: " + ftpHost);
				logger.debug("INPUT FILE: " + thumb1Location);
				logger.debug("DESTINATION: " + thumb1NewDest);
				FtpHelper.sendFileWithFtp(thumb1Location, thumb1NewDest, ftpHost, imageUser, imagePass, true);

				prop = "<fsxml><properties><width>" + t1Width + "</width><height>" + t1Height
						+ "</height><extension>jpg</extension></properties></fsxml>";

				PropertyHandler.saveProperties(thumb1Dest, type, prop);

			} else {
				logger.error("There was an error creating the thumbnail 1");
			}
			if (createThumb(t2Width, t2Height, origin, thumb2Location)) {

				String thumb2NewDest = imagePath + thumb2Dest;

				logger.debug("FTP HOST: " + ftpHost);
				logger.debug("INPUT FILE: " + thumb2Location);
				logger.debug("DESTINATION: " + thumb2NewDest);
				FtpHelper.sendFileWithFtp(thumb2Location, thumb2NewDest, ftpHost, imageUser, imagePass, true);

				prop = "<fsxml><properties><width>" + t2Width + "</width><height>" + t2Height
						+ "</height><extension>jpg</extension></properties></fsxml>";

				PropertyHandler.saveProperties(thumb2Dest, type, prop);

			} else {
				logger.error("There was an error creating the thumbnail 2");
			}
			if (createThumb(t3Width, t3Height, origin, thumb3Location)) {

				String thumb3NewDest = imagePath + thumb3Dest;

				logger.debug("FTP HOST: " + ftpHost);
				logger.debug("INPUT FILE: " + thumb3Location);
				logger.debug("DESTINATION: " + thumb3NewDest);
				FtpHelper.sendFileWithFtp(thumb3Location, thumb3NewDest, ftpHost, imageUser, imagePass, true);

				prop = "<fsxml><properties><width>" + t3Width + "</width><height>" + t3Height
						+ "</height><extension>jpg</extension></properties></fsxml>";

				PropertyHandler.saveProperties(thumb3Dest, type, prop);

			} else {
				logger.error("There was an error creating the thumbnail 3");
			}

			logger.debug("FTP HOST: " + ftpHost);
			logger.debug("INPUT FILE: " + origin);
			logger.debug("DESTINATION: " + newDest);
			FtpHelper.sendFileWithFtp(origin, newDest, ftpHost, imageUser, imagePass, true);

		}

		if (response == null) {

			response = FSXMLBuilder.getStatusMessage("The file was succesfully ingested into the FS", "", "");
		}

		return response;
	}

	public String ingestAudio(String uri, String domain, String xml) {
		logger.debug("Processing audio ingest");
		logger.debug("\n\n XML is: " + xml);

		String domIngest = "/domain/" + domain + "/settings/ingest";
		String ingest = PropertyHandler.getXMLfromProp(domIngest);

		String response = null;
		String origin = "";
		String destination = "";
		String fileName = XmlHelper.getPropertyValue(xml, XmlHelper.PROP_DATA);
		String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
		String collection = XmlHelper.getPropertyValue(xml, XmlHelper.PROP_COLLECTION);
		origin = GlobalConfig.instance().getIngestBaseDir() + uri + "/" + fileName;
		String url = XmlHelper.getPropertyValue(xml, XmlHelper.PROP_DESTINATION);
		String dest = url + "/rawaudio/1";
		destination = XmlHelper.getValueFromIngest(ingest, "/fsxml/properties/image/basedir") + dest + "/raw."
				+ extension;
		String mount = null;
		String backupMount = null;

		boolean useFtp = XmlHelper.useFtp(ingest, "audio");

		if (useFtp) {

			String offPath = "/fsxml/properties/audio/ftp/offset";
			String servPath = "/fsxml/properties/audio/ftp/servers";
			String prefPath = "/fsxml/properties/audio/ftp/prefix";
			String sufPath = "/fsxml/properties/audio/ftp/suffix";
			String mtnPath = "/fsxml/properties/audio/ftp/mount";

			String offset = XmlHelper.getValueFromIngest(ingest, offPath);
			String servers = XmlHelper.getValueFromIngest(ingest, servPath);
			String prefix = XmlHelper.getValueFromIngest(ingest, prefPath);
			String suffix = XmlHelper.getValueFromIngest(ingest, sufPath);
			String mountprf = XmlHelper.getValueFromIngest(ingest, mtnPath);
			int servs = Integer.valueOf(servers);
			int offs = Integer.valueOf(offset);

			int sid = getFTPServerIdForCollectionAudio(url, collection, servs, offs);
			if (sid != -1) {
				int bid = getBackupFTPServerId(sid, servs, offs);
				String sHostBase = GlobalConfig.instance().getFtpServerPrefix() + sid;
				String bHostBase = GlobalConfig.instance().getFtpServerPrefix() + bid;
				String ftpServer = sHostBase + GlobalConfig.instance().getFtpServerSuffix();
				String backupFtpServer = bHostBase + GlobalConfig.instance().getFtpServerSuffix();

				// dest += "/raw." + extension;
				logger.debug("FTP SERVER: " + ftpServer);
				logger.debug("BACKUP FTP SERVER: " + backupFtpServer);
				logger.debug("INPUT FILE: " + origin);
				logger.debug("DESTINATION: " + dest);

				if (FtpHelper.sendFileWithFtp(origin, dest, ftpServer, sHostBase, sHostBase, false)) {
					mount = GlobalConfig.instance().getMountPrefix() + sid;
				}
				if (FtpHelper.sendFileWithFtp(origin, dest, backupFtpServer, bHostBase, bHostBase, true)) {
					backupMount = GlobalConfig.instance().getMountPrefix() + bid;
				}

				if (mount == null && backupMount == null) {
					response = FSXMLBuilder.getErrorMessage("500", "Could not transfer the ingest file to the FS",
							"The file: " + origin + " could not be sent to either: " + sHostBase,
							"http://teamelements.noterik.com/team");
				}
			} else {
				response = FSXMLBuilder.getErrorMessage("500", "Invalid destination URL parameter",
						"The destination URL: " + url + " is invalid", "http://teamelements.noterik.com/team");
			}
		} else {

			logger.debug("Copying file to FS on b10 (old file system)");

			if (!FileHelper.copyFile(origin, destination)) {
				response = FSXMLBuilder.getErrorMessage("500", "Could not transfer the ingest file to the old (b10) FS",
						"The file: " + origin + " could not be sent to the old (b10) FS",
						"http://teamelements.noterik.com/team");
			}
		}

		if (response == null) {

			String prop = "<fsxml>" + "<properties>" + "<extension>" + extension + "</extension>" + "</properties>"
					+ "</fsxml>";
			String type = "rawaudio";

			if (mount != null || backupMount != null) {
				String m = mount;
				if (backupMount != null) {
					m = m == null ? backupMount : m + "," + backupMount;
				}
				prop = XmlHelper.setMountProperty(prop, m);
			}

			// add the properties to data base
			PropertyHandler.saveProperties(dest, type, prop);

			response = FSXMLBuilder.getStatusMessage("The file was succesfully ingested into the FS", "", "");
		}

		return response;
	}

	/**
	 * creates a thumbnail of a given image
	 *
	 * @param width
	 * @param height
	 * @param origin
	 * @param dest
	 * @return
	 */
	public boolean createThumb(int width, int height, String origin, String dest) {
		logger.debug("CREATING THUMB FROM: " + origin);
		boolean success = true;

		BufferedImage img = null;
		try {
			img = ImageIO.read(new File(origin));
		} catch (IOException e) {
			logger.error("",e);
			success = false;
		}

		logger.debug("thumb origin is: " + origin);

		String ext = origin.substring(origin.lastIndexOf(".") + 1, origin.length());
		int type = 0;

		if (ext.toLowerCase().equals("png")) {
			type = 5;
		} else {
			type = img.getType();
		}

		logger.debug("origin for thumb is: " + origin);
		logger.debug("destination for thumb is: " + dest);

		BufferedImage bufimg2 = new BufferedImage(width, height, type);
		Graphics2D g2d = (Graphics2D) bufimg2.getGraphics();
		g2d.scale((double) width / img.getWidth(), (double) height / img.getHeight());
		g2d.drawImage(img, 0, 0, null);
		img = bufimg2;

		try {
			BufferedImage bi = img; // retrieve image
			File outputfile = new File(dest);
			ImageIO.write(bi, "jpg", outputfile);
		} catch (IOException e) {
			logger.error("",e);
			success = false;
		}

		return success;
	}

}