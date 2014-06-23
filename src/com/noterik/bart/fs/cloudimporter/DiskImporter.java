package com.noterik.bart.fs.cloudimporter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.dom4j.Document;
import org.dom4j.Element;

import com.noterik.bart.fs.LazyHomer;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;

public class DiskImporter {
	
	public static void importSmithersNode(String uri) {
		Document nodeXML = FSXMLRequestHandler.instance().getNodeProperties(uri, false);
		if (nodeXML!=null) {
			Element p = (Element) nodeXML.selectSingleNode("/fsxml/nodes/properties/importurl");
			if (p==null) return;
			String importurl = p.getText();
			if (p!=null && !p.getText().equals("")) {
				FSXMLRequestHandler.instance().handlePUT(uri+"/properties/importurl","");
				String l[] = importurl.split(",");
				String target = l[0];
				String source = l[1];	
				System.out.println("IMPORTING NODES TO="+target+" SOURCE="+source);

				LazyHomer.send("TRACE", importurl, "Starting import");
				String path = "/springfield/smithers/import/"+source;
				if (LazyHomer.isWindows()) {
					path = "c:\\springfield\\smithers\\import\\"+source;
				}
				importCloud(path,target);
			}
		} else {
			System.out.println("IMPORT NODE NOT VALID ("+uri+")");
		}
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
		    		System.out.println("HEADER="+header+" "+uri+child.getName());
					FSXMLRequestHandler.instance().handlePUT(uri+child.getName()+"/properties",body);
					
					// so its a directory how cool so there is subs ! 
					importCloud(path+File.separator+child.getName(),uri+child.getName()+"/"); // *** recursive ***
				}
			}
		}
	}
	
}

