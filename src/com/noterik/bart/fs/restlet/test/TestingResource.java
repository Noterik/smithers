package com.noterik.bart.fs.restlet.test;

import org.apache.log4j.Logger;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Resource for testing purposes.
 * 
 * To create a test create a new <b>static</b> method annotated with 
 * <code>Test</code>, and return type <code>String</code>.
 * 
 * <p>Example:
 * <pre>
 * <b>&#064;Test</b> 
 * public static String fu() {
 * 		return "bar";
 * }
 * </pre>
 * </p>
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2009
 * @package com.noterik.bart.fs.restlet
 * @access private
 * @version $Id: TestingResource.java,v 1.3 2011-11-21 11:15:59 derk Exp $
 *
 */
public class TestingResource extends ServerResource {
	/** the TestingResource's log4j Logger */
	private static Logger logger = Logger.getLogger(TestingResource.class);
	
	/** the TestingResource's methods */
	private static Method[] methods = TestingResource.class.getMethods();
	
	// allowed actions: GET 
	public boolean allowPut() {return false;}
	public boolean allowPost() {return false;}
	public boolean allowGet() {return true;}
	public boolean allowDelete() {return false;}
	
	/**
	 * GET
	 */
	@Get
    public Representation getRepresentation() {
		String responseBody = "";
		
		// get parameters
		Form qForm = getRequest().getResourceRef().getQueryAsForm();
		String methodName = qForm.getFirstValue("method",null);
		logger.debug("selected method: " + methodName);
		
		// get arguments
		String[] args = arguments();
		
		// check methods
		boolean methodAvailable = false;
		for (Method m : methods) {
			if(m.isAnnotationPresent(Test.class)) {  // check annotation for security
				if(m.getName().equals(methodName)) {
					try {
						methodAvailable = true;
						responseBody = (String)m.invoke(null,(Object[])args);
					} catch(Exception e) {
						logger.error("",e);
						responseBody = "an exception occured during the execution of method: " + methodName;
					}
				}
			}
		}
		
		// check 
		if(!methodAvailable) {
			responseBody = "method " + methodName + " is not available";
		}
		
		// return
		Representation entity = new StringRepresentation(responseBody);
        return entity;
	}
	
	/**
	 * Returns the arguments passed as parameters.
	 * 
	 * @return The arguments passed as parameters.
	 */
	private String[] arguments() {
		// get query parameters and remove 'method'
		Form qForm = getRequest().getResourceRef().getQueryAsForm();
		qForm.removeAll("method");
		
		// create sorted set
		SortedSet<String> sortedParams = new TreeSet<String>();
		
		// add only if starting with 'arg'
		Set<String> params = qForm.getNames();
		for(String p : params) {
			if(p.startsWith("arg")) {
				p = p.substring("arg".length());
				sortedParams.add(p);
			}
		}
		
		// loop through arguments in sorted vasion
		String[] args = new String[sortedParams.size()];
		int i=0;
		for(String p : sortedParams) {
			args[i] = qForm.getFirst("arg"+p).getValue();
			logger.debug("arg"+i+": " + args[i]);
			i++;
		}
		
		// return array
		return args;
	}
	
	/**
	 * Returns pong.
	 * 
	 * @return pong.
	 */
	@Test
	public static String ping() {
		return "pong";
	}
	
	/**
	 * Test for the FSXMLHandler's hasProperties function. 
	 * Returns if the specified uri has properties or not.
	 *  
	 * @param uri	Resource uri
	 * @return 		If the specified uri has properties or not.
	 */
	@Test
	public static String hasProperties(String uri) {
		logger.debug("hasProperties -- uri="+uri);
		boolean has = FSXMLRequestHandler.instance().hasProperties(uri);
		return "hasProperties("+uri+") : "+has;
	}
}
