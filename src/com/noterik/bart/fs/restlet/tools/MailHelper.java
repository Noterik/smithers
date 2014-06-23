package com.noterik.bart.fs.tools;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailHelper {
	
	private static String smtpHost = "mail.noterik.com"; 
	private static String smtpPort = "26";
	
	/**
	 * Send email
	 * 
	 * @param from
	 * @param to
	 * @param subject
	 * @param content
	 * @throws AddressException
	 * @throws MessagingException
	 */
	public static void send(String from, String to, String bcc, String subject, String content) 
	throws AddressException, MessagingException {
		
		// set smtp properties
		Properties props = new Properties();
		props.put("mail.smtp.host", smtpHost);
		props.put("mail.smtp.port", smtpPort);
		
		// create new session
		Session session = Session.getDefaultInstance(props, null);
		
		// create message
		Message msg = new MimeMessage(session);
		msg.setFrom(new InternetAddress(from));
		msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
		if (bcc != null) {
			msg.setRecipient(Message.RecipientType.BCC, new InternetAddress(bcc));
		}
		msg.setSubject(subject);
		msg.setText(content);
		
		// send message
		Transport.send(msg);
	}
}
