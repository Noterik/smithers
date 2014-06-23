package com.noterik.bart.fs;

public class SmithersProperties {
	private String ipnumber;
	private boolean alive = true;

	public void setIpNumber(String i) {
		ipnumber = i;
	}
	
	public String getIpNumber() {
		return ipnumber;
	}
	
	public void setAlive(boolean a) {
		alive = a;
	}
	
	public boolean isAlive() {
		return alive;
	}
	
}
