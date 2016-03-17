/* 
* TimerCondition.java
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
			try {
				//as actions are initiated by a timer provide empty triggerevent
				TriggerEvent triggerEvent = new TriggerEvent(null, null, null, null);
				action.setTriggerEvent(triggerEvent);
				action.run();
			} catch (RuntimeException e) {
				logger.error("Uncaught runtime exception ",e);
				return;
			} catch (Throwable e) {
				logger.error("Unrecoverable error",e);
			}
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
