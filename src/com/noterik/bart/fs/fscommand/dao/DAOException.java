package com.noterik.bart.fs.dao;

public class DAOException extends Exception {
	
	public DAOException() {
		super("DAO Exception");
	}
	
	public DAOException(String message) {
		super(message);
	}
	
}
