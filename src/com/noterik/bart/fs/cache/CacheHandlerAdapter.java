/* 
* CacheHandlerAdapter.java
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
package com.noterik.bart.fs.cache;

public abstract class CacheHandlerAdapter implements CacheHandler {
	protected String name;
	protected boolean enabled;
	public CacheHandlerAdapter(String name) {
		this(name,true); 
	}
	public CacheHandlerAdapter(String name, boolean enabled) {
		this.name = name;
		this.enabled = enabled;
		this.enabled = false; // daniel turned cache off
	}
	public String getName() {
		return name;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		this.enabled = false;  // daniel turned cache off
		// remove all items (always)
		deleteAll();
	}
	public boolean getEnabled() {
		return enabled;
	}
}
