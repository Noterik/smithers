/* 
* CommandHandler.java
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
package com.noterik.bart.fs.fscommand;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.GlobalConfig;
import com.noterik.springfield.tools.XMLHelper;

public class CommandHandler {

	private static CommandHandler instance;
	private Map<String, Command> commands = new HashMap<String, Command>();
	private static final String CONFIG_FILE = "commands.xml";
	private static Logger logger = Logger.getLogger(CommandHandler.class);
	private CommandClassLoader commandClassLoader;


	private CommandHandler(){
		initCommandList();
	}

	public static CommandHandler instance(){
		if (instance == null){
			instance = new CommandHandler();
		}
		return instance;
	}

	private void initCommandList() {
		File file = new File(GlobalConfig.instance().getBaseDir() + "conf/" + CONFIG_FILE);
		logger.info("Initializing command list: " + file.getAbsolutePath());
		Document doc = XMLHelper.getXmlFromFile(file);
		if (doc != null){
			List<Node> nl = doc.selectNodes("//command");
			for(Node n : nl){
				if(n instanceof Element){
					if(n.getName() != null && n.getName().equals("command")){
						Node idn = n.selectSingleNode("./id");
						Node cln = n.selectSingleNode("./class");
						Node jn  = n.selectSingleNode("./jar");
						if(idn != null && cln != null){
							
							String id = idn.getText();
							String cl = cln.getText();
							String jar = null;
							if(jn != null && cl != null) {
								jar = jn.getText();
							}
							if(id != null && cl != null){
								try {
									if(jar != null) {
										logger.info("Loading jar "+jar+" for class "+cl);
										Class<?> commandClass = loadJar(jar, cl);
										Command o = (Command)commandClass.newInstance();
										commands.put(id,o);
									} else {
										Class c = Class.forName(cl);
										Object o = c.newInstance();
										if(o instanceof Command){
											commands.put(id, (Command)o);
										}
									}
								} catch (ClassNotFoundException e) {
									//logger.error("",e);
									System.out.println("command : "+cl+" not in classpath");
								} catch (InstantiationException e) {
									logger.error("",e);
								} catch (IllegalAccessException e) {
									logger.error("",e);
								}
							}
						}
					}
				}
			}
		}
	}

	public String executeCommand(String id, String uri, String xml){
		Command cmd = commands.get(id);
		if(cmd != null){
			logger.debug("about to run command " + id);
			return cmd.execute(uri, xml);
		} else {
			//last try to load the command
			cmd = loadCommand(id);
			if (cmd != null) {
				logger.debug("about to run command " + id);
				return cmd.execute(uri, xml);
			}
		}
		logger.debug("command not found in the command.xml");
		return null;
	}
	
	/**
	 * Return a specific command
	 * 
	 * @param id
	 * @return
	 */
	public Command getCommand(String id) {
		return commands.get(id);
	}
	
	/**
	 * Load a specific command
	 * @param cid the command to load
	 * @return the command object loaded
	 */
	private Command loadCommand(String cid) {
		File file = new File(GlobalConfig.instance().getBaseDir() + "conf/" + CONFIG_FILE);
		Document doc = XMLHelper.getXmlFromFile(file);
		if (doc != null){
			List<Node> nl = doc.selectNodes("//command");
			for(Node n : nl){
				if(n instanceof Element){
					if(n.getName() != null && n.getName().equals("command")){
						Node idn = n.selectSingleNode("./id");
						Node cln = n.selectSingleNode("./class");
						Node jn = n.selectSingleNode("./jar");
						if(idn != null && cln != null){
							String id = idn.getText();
							String cl = cln.getText();
							String jar = null;
							
							if(id != null && cl != null && id.equals(cid)){
								if(jn != null && cl != null) {
									jar = jn.getText();
								}
								try {
									if(jar != null) {
										logger.info("Loading jar "+jar+" for class "+cl);
										Class<?> commandClass = loadJar(jar, cl);
										Command o = (Command)commandClass.newInstance();
										commands.put(id,o);
									} else {
										Class c = Class.forName(cl);
										Object o = c.newInstance();
										if(o instanceof Command){
											commands.put(id, (Command)o);
										}
									}
								} catch (ClassNotFoundException e) {
									logger.error("",e);
								} catch (InstantiationException e) {
									logger.error("",e);
								} catch (IllegalAccessException e) {
									logger.error("",e);
								}
							}
						}
					}
				}
			}
		}
		return commands.get(cid);
	}
	
	/**
	 * Load class from external jar
	 * 
	 * @param jarName - the name of the jar to load from
	 * @param className - the class to load
	 * @return the requested class if successful, otherwise null
	 */
	private Class<?> loadJar(String jarName, String className) {
		Class<?> actionClass = null;
		
		if (commandClassLoader == null) {
			commandClassLoader = new CommandClassLoader();
		}
		try {
			commandClassLoader.setJar(jarName);
			actionClass = commandClassLoader.loadClass(className);
		} catch (ClassNotFoundException e) {
			logger.error("Class "+className+" not found "+e.toString());
		}
		return actionClass;
	}

}