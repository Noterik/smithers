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
