/* 
* TimeLine.java
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
package com.noterik.bart.fs.fscommand.dynamic.playlist.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;

public class TimeLine {

	private Element _presentation = null;
	private List oldnodes = new ArrayList<Node>();
	private List newnodes = new ArrayList<Node>();
	
	public TimeLine(Element pr) {
		_presentation = pr;
		mapAllEvents(pr);
	}
	
	private void mapAllEvents(Element pr) {
		Element pl = (Element) pr.selectSingleNode("videoplaylist");
		//System.out.println("PLNODE="+pl);
		//sometimes a presentation on accident doesn't have an videoplaylist
		if (pl == null) {
			return;
		}
		for(Iterator<Node> iter = pl.elements().iterator(); iter.hasNext(); ) {
			Element node = (Element) iter.next();
			//System.out.println("NODE NAME="+node.getName());
			if (!node.getName().equals("video") && !node.getName().equals("properties")) {
				oldnodes.add(node);
				//System.out.println("N="+node.asXML());
			}
		}
		//System.out.println("OLDNODES="+oldnodes.size());	
	}
	
	public void remapEvents(float starttime,float duration,float gap) {
		//System.out.println("S="+starttime+" D="+duration+" G="+gap);
		
		for(Iterator<Node> iter = oldnodes.iterator(); iter.hasNext(); ) {
			Element node = (Element) iter.next();
			float nstart = Float.valueOf(node.selectSingleNode("properties/starttime").getText());
			float ndur = Float.valueOf(node.selectSingleNode("properties/duration").getText());

			//System.out.println("NSTART="+nstart+" DUR="+ndur+" NAME="+node.getName());
			
			// is the node in the range of the remap ?
			if (nstart<starttime) {
				Element newnode = (Element)node.clone();
				// it start before the gap so move start
				newnode.selectSingleNode("properties/starttime").setText(""+(nstart-gap));	
				//System.out.println("MOVE START="+node.getName());
				// is the end also before it ?
				if (starttime<(nstart+ndur)) {
					System.out.println("END ALSO BEFORE");
				} else {
					System.out.println("END IS IN THE BLOCK");
				}
				
			}
			
			
			/*
			if (nstart>=starttime && nstart<(starttime+duration)) { // nstart is in the range 
				// so we have to copy the full or part of the block
				Element newnode = (Element)node.clone();
				
				if ((nstart+ndur)<(starttime+duration)) {
					// yes no need to cut
					newnode.selectSingleNode("properties/starttime").setText(""+(nstart-gap));
				} else {
					newnode.selectSingleNode("properties/starttime").setText(""+(nstart-gap));
					System.out.println("NEWDUR="+node.getName()+" "+((nstart+ndur)-(starttime+duration)));
					newnode.selectSingleNode("properties/duration").setText(""+((nstart+ndur)-(starttime+duration)));
				}
				//System.out.println("NEWNODE="+newnode.asXML());	
				newnodes.add(newnode);
			}
			*/
		}
	}
	
	public void remapVideoEvents(Element fsxml) {
		Element pl = (Element) _presentation.selectSingleNode("videoplaylist");
		//System.out.println("PLNODE="+pl);
		float offset = 0;
		int mapcounter = 0;
		for(Iterator<Node> iter = pl.elements().iterator(); iter.hasNext(); ) {
			Element node = (Element) iter.next();
			if (node.getName().equals("video")) {
				//System.out.println("V1="+node.asXML());
				float starttime = 0f;
				if (node.selectSingleNode("properties/starttime")!=null && !node.selectSingleNode("properties/starttime").getText().equals("")) {
					starttime = Float.valueOf(node.selectSingleNode("properties/starttime").getText());
				}
				float duration = 999999999f; // a bit of a hack
				if (node.selectSingleNode("properties/duration")!=null && !node.selectSingleNode("properties/duration").getText().equals("")) {
					duration = Float.valueOf(node.selectSingleNode("properties/duration").getText());
				}
				String referid = node.attributeValue("referid");
				//System.out.println("XMLL="+fsxml.asXML());
				Element videonode = (Element) fsxml.selectSingleNode("video[@fullid='"+referid+"']");
				
				//error case, but sometimes occurs
				if (videonode == null) {
					return;
				}
				
				//Element videonode = (FSXMLRequestHandler.instance().getNodeProperties(referid,true)).getRootElement().element("video");
				//System.out.println("VID="+referid);
					
				for(Iterator<Node> viter = videonode.elements().iterator(); viter.hasNext(); ) {
					Element lnode = (Element) viter.next();
					String name = lnode.getName();
					if (!name.equals("properties") && !name.equals("rawvideo") && !name.equals("screens")) {

						// so we have a node lets remap it and place it in the timeline
						Element nnode = (Element)lnode.clone();
						float ns = 0;
						float nd = 0;
						
						if (nnode.selectSingleNode("properties/starttime") != null && nnode.selectSingleNode("properties/starttime").getText() != null) {
							try {
								ns= Float.valueOf(nnode.selectSingleNode("properties/starttime").getText());
							} catch (Exception e) { /* ignore */ }
						}
						if (nnode.selectSingleNode("properties/duration") != null && nnode.selectSingleNode("properties/duration").getText() != null) {
							try {
								nd= Float.valueOf(nnode.selectSingleNode("properties/duration").getText());
							} catch (Exception e) { /* ignore */ }
						}
						//System.out.println("LN="+lnode.getName()+" "+lnode.asXML());
						//these are our defaults, no need to shift if these are the same
						float defaultDuration = 999999999f;
						if (Float.compare(ns, nd) == 0 && Float.compare(ns, starttime) == 0 && Float.compare(duration, defaultDuration) == 0) {
							continue;
						}
						
						// is this block used in this video's start/duration range ?
						if (ns>=starttime && ns<=(starttime+duration)) {
							// we need to shift the starttime by both the offset and starttime
							// does the end point also fall in the range, if not we need to shorten it
							float newstart = ((ns-starttime)+offset);
							if ((ns+nd)<(starttime+duration)) {
								// yes no need to cut
								try {
									nnode.selectSingleNode("properties/starttime").setText(""+newstart);
								} catch (Exception e) { /* ignore */ }
							} else {
								//System.out.println("LN="+lnode.getName()+" "+lnode.asXML());
								//System.out.println("NEED CUT "+ns+" "+nd+" "+starttime+" "+duration+" "+newstart);
								//System.out.println("NEW DUR "+((ns+nd)-(starttime+duration)));
								try {
									nnode.selectSingleNode("properties/starttime").setText(""+newstart);	
									nnode.selectSingleNode("properties/duration").setText(""+((ns+nd)-(starttime+duration)));
								} catch (Exception e) { /* ignore */ }
							}
							newnodes.add(nnode);
							mapcounter++;
						}
					}
				}
				offset += duration; // shift the offset so all the events of the next video are shifted
			}
		}
		//System.out.println("MAPCOUNTER="+mapcounter);
	}
	
	public void insertNode(Element node) {
		newnodes.add(node);
	}
	
	
	public void remapOldEvents() {
		Element pl = (Element) _presentation.selectSingleNode("videoplaylist");
		//System.out.println("PLNODE="+pl);
		float offset = 0;
		int mapcounter = 0;
		for(Iterator<Node> iter = pl.elements().iterator(); iter.hasNext(); ) {
			Element node = (Element) iter.next();
			if (node.getName().equals("video")) {
				//System.out.println("V1="+node.asXML());
				float starttime = 0;
				if (node.selectSingleNode("properties/starttime")!=null && !node.selectSingleNode("properties/starttime").getText().equals("")) {
					starttime = Float.valueOf(node.selectSingleNode("properties/starttime").getText());
				}
				float duration = 999999999; // a bit of a hack
				if (node.selectSingleNode("properties/duration")!=null && !node.selectSingleNode("properties/duration").getText().equals("")) {
					duration = Float.valueOf(node.selectSingleNode("properties/duration").getText());
				}
				for(Iterator<Node> viter = oldnodes.iterator(); viter.hasNext(); ) {
					Element lnode = (Element) viter.next();
					String name = lnode.getName();
					if (!name.equals("properties") && !name.equals("rawvideo") && !name.equals("screens")) {

						// so we have a node lets remap it and place it in the timeline
						Element nnode = (Element)lnode.clone();
						float ns = 0L;
						float nd = 0L;
						if (nnode.selectSingleNode("properties/starttime") != null && nnode.selectSingleNode("properties/starttime").getText() != null) {
							try {
								ns= Float.valueOf(nnode.selectSingleNode("properties/starttime").getText());
							} catch (Exception e) {
								//System.out.println("NS="+nnode.selectSingleNode("properties/starttime").getText());
								//e.printStackTrace();
							}
						} else {
							Element nnnode = (Element) nnode.selectSingleNode("//properties");
							nnnode.addElement("starttime").addText(Float.toString(ns));
						}
						if (nnode.selectSingleNode("properties/duration") != null && nnode.selectSingleNode("properties/duration").getText() != null) {
							try {
								nd= Float.valueOf(nnode.selectSingleNode("properties/duration").getText());
							} catch (Exception e) { /* ignore */ }
						} else {
							Element nnnode = (Element) nnode.selectSingleNode("//properties");
							nnnode.addElement("duration").addText(Float.toString(nd));
						}
						
						// is this block used in this video's start/duration range ?
						if ((ns>=starttime && ns<=(starttime+duration)) || ((ns+nd)>starttime)) {
				
							// we need to shift the starttime by both the offset and starttime
							// does the end point also fall in the range, if not we need to shorten it
							float newstart = ((ns-starttime)+offset);
							
							/* Daniel/Pieter removed because not sure what it did, first check if trouble.
							if(((ns+nd)>starttime)) {
								newstart = starttime + offset;
							}
							*/
							
							if ((ns+nd)<(starttime+duration)) {
								// yes no need to cut
								nnode.selectSingleNode("properties/starttime").setText(""+newstart);	
							} else {
								//System.out.println("LN="+lnode.getName()+" "+lnode.asXML());
								//System.out.println("NEED CUT "+ns+" "+nd+" "+starttime+" "+duration+" "+newstart);
								//System.out.println("NEW DUR "+(nd-((ns+nd)-(starttime+duration))));	
								nnode.selectSingleNode("properties/starttime").setText(""+newstart);	
								nnode.selectSingleNode("properties/duration").setText(""+(nd-((ns+nd)-(starttime+duration))));			
							}
							newnodes.add(nnode);
							mapcounter++;
						}
					}
				}
				offset += duration; // shift the offset so all the events of the next video are shifted
			}
		}
		//System.out.println("MAPCOUNTER OLD="+mapcounter);
	}

	
	public void insertNodesInPresentation() {
		//System.out.println("COPY IN PRES "+newnodes.size());
		// remove all the old event nodes, so we can replace them with the new ones.
		Element pl = (Element) _presentation.selectSingleNode("videoplaylist");
		for(Iterator<Node> iter = pl.elements().iterator(); iter.hasNext(); ) {
			Element node = (Element) iter.next();
			if (!node.getName().equals("video") && !node.getName().equals("properties")) {
				node.detach();
			}
		}

		for(Iterator<Node> iter = newnodes.iterator(); iter.hasNext(); ) {
			Element newnode = (Element) iter.next();
			//System.out.println("NEWNODE="+newnode.asXML());
			((Element)_presentation.selectSingleNode("videoplaylist")).add(newnode);
		}

	}
	
	public List<Node> getSortedList(List<Node> input) {
		
	    final class SortElement implements Comparator<SortElement> {
	        // start stepping through the array from the beginning
	        public double starttime;
	        public Node value; 
	        
	        public int compare(SortElement n,SortElement n2) {
	            return (n.starttime<n2.starttime ? -1 : (n.starttime==n2.starttime ? 0 : 1));
	        }

	    }
	    
	    List<SortElement> sorted = new ArrayList<SortElement>();
		
		for (Iterator<Node> i = input.iterator();i.hasNext();) {
			Node block = i.next();
			SortElement sn = new SortElement();
			sn.starttime = Double.parseDouble(block.selectSingleNode("properties/starttime").getText());
			sn.value = block;
			sorted.add(sn);
		}
		Collections.sort(sorted,new SortElement());
		
		List<Node> result = new ArrayList<Node>();
		for (Iterator<SortElement> i = sorted.iterator();i.hasNext();) {
			SortElement e = i.next();
			result.add(e.value);
		}
		return result;
	}

}
