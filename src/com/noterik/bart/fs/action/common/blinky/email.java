package com.noterik.bart.fs.action.common.blinky;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.action.common.wiggum.MailSender;
import com.noterik.springfield.tools.fs.URIParser;

/**
 *
 * @author Daniel Ockeloen <daniel@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.action
 * @access private
 * @version $Id: email.java,v 1.2 2012-06-22 08:02:50 dimitrios Exp $
 *
 */
public class email extends ActionAdapter {
	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(email.class);
	
	
	public String run() {	
		logger.info("Action Triggered");
		String requestBody = event.getRequestData();
		logger.debug("XML: " + requestBody);
		String uri = event.getUri();
		logger.debug("URI" + uri);
		Document doc = null;
		String[] fields = uri.split("/");
		//System.out.println("GOT REQUEST\n"+ requestBody+"\n"+uri);
		String eventTrigger = "";
		String user = fields[6];
		String domain = URIParser.getDomainFromUri(uri);
		
		String jobID = fields[8];
		
		String failMessage="Dear "+user+",\nYou started the file upload at "+domain+"." +
				" Unfortunately the upload of file "+jobID+" has failed. " +
				"Click on this link (link to upload page) to retry your upload. \n\n" +
				"Best regards,\n\n"+domain;
		
		String successMessage = "Dear "+ user+"," +
				"\n\nThe file "+jobID+" has been successfully uploaded, transcoded and saved. " +
				"Click on this link (link to upload page) to see your overview of uploads. " +
				"\n\nBest regards, \n\n"+domain;
		String failSubject = "Notification: Upload Job Failure!";
		
		String successSubject = "Notification: Upload Job Successfully completed!";
		
		String startProcess = "Dear "+ user+",\n\nYou started a file upload at "+ domain+"." +
				"The file "+jobID+" is successfully uploaded. It will be transcoded and saved in our system." +
				" We will send you an email with a link to your overview of uploads when this process is completed." +
				"\n\nBest regards,\n\n"+domain;
		
		System.out.println("job id:: "+jobID);
		//System.out.println("event:"+fields[11]+"\nuser:"+user);
		
		if (requestBody != null) {
			try {
				doc = DocumentHelper.parseText(requestBody);
			} catch (DocumentException e) {
				e.printStackTrace();
			}
			
			if(fields.length>11)eventTrigger = fields[11];
			System.out.println("Got request for "+eventTrigger);
			
			if(eventTrigger.equals("ingest")){
				
				Node status = doc.selectSingleNode("//properties/status");
				
				if(status.getText().equals("init")){
					
				}
				
				else if(status.getText().equals("failed")){
					String userEmail = getEmail(domain, user);
					if (userEmail == null) return "failed";
					System.out.println("email::"+userEmail);
					
					String to = new String(userEmail);
					String subject = new String("Notification: Upload Job Failure!");
					String body = new String(failMessage);
					MailSender mail = new MailSender(to, subject, body);
					mail.send();
					System.out.println("mail sent; event:" + eventTrigger + " ; status:" + status.getText());					
				}
				
			}
			
			else if(eventTrigger.equals("uploading")){
				Node status = doc.selectSingleNode("//properties/status");
				
				if(status.getText().equals("failed")){
					String userEmail = getEmail(domain, user);
					if (userEmail == null) return "failed";
					System.out.println("email::"+userEmail);
					MailSender mail = new MailSender(userEmail, failSubject, failMessage);
					mail.send();
					System.out.println("mail sent; event:" + eventTrigger + "; status:" + status.getText());					
				}
				else if(status.getText().equals("done")){
					String userEmail = getEmail(domain, user);
					if (userEmail == null) return "failed";
					System.out.println("email::"+userEmail);
					
					String to = new String(userEmail);
					String subject = new String("Notification: Upload Job Started!");
					String body = new String(startProcess);
					MailSender mail = new MailSender(to, subject, body);
					mail.send();
					System.out.println("mail sent; event:" + eventTrigger + "; status:" + status.getText());
					
				}
				
			}
			
			else if(eventTrigger.equals("transcode")){
				Node status = doc.selectSingleNode("//properties/status");
				if(status.getText().equals("failed")){
					String userEmail = getEmail(domain, user);
					if (userEmail == null) return "failed";
					System.out.println("email::"+userEmail);
					MailSender mail = new MailSender(userEmail, failSubject, failMessage);
					mail.send();
					System.out.println("mail sent; event:" + eventTrigger + "; status:" + status.getText());					
				}
				
			}
			
			else if(eventTrigger.equals("screenshot")){
				Node status = doc.selectSingleNode("//properties/status");
				if(status.getText().equals("failed")){
					String userEmail = getEmail(domain, user);
					if (userEmail == null) return "failed";
					System.out.println("email::"+userEmail);
					MailSender mail = new MailSender(userEmail, failSubject, failMessage);
					mail.send();
					System.out.println("mail sent; event:" + eventTrigger + "; status:" + status.getText());					
				}
				else if(status.getText().equals("done")){
					String userEmail = getEmail(domain, user);
					if (userEmail == null) return "failed";
					System.out.println("email::"+userEmail);
					String to = new String(userEmail);
					String subject = new String("Upload Jog Successully Completed!");
					String body = new String(successMessage);
					MailSender mail = new MailSender(userEmail, successSubject, successMessage);
					mail.send();
					System.out.println("mail sent; event:" + eventTrigger + "; status:" + status.getText());		
					
				}
				
			}
		}
		System.out.println("WOOOOOO EXIT");
		return null;
	}
	
	private static String makeGetReq(String req){
		
		URL url;
		HttpURLConnection conn;
		BufferedReader rd;
		String line;
		String result = "";
			
		try {
	         url = new URL(req);
	         conn = (HttpURLConnection) url.openConnection();
	         conn.setRequestMethod("GET");
	         rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	         while ((line = rd.readLine()) != null) {
	            result += line;
	         }
	         rd.close();
	         conn.disconnect();
	      } catch (Exception e) {
	         e.printStackTrace();
	      }
			return result;
}
	
	private String getEmail(String domain, String user){
		String userInfoReq = "http://c7.noterik.com:8080/simple-um/restlet/domain/"+domain+"/user/"+user;
		String email= null;
		
		String userinfo =  makeGetReq(userInfoReq);

		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder;
		try {
			docBuilder = docBuilderFactory.newDocumentBuilder();
			org.w3c.dom.Document document = docBuilder.parse(new InputSource(new StringReader(userinfo)));
			Element root = (Element) document.getElementsByTagName("email").item(0);
			email = root.getTextContent();
		} catch (ParserConfigurationException e2) {
			e2.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return email;
		
	}
	
	
}