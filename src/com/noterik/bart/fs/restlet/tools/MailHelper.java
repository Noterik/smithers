/* 
* MailHelper.java
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
