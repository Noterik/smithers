package com.noterik.bart.fs.action.dance4life;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;

public class TotalViewsSorted {
	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(TotalViewsSorted.class);
	
	/**
	 *  sorted set
	 */
	private static SortedSet<Pair> sortedSet = Collections.synchronizedSortedSet(new TreeSet<Pair>());
	
	/**
	 * instance
	 */
	private static TotalViewsSorted instance = new TotalViewsSorted();
	public static TotalViewsSorted instance() {return instance;}
	
	/**
	 * init boolean
	 */
	private boolean initialized = false;
	
	/**
	 *  timer for redo
	 */
	private Timer timer = new Timer();
	
	/**
	 * redo cycle (10min)
	 */
	private static final long REDO_MILIS = 60000;
	
	/**
	 * private constructor
	 */
	private TotalViewsSorted() {
		// create timertask that redoes the sorted set
		TimerTask task = new TimerTask(){
			@Override
			public void run() {
				redo();
			}			
		};
		
		// schedule task
		//timer.schedule(task, 10000); // disabled (contest finished)
	}
	
	/**
	 * Get top N
	 * @param n
	 * @return
	 */
	public List<String> getTopN(int n) {
		// don't return if sorted set was never filled
		if(!initialized) {
			return null;
		}
		
		List<String> topAll = new ArrayList<String>();
		Pair pair;
		for(Iterator<Pair> iter = sortedSet.iterator(); iter.hasNext(); ) {
			pair = iter.next();
			topAll.add(0,pair.name);
		}
		
		// invert
		List<String> topN = new ArrayList<String>();
		int i=0;
		for(Iterator<String> iter = topAll.iterator(); iter.hasNext() && i<n; i++) {
			topN.add(iter.next());
		}
		
		return topN;
	}
	
	/**
	 * Get the position in the map of this uri
	 * @param uri
	 * @return
	 */
	public int getPosition(String uri) {
		// don't return if sorted set was never filled
		if(!initialized) {
			return -1;
		}
		
		int i=0;
		for(Iterator<Pair> iter = sortedSet.iterator(); iter.hasNext(); i++) {
			if(iter.next().name.equals(uri)) {
				break;
			}
		}
		// invert
		return sortedSet.size() -i;
	}
	
	/**
	 * redoes the sorting of the map
	 */
	private void redo() {
		logger.debug("TotalViewsSorted: redoing sorted set");
		logger.error("TotalViewsSorted: redoing sorted set");
		
		// debug print complete set
		logger.error("TotalViewsSorted: set " + sortedSet.toString());
		
		// all user names
		String uri = "/domain/dance4life/user";
		//String uri = "/domain/examples/user";
		Document doc = FSXMLRequestHandler.instance().getNodePropertiesByType(uri,0,0,-1);
		
		// loop over users
		Document uDoc;
		Element elem1, elem2;
		String userid, presUri;
		int views_total;
		for(Iterator<Element> iter = doc.getRootElement().elementIterator(); iter.hasNext(); ) {
			elem1 = iter.next();
			if(elem1.getName().equals("user")) {
				userid = elem1.attributeValue("id");
				
				logger.debug("TotalViewsSorted: userid " + userid);
				
				if(userid!=null) {
					// get this users data
					uDoc = FSXMLRequestHandler.instance().getNodePropertiesByType(uri+"/"+userid+"/presentation");
					
					// get views total
					elem2 = (Element)uDoc.selectSingleNode("//views_total");
					if(elem2!=null) {
						views_total = Integer.parseInt(elem2.getText());
						
						// get uri
						presUri = uri + "/" + userid + "/presentation/" + elem2.getParent().getParent().attributeValue("id");
						
						logger.debug("TotalViewsSorted: adding " + presUri + " -- " + views_total);
						logger.error("TotalViewsSorted: adding " + presUri + " -- " + views_total);
						
						// add to map
						Pair pair = new Pair(presUri, new Integer(views_total));
						sortedSet.remove( pair );
						sortedSet.add( pair );
					}
				}
			}
		}
		initialized = true;
	}
	
	/**
	 * Comparable pair
	 *
	 * @author Derk Crezee <d.crezee@noterik.nl>
	 * @copyright Copyright: Noterik B.V. 2008
	 * @package com.noterik.bart.fs.action.dance4life
	 * @access private
	 * @version $Id: TotalViewsSorted.java,v 1.9 2009-02-11 10:34:34 jaap Exp $
	 *
	 */
	class Pair implements Comparable {
	    private final String name;
	    private final Integer number;
	 
	    public Pair(String name, Integer number) {
	        this.name = name;
	        this.number = number;
	    }
	 
	    public int compareTo(Object o) {
	        if (o instanceof Pair) {
	            int cmp = number.compareTo(((Pair) o).number);
	            if (cmp != 0) {
	                return cmp;
	            }
	            return name.compareTo(((Pair) o).name);
	        }
	        throw new ClassCastException("Cannot compare Pair with " + o.getClass().getName());
	    }
	    
	    @Override
	    public boolean equals(Object o) {
			if(o!=null) {
				if(o instanceof Pair) {
					return name.equals(((Pair)o).name);
				}
			}
			return false;
	    }
	 
	    public String toString() {
	        return name + ' ' + number;
	    }
	}
}
