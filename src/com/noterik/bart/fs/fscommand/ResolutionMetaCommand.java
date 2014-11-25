/* 
* ResolutionMetaCommand.java
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
package com.noterik.bart.fs.fscommand;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.List;
import java.util.Properties;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.LazyHomer;
import com.noterik.bart.fs.fscommand.dynamic.playlist.util.SBFReader;
import com.noterik.bart.fs.fscommand.dynamic.playlist.util.SBFile;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.XMLHelper;

public class ResolutionMetaCommand implements Command {
	
	public enum inputs { analog,analog2,analog23,analog3,digital,run1,run2,analogfiltered,analog2filtered,analog23filtered,analog3filtered,digitalfiltered,highspeed,highspeedfiltered; }
	public enum highspeed { analog,analog2,analog23,analog3,digital;}
	public enum highspeedfiltered { analogfiltered,analog2filtered,analog23filtered,analog3filtered,digitalfiltered;}
	
	public String execute(String uri, String xml) {

		Properties props = getInputParameters(xml);
		String basePath = "/springfield/smithers/data/";
		if(LazyHomer.isWindows()) {
			basePath = "c:\\springfield\\smithers\\data\\";
		}
		
		String input = props.getProperty("input");
		
		Element metanode = null;
		if(!input.isEmpty()) {
			metanode = createMeta(input);
		}

		StringBuffer fsxml = new StringBuffer();
		fsxml.append("<fsxml>");
		if(metanode!=null) {
			fsxml.append(metanode.asXML());
		} else {
			fsxml.append("<error>No input found on the server.</error>");
		}
		fsxml.append("</fsxml>");
		return fsxml.toString();
	}
	
	private Element createMeta(String input) {
		String basePath = "/springfield/smithers/data/";
		if(LazyHomer.isWindows()) {
			basePath = "c:\\springfield\\smithers\\data\\";
		}
		File srcFolder =  new File(basePath + input);
		
		if(!srcFolder.exists()) return null;
		if(!srcFolder.isDirectory()) return null;
		
		File[] metafiles = srcFolder.listFiles(new FilenameFilter() {
			
			public boolean accept(File dir, String name) {
				if(name.equals(".") || name.equals("..")) {
					return false;
				}
				if(name.toUpperCase().endsWith(".META")) {
					return true;
				}
				return false;
			}
		});
		
		
		Element metanode = DocumentHelper.createElement("resolutionmeta");
		
		// set the id and aim it to our original video
		metanode.addAttribute("id", "1");
		
		// create the properties and set them (this can be done easer?)
		Element p = DocumentHelper.createElement("properties");
		Element dataunits = DocumentHelper.createElement("dataunits");
		//Element availableinputs = DocumentHelper.createElement("inputs");
		
		
		String body = "<![CDATA[";
		String sep = "";
		body += "{";
		for(int i=0; i<metafiles.length; i++) {
			File f = metafiles[i];
			Properties meta = readMetaFile(f);
			String filename = f.getName();
			String name = filename.substring(0, filename.lastIndexOf("."));
			if(meta.getProperty("startmeasurement")!=null) {
				body += sep + "\"" + name + "\" : { \"name\" : \"" +  meta.getProperty("name") + "\", \"unit\" : \"" + meta.getProperty("unit") + "\", \"startmeasurement\" : \"" + meta.getProperty("startmeasurement") + "\", \"endmeasurement\" : \"" + meta.getProperty("endmeasurement") + "\"}";
			} else {
				body += sep + "\"" + name + "\" : { \"name\" : \"" +  meta.getProperty("name") + "\", \"unit\" : \"" + meta.getProperty("unit") + "\"}";
			}
			sep = ",";
		}
		body += "}";
		body += "]]>";
		

		
		dataunits.setText(body);
		
		body = "";
		sep = "";
		for (inputs val : inputs.values()) {
			body += sep + val;
			sep = ",";
		}
		
		//availableinputs.setText(body);
		//p.add(availableinputs);
		p.add(dataunits);
		
		// add the properties to the video node so it plays just that part.
		metanode.add(p);
		
		return metanode;
	}
	
	private Properties readMetaFile(File file) {
		FileInputStream in = null;
		StringBuffer str = new StringBuffer("");
		try {
			int ch;
			
			in = new FileInputStream(file);
									
			/* read text */
			while ((ch = in.read()) != -1) {
				str.append((char)ch);
			}
			in.close();
		} catch(Exception e) {
			System.out.println(e);
		}
		
		String[] metaString = str.toString().split(",");  
		Properties meta = new Properties();
		meta.put("name", metaString[3]);
		meta.put("unit", metaString[4]);
		if(metaString.length>5) {
			meta.put("startmeasurement", metaString[5]);
			meta.put("endmeasurement", metaString[6]);
		}
		return meta;
	}
	
	
	private Element createMeta(String sbfFile, inputs input) {
		SBFReader sbfr = new SBFReader(sbfFile);
		
		Element metanode = DocumentHelper.createElement("resolutionmeta");
		
		// set the id and aim it to our original video
		metanode.addAttribute("id", "1");
		
		// create the properties and set them (this can be done easer?)
		Element p = DocumentHelper.createElement("properties");
		Element dataunits = DocumentHelper.createElement("dataunits");
		Element availableinputs = DocumentHelper.createElement("inputs");
		
	
		String body = "";
		SBFile dataFile = sbfr.getDataFile();
		int cols = (int) dataFile.getColumnCount();
		String sep = "";
		body += "{";
		for(int i=1; i<cols; i++) {
			body += sep + "\"" + input + "_" + Integer.toString(i) + "\" : { \"name\" : \"" +  dataFile.getDataColumns(i) + "\", \"unit\" : \"" + dataFile.getUnitColumns(i) + "\"}";
			sep = ",";
		}
		body += "}";
		
		sbfr = null;
		
		dataunits.setText(body);
		
		body = "";
		sep = "";
		for (inputs val : inputs.values()) {
			body += sep + val;
			sep = ",";
		}
		
		availableinputs.setText(body);
		p.add(availableinputs);
		p.add(dataunits);
		
		// add the properties to the video node so it plays just that part.
		metanode.add(p);
		
		return metanode;
	}
	
	public ManualEntry man() {
		return null;
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
			Node n = doc.selectSingleNode("./fsxml/properties/input");			
			if(n != null && n instanceof Element){
				props.put("input", ((Element)n).getText());				
			} else {
				return null;
			}
		}		
		return props;
	}
}
