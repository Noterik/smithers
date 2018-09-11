/* 
* TriggerSystemQueue.java
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

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.log4j.Logger;

/**
 * A queue for incoming trigger events
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.triggering
 * @access private
 * @version $Id: TriggerSystemQueue.java,v 1.9 2010-06-08 09:06:41 derk Exp $
 *
 */
public class TriggerSystemQueue extends Observable implements Observer, Runnable {

	/**
	 * variable for stopping the queue thread
	 */
	private boolean stopped = false;
	
	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(TriggerSystemQueue.class);
	
	/**
	 * queue for trigger events
	 */
	private ArrayBlockingQueue<TriggerEvent> queue;
	
	/**
	 * Type of requests this queue takes
	 */
	private String method;
	
	/**
	 * Sole constructor
	 * 
	 * @param method the request method this queue accepts
	 * @param size the size of the queue
	 */
	public TriggerSystemQueue(String method, int size) {
		this.method = method;
		
		// init queue
		queue = new ArrayBlockingQueue<TriggerEvent>(size);
		
		// add shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				stopThread();
			}
		});
	}

	public void run() {
		logger.debug("Starting Trigger System Queue");
		
		TriggerEvent tEvent;
		while(!stopped) {
			try {
				// wait for incomming events and notify
				tEvent = queue.take();
				
				logger.debug("Handling event: " + tEvent.getUri());
				
				// notify
				notifyObservers(tEvent);
			} catch (Exception e) {
				logger.error("Error during event handle",e);
			}
		}
		
		logger.info("Stopping Trigger System Queue");
	}

	/**
	 * Stop this Queue from running
	 */
	public void stopThread() {
	    	logger.info("Stopping thread for "+this.method+" queue because a shutdownhook was activated");
		stopped = true;
	}
	
	/**
	 * Adds the trigger event to the queue (blocking)
	 */
	public void update(Observable o, Object arg) {
		if(arg instanceof TriggerEvent){
			try {
				// cast
				TriggerEvent tEvent = (TriggerEvent)arg;
				
				// check method
				if(this.method.equals(tEvent.getMethod())) {
					logger.debug("<<<<Adding event to " + this.method + " queue: " + tEvent.getUri() + ">>>>");
					queue.put(tEvent);
				}
			} catch (InterruptedException e) {
				logger.error("Error during update",e);
			}
		}
	}
	
	public void addObserver(Observer o) {
		super.addObserver(o);
		logger.debug("Added observer: " + o);
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
	
	/**
	 * Returns a snapshot of this queue's items
	 * 
	 * @return a snapshot of this queue's items
	 */
	public TriggerEvent[] toArray() {
		TriggerEvent[] teArray = null;
		synchronized(queue) {
			teArray = new TriggerEvent[queue.size()];
			queue.toArray(teArray);
		}
		return teArray;
	}

	/**
	 * Returns the size of this queue
	 * 
	 * @return the size of this queue
	 */
	public int size() {
		return queue.size();
	}
}
