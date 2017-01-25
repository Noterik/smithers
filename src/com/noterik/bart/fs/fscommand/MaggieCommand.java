/* 
* MaggieCommand.java
* 
* Copyright (c) 2016 Noterik B.V.
* 
* This file is part of smithers, related to the Noterik Springfield project.
*
* smithers is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* smithers is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with smithers.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.noterik.bart.fs.fscommand;

import java.util.List;

import org.apache.log4j.Logger;
import org.springfield.fs.FSList;
import org.springfield.fs.FSListManager;
import org.springfield.fs.FsNode;

/**
 * MaggieCommand.java
 *
 * @author Pieter van Leeuwen
 * @copyright Copyright: Noterik B.V. 2016
 * @package com.noterik.bart.fs.fscommand
 * 
 */
public class MaggieCommand implements Command {
	
	private static Logger logger = Logger.getLogger(MaggieCommand.class);

	public String execute(String uri, String xml) {
		logger.debug("get Maggie command");
		
		// allways 'loads' the full result set with all the items from the manager
		String queryUri = "/domain/euscreenxl/user/*/*"; // does this make sense, new way of mapping (daniel)
		 FSList allNodes = FSListManager.get(queryUri);
		 
		 List<FsNode> nodes = null;
		 nodes = allNodes.getNodes();
		 
		 System.out.println("Total numbers of nodes found "+nodes.size());
		
		return "";
	}
	
	public ManualEntry man() {
		return null;
	}
}
