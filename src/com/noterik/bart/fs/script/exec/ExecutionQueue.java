/*
 * Created on Jan 27, 2009
 */
package com.noterik.bart.fs.script.exec;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.log4j.Logger;

import com.noterik.bart.fs.GlobalConfig;
import com.noterik.bart.fs.script.ActionSet;

/**
 * This class controls the execution of a particular event queue. (Each
 * TriggerSystemQueue has a single ExecutionQueue)
 * 
 * for each type of queue (POST, PUT, GET, DELETE) there is a maximum number of
 * concurrent threads configuratble (see config.xml).
 * 
 * IMPORTANT:
 * 
 * Next to this parameter it is important to look at the maximum number of db
 * connections (see config.xml). It is important that the sum of all max threads
 * is not higher than the amount of max db connections.
 * 
 * TODO persist the actions in the queue for recovery! (kicktomcat is run every
 * night!!)
 * 
 * 
 * 
 * TODO make sure on start up the persisted actions are recovered and requeued!
 * 
 * @author Jaap Blom
 */

public class ExecutionQueue implements Runnable {
	
	private static Logger logger = Logger.getLogger(ExecutionQueue.class);
	private static final int MAX_QUEUE_SIZE = 1000;
	private static final int MAX_WAIT = 300;
	private int maxThreads = 0;
	private ArrayBlockingQueue<ActionSet> actionSets = new ArrayBlockingQueue<ActionSet>(MAX_QUEUE_SIZE);
	private int numThreads = 0;
	private String method;

	public ExecutionQueue(String method) {
		this.method = method;
		if (method != null) {
			if ("DELETE".equals(method)) {
				maxThreads = GlobalConfig.instance().getMaxNumDELETEThreads();
			} else if ("GET".equals(method)) {
				maxThreads = GlobalConfig.instance().getMaxNumGETThreads();
			} else if ("POST".equals(method)) {
				maxThreads = GlobalConfig.instance().getMaxNumPOSTThreads();
			} else if ("PUT".equals(method)) {
				maxThreads = GlobalConfig.instance().getMaxNumPUTThreads();
			} else {
				logger.error("Invalid method defined " + method);
				throw new ExceptionInInitializerError();
			}
			Thread t = new Thread(this);
			t.start();
		} else {
			logger.error("No method defined " + method);
		}
	}

	public void run() {
		ActionSet as = null;
		long total, s = 0l;
		while (true) {
			try {
				if (numThreads < maxThreads) {
					as = actionSets.take();
				} else {
					as = null;
					logger.debug("TOO MANY THREADS (" + method + ") max=" + numThreads);
					try {
						// is this also the current thread of this class??
						Thread.sleep(200);
					} catch (InterruptedException e) {
						logger.error("",e);
					}
				}
			} catch (InterruptedException ex) {
				logger.error("",ex);
			}
			if (as != null) {
				logger.debug("<<<<<<<<<<<<<<<<EXECUTING FROM EXECUTION QUEUE>>>>>>>>>>>>>>");
				ActionThread at = new ActionThread(this, as);
				total = 0l;
				s = System.currentTimeMillis();
				while (!at.isDone()) {
					total = (System.currentTimeMillis() - s);
					if (total > MAX_WAIT) {
						// TODO register this action so we can later check up on it.						
						logger.debug("REGISTERING SLOW ACTION");
						break;
					}
				}
				logger.debug(total);
			}
		}
	}

	public void increaseNumberOfThreads() {
		numThreads++;
	}

	public void decreaseNumberOfThreads() {
		numThreads--;		
	}

	public void addToQueue(ActionSet as) {
		try {
			if(GlobalConfig.instance().useQueueRecovery()){
				// serialize the action in the db for possible recovery in case of a crash
				if(as.getDbId() <= 0){
					// so only persist if the action set is not already in the db
					int id = ExecutionQueueHandler.instance().serializeActionInDb(as, method);
					as.setDbId(id);
				}
			}
			actionSets.put(as);
		} catch (InterruptedException e) {
			logger.error("",e);
		}
	}	

}