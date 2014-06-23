/*
 * Created on Feb 10, 2009
 */
package com.noterik.bart.fs.search;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.GlobalConfig;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.bart.fs.tools.ServiceHelper;
import com.noterik.bart.marge.model.Service;
import com.noterik.springfield.tools.HttpHelper;
import com.noterik.springfield.tools.fs.URIParser;

public class SearchQueue implements Runnable {

	private static Logger logger = Logger.getLogger(SearchQueue.class);
	private static final int MAX_QUEUE_SIZE = 1000;
	// TODO determine the type of object to reflect the search
	private ArrayBlockingQueue<SearchParams> searches = new ArrayBlockingQueue<SearchParams>(MAX_QUEUE_SIZE);

	public SearchQueue() {
		Thread t = new Thread(this);
		t.start();
	}

	public void run() {
		SearchParams s = null;
		while (true) {
			try {
				s = searches.take();
				logger.debug("Got something!");
			} catch (InterruptedException e) {
				logger.error("",e);
			}
			if (GlobalConfig.instance().getSearchEngine().equals(GlobalConfig.SEARCH_LISA)) {
				doLisaSearch(s);
			} else if (GlobalConfig.instance().getSearchEngine().equals(GlobalConfig.SEARCH_MEMORY)) {
				doInMemorySearch(s);
			} else {
				logger.error("No search engine specified in config!!");
			}
		}
	}

	private void doInMemorySearch(SearchParams sp) {
		logger.debug("Searching:\n\t" + sp.getQuery());
	}

	private void doLisaSearch(SearchParams sp) {
		String sUri = "";
		if (sp.getQuery() != null && !sp.getQuery().equals("")) {			
			sUri = sp.getQuery();
			logger.debug("Custom query: " + sp.getQuery());
		} else {
			if (sp.getStart() != null) {
				sUri += "&start=" + sp.getStart();
			}
			if (sp.getLimit() != null) {
				sUri += "&limit=" + sp.getLimit();
			}
			if (sp.getSort() != null) {
				sUri += "&sort=" + sp.getSort();
			}
			if (sp.isChilds()) {
				sUri += "&childs=true";
			}
			sUri = sUri.replaceFirst("&", "?");
		}
		logger.debug("Event URI: " + sp.getEventUri());
		// get service
		String domain = URIParser.getDomainFromUri(sp.getEventUri());
		logger.debug("DOMAIN: " + domain);
		Service service = ServiceHelper.getService(domain, "searchmanager");
		logger.debug("SERVICE: " + service);
		if (service == null) {
			logger.error("SearchAction: Service was null");
			return;
		}
		logger.debug("SERVICE URL: " + service.getUrl());
		logger.debug("SEARCH URI: " + sp.getUri());
		logger.debug("SURI: " + sUri);
		// build final url
		String finalUrl = service.getUrl() + sp.getUri() + sUri;
		logger.debug("FINAL LISA URI: " + finalUrl);
		String response = HttpHelper.sendRequest("GET", finalUrl, null, null, null);
		logger.debug("RESPONSE\n\t " + response);
		String fsxml = parseLisaResponse(response);
		logger.debug("LISA RESPONSE\n\t " + fsxml);
		// TODO write to output
		String uri = sp.getEventUri().replace("/input/", "/output/");
		// first delete the old results from the output
		FSXMLRequestHandler.instance().handleDELETE(uri, null);
		// write the new results to the output 
		FSXMLRequestHandler.instance().saveFsXml(uri, fsxml, "PUT", false);
	}

	private static String parseLisaResponse(String xml) {
		String fsxml = "";

		// start with empty properties
		fsxml += "<properties />";

		try {
			// parse xml (only add result elements)
			Document doc = DocumentHelper.parseText(xml);
			List<Node> nList = doc.selectNodes("//result");
			Node node;
			for (Iterator<Node> iter = nList.iterator(); iter.hasNext();) {
				node = iter.next();
				if (node instanceof Element) {
					fsxml += node.asXML();
				}
			}
		} catch (Exception e) {
			logger.error("SearchAction: error parsing response from lisa",e);
		}

		// add fsxml
		fsxml = "<fsxml>" + fsxml + "</fsxml>";

		return fsxml;
	}

	public void addToQueue(SearchParams search) {
		try {
			logger.debug("Adding to queue: " + search);
			searches.put(search);
		} catch (InterruptedException e) {
			logger.error("",e);
		}
	}	

}