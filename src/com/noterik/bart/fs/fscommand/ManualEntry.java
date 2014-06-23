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
