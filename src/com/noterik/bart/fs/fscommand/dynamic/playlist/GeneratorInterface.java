/* 
* GeneratorInterface.java
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
package com.noterik.bart.fs.fscommand.dynamic.playlist;

import org.dom4j.*;

/**
 * 
 * @author Daniel Ockeloen
 * 
 * Interface that is used for playlist generators, found in the generator package
 * its called from PlaylistGenerator once the correct one has been selected
 *
 */
public interface GeneratorInterface {

	// main call with the current presentation and the playlist we want
	public Element generate(Element pr,String wantedplaylist,Element params,Element domainvpconfig,Element fsxml);
	
}
