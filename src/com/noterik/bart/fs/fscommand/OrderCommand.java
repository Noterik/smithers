package com.noterik.bart.fs.fscommand;

import java.util.ArrayList;
import java.util.Arrays;
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

import com.noterik.springfield.tools.HttpHelper;
import com.noterik.springfield.tools.XMLHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;

public class OrderCommand implements Command {
	
	/** The ShiftCommand's log4j Logger */
	private static Logger logger = Logger.getLogger(OrderCommand.class);
	private static ArrayList<Properties> map = new ArrayList<Properties>();
	private static final String PROJECT_URI_TEMPLATE = "/domain/{domain}/user/{user}/project/{project}";
	private static final String[] BART_SERVERS = new String[] {"http://bart1.noterik.com/bart", "http://bart2.noterik.com/bart", "http://bart3.noterik.com/bart", "http://bart5.noterik.com/bart"};
	
	public String execute(String uri, String xml) {
		logger.debug("input xml (alignorder): " + xml);
		
		// get input parameters and run command
		Properties input = getInputParameters(xml);
		if(input != null) {
			String fix = input.getProperty("fixorder", "false");
			logger.debug("fixorder param: " + fix);
			boolean fixOrder = Boolean.parseBoolean(fix);
			if(fixOrder) {
				return FixOrder(input, uri);
			} else {
				return order(input, uri);
			}
		}
		
		// error message
		return FSXMLBuilder.getErrorMessage("500", "Incorrect parameters", "Please call this command as follows: split AT MILLIS", "http://teamelements.noterik.com/team");
	}
	
	private String FixOrder(Properties input, String uri) {
		map.clear();
		map = null;
		map = new ArrayList<Properties>();
		String domain = URIParser.getDomainIdFromUri(uri);
		String user = URIParser.getUserIdFromUri(uri);
		String project = getTypeIdFromUri(uri,"project");
		
		// check uri
		if(!URIParser.getCurrentUriPart(uri).equals("page")) {
			return FSXMLBuilder.getErrorMessage("500", "Incorrect uri, should be a page uri", "Please call this command as follows: order [from] position [to] position", "http://teamelements.noterik.com/team");
		}
		
		logger.debug("Fixing the order of " + uri);
		boolean has_pages = getPages(uri);
		
		if(!has_pages) {
			return FSXMLBuilder.getErrorMessage("500", "Could not get pages for project", "Please call this command as follows: order [from] position [to] position", "http://teamelements.noterik.com/team");
		}
		
		Map<String,String> ordered_map = new HashMap<String,String>();
		for(Iterator<Properties> iter = map.iterator(); iter.hasNext(); ) {
			Properties properties = iter.next();
			String id  = properties.getProperty("id");
			String order_str = properties.getProperty("order");
			ordered_map.put(order_str, id);
		}
		
		Object[] key = ordered_map.keySet().toArray();  
        Arrays.sort(key);  

        for(int i=0; i<key.length; i++)   {  
			String id = ordered_map.get(key[i]);
			String order_str = Integer.toString(i+1);
			String updateUri = PROJECT_URI_TEMPLATE.replace("{domain}", domain).replace("{user}", user).replace("{project}", project);
			updateUri = updateUri + "/page/" + id + "/properties/order";
			logger.debug("Update URI: " + updateUri + "; set order=" + order_str);
			FSXMLRequestHandler.instance().updateProperty(updateUri, "order", order_str, "PUT", true);
		}
		
		return FSXMLBuilder.getStatusMessage("Successfully aligned the order", "Successfully aligned the order", uri);
	}

	/**
	 * Reorders elements based on the input parameters
	 * 
	 * @param input - should contain [from] and [to] params
	 * @param uri
	 * @return
	 */
	private String order(Properties input, String uri) {		
		// parse parameters
		map.clear();
		map = null;
		map = new ArrayList<Properties>();
		String domain = URIParser.getDomainIdFromUri(uri);
		String user = URIParser.getUserIdFromUri(uri);
		String project = getTypeIdFromUri(uri,"project");
		int from = -1; 
		int to = -1; 
		try {
			from = Integer.parseInt(input.getProperty("from", "-1"));
			to = Integer.parseInt(input.getProperty("to", "-1"));
		} catch(Exception e) {
			logger.error("Parameters could not be parsed",e);
			return FSXMLBuilder.getErrorMessage("500", "Parameters could not be parsed", "Please call this command as follows: order [from] position [to] position", "http://teamelements.noterik.com/team");
		}
		
		// check uri
		if(!URIParser.getCurrentUriPart(uri).equals("page")) {
			return FSXMLBuilder.getErrorMessage("500", "Incorrect uri, should be a page uri", "Please call this command as follows: order [from] position [to] position", "http://teamelements.noterik.com/team");
		}
		
		if(from==-1 || to==-1) {
			return FSXMLBuilder.getErrorMessage("500", "Incorrect params [from] or [to]", "Please call this command as follows: order [from] position [to] position", "http://teamelements.noterik.com/team");
		}
		
		logger.debug("Aligning the order of " + uri);
		logger.debug("Moving page from position " + from + " to position " + to);
		boolean has_pages = getPages(uri);
		
		if(!has_pages) {
			return FSXMLBuilder.getErrorMessage("500", "Could not get pages for project", "Please call this command as follows: order [from] position [to] position", "http://teamelements.noterik.com/team");
		}
		
		String mode = (from-to<0)?"up":"down";
		
		ArrayList<Properties> ordered_map = new ArrayList<Properties>();
		for(Iterator<Properties> iter = map.iterator(); iter.hasNext(); ) {
			// get uri, property pairs
			Properties properties = iter.next();
			String id  = properties.getProperty("id");
			logger.debug("id: " + id);
			String order_str = properties.getProperty("order");
			int order = Integer.parseInt(order_str);
			
			Properties new_props = new Properties();
			if(order==from) {
				new_props.put("id", id);
				new_props.put("order", Integer.toString(to));
				ordered_map.add(new_props);
				continue;
			}
			if(mode=="down") {
				if(order>=from) {
					new_props.put("id", id);
					new_props.put("order", Integer.toString(order));
					ordered_map.add(new_props);
					continue;
				}
				if(order>=to) {
					new_props.put("id", id);
					new_props.put("order", Integer.toString(order+1));
					ordered_map.add(new_props);
				}else{
					new_props.put("id", id);
					new_props.put("order", Integer.toString(order));
					ordered_map.add(new_props);
				}
			} else {
				if(order<=from) {
					new_props.put("id", id);
					new_props.put("order", Integer.toString(order));
					ordered_map.add(new_props);
					continue;
				}
				if(order<=to) {
					new_props.put("id", id);
					new_props.put("order", Integer.toString(order-1));
					ordered_map.add(new_props);
				}else{
					new_props.put("id", id);
					new_props.put("order", Integer.toString(order));
					ordered_map.add(new_props);
				}
			}
		}
		logger.debug("Ordered map is: " + ordered_map.toString());
		
		for(Iterator<Properties> iter = ordered_map.iterator(); iter.hasNext(); ) {
			Properties properties = iter.next();
			String id  = properties.getProperty("id");
			String order_str = properties.getProperty("order");
			String updateUri = PROJECT_URI_TEMPLATE.replace("{domain}", domain).replace("{user}", user).replace("{project}", project);
			updateUri = updateUri + "/page/" + id + "/properties/order";
			logger.debug("Update order URI: " + updateUri + "; set order=" + order_str);
			FSXMLRequestHandler.instance().updateProperty(updateUri, "order", order_str, "PUT", true);
		}
		
		return FSXMLBuilder.getStatusMessage("Successfully aligned the order", "Successfully aligned the order", uri);
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
			Node n = doc.selectSingleNode("./fsxml/properties/fixorder");
			if(n != null && n instanceof Element){
				String fix = ((Element)n).getText();
				if(fix.equalsIgnoreCase("true")) {
					props.put("fixorder", "true");
				}
			}
			n = doc.selectSingleNode("./fsxml/properties/from");
			if(n != null && n instanceof Element){
				String from = ((Element)n).getText();
				if(!(from=="0" || from=="")) {
					props.put("from", from);
				}
			}
			n = doc.selectSingleNode("./fsxml/properties/to");			
			if(n != null && n instanceof Element){
				String to = ((Element)n).getText();
				if(!(to=="0" || to=="")) { 
					props.put("to", to);	
				}
			}
		}		
		return props;
	}
	
	public ManualEntry man() {
		return null;
	}
	
	private static boolean getPages(String uri) {
		logger.debug("getting pages for project: "+uri);
		uri = getServer() + uri + "?start={start}&limit={limit}";
		int start = 0;
		int limit = 0;
		String baseUrl = uri.replace("{start}", Integer.toString(start)).replace("{limit}", Integer.toString(limit));
		try {
			List<Node> nodes = new ArrayList<Node>();
			List<Node> partialNodes;
			String response = HttpHelper.sendRequest("GET", baseUrl, null, null);
			Document doc = DocumentHelper.parseText(response);
			int hits = Integer.parseInt(doc.selectSingleNode("//totalResultsAvailable").getText());	
			
			start = 0;
			limit = 10;
			if(hits>0) {
				while(start<hits) {
					baseUrl = uri.replace("{start}", Integer.toString(start)).replace("{limit}", Integer.toString(limit));
					
					logger.error("get pages "+baseUrl);
					response = HttpHelper.sendRequest("GET",baseUrl, null, null);
					doc = DocumentHelper.parseText(response);
					partialNodes = doc.selectNodes("//page");
					start+=limit;
					nodes.addAll(partialNodes);
				}
				
				String id, name, order;
				Element propElem;
				logger.error("nr of nodes found: "+nodes.size());
				for(Iterator<Node> iter = nodes.iterator(); iter.hasNext(); ) {
					Element elem = (Element) iter.next();
					Properties props = new Properties();
					//props.put("starttime", starttime);
					// get id, name, uri and properties
					id = elem.valueOf("@id");
					name = elem.getName();
					if(!name.equals("page")) continue;
					
					propElem = (Element) elem.selectSingleNode("properties");
					order = "";
					order = propElem.valueOf("order");
					if(order.equals("")) continue;
					props.put("id", id);
					props.put("order", order);
					map.add(props);
				}
			} else {
				return false;
			}
		} catch(Exception e) {
			logger.error("",e);
			return false;
		}
		return true;
	}
	
	private static String getTypeIdFromUri(String uri, String type) {
		String value = null;
		String typeUriPart = "/"+type+"/";
		int index1, index2;

		index1 = uri.indexOf(typeUriPart);
		if (index1 != -1) {
			index1 += typeUriPart.length();
			index2 = uri.indexOf("/", index1);
			if (index2 != -1) {
				value = uri.substring(index1, index2);
			} else {
				value = uri.substring(index1);
			}
		}
		return value;
	}
	
	/**
	 * Returns a random server
	 * 
	 * @return
	 */
	private static String getServer() {
		int num = (int)Math.floor(Math.random()*BART_SERVERS.length);
		return BART_SERVERS[num];
	}
}
