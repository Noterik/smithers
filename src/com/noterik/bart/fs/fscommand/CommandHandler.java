/*
 * Created on Aug 27, 2008
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
						if(idn != null && cln != null){
							String id = idn.getText();
							String cl = cln.getText();
							if(id != null && cl != null){
								try {
									Class c = Class.forName(cl);
									Object o = c.newInstance();
									if(o instanceof Command){
										commands.put(id, (Command)o);
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
						if(idn != null && cln != null){
							String id = idn.getText();
							String cl = cln.getText();
							if(id != null && cl != null && id.equals(cid)){
								try {
									Class c = Class.forName(cl);
									Object o = c.newInstance();
									if(o instanceof Command){
										commands.put(id, (Command)o);
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

}