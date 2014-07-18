/* 
* CommandAdapter.java
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
