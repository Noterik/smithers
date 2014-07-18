/* 
* checkup.java
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
package com.noterik.bart.fs.action.common.videoimport;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.UnmappableCharacterException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.fsutil.Encoding;

/**
 *
 * @author Daniel Ockeloen <daniel@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.action
 * @access private
 * @version $Id: checkup.java,v 1.4 2012-07-04 09:55:51 daniel Exp $
 *
 */
public class checkup extends ActionAdapter {
	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(checkup.class);
	//private List<String> languages = new ArrayList<String>();
	private HashMap<String,ArrayList<String>> ids = new HashMap<String,ArrayList<String>>();
	private static final int BOM_SIZE = 4;
	
	
	public String run() {	

		//if (1==1) return null;
		if (event.getUri().indexOf("subtitles")!=-1) {
			return null;
		}
		
		logger.debug("starting action");
		
		System.out.println("VIDEO IMPORT CHECKUP="+event.getUri());
		// parse request
		String requestBody = event.getRequestData();
		String uri = event.getUri();
		try {
			Document doc = DocumentHelper.parseText(requestBody);
			//System.out.println("BODY="+doc.asXML());
			
			Element prop = (Element)doc.selectSingleNode("//properties");
			for(Iterator<Node> iter = prop.nodeIterator(); iter.hasNext(); ) {
				Node node = iter.next();
				if(node.getName()!=null) {
					if (node.getName().indexOf("srt_")!=-1 && node.getName().indexOf("srt_md5")==-1) {
						System.out.println("PROP="+node.getText());	
						mapSubtitles(node.getName().substring(4),node.getText(), "Windows-1252");
					}
				}
			}
			
	        String newbody = "<fsxml>";
			for(int i=0;i<ids.size();i++) {
				ArrayList word = ids.get(""+(i+1));
				if (word!=null) {
		        	newbody+="<subtitles id=\""+word.get(0)+"\"><properties>";
		        	newbody+="<starttime>"+word.get(1)+"</starttime>";
		        	newbody+="<duration>"+word.get(2)+"</duration>";
					for(int j=3;j<word.size();j++) {
						String line = (String)word.get(j);
						String cc = line.substring(0,2);
						String value = (String)word.get(j);
						if (cc.equals("uk")) {
							newbody+="<text>"+Encoding.encode(value.substring(3))+"</text>";
						} else {
							newbody+="<"+cc+"_text>"+Encoding.encode(value.substring(3))+"</"+cc+"_text>";
						}
					}
		        	newbody+="</properties></subtitles>";			
				}
			}
	        newbody+="</fsxml>";
		//	System.out.println("BODY="+newbody);
	        MessageDigest m=MessageDigest.getInstance("MD5");
	        m.update(newbody.getBytes(),0,newbody.length());
	        String md5 = ""+new BigInteger(1,m.digest()).toString(16);
	       // System.out.println("MD5="+md5);
	        
	        // check if we have a new md5
	        String oldmd5 = FSXMLRequestHandler.instance().getPropertyValue(uri+"/properties/srt_md5");
	       // System.out.println("OLDMD5="+oldmd5);
	        if (!md5.equals(oldmd5)) {
	        	System.out.println("MD5 CHANGED DOING INSERT ON "+uri);
	        	FSXMLRequestHandler.instance().handlePUT(uri+"/properties/srt_md5",md5);
	        	System.out.println("DELETING old subtitles: " + uri + "/subtitles/");
	        	FSXMLRequestHandler.instance().handleGET(uri + "/subtitles/?method=delete", null);
	        	System.out.println("PUT RESULT="+FSXMLRequestHandler.instance().handlePUT(uri+"/properties",newbody));
	        } else {
	        	System.out.println("MD5 MATCH NO INSERT");
	        }

		} catch (Exception e) {
			logger.error("",e);
			System.out.println(e);
		}	
		return null;
	}
	
	private void mapSubtitles(String name,String url, String chars) {
		System.out.println("URL="+url);
		if (url.indexOf("http://")==-1) {
			url = "http://"+url;
		}	
		try {
			
			URL loadurl = new URL(url);
			Charset charset = Charset.forName(chars);
			CharsetDecoder decoder = charset.newDecoder();
			InputStream inStream = loadurl.openStream();
			//Get rid of the BOM if exists
			inStream = checkForUtf8BOMAndDiscardIfAny(inStream);
			
			InputStreamReader reader = new InputStreamReader(inStream, decoder);
			BufferedReader in = new BufferedReader(reader);

			String subtitleId = null;
			String startEndTime = null;
			String subtitleText = null;
			String newbody = "<fsxml>";
        
			while ((subtitleId = in.readLine()) != null) {
				System.out.println("Subtitle ID = " + subtitleId);
				startEndTime =  in.readLine();
				subtitleText =  in.readLine();
				String blank = in.readLine();
				if(blank!=null) {
					while (blank.length()>2) {
						subtitleText+=" --- "+blank;
						blank = in.readLine();
						//newbody += blank;
	 
					}
				}
				int splitpos  = startEndTime.indexOf("-->");
				int starttime = decodeSrtTime(startEndTime.substring(0,splitpos-1));
				int endtime = decodeSrtTime(startEndTime.substring(splitpos+4));
				int duration = endtime-starttime;

				ArrayList<String> words = ids.get(subtitleId);
				if (words==null) {
					// ok lets insert our language
					words = new ArrayList<String>();
					words.add(subtitleId);
					words.add(""+starttime);
					words.add(""+duration);
					words.add(name+"="+subtitleText);
					ids.put(subtitleId, words);
				} else {
					// we already have this subtitle, lets add our new language
					if(name.equals("uk")) { //We always want to use the timing from the UK subs
						String sub = words.get(3);
						words.remove(3);
						words.remove(2);
						words.remove(1);
						words.add(1,""+starttime);
						words.add(2,""+duration);
						words.add(3,sub);
					}
					words.add(name+"="+subtitleText);

			
				}
				//System.out.println("W="+words.toString());
        	}	
        	in.close();
        	//System.out.println("BODY="+newbody);
		} catch (UnmappableCharacterException e) {
			if (chars.equals("Windows-1252")) {
				mapSubtitles(name,url,"UTF-8");
			}			
		} catch (Exception e) {
			logger.error("",e);
			System.out.println(e);
		}
	}
	
	private static InputStream checkForUtf8BOMAndDiscardIfAny(InputStream inputStream) throws IOException {
	    PushbackInputStream pushbackInputStream = new PushbackInputStream(new BufferedInputStream(inputStream), 3);
	    byte[] bom = new byte[3];
	    if (pushbackInputStream.read(bom) != -1) {
	        if (!(bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF)) {
	            pushbackInputStream.unread(bom);
	        }
	    }
	    return pushbackInputStream; 
	}
	
	private int decodeSrtTime(String timestring) {
		//System.out.println("TIME="+timestring);
		int result = Integer.parseInt(timestring.substring(0,2))*3600000;
		result += Integer.parseInt(timestring.substring(3,5))*60000;
		result += Integer.parseInt(timestring.substring(6,8))*1000;
		result += Integer.parseInt(timestring.substring(9));
		return result;
	}	
}
