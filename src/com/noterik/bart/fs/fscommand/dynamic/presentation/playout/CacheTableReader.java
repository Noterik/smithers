/* 
* CacheTableReader.java
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
package com.noterik.bart.fs.fscommand.dynamic.presentation.playout;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.Element;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;


import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;

public class CacheTableReader extends Thread {
	
	private boolean loaded = false;
	private volatile boolean running = true;
	
	public CacheTableReader(String name) {
		super(name);
	}
	
	public void run() {
		if (running) {
			loaded = true;
			return;
		}
		try {
			String hostname = InetAddress.getLocalHost().toString();
			int pos = hostname.indexOf("/");
			if (pos!=-1) hostname=hostname.substring(pos+1);
			Document cacheXml = FSXMLRequestHandler.instance().getNodeProperties("/domain/webtv/tmp/cache/dataset/"+hostname, false);
			if (cacheXml!=null) {
				System.out.println("Reader cache tables "+cacheXml.asXML());
				Element p = (Element) cacheXml.selectSingleNode("/fsxml/dataset[@id='"+hostname+"']/properties/list");
				if (p!=null) {
					
					String list[] = p.getText().split(",");
					for(int i=0;i<list.length;i++) {
						System.out.println("ITEM ("+(i+1)+")="+list[i]);
						Document result = null;
						String key = list[i];
						key = key.replace(";", ",");
						int cpos = key.indexOf("{");
						if (cpos==-1) {
							result = FSXMLRequestHandler.instance().getNodeProperties(list[i], false);
							cache.put(list[i], result);
						} else {
							try {
								String params = key.substring(cpos+1);
								params = params.substring(0,params.length()-1);
								//System.out.println("P1="+params);
								String[] pa = params.split(",");
								String pb = "<fsxml><properties>";
								for (int pi=0;pi<pa.length;pi++) {
									int ispos = pa[pi].indexOf("=");
									String name=pa[pi].substring(0,ispos);
									String value=pa[pi].substring(ispos+1);
									if (name.charAt(0)==' ') name=name.substring(1);
									pb+="<"+name+">"+value+"</"+name+">";
								}
								pb+="</properties></fsxml>";
								System.out.println("PB="+pb);
								result = FSXMLRequestHandler.instance().handleDocGET(key.substring(0,cpos), pb);

							} catch(Exception ee) {
								System.out.println("ERROR WE NEED TO LOOK AT DANIEL");
							}
							//System.out.println("RSULT="+result);
							//System.out.println("EEEE="+key.substring(0,cpos)+" R="+key.substring(cpos));
							
							if (result!=null) cache.putParams(key.substring(0,cpos), result, key.substring(cpos));							
						}
						sleep(30);
					}
					loaded = true;
				}
				System.out.println("Reader cache tables loaded size="+cache.getCacheSize());	

			} else {
				loaded = true;
				System.out.println("Reader cache tables empty");
			}

		} catch(Exception e) {
			System.out.println("Can't sleep in CacheTableReader "+e);
		}
		loaded = true;
	}
	
	public boolean fullyLoaded() {
		return loaded;
	}
	
	

}
