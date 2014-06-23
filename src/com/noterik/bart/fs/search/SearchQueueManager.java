/*
 * Created on Feb 10, 2009
 */
package com.noterik.bart.fs.search;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * This class manages the search queues for each domain. Each domain only has
 * one instance of a search queue. The search queue is a threaded object that
 * will handle incoming search requests (triggered by the xml/search.xml script).
 * @author Jaap Blom
 */


public class SearchQueueManager {
	
	
	private static Logger logger = Logger.getLogger(SearchQueueManager.class);
	private static SearchQueueManager instance;
	private Map<String, SearchQueue> searchQueues;
	
	private SearchQueueManager(){
		searchQueues = new HashMap<String, SearchQueue>();
	}
	
	public static SearchQueueManager instance(){
		if(instance == null){
			instance = new SearchQueueManager();
		}
		return instance;
	}
	
	public SearchQueue getSearchQueueOfDomain(String domain){
		logger.debug("Getting queue for domain: " + domain);
		if(searchQueues.containsKey(domain)){
			return searchQueues.get(domain);
		} else {
			SearchQueue sq = new SearchQueue();
			searchQueues.put(domain, sq);
			return sq;
		}
	}

}
