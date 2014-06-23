/*
 * Created on Jul 17, 2008
 */
package com.noterik.bart.fs.type;

public enum ResourceNodeType {

	DEFAULT ("default"),
	PROPERTIES ("properties"),
	DOMAIN ("domain"),
	USER ("user"),
	VIDEO ("video"),
	RAWVIDEO ("rawvideo"),
	IMAGE ("image"),
	RAWIMAGE ("rawimage"),
	PRESENTATION ("presentation");

	private String name;

	private ResourceNodeType(String name){
		this.name = name;
	}

	public String getName(){
		return name;
	}

}