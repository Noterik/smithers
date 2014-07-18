/* 
* MonitoringResource.java
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

import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import com.noterik.bart.fs.db.ConnectionHandler;

/**
 * Resource used for monitoring purposes
 * 
 * Overview of smithers resources such as Thread, Connections, etc
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2009
 * @package com.noterik.bart.fs.restlet
 * @access private
 * @version $Id: MonitoringResource.java,v 1.3 2011-11-21 11:15:59 derk Exp $
 *
 */
public class MonitoringResource extends ServerResource {

	// allowed actions: GET 
	public boolean allowPut() {return false;}
	public boolean allowPost() {return false;}
	public boolean allowGet() {return true;}
	public boolean allowDelete() {return false;}
	
	/**
	 * GET
	 */
	@Get
	public Representation doGet() {
		String responseBody = "";
		
		// get open connections
		int numOpenConnections = ConnectionHandler.instance().getNumberOfOpenConnections();
		responseBody += "Active connections: "+numOpenConnections+"\n";
		
		// return
		Representation entity = new StringRepresentation(responseBody);
        return entity;
	}
}
