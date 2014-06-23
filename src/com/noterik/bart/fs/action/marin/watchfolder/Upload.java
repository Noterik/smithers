package com.noterik.bart.fs.action.marin.watchfolder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;

public class Upload extends ActionAdapter {
	private static final long serialVersionUID = 1L;

	/** the UploadAction's log4j logger */
	private static final Logger LOG = Logger.getLogger(Upload.class);
	
	private Config conf;
	private static String rawRootDir;
	private static String xmlUploadInputDir;
	private static String xmlUploadQueueDir;
	private static String xmlUploadFailedDir;
	private static String xmlOldDir;
				
	/**
	 * Executes this action
	 */
	public String run() {
		//get config
		conf = new Config();

		LOG.info("---------------------------");
		LOG.info("Automated ingest triggered");
		LOG.info("---------------------------");
		
		rawRootDir = conf.rawRootDir;
		xmlUploadInputDir = conf.xmlUploadInputDir;
		xmlUploadQueueDir = conf.xmlUploadQueueDir;
		xmlUploadFailedDir = conf.xmlUploadFailedDir;
		xmlOldDir = conf.xmlOldDir;	
		
		LOG.info("xml queue dir: "+xmlUploadQueueDir);
		File xmlUploadQueue = new File(xmlUploadQueueDir);
		if (!xmlUploadQueue.exists()) {
			makeDirectory(xmlUploadQueue);
		}
		
		LOG.info("xml failed dir: "+xmlUploadFailedDir);
		File xmlUploadFailed = new File(xmlUploadFailedDir);
		if (!xmlUploadFailed.exists()) {
			makeDirectory(xmlUploadFailed);
		}
		
		LOG.info("xml old dir: "+xmlOldDir);
		File xmlOld = new File(xmlOldDir);
		if (!xmlOld.exists()) {
			makeDirectory(xmlOld);
		}

		LOG.info("Fetching content from the xml queue");
		int totalFiles;
		if ((totalFiles = getDirectoryContent(xmlUploadQueueDir).length) > 0) {
			LOG.info("queue has "+totalFiles+" files still in the queue, exiting");
			return "";
		} else {
			LOG.info("queue is empty, proceeding with enqueueing");
		}
		
		//Get all the XML files.
	   	LOG.info("Status: Fetching valid xml files");  
		
	   	File[] fileList = getDirectoryContent(xmlUploadInputDir);
	   	totalFiles = fileList.length;
	   	LOG.info("Status: Found "+totalFiles+" files to be processed");
	   	
	   	for (File xmlFile : fileList) {
	   		if (xmlFile.exists()) {
	   			LOG.info("Status: XML file exists: "+xmlFile.getPath());
	   			
	   			// touch file (for moving failed jobs)
	   			xmlFile.setLastModified(new Date().getTime());
	   			
	   			Document doc = getDocument(xmlFile);
	   			// error => move file 
	   			if (doc == null) {
	   				// move file
	   				xmlFile.renameTo(new File(xmlUploadFailedDir+xmlFile.getName()+"_FAILED_XML"));
	   				continue;
	   			}
	   			
	   			//Create new asset
	   			int success = insertItem(xmlFile, doc);
		
				if(success != -1) {
			        //Move the input file to the xml old folder folder
					xmlFile.renameTo(new File(xmlOldDir+xmlFile.getName()));
				} else {
					LOG.info("Successfully processed xml item from xml upload input dir");
				}
	   		} else {
	   			LOG.error("ERROR: file does not exists "+xmlFile.getPath());
	   		}
	   	}
	   	LOG.info("Finished handling xml upload input dir");
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
	
	private int insertItem(File xml, Document doc) {
		String projectnr = conf.getProperty(doc, "/Project/ProjectNumber");
		//TODO: how is the projectSubnumer going to be handled?
		String projectSubnumber = conf.getProperty(doc, "/Project/ProjectSubnumber");
		String projectLevel = projectnr.substring(0,2)+"xxx";		

		//check version, if not correct version
		//String xmlVersion = conf.getProperty(doc, "/Project/version??");
		//if (!versionCheck(xmlVersion)) {
		//	LOG.error("Could not move file "+filenameCorrected+" "+location);
		//	xml.renameTo(new File(xmlUploadFailedDir+xml.getName()+"_VIDEO_NOT_FOUND"));
		//	return -1;
		//}
		
		//Handling files separately
		List<Node> files = conf.getChilds(doc, "/Project/Files/File");
		String target = "";
		String mediaType = "";
		
		//first check if all files are available, only then start processing, otherwise retry later
		int fileCounter = 1;
		HashSet<Integer> missingFiles = new HashSet<Integer>();
		
		for (Node file : files) {
			String filename = conf.getProperty(file, "FileName");			
			//Correct filename	
			String filenameCorrected = filename.replaceAll("\\\\", "/");
			
			File mediaFile = new File(filenameCorrected);   
			if (!mediaFile.exists()) {
				LOG.error("The folowing media file was not found: "+filenameCorrected);
				
				missingFiles.add(new Integer(fileCounter));	
			}
			fileCounter++;
		}
		
		//Append to filename the position of the missing file(s)
		if (missingFiles.size() > 0) {
			String missingString = "";
			for (int i = 0; i < missingFiles.size(); i++) {
				missingString += i+"_";
			}
			
			xml.renameTo(new File(xmlUploadFailedDir+xml.getName()+"_FILE_"+missingString+"NOT_FOUND"));
			return -1;
		}		
		
		LOG.info("All mediafiles exist");
		//Generate default project structure, creates all sub folders that don't exist yet
		LOG.info("About to make project structure");
		conf.createProjectStructure(projectLevel, projectnr);
		
		for (Node file : files) {
			file = file.detach();
			String filename = conf.getProperty(file, "FileName");
			
			//Linux compatible basepath, this needs to be done to prevent the basename function from erroring out.
			String filepath = filename.replace("\\", "/");
			int a = filepath.lastIndexOf("/")+1;
			String newFilename = filename.substring(a);
			LOG.info("Status: Refactored filename: "+newFilename);
			String baseFilename = newFilename.substring(0, newFilename.lastIndexOf("."));
			String extension = newFilename.substring(newFilename.lastIndexOf(".")+1);
			
			target = conf.getProperty(file, "Target");
			target = Character.toUpperCase(target.charAt(0)) + target.substring(1); //Uppercase 1 letter
			String subTarget = null;
			if (target.equals("Export")) {
				subTarget = "transcode";
			}
			mediaType = conf.getProperty(file, "FileType");

			String location = subTarget == null ? conf.USER_ROOT+"/video/"+projectnr+":"+target+":"+mediaType+":"+baseFilename : conf.USER_ROOT+"/video/"+projectnr+":"+target+":"+mediaType+":"+subTarget+":"+baseFilename;	
			Document d = FSXMLRequestHandler.instance().getNodeProperties(location, false);
			
			if (d != null && d.selectSingleNode("/error") == null) {
				LOG.error("Existing assets found for: "+ xml);
				// move file
				xml.renameTo(new File(xmlUploadFailedDir+xml.getName()+"_FAILED_EXISTS"));
				updateIndex("/"+projectLevel+"/"+projectnr);
				return -1;
			}
			
			//Move media file to correct directory			
			String filenameCorrected = filename.replaceAll("\\\\", "/");
			location = subTarget == null ? rawRootDir+projectLevel+"/"+projectnr+"/"+target+"/"+mediaType+"/"+newFilename : rawRootDir+projectLevel+"/"+projectnr+"/"+target+"/"+mediaType+"/"+subTarget+"/"+newFilename;
			LOG.info("About to move file "+filenameCorrected+" to "+location);
            File mediaFile = new File(filenameCorrected);        
            boolean moveSuccess = mediaFile.renameTo(new File(location));
			if (!moveSuccess) {
				LOG.error("Could not move file "+filenameCorrected+" "+location);
				xml.renameTo(new File(xmlUploadFailedDir+xml.getName()+"_FILE_NOT_FOUND"));
				updateIndex("/"+projectLevel+"/"+projectnr);
				return -1;
			}
            
            LOG.info("About to create video in fs");
            String response;
            if (subTarget == null) {
            	response = conf.createVideo(doc, "/"+projectLevel+"/"+projectnr+"/"+target+"/"+mediaType+"/", baseFilename, extension, projectnr, mediaType, target, null);
            } else {
            	response = conf.createVideo(doc, "/"+projectLevel+"/"+projectnr+"/"+target+"/"+mediaType+"/"+subTarget+"/", baseFilename, extension, projectnr, mediaType, target, subTarget);
            }
            
            Document videoResponse = null;
			
			try {
				videoResponse = DocumentHelper.parseText(response);
			} catch (DocumentException e) {
				LOG.error("Could not create video (documentexception) -"+response);
				updateIndex("/"+projectLevel+"/"+projectnr);
				return -1;
			}
			
			if (videoResponse.selectSingleNode("error") != null) {
				LOG.error("Could not create video - "+response);
				updateIndex("/"+projectLevel+"/"+projectnr);
				return -1;
			}
			String videoUri = videoResponse.selectSingleNode("status/properties/uri") == null ? "" : videoResponse.selectSingleNode("status/properties/uri").getText();
			
			if (videoUri.equals("")) {
				LOG.error("Video not correctly created");
				updateIndex("/"+projectLevel+"/"+projectnr);
				return -1;
			}
			
			//check transcodingAction
			String transcodingAction = conf.getProperty(file, "TfProfile");
			LOG.info("TF profile = "+transcodingAction);
			if (transcodingAction != "" && mediaType.equals("video")) {
				conf.transcodeVideo(videoUri, transcodingAction, "/"+projectLevel+"/"+projectnr+"/Export/"+mediaType+"/transcode/"+newFilename, projectnr, newFilename);
			}			
		}	
	
		// Save the xml file to file system
        //// Move original file to OLD dir
        
        // Save the new XML file (same as original to the raw folder with _Mx)
		String xmlBase = xml.getName();		
        String rawFilename = xmlBase.substring(0, xmlBase.lastIndexOf("."));
        rawFilename += ".xml";       
        
        LOG.info("About to move xml file to project directories");
        // move xml to correct raw directory
        if (mediaType.equals("")) {
        	xml.renameTo(new File(xmlUploadFailedDir+xml.getName()+"_NO_FILES_FOUND"));
			updateIndex("/"+projectLevel+"/"+projectnr);
			return -1;
        } else {
        	File dest = new File(rawRootDir+projectLevel+"/"+projectnr+"/Raw/"+mediaType+"/"+rawFilename);        	
        	xml.renameTo(dest);     
        }
        
        updateIndex("/"+projectLevel+"/"+projectnr);
        
		return 0;
	}
	
	private void makeDirectory(File directory) {
		if (!directory.exists()) {
			directory.mkdirs();
			directory.setExecutable(true);
			directory.setReadable(true);
			directory.setWritable(true);
		}
	}
	
	private void updateIndex(String project) {
		String cmd = "php /springfield/mediamix/cmd.php -u=noterik -p=noterik12 -a=index -r="+conf.repositoryId+" --dir=";

		Process proc = null;
		StringBuffer response = new StringBuffer();
   		try {
   			LOG.info("about to execute: "+cmd+project);
   			proc = Runtime.getRuntime().exec(cmd+project);
   			BufferedReader br = null; 
	        String currentInput = null;
            try {
            	br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
	            while ( (currentInput = br.readLine()) != null) {
	            	response.append(currentInput);
	            }
            }
            catch (Exception e) {
            	LOG.error("Error during reading inputStream");
            }
            finally { 
            	LOG.info("Updated index for "+project+" response: "+response.toString());
            	if (br != null)
	            	try {
	            			br.close(); 
	            		}
	            	catch (Exception e) {
	            		//silently
	            	}
            }
	        
			int val = proc.waitFor();
		} catch (IOException e) {
			// thrown by process getRuntime()
			e.printStackTrace();
		} catch (InterruptedException e) {
			// thrown by proc.waitFor()
			e.printStackTrace();
		}
		finally {
			if (proc != null) {
				try {
					proc.getErrorStream().close();
					proc.getInputStream().close();
					proc.getOutputStream().close();
				}
				catch (Exception e) {
					//silently closing
				}		
			}
		}
	}
	
	private void copyFile(File source, File dest) {
		InputStream is = null;
		OutputStream os = null;
		try {
			is = new FileInputStream(source);
			os = new FileOutputStream(dest);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = is.read(buffer)) > 0) {
				os.write(buffer, 0, length);
			}
			is.close();
			os.close();
		} catch (IOException e) {
			
		}
	}
	
	//Check if the XML version is at least the mimimal configured version
	private boolean versionCheck(String xmlVersion) {
		String minimalSupportedVersion = conf.minXmlVersion;
		
		String[] minimalVersionArray = minimalSupportedVersion.split(".");
		String[] xmlVersionArray = xmlVersion.split(".");
		
		for (int i = 0; i < minimalVersionArray.length; i++) {
			
			//version vs minimalversion - 1.10 vs 1.9.1
			if (xmlVersionArray.length == i) {
				int m = Integer.parseInt(minimalVersionArray[i-1]);
				int c = Integer.parseInt(xmlVersionArray[i-1]);
				if (c > m) {
					return true;
				}
				return false;
			}
			
			int m = Integer.parseInt(minimalVersionArray[i]);
			int c =Integer.parseInt(xmlVersionArray[i]);
			
			//see if this digit from the current version is smaller then mimimal version
			if (c < m) {
				return false;
			}
		}
		return true;
	}
}
