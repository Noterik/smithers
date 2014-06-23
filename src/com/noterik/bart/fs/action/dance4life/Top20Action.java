package com.noterik.bart.fs.action.dance4life;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.noterik.bart.fs.action.ActionAdapter;

/**
 * Gets the best viewed presentations
 * 
 * IMPORTANT: switched to database solution since lucene cannot 
 * handle a resultset larger than 1024
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.action.dance4life
 * @access private
 * @version $Id: Top20Action.java,v 1.7 2008-12-18 09:31:14 derk Exp $
 *
 */
public class Top20Action extends ActionAdapter {
	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(Top10Action.class);
	
	/**
	 * views
	 */
	private static TotalViewsSorted views = TotalViewsSorted.instance();
	
	@Override
	public String run() {	
		if(true) return null;
		try {
			logger.error("Top20Action: running top20action");
			
			// get top 20
			List<String> top20 = views.getTopN(20);
			
			// check return
			if(top20==null) {
				logger.error("Top20Action: list was null");
				return null;
			}
			
			// create xml;
			String xml = "<fsxml><properties />";
			int i= 1;
			for(Iterator<String> iter = top20.iterator(); iter.hasNext(); i++) {
				xml += "<result id='"+i+"' referid='"+iter.next()+"' />";
			}
			xml += "</fsxml>";
			
			logger.error("Top20Action: xml " + xml);
			
			// store in output
			script.getOutput().setOutput(xml);
		} catch (Exception e) {
			logger.error(e);
		}
		
		return null;
	}
}