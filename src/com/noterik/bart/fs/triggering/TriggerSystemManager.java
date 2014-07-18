/* 
* TriggerSystemManager.java
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
package com.noterik.bart.fs.triggering;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;

import com.noterik.bart.fs.GlobalConfig;
import com.noterik.bart.fs.db.ConnectionHandler;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.bart.fs.script.FSScript;
import com.noterik.bart.fs.script.exec.ExecutionQueueHandler;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * Initiates and manages the triggering system
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.test
 * @access private
 * @version $Id: TriggerSystemManager.java,v 1.26 2011-07-01 11:38:56 derk Exp $
 *
 */
public class TriggerSystemManager {
	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(TriggerSystemManager.class);
	
	/**
	 * Mimetype of scripts
	 */
	private static final String SCRIPT_MIMETYPE = "application/fsscript";

	/**
	 * Collection of triggering systems. One per domain.
	 */
	private Hashtable<String, TriggerSystem> TSs;

	/**
	 * Collection of scripts that are available throughout the system
	 */
	private Hashtable<String, FSScript> scripts;

	/**
	 * Shared instance throughout all classes
	 */
	private static TriggerSystemManager instance;

	public static TriggerSystemManager getInstance() {
		if (instance == null) {
			instance = new TriggerSystemManager();
		}
		return instance;
	}

	private TriggerSystemManager() {
		TSs = new Hashtable<String, TriggerSystem>();
		scripts = new Hashtable<String, FSScript>();
		init();
	}

	/**
	 * Get triggering system for domain
	 *
	 * @param domain
	 * @return
	 */
	public TriggerSystem getDomainTS(String domain) {
		TriggerSystem ts = null;
		if (TSs.containsKey(domain)) {
			ts = TSs.get(domain);
		}
		// if it's not there, create
		if (ts == null) {
			ts = new TriggerSystem();
			
			// add to Hashtable
			TSs.put(domain, ts);
		}
		return ts;
	}

	/**
	 * Initiates the triggering system
	 */
	private void init() {
		logger.info("Initializing scripts");

		// get all scripts
		Hashtable<String, FSScript> scripts = getAllScripts();

		// add scripts to the triggering system
		String uri;
		FSScript script;
		for (Iterator<String> iter = scripts.keySet().iterator(); iter.hasNext();) {
			uri = iter.next();
			script = scripts.get(uri);
			addScript(uri, script);
			logger.info("added script: " + uri);
		}
		if(GlobalConfig.instance().useQueueRecovery()) {
			// recover the queues from the database --> actions table
			ExecutionQueueHandler.instance().recoverQueues();
		}
		logger.info("finished initialization");
	}
	
	public void destroy() {
		String uri;
		FSScript script;
		
		for (Iterator<String> iter = scripts.keySet().iterator(); iter.hasNext();) {
			uri = iter.next();
			script = scripts.get(uri);
			script.destroy();
		}
	}

	/**
	 * Add script to trigger system
	 *
	 * @param uri
	 * @param script
	 */
	public void addScript(String uri, FSScript script) {
		logger.debug("ADD SCRIPT: " + uri);
		logger.debug("get: " + script.get() + ", put: " + script.put() + ", post: " + script.post() + ", delete: " + script.delete());
		
		// get domain
		String domain = URIParser.getDomainFromUri(uri);
		
		// remove
		removeScript(uri);		
		
		// add to get/put/post/delete queue
		if(script.get()) {
			getDomainTS(domain).getGetQueue().addObserver(script);
		}
		if(script.put()) {
			getDomainTS(domain).getPutQueue().addObserver(script);
		}
		if(script.post()) {
			getDomainTS(domain).getPostQueue().addObserver(script);
		}
		if(script.delete()) {
			getDomainTS(domain).getDeleteQueue().addObserver(script);
		}
		
		// add to scripts
		scripts.put(uri, script);
	}

	/**
	 * Removes a script from the triggering system.
	 *
	 * @param uri
	 */
	public void removeScript(String uri) {
		if(scripts.contains(uri)) {
			FSScript script = scripts.get(uri);
			String domain = URIParser.getDomainFromUri(uri);
			
			// remove from hashtable
			scripts.remove(uri);
			
			// remove from all the queues
			getDomainTS(domain).getGetQueue().deleteObserver(script);
			getDomainTS(domain).getPutQueue().deleteObserver(script);
			getDomainTS(domain).getPostQueue().deleteObserver(script);
			getDomainTS(domain).getDeleteQueue().deleteObserver(script);

		}
	}

	/**
	 * Get all scripts from the database
	 *
	 * @return
	 */
	private Hashtable<String, FSScript> getAllScripts() {
		Hashtable<String, FSScript> scripts = new Hashtable<String, FSScript>();

		String query = "SELECT p_uri FROM properties WHERE p_mimetype='" + SCRIPT_MIMETYPE + "'";
		logger.debug("QUERYING FOR SCRIPTS: " + query);
		Connection conn = ConnectionHandler.instance().getConnection();
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);
			String p_uri, xml;
			FSScript script;
			Document doc = null;
			while (rs.next()) {
				try {
					p_uri = rs.getString("p_uri");
					logger.debug("Getting script for uri: " + p_uri);
					doc = FSXMLRequestHandler.instance().getNodeProperties(p_uri,false);
					if (doc != null) {
						xml = doc.asXML();
						logger.debug("Got script!: " + xml);
						script = new FSScript(p_uri,xml);
						if (script != null) {
							scripts.put(p_uri, script);
						}
					}
				} catch (DocumentException de) {
					logger.error("",de);
				}
			}
			rs.close();
			st.close();
		} catch (Exception e) {
			logger.error("",e);
		} finally {			
			ConnectionHandler.instance().closeConnection(conn);			
		}

		return scripts;
	}

	public FSScript getScriptOfUri(String uri) {
		if (uri == null || uri.equals("")) {
			return null;
		}
		if (scripts.containsKey(uri)) {
			return scripts.get(uri);
		}
		return null;
	}

}