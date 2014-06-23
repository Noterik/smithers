package com.noterik.bart.fs.fscommand.dynamic.playlist.generators;

import java.util.*;
import org.apache.log4j.Logger;
import org.dom4j.Element;

import com.noterik.bart.fs.fscommand.dynamic.config.flash;
import com.noterik.bart.fs.fscommand.dynamic.playlist.GeneratorInterface;
import com.noterik.bart.fs.fscommand.dynamic.playlist.PlaylistGenerator.generators;

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
public class FilterLayer implements GeneratorInterface {

	/** Logger */
	private static Logger logger = Logger.getLogger(flash.class);
	
	// a normal case on a string is not possible so create a list to allow for a switch
	public enum layertypes { person,keyword,chapter,location,peercomment,restricted; }
	
	
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
			
			String layername = params.selectSingleNode("handlerparams/properties/layername").getText();
			String filter = params.selectSingleNode("handlerparams/properties/filter").getText().toLowerCase();

			// select the selected layer.
			List<Node> vpnodes = pr.selectNodes("videoplaylist/"+layername);		

			// ok lets loop all the blocks and create video nodes based on them
			int idcounter = 1; // we need to number the new video nodes
			for (Iterator<Node> i = vpnodes.iterator();i.hasNext();) {
				Node block = i.next();
				
		
				String blockvalue = "";
				// use a switch to select and map the correct fields to filter against
				switch (layertypes.valueOf(layername)) {
					case keyword : blockvalue = block.selectSingleNode("properties/keyword").getText(); break;
					case person : blockvalue = block.selectSingleNode("properties/name").getText(); break;
					case chapter : blockvalue = block.selectSingleNode("properties/title").getText(); break;
					case location : blockvalue = block.selectSingleNode("properties/name").getText(); break;
					case peercomment : blockvalue = block.selectSingleNode("properties/comment").getText(); break;
					case restricted : blockvalue = block.selectSingleNode("properties/level").getText(); break;
				}
				if (blockvalue.toLowerCase().indexOf(filter)!=-1) {

					// get the starttime and duration of the block as a base for the new
					// video's start/duration
					String starttime = block.selectSingleNode("properties/starttime").getText();
					String duration = block.selectSingleNode("properties/duration").getText();
					// create the new video node
					Element newvideonode = DocumentHelper.createElement("video");
				
					// set the id and aim it to our original video
					newvideonode.addAttribute("id", ""+idcounter++);
					newvideonode.addAttribute("referid", referid);
				
					// create the properties and set them (this can be done easer?)
					Element p = DocumentHelper.createElement("properties");
					Element st = DocumentHelper.createElement("starttime");
					Element du = DocumentHelper.createElement("duration");
					st.setText(starttime);
					du.setText(duration);
					p.add(st);
					p.add(du);
					// add the properties to the video node so it plays just that part.
					newvideonode.add(p);
					
					// we deleted (detached) the old video nodes so lets now add the
					// new video's we created to the original document.
					((Element)pr.selectSingleNode("videoplaylist")).add(newvideonode);
				}
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
