/* 
* RandomLayer.java
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
package com.noterik.bart.fs.fscommand.dynamic.playlist.generators;

import java.util.*;

import org.apache.log4j.Logger;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.fscommand.dynamic.config.flash;
import com.noterik.bart.fs.fscommand.dynamic.playlist.GeneratorInterface;
import com.noterik.bart.fs.fscommand.dynamic.playlist.PlaylistGenerator.generators;
import com.noterik.bart.fs.fscommand.dynamic.playlist.util.TimeLine;

import org.dom4j.*;

/**
 * 
 * @author Daniel Ockeloen
 * 
 * Create a videoplaylist based on the layer name and filter , It generates video blocks
 * based on found blocks.
 * 
 * all events are shifted based on that (not implemented yet).
 *
 */
public class RandomLayer implements GeneratorInterface {

	/** Logger */
	private static Logger logger = Logger.getLogger(flash.class);
	

	
	/*
	 * the incoming generate call from either quickpresentation or direct restlet
	 * 
	 * @see com.noterik.bart.fs.fscommand.dynamic.playlist.GeneratorInterface#generate(org.dom4j.Node, java.lang.String)
	 */
	public Element generate(Element pr,String wantedplaylist, Element params,Element domainvpconfig,Element fsxml) {
		// select the original playlist (now hardcoded to playlist with id 1).
		Element videonode = (Element) pr.selectSingleNode("videoplaylist/video[@id='1']");
		
		if (videonode!=null) {
			// if its a videonode than detach it from the document (delete it) since we are
			// doing a rewrite on the document not generating a new one
			videonode.detach(); // remove it from the presentation
			
			// we need the referid, so we can set this in all the new video's, This is
			// now limited to one video as imput and needs a rewrite to support more input 
			// video's in a playlist.
			String referid = videonode.attributeValue("referid");
			
			// select the selected layer.
			int numberofparts = 20;
			int duration = 1000;
			int range = 2950;

			// ok lets loop all the blocks and create video nodes based on them
			int idcounter = 1; // we need to number the new video nodes
		    Random rnd = new Random();
			for (int i = 0;i<numberofparts;i++) {
		
				// create the new video node
				Element newvideonode = DocumentHelper.createElement("video");
				
				// set the id and aim it to our original video
				newvideonode.addAttribute("id", ""+idcounter++);
				newvideonode.addAttribute("referid", referid);
				
				// create the properties and set them (this can be done easer?)
				Element p = DocumentHelper.createElement("properties");
				Element st = DocumentHelper.createElement("starttime");
				Element du = DocumentHelper.createElement("duration");
				int starttime = rnd.nextInt(range);
				System.out.println("ID="+idcounter+" RANDOM="+starttime);
				st.setText(""+(starttime*1000));
				du.setText(""+duration);
				p.add(st);
				p.add(du);
				
				// add the properties to the video node so it plays just that part.
				newvideonode.add(p);
				
				// we deleted (detached) the old video nodes so lets now add the
				// new video's we created to the original document.
				((Element)pr.selectSingleNode("videoplaylist")).add(newvideonode);
			}
			return pr; // return it, not really needed its changed in place but..
		} else {
			// if no video node is found something is wrong so error.
			logger.error("No video 1 node found in playlist");
		}
		
		// always return the presentation even if no hits found (empty video list now)
		return pr;
	}
	
}
