package com.noterik.bart.fs.fscommand;

import java.util.Iterator;
import java.util.Properties;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.springfield.tools.XMLHelper;

public abstract class CommandAdapter implements Command {	
	/**
	 * Returns the input parameters.
	 * 
	 * @param xml	The xml specifying the commands parameters.
	 * @return		The input parameters.
	 */
	public Properties getInputParameters(String xml){
		Properties props = new Properties();
		Document doc = XMLHelper.asDocument(xml);
		if(doc == null){
			return null;
		} else {
			Node n = doc.selectSingleNode("./fsxml/properties");
			if(n instanceof Element) {
				Element properties = (Element)n;
				for(Iterator i = properties.elementIterator(); i.hasNext(); ) {
					Element elem = (Element)i.next();
					props.put(elem.getName(), elem.getText().trim());
				}
			}
		}	
		return props;
	}
}
