package com.noterik.bart.fs.action.marin.watchfolder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;

public class Config {
	/** the WatchFolder config log4j logger */
	private static final Logger LOG = Logger.getLogger(Config.class);
			
	public final String USER_ROOT = "/domain/marin/user/admin";
	public final String WATCH_FOLDERS_CONFIG = "/domain/marin/config/watchfolders";
	public final String INDEXATION_CONFIG = "/domain/marin/config/indexation";
	public final String PROFILE_SETTINGS = "/domain/marin/config/ingest/setting/video/profile";
	public String rawRootDir;
	public Long xmlMaxExecutionTime;
	public String xmlUploadInputDir;
	public String xmlUploadQueueDir;
	//public String xmlUploadOutputDir;
	public String xmlUploadFailedDir;
	public String xmlUpdateInputDir;
	public String xmlUpdateQueueDir;
	public String xmlUpdateOutputDir;
	public String xmlDeleteInputDir;
	public String xmlDeleteQueueDir;
	public String xmlDeleteOutputDir;
	public String xmlOldDir;
	public String dataInputDir;
	public String minXmlVersion;
	public String repositoryId;
	
	public Config() {
		Document doc = FSXMLRequestHandler.instance().getNodeProperties(WATCH_FOLDERS_CONFIG, false);
		
		if (doc == null) {
			LOG.error("Could not load watch folder config "+WATCH_FOLDERS_CONFIG);
			return;
		}
		rawRootDir = doc.selectSingleNode("//raw_root_dir") == null ? "" : doc.selectSingleNode("//raw_root_dir").getText();
		xmlMaxExecutionTime = doc.selectSingleNode("//xml_max_execution_time") == null ? 0L : Long.parseLong(doc.selectSingleNode("//xml_max_execution_time").getText());
		xmlUploadInputDir = doc.selectSingleNode("//xml_upload_input_dir") == null ? "" : doc.selectSingleNode("//xml_upload_input_dir").getText();
		xmlUploadQueueDir = doc.selectSingleNode("//xml_upload_queue_dir") == null ? "" : doc.selectSingleNode("//xml_upload_queue_dir").getText();
		//xmlUploadOutputDir = doc.selectSingleNode("//xml_upload_output_dir") == null ? "" : doc.selectSingleNode("//xml_upload_output_dir").getText();
		xmlUploadFailedDir = doc.selectSingleNode("//xml_upload_failed_dir") == null ? "" : doc.selectSingleNode("//xml_upload_failed_dir").getText();
		xmlUpdateInputDir = doc.selectSingleNode("//xml_update_input_dir") == null ? null : doc.selectSingleNode("//xml_update_input_dir").getText();
		xmlUpdateQueueDir = doc.selectSingleNode("//xml_update_queue_dir") == null ? null : doc.selectSingleNode("//xml_update_queue_dir").getText();
		xmlUpdateOutputDir = doc.selectSingleNode("//xml_update_output_dir") == null ? null : doc.selectSingleNode("//xml_update_output_dir").getText();
		xmlDeleteInputDir = doc.selectSingleNode("//xml_delete_input_dir") == null ? null : doc.selectSingleNode("//xml_delete_input_dir").getText();
		xmlDeleteQueueDir = doc.selectSingleNode("//xml_delete_queue_dir") == null ? null : doc.selectSingleNode("//xml_delete_queue_dir").getText();
		xmlDeleteOutputDir = doc.selectSingleNode("//xml_delete_output_dir") == null ? null : doc.selectSingleNode("//xml_delete_output_dir").getText();
		xmlOldDir = doc.selectSingleNode("//xml_old_dir") == null ? "" : doc.selectSingleNode("//xml_old_dir").getText();
		dataInputDir = doc.selectSingleNode("//data_input_dir") == null ? "" : doc.selectSingleNode("//data_input_dir").getText();
		minXmlVersion = doc.selectSingleNode("//min_xml_version") == null ? "" : doc.selectSingleNode("//min_xml_version").getText();
		
		doc = FSXMLRequestHandler.instance().getNodeProperties(INDEXATION_CONFIG, false);
		if (doc == null) {
			LOG.error("Could not load indexation config "+INDEXATION_CONFIG);
			return;
		}
		repositoryId = doc.selectSingleNode("//repository_id") == null ? "" : doc.selectSingleNode("//repository_id").getText();
	}
	
	/**
	 * Create video based on the newFilename property without any extension
	 * 
	 * @param properties
	 * @return
	 */
	public String createVideo(Document doc, String path, String filename, String extension, String projectnr, String mediatype, String target, String subTarget) {		
		String fsxml = "<fsxml><properties/><rawvideo id='1'><properties>"
						+ "<transcoder>apu</transcoder><format>H.264</format><extension>mp4</extension>"
						+ "<reencode>false</reencode><mount>marin</mount><original>true</original>"
						+ "<filename>"+path+filename+"."+extension+"</filename><status>done</status>"
						+ "</properties></rawvideo></fsxml>";
		
		String location = subTarget == null ? USER_ROOT+"/video/"+projectnr+":"+target+":"+mediatype+":"+filename+"/properties" : USER_ROOT+"/video/"+projectnr+":"+target+":"+mediatype+":"+subTarget+":"+filename+"/properties";
		
		LOG.info("Create "+location);
		return FSXMLRequestHandler.instance().handlePUT(location, fsxml);
	}
	
	/**
	 * Delete node from FS
	 * 
	 * @param videoUri
	 * @return
	 */
	public String deleteNode(String node) {
		return FSXMLRequestHandler.instance().handleDELETE(node, null);
	}
	
	/**
	 * Create Presentation with the supplied video based on the test- and runnumber
	 * 
	 * @param properties
	 * @param videoUri
	 * @return
	 */
	public String createPresentation(HashMap<String, String> properties, String videoUri) {
		String fsxml = "<fsxml><properties/><videoplaylist id='1'><properties/>"
				+ "<video referid='"+videoUri+"'/>"
				+ "</videoplaylist></fsxml>";
		
		String testnr = properties.get("testNumber"); 
		String runnr = properties.get("runNumber");
				
		return FSXMLRequestHandler.instance().handlePUT(USER_ROOT+"/presentation/"+testnr+"-"+runnr+"/properties", fsxml);
	}
	
	/**
	 * Add video to existing presentation
	 * 
	 * @param properties
	 * @param presentationUri
	 * @param videoUri
	 * @return
	 */
	public String addVideoToPresentation(String presentationUri, String videoUri) {
		String fsxml = "<fsxml><attributes><referid>"+videoUri+"</referid></attributes></fsxml>";

		return FSXMLRequestHandler.instance().handlePOST(presentationUri+"/videoplaylist/1/video", fsxml);
	}	
	
	/**
	 * Create Collection with the supplied presentation based on the projectnumber
	 * 
	 * @param properties
	 * @param presentationUri
	 * @return
	 */
	public String createCollection(HashMap<String, String> properties, String presentationUri) {
		String testnr = properties.get("testNumber"); 
		String runnr = properties.get("runNumber");
		
		String fsxml = "<fsxml><properties/><presentation id='"+testnr+"-"+runnr+"' referid='"+presentationUri+"'/></fsxml>";
		
		String projectnr = properties.get("projectNumber");
		
		return FSXMLRequestHandler.instance().handlePUT(USER_ROOT+"/collection/"+projectnr+"/properties", fsxml);
	}
	
	/**
	 * Add presentation to existing collection
	 * 
	 * @param properties
	 * @param collectionUri
	 * @param presentationUri
	 * @return
	 */
	public String addPresentationToCollection(HashMap<String, String> properties, String collectionUri, String presentationUri) {
		String fsxml = "<fsxml><attributes><referid>"+presentationUri+"</referid></attributes></fsxml>";
		
		String testnr = properties.get("testNumber"); 
		String runnr = properties.get("runNumber");
		
		return FSXMLRequestHandler.instance().handlePUT(collectionUri+"/presentation/"+testnr+"-"+runnr+"/properties", fsxml);
	}
	
	
	/**
	 * Moves a node from source to dest
	 * 
	 * @param sourceNode
	 * @param destNode
	 * @return
	 */
	public String moveItem(String sourceNode, String destNode) {		
		String fsxml = "<fsxml mimetype='application/fscommand' id='move'>"
						+ "<properties>"
						+ "<source>"+sourceNode+"</source>"
						+ "<destination>"+destNode+"</destination>"
						+ "<params></params></properties></fsxml>";
		
		return FSXMLRequestHandler.instance().handlePOST(sourceNode, fsxml);
	}
	
	public List<String> getRefers(String node) {
		List<String> results = new ArrayList<String>();		
		String fsxml = "<fsxml mimetype='application/fscommand' id='showrefs'></fsxml>";
		
		String response = FSXMLRequestHandler.instance().handlePOST(node, fsxml);
		
		Document doc = null;
		try {
			doc = DocumentHelper.parseText(response);
		} catch (DocumentException e) {
			return results;
		}
		List<Node> refers = doc.selectNodes("//parent");
		for (Iterator<Node> it = refers.iterator(); it.hasNext(); ) {
			Node refer = it.next();			
			String parent = refer.getText();
			results.add(parent);
		}
		return results;
	}
	
	/**
	 * Updates all refers of originalNode to the newNode
	 * @param originalNode
	 * @param newNode
	 * @return
	 */
	public boolean updateRefers(String originalNode, String newNode) {
		List<String> refers = getRefers(originalNode);

		for (Iterator<String> it = refers.iterator(); it.hasNext(); ) {
			String refer = it.next();			
			
			String fsxml = "<fsxml><attributes><referid>"+newNode+"</referid></attributes></fsxml>";
			//TODO: check response
			FSXMLRequestHandler.instance().handlePUT(refer+"/attributes", fsxml);			
		}
		return true;
	}
	
	public void transcodeVideo(String videoUri, String profile, String filePath, String projectnr, String filename) {
		Document videoDocument = DocumentHelper.createDocument();
		Element fsxml = videoDocument.addElement("fsxml");
		fsxml.addElement("properties");
		Element rawvideo = fsxml.addElement("rawvideo");
		rawvideo.addAttribute("id", "1");
		
		LOG.info("Looking for ingest profiles");
		
		Document profileDoc = FSXMLRequestHandler.instance().getNodePropertiesByType(PROFILE_SETTINGS);
		boolean foundProfile = false;
		Element wantedProfile = null;
		
		List<Node> encodingProfiles = profileDoc.selectNodes("//encodingprofile/properties");
		for (Node encodingProfile : encodingProfiles) {
			String name = encodingProfile.selectSingleNode("name") == null ? "" : encodingProfile.selectSingleNode("name").getText();
			//matching profile found
			if (name.toLowerCase().equals(profile.toLowerCase())) {
				//Add profile name to last part of filename, just before extension
				String prefile = filePath.substring(0, filePath.lastIndexOf("."));
				String postfile = filePath.substring(filePath.indexOf("."));
				
				LOG.info("Profile "+profile+" found!");
				wantedProfile = (Element) encodingProfile.detach();				
				wantedProfile.addElement("reencode").addText("true");
				wantedProfile.addElement("original").addText(videoUri);
				wantedProfile.addElement("filename").addText(prefile+"_"+name+postfile);
				wantedProfile.addElement("mount").addText("marin");
				wantedProfile.selectSingleNode("name").detach();
				rawvideo.add(wantedProfile);
				foundProfile = true;
				break;
			}
		}
		
		if (!foundProfile) {
			LOG.error("Could not find profile "+profile);
		} else {
			String videoXml = videoDocument.asXML();
			LOG.info("Adding video node "+videoXml);
			FSXMLRequestHandler.instance().handlePUT(USER_ROOT+"/video/"+projectnr+":Export:video:transcode:"+filename+"/properties", videoXml);
		}
	}
	
	public String getProperty(Document doc, String name) {
		Node property  = doc.selectSingleNode(name);
		if (property != null) {
			return property.getText();
		}
		return "";		
	}
	
	public String getProperty(Node node, String name) {
		Node n = node.selectSingleNode(name);
		if (n != null) {
			return n.getText();
		}
		return "";
	}

	public List<Node> getChilds(Document doc, String name) {
		List<Node> nodes = doc.selectNodes(name);		
		return nodes;
	}
	
	//TODO: make this all configurable
	public void createProjectStructure(String projectLevel, String projectnr) {		
		//Create directories (only if they don't exist) in case of a new project
		//so we have the basic structure        
		makeDirectory(rawRootDir+projectLevel+"/"+projectnr+"/Raw/video/");
		makeDirectory(rawRootDir+projectLevel+"/"+projectnr+"/Raw/high-speed/");
		makeDirectory(rawRootDir+projectLevel+"/"+projectnr+"/Raw/photo/");
		makeDirectory(rawRootDir+projectLevel+"/"+projectnr+"/Edit/");
		makeDirectory(rawRootDir+projectLevel+"/"+projectnr+"/Export/video/manual/");
		makeDirectory(rawRootDir+projectLevel+"/"+projectnr+"/Export/video/raw_copy/");
		makeDirectory(rawRootDir+projectLevel+"/"+projectnr+"/Export/video/transcode/");
		makeDirectory(rawRootDir+projectLevel+"/"+projectnr+"/Export/data/manual/");
		makeDirectory(rawRootDir+projectLevel+"/"+projectnr+"/Export/data/raw_copy/");
		makeDirectory(rawRootDir+projectLevel+"/"+projectnr+"/Export/data/transcode/");
		makeDirectory(rawRootDir+projectLevel+"/"+projectnr+"/Export/high-speed/manual/");
		makeDirectory(rawRootDir+projectLevel+"/"+projectnr+"/Export/high-speed/raw_copy/");
		makeDirectory(rawRootDir+projectLevel+"/"+projectnr+"/Export/high-speed/transcode/");
		makeDirectory(rawRootDir+projectLevel+"/"+projectnr+"/Export/other/");
		makeDirectory(rawRootDir+projectLevel+"/"+projectnr+"/Export/photo/manual/");
		makeDirectory(rawRootDir+projectLevel+"/"+projectnr+"/Export/photo/raw_copy/");
		makeDirectory(rawRootDir+projectLevel+"/"+projectnr+"/Export/photo/transcode/");
		makeDirectory(rawRootDir+projectLevel+"/"+projectnr+"/Export/report/");		
		makeDirectory(rawRootDir+projectLevel+"/"+projectnr+"/Export/visualizations/");
		makeDirectory(rawRootDir+projectLevel+"/"+projectnr+"/Export/delivery_log/");
	}
	
	private void makeDirectory(String directory) {
		File dir = new File(directory);
		if (!dir.exists()) {
			dir.mkdirs();
			dir.setExecutable(true);
			dir.setReadable(true);
			dir.setWritable(true);
		}
	}
}
