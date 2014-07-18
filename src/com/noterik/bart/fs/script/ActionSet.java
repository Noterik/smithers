/* 
* ActionSet.java
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Timer;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import com.noterik.bart.fs.LazyHomer;
import com.noterik.bart.fs.action.Action;
import com.noterik.bart.fs.triggering.TriggerEvent;
import com.noterik.bart.fs.type.ReferUriType;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * Class to define what actions are performed when specific 
 * changes are recorded on certain resources.
 * 
 * This class contains a set of conditions/rules and a set
 * of actions. The actions are performed when the conditions
 * are satisfied.
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.script
 * @access private
 * @version $Id: ActionSet.java,v 1.14 2011-07-01 13:00:50 derk Exp $
 *
 */
public class ActionSet implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(ActionSet.class);
	
	/**
	 * ID used in the actions db table for serialization
	 */
	
	private int dbId;
	
	/**
	 * ID (uri) of this actionset
	 */
	private String id;
	
	/**
	 * Conditions that need to be satisfied
	 */
	private List<Condition> conditions;
	
	/**
	 * Timer conditions
	 */
	private List<Timer> timerConditions;
	
	/**
	 * Actions that need to be performed
	 */
	private List<Action> actions;
	
	/**
	 * pointer to the parent script
	 */
	private FSScript script; 

	private TriggerEvent triggerEvent;
	
	private ActionClassLoader actionClassLoader;
	/**
	 * Parses xml into a ActionSet
	 *
	 * @param xml
	 * @throws DocumentException
	 */
	public ActionSet(FSScript script, String id, String xml) throws DocumentException {
		this(script,id,DocumentHelper.parseText(xml));
	}

	/**
	 * Parses xml into a ActionSet
	 *
	 * @param doc
	 */
	public ActionSet(FSScript script, String id, Document doc) {
		this.script = script;
		this.id = id;
		conditions = new ArrayList<Condition>();
		timerConditions = new ArrayList<Timer>();
		actions = new ArrayList<Action>();
		parseActions(doc);
		parseConditions(doc);
	}
	
	/**
	 * Get id
	 */
	public String getID() {
		return id;
	}
	
	/**
	 * Add condition to the list of conditions
	 * @param cond
	 */
	public void addCondition(Condition cond) {
		conditions.add(cond);
	}
	
	/**
	 * Add timer condition to the list of timer conditions
	 * @param cond
	 */
	public void addTimerCondition(Timer timer) {
		timerConditions.add(timer);
	}
	
	/**
	 * Removes condition from the list of conditions
	 * @param cond
	 */
	public void removeCondition(Condition cond) {
		conditions.remove(cond);
	}
	
	/**
	 * Removes timer condition from the list of timer conditions
	 * @param cond
	 */
	public void removeTimerCondition(Timer timer) {
		timer.cancel();
		timer.purge();
		timerConditions.remove(timer);
	}
	
	/**
	 * Add an action to the list of actions
	 * @param action
	 */
	public void addAction(Action action) {
		actions.add(action);
	}
	
	/**
	 * Removes an action from the list of actions
	 * @param action
	 */
	public void removeAction(Action action) {
		actions.remove(action);
	}
	
	/**
	 * Do all conditions in the list of conditions apply
	 * @return
	 */
	public boolean conditionsApply(TriggerEvent event) {
		for(Condition cond : conditions) {
			if(!cond.applies(event)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Execute all actions
	 */
	public void execute(TriggerEvent event) {
		for(Action action : actions) {
			logger.debug("running action: " + action.toString());
			
			// copy action because of simultaneous execution
			Action cAction = null;
			try {
				cAction = action.getClass().newInstance();
			} catch (Exception e) {
				logger.error("Could not copy class",e);
				break;
			}
			cAction.setTriggerEvent(event);
			cAction.run();
		}
	}
		
	/**
	 * This function should only be called when the triggerEvent is set
	 */
	
	public void execute() {
		if(triggerEvent != null){
			for(Action action : actions) {
				logger.debug("running action: " + action.toString());
				action.setTriggerEvent(triggerEvent);
				action.run();
			}
		}
	}

	/**
	 * Parses document for conditions
	 * @param doc
	 */
	private void parseConditions(Document doc) {
		logger.debug("parsing conditions");
		
		List<Node> nodelist = doc.selectNodes("//condition");
		Node node;
		Condition cond;
		String type, milis, p_uri, p_method, p_mimetype;
		for(Iterator<Node> iter = nodelist.iterator(); iter.hasNext(); ) {
			node = iter.next();
			type = node.valueOf("properties/type");
			if(type.equals("filter")) {
				p_uri = node.valueOf("properties/uri");
				p_method = node.valueOf("properties/method");
				p_mimetype = node.valueOf("properties/mimetype");
				cond = new FilterCondition(p_uri,p_method,p_mimetype);
				
				// FIXME: ugly ass hell
				// set get/put/post/delete variables
				if( ((FilterCondition)cond).methodPatternApplies("GET") ) {
					script.get(true);
				}
				if( ((FilterCondition)cond).methodPatternApplies("PUT") ) {
					script.put(true);
				}
				if( ((FilterCondition)cond).methodPatternApplies("POST") ) {
					script.post(true);
				}
				if( ((FilterCondition)cond).methodPatternApplies("DELETE") ) {
					script.delete(true);
				}
				
				logger.debug("adding filter condition: uri=" + p_uri + ", method="+p_method+", mimetype="+p_mimetype);
				addCondition(cond);
			} else if (type.equals("periodically")){
				logger.info("Found periodically script "+this.id);
				//check if this smithers is configured to handle this timer
				String[] validDomains = LazyHomer.getTimerScriptDomains();
				boolean valid = false;
				
				String domain = URIParser.getDomainFromUri(this.id);
				
				for (String validDomain : validDomains) {
					if (validDomain.equals(domain)) {
						valid = true;
					}
				}
				
				if (valid) {
					milis = node.valueOf("properties/milis");
					TimerCondition timerCondition = new TimerCondition(Long.parseLong(milis), actions);
					Timer timer = new Timer();
					timer.schedule(timerCondition, 1000, Long.parseLong(milis));			
					addTimerCondition(timer);
					
					logger.debug("adding timer condition: milis=" + milis);
				} else {
					logger.debug("No matching domain for domain "+domain);
				}
			}
		}
	}
	
	/**
	 * Parses the input document for actions
	 * 
	 * @param doc
	 */
	private void parseActions(Document doc) {
		logger.debug("parsing actions");
		
		// get action nodes
		List<Node>actionlist = doc.selectNodes("//action");

		// loop through nodes
		Node node;
		Action action;
		Properties properties;
		String refer;
		String jarName;
		String className = "";
		Class<?> actionClass = null;
		for(Iterator<Node> iter1 = actionlist.iterator(); iter1.hasNext(); ) {
			node = iter1.next();
			action = null;

			// init action class
			refer = node.valueOf("@referid");

			try {
				refer = refer.substring(ReferUriType.JAVA_URI.getProtocol().length());
				
				//check if we need to load from additional jar
				if (refer.indexOf("@") != -1) {
					className = refer.substring(0, refer.indexOf("@"));
					jarName = refer.substring(refer.indexOf("@")+1);

					logger.info("Loading jar "+jarName+" for class "+className);
					actionClass = loadJar(jarName, className);		
				} else {
					className = refer;
					actionClass = Class.forName(className);
				}
				
				if (actionClass != null) {
					action = (Action)actionClass.newInstance();
				}
			} catch(Exception e) {
				logger.error("Error while initiating class: "+className+", for id: "+id+" "+e);
			}

			if(action!=null) {
				// TODO: get and set properties of action
				properties = null;
				action.setProperties(properties);
				
				// set uri
				String uri = id+"/action/"+node.valueOf("@id");
				action.setID(uri);
				
				// set script
				action.setScript(script);

				logger.debug("adding action: " + className + ", uri="+uri);
				
				// add to list of actions
				addAction(action);
			}
		}
	}
	
	/**
	 * Load class from external jar
	 * 
	 * @param jarName - the name of the jar to load from
	 * @param className - the class to load
	 * @return the requested class if successful, otherwise null
	 */
	private Class<?> loadJar(String jarName, String className) {
		Class<?> actionClass = null;
		
		if (actionClassLoader == null) {
			actionClassLoader = new ActionClassLoader();
		}
		try {
			actionClassLoader.setJar(jarName);
			actionClass = actionClassLoader.loadClass(className);
		} catch (ClassNotFoundException e) {
			logger.error("Class "+className+" not found "+e.toString());
		}
		return actionClass;
	}
	
	public void setTriggerEvent(TriggerEvent triggerEvent){
		this.triggerEvent = triggerEvent;
	}

	public int getDbId() {
		return dbId;
	}

	public void setDbId(int dbId) {
		this.dbId = dbId;
	}
	
	public void destroy() {
		for (Timer timer : timerConditions) {
			System.out.println("Smithers: destroying timer script");
			removeTimerCondition(timer);
		}
	}
}
