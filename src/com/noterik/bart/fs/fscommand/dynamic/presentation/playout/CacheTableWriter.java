package com.noterik.bart.fs.fscommand.dynamic.presentation.playout;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Iterator;

import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;

public class CacheTableWriter extends Thread {
	
	private CacheTableReader reader = null;
	private int mb = 1024*1024;
	private volatile boolean running = true;
	
	public CacheTableWriter(String name,CacheTableReader reader) {
		super(name);
		this.reader = reader;
	}
	
	public void run() {
		while (running) {
			try {
				sleep(10*1000);
				//System.out.println("FR="+reader.fullyLoaded()+" C="+cache.getCachedUrls());
				if (reader.fullyLoaded() && cache.getCachedUrls()!=null) {
					
					String body = "<fsxml><properties><list>";
				
					for(Iterator<String> iter = cache.getCachedUrls(); iter.hasNext(); ) {
						String url = (String)iter.next();
						url = url.replace(",", ";");
						//System.out.println("URL="+url);
						body+=url;
						if (iter.hasNext()) body+=",";
					}
				
					body+="</list></properties></fsxml>";
					String hostname = InetAddress.getLocalHost().toString();
					int pos = hostname.indexOf("/");
					if (pos!=-1) hostname=hostname.substring(pos+1);
					//System.out.println("HOSTNAME="+hostname+" "+body);
					FSXMLRequestHandler.instance().handlePUT("/domain/webtv/tmp/cache/dataset/"+hostname+"/properties",body);;
				}
				Runtime runtime = Runtime.getRuntime();

				long totalmem = runtime.totalMemory() / mb;
				long freemem = runtime.freeMemory() / mb;
				long usedmem = (runtime.totalMemory() - runtime.freeMemory()) / mb;
				long maxmem = runtime.maxMemory() / mb;
				//System.out.println("totalmem = "+totalmem+"MB");
				//System.out.println("freemem = "+freemem+"MB");
				//System.out.println("usedmem = "+usedmem+"MB");
				//System.out.println("maxmem = "+maxmem+"MB");
				sleep(50*1000);
			} catch (InterruptedException ex) {
				// break out of the loop without a error
			} catch(Exception e) {
				System.out.println("Can't sleep in CacheTableWriter."+e);
			//	e.printStackTrace();
			}
		}
		System.out.println("Smithers: shutting down CacheTableWriter");
	}
	
	public void destroy() {
		running = false;
		this.interrupt();
	}
	

}
