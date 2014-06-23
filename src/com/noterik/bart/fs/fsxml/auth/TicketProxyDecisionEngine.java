package com.noterik.bart.fs.fsxml.auth;

import org.apache.log4j.Logger;
import org.restlet.Request;
import org.restlet.data.Method;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.HttpHelper;


public class TicketProxyDecisionEngine extends DecisionEngine {
	/** TicketProxyDecisionEngine's log4j Logger */
	private static Logger logger = Logger.getLogger(TicketProxyDecisionEngine.class);
	
	//name of the cookie that contains the OU ticket
	private static final String TICKET_COOKIE="opus_user_id";
	//name of the cookie that indicates request is a bart request
	private static final String BART_COOKIE="bart_request";
	//name of the property in the resources that describes auth info
	private static final String AUTH_PROPERTY="authstring";
	//auth server dns name
	private static final String OU_AUTH_SERVER="telem-auth-test.openu.ac.il";
	//private static final String OU_AUTH_SERVER="http://telem-auth.openu.ac.il:17001/auth";
	public boolean decide(Request request) {
		String ticket = request.getCookies().getFirstValue(TICKET_COOKIE);
		String bartRequest = request.getCookies().getFirstValue(BART_COOKIE);
		logger.debug("[TicketProxy] deciding on request, bart='" +bartRequest+ "' ticket='" + ticket +"'");
		//If request is not from bart, everything goes!
		if (bartRequest==null || bartRequest.equals("")) return true;
		if (ticket==null) ticket="";
		String propUri = getResourceUri(request) + "/properties/" + AUTH_PROPERTY;
		String authString = FSXMLRequestHandler.instance().getPropertyValue(propUri);
		logger.debug("[TicketProxy] authString: " + authString);
		// no auth info on this resource? everything goes!
		if (authString==null) return true;
		String uri="http://" + OU_AUTH_SERVER + "/getPermissionLevel?" + authString + "&ut=" + ticket;
		logger.debug("[TicketProxy] Uri to get permission: " + uri);
		String result=null;
		try{
			result = HttpHelper.sendRequest("GET", uri, null, null);
		}catch(Exception e){ result = null;}
		logger.debug("[TicketProxy] Authorization server returned: " + result);
		//something wrong with auth server... our portal will not be accessible
		if (result==null) return false;
		result=result.trim();
		result=result.toLowerCase();
		result=parseResult(result);
		logger.debug("[TicketProxy] Permission level: " + result);
		
		if (result.equals("edit") || 
				result.equals("super")|| 
				result.equals("admin"))
			return true;
		if ((result.equals("public") || result.equals("read")) && 
				request.getMethod()==Method.GET) 
			return true;
		return false;
	}
	
	protected String parseResult(String result){
		if (result.charAt(0)!='{')return result;
		result=cleanResult(result);
		String[] arElements=result.split(",");
		for(String strElement : arElements){
			String[] arPair=strElement.split(":");
			if ("result".equals(arPair[0]))return arPair[1];
		}
		//return an empty string, which will match none of the tests, so the request is denied
		return "";
	}
	
	public static String cleanResult(String input) {
	   String output = "";
	   for (int i = 0; i < input.length(); i ++) {
	      if ((input.charAt(i) != '{')&&(input.charAt(i) != '}')&&(input.charAt(i) != '"')) output += input.charAt(i);
	      }
	   return output;
	}
	
	protected final String getResourceUri(Request request) {
		String path = request.getResourceRef().getPath();
		String uri2 = path.substring(2);
		String uri = uri2.substring(uri2.indexOf("/"));
		if (uri.lastIndexOf("/") == uri.length() - 1) {
			uri = uri.substring(0, uri.lastIndexOf("/"));
		}
		return uri;
	}
}
