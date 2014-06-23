package com.noterik.bart.fs;

import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.restlet.Context;

import com.noterik.bart.fs.fscommand.dynamic.presentation.playout.cache;
import com.noterik.bart.fs.triggering.TriggerSystemManager;
import com.noterik.bart.fs.LazyHomer;

public class SmithersContextListener implements ServletContextListener {

	public void contextInitialized(ServletContextEvent event) {
		System.out.println("Smithers: context initialized");
		ServletContext servletContext = event.getServletContext();
		
		if(servletContext!=null) {
			// initialize global config
			GlobalConfig.initialize(servletContext.getRealPath("/"));
		}
		
		// turn logging off
		Context.getCurrentLogger().setLevel(Level.SEVERE);
		Logger.getLogger("").setLevel(Level.SEVERE);
		
		try {
			// determine hostname
			String hostName = InetAddress.getLocalHost().getHostName();
			GlobalConfig.instance().setHostName(hostName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// init triggering system manager
		TriggerSystemManager.getInstance();
 
		LazyHomer lh = new LazyHomer();
		lh.init(servletContext.getRealPath("/"));
		GlobalConfig.instance().setLazyHomer(lh);
	}
	
	public void contextDestroyed(ServletContextEvent event) {
		System.out.println("Smithers: context destroyed");
		GlobalConfig.instance().destroy();
	}

}
