package com.noterik.bart.fs.action.marin.watchfolder;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;

public class Delete extends ActionAdapter {
	private static final long serialVersionUID = 1L;

	/** the DeleteAction's log4j logger */
	private static final Logger LOG = Logger.getLogger(Delete.class);
	
	private Config conf;
	private static String rawRootDir;
	private static String xmlDeleteInputDir;
 	private static String xmlDeleteQueueDir;
 	private static String xmlDeleteOutputDir;
	
	/**
	 * Executes this action
	 */
	public String run() {
		//get config
		conf = new Config();
		
		LOG.info("---------------------------");
		LOG.info("Automated delete triggered");
	 	LOG.info("---------------------------");
		
	 	xmlDeleteInputDir = conf.xmlDeleteInputDir;
	 	xmlDeleteQueueDir = conf.xmlDeleteQueueDir;
	 	xmlDeleteOutputDir = conf.xmlDeleteOutputDir;
	 	
	 	LOG.info("xml delete input dir: "+xmlDeleteInputDir);
	 	LOG.info("xml delete queue dir: "+xmlDeleteQueueDir);
	 	LOG.info("xml delete output dir: "+xmlDeleteOutputDir);
	 	
	 	//TODO: needed this override boolean?
	 	// override
	 	boolean override = true;

	   	//Get all the XML files for input dir and move them to queue dir
	   	LOG.info("Status: Fetching valid xml files.");  	
		int totalFiles;	   	
		File[] fileList = getDirectoryContent(xmlDeleteInputDir);
	   	totalFiles = fileList.length;
		LOG.info("Status: Found "+totalFiles+" files to be processed in the input dir.");
		for (File xmlFile : fileList) {
	   		if (xmlFile.exists()) {
	   			xmlFile.renameTo(new File(xmlDeleteQueueDir+xmlFile.getName()));
	   			xmlFile.setLastModified(new Date().getTime());
	   		}
		}
		
		// get queue
		fileList = getDirectoryContent(xmlDeleteQueueDir);
		totalFiles = fileList.length;
		LOG.info("Status: Found "+totalFiles+" files to be processed in the queue dir.");
		
		//Parse the xml files.
	   	for (File xmlFile : fileList) {
	   		if (xmlFile.exists()) {
	   			LOG.info("Status: XML file exists: "+xmlFile.getPath());
	   			
	   			Document doc = getDocument(xmlFile);		
	   			// error => move file 
	   			if (doc == null) {
	   				LOG.error("error parsing xml file: "+xmlFile.getPath());
	   				
	   				// move file
	   				xmlFile.renameTo(new File(xmlDeleteOutputDir+xmlFile.getName()+"_FAILED_XML"));	     			
	     			continue;
	   			}
	   			
	   			//put all properties in a map
	   			HashMap<String, String> properties = new HashMap<String, String>();	   			
	   			List<Node> items = doc.selectNodes("/project/*");
	   			for (Node child : items) {
	   				properties.put(child.getName(), child.getText());
	   			}
	   			
	   			//Linux compatible basepath, this needs to be done to prevent the basename function from erroring out.
	   			String filenameVideo = properties.get("filenameVideo");
	   			String file = filenameVideo.replace("\\", "/");
	   			int a = file.lastIndexOf("/")+1;
	   			filenameVideo = filenameVideo.substring(a);
	   			LOG.info("Status: Refactored filename: "+filenameVideo);
	   			properties.put("newFilename", filenameVideo);

	   			//TODO: handle response from deleteItem
	   			//delete asset
		        deleteItem(xmlFile, properties, override);	
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
	
	private int deleteItem(File xml, HashMap<String, String> properties, boolean override) {
		String projectNumber = properties.get("projectNumber");
		String projectLevel = projectNumber.substring(0,3)+"xxx";
		String videoFilename = properties.get("newFilename").substring(0, properties.get("newFilename").lastIndexOf("."));
		
		Document doc = FSXMLRequestHandler.instance().getNodeProperties(conf.USER_ROOT+"/video/"+videoFilename, false);
		
		if (doc == null) {
			LOG.error("No assets found for: "+ xml.getPath());
			
			// move file if limit is exceeded
			if(fileExceedsTimeLimit(xml, conf.xmlMaxExecutionTime)) {
				LOG.info("Moving: "+ xml.getPath());
				xml.renameTo(new File(xmlDeleteOutputDir+xml.getName()+"_FAILED_NOT_FOUND"));
			}			
			return -1;
		} 
		//TODO: check if the file is in queue for manual processing
		
		// delete files
		//TODO: do high and low quality both get a different xml file?
		String mediaType = properties.get("fileType");
		String highQualityVideo = rawRootDir+projectLevel+"/"+projectNumber+"/Raw/"+mediaType+"/"+videoFilename;
		String lowQualityVideo = rawRootDir+projectLevel+"/"+projectNumber+"/Raw/"+mediaType+"/"+videoFilename;		
		String xmlFile = rawRootDir+projectLevel+"/"+projectNumber+"/Raw/"+mediaType+"/"+videoFilename.substring(0, videoFilename.lastIndexOf("."))+".xml";
		
		File videoHQ = new File(highQualityVideo);
		File videoLQ = new File(lowQualityVideo);
		File file = new File(xmlFile);
		
		boolean success = videoHQ.delete();
		if (!success) {
			LOG.error("Could not remove video "+videoHQ.getName());
		}
		success = videoLQ.delete();
		if (!success) {
			LOG.error("Could not remove video "+videoLQ.getName());
		}
		success = file.delete();
		if (!success) {
			LOG.error("Could not remove xml "+file.getName());
		}
		
		//delete video from FS
		String response = conf.deleteNode(conf.USER_ROOT+"/video/"+videoFilename);

		// move file
		xml.renameTo(new File(xmlDeleteOutputDir+xml.getName()));
		
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
	
	private int getNumberOfChilds(String node) {
		Document doc = FSXMLRequestHandler.instance().getNodeProperties(node, false);
		
		if (doc != null) {
			String results = doc.selectSingleNode("//properties/totalResultsAvailable") == null ? "0" : doc.selectSingleNode("//properties/totalResultsAvailable").getText();
			return Integer.parseInt(results);
		} else {
			return 0;
		}
		
	}
}
