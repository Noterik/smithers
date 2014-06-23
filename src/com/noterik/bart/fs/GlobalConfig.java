/*
 * Created on Jan 25, 2007
 */
package com.noterik.bart.fs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.noterik.bart.fs.cache.CacheHandler;
import com.noterik.bart.fs.db.ConnectionHandler;
import com.noterik.bart.fs.fsxml.DAOFactory;
import com.noterik.bart.fs.triggering.TriggerSystemManager;
import com.noterik.springfield.tools.net.MessageDispatcher;
import com.noterik.springfield.tools.net.MessageDispatcherFactory;


public class GlobalConfig {
	
	/**
	 * Logger
	 */
	private static Logger logger = Logger.getLogger(GlobalConfig.class);
	
	
	
	/** Noterik package root */
	public static final String PACKAGE_ROOT = "com.noterik";
	
	/** service name */
	public static final String SERVICE_NAME = "smithers2";
	
	public static final String SEARCH_LISA = "lisa";
	public static final String SEARCH_MEMORY = "in-memory";

	private static GlobalConfig instance;
	private static String CONFIG_FILE = "config.xml";
	private static int DEF_MAX_GET_THREADS = 20;
	private static int DEF_MAX_POST_THREADS = 20;
	private static int DEF_MAX_PUT_THREADS = 20;
	private static int DEF_MAX_DELETE_THREADS = 20;
	private static int DEF_MAX_DB_CONN = 100;
	
	/**
	 * path in which tomcat is running
	 */
	public static String baseDir;

	// database settings
	private String dbHost;
	private String dbName;
	private String dbUser;
	private String dbPassword;

	// Connection handler type
	private String connectionHandlerType;
	private int maxNumDbConnections;
	
	// DAOFactory
	private DAOFactory daoFactory;
	public DAOFactory getDAOFactory() {return daoFactory;}

	// for threads & scripts
	private boolean threadedScriptExec = false;
	private boolean useQueueRecovery = false;
	private int maxNumGETThreads;
	private int maxNumPUTThreads;
	private int maxNumPOSTThreads;
	private int maxNumDELETEThreads;
	
	// search settings
	private String searchEngine;
	
	// ingest/ftp settings
	private String tempDir;
	private String ingestBaseDir;
	private String imageServerHost;
	private int serverRangeOffset;
	private int numberOfServers;
	private String ftpServerPrefix;
	private String ftpServerSuffix;
	private String ftpImage;
	private String ftpImageUser;
	private String ftpImagePass;
	private String ftpImagePath;
	private String mountPrefix;
	
	/* host name (read from servlet context, used for serializing objects in db to distinguish 
	* the smithers host who added the action)
	*/
	private String hostName;

	
	
	/* lazy homer */
	private LazyHomer lazyHomer;

	private GlobalConfig() {
		System.out.println("Smithers: initialization starting.");
		
		initConfig();
		initLogging();
		
		System.out.println("Smithers: initialization done.");
	}
	
	/**
	 * Stop global processes/threads.
	 */
	public void destroy() {
		// don't use log4j since it doesn't exist anymore
		System.out.println("Smithers: destroying GlobalConfig.");
		

		destroyConnectionHandler();
		lazyHomer.destroy();
		destroyActionSets();	//TODO: seems like destroyActionsSets stops the destroy?		
		
		System.out.println("Smithers: destroying GlobalConfig done.");
	}

	private void initConfig() {
		System.out.println("Smithers: initializing configuration.");
		
		// properties
		Properties props = new Properties();
		
		// new loader to load from disk instead of war file
		String configfilename = "/springfield/homer/config.xml";
		if (LazyHomer.isWindows()) {
			configfilename = "c:\\springfield\\homer\\config.xml";
		}
		
		// load from file
		try {
			System.out.println("INFO: Loading config file from load : "+configfilename);
		//	File file = new File(baseDir + "conf/" + CONFIG_FILE);
			File file = new File(configfilename);

			if (file.exists()) {
				props.loadFromXML(new BufferedInputStream(new FileInputStream(file)));
			} else { 
				System.out.println("FATAL: Could not load config "+configfilename);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// database settings
		dbHost = props.getProperty("db-host");
		dbName = props.getProperty("db-name");
		dbUser = props.getProperty("db-user");
		dbPassword = props.getProperty("db-password");
		
		// init connection handler type
		connectionHandlerType = props.getProperty("connection-handler-type");
		try {
			maxNumDbConnections = new Integer(props.getProperty("max-num-db-connections"));
		} catch (NumberFormatException e) {
			maxNumDbConnections = DEF_MAX_DB_CONN;
		}
		
		// initialize DAOFactory
		daoFactory = DAOFactory.getDAOFactory(DAOFactory.MYSQL);
		
		// search settings
		searchEngine = props.getProperty("search-engine");
		
		// thread related properties
		if(props.getProperty("threaded-script-exec") != null && props.getProperty("threaded-script-exec").equals("true")){
			threadedScriptExec = true;
		}
		if(props.getProperty("use-queue-recovery") != null && props.getProperty("use-queue-recovery").equals("true")){
			useQueueRecovery = true;
		}
		try {
			maxNumGETThreads = new Integer(props.getProperty("GET-max-num-threads"));
			maxNumPOSTThreads = new Integer(props.getProperty("POST-max-num-threads"));
			maxNumPUTThreads = new Integer(props.getProperty("PUT-max-num-threads"));
			maxNumDELETEThreads = new Integer(props.getProperty("DELETE-max-num-threads"));
		} catch (NumberFormatException e) {
			maxNumGETThreads = DEF_MAX_GET_THREADS;
			maxNumPOSTThreads = DEF_MAX_POST_THREADS;
			maxNumPUTThreads = DEF_MAX_PUT_THREADS;
			maxNumDELETEThreads = DEF_MAX_DELETE_THREADS;
		}
		
		// old ingest properties for IngestHandler
		ingestBaseDir = props.getProperty("ingest-base-dir");
		imageServerHost = props.getProperty("image-server-host");
		if(props.getProperty("number-of-servers") != null){
			numberOfServers = new Integer(props.getProperty("number-of-servers")).intValue();
		}
		if(props.getProperty("server-range-offset") != null){
			serverRangeOffset = new Integer(props.getProperty("server-range-offset")).intValue();
		}
		ftpServerPrefix = props.getProperty("ftp-server-prefix");
		ftpServerSuffix = props.getProperty("ftp-server-suffix");
		ftpImage = props.getProperty("ftp-image");
		mountPrefix = props.getProperty("mount-prefix");
		ftpImageUser = props.getProperty("ftp-image-user");
		ftpImagePass = props.getProperty("ftp-image-pass");
		ftpImagePath = props.getProperty("ftp-image-path");
		
		// message dispatching group
		//messageDispatcherGroup = props.getProperty("message-dispatcher-group");
		//System.out.println("CACHE PROBLEM="+messageDispatcherGroup);
		
		// temporary folder
		tempDir = props.getProperty("temp-dir");
		
		System.out.println("Smithers: configuration initialized.");
	}
	
	/**
	 * Cleans up connection handler
	 */
	private void destroyConnectionHandler() {
		ConnectionHandler.instance().destroy();
	}

	/**
	 * Initialize logging
	 */
	private void initLogging() {
		System.out.println("Smithers: initializing logging.");
		
		// enable appenders
		String logPath = "";
		int index = baseDir.indexOf("webapps");
    	if(index!=-1) {
    		logPath += baseDir.substring(0,index);
    	}
		logPath += "logs/"+SERVICE_NAME+"/"+SERVICE_NAME+".log";
		
		try {
			// default layout
			//Layout layout = new PatternLayout("%r [%t] %-5p %c %x - %m%n");
			Layout layout = new PatternLayout("%-5p: %d{yyyy-MM-dd HH:mm:ss,SSS} %c %x - %m%n");
			
			// rolling file appender
			DailyRollingFileAppender appender1 = new DailyRollingFileAppender(layout,logPath,"'.'yyyy-MM-dd");
			BasicConfigurator.configure(appender1);
			
			// console appender 
			ConsoleAppender appender2 = new ConsoleAppender(layout);
			// only log error messages to console
			//appender2.setThreshold(Level.ERROR);
			BasicConfigurator.configure(appender2);
		}
		catch(IOException e) {
			System.out.println("GlobalConfig got an exception while initializing the logging configuration");
			e.printStackTrace();
		}
		
		/*
		 *  turn off all logging, and enable ERROR logging for noterik root package
		 *  use restlet.LoggingResource to enable specific logging
		 */
		Logger.getRootLogger().setLevel(Level.OFF);
		Logger.getLogger(PACKAGE_ROOT).setLevel(Level.ERROR);
		
		System.out.println("Smithers: initializing logging done.");
	}

	public static GlobalConfig instance() {
		if (instance == null) {
			instance = new GlobalConfig();
		}
		return instance;
	}

	

	/**
	 * Destroy timer actionsets from timer scripts
	 */
	private void destroyActionSets() {
		TriggerSystemManager tsm = TriggerSystemManager.getInstance();
		tsm.destroy();
	}
	
	public static String getBaseDir() {
		return GlobalConfig.baseDir;
	}

	public static void initialize(String baseDir) {
		GlobalConfig.baseDir = baseDir;
		if (instance == null) {
			instance = new GlobalConfig();
		}
	}
	
	public void setLazyHomer(LazyHomer lazyHomer) {
		this.lazyHomer = lazyHomer;
	}

	public String getBaseAssetPath() {
		return baseDir + "xml/";
	}

	public String getDatabaseHost() {
		return dbHost;
	}

	public String getDatabaseName() {
		return dbName;
	}

	public String getDatabaseUser() {
		return dbUser;
	}

	public String getDatabasePassword() {
		return dbPassword;
	}

	public String getTempDir(){
		return tempDir;
	}
	
	public String getIngestBaseDir(){
		return ingestBaseDir;
	}

	public String getImageServerHost(){
		return imageServerHost;
	}

	public int getServerRangeOffset(){
		return serverRangeOffset;
	}

	public int getNumberOfServers() {
		return numberOfServers;
	}

	public String getFtpServerPrefix() {
		return ftpServerPrefix;
	}

	public String getFtpServerSuffix() {
		return ftpServerSuffix;
	}

	public String getMountPrefix() {
		return mountPrefix;
	}

	public String getFtpImage() {
		return ftpImage;
	}

	public String getFtpImageUser() {
		return ftpImageUser;
	}

	public String getFtpImagePass() {
		return ftpImagePass;
	}

	public String getFtpImagePath() {
		return ftpImagePath;
	}
	
	public String getSearchEngine(){
		return searchEngine;
	}
	
	public int getMaxNumDbConnections() {
		return maxNumDbConnections;
	}

	public int getMaxNumDELETEThreads() {
		return maxNumDELETEThreads;
	}

	public int getMaxNumGETThreads() {
		return maxNumGETThreads;
	}

	public int getMaxNumPOSTThreads() {
		return maxNumPOSTThreads;
	}

	public int getMaxNumPUTThreads() {
		return maxNumPUTThreads;
	}
	
	public String getRecoveryDir() {
		return baseDir + "recovery";
	}
	
	public String getConnectionHandlerType() {
		return connectionHandlerType;
	}
	
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	
	public String getHostName() {
		return hostName;
	}
	
	public boolean isThreadedScriptExec(){
		return threadedScriptExec;
	}
	
	public boolean useQueueRecovery(){
		return useQueueRecovery;
	}
}