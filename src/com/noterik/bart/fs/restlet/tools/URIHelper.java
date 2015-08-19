/* 
* ReferUriType.java
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
/*
 * implied by Springfield REST URI's.
 *
 * @author Jaap Blom <j.blom@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.restlet.nodes.tools
 * @access private
 * @version $Id: URIHelper.java,v 1.7 2011-07-01 11:57:41 derk Exp $
 *
 */
package com.noterik.bart.fs.restlet.tools;

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