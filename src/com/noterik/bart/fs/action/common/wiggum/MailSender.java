package com.noterik.bart.fs.action.common.wiggum;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class MailSender {
	
	private String from;
	private String to;
	private String subject;
	private String text;
	

	
	public MailSender(String to, String subject, String text){
		this.from="dimitrios@noterik.nl";
		this.to=to;
		this.subject=subject;
		this.text=text;
	}
	
	public void send(){
		//sends a mail
		
		//mail server properties
		Properties props = new Properties();
		props.put("mail.smtp.host", "mail.noterik.com");
		props.put("mail.smtp.port", "26");
		
		Session mailSession = Session.getDefaultInstance(props);
		Message simpleMessage = new MimeMessage(mailSession);
		
		InternetAddress fromAddress = null;
		InternetAddress toAddress = null;
		
		try {
			fromAddress = new InternetAddress(from);
			toAddress = new InternetAddress(to);
		} catch (AddressException e) {
			
			e.printStackTrace();
		}
		
		try {
			simpleMessage.setFrom(fromAddress);
			simpleMessage.setRecipient(RecipientType.TO, toAddress);
			simpleMessage.setSubject(subject);
			simpleMessage.setText(text);
			
			Transport.send(simpleMessage);			
		} catch (MessagingException e) {
			
			e.printStackTrace();
		}		
		
	}
	
	public static boolean okToSend(String url) throws ParserConfigurationException, SAXException, IOException{
		String response = makeGetReq(url);
		String dateTime = getLastNodeTextContentWithTag(response, "date");
		
		if(dateTime.equals("empty")) return true;
		
		String[] dateTimeTemp = dateTime.split("-");
		
		int day=Integer.parseInt(dateTimeTemp[1]), month=Integer.parseInt(dateTimeTemp[2]), year=Integer.parseInt(dateTimeTemp[0]);
		
		day = Integer.parseInt(dateTimeTemp[1]);
		System.out.println("day: "+day);
		month = Integer.parseInt(dateTimeTemp[2]);
		System.out.println("month: "+month);
		year = Integer.parseInt(dateTimeTemp[0]);
		System.out.println("year: "+year);
		
		String time[] = dateTimeTemp[3].split(":");
		int hour = Integer.parseInt(time[0]);
		System.out.println("hour: "+hour);
		//System.out.println("day:"+day+" month:"+month+" year:"+year+" hour:"+hour);
		
		Calendar cal = Calendar.getInstance();
		int curMonth = cal.get(Calendar.MONTH)+1;
		int curDay = cal.get(Calendar.DAY_OF_MONTH);
		int curYear = cal.get(Calendar.YEAR);
		int curHour = cal.get(Calendar.HOUR_OF_DAY);
		
		Boolean b1 = ((day==curDay) && curHour-hour>7);
		Boolean b2 =((curDay-day==1) && curHour-hour<-17);
		Boolean b3 =((curDay-day>1) && month==curMonth);
		Boolean b4 =(curMonth!=month && year==curYear);
		
		System.out.println(b1.toString()+b2.toString()+b3.toString()+b4.toString());
		
		boolean okToSend = ((day==curDay) && curHour-hour>7) || ((curDay-day==1) && curHour-hour<-17)  || ((curDay-day>1) && month==curMonth) || (curMonth!=month && year==curYear) || (curYear!=year);
		
		return okToSend;
	}
	
	

public static String makeGetReq(String req){
	
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


public static String getLastNodeTextContentWithTag(String xmlString, String tag) throws ParserConfigurationException, SAXException, IOException{
	
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    InputSource is = new InputSource(new StringReader(xmlString));
	Document doc = builder.parse(is);
	
	if(doc.getElementsByTagName(tag).getLength()>1){
		Element el = (Element) doc.getElementsByTagName(tag).item(doc.getElementsByTagName(tag).getLength()-2);
	System.out.println(el.getTextContent());
	return el.getTextContent();
	}
	
	return "empty";
}
	

}