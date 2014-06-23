package com.noterik.bart.fs.fscommand.dynamic.presentation.playout;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.collections.map.LRUMap;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.noterik.bart.fs.LazyHomer;
import com.noterik.bart.fs.cloudexporter.DiskExporter;
import com.noterik.bart.fs.cloudimporter.DefaultImporter;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;


public class cache {
	/** Logger */
	private static Logger logger = Logger.getLogger(cache.class);
	private static LRUMap cached = new LRUMap(3000);
	private static HashMap<String,String> refers = new HashMap<String,String>();
	private static HashMap<String,ArrayList<String>> paramref = new HashMap<String,ArrayList<String>>();
	private static Document empty = DocumentHelper.createDocument();
	private static CacheMulticastReceiver receiver = null;
	private static boolean multicast = true;
	private static boolean active = false;
	private static String debuglevel = "off";
	private static int totalreq = 0;
	private static int hitreq = 0;
	private static enum methods { GET,POST,PUT,DELETE,TRACE,LINK; }
	private static CacheTableWriter cachewriter = null;
	private static CacheTableReader cachereader = null;
	public static Boolean started = false;

    static {
    	/*
    	readCacheConfig();
		if (receiver==null) {
			receiver = new CacheMulticastReceiver("receiver");
			receiver.start();
			cachereader = new CacheTableReader("cachereader");
			cachereader.start();
			cachewriter = new CacheTableWriter("cachewriter",cachereader);
			cachewriter.start();
		}
		*/
    	init();
    }

    public static void init() {
    	
    	readCacheConfig();
 
		if (receiver==null) {
			started = true;
			receiver = new CacheMulticastReceiver("receiver");
			receiver.start();
			cachereader = new CacheTableReader("cachereader");
			cachereader.start();
			cachewriter = new CacheTableWriter("cachewriter",cachereader);
			cachewriter.start();
		}	
		try{
            InetAddress mip=InetAddress.getLocalHost();
            String myip = ""+mip.getHostAddress();
            String sends = myip+":"+LazyHomer.getSmithersPort()+":"+LazyHomer.getPort()+":"+LazyHomer.getRole();
            CacheMulticastSender.send(sends, "INFO", "ALIVE");
		}catch (Exception e){
			System.out.println("Exception ="+e.getMessage());
		}
    }

	
	public static Document get(String url) {
		if (!active) return null;
		
		totalreq++;
		Document result = (Document)cached.get(url);
		if (result==null) {
			if (debuglevel.equals("high")) System.out.println("CACHE MISS="+url);	
			return null;
		} else {
			hitreq++;
			if (debuglevel.equals("high")) System.out.println("CACHE HIT= "+url);
			if (result==empty) return null;
			result = (Document)result.clone();
			return result;
		}
	}
	
	public static Boolean isEmpty(String url) {
		Document result = (Document)cached.get(url);
		if (result==empty) {
			return true;
		}
		return false;
	}
	
	
	public static void put(String url,Document result) {
		if (!active) return;

		if (result==null) {
			cached.put(url, empty);
			return;
		}
		cached.put(url, (Document)result.clone());
		setCacheWatchers(url,result.asXML());
	}
	
	public static void putParams(String url,Document result,String params) {
		if (!active) return;

		if (result==null) {
			cached.put(url+params, empty);
			return;
		}
		//System.out.println("PUTPARAMS");
		cached.put(url+params, (Document)result.clone());
		setCacheWatchers(url+params,result.asXML());
		setParametersWatchers(url,params);
	}
	
	private static void setParametersWatchers(String url,String params) {
		System.out.println("PW="+url+" "+params);
		// try to get the list we already have for this base url
		ArrayList<String> curlist = paramref.get(url);
		if (curlist!=null) {
			// add to the list
			curlist.add(params);
		} else {
			// no list so make one
			curlist = new ArrayList<String>();
			curlist.add(params);
			paramref.put(url, curlist);
		}
		System.out.println("CL="+curlist);
	}
	
	
	private static void setCacheWatchers(String url,String body) {
		int pos = body.indexOf("referid=");
		while (pos!=-1) {
			int endpos = body.indexOf("\"",pos+10);
			String refer = body.substring(pos+9, endpos);
			if (debuglevel.equals("high")) System.out.println("REFER="+refer+" SOURCE="+url);
			refers.put(refer, url);
			pos = body.indexOf("referid=",endpos+1);
		}
	}
	
	
	public static void signal(String adr,String method, String msg) {
		if (multicast) {
			switch (methods.valueOf(method)) {
				case POST : CacheMulticastSender.send(adr.toString(), method, msg);break;
				case PUT : CacheMulticastSender.send(adr.toString(), method, msg);break;
				case DELETE : CacheMulticastSender.send(adr.toString(), method, msg);break;
				case TRACE : break;
				case LINK : CacheMulticastSender.send(adr.toString(), method, msg);break;
			}
		} else {
			switch (methods.valueOf(method)) {
				case POST : signalPost(adr,msg);break;
				case PUT : signalPut(adr,msg);break;
				case DELETE : signalDelete(adr,msg);break;
				case TRACE : break;
			}	
		}
	}
	
	public static void signalRemote(String adr,String method, String uri) {
		switch (methods.valueOf(method)) {
			case POST : signalPost(adr,uri);break;
			case PUT : signalPut(adr,uri);break;
			case DELETE : signalDelete(adr,uri);break;
		}	
	}
	
	public static void signalPut(String adr, String uri) {
		if (debuglevel.equals("high")) System.out.println("SIGNAL PUT = "+adr.toString()+" U="+uri);
		//System.out.println("SIGNAL PUT = "+adr.toString()+" U="+uri);
		
		// do we have some of this in cache ?
		if (uri.indexOf("/domain/webtv/config/cache/presentationquickstart/1")!=-1) {
			readCacheConfig();
			deleteCacheRecursive(uri,0);
		} else if (uri.indexOf("/domain/webtv/service/smithers/nodes/192.168.1.107")!=-1) {
				DiskExporter.exportSmithersNode(uri);
				deleteCacheRecursive(uri,0);
		} else {
			deleteCacheRecursive(uri,0);
		}
	} 
	
	public static void readCacheConfig() {
		System.out.println("READ CACHENODE");
		Document cachenode = FSXMLRequestHandler.instance().getNodeProperties("/domain/webtv/config/cache/presentationquickstart/1", false);
		System.out.println("CACHENODE="+cachenode);
		if (cachenode != null) {
			Element activenode = (Element)cachenode.selectSingleNode("/fsxml/presentationquickstart[@id='1']/properties/active");
			if (activenode.getText().equals("true")) {
				active = true;
				logger.debug("CACHE TURNED ON");
			} else {
				active = false;
				logger.debug("CACHE TURNED OFF");
			}
			Element debuglevelnode = (Element)cachenode.selectSingleNode("/fsxml/presentationquickstart[@id='1']/properties/debuglevel");
			if (debuglevelnode.getText().equals("high")) {
				debuglevel = "high";
				FSXMLRequestHandler.debuglevel = "high";
				logger.debug("CACHE DEBUG LEVEL HIGH");
			} else {
				debuglevel = "off";
				FSXMLRequestHandler.debuglevel = "off";
				logger.debug("CACHE DEBUG LEVEL OFF");
			}
		} else {
			logger.debug("CACHE CONFIG MISSING");
			//DefaultImporter.importDefaultCloud();
		}

	}
	
	public static void signalPost(String adr, String uri) {
		if (debuglevel.equals("high")) System.out.println("SIGNAL POST = "+adr.toString()+" U="+uri);
		//System.out.println("SIGNAL POST = "+adr.toString()+" U="+uri);
		
		// do we have some of this in cache ?
		deleteCacheRecursive(uri,0);
	}
	
	public static void signalDelete(String adr, String uri) {
		if (debuglevel.equals("high")) System.out.println("SIGNAL DELETE = "+adr.toString()+" U="+uri);
		//System.out.println("SIGNAL DELETE = "+adr.toString()+" U="+uri);
		
		// do we have some of this in cache ?
		deleteCacheRecursive(uri,0);
	}
	
	private static void deleteCacheRecursive(String uri,int depth) {
		int pos = uri.lastIndexOf("/");
		while (pos!=-1) { // we need to walk up the tree to check
			Document doc = (Document)cached.get(uri);
			if (doc!=null) {
				// we have it in cache lets delete the main cache entry
				cached.remove(uri);
				
				
				if (debuglevel.equals("high")) System.out.println(depth+" removed main="+uri);
				//System.out.println(depth+" removed main="+uri);
				// now was this uri also a referid somewhere ? only works for one (daniel?)
				String refer = refers.get(uri);
				if (refer!=null) {
					refers.remove(uri); // remove it
					// call it for subchilds
					if (depth<10) {				
						if (debuglevel.equals("high")) System.out.println(depth+" checking refer="+refer);
						//System.out.println(depth+" checking refer="+refer);

						deleteCacheRecursive(refer,depth++);
					} else {
						System.out.println("***** RECURSIVE ERROR IN CACHE REACHED 10");
					}
				}
			}
			
			// also remove them from the ones with params
			ArrayList<String> list = paramref.get(uri);
			if (list!=null) {
				for (Iterator<String> i = list.iterator(); i.hasNext();) {
					String p = i.next();
					//System.out.println("REMOVE PARAM="+uri+p);
					cached.remove(uri+p);
					paramref.remove(uri);
				}
			}
			
			uri = uri.substring(0,pos);
			pos = uri.lastIndexOf("/");
			//System.out.println(depth+" new uri="+uri);
		}
	}
	
	public static String getPerformance() {
		if (totalreq!=0) {
			return ""+((hitreq/(float)totalreq)*100);
		}
		return "0";
	}
	
	public static int getTotalRequest() {
		return totalreq;
	}
	
	public static int getCacheSize() {
		return cached.size();
	}
	
	
	
	public static Iterator<String> getCachedUrls() {
		if (cached.size()==0) {
			return null;
		} else {
			return cached.keySet().iterator();
		}
	}
	
	public static void destroy() {		
		System.out.println("Smithers: shutting down PresentationQuickStart Cache");
		if (cachewriter!=null) cachewriter.destroy();
		if (receiver!=null) receiver.destroy();
	}
}
