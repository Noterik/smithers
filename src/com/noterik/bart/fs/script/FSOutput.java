package com.noterik.bart.fs.script;

import java.io.Serializable;

import org.dom4j.Document;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;

public class FSOutput implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1222L;
	/**
	 * pointer to parent script 
	 */
	private FSScript script;
	
	public FSOutput(FSScript script) {
		this.script = script;
	}
	
	/**
	 * Set the output element of the script
	 * @param doc
	 */
	public void setOutput(Document doc) {
		setOutput(doc.asXML());
	}
	
	/**
	 * Set the output element of the script
	 * @param xml
	 */
	public void setOutput(String xml) {
		setOutput("1",xml);
	}
	
	/**
	 * Set the output element of the script
	 * @param id
	 * @param xml
	 */
	public void setOutput(String id, String xml) {
		String uri = script.getID() + "/output/"+id;
		FSXMLRequestHandler.instance().saveFsXml(uri, xml, "PUT", false);  // do not send trigger events
	}
}