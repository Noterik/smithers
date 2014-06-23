package com.noterik.bart.fs.script;

import java.io.Serializable;

import com.noterik.bart.fs.triggering.TriggerEvent;

/**
 * 
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.script
 * @access private
 * @version $Id: Condition.java,v 1.2 2009-02-11 10:34:34 jaap Exp $
 *
 */
public interface Condition extends Serializable {
	public boolean applies();
	public boolean applies(TriggerEvent event);
}
