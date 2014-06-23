package com.noterik.bart.fs.fscommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.XMLHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

public class ShiftCommand implements Command {
	
	/** The ShiftCommand's log4j Logger */
	private static Logger logger = Logger.getLogger(ShiftCommand.class);
	
	/** constants defining the maximum and minimum value */
	public static final String MAX = "Inf";
	public static final String MIN = "-Inf";
	
	/** Elements to exclude */
	private static final List<String> EXCLUDE_ELEMENTS = new ArrayList<String>();
	static {
		EXCLUDE_ELEMENTS.add("properties");
		EXCLUDE_ELEMENTS.add("video");
	}
	
	public String execute(String uri, String xml) {
		logger.debug("input xml (shit): " + xml);
		
		// get input parameters and run command
		Properties input = getInputParameters(xml);
		if(input != null) {
			return shift(input, uri);
		}
		
		// error message
		return FSXMLBuilder.getErrorMessage("500", "Incorrect parameters", "Please call this command as follows: shift FROM TO MILLIS", "http://teamelements.noterik.com/team");
	}

	/**
	 * Shift the events from this uri by a number of milliseconds.  
	 * 
	 * @param input
	 * @param uri
	 * @return
	 */
	private String shift(Properties input, String uri) {		
		// parse parameters
		double from, to, millis;
		String fromStr, toStr;
		try {
			fromStr = input.getProperty("from");
			toStr = input.getProperty("to");
			millis = Double.parseDouble(input.getProperty("millis"));
			
			// check from input
			if(fromStr.equals(MIN)) {
				from = Double.NEGATIVE_INFINITY;
			} else {
				from = Double.parseDouble(fromStr);
			}
			
			// check to input
			if(toStr.equals(MAX)) {
				to = Double.POSITIVE_INFINITY;
			} else {
				to = Double.parseDouble(toStr);
			}
		} catch(Exception e) {
			logger.error("Parameters could not be parsed",e);
			return FSXMLBuilder.getErrorMessage("500", "Parameters could not be parsed", "Please call this command as follows: shift FROM TO MILLIS", "http://teamelements.noterik.com/team");
		}
		
		// check uri
		if(!URIParser.getParentUriPart(uri).equals("presentation")) {
			return FSXMLBuilder.getErrorMessage("500", "Incorrect uri, should be a presentation uri", "Please call this command as follows: shift FROM TO MILLIS", "http://teamelements.noterik.com/team");
		}
		
		// get all events from the given uri
		Map<String,String> currentEvents = getEvents(uri);
		
		// refactor events using the specified parameters
		Map<String,String> newEvents = refactorEvents(currentEvents, from, to, millis);
		
		// update events
		boolean success = updateEvent(newEvents);
		
		return FSXMLBuilder.getStatusMessage("Successfully updated events", "Successfully updated events", uri);
	}

	/**
	 * Returns a map of events that belong to the given presentation uri
	 * @param uri
	 * @return
	 */
	private Map<String,String> getEvents(String uri) {
		logger.debug("getting events for presentation: "+uri);
		Map<String,String> map = new HashMap<String,String>();
		Document doc = FSXMLRequestHandler.instance().getNodeProperties(uri, false);
		try {
			// get videoplaylist
			Element videoplaylist = (Element)doc.selectSingleNode("//videoplaylist[@id='1']");
			
			// get all child nodes
			Element elem, propElem;
			String id, name, eUri, properties;
			for(Iterator<Element> iter = videoplaylist.elementIterator(); iter.hasNext(); ) {
				try {
					// get element
					elem = iter.next();
					
					// get id, name, uri and properties
					id = elem.valueOf("@id");
					name = elem.getName();
					propElem = (Element) elem.selectSingleNode("properties");
					properties = propElem != null ? propElem.asXML() : null;
					eUri = uri + "/videoplaylist/1/" + name + "/" + id;
					logger.debug("id: "+id+", name: "+name+", eventUri: "+eUri+", properties: "+properties);
					
					// check to exclude elements
					if(EXCLUDE_ELEMENTS.contains(name)) {
						continue;
					}
					
					// add to map
					map.put(eUri, properties);
				} catch(Exception e) {
					logger.error("",e);
				}
			}
		} catch(Exception e) {
			logger.error("",e);
		}
		return map;
	}
	
	/**
	 * Returns a map with the refactorred events
	 * 
	 * @param currentEvents
	 * @param from
	 * @param to
	 * @param millis
	 * @return
	 */
	private Map<String, String> refactorEvents(Map<String, String> currentEvents, double from, double to, double millis) {
		logger.debug("refactoring events");
		Map<String,String> map = new HashMap<String, String>();
		
		// loop through events
		String uri, properties, starttimeStr;
		Document doc;
		Element propElem;
		double stOld, stNew;
		for(Iterator<String> iter = currentEvents.keySet().iterator(); iter.hasNext(); ) {
			uri = iter.next();
			properties = currentEvents.get(uri);
			try {
				// parse properties
				doc = DocumentHelper.parseText(properties);
				propElem = doc.getRootElement();
				
				// starttime
				starttimeStr = propElem.selectSingleNode("starttime").getText();
				stOld = Double.parseDouble(starttimeStr);
				
				// check if starttime is within boundaries
				if( stOld >= from && stOld <= to ) {
					// update starttime
					stNew = stOld + millis;
					propElem.selectSingleNode("starttime").setText(Double.toString(stNew));
					logger.debug("refactor -- uri: "+uri+", old: "+stOld+", new: "+stNew);
					
					// put in map
					map.put(uri, propElem.asXML());
				}
			} catch(Exception e) {
				logger.error("",e);
			}
		}
		
		return map;
	}
	
	/**
	 * Updates all the events in the given hashmap
	 * @param newEvents
	 * @return
	 */
	private boolean updateEvent(Map<String, String> events) {
		// loop through events
		String uri, properties;
		boolean success;
		for(Iterator<String> iter = events.keySet().iterator(); iter.hasNext(); ) {
			// get uri, property pairs
			uri = iter.next();
			properties = events.get(uri);
			
			// save into database
			success = FSXMLRequestHandler.instance().saveFsXml(uri+"/properties", "<fsxml>"+properties+"</fsxml>", "PUT", true);
		}
		return true;
	}

	/**
	 * Returns the input parameters.
	 * 
	 * @param xml	The xml specifying the commands parameters.
	 * @return		The input parameters.
	 */
	private Properties getInputParameters(String xml){
		Properties props = new Properties();
		Document doc = XMLHelper.asDocument(xml);
		if(doc == null){
			return null;
		} else {
			Node n = doc.selectSingleNode("./fsxml/properties/from");			
			if(n != null && n instanceof Element){
				props.put("from", ((Element)n).getText());				
			} else {
				return null;
			}
			n = doc.selectSingleNode("./fsxml/properties/to");
			if(n != null && n instanceof Element){
				props.put("to", ((Element)n).getText());				
			} else {
				return null;
			}
			n = doc.selectSingleNode("./fsxml/properties/millis");
			if(n != null && n instanceof Element){
				props.put("millis", ((Element)n).getText());				
			} else {
				return null;
			}
		}		
		logger.debug(props.toString());
		return props;
	}
	
	public ManualEntry man() {
		return null;
	}
}
