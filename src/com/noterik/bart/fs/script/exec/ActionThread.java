/*
 * Created on Jan 27, 2009
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