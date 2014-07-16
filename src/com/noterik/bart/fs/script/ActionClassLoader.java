package com.noterik.bart.fs.script;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.log4j.Logger;

/**
 * Class loader to load external jars with
 * additional smithers actions
 * 
 * @author Pieter van Leeuwen
 * @copyright Copyright: Noterik B.V. 2014
 * @package com.noterik.bart.fs.script
 *
 */
public class ActionClassLoader extends ClassLoader {
	
	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(ActionClassLoader.class);
	
	private String jarFile;

	/**
	 * Set the jar file
	 * 
	 * @param jarFile - the jar file to use
	 */
	public void setJar(String jarFile) {
		//check if jar is providing full path
		if (new File(jarFile).isFile()) {
			this.jarFile = jarFile;
		} else {
			//assume jar will be in the systems default dir
			//TODO: make this configurable and windows compatible
			this.jarFile = "/springfield/smithers/jars/"+jarFile;
		}
	}
	
	/**
	 * Find class, first tries to find classes in smithers, 
	 * otherwise will try to load from defined jar
	 * 
	 * @param className - the name of the class to load
	 * @return the class requested
	 */
	@Override
	public Class<?> findClass(String className) {
		byte classByte[];  
        Class<?> result = null; 
		
        // First check if we are loading a class from smithers
        try {
        	ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        	result =  classLoader.loadClass(className);
        	return result;
        }  catch (Exception e) { }  
        
        try {  
            result = findSystemClass(className);  
        	return result;
        } catch (Exception e) { }  
        
        // Class not found in smithers, now checking the jar
		try {  
            JarFile jar = new JarFile(jarFile);  
            String filename = className.replace('.','/');
            JarEntry entry = jar.getJarEntry(filename + ".class");  

            if (entry != null) {
            	InputStream is = jar.getInputStream(entry);  
            	ByteArrayOutputStream byteStream = new ByteArrayOutputStream();  
            	int nextValue = is.read();  
            	while (-1 != nextValue) {  
            		byteStream.write(nextValue);  
            		nextValue = is.read();  
            	}  
  
            	classByte = byteStream.toByteArray(); 
       
            	result = defineClass(className, classByte, 0, classByte.length);
            }
            
            return result;  
        } catch (Exception e) {  
        	logger.error("ApplicationClassLoader error, could not load "+className+"from "+jarFile);
            return null;  
        }
	}	
}
