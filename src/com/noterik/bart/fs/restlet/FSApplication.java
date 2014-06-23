package com.noterik.bart.fs.restlet;

import org.restlet.Application;
import org.restlet.Restlet;

import com.noterik.bart.fs.restlet.FSRestlet;

public class FSApplication extends Application {

	@Override
	public Restlet createInboundRoot() {
		return new FSRestlet(super.getContext());
	}

}