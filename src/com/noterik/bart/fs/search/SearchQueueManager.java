/* 
* SearchQueue.java
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
