/* 
* WaitLongAction.java
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
