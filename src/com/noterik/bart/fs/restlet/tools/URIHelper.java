/**
 * This class has convenient functions for retreiving conceptual information
 * implied by Springfield REST URI's.
 *
 * @author Jaap Blom <j.blom@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.restlet.nodes.tools
 * @access private
 * @version $Id: URIHelper.java,v 1.7 2011-07-01 11:57:41 derk Exp $
 *
 */
package com.noterik.bart.fs.tools;

import com.noterik.bart.fs.type.ReferUriType;

@Deprecated
public class URIHelper {

	public static ReferUriType getReferUriType(String referUri){
		if(referUri == null){
			return null;
		}
		if(referUri.indexOf("java://") != -1){
			return ReferUriType.JAVA_URI;
		} else if(referUri.indexOf("http://") != -1){
			return ReferUriType.HTTP_URI;
		} else {
			return ReferUriType.FS_URI;
		}
	}
	
}