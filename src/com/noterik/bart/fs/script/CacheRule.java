/* 
* CacheRule.java
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
