package com.noterik.bart.fs.script;

import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.noterik.bart.fs.action.Action;
import com.noterik.bart.fs.triggering.TriggerEvent;

/**
 * Timer condition
 * 
 * This condition applies once in every n miliseconds. After
 * it has applied, it does not apply for at least n miliseconds.
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.script
 * @access private
 * @version $Id: TimerCondition.java,v 1.5 2008-12-18 09:31:14 derk Exp $
 *
 */
public class TimerCondition extends TimerTask implements Condition {
	private static final long serialVersionUID = 1L;
	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(TimerCondition.class);

	/**
	 * The number of miliseconds before this condition is met again
	 */
	private long milis;
	
	private List<Action> actions;
	
	public TimerCondition(long milis, List<Action> actions) {
		this.milis = milis;
		this.actions = actions;
	}
	
	public void run() {
		for(Action action : actions) {
			logger.debug("running action: " + action.toString());
			//as actions are initiated by a timer provide empty triggerevent
			TriggerEvent triggerEvent = new TriggerEvent(null, null, null, null);
			action.setTriggerEvent(triggerEvent);
			action.run();
		}
	}
	
	//actions are handled by the timertask
	public boolean applies() {
		return false;
	}

	//actions are handled by the timertask
	public boolean applies(TriggerEvent event) {
		return false;
	}

}
