/*
 * Created on Jan 27, 2009
 */
package com.noterik.bart.fs.action.common.test;

import org.apache.log4j.Logger;

import com.noterik.bart.fs.action.ActionAdapter;

public class WaitLongAction extends ActionAdapter {

	private static Logger logger = Logger.getLogger(WaitLongAction.class);
	
	@Override
	public String run() {
		logger.debug("Waiting for ten seconds");
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			logger.error("",e);
		}
		logger.debug(":::::::THE ACTION HAS COMPLETED:::::::");
		return null;
	}
	
}
