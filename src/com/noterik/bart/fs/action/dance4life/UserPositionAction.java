package com.noterik.bart.fs.action.dance4life;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;

/**
 * Select current position for all users
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.action.dance4life
 * @access private
 * @version $Id: UserPositionAction.java,v 1.15 2009-02-11 10:34:34 jaap Exp $
 *
 */
public class UserPositionAction extends ActionAdapter {
	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(ViewCountAction.class);
	
	/**
	 * views
	 */
	private static TotalViewsSorted views = TotalViewsSorted.instance();
	
	@Override
	public String run() {	
		if(true) return null;
		try {
			logger.error("UserPositionAction: starting");
			
			// get all scores in a sorted way
			List<String> pIDs = views.getTopN(Integer.MAX_VALUE);
			
			// check return
			if(pIDs==null) {
				logger.error("UserPositionAction: list was null");
				return null;
			}			
			
			// add the position the every presentation
			String uri;
			int i=1;
			for(Iterator<String> iter = pIDs.iterator(); iter.hasNext(); i++) {
				uri = iter.next();
				addScoreToPresentations(uri, i);
			}
		} catch (Exception e) {
			logger.error(e);
		}
		
		return null;
	}
	
	/**
	 * Add a score to a single presentation
	 * @param uri
	 * @param position
	 */
	private void addScoreToPresentations(String uri, int position) {
		logger.debug("UserPositionAction: adding " + uri + " -- position " + position);
		logger.error("UserPositionAction: adding " + uri + " -- position " + position);
		try {				
			// store position
			FSXMLRequestHandler.instance().updateProperty(uri+"/properties/position", "position", ""+position, "PUT", true);
		} catch(Exception e) {
			logger.debug("UserPositionAction: exception during adding scores");
			logger.error(e);
		}
	}
}
