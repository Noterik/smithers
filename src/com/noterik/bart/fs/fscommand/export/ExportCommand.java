/* 
* ExportCommand.java
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
package com.noterik.bart.fs.fscommand.export;

import java.util.Properties;

import com.noterik.bart.fs.fscommand.CommandAdapter;
import com.noterik.bart.fs.fscommand.ManualEntry;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.PropertiesHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

public class ExportCommand extends CommandAdapter {
	/** export configuration uri */
	private static final String EXPORTCONFIG_URI_TEMPLATE = "/domain/{domain}/config/export/profile/{profile}";
	
	public String execute(String uri, String xml) {
		// get input parameters
		String domain = URIParser.getDomainFromUri(uri);
		Properties params = getInputParameters(xml);
		String profile = PropertiesHelper.getString(params, "profile", null);
		
		// parameter check
		if(domain==null || profile==null) {
			return FSXMLBuilder.getErrorMessage("400", "Bad Request", "Parameters missing", "");
		}
		
		// create export node
		String configURI = EXPORTCONFIG_URI_TEMPLATE.replace("{domain}", domain).replace("{profile}", profile);
		String exportURI = uri.lastIndexOf("/") == uri.length()-1 ? uri + "export" : uri + "/export";
		String exportXML = "<fsxml><properties><config>"+configURI+"</config></properties></fsxml>";
		FSXMLRequestHandler.instance().handlePOST(exportURI, exportXML);
				
		return FSXMLBuilder.getStatusMessage("200", "Export Started", "Export was started successfully", "");
	}

	public ManualEntry man() {
		ManualEntry entry = new ManualEntry();
		entry.setDescription("Export an asset");
		entry.setSyntax("export [options ...]");
		entry.addOption("profile", "the profile number to use");
		return entry;
	}

}
