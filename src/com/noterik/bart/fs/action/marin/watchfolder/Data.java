package com.noterik.bart.fs.action.marin.watchfolder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.noterik.bart.fs.action.ActionAdapter;

public class Data extends ActionAdapter {
	private static final long serialVersionUID = 1L;

	/** the UploadAction's log4j logger */
	private static final Logger LOG = Logger.getLogger(Data.class);
	
	private Config conf;
	private static String rawRootDir;
	private static String dataInputDir;
	
	/**
	 * Executes this action
	 */
	public String run() {
		//get config
		conf = new Config();

		LOG.info("---------------------------");
		LOG.info("Automated data ingest triggered");
		LOG.info("---------------------------");
		
		rawRootDir = conf.rawRootDir;
		dataInputDir = conf.dataInputDir;
		
		//Get all the XML files.
	   	LOG.info("Status: Fetching valid data files");  
		
	   	File[] fileList = getDirectoryContent(dataInputDir);
	   	int totalFiles = fileList.length;
	   	LOG.info("Status: Found "+totalFiles+" SBF files to be processed");
	   	
	   	Set<String> projects = new HashSet<String>();
	   	
	   	for (File sbfFile : fileList) {
	   		if (sbfFile.exists()) {
	   			LOG.info("Status: SBF file exists: "+sbfFile.getPath());	   			
	   			
	   			// touch file (for moving failed jobs)
	   			sbfFile.setLastModified(new Date().getTime());

	   			String projectnr = getProjectNumber(sbfFile.getName());
	   			String projectLevel = projectnr.substring(0,2)+"xxx";
	   			
	   			LOG.info("About to make project structure");
	   			conf.createProjectStructure(projectLevel, projectnr);
	   			projects.add("/"+projectLevel+"/"+projectnr+"/Export/data/raw_copy");	   			
	   			
	   			LOG.info("About to move file to "+rawRootDir+projectLevel+"/"+projectnr+"/Export/data/raw_copy/"+sbfFile.getName());
	   			boolean success = sbfFile.renameTo(new File(rawRootDir+projectLevel+"/"+projectnr+"/Export/data/raw_copy/"+sbfFile.getName()));
	   			if (!success) {
	   				LOG.error("Failed to move file to "+rawRootDir+projectLevel+"/"+projectnr+"/Export/data/raw_copy/"+sbfFile.getName());
	   			}
	   		}
	   	}
	   	
	   	//Now trigger index for all projects export/data/transcode dirs
	   	String cmd = "php /springfield/mediamix/cmd.php -u=noterik -p=noterik12 -a=index -r="+conf.repositoryId+" --dir=";
	   	mediamixUpdater(cmd, projects);
	   
	   	//Now trigger for *.dat file creation from the sbf files
	   	cmd = "php /springfield/mediamix/cmd.php -u=noterik -p=noterik12 -a=convert_sbf -r="+conf.repositoryId+" --dir=";
	   	mediamixUpdater(cmd, projects);
	   	
	   	LOG.info("Finished handling data input dir");
	   	
	   	return "";
	}
	private static File[] getDirectoryContent(String directory) {
		File d = new File(directory);
		File[] array = d.listFiles(new FilenameFilter() {
	   						
			public boolean accept(File dir, String name) {
				if(name.toLowerCase().endsWith(".sbf")) {
		   			return true;
		   		}
		   		return false;
		   	}
	   	});
	   	return array;
	}
	
	private String getProjectNumber(String filename) {
		Pattern p = Pattern.compile("[a-zA-Z]+([0-9.]+)_.+");
		Matcher m = p.matcher(filename);
		if (m.find()) {
			return m.group(1);
		}
		return "";
	}
	
	private void mediamixUpdater(String cmd, Set<String> projects) {
		for (String p : projects) {
	   		Process proc = null;
	   		StringBuffer response = new StringBuffer();
	   		try {
	   			LOG.info("about to execute: "+cmd+p);
	   			proc = Runtime.getRuntime().exec(cmd+p);
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
	            	LOG.info("Updated index for "+p+" response: "+response.toString());
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
	}
}
