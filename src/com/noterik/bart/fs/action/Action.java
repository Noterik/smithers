/* 
* Action.java
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
package com.noterik.bart.fs.action;

import java.io.Serializable;
import java.util.Properties;

import com.noterik.bart.fs.script.FSScript;
import com.noterik.bart.fs.triggering.TriggerEvent;

public interface Action extends Serializable{

	public String run();

	public void setProperties(Properties properties);

	public void setID(String id);

	public void setTriggerEvent(TriggerEvent event);

	public void setScript(FSScript script);

}