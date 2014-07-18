/* 
* PlaylistGenerator.java
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

import org.apache.log4j.Logger;

import org.dom4j.Element;
import org.w3c.dom.Node;

import com.noterik.bart.fs.fscommand.dynamic.config.flash;
import com.noterik.bart.fs.fscommand.dynamic.playlist.generators.FilterLayer;
import com.noterik.bart.fs.fscommand.dynamic.playlist.generators.OpenTagging;
import com.noterik.bart.fs.fscommand.dynamic.playlist.generators.RandomLayer;
import com.noterik.bart.fs.fscommand.dynamic.playlist.generators.ResolutionData;
import com.noterik.bart.fs.fscommand.dynamic.playlist.generators.RestrictedLayer;
import com.noterik.bart.fs.fscommand.dynamic.playlist.generators.SelectedLayer;

/**
 * 
 * @author Daniel Ockeloen 
 *
 */
public class PlaylistGenerator {
	
	/** Logger */
	private static Logger logger = Logger.getLogger(flash.class);
	
	
	// a normal case on a string is not possible so create a list to allow for a switch
	public enum generators { selected,filter,restricted,tagging,random,resolutiondata; }
	
	public static Element generate(Element pr,String wantedplaylist, Element params,Element domainvpconfig,Element fsxml) {
		// find the correct generator based on the wanted playlist
		// it doesn't check for settings in smithers yet.
		GeneratorInterface generator = null;
		
		// use a switch to select
		switch (generators.valueOf(wantedplaylist)) {
			case selected : generator = new SelectedLayer(); break;
			case filter : generator = new FilterLayer(); break;
			case restricted : generator = new RestrictedLayer(); break;
			case tagging : generator = new OpenTagging(); break;
			case random : generator = new RandomLayer(); break;
			case resolutiondata : generator = new ResolutionData(); break;
		}
		// if we found one then call it
		if (generator!=null) pr = generator.generate(pr, wantedplaylist, params, domainvpconfig,fsxml);
		
		// return the presentation
		return pr;
	}

}
