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
