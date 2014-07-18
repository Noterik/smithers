/* 
* CachingResource.java
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
package com.noterik.bart.fs.restlet;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.restlet.data.Form;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import com.noterik.bart.fs.GlobalConfig;
import com.noterik.bart.fs.cache.CacheHandler;

/**
 * Resource to see some caching properties.
 * 
 * Resource uri:
 * 		/caching
 * 
 * Agruments:
 *  	name (optional)		The name of the cache to inspect. Possible names are all (default), properties, children and referred.
 *  	action (optional)	The action to perform on the cache. Possible actions are stats (default), show, clear, clearstats, enable and disable. 
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2009
 * @package com.noterik.bart.fs.restlet
 * @access private
 * @version $Id: CachingResource.java,v 1.4 2011-11-21 11:15:59 derk Exp $
 *
 */
public class CachingResource extends ServerResource {

	// allowed actions: GET 
	public boolean allowPut() {return false;}
	public boolean allowPost() {return false;}
	public boolean allowGet() {return true;}
	public boolean allowDelete() {return false;}
	
	/**
	 * GET
	 */
	@Get
    public Representation getRepresentation() {
		String responseBody = "";
		
		// cache handlers
		//CacheHandler propertyCacheHandler = GlobalConfig.instance().getPropertyCacheHandler();
		//CacheHandler childCacheHandler = GlobalConfig.instance().getChildCacheHandler();
		//CacheHandler referredCacheHandler = GlobalConfig.instance().getReferredCacheHandler();
		
		// get parameters
		Form qForm = getRequest().getResourceRef().getQueryAsForm();
		String name = qForm.getFirstValue("name","all"); 
		String action = qForm.getFirstValue("action","stats");
		
		// check parameters
		if(name==null) {
			responseBody = "please provide correct cache name (document, properties, children, referred, all).";
			Representation entity = new StringRepresentation(responseBody);
	        return entity;
		}
		
		// get cache handler(s)
		List<CacheHandler> cHandlerList = new ArrayList<CacheHandler>();
		if(name.equals("properties")) {
		//	cHandlerList.add(propertyCacheHandler);
		} else if(name.equals("children")) {
		//	cHandlerList.add(childCacheHandler);
		} else if(name.equals("referred")) {
		//	cHandlerList.add(referredCacheHandler);
		} else if(name.equals("all")) {
		//	cHandlerList.add(propertyCacheHandler);
		//	cHandlerList.add(childCacheHandler);
		//	cHandlerList.add(referredCacheHandler);
		}
		
		// check cache handler
		if(cHandlerList.size()==0) {
			responseBody = "please provide correct cache name (document, properties, children, referred, all).";
			Representation entity = new StringRepresentation(responseBody);
	        return entity;
		}
		
		// do action
		CacheHandler cHandler;
		if(action.equals("stats")) {
			for(Iterator<CacheHandler> iter = cHandlerList.iterator(); iter.hasNext(); ) {
				cHandler = iter.next();
				responseBody += getStringStats(cHandler) + "\n\n\n";
			}
		} 
		else if(action.equals("show")) {
			for(Iterator<CacheHandler> iter = cHandlerList.iterator(); iter.hasNext(); ) {
				cHandler = iter.next();
				responseBody += getStringItems(cHandler) + "\n\n\n";
			}
		} 
		else if(action.equals("clear")) {
			for(Iterator<CacheHandler> iter = cHandlerList.iterator(); iter.hasNext(); ) {
				cHandler = iter.next();
				
				// get data
				String cName = cHandler.getName();
				int numItems =  cHandler.getKeys().size();
				
				// clear cache
				cHandler.deleteAll();
				
				responseBody += cName+":\n";
				responseBody += "Cleared "+numItems+" items. \n\n\n";
			}
		}
		
		else if(action.equals("clearstats")) {
			for(Iterator<CacheHandler> iter = cHandlerList.iterator(); iter.hasNext(); ) {
				cHandler = iter.next();
				cHandler.clearStatistics();
			}
		}
		else if(action.equals("disable")) {
			for(Iterator<CacheHandler> iter = cHandlerList.iterator(); iter.hasNext(); ) {
				cHandler = iter.next();
				cHandler.setEnabled(false);
			}
		}
		else if(action.equals("enable")) {
			for(Iterator<CacheHandler> iter = cHandlerList.iterator(); iter.hasNext(); ) {
				cHandler = iter.next();
				cHandler.setEnabled(true);
			}
		}
		else {
			responseBody = "please provide correct action (stats, show, clear, clearstats, enable, disable).";
			Representation entity = new StringRepresentation(responseBody);
	        return entity;
		}
		
		// return
		Representation entity = new StringRepresentation(responseBody);
        return entity;
	}
	
	/**
	 * Returns a String representation of the statistics of a ConnectionHandler.
	 * 
	 * @param cHandler	The ConnectionHandler
	 * @return			A String representation of the statistics of a ConnectionHandler.
	 */
	private static String getStringStats(CacheHandler cHandler) {
		StringBuffer sb = new StringBuffer();
		
		// get data
		String name = cHandler.getName();
		long hits = cHandler.getStatistics().getCacheHits();
		long misses = cHandler.getStatistics().getCacheMisses();
		int numItems = cHandler.getKeys().size();
		String status = cHandler.getEnabled() ? "enabled" : "disabled";
		
		// percentages
		double hitRate = hits/(double)(hits+misses);
		double misRate = 1-hitRate;
		
		// format in percentages
		NumberFormat nFormat = NumberFormat.getPercentInstance();
		
		// add data
		sb.append(name + ": \n");
		sb.append(cHandler + "\n\n");
		sb.append("-status \t\t\t"+status+"\n");
		sb.append("-number of items \t\t"+numItems+"\n");
		sb.append("-cache hits \t\t\t"+hits+" ("+nFormat.format(hitRate)+") \n");
		sb.append("-cache misses \t\t\t"+misses+" ("+nFormat.format(misRate)+") \n");
		
		return sb.toString();
	}
	
	/**
	 * Returns an XML representation of the statistics of a ConnectionHandler.
	 * 
	 * @param cHandler	The ConnectionHandler
	 * @return			An XML representation of the statistics of a ConnectionHandler.
	 */
	private static String getXMLStats(CacheHandler cHandler) {
		// TODO
		return null;
	}
	
	/**
	 * Returns a String representation of the items in a ConnectionHandler.
	 * 
	 * @param cHandler	The ConnectionHandler
	 * @return			A String representation of the items in a ConnectionHandler.
	 */
	private static String getStringItems(CacheHandler cHandler) {
		StringBuffer sb = new StringBuffer();
		
		// get data
		String name = cHandler.getName();
		List<Object> keys = cHandler.getKeys();
		int size = keys.size();
		String status = cHandler.getEnabled() ? "enabled" : "disabled";
		
		// add data
		sb.append(name + ": \n");
		sb.append("-status \t\t\t"+status+"\n");
		sb.append("-number of items \t "+size+"\n");
		if(cHandler.getEnabled()) {
			for(Iterator<Object> iter = keys.iterator(); iter.hasNext(); ) {
				sb.append("-"+iter.next()+"\n");
			}
		}
		
		return sb.toString();
	}
	
	/**
	 * Returns an XML representation of the items in a ConnectionHandler.
	 * 
	 * @param cHandler	The ConnectionHandler
	 * @return			An XML representation of the items in a ConnectionHandler.
	 */
	private static String getXMLItems(CacheHandler cHandler) {
		// TODO
		return null;
	}
}
