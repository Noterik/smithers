/* 
* DiskExporter.java
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
package com.noterik.bart.fs.cloudexporter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.tree.*;

import com.noterik.bart.fs.LazyHomer;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.bart.fs.type.MimeType;
import com.noterik.springfield.tools.fs.URIParser;

public class DiskExporter {
	
	private static int count = 0;
	private static String[] ignorelist = {"depth","start","limit","totalResultsAvailable","totalResultsReturned"};


	public static void exportSmithersNode(String uri) {
		Document nodeXML = FSXMLRequestHandler.instance().getNodeProperties(uri, false);
		if (nodeXML!=null) {
			Element p = (Element) nodeXML.selectSingleNode("/fsxml/nodes/properties/exporturl");
			if (p==null) return;
			String exporturl = p.getText();
			if (p!=null && !p.getText().equals("")) {
				FSXMLRequestHandler.instance().handlePUT(uri+"/properties/exporturl","");
				System.out.println("EXPORTING NODES FROM="+exporturl);
				count = 0;
				LazyHomer.send("TRACE", exporturl, "Starting export");
				exportNodes(exporturl,"/springfield/smithers/export"+exporturl);
			}
		} else {
			System.out.println("EXPORT NODE NOT VALID ("+uri+")");
		}
	}
	
	public static void main(String [ ] args)
	{
	}
	
	private static boolean exportNodes(String exporturl,String exportpath) {
	//	if (exporturl.indexOf("/user")!=-1) return true;
		//Skip backing up the indexes
		/*
		if (exporturl.indexOf("/collectionindex")!=-1) return true;
		if (exporturl.indexOf("/keywordindex")!=-1) return true;
		if (exporturl.indexOf("/subtitlesindex")!=-1) return true;
		if (exporturl.indexOf("/speakersindex")!=-1) return true;
		if (exporturl.indexOf("/themesindex")!=-1) return true;
		if (exporturl.indexOf("/topicsearch")!=-1) return true;
		*/
		System.out.println("("+(count++)+") URL="+exporturl);
		File dirs = new File(exportpath);
		boolean result=dirs.mkdirs();
		if(dirs.exists()) {
			result = true;
		}
		if (!result) return false;
		 
		Document exportXML = FSXMLRequestHandler.instance().handleDocGET(exporturl, "<properties><depth>0</depth></properties>");
		if (exportXML!=null) {
			
			for (Iterator i = exportXML.nodeIterator(); i.hasNext();) {
				Element fsnode = (Element)i.next();
				if(fsnode.getName().equals("error")) return true; // Check if the current node is <error>
				for (Iterator j = fsnode.nodeIterator(); j.hasNext();) {
					Element mnode = (Element)j.next();
					for (Iterator k = mnode.nodeIterator(); k.hasNext();) {
						Element dnode = (Element)k.next();
						String name = dnode.getName();
						String id = dnode.attributeValue("id");
						//System.out.println("DNAME="+name);
						if (Arrays.asList(ignorelist).contains(name)) {
							System.out.println("ignoring "+name);
						} else if (!name.equals("properties")) {
							exportNodes(exporturl+"/"+name+"/"+id,exportpath+"/"+name+"/"+id);
						} else {
							try {
								MimeType mimetype = FSXMLRequestHandler.instance().getMimeTypeOfResource(exporturl);
								BufferedWriter propfile = new BufferedWriter(new FileWriter(exportpath+"/properties.txt"));
								String propbody = "<properties>\n";
								// set the mimetype
								propbody+="\t<mimetype>";
								if (mimetype.toString().equals("MIMETYPE_FS_SCRIPT")) {
									propbody+="application/fsscript";
								} else if (mimetype.toString().equals("MIMETYPE_FS_COMMAND")) {
										propbody+="application/fscommand";
								} else {
									propbody+="text/fsxml";
								}
								propbody+="</mimetype>\n";								
								for (Iterator l = dnode.nodeIterator(); l.hasNext();) {
									Object p = l.next();
									if (p instanceof Element) {
										Element pnode = (Element)p;
										String pname = pnode.getName();
										String pvalue = pnode.getText();
										propbody += "\t<"+pname+">"+pvalue+"</"+pname+">\n";	
									} else if (p instanceof DefaultText) {
										DefaultText tnode = (DefaultText)p;
										//System.out.println("DEFTEXT="+tnode.toString());
										String pname = tnode.getName();
										String pvalue = tnode.getText();
										//propbody += "\t<"+pname+">"+pvalue+"</"+pname+">\n";	
									} else if (p instanceof DefaultComment) {
										DefaultComment cnode = (DefaultComment)p;
										String pname = cnode.getName();
										String pvalue = cnode.getText();
										//propbody += "\t<!-- <"+pname+">"+pvalue+"</"+pname+"> -->\n";	
										//System.out.println("DEFCOMMENT="+cnode.toString());
									} else {
										System.out.println("UNCATCHED TYPE="+p.toString());
									}
								}
								propbody += "</properties>\n";
								propfile.write(propbody);
								propfile.close();
							} catch(Exception e) {
								System.out.println("PROP ERROR");
								e.printStackTrace();
							}
						}
						
						if (mnode.attributeValue("id")!=null && mnode.attributeValue("referid")!=null && !name.equals("user")) {
							try {
								System.out.println("MNODE2="+mnode.asXML());
								BufferedWriter attrfile = new BufferedWriter(new FileWriter(exportpath+"/attributes.txt"));
								String attrbody = "<attributes>\n";
								attrbody += "\t<referid>"+mnode.attributeValue("referid")+"</referid>\n";			
								attrbody += "</attributes>\n";
								attrfile.write(attrbody);
								attrfile.close();	
							} catch(Exception e) {
								e.printStackTrace();
							}		
						}
						
					}
				}
			}
				
			return true;
		} else {
			System.out.println("EXPORT URL NOT VALID ("+exporturl+")");
			return false;
		}
	}
	
	
}
