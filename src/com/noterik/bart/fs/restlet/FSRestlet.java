/* 
* FSRestlet.java
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

import org.restlet.Context;
import org.restlet.routing.Router;

import com.noterik.bart.fs.dns.DNS;
import com.noterik.bart.fs.legacy.restlet.FSFileResource;
import com.noterik.bart.fs.restlet.test.TestingResource;

public class FSRestlet extends Router {

	public FSRestlet(Context cx) {
		super(cx);
		
		// set routing mode
		this.setRoutingMode(MODE_BEST_MATCH);
		
		// for the ingest
		this.attach("/domain/{domain}/ingest/{order}/input", FSFileResource.class);	
		this.attach("/domain/{domain}/fsingest", FSSimpleIngestResource.class);
		
		// logging purposes
		this.attach("/logging",LoggingResource.class);
		
		// caching purposes
		this.attach("/caching",CachingResource.class);
		
		// for system monitoring purposes
		this.attach("/monitor",MonitoringResource.class);
		
		// for internal trigger queue monitoring
		this.attach("/queue",FSQueueResource.class);
		
		// for system testing purposes
		this.attach("/test",TestingResource.class);
		
		// for dns calls
		this.attach("/domain/{domain}/_{identifier}", DNS.class);
		
		// for the new property node handler style
		this.attachDefault(FSResource.class);
	}

}