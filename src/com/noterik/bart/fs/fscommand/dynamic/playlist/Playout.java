package com.noterik.bart.fs.fscommand.dynamic.playlist;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.fscommand.DynamicCommand;
import com.noterik.bart.fs.fscommand.DynamicCommandHandler;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * 
 * @author Daniel Ockeloen
 * 
 * Wrapper class if frontend asks for a dynamic playlist directly. Normally
 * its called from quickpresentation start but when a update is needed a frontend
 * can request it directly 
 *
 */
public class Playout implements DynamicCommand {
	/** Logger */
	private static Logger logger = Logger.getLogger(Playout.class);
	
	// clearly a dummy class for now until i finish the quickpresentation start.	
	public String run(String uri,String xml) {	
		logger.error("HELLO WORLD !");
		return "";
	}
}
