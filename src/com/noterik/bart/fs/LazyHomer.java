/* 
* LazyHomer.java
* 
* Copyright (c) 2012 Noterik B.V.
* 
* This file is part of smithers, related to the Noterik Springfield project.
*
* Smithers is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Smithers is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Smithers.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.noterik.bart.fs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.*;
import org.dom4j.*;
import org.springfield.mojo.interfaces.ServiceManager;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.HttpHelper;

public class LazyHomer implements MargeObserver {
	
	private static Logger LOG = Logger.getLogger(LazyHomer.class);

	/** Noterik package root */
	public static final String PACKAGE_ROOT = "com.noterik";
	private static enum loglevels { all,info,warn,debug,trace,error,fatal,off; }
	public static String myip = "unknown";
	private static int port = -1;
	private static int smithers_port = -1;
	private static int bart_port = -1;
	static String role = "production";
	static String group = "224.0.0.0";
	static int ttl = 32;
	static boolean registered = false;
	//static LazyMarge marge;
	static SmithersProperties selectedsmithers = null;
	private static boolean running = false;
	private static String rootPath = null;
	private static long MAX_SERVICE_DELAY = 30000L;
	
	private static Map<String, SmithersProperties> smithers = new HashMap<String, SmithersProperties>();
	private static LazyHomer ins;
	private static DiscoveryThread thread;
	
	/**
	 * Initializes the configuration
	 */
	public void init(String r) {
		rootPath = r;
		ins = this;
		initLogger();
		if (port==-1) initConfig();
		try{
			InetAddress mip=InetAddress.getLocalHost();
			myip = ""+mip.getHostAddress();
		}catch (Exception e){
			LOG.error("Exception ="+e.getMessage());
		}
		LOG.info("Smithers init service name = smithers on ipnumber = "+myip+" on port number "+port);
	//	marge = new LazyMarge();
		
		// lets watch for changes in the service nodes in smithers
//		marge.addObserver("/domain/internal/service/smithers/nodes/"+myip, ins);
//		marge.addTimedObserver("/smithers/downcheck",6,this);
		thread = new DiscoveryThread();	

	}
	
	public static int getSmithersPort() {
		return smithers_port;
	}
	
	public static void addSmithers(String ipnumber) {
		// do we need to know about the others ?
		// we could poll all of them every 10sec ?
	}
	
	
	
	private Boolean checkKnown() {	
		String xml = "<fsxml><properties><depth>1</depth></properties></fsxml>";
		String nodes = LazyHomer.sendRequest("GET","/domain/internal/service/smithers/nodes",xml,"text/xml");

		boolean iamok = false;

		try { 
			boolean foundmynode = false;
			
			Document result = DocumentHelper.parseText(nodes);
			for(Iterator<Node> iter = result.getRootElement().nodeIterator(); iter.hasNext(); ) {
				Element child = (Element)iter.next();
				if (!child.getName().equals("properties")) {
					String ipnumber = child.attributeValue("id");
					String status = child.selectSingleNode("properties/status").getText();
					String name = child.selectSingleNode("properties/name").getText();

					if (ipnumber.equals(myip)) {
						foundmynode = true;
						if (name.equals("unknown")) {
							LOG.info("This smithers is not verified change its name, use smithers todo this for ip "+myip);
						} else {
							// so we have a name (verified) return true
							iamok = true;
						}
					}
				}	
			}
			if (!foundmynode) {
				LOG.info("LazyHomer : Creating my processing node "+LazyHomer.getSmithersUrl()  + "/domain/internal/service/smithers/properties");
				String os = "unknown"; // we assume windows ?
				try{
					  os = System.getProperty("os.name");
				} catch (Exception e){
					LOG.error("LazyHomer : "+e.getMessage());
				}
				
				String newbody = "<fsxml>";
	        	newbody+="<nodes id=\""+myip+"\"><properties>";
	        	newbody+="<name>master</name>";
	        	newbody+="<status>on</status>";
	        	newbody+="<lastseen>"+new Date().getTime()+"</lastseen>";
	        	newbody+="<preferedsmithers>"+myip+"</preferedsmithers>";
	        	newbody+="<activesmithers>"+myip+"</activesmithers>";

	        	// i know this looks weird but left it for future extentions
	        	if (isWindows()) {
	        		newbody+="<defaultloglevel>info</defaultloglevel>";
	        	} if (isMac()) {
	        		newbody+="<defaultloglevel>info</defaultloglevel>";
	        	} if (isUnix()) {
	        		newbody+="<defaultloglevel>info</defaultloglevel>";
	        	} else {
	        		newbody+="<defaultloglevel>info</defaultloglevel>";
	        	}
	        	newbody+="</properties></nodes></fsxml>";	
				LazyHomer.sendRequest("PUT","/domain/internal/service/smithers/properties",newbody,"text/xml");
			}
		} catch (Exception e) {
			LOG.info("LazyHomer exception doc");
			e.printStackTrace();
		}
		return iamok;
	}

	
	public static void setLastSeen() {
		Long value = new Date().getTime();
		LazyHomer.sendRequest("PUT", "/domain/internal/service/smithers/nodes/"+myip+"/properties/lastseen", ""+value, "text/xml");
	}
	

	
	public static void send(String method, String uri) {
		try {
			MulticastSocket s = new MulticastSocket();
			String msg = myip+" "+method+" "+uri;
			byte[] buf = msg.getBytes();
			DatagramPacket pack = new DatagramPacket(buf, buf.length,InetAddress.getByName(group), port);
			s.send(pack,(byte)ttl);
			s.close();
		} catch(Exception e) {
			LOG.error("LazyHomer error "+e.getMessage());
		}
	}
	
	public static void send(String method, String uri,String body) {
		try {
			MulticastSocket s = new MulticastSocket();
			String msg = myip+" "+method+" "+uri+" "+body;
			byte[] buf = msg.getBytes();
			DatagramPacket pack = new DatagramPacket(buf, buf.length,InetAddress.getByName(group), port);
			s.send(pack,(byte)ttl);
			s.close();
		} catch(Exception e) {
			LOG.error("LazyHomer error "+e.getMessage());
		}
	}
	
	public static Boolean up() {
		if (smithers==null) return false;
		return true;
	}
	
	public static String getSmithersUrl() {
		// should always be aimed at myself ?
		return "http://"+selectedsmithers.getIpNumber()+":"+smithers_port+"/smithers2";
	}
	
	public void remoteSignal(String from,String method,String url) {
		// LazyMarge not turned on yet so never called
	}
	
	public static boolean isWindows() {
		String os = System.getProperty("os.name").toLowerCase();
		return (os.indexOf("win") >= 0);
	}
 
	public static boolean isMac() {
 		String os = System.getProperty("os.name").toLowerCase();
		return (os.indexOf("mac") >= 0);
 	}
 
	public static boolean isUnix() {
 		String os = System.getProperty("os.name").toLowerCase();
		return (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0);
 	}

	public synchronized static String sendRequest(String method,String url,String body,String contentType) {
		return sendRequest(method,url,body,contentType,null);
	}
	
	public synchronized static String sendRequest(String method,String url,String body,String contentType,String cookies) {
		String fullurl = getSmithersUrl()+url;
		String result = null;
		boolean validresult = true;
		
		// first try 
		try {
			result = HttpHelper.sendRequest(method, fullurl, body, contentType,cookies);
			if (result.indexOf("<?xml")==-1) {
				LOG.error("FAIL TYPE ONE ("+fullurl+")");
				LOG.error("XML="+result);
				validresult = false;
			}
		} catch(Exception e) {
			LOG.error("FAIL TYPE TWO ("+fullurl+")");
			LOG.error("XML="+result);
			validresult = false;
		}
		
		LOG.debug("Valid request ("+fullurl+") ");
		return result;
	}
	
	/**
	 * get root path
	 */
	public static String getRootPath() {
		return rootPath;
	}
	

 
	/**
	 * Initializes logger
	 */
    private void initLogger() {    	 
    	System.out.println("Initializing logging for smithers");
    	
    	// get logging path
    	String logPath = LazyHomer.getRootPath().substring(0,LazyHomer.getRootPath().indexOf("webapps"));
		logPath += "logs/smithers/smithers.log";	
		
		try {
			// default layout
			Layout layout = new PatternLayout("%-5p: %d{yyyy-MM-dd HH:mm:ss} %c %x - %m%n");
			
			// rolling file appender
			DailyRollingFileAppender appender1 = new DailyRollingFileAppender(layout,logPath,"'.'yyyy-MM-dd");
			BasicConfigurator.configure(appender1);
			
			// console appender 
			ConsoleAppender appender2 = new ConsoleAppender(layout);
			BasicConfigurator.configure(appender2);
		}
		catch(IOException e) {
			System.out.println("SmithersServer got an exception while initializing the logger.");
			e.printStackTrace();
		}
		
		Level logLevel = Level.INFO;
		LOG.getRootLogger().setLevel(Level.OFF);
		LOG.getLogger(PACKAGE_ROOT).setLevel(logLevel);
		LOG.info("logging level: " + logLevel);
		
		LOG.info("Initializing logging done.");
    }

	
	
	private static void setLogLevel(String level) {
		Level logLevel = Level.INFO;
		Level oldlevel = LOG.getLogger(PACKAGE_ROOT).getLevel();
		switch (loglevels.valueOf(level)) {
			case all : logLevel = Level.ALL;break;
			case info : logLevel = Level.INFO;break;
			case warn : logLevel = Level.WARN;break;
			case debug : logLevel = Level.DEBUG;break;
			case trace : logLevel = Level.TRACE;break;
			case error: logLevel = Level.ERROR;break;
			case fatal: logLevel = Level.FATAL;break;
			case off: logLevel = Level.OFF;break;
		}
		if (logLevel.toInt()!=oldlevel.toInt()) {
			LOG.getLogger(PACKAGE_ROOT).setLevel(logLevel);
			LOG.info("logging level: " + logLevel);
		}
	}
	
	public static boolean isRunning() {
		return running;
	}
 

	
    /**
     * Shutdown
     */
	public void destroy() {
		System.out.println("Shutting down dthread");
		thread.destroy();
		// destroy timer
		//if (marge!=null) marge.destroy();
	}
	
	
	private class DiscoveryThread extends Thread {
	    private volatile boolean running = true; 
	    
		DiscoveryThread() {
	      super("dthread");
	      start();
	    }

	    public void run() {
	      while (running) {
	        try {
	          // very weird way to start
	          if (!registered) {
	        	  // lets create myself as a smithers
	        	  SmithersProperties sp = new SmithersProperties();
	        	  smithers.put(myip, sp);
	        	  sp.setIpNumber(myip);
	        	  sp.setAlive(true); // since talking its alive 
	        	  selectedsmithers = sp;
	        	  registered = checkKnown();
	        	  ServiceManager.setService(new ServiceHandler());
	          } else {
	        	  setLastSeen();
	        	  //setMargeStats();
	        	  //setHomerStats();
	          }
	        } catch(Exception e1) {
	        	e1.printStackTrace();
	        }
	        try {  
	          sleep(10*1000);
	        } catch (InterruptedException e) {
	          throw new RuntimeException(e);
	        }
	      }
	    }
	    
	    public void destroy() {
	    	running = false;
	    	this.interrupt();
	    }
	}
	
	private static void initConfig() {
		System.out.println("Smithers: initializing configuration.");
		
		// properties
		Properties props = new Properties();
		
		// new loader to load from disk instead of war file
		String configfilename = "/springfield/homer/config.xml";
		if (isWindows()) {
			configfilename = "c:\\springfield\\homer\\config.xml";
		}
		
		// load from file
		try {
			System.out.println("INFO: Loading config file from load : "+configfilename);
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
		
		// only get the marge communication port unless we are a smithers
		port = Integer.parseInt(props.getProperty("marge-port"));
		smithers_port = Integer.parseInt(props.getProperty("default-smithers-port"));
		bart_port = Integer.parseInt(props.getProperty("default-bart-port"));
		role = props.getProperty("role");
		if (role==null) role = "production";
		System.out.println("SMITHERS SERVER ROLE="+role);
	}
	
	public static int getPort() {
		if (port==-1) initConfig();
		return port;
	}
	
	public static String getRole() {
		return role;
	}
	
	/**
	 * Get the address of a active service in the cluster
	 * 
	 * @param service - The name of the service requested
	 */
	public static String getActiveService(String service) {
		String serviceAddress = "";
		long mostRecentServiceTime = 0L;
		
		service = service.toLowerCase();
		String response = LazyHomer.sendRequest("GET","/domain/internal/service/"+service+"/nodes",null,null);
		
		//Get all nodes for this service
		try { 
			Document result = DocumentHelper.parseText(response);
			for(Iterator<Node> iter = result.getRootElement().nodeIterator(); iter.hasNext(); ) {
				Element child = (Element)iter.next();
				if (!child.getName().equals("properties")) {
					String name = child.attributeValue("id");
					String status = child.selectSingleNode("properties/status") == null ? ""  : child.selectSingleNode("properties/status").getText();
					String lastSeen = child.selectSingleNode("properties/lastseen") == null ? "0"  : child.selectSingleNode("properties/lastseen").getText();
					long serviceTime = Long.parseLong(lastSeen);
					
					LOG.debug("node "+name+" found with status "+status+" lastseen "+lastSeen+" - "+serviceTime);
					
					//Check if this service is enabled and recently active
					if (status.equals("on") && serviceTime >  mostRecentServiceTime) {
						mostRecentServiceTime = serviceTime;
						serviceAddress = name;
					}				
				}
			}
		} catch (DocumentException e) {
			LOG.info("LazyHomer: "+e.getMessage());
		}
	
		//Check if this service was active within the MAX_SERVICE_DELAY
		long currentTime = new Date().getTime();
		if ((mostRecentServiceTime + MAX_SERVICE_DELAY) >  currentTime) {
			return serviceAddress;
		}
		return null;
	}
	
	/**
	 * @return - Returns the domains this smithers is configured to handle the timer scripts
	 */
	public static String[] getTimerScriptDomains() {
		String[] domains = new String[0];
		
		//check if IP is already initialized
		if (myip.equals("unknown")) {
			try{
				InetAddress mip=InetAddress.getLocalHost();
				myip = ""+mip.getHostAddress();
			}catch (Exception e){
				LOG.error("Exception ="+e.getMessage());
			}
		}

		String response = FSXMLRequestHandler.instance().getPropertyValue("/domain/internal/service/smithers/nodes/"+myip+"/properties/timerscriptdomains");

		if (response != null) {
			domains = response.split(",");
		}		
		return domains;
	}
}
