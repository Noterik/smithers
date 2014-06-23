package com.noterik.bart.fs.fscommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.XMLHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.FSXMLParser;
import com.noterik.springfield.tools.fs.URIParser;

public class SplitCommand implements Command {
	
	/** The ShiftCommand's log4j Logger */
	private static Logger logger = Logger.getLogger(SplitCommand.class);
	private static double totalPrevDuration = 0d;
	private static ArrayList<Properties> map_joined = new ArrayList<Properties>();
	
	public String execute(String uri, String xml) {
		logger.debug("input xml (split): " + xml);
		
		// get input parameters and run command
		Properties input = getInputParameters(xml);
		if(input != null) {
			return split(input, uri);
		}
		
		// error message
		return FSXMLBuilder.getErrorMessage("500", "Incorrect parameters", "Please call this command as follows: split AT MILLIS", "http://teamelements.noterik.com/team");
	}

	/**
	 * Splits the videoplaylist at a given number of milliseconds.
	 * Finds which video clip is running at the given time and splits it  
	 * 
	 * @param input
	 * @param uri
	 * @return
	 */
	private String split(Properties input, String uri) {		
		// parse parameters
		double time;
		String timeStr;
		totalPrevDuration = 0d;
		int new_position = 0;
		map_joined.clear();
		map_joined = null;
		map_joined = new ArrayList<Properties>();
		
		try {
			
			time = Double.parseDouble(input.getProperty("time"));
			timeStr = Double.toString(time);
		} catch(Exception e) {
			logger.error("Parameters could not be parsed",e);
			return FSXMLBuilder.getErrorMessage("500", "Parameters could not be parsed", "Please call this command as follows: split AT MILLIS", "http://teamelements.noterik.com/team");
		}
		
		// check uri
		if(!URIParser.getParentUriPart(uri).equals("videoplaylist")) {
			return FSXMLBuilder.getErrorMessage("500", "Incorrect uri, should be a videoplaylist uri", "Please call this command as follows: split AT MILLIS", "http://teamelements.noterik.com/team");
		}
		
		// get all clips from the given uri
		ArrayList<Properties> currentClips = getClips(uri);
		//Get the clip that needs to be splitted in the videoplaylist
		String clipURI = getVideoClip(currentClips, time);
		if(clipURI==null) {
			return FSXMLBuilder.getErrorMessage("500", "Clip to split not found!", "Please call this command as follows: split AT MILLIS", "http://teamelements.noterik.com/team");
		}
		logger.debug("Clip to split" + clipURI);
		
		Document doc = FSXMLRequestHandler.instance().getNodeProperties(clipURI, false);
		//Get the referif
		Element videoElem = (Element) doc.selectSingleNode("//video");
		String referid =  videoElem.valueOf("@referid");
		
		//Get the video duration from the original video
		double originalDuration = getOriginalDuration(referid);	
		
		logger.debug("Original duration" + Double.toString(originalDuration));
		
		Element propElem = (Element) doc.selectSingleNode("//properties");
		Element elem;
		String key, src_properties, trg_properties;
		
		double src_duration = time - totalPrevDuration;
		trg_properties = "<properties>";
		src_properties = "<properties>";
		boolean foundStart=false, foundDuration=false;
		for(Iterator<Element> iter = propElem.elementIterator(); iter.hasNext(); ) {
			try {
				// get element
				elem = iter.next();
				key = elem.getName();
				logger.debug("Element: " + key);
				if(key=="starttime") {
					foundStart=true;
					logger.debug("FOUND starttime!!!");
					src_properties += elem.asXML();
					double starttime = Double.parseDouble(elem.getTextTrim());
					elem.setText(Double.toString(starttime+src_duration));
					trg_properties += elem.asXML();
				}
				
				if(key=="duration") {
					foundDuration=true;
					logger.debug("FOUND duration!!!");
					src_properties += "<duration>"+Double.toString(src_duration)+"</duration>";
					double trg_duration = Double.parseDouble(elem.getTextTrim());
					logger.debug("Setting target duration: " + Double.toString(trg_duration - src_duration));
					elem.setText(Double.toString(trg_duration - src_duration));
					trg_properties += elem.asXML();
				}
				
				if(key=="position") {
					src_properties += elem.asXML();
					int position = Integer.parseInt(elem.getTextTrim());
					position++;
					new_position = position;
					logger.debug("FOUND position!!!" + Integer.toString(new_position));
					trg_properties += "<position>"+Integer.toString(position)+"</position>";
				}
			} catch(Exception e) {
				logger.error("",e);
			}
		}
		if(!foundStart) {
			logger.debug("NOT FOUND starttime!!!");
			src_properties += "<starttime>0</starttime>";
			trg_properties += "<starttime>"+timeStr+"</starttime>";
		}
		
		if(!foundDuration) {
			logger.debug("NOT FOUND duration!!!");
			src_properties += "<duration>"+Double.toString(src_duration)+"</duration>";
			trg_properties += "<duration>"+Double.toString(originalDuration - src_duration)+"</duration>";
			logger.debug("Setting target duration: " + Double.toString(originalDuration - src_duration));
		}
		
		src_properties += "</properties>";
		trg_properties += "</properties>";
		logger.debug("Source video prop XML: " + src_properties);
		logger.debug("Target video prop XML:: " + trg_properties);
		// process splitting
		boolean success = FSXMLRequestHandler.instance().saveFsXml(clipURI+"/properties", "<fsxml>"+src_properties+"</fsxml>", "PUT", true);
		if(success) {
			String attributes = "<attributes><referid>"+referid+"</referid></attributes>";
			String response = FSXMLRequestHandler.instance().handlePOST(uri+"/video", "<fsxml>"+attributes+"</fsxml>");
			if(FSXMLParser.getErrorMessageFromXml(response)!=null) {
				return FSXMLBuilder.getErrorMessage("500", "Failed to duplicate video in playlist ", "Please call this command as follows: split AT MILLIS", "http://teamelements.noterik.com/team");
			}
			
			fixPositions(uri, new_position);
			
			Document resDoc = XMLHelper.asDocument(response);
			Element resElem = (Element) resDoc.selectSingleNode("//uri");
			String new_video_uri = resElem.getTextTrim();
			response = FSXMLRequestHandler.instance().handlePUT(new_video_uri+"/properties", "<fsxml>"+trg_properties+"</fsxml>");
			if(FSXMLParser.getErrorMessageFromXml(response)!=null) {
				return FSXMLBuilder.getErrorMessage("500", "Failed to update properties of " + new_video_uri, "Please call this command as follows: split AT MILLIS", "http://teamelements.noterik.com/team");
			}
		} else {
			return FSXMLBuilder.getErrorMessage("500", "Failed to update properties of " + clipURI, "Please call this command as follows: split AT MILLIS", "http://teamelements.noterik.com/team");
		}
		
		logger.debug("*********** END OF PROCESS **********");
		return FSXMLBuilder.getStatusMessage("Successfully updated events", "Successfully updated events", clipURI);
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
			Node n = doc.selectSingleNode("./fsxml/properties/time");			
			if(n != null && n instanceof Element){
				String time = ((Element)n).getText();
				if(time=="0" || time=="") return null;
				props.put("time", time);				
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
	
	private ArrayList<Properties> getClips(String uri) {
		logger.debug("getting clips for videoplaylist: "+uri);
		ArrayList<Properties> map = new ArrayList<Properties>();
		SortedMap map_positioned = new TreeMap();
		Document doc = FSXMLRequestHandler.instance().getNodeProperties(uri, false);
		try {
			// get videoplaylist
			Element videoplaylist = (Element)doc.selectSingleNode("//videoplaylist[@id='1']");
			
			// get all child nodes
			Element elem, propElem;
			String id, name, eUri, properties, starttime, duration, referid, position;
			for(Iterator<Element> iter = videoplaylist.elementIterator(); iter.hasNext(); ) {
				try {
					// get element
					elem = iter.next();
					
					// get id, name, uri and properties
					id = elem.valueOf("@id");
					name = elem.getName();
					if(!name.equals("video")) continue;
					
					referid = elem.valueOf("@referid");
					
					
					Properties props = new Properties();
					propElem = (Element) elem.selectSingleNode("properties");
					starttime = propElem.valueOf("starttime");
					starttime = starttime != "" ? starttime : "0";
					props.put("starttime", starttime);
					duration = propElem.valueOf("duration");
					if(duration==null || duration == "") {
						Document refDoc = FSXMLRequestHandler.instance().getNodeProperties(referid, false);
						// get original video
						Element origVideoProps = (Element)refDoc.selectSingleNode("//rawvideo[@id='1']").selectSingleNode("properties");
						if(origVideoProps==null) {
							origVideoProps = (Element)refDoc.selectSingleNode("//rawvideo[@id='2']").selectSingleNode("properties");
						}
						duration = origVideoProps.valueOf("duration");
						//Convert to milliseconds
						double dur = Double.parseDouble(duration);
						dur = dur*1000;
						duration = Double.toString(dur);
					}
					props.put("duration", duration);
					position = propElem.valueOf("position");
					if(position!=null && position != "") {
						props.put("position", position);
					}
					logger.debug(name + "/" + id + " starttime: " + starttime);
					logger.debug(name + "/" + id + " duration: " + duration);
					properties = propElem != null ? propElem.asXML() : null;
					eUri = uri + "/" + name + "/" + id;
					logger.debug("id: "+id+", name: "+name+", eventUri: "+eUri+", properties: "+properties);
					props.put("uri", eUri);
					// add to map
					
					if(position!=null && position != "") {
						map_positioned.put(Integer.parseInt(position), props);
					} else {
						map.add(props);
					}
				} catch(Exception e) {
					logger.error("",e);
				}
			}
		} catch(Exception e) {
			logger.error("",e);
		}
		
		map_joined.addAll(map);
		map_joined.addAll(map_positioned.values());
		int count = 0;
		for(Iterator<Properties> iter = map_joined.iterator(); iter.hasNext(); ) {
			count++;
			Properties cur_map = iter.next();
			String map_uri = cur_map.getProperty("uri");
			String map_starttime = cur_map.getProperty("starttime");
			String map_duration = cur_map.getProperty("duration");
			String position = "<properties><position>"+Integer.toString(count)+"</position><starttime>"+map_starttime+"</starttime><duration>"+map_duration+"</duration></properties>";
			logger.debug("Joining maps uri: " + map_uri);
			logger.debug("Joining maps properties: " + position);
			String response = FSXMLRequestHandler.instance().handlePUT(map_uri+"/properties", "<fsxml>"+position+"</fsxml>");
		}
		
		return map_joined;
	}
	
	private double getOriginalDuration(String origUri) {
		Document refDoc = FSXMLRequestHandler.instance().getNodeProperties(origUri, false);
		// get original video
		Element origVideoProps = (Element)refDoc.selectSingleNode("//rawvideo[@id='1']").selectSingleNode("properties");
		if(origVideoProps==null) {
			origVideoProps = (Element)refDoc.selectSingleNode("//rawvideo[@id='2']").selectSingleNode("properties");
		}
		String duration = origVideoProps.valueOf("duration");
		return (Double.parseDouble(duration)*1000); //Convert to milliseconds
	}
	
	private void fixPositions(String uri, int position) {
		logger.debug("fix positions for: "+uri);
		int current_position = position;
		Document doc = FSXMLRequestHandler.instance().getNodeProperties(uri, false);
		try {
			// get videoplaylist
			Element videoplaylist = (Element)doc.selectSingleNode("//videoplaylist[@id='1']");
			
			// get all child nodes
			Element elem, propElem;
			String id, name;
			for(Iterator<Element> iter = videoplaylist.elementIterator(); iter.hasNext(); ) {
				try {
					// get element
					elem = iter.next();
					
					// get id, name, uri and properties
					id = elem.valueOf("@id");
					name = elem.getName();
					if(!name.equals("video")) continue;
					String properties = "<properties>";
					propElem = (Element) elem.selectSingleNode("properties");
					
					for(Iterator<Element> iter2 = propElem.elementIterator(); iter2.hasNext(); ) {
						try {
							// get element
							Element videoElem = iter2.next();
							String property = videoElem.getName();
							String value = videoElem.getTextTrim();
							if(property!="starttime" && property!="duration" && property!="position") continue;
							if(property=="position") {
								int posvalue = (value!="" ? Integer.parseInt(value) : 0);
								logger.debug("OLD Position is: " + Integer.toString(posvalue));
								if(posvalue>=current_position) {
									posvalue++;
									logger.debug("NEW Position is: " + Integer.toString(posvalue));
									properties+="<position>"+Integer.toString(posvalue)+"</position>";
								} else {
									properties += videoElem.asXML();
								}
							} else {
								properties += videoElem.asXML();
							}
							
						} catch(Exception e) {
							logger.error("",e);
						}
					}
					properties += "</properties>";
					logger.debug("Fix position of: " + uri + "/video/" + id);
					logger.debug("Properties are: " + properties);
					String response = FSXMLRequestHandler.instance().handlePUT(uri + "/video/" + id + "/properties", "<fsxml>"+properties+"</fsxml>");
				} catch(Exception e) {
					logger.error("",e);
				}
			}
		} catch(Exception e) {
			logger.error("",e);
		}
	}
	
	private String getVideoClip(ArrayList<Properties> clips, double time) {
		String uri, clipUri=null;
		double totalDuration = 0d;
		logger.debug("getVideoClip");
		for(Iterator<Properties> iter = clips.iterator(); iter.hasNext(); ) {
			// get uri, property pairs
			Properties properties = iter.next();
			uri = properties.getProperty("uri");
			logger.debug("key: " + uri);
			String duration = properties.getProperty("duration");
			logger.debug("Duration: " + duration);
			if(duration==null) continue;
			totalDuration +=  Double.parseDouble(duration);
			logger.debug("totalDuration: " + Double.toString(totalDuration));
			if(time==totalDuration) { 
				break; //Nothing to split - it is already splitted at this time.
			}
			if(time>totalDuration) {
				totalPrevDuration = totalDuration;
				logger.debug("totalPrevDuration: " + Double.toString(totalPrevDuration));
				continue;
			} else {
				clipUri = uri;
				break;
			}
		}
		return clipUri;
	}
}
