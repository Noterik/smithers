/* 
* DefaultImporter.java
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
package com.noterik.bart.fs.cloudimporter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.dom4j.Document;
import org.dom4j.Element;

import com.noterik.bart.fs.LazyHomer;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;

public class DefaultImporter {
	
	public static void importDefaultCloud() {
		System.out.println("Starting default cloud import");
		String path = "/springfield/smithers/default/domain";
		if (LazyHomer.isWindows()) {
			path = "c:\\springfield\\smithers\\default\\domain";
		}
		importCloud(path,"/domain/");
		System.out.println("\nFinished default cloud import");
	}
	
	
	private static void importCloud(String path,String uri) {
		File dir = new File(path);
		File[] children = dir.listFiles();
		if (children != null) {
			for (File child : children) {
				if (child.isDirectory()) {
					System.out.print(".");
					
					// now we check INSIDE the dir to see if we have attributes file !
					String referid = null;
					File attrfile = new File(path+File.separator+child.getName()+File.separator+"attributes.txt");
					if (attrfile.exists()) {
						try {
							BufferedReader input = new BufferedReader(new FileReader(attrfile));
						    String line;
						    while ((line = input.readLine()) != null) {
						    	int pos = line.indexOf("<referid>");
						    	if (pos!=-1) {
						    		referid = line.substring(pos+9);
						    		referid = referid.substring(0,referid.indexOf("<"));
									String body = "<fsxml><attributes><referid>"+referid+"</referid></attributes></fsxml>"; 
									FSXMLRequestHandler.instance().handlePUT(uri+child.getName()+"/attributes",body);
						    	}
						    }
						    input.close();
						} catch(Exception e) {
							
						}
					}

					
					
					String header = "<fsxml>";
					String body = "<properties>";
					// now we check INSIDE the dir to see if we have properties !
					File propfile = new File(path+File.separator+child.getName()+File.separator+"properties.txt");
					if (propfile.exists()) {
						try {
							BufferedReader input = new BufferedReader(new FileReader(propfile));
						    String line;
						    while ((line = input.readLine()) != null) {
						    	if (line.indexOf("<mimetype>")!=-1) {
						    		line = line.substring(11);
						    		int pos = line.indexOf("<");
						    		header = "<fsxml mimetype=\""+line.substring(0,pos)+"\">";
						    	} else if (line.indexOf("<querttime>")!=-1) {	
						    	} else if (line.indexOf("properties>")!=-1) {
						    		// do nothing kill these lines
						    	} else {
						    		body+=line; // add the line
						    	}
						    }
						    input.close();
						} catch(Exception e) {
							
						}
					}
					body += "</properties></fsxml>";
					body = header+body;
		    	//	System.out.println("HEADER="+header+" "+uri+child.getName());
					FSXMLRequestHandler.instance().handlePUT(uri+child.getName()+"/properties",body);
					
					// so its a directory how cool so there is subs ! 
					importCloud(path+File.separator+child.getName(),uri+child.getName()+"/"); // *** recursive ***
				}
			}
		}
	}
	
}
