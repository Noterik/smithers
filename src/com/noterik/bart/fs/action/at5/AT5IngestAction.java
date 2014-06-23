package com.noterik.bart.fs.action.at5;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.action.ActionException;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.bart.fs.ingest.IngestInputData;
import com.noterik.bart.fs.ingest.SimpleIngestHandler;
import com.noterik.springfield.tools.HttpHelper;
import com.noterik.springfield.tools.XMLHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.FSXMLParser;
import com.noterik.springfield.tools.fs.URIParser;
import com.noterik.springfield.tools.fs.model.config.FileIngestConfig;
import com.noterik.springfield.tools.fs.model.config.UploadConfig;
import com.noterik.springfield.tools.fs.model.config.ingest.EncodingProfile;


public class AT5IngestAction extends ActionAdapter {
	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(AT5IngestAction.class);
	
	public static final String RESPONSE_OK = "200";
	
	/**
	 * Event uri
	 */
	private String eventUri;
	
	/**
	 * Ingest Settings URI
	 */
	private static final String INGEST_SETTINGS_URI = "/domain/{domainid}/config/upload";
	
	/**
	 * Default collection uri
	 */
	private static final String DEFAULT_COLLECTION_URI = "/domain/{domainid}/user/{userid}/collection/default/video";
	
	/**
	 * parameters
	 */
	private String videofilename;
	private String xmlfilename;
	private String at5id;
	private String userid;
	private String domain;
	private String httpDataServer = "http://vdx.at5.net/data/vdx/";
	private String lFolder = "/usr/local/data/va-data4/domain/{domainid}/ingest/{id}/";
	private int preferredStream = 4;
	
	@Override
	public String run() {
		init();
		
		try {
			// remove (possibly) old status/error messages
			removeError();
			removeStatus();
			
			// parse request data
			setStatus("Initializing");
			doStatusCallback("initialize");
			parseRequestData();
		
			// get video file from ftp server
			setStatus("Getting video file from http");
			doStatusCallback("gethttp");
			getVideoFile();
			
			// replacement for xml ingest
			setStatus("Ingesting video file");
			doStatusCallback("ingest");
			ingestVideoFile();
			
			// get xml file from ftp server
			/*
			setStatus("Getting xml file from http");
			getXmlFile();
			
			// ingest xml file (metadata)
			setStatus("Ingesting xml file");
			ingestXmlFile();
			*/
			
			// add to collection
			addToDefaultCollection();
		
			// transcode
			setStatus("Transcoding file");
			doStatusCallback("transcode");
			transcodeVideoFile();
		} catch (Exception e) {
			if(e instanceof ActionException) {
				setError("",e.getMessage());
			} else {
				setError("Internal system error",e.getMessage());
			}
			logger.error("",e);
			return null;
		} 
		
		return null;
	}

	/**
	 * Adds the videos to the default collection
	 */
	private void addToDefaultCollection() {
		// build collection and video uri
		String cUri = DEFAULT_COLLECTION_URI.replace("{domainid}", domain).replace("{userid}", userid);
		String vUri = "/domain/"+domain+"/user/"+userid+"/video/"+at5id;
		
		// update cUri
		cUri += "/"+at5id;
		
		// add referid
		logger.debug("Adding referid to default collection: " + vUri);
		StringBuffer xml = new StringBuffer("<fsxml>");
		xml.append("<attributes>");
		xml.append("<referid>");
		xml.append(vUri);
		xml.append("</referid>");
		xml.append("</attributes>");
		xml.append("</fsxml>");
		FSXMLRequestHandler.instance().saveAttributes(cUri, xml.toString(), "PUT");
	}

	private void init() {
		eventUri = event.getUri();
		
		// get id's
		at5id = URIParser.getCurrentUriPart(event.getUri());
		userid = URIParser.getUserFromUri(event.getUri());
		domain = URIParser.getDomainFromUri(event.getUri());
		logger.debug("uri: "+event.getUri()+", domain: "+domain+", user: "+userid+", id: "+at5id);
		
		// update local folder
		lFolder = lFolder.replace("{domainid}", domain).replace("{id}",at5id);
	}
	
	/**
	 * Parses the request data for specific parameters
	 * @throws ActionException 
	 */
	private void parseRequestData() throws ActionException {
		// get xml data
		String requestData = event.getRequestData();
		Document doc;
		try {
			doc = DocumentHelper.parseText(requestData);
			videofilename = doc.valueOf("//properties/videofilename");
			xmlfilename = doc.valueOf("//properties/xmlfilename");
		} catch (Exception e) {
			logger.error("",e);
			throw new ActionException("Could not parse request data");
		}
	}
	
	/**
	 * Gets video file from http server
	 * @throws ActionException 
	 */
	private void getVideoFile() throws ActionException {
		logger.debug("getting video file: " + videofilename);
		
		String url = httpDataServer + videofilename;
		String lFileName = lFolder + videofilename;
		logger.debug("url: "+url);
		logger.debug("lFileName: "+lFileName);
		if(!HttpHelper.getFileWithHttp(url, lFileName)) {
			throw new ActionException("Could not get video file from http server");
		}
	}
	
	/**
	 * Gets xml file from http server
	 * @throws ActionException 
	 */
	private void getXmlFile() throws ActionException {
		logger.debug("getting xml file: " + xmlfilename);
		
		// TODO: get actual xml
		/*
		String url = httpDataServer + xmlfilename;
		String lFileName = lFolder + xmlfilename;
		*/
		String url = "http://at5.devel.noterik.com/test/at5test.xml";
		String lFileName = lFolder + xmlfilename;
		if(!HttpHelper.getFileWithHttp(url, lFileName)) {
			throw new ActionException("Could not get xml file from http server");
		}
	}
	
	/**
	 * Process xml metadata file
	 * @throws ActionException
	 */
	private void ingestXmlFile() throws ActionException {		
		// parse xml
		Document document = null;
		try {
			SAXReader reader = new SAXReader();
	        document = reader.read(lFolder+xmlfilename);
		} catch(DocumentException e) {
			logger.error("",e);
			throw new ActionException("Document Exception: " + e.getMessage());
		}
		
		if(document!=null) {
			// temporary parsing of xml
			Element tmpProp =(Element)document.getRootElement().selectSingleNode("properties");
			
			logger.debug("ingest xml file: " + tmpProp.asXML());
						
			// TODO: implement parsing to assets
			
			// store video
			String videoUri = "/domain/"+domain+"/user/"+userid+"/video/"+at5id;
			if(!FSXMLRequestHandler.instance().saveFsXml(videoUri, "<fsxml>"+tmpProp.asXML()+"</fsxml>", "PUT", true)) {
				throw new ActionException("Video could not be saved");
			}
		}
	}
	
	/**
	 * Ingest video file with empty properties
	 * @throws ActionException
	 */
	private void ingestVideoFile() throws ActionException {		
		// store video
		String vidProperties = "<fsxml>" +
								"<properties>" +
								"<title>"+at5id+"</title>" +
								"</properties>"	+							
								"</fsxml>";
		String videoUri = "/domain/"+domain+"/user/"+userid+"/video/"+at5id;
		if(!FSXMLRequestHandler.instance().saveFsXml(videoUri, vidProperties, "PUT", true)) {
			throw new ActionException("Video could not be saved");
		}		
		
		/******  ingest original file ******/
		String rawvideoUri = videoUri+"/rawvideo/1";
		String ingestXml = "<fsxml>" +
				"<properties>" +
				"<destinationuri>"+rawvideoUri+"</destinationuri>" +
				"<source>"+lFolder+videofilename+"</source>" +
				"<smart>true</smart>" +
				"<collection>1</collection>" +
				"<preferred>"+preferredStream+"</preferred>"+
				"</properties>" +
				"</fsxml>";		
		
		// set extension tag 
		String extension = videofilename.substring(videofilename.lastIndexOf(".")+1);
		String rawXml = "<fsxml><properties><extension>"+extension+"</extension></properties></fsxml>";
		FSXMLRequestHandler.instance().handlePUT(rawvideoUri+"/properties", rawXml);
		
		if(!ingestOriginal(domain,ingestXml)) {
			logger.debug("ingest xml: " + ingestXml);
			throw new ActionException("Could not ingest original file");
		}
		
		// set original
		FSXMLRequestHandler.instance().updateProperty(rawvideoUri+"/properties/original", "original", "true", "PUT", false);
	}

	/**
	 * Ingest original file and crete jobs for momar 
	 * 
	 * @throws ActionException
	 */
	private void transcodeVideoFile() throws ActionException {
		logger.debug("transcoding video file: " + videofilename);
				
		String videoUri = "/domain/"+domain+"/user/"+userid+"/video/"+at5id;
		String rawvideoUri = videoUri+"/rawvideo/1";
		
		/******  create jobs for momar ******/
		// get stream
		String mount = FSXMLRequestHandler.instance().getPropertyValue(rawvideoUri+"/properties/mount");
		
		// get domain ingest config
		String configXml = FSXMLRequestHandler.instance().getNodeProperties(INGEST_SETTINGS_URI.replace("{domainid}", domain), false).asXML();
		UploadConfig uc = FSXMLParser.getUploadConfigFromXml(configXml);
		
		// get profiles
		List<EncodingProfile> profiles = uc.getEncodingProfiles();
		List<String> checklist = new ArrayList<String>();
		String fsxml;
		EncodingProfile encProf;
		int rawvideoid=2;
		for(Iterator<EncodingProfile> iter = profiles.iterator(); iter.hasNext(); rawvideoid++) {
			// create fsxml
			encProf = iter.next();
			fsxml = "<fsxml>" +
					"<properties>" +
					"<format>"+encProf.getFormat()+"</format>" +
					"<extension>"+encProf.getExtension()+"</extension>" +
					"<wantedwidth>"+encProf.getWidth()+"</wantedwidth>" +
					"<wantedheight>"+encProf.getHeight()+"</wantedheight>" +
					"<wantedbitrate>"+encProf.getBitRate()+"</wantedbitrate>" +
					"<wantedframerate>"+encProf.getFrameRate()+"</wantedframerate>" +
					"<wantedkeyframerate>"+encProf.getKeyFrameRate()+"</wantedkeyframerate>" +
					"<wantedaudiobitrate>"+encProf.getAudioBitRate()+"</wantedaudiobitrate>";
			if(encProf.getBatchFile() != null) {
				fsxml += "<batchfile>"+encProf.getBatchFile()+"</batchfile>";
			}
			fsxml += "<reencode>true</reencode>" +
					"<mount>"+mount+"</mount>" +
					"</properties>" +
					"</fsxml>";
			rawvideoUri = videoUri+"/rawvideo/"+rawvideoid;
			
			// put in checklist
			checklist.add(rawvideoUri);
			
			// store
			FSXMLRequestHandler.instance().saveFsXml(rawvideoUri, fsxml, "PUT", true);
		}
		
		
		/*
		String screenProperties = "<fsxml>" +
									"<properties>" +
										"<size>320x240</size>" +
										"<interval>1</interval>" +
										"<redo>true</redo>" +
									"</properties>" +
								"</fsxml>";
							
		String screensUri = "/domain/"+domain+"/user/"+userid+"/video/"+at5id+"/screens/1/properties";
		if(!FSXMLRequestHandler.instance().saveFsXml(screensUri, screenProperties, "PUT", true)) {
			throw new ActionException("Screens properties could not be set");
		}
		*/
	}
	
	/**
	 * Ingest script, from Jaap
	 * @param domain
	 * @param xml
	 */
	private boolean ingestOriginal(String domain, String xml) {
		String confUri = "/domain/" + domain + "/config/ingest";
		logger.debug("CONF URI: " + confUri);
		Document doc = FSXMLRequestHandler.instance().getNodeProperties(confUri, false);
		if (doc != null) {
			logger.debug("****************************************************\n" + doc.asXML());
			logger.debug("****************************************************\n");
			FileIngestConfig conf = FSXMLParser.getFileIngestConfigFromXml(doc.asXML());
			if (conf == null) {
				logger.error("ERROR IN CONFIG");
			}
			logger.debug("----------------------------------------------------\n" + xml);
			logger.debug("----------------------------------------------------\n");
			Document input = XMLHelper.asDocument(xml);
			if(input == null){
				//TODO error
				logger.error("ERROR IN REQUEST");
			} else {
				logger.debug("GOING TO PROCESS INGEST");
				IngestInputData iid = SimpleIngestHandler.instance().getInputVariables(input);
				if(iid == null){
					logger.error("THE INPUT VARIABLES WERE INVALID");
					return false;
				}		
				String response = SimpleIngestHandler.instance().ingestVideo(conf, iid);
				logger.debug("ingest video response: " + response);
				
				// check for error
				return FSXMLParser.getErrorMessageFromXml(response)==null;
			}
		}
		return false;
	}
	
	/**
	 * Set an error message
	 * @param message
	 * @param details
	 */
	public void setError(String message, String details) {
		// make xml
		String errorXml = FSXMLBuilder.getFSXMLErrorMessage("500",message,details,"http://blackboots.noterik.com/team");
		
		// set error message
		String errorUri = eventUri + "/error/1";
		FSXMLRequestHandler.instance().saveFsXml(errorUri, errorXml, "PUT", false);
		
		doFailedCallback();
	}
	
	/**
	 * remove error message
	 */
	public void removeError() {
		String errorUri = eventUri + "/error/1";
		FSXMLRequestHandler.instance().deleteNodeProperties(errorUri, false);
	}
	
	/**
	 * Set a status message
	 * @param message
	 */
	public void setStatus(String message) {
		// make xml
		String statusXml = FSXMLBuilder.getFSXMLStatusMessage(message, "", "http://blackboots.noterik.com/team");
			
		// set status message
		String statusUri = eventUri + "/status/1";
		FSXMLRequestHandler.instance().saveFsXml(statusUri, statusXml, "PUT", false);
	}
	
	/**
	 * remove status message
	 */
	public void removeStatus() {
		String statusUri = eventUri + "/status/1";
		FSXMLRequestHandler.instance().deleteNodeProperties(statusUri, false);
	}
	
	public void doFailedCallback() {
		doStatusCallback("failed");
	}
	
	public void doStatusCallback(String status) {
		String callback;
		if(domain.equals("at5.devel")) {
			callback = "http://api.at5.nl/ping/noterikbeta?id={id}&status={status}";
		} else {
			callback = "http://vdx.at5.net/ping/noterik?id={id}&status={status}";
		}
		String url = callback.replace("{id}", at5id).replace("{status}", status);
		logger.debug("doing status callback for: " + at5id + ", to: "+url);
		String response = HttpHelper.sendRequest("GET", url, null, null);
		
		logger.debug("response from at5 callback was: "+response);
		try {
			Document doc = DocumentHelper.parseText(response);
			String code = doc.valueOf("//code");
			if(code==null || !code.equals(RESPONSE_OK)) {
				logger.error("Callback was unsuccesful, code: "+code+", response: "+response);
			}
		} catch(Exception e) {
			logger.error("response from at5 callback was: "+response, e);
		}
	}
}
