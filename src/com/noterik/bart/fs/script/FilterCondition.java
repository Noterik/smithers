package com.noterik.bart.fs.script;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.noterik.bart.fs.triggering.TriggerEvent;

/**
 * A condition that uses filters on trigger events to determine
 * the applicability
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.script
 * @access private
 * @version $Id: FilterCondition.java,v 1.9 2009-02-11 10:34:34 jaap Exp $
 *
 */
public class FilterCondition implements Condition{	

	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(FilterCondition.class);
	
	/**
	 * uri pattern
	 */
	private Pattern uri_pattern;

	/**
	 * method pattern
	 */
	private Pattern method_pattern;

	/**
	 * mimetye pattern
	 */
	private Pattern mimetype_pattern;

	/**
	 * Creates a new filter rule
	 *
	 * @param uri_pattern_str
	 * @param method_pattern_str
	 * @param mimetype_pattern_str
	 */
	public FilterCondition(String uri_pattern_str, String method_pattern_str, String mimetype_pattern_str) {
		this.uri_pattern = Pattern.compile(uri_pattern_str);
		this.method_pattern = Pattern.compile(method_pattern_str);
		this.mimetype_pattern = Pattern.compile(mimetype_pattern_str);
	}

	/**
	 * Check if this rule applies to the event that was triggered
	 *
	 * @param event
	 * @return
	 */
	public boolean applies(TriggerEvent event) {
		if(event==null) {
			return false;
		}

		Matcher matcher;

		logger.debug("checking mimetype");
		// mimetype
		matcher = mimetype_pattern.matcher(event.getMimeType());
		if(!matcher.find()) {
			logger.debug("MIMETYPE CHECK: FALSE");
			return false;
		}

		logger.debug("checking uri: " + event.getUri());
		// uri
		matcher = uri_pattern.matcher(event.getUri());
		if(!matcher.find()) {
			logger.debug("URI CHECK: FALSE" );
			return false;
		}
		logger.debug("checking method");
		// method
		matcher = method_pattern.matcher(event.getMethod());
		if(!matcher.find()) {
			logger.debug("METHOD CHECK: FALSE");
			return false;
		}

		return true;
	}

	public boolean applies() {
		// TODO Auto-generated method stub
		return false;
	}
	
	/**
	 * Hack for queueing system
	 * @param method
	 * @return
	 */
	public boolean methodPatternApplies(String method) {
		Matcher matcher = method_pattern.matcher(method);
		return matcher.find();
	}
}
