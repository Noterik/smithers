/* 
* ActionThread.java
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
package com.noterik.bart.fs.script.exec;

import org.apache.log4j.Logger;

import com.noterik.bart.fs.GlobalConfig;
import com.noterik.bart.fs.script.ActionSet;

public class ActionThread implements Runnable{
	
	private static Logger logger = Logger.getLogger(ActionThread.class);
	private ActionSet as;
	private boolean done = false;
	private ExecutionQueue queue;
	
	public ActionThread(ExecutionQueue queue, ActionSet as){
		this.queue = queue;
		this.as = as;
		Thread t = new Thread(this);
		t.start();
	}
	
	public void run() {
		logger.debug(":::::::RUNNING THE ACTION ("+as+"):::::::");
		// add 1 to the number of total threads of the queue
		queue.increaseNumberOfThreads();
		as.execute();
		// substract 1 from the number of total threads of the queue (because it is done)
		queue.decreaseNumberOfThreads();
		if(GlobalConfig.instance().useQueueRecovery()){
			// delete the serialized action from the recovery table as it has finished
			ExecutionQueueHandler.instance().deleteSerializedActionFromDb(as);
		}
		done = true;
	}
	
	public boolean isDone(){
		return done;
	}
	
}