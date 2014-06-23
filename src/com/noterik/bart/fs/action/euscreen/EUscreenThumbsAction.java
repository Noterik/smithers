package com.noterik.bart.fs.action.euscreen;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.GlobalConfig;
import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.HttpHelper;
import com.noterik.springfield.tools.fs.URIParser;
import com.noterik.springfield.tools.ftp.FtpHelper;

/**
 * Determines the thumbnail for EUscreen items, and places them on a 
 * specific location of the image server. It uses the characters from the 
 * EUscreen ID, because a linux filesystem can only handle around 32.000 
 * links per iNode.
 * 
 * Location:
 * http://images1.noterik.com/domain/euscreen/{char5}/{char6}/{euscreen-id}.jpg
 */
public class EUscreenThumbsAction extends ActionAdapter {
	/** */
	private static final long serialVersionUID = 1L;
	
	/** the EUscreenThumbsAction's log4j logger */
	private static final Logger LOG = Logger.getLogger(EUscreenThumbsAction.class);
	
	/** search constants */
	private static final String FRINK_SEARCH_URL = "http://frink1.noterik.com/frink/collection";
	private static final String NTUA_SEARCH_URL = "http://euscreen.image.ece.ntua.gr/euscreen/Search";
	private static final String FILENAME_FIELD = "/eus:AdministrativeMetadata/eus:filename_tg";
	private static final String IDENTIFIER_FIELD = "/eus:AdministrativeMetadata/eus:identifier_tg";
	private static final String SCREENS_URL_PREFIX = "http://images1.noterik.com";
	
	/**
	 * Executes this action
	 */
	public String run() {
		// parse event data
		String uri = event.getUri();
		String domain = URIParser.getDomainIdFromUri(uri);
		
		// determine if required data is available
		Document doc = FSXMLRequestHandler.instance().getNodeProperties(uri, false);
		Node pNode = doc.selectSingleNode("//presentationuri");
		Node sNode = doc.selectSingleNode("//presentationscreenshot");
		if(pNode == null || pNode.getText().equals("")) {
			LOG.debug("No presentationuri property found");
			return null;
		}
		String presentationUri = pNode.getText();
		presentationUri = URIParser.removeLastSlash(presentationUri);
		if(sNode == null || sNode.getText().equals("")) {
			LOG.debug("No presentationscreenshot property found");
			return null;
		}
		String screenshotUrl = sNode.getText();
		LOG.debug("presentation uri: "+presentationUri+", screenshot url: "+screenshotUrl);
		
		// check screenshot url
		if(screenshotUrl == null || !screenshotUrl.startsWith(SCREENS_URL_PREFIX)) {
			LOG.error("No screenshot URL found for "+presentationUri);
			return null;
		}
		
		// check frink for filename
		String filename = getFilenameFromFrink(presentationUri);
		if(filename==null) {
			LOG.error("Could not determine filename");
			return null;
		}
		
		// get item properties from the NTUA server
		Properties ntuaItemProperties = getNTUAItemPropertiesByFilename(filename);
		String id = ntuaItemProperties.getProperty(IDENTIFIER_FIELD);
		if(id == null) {
			LOG.error("Could not determine EUscreen ID from NTUA properties");
			return null;
		}
		
		// ftp data
		String server = GlobalConfig.instance().getFtpImage();
		String username = GlobalConfig.instance().getFtpImageUser();
		String password = GlobalConfig.instance().getFtpImagePass();
		String imagePath = GlobalConfig.instance().getFtpImagePath();
		String rFolder, lFolder, lFilename, rFilename;
		boolean success;
		
		// get input file
		String tempFolder = GlobalConfig.instance().getTempDir();
		String screensFile = screenshotUrl.substring(SCREENS_URL_PREFIX.length());
		String localFile = tempFolder + screensFile;
		LOG.debug("screens file: "+screensFile+", local file: "+localFile);
		
		// get file using ftp
		rFolder = imagePath + new File(screensFile).getParent();
		lFolder = new File(localFile).getParent();
		rFilename = new File(screensFile).getName();
		lFilename = new File(localFile).getName();
		success = FtpHelper.commonsGetFile(server, username, password, rFolder, lFolder, rFilename,  lFilename);
		if(!success) {
			LOG.error("Could not get thumb file");
			return null;
		}
		
		// determine output file
		String remoteFile = getScreenshotOutputFile(domain,id);
		LOG.debug("remote converted file: "+remoteFile);
		
		// use FTP to upload screenshot to correct location
		rFolder = imagePath + new File(remoteFile).getParent();
		lFolder = new File(localFile).getParent();
		rFilename = new File(remoteFile).getName();
		lFilename = new File(localFile).getName();
		success = FtpHelper.commonsSendFile(server, username, password, rFolder, lFolder, rFilename, lFilename, true);
		if(!success) {
			LOG.error("Could not put thumb file");
			return null;
		}
		
		return null;
	}
	
	/**
	 * Get all the metadata fields from the NTUA server based on the filename
	 * 
	 * @param filename
	 * @return
	 */
	private static Properties getNTUAItemPropertiesByFilename(String filename) {
		Properties p = new Properties();
		try {
			String ntuaResponse = performSearchRequestNTUA(FILENAME_FIELD, filename);
			Document doc = DocumentHelper.parseText(ntuaResponse);
			Element partialItem = (Element) doc.selectSingleNode("//partialItem");
			for(Iterator<Element> iter = partialItem.elementIterator(); iter.hasNext(); ) {
				Element field = iter.next();
				String name = field.valueOf("@name");
				String value = field.getText();
				p.put(name, value);
			}
		} catch(Exception e) {
			LOG.error("",e);
		}
		return p;
	}
	
	/**
	 * Performs a search request using the supplied ID and returns the response
	 * @param key
	 * @param value
	 * @return
	 */
	private static String performSearchRequestNTUA(String key, String value) {
		// construct url
		String query = field2query(key) + ":" + value;
		String url = null;
		try {
			url = NTUA_SEARCH_URL + "?query=" + URLEncoder.encode(query,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOG.error("",e);
		}
		
		// perform request		
		LOG.debug("Sending request to: "+url);
		String response = HttpHelper.sendRequest("GET", url, null, null); 
		LOG.debug("Got response: "+response);
		return response;
	}
	
	/**
	 * Returns the filename
	 * @param presentationUri
	 * @return
	 */
	private static String getFilenameFromFrink(String presentationUri) {
		try {
			String responseFrink = performSearchRequestFrink("springfield", presentationUri);
			Document doc = DocumentHelper.parseText(responseFrink);
			List<Node> urnList = doc.selectNodes("//urn");
			for(Node node : urnList) {
				String nid = node.valueOf("nid");
				String nss = node.valueOf("nss");
				LOG.debug("nid: "+nid+", nss: "+nss);
				if(nid.startsWith("euscreen")) {
					return nss;
				}
			}
		} catch(Exception e) {
			LOG.error("",e);
		}
		return null;
	}
	
	/**
	 * Performs a search request on frink
	 * @param nid
	 * @param nss
	 * @return
	 */
	private static String performSearchRequestFrink(String nid, String nss) {
		String url = FRINK_SEARCH_URL;
		try {
			url+= "?nid="+nid;
			url +="&nss="+URLEncoder.encode(nss, "UTF-8");
			url += "&format=xml";
		} catch (UnsupportedEncodingException e) {
			LOG.error("",e);
		}
		
		// perform request		
		LOG.debug("Sending request to: "+url);
		String response = HttpHelper.sendRequest("GET", url, null, null); 
		LOG.debug("Got response: "+response);
		return response;
	}
	
	/**
	 * Converts a field to a query parameter
	 * @param field
	 * @return
	 */
	private static String field2query(String field) {
		return field.replace(":", "\\:");
	}
	
	/**
	 * Returns the screenshot output file
	 * Format:
	 * /domain/{domain}/{char5}/{char6}/{euscreen-id}.jpg
	 * 
	 * @param id
	 * @return
	 */
	private static String getScreenshotOutputFile(String domain, String id) {
		return "/domain/"+domain+"/thumbs/"+id.charAt(5) + "/" + id.charAt(6) + "/" + id + ".jpg";
	}	
}
