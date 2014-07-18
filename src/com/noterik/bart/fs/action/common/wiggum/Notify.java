/* 
* Notify.java
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
package com.noterik.bart.fs.action.common.wiggum;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.xml.sax.SAXException;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.action.common.ingest.IngestAudioAction;


public class Notify extends ActionAdapter{
	
	private static Logger logger = Logger.getLogger(IngestAudioAction.class);
	
	@Override
	public String run() {
		
		String response = "";
		
				System.out.println("New Failed Upload Detected! Notifying devel-list.");	
		
		try {
			if(MailSender.okToSend("http://localhost:8080/bart/domain/springfieldwebtv/tmp/wiggum")){
			String to = new String("dimitrios@noterik.com");
			String subject = new String("upload failure!");
			String body = new String("Uploading files failure report.\nPlease see the log for more information.\n\nhttp://localhost:8080/bart/domain/springfieldwebtv/tmp/wiggum");
			MailSender mail = new MailSender(to, subject, body);
			mail.send();
			
				
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return response;
	}
	
	

}
