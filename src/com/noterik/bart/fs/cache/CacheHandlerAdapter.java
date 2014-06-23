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
