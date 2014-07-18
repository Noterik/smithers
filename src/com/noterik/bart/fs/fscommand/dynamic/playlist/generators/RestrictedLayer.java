/* 
* RestrictedLayer.java
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
import org.dom4j.Element;

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
public class RestrictedLayer implements GeneratorInterface {

	/** Logger */
	private static Logger logger = Logger.getLogger(flash.class);
	
	/*
	 * the incoming generate call from either quickpresentation or direct restlet
	 * 
	 * @see com.noterik.bart.fs.fscommand.dynamic.playlist.GeneratorInterface#generate(org.dom4j.Node, java.lang.String)
	 */
	public Element generate(Element pr,String wantedplaylist, Element params,Element domainvpconfig,Element fsxml) {
		//System.out.println("MADE IT INTO RESTRICED");
		long timer_start = new Date().getTime();
		
		// select the original playlist (now hardcoded to playlist with id 1).
		
		// create a timeline where we can map all the events
		TimeLine timeline = new TimeLine(pr);
		
		Element videonode = (Element) pr.selectSingleNode("videoplaylist/video");
		if (videonode!=null) {
			// if its a videonode than detach it from the document (delete it) since we are
			// doing a rewrite on the document not generating a new one
			videonode.detach(); // remove it from the presentation
			
			// we need the referid, so we can set this in all the new video's, This is
			// now limited to one video as imput and needs a rewrite to support more input 
			// video's in a playlist.
			String referid = videonode.attributeValue("referid");

			String level = "Not Public";
			if (domainvpconfig!=null) level = domainvpconfig.selectSingleNode("properties/level").getText();
			if (params!=null) level = params.selectSingleNode("handlerparams/properties/level").getText();
			//System.out.println("LEVEL="+level);
			// select the selected layer.
			List<Node> vpnodes = pr.selectNodes("videoplaylist/restricted");
			//System.out.println("NODES="+vpnodes.size());
			List<Node> sortednodes = timeline.getSortedList(vpnodes);
			//System.out.println("NODES2="+sortednodes.size());
			// ok lets loop all the blocks and create video nodes based on them
			int idcounter = 1; // we need to number the new video nodes
			float starttime = 0;
			float duration = 0;
			float gap = 0;
			for (Iterator<Node> i = sortednodes.iterator();i.hasNext();) {
				Node block = i.next();
				//System.out.println("LOOP1="+block.selectSingleNode("properties/level"));
				// this is tricky everytime we find a block we need to exclude it
				// so save a block upto that point and then setup the next one
				String blockvalue = "Not Public";
				if (block.selectSingleNode("properties/level")!=null) blockvalue = block.selectSingleNode("properties/level").getText();
				//System.out.println("LOOP1b");
				if (blockvalue.equals(level)) {
					// get the starttime and duration of the block as a base for the new
					// video's start/duration
					//System.out.println("LOOP2a");
					float foundstart = Float.parseFloat(block.selectSingleNode("properties/starttime").getText());
					float foundduration = Float.parseFloat(block.selectSingleNode("properties/duration").getText());
					// so the duration of the block is current foundstart - current start
					duration = foundstart - starttime;
					//System.out.println("LOOP2b");
					if (duration!=0) {
						// create the new video node
						Element newvideonode = DocumentHelper.createElement("video");				
						// set the id and aim it to our original video
						newvideonode.addAttribute("id", ""+idcounter++);
						newvideonode.addAttribute("referid", referid);
				
						// create the properties and set them (this can be done easer?)
						Element p = DocumentHelper.createElement("properties");
						Element st = DocumentHelper.createElement("starttime");
						Element du = DocumentHelper.createElement("duration");
						st.setText(""+starttime);
						du.setText(""+duration);
						p.add(st);
						p.add(du);
						//System.out.println("LOOP3");
						// now also copy all the old properties if needed from
						// the original video node.
						Element props = (Element)videonode.elements().get(0);
						for (Iterator<Node> vn = props.elements().iterator();vn.hasNext();) {
							Node prop = vn.next();
							String pname = prop.getName();
							String pvalue = prop.getText();
							Element npr = DocumentHelper.createElement(pname);
							npr.setText(pvalue);
							p.add(npr);
						}
					
						// add the properties to the video node so it plays just that part.
						newvideonode.add(p);
					
					
						// we deleted (detached) the old video nodes so lets now add the
						// new video's we created to the original document.
						((Element)pr.selectSingleNode("videoplaylist")).add(newvideonode);

						// also add the events 'hidden' in this video space
						//timeline.remapEvents(starttime,duration,gap);
					}
					
					// now move the start point for the next commit
					gap = (foundstart+foundduration)-(starttime+duration);
				//	System.out.println("DIFF="+gap);
					starttime = foundstart+foundduration;

				}
			}
			//System.out.println("LOOP EXIT");
			//timeline.remapEvents(starttime,duration,gap);
			
			// in the end we allways insert one either with no start/duration or with
			// create the new video node
			Element newvideonode = DocumentHelper.createElement("video");				
			// set the id and aim it to our original video
			newvideonode.addAttribute("id", ""+idcounter++);
			newvideonode.addAttribute("referid", referid);
		
			// create the properties and set them (this can be done easer?)
			Element p = DocumentHelper.createElement("properties");
			Element st = DocumentHelper.createElement("starttime");
			Element du = DocumentHelper.createElement("duration");
			
			if (starttime!=0) { // if we never cut don't even limit the video by setting points
				st.setText(""+starttime);
				p.add(st);
				p.add(du);
			}
				
			// add the properties to the video node so it plays just that part.
			newvideonode.add(p);
			
			// we deleted (detached) the old video nodes so lets now add the
			// new video's we created to the original document.
			((Element)pr.selectSingleNode("videoplaylist")).add(newvideonode);
			
			// lets map old events  into the playlist
			timeline.remapOldEvents();
			
			// lets map all the video based events into the playlist
			timeline.remapVideoEvents(fsxml);
			
			// ok we are all done so put all the events into the presentation
			timeline.insertNodesInPresentation();
			long timer_end = new Date().getTime();
			System.out.println("RESTRICT TIME="+(timer_end-timer_start));
			return pr; // return it, not really needed its changed in place but..
		} else {
			// if no video node is found something is wrong so error.
			logger.error("No video 1 node found in playlist");
		}
		

		// always return the presentation even if no hits found (empty video list now)
		return pr;
	}
	
	
}
