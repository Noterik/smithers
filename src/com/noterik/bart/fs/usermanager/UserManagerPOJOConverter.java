package com.noterik.bart.fs.usermanager;

import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;

public class UserManagerPOJOConverter {
	/** The UserManagerPOJOConverter's log4j Logger */
	private static Logger logger = Logger.getLogger(UserManagerPOJOConverter.class);
	
	/**
	 * Converts a user to XML
	 * 
	 * @param user
	 * @return
	 */
	public static String user2xml(User user) {
		StringBuffer buff = new StringBuffer();
		buff.append("<user id='"+user.getId()+"'>");
		buff.append("<properties>");
		buff.append("<firstname>").append(user.getFirstname()).append("</firstname>");
		buff.append("<lastname>").append(user.getLastname()).append("</lastname>");
		buff.append("<email>").append(user.getEmail()).append("</email>");
		buff.append("</properties>");
		buff.append("</user>");
		return buff.toString();
	}
	
	/**
	 * Converts a list of users to XML
	 * 
	 * @param uList
	 * @return
	 */
	public static String userlist2xml(List<User> uList) {
		StringBuffer buff = new StringBuffer();
		buff.append("<userlist id='1'>");
		buff.append("<properties/>");
		for(User user : uList) {
			buff.append(user2xml(user));
		}
		buff.append("</userlist>");
		return buff.toString();
	}
	
	/**
	 * Converts a group to XML
	 * 
	 * @param group
	 * @return
	 */
	public static String group2xml(Group group) {
		StringBuffer buff = new StringBuffer();
		buff.append("<group id='"+group.getId()+"'>");
		buff.append("<properties>");
		buff.append("<name>").append(group.getGroupname()).append("</name>");
		buff.append("</properties>");
		buff.append("</group>");
		return buff.toString();
	}
	
	/**
	 * Converts a list of groups to XML
	 * 
	 * @param gList
	 * @return
	 */
	public static String grouplist2xml(List<Group> gList) {
		StringBuffer buff = new StringBuffer();
		buff.append("<grouplist id='1'>");
		buff.append("<properties/>");
		for(Group group : gList) {
			buff.append(group2xml(group));
		}
		buff.append("</grouplist>");
		return buff.toString();
	}
	
	/**
	 * Converts an xml string to a Group
	 * 
	 * @param xml
	 * @return
	 */
	public static Group umxml2group(String xml) {
		Group group = new Group();
		try {
			Document doc = DocumentHelper.parseText(xml);
			String id = doc.getRootElement().selectSingleNode("attribute::id").getText();
			String groupname = doc.selectSingleNode("//properties/name").getText();
			group.setId(id);
			group.setGroupname(groupname);
		} catch(Exception e) {
			logger.debug("Error while parsing xml",e);
			group = null;
		}
		return group;
	}

	/**
	 * Convert an xml string to a User 
	 * 
	 * @param asXML
	 * @return
	 */
	public static User umxml2user(String xml) {
		User user = new User();
		try {
			Document doc = DocumentHelper.parseText(xml);
			String id = doc.getRootElement().selectSingleNode("attribute::id").getText();
			String firstname = doc.selectSingleNode("//properties/firstname").getText();
			String lastname = doc.selectSingleNode("//properties/lastname").getText();
			String email = doc.selectSingleNode("//properties/email").getText();
			user.setId(id);
			user.setFirstname(firstname);
			user.setLastname(lastname);
			user.setEmail(email);
		} catch(Exception e) {
			logger.debug("Error while parsing xml",e);
			user = null;
		}
		return user;
	}
}
