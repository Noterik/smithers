package com.noterik.bart.fs.action.marin.watchfolder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;

public class Update extends ActionAdapter {
	private static final long serialVersionUID = 1L;

	/** the UpdateAction's log4j logger */
	private static final Logger LOG = Logger.getLogger(Update.class);
	
	private Config conf;
	private static String rawRootDir;
	private static String xmlUpdateInputDir;
	private static String xmlUpdateQueueDir;
	private static String xmlUpdateOutputDir;
	private static String xmlOldDir;
	
	/**
	 * Executes this action
	 */
	public String run() {
		//get config
		conf = new Config();
		
		LOG.info("---------------------------");
	 	LOG.info("Automated update triggered");
	 	LOG.info("---------------------------");
		
	 	rawRootDir = conf.rawRootDir;
	 	xmlUpdateInputDir = conf.xmlUpdateInputDir;
	 	xmlUpdateQueueDir = conf.xmlUpdateQueueDir;
	 	xmlUpdateOutputDir = conf.xmlUpdateOutputDir;
	 	xmlOldDir = conf.xmlOldDir;
	 	
	 	//Get all the XML files for input dir and move them to queue dir
	   	LOG.info("Status: Fetching valid xml files.");  	
	   	int totalFiles;	   	
		File[] fileList = getDirectoryContent(xmlUpdateInputDir);
	   	totalFiles = fileList.length;
	   	LOG.info("Status: Found "+totalFiles+" files to be processed in the input dir.");
		for (File xmlFile : fileList) {
	   		if (xmlFile.exists()) {
	   			xmlFile.renameTo(new File(xmlUpdateQueueDir+xmlFile.getName()));
	   			xmlFile.setLastModified(new Date().getTime());
	   		}
		}
		
		// get queue
		fileList = getDirectoryContent(xmlUpdateQueueDir);
	   	totalFiles = fileList.length;
	   	LOG.info("Status: Found "+totalFiles+" files to be processed in the queue dir.");
	   	
	   	// sort file alphabetically, in order to ensure that successive updates (0A, 0B, 0C, etc) are executed successively
	    Arrays.sort(fileList);
	   	
	   	//Parse the xml files.
	   	for (File xmlFile : fileList) {
	   		if (xmlFile.exists()) {
	   			LOG.info("Status: XML file exists: "+xmlFile.getPath());
	   			
	   			Document doc = getDocument(xmlFile);
	   			// error => move file 
	   			if (doc == null) {
	   				LOG.error("error parsing xml file: "+xmlFile.getPath());
	   				
	   				// move file
	   				xmlFile.renameTo(new File(xmlUpdateOutputDir+xmlFile.getName()+"_FAILED_XML"));	     			
	     			continue;
	   			}
	   			//TODO: handle response code from updateItem
				//update asset
		        updateItem(xmlFile, doc);	   			
	   		} else {
	   			LOG.error("ERROR: Parsing the xml file failed: "+xmlFile.getPath());
	   		}
	   	}	   	
		return "";
	}
	
	private static File[] getDirectoryContent(String directory) {
		File d = new File(directory);
		File[] array = d.listFiles(new FilenameFilter() {
					
			public boolean accept(File dir, String name) {
				if(name.equals(".") || name.equals("..")) {
					return false;
				}
				return true;
			}
		});
		return array;
	}
	
	private static Document getDocument(File file) {
		Document document =null;
		SAXReader reader = new SAXReader();
		try {
			document = reader.read(file);
		} catch (DocumentException e) {
			LOG.error("error parsing xml file: "+file.getPath());
		}
		return document;
	}
	
	private int updateItem(File xml, Document doc) {
		/*String projectnr = conf.getProperty(doc, "/project/projectNumber");
		//TODO: how is the projectSubnumer going to be handled?
		String projectSubnumber = conf.getProperty(doc, "/project/projectSubnumber");
		String projectLevel = projectnr.substring(0,2)+"xxx";

		//Handling files seperately
		List<Node> files = conf.getChilds(doc, "/project/file");
		
		for (Node file : files) {
			file = file.detach();
			String filename = conf.getProperty(file, "filename");
			
			//Linux compatible basepath, this needs to be done to prevent the basename function from erroring out.
   			String filepath = filename.replace("\\\\", "/");
   			int a = filepath.lastIndexOf("/")+1;
   			String newFilename = filename.substring(a);
   			LOG.info("Status: Refactored filename: "+newFilename);
   			String baseFilename = newFilename.substring(0, newFilename.lastIndexOf("."));
			String extension = newFilename.substring(newFilename.lastIndexOf(".")+1);
			
			Document doc = FSXMLRequestHandler.instance().getNodeProperties(conf.USER_ROOT+"/video/"+baseFilename, false);
		
		if (doc == null) {
			LOG.error("No assets found for: "+ xml.getPath());
			
			// move file if limit is exceeded
			if(fileExceedsTimeLimit(xml, conf.xmlMaxExecutionTime)) {
				LOG.info("Moving: "+ xml.getPath());
				xml.renameTo(new File(xmlUpdateOutputDir+xml.getName()+"_FAILED_NOT_FOUND"));
			}			
			return -1;
		} 
		
		//get projectnumber from this item
		String projectNumber = doc.selectSingleNode("//properties/projectnumber") == null ? "" : doc.selectSingleNode("//properties/projectnumber").getText();
		
		if (projectNumber.equals("")) {
			LOG.error("No projectnumber found for video "+videoFilename);
			return -1;	
		}*/
		//TODO: check if the file is in a queue for manual processing?
		
		/**
		 * Other option, use modified xml name
		 */
		/*String updateVersion = "";
		String xmlBase = xml.getName();
		String newFileBase = xmlBase.substring(0, xmlBase.lastIndexOf("."));  
		if (newFileBase.substring(newFileBase.length()-3,2).equals("_0")) { //Not to remove the dv from the file, only the versioning
			newFileBase = newFileBase.substring(0, newFileBase.lastIndexOf("_"));
			updateVersion = newFileBase.substring(newFileBase.lastIndexOf("_"));
		}
		if (newFileBase.substring(newFileBase.length()-3,2).equals("_M")) { //Not to remove the dv from the file, only the versioning
			newFileBase = newFileBase.substring(0, newFileBase.lastIndexOf("_"));
			updateVersion = newFileBase.substring(newFileBase.lastIndexOf("_"));
		}
		LOG.info("update version: "+updateVersion);
		
		// move original file
		String filenameVideo = properties.get("filenameVideo");
		String extension = filenameVideo.substring(filenameVideo.lastIndexOf("."));
		/*
		String dir = projectNumber+"/Raw/";
		String fin = rawRootDir+dir+filenameVideo;
		
		String fout = rawRootDir+dir+newFileBase+extension;

		LOG.info("renaming file: "+fin+" to "+fout);
		LOG.info("dir: "+dir);
		LOG.info("extension: "+extension);
		
		// move
		new File(fin).renameTo(new File(fout));
				
		// move video in FS
		String response = conf.moveItem(conf.USER_ROOT+"/video/"+videoFilename, conf.USER_ROOT+"/video/"+newFileBase);
		
		Document videoResponse = null;
		try {
			videoResponse = DocumentHelper.parseText(response);
		} catch (DocumentException e) {
			LOG.error("Could not move video (documentexception) -"+response);
			return -1;
		}
		if (videoResponse.selectSingleNode("error") != null) {
			LOG.error("Could not move video - "+response);
			return -1;
		}
		String videoUri = videoResponse.selectSingleNode("status/properties/uri") == null ? "" : videoResponse.selectSingleNode("status/properties/uri").getText();
		
		if (videoUri.equals("")) {
			LOG.error("Video not correctly moved");
			return -1;
		}
		
		//update refers in FS
		boolean success = conf.updateRefers(conf.USER_ROOT+"/video/"+videoFilename, conf.USER_ROOT+"/video/"+newFileBase);
		if (!success) {
			LOG.error("Could not update refers for video"+videoFilename);
			return -1;
		}	*/	
		
		// update filename
		/*String oldVideoFile = filenameVideo;
		properties.put("filenameVideo", newFileBase+extension);		
		
		// create new xml
		String xmlString = formatToXML(properties);
	
		int maxFollowNumber = ((int) 'A')-1;
		boolean addDV = false;
		//Catch the xml file that needs to be moved to the old folder based on the filenameVideo in the input xml
		String start = oldVideoFile.substring(0, oldVideoFile.lastIndexOf("."));
		if (start.substring(start.length()-3,2).equals("_0")) {
			start = start.substring(0, start.lastIndexOf("_0"));
		}
		if (start.substring(start.length()-3).equals("_dv")) {
			addDV = true;
			start = start.substring(0, start.lastIndexOf("_dv"));
		}
		
		String origRawFile = start;
		File d = new File(xmlOldDir);
		LOG.info("Path: "+d.getPath());
		
		File[] entries = d.listFiles();
		for (File file : entries) {
			String filename = file.getName();			
			//TODO: definitly not the best for many items
			if (filename.indexOf(start) == 0) {
				if (filename.lastIndexOf(".xml") == filename.length()-".xml".length()) { //endswith
					LOG.info("fits description: "+filename);
					
					// check if the old file is a modified version or if it is the original
					String modified = filename.substring(filename.length()-7,2);
					LOG.info("modified: "+modified);
					if (modified.equals("_M")) {
						char tmp = filename.substring(filename.length()-5,1).charAt(0);
						int tmpFollowNumber = (int) tmp;
						LOG.info("tmpFollowNumber: "+tmpFollowNumber);
						if (tmpFollowNumber>maxFollowNumber) {
							maxFollowNumber = tmpFollowNumber;
						}
					}
				}
			}
		}

		char followCharacter = ((char) (maxFollowNumber+1));
		LOG.info("follow character is: "+followCharacter);
		String oldXmlFile = origRawFile;
		if(addDV) {
			oldXmlFile += "_dv";
			origRawFile += "_dv";
		}
		oldXmlFile += "_M"+followCharacter+".xml";
		LOG.info("Old XML file: "+oldXmlFile);
		String mediaType = properties.get("fileType");
		new File(rawRootDir+projectNumber+"/Raw/"+mediaType+"/"+origRawFile+".xml").renameTo(new File(xmlOldDir+oldXmlFile));
		
		//TODO: handle both qualities or do they have two xml files?
		// change filename, in order to store correct MMD path for MARIN people
		properties.put("filenameVideo", "\\\\mmd\\MovieMix\\"+projectNumber+"\\Raw\\"+mediaType+"\\"+newFileBase+".mp4"); //TODO: ugly and cannot work
	
		xmlString = formatToXML(properties);
		LOG.info("xml correct filename: "+xmlString);
		
		String rawXmlFilename = xml.getName();
		rawXmlFilename = rawXmlFilename.substring(0,rawXmlFilename.lastIndexOf("."));
		if (rawXmlFilename.substring(rawXmlFilename.length()-3, 2).equals("_0")) {
			rawXmlFilename = rawXmlFilename.substring(0, rawXmlFilename.lastIndexOf("_0"));
		}
		if (rawXmlFilename.substring(rawXmlFilename.length()-3).equals("_dv")) {
			rawXmlFilename = rawXmlFilename.substring(0, rawXmlFilename.lastIndexOf("_dv"));
		}

		//Check if needed to add the velocity to the filename
		String[] facilities = new String[] {"DWB", "DT", "VT"};
		if (Arrays.asList(facilities).contains(properties.get("facility")) && xml.getName().indexOf("_velocity") > -1 && properties.containsKey("vship") && !properties.get("vship").equals("")) {
			String replaceString = properties.get("facility")+properties.get("projectNumber")+"_"+properties.get("testNumber")+"_r"+properties.get("runNumber")+"_vc"+properties.get("videoChannel");
			rawXmlFilename = rawXmlFilename.replace(replaceString, "");
			rawXmlFilename = rawXmlFilename.substring(0, rawXmlFilename.lastIndexOf("_"));
			if (!rawXmlFilename.equals("")) {
				rawXmlFilename = "_"+rawXmlFilename;
			}
			String vship = properties.get("vship").replaceAll(" ", "_");
			rawXmlFilename = properties.get("facility")+properties.get("projectNumber")+"_"+properties.get("testNumber")+"_r"+properties.get("runNumber")+"_velocity"+vship+"_vc"+properties.get("videoChannel")+rawXmlFilename;
		}
		
		if (addDV) {
			rawXmlFilename += "_dv";
		}
		rawXmlFilename += ".xml";

		// save the updated xml to the RAW folder
		try {
			File file = new File(rawRootDir+projectNumber+"/Raw/"+mediaType+"/"+rawXmlFilename);
			FileOutputStream os = new FileOutputStream(file);
			OutputStreamWriter osw = new OutputStreamWriter(os);
			Writer w = new BufferedWriter(osw);
			w.write(xmlString);
			w.close();
		} catch (IOException e) {
			LOG.error("Could not write to xml file "+rawXmlFilename);
		}
	
		// remove file
		xml.delete();		
		*/
		return 0;
	}
	
	private boolean fileExceedsTimeLimit(File xml, long maxExecutionTime) {
		Long currentTime = new Date().getTime();
		Long fileTime = xml.lastModified();
		Long lifeTime = currentTime - fileTime;
		
		if (lifeTime > maxExecutionTime) {
			return true;
		}
		return false;
	}
	
	private String formatToXML(HashMap<String, String> properties) {
		Document doc = DocumentHelper.createDocument();
		Element props = doc.addElement("project");
		
		for (Iterator<Map.Entry<String, String>> it = properties.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<String, String> p = it.next();
			props.addElement(p.getKey(), p.getValue());
		}	
		return doc.asXML();
	}
}
