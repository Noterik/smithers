/*
 * Created on Aug 11, 2008
 */
package com.noterik.bart.fs.action;

import java.util.Properties;

import com.noterik.bart.fs.script.FSScript;
import com.noterik.bart.fs.triggering.TriggerEvent;

public class ActionAdapter implements Action{

	protected Properties properties;
	protected String id;
	protected TriggerEvent event;
	protected FSScript script;

	public String run() {
		return null;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public void setID(String id){
		this.id = id;
	}

	public void setTriggerEvent(TriggerEvent event){
		this.event = event;
	}

	public void setScript(FSScript script) {
		this.script = script;
	}

}