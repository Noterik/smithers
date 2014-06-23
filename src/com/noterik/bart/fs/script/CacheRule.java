package com.noterik.bart.fs.script;

import java.io.Serializable;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

public class CacheRule implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 16666L;

	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(CacheRule.class);
	
	/**
	 * Number of miliseconds that the cached item may live
	 */
	private long timeToLiveMilis;
	
	/**
	 * Is the caching eternal
	 */
	private boolean eternal;
	
	/**
	 * parent script of this cache rule
	 */
	private FSScript script;
	
	/**
	 * timer stuff
	 */
	private Timer timer;
	private RunScriptTask task;
	
	public CacheRule(FSScript script, long timeToLiveMilis) {
		this(script,timeToLiveMilis,false);
	}	
	public CacheRule(FSScript script, long timeToLiveMilis, boolean eternal) {
		this.timeToLiveMilis = timeToLiveMilis;
		this.eternal = eternal;
		this.script = script;
		
		timer = new Timer();
		task = new RunScriptTask();
		timer.schedule(task, 10000, timeToLiveMilis);
	}
	
	private class RunScriptTask extends TimerTask {
		@Override
		public void run() {
			logger.debug("running timer task");
			script.execute();
		}
	}
}
