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