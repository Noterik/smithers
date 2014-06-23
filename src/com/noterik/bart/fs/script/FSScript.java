package com.noterik.bart.fs.script;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.GlobalConfig;
import com.noterik.bart.fs.script.exec.ExecutionQueueHandler;
import com.noterik.bart.fs.triggering.TriggerEvent;

/**
 * Filesystem script
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.triggering.script
 * @access private
 * @version $Id: FSScript.java,v 1.24 2011-06-24 08:00:06 derk Exp $
 *
 */
public class FSScript implements Observer, Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1100L;

	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(FSScript.class);
	
	/**
	 * ID (uri)  of this script
	 */
	private String id;

	/**
	 * action sets
	 */
	private List<ActionSet> actionsets;

	/**
	 * caching rules
	 */
	private List<CacheRule> c_rules;

	/**
	 * input
	 */
	private FSInput fsinput;

	/**
	 * output
	 */
	private FSOutput fsoutput;
	
	/**
	 * FIXME ugly as hell
	 * 
	 * booleans for type of requests
	 */
	private boolean get = false;
	public boolean get() { return get; }
	public void get(boolean get) { this.get=get; }
	
	private boolean put = false;
	public boolean put() { return put; }
	public void put(boolean put) { this.put=put; }
	
	private boolean post = false;
	public boolean post() { return post; }
	public void post(boolean post) { this.post=post; }
	
	private boolean delete = false;
	public boolean delete() { return delete; }
	public void delete(boolean delete) { this.delete=delete; }

	/**
	 * Parses xml into a FSScript
	 *
	 * @param xml
	 * @throws DocumentException
	 */
	public FSScript(String id, String xml) throws DocumentException {
		this(id,DocumentHelper.parseText(xml));
	}

	/**
	 * Parses xml into a FSScript
	 *
	 * @param doc
	 */
	public FSScript(String id, Document doc) {
		this.id = id;
		actionsets = new ArrayList<ActionSet>();
		c_rules = new ArrayList<CacheRule>();
		parseActionSets(doc);
		parseCacheRules(doc);
		fsinput = new FSInput(this);
		fsoutput = new FSOutput(this);

		// try to execute script
		//this.execute();
	}

	/**
	 * Returns the id of this script
	 * @return
	 */
	public String getID() {
		return id;
	}

	/**
	 * Get input of this script
	 * @return
	 */
	public FSInput getInput() {
		return fsinput;
	}

	/**
	 * Get output of this script
	 * @return
	 */
	public FSOutput getOutput() {
		return fsoutput;
	}

	/**
	 * Add an actionset to the list of actionsets
	 * @param set
	 */
	public void addActionSet(ActionSet set) {
		actionsets.add(set);
	}

	/**
	 * Remove an actionset from the list of actionsets
	 * @param set
	 */
	public void removeActionSet(ActionSet set) {
		actionsets.remove(set);
	}

	/**
	 * Add a cacherule to the list of cachingrules
	 * @param rule
	 */
	public void addCacheRule(CacheRule rule) {
		c_rules.add(rule);
	}

	/**
	 * Remove a cacherule from the list of cachingrules
	 * @param set
	 */
	public void removeCacheRule(CacheRule rule) {
		c_rules.remove(rule);
	}

	/**
	 * Parses documents for the actionsets
	 * @param doc
	 */
	private void parseActionSets(Document doc) {
		List<Node> nodeList = doc.selectNodes("//actionset");
		Node node;
		String uri;
		for(Iterator<Node> iter = nodeList.iterator(); iter.hasNext(); ) {
			node = iter.next();
			try {
				if(node instanceof Element) {
					Element el = (Element)node;
					uri = this.id + "/actionset/" + el.attributeValue("id", "");
					
					logger.debug("actionset uri: " + uri);					
					addActionSet(new ActionSet(this,uri,node.asXML()));
				}
			} catch(Exception e) {
				logger.error("",e);
			}
		}
	}

	/**
	 * Parse caching rules
	 * @param doc
	 */
	private void parseCacheRules(Document doc) {
		List<Node> nodeList = doc.selectNodes("//cacherule");
		Node node;
		CacheRule cRule;
		String uri;
		for(Iterator<Node> iter = nodeList.iterator(); iter.hasNext(); ) {
			node = iter.next();
			try {
				if(node instanceof Element) {
					Element el = (Element)node;
					uri = this.id + "/cacherule/" + el.attributeValue("id", "");
					
					logger.debug("cacherule uri: " + uri);
					
					long timeToLiveMilis = 0;
					try {
						timeToLiveMilis = Long.parseLong(el.valueOf("properties/timeToLiveMilis"));
					} catch(NumberFormatException e) {}
					cRule = new CacheRule(this,timeToLiveMilis);
					addCacheRule(cRule);
				}
			} catch(Exception e) {
				logger.error("",e);
			}
		}
	}

	/**
	 * Check if caching should be applied. If so, checks if caches have expired?
	 *
	 * TODO: this shizzl
	 *
	 * @param event
	 * @return
	 */
	public boolean cachingExpired(TriggerEvent event) {
		return true;
	}

	/**
	 * Point of incoming events
	 */
	public void update(Observable o, Object arg) {
		if(arg instanceof TriggerEvent){
			// TODO: put in queue			
			logger.debug(execute((TriggerEvent)arg));
		}
	}

	/**
	 * Checks incomming events and decides which actionsets apply,
	 * thus which actions to execute. Also, it checks if the caching
	 * rules apply before executing actionsets.
	 * @param event
	 * @return
	 */
	private String execute(TriggerEvent event){
		String response = "";

		logger.debug("Checking script: " + this.id);
		logger.debug("event uri: " + event.getUri());

		if(cachingExpired(event)) {
			logger.debug("checking action sets");
			// pass to all action sets
			for(ActionSet actionSet : actionsets) {
				if(actionSet.conditionsApply(event)) {
					logger.debug("executing: " + actionSet.getID());
					// FIXME this is the old solution
					if(GlobalConfig.instance().isThreadedScriptExec()){
						// this is the new solution
						actionSet.setTriggerEvent(event);
						ExecutionQueueHandler.instance().getQueue(event.getMethod()).addToQueue(actionSet);						
					} else {
						actionSet.execute(event);
					}
				} else {
					logger.debug("not executing");
				}
			}
		} else {
			logger.debug("cached");
		}
		return response;
	}

	/**
	 * TODO rewrite this for directly executing a script
	 * @return
	 */
	public String execute() {
		for(ActionSet actionSet : actionsets) {
			actionSet.execute();
		}
		return null;
	}

	@Override
	public boolean equals(Object o){
		if(o != null && o instanceof FSScript){
			if(((FSScript)o).getID() != null){
				if(((FSScript)o).getID().equals(id)){
					return true;
				}
			}
		}
		return false;
	}
	
	public void destroy() {
		for (ActionSet action : actionsets) {
			action.destroy();
		}
	}
}