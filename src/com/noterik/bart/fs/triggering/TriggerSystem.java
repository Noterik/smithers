/* 
* TriggerSystem.java
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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Observable;

import org.apache.log4j.Logger;

import com.noterik.bart.fs.script.FSScript;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * Triggering System
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @author Jaap Blom <j.blom@noterik.nl>
 * @author Pedro <pedro@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.triggering
 * @access private
 * @version $Id: TriggerSystem.java,v 1.18 2011-10-19 13:24:49 derk Exp $
 *
 */
public class TriggerSystem extends Observable {

	/**
	 * logger class
	 */
	private static Logger logger = Logger.getLogger(TriggerSystem.class);
	
	/**
	 * get queue
	 */
	private TriggerSystemQueue getQueue;
	private Thread getQueueThread;
	
	/**
	 * put queue
	 */
	private TriggerSystemQueue putQueue;
	private Thread putQueueThread;
	
	/**
	 * post queue
	 */
	private TriggerSystemQueue postQueue;
	private Thread postQueueThread;
	
	/**
	 * delete queue
	 */
	private TriggerSystemQueue deleteQueue;
	private Thread deleteQueueThread;
	
	public TriggerSystem(){
		super();
		
		// create new GET, PUT, POST and DELETE Queue's
		getQueue = new TriggerSystemQueue("GET",100000);
		getQueueThread = new Thread(getQueue);
		getQueueThread.start();
		
		putQueue = new TriggerSystemQueue("PUT",100000);
		putQueueThread = new Thread(putQueue);
		putQueueThread.start();
		
		postQueue = new TriggerSystemQueue("POST",100000);
		postQueueThread = new Thread(postQueue);
		postQueueThread.start();
		
		deleteQueue = new TriggerSystemQueue("DELETE",100000);
		deleteQueueThread = new Thread(deleteQueue);
		deleteQueueThread.start();
		
		// add to trigger system as observer
		this.addObserver(getQueue);
		this.addObserver(putQueue);
		this.addObserver(postQueue);
		this.addObserver(deleteQueue);
	}
	
	/** Getters	**/
	
	public TriggerSystemQueue getGetQueue() {
		return getQueue;
	}
	
	public TriggerSystemQueue getPutQueue() {
		return putQueue;
	}
	
	public TriggerSystemQueue getPostQueue() {
		return postQueue;
	}
	
	public TriggerSystemQueue getDeleteQueue() {
		return deleteQueue;
	}

	/**
	 * Triggers an event
	 *
	 * @param uri
	 * @param method
	 * @param mimeType
	 * @param requestData
	 */
	public void eventHappened(String uri, String method, String mimeType, String requestData){
		// debug info
		logger.debug("TRIGGER -- uri: " + uri + ", method: " + method);
		
		if (method.toUpperCase().contentEquals("GET")) {
			if (getQueueThread != null && getQueueThread.getState() == Thread.State.TERMINATED) {
				logger.info("TERMINATED GET QUEUE -> RESTARTING");
				getQueueThread = new Thread(getQueue);
				TriggerSystemManager manager = TriggerSystemManager.getInstance();
				
				// get all scripts
				Hashtable<String, FSScript> scripts = manager.getAllScripts();

				// add scripts to the triggering system
				String scriptUri;
				FSScript script;
				for (Iterator<String> iter = scripts.keySet().iterator(); iter.hasNext();) {
					scriptUri = iter.next();
					script = scripts.get(scriptUri);
					
					if(script.get()) {
						getGetQueue().deleteObserver(script);
						getGetQueue().addObserver(script);
					}
					logger.info("added script: " + scriptUri);
				}
				
				getQueueThread.start();
			}
		} else if (method.toUpperCase().contentEquals("PUT")) {
			if (putQueueThread != null && putQueueThread.getState() == Thread.State.TERMINATED) {
				logger.info("TERMINATED PUT QUEUE -> RESTARTING");
				putQueueThread = new Thread(putQueue);
				
				TriggerSystemManager manager = TriggerSystemManager.getInstance();
				
				// get all scripts
				Hashtable<String, FSScript> scripts = manager.getAllScripts();

				// add scripts to the triggering system
				String scriptUri;
				FSScript script;
				for (Iterator<String> iter = scripts.keySet().iterator(); iter.hasNext();) {
					scriptUri = iter.next();
					script = scripts.get(scriptUri);
					
					if(script.put()) {
						getPutQueue().deleteObserver(script);
						getPutQueue().addObserver(script);
					}
					logger.info("added script: " + scriptUri);
				}
				
				putQueueThread.start();
			}
		} else if (method.toUpperCase().contentEquals("POST")) {
			logger.info("postQueueThread:");
			logger.info(postQueueThread);
			logger.info(postQueueThread.getState());
			if (postQueueThread != null && postQueueThread.getState() == Thread.State.TERMINATED) {
				logger.info("TERMINATED POST QUEUE -> RESTARTING");
				postQueueThread = new Thread(postQueue);
				
				TriggerSystemManager manager = TriggerSystemManager.getInstance();
				
				// get all scripts
				Hashtable<String, FSScript> scripts = manager.getAllScripts();
				logger.info("number of scripts "+scripts.size());

				// add scripts to the triggering system
				String scriptUri;
				FSScript script;
				for (Iterator<String> iter = scripts.keySet().iterator(); iter.hasNext();) {
					scriptUri = iter.next();
					script = scripts.get(scriptUri);
					
					if(script.post()) {
						getPostQueue().deleteObserver(script);
						getPostQueue().addObserver(script);
					}
					logger.info("added script: " + scriptUri);
				}
				
				postQueueThread.start();
			}
		} else if (method.toUpperCase().contentEquals("DELETE")) {
			if (deleteQueueThread != null && deleteQueueThread.getState() == Thread.State.TERMINATED) {
				logger.info("TERMINATED DELETE QUEUE -> RESTARTING");
				deleteQueueThread = new Thread(deleteQueue);
				
				TriggerSystemManager manager = TriggerSystemManager.getInstance();
				
				// get all scripts
				Hashtable<String, FSScript> scripts = manager.getAllScripts();

				// add scripts to the triggering system
				String scriptUri;
				FSScript script;
				for (Iterator<String> iter = scripts.keySet().iterator(); iter.hasNext();) {
					scriptUri = iter.next();
					script = scripts.get(scriptUri);
					
					if(script.delete()) {
						getDeleteQueue().deleteObserver(script);
						getDeleteQueue().addObserver(script);
					}
					logger.info("added script: " + scriptUri);
				}
				
				deleteQueueThread.start();
			}
		}
		
		// create event and notify observers
		TriggerEvent tEvent = new TriggerEvent(uri, method, mimeType, requestData);
		notifyObservers(tEvent);
	}

	/**
	 * Notifies observers
	 */
	public void notifyObservers(){
		setChanged();
		super.notifyObservers();
	}

	/**
	 * Notifies observers
	 */
	public void notifyObservers(Object arg){
		setChanged();
		super.notifyObservers(arg);
	}
}