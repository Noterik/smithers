/* 
* FSInput.java
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
package com.noterik.bart.fs.script;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;

/**
 * Input of fs script
 * 
 * Abstraction level for getting the input tags 
 * of an fs script
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.script
 * @access private
 * @version $Id: FSInput.java,v 1.5 2009-02-11 10:34:34 jaap Exp $
 *
 */
public class FSInput implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 123L;
	/**
	 * pointer to parent script 
	 */
	private FSScript script;
	
	public FSInput(FSScript script) {
		this.script = script;
	}
	
	/**
	 * Get a list of input elements
	 * @return
	 */
	public List<Element> getInputElements() {
		String scriptID = script.getID();
		Document doc = FSXMLRequestHandler.instance().getNodeProperties(scriptID,false); // do not send trigger events 
		List<Node> nodes = doc.selectNodes("//input");
		List<Element> elements = new ArrayList<Element>();
		for(Iterator<Node> iter = nodes.iterator(); iter.hasNext(); ) {
			Node node = iter.next();
			if(node instanceof Element) {
				elements.add((Element)node);
			}
		}
		return elements;
	}
}
