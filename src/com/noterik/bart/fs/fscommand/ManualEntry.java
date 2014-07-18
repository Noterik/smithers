/* 
* ManualEntry.java
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

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Manual entry class
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2010
 * @package com.noterik.bart.fs.fscommand
 * @access private
 *
 */
public class ManualEntry {
	private String description;
	private String syntax;
	private Set<Option> options;
	
	public ManualEntry() {
		options = new LinkedHashSet<Option>();
	}
	
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * @return the syntax
	 */
	public String getSyntax() {
		return syntax;
	}
	
	/**
	 * @param syntax the syntax to set
	 */
	public void setSyntax(String syntax) {
		this.syntax = syntax;
	}
	
	/**
	 * @return the options
	 */
	public Set<Option> getOptions() {
		return options;
	}
	
	/**
	 * Add option
	 * 
	 * @param id			id
	 * @param description	description
	 */
	public void addOption(String id, String description) {
		addOption(id,null,description);
	}
	
	/**
	 * Add option
	 * 
	 * @param id			id
	 * @param alt			alternative
	 * @param description	description
	 */
	public void addOption(String id, String alt, String description) {
		options.add(new Option(id,alt,description));
	}
	
	public class Option {
		private String id;
		private String alt;
		private String description;
		
		/**
		 * @param id
		 * @param alt
		 * @param description
		 */
		public Option(String id, String alt, String description) {
			this.id = id;
			this.alt = alt;
			this.description = description;
		}

		/**
		 * @return the id
		 */
		public String getId() {
			return id;
		}

		/**
		 * @return the alt
		 */
		public String getAlt() {
			return alt;
		}

		/**
		 * @return the description
		 */
		public String getDescription() {
			return description;
		}
	}
}
