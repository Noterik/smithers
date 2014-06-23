package com.noterik.bart.fs.fscommand.usermanagement;

import java.util.Properties;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.dom4j.Document;

import com.noterik.bart.fs.dao.DAOException;
import com.noterik.bart.fs.fscommand.CommandException;
import com.noterik.bart.fs.fscommand.ManualEntry;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.bart.fs.tools.MailHelper;
import com.noterik.bart.fs.usermanager.User;
import com.noterik.bart.fs.usermanager.UserKey;
import com.noterik.springfield.tools.PropertiesHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * Sends
 * 
 * SYNTAX
 * 
 * forgotpw [options ...] -uid LOGIN
 * 
 * OPTIONS
 * 
 * -u, --uid LOGIN
 * 		The user's id, or LOGIN
 * 
 * -lang LANGUAGE
 * 		The language of the email
 * 
 * 
 * This command is used in combination with the {@link ResetPasswordCommand}
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2011
 * @package com.noterik.bart.fs.fscommand.usermanagement
 * @access private
 *
 */
public class ForgotPasswordCommand extends UserCommandAdapter {
	/** the ForgotPasswordCommand's log4j Logger */
	private static Logger logger = Logger.getLogger(ForgotPasswordCommand.class);
	
	/** mail template uri */
	private static final String MAILTEMPLATE_URI_TEMPLATE = "/domain/{domain}/config/user/setting/forgotpw-mail/template/{language}";
	
	/** default language */
	private static final String DEFAULT_LANGUAGE = "en";
	
	public String execute(String uri, String xml) {
		// get input parameters
		String domain = URIParser.getDomainFromUri(uri);
		Properties params = getInputParameters(xml);
		String uid = PropertiesHelper.getString(params, "u", null);
		uid = (uid==null) ? PropertiesHelper.getString(params, "uid", null) : uid;
		String lang = PropertiesHelper.getString(params, "lang", null);
		lang = (lang==null) ? DEFAULT_LANGUAGE : lang;
		
		// parameter check
		if(domain==null || uid==null) {
			return FSXMLBuilder.getErrorMessage("400", "Bad Request", "Parameters missing", "");
		}
		
		// check if user exists
		User user = null; 
		try {
			user = udao.read(new UserKey(domain, uid));
			if(user == null) {
				return FSXMLBuilder.getErrorMessage("404", "Not Found", "Could not find user", "");
			}
		} catch (DAOException e) {
			return FSXMLBuilder.getErrorMessage("500", "Internal System Error", e.getMessage(), "");
		}
		
		// create email
		try {
			sendForgotPWMail(user,lang);
		} catch (CommandException e) {
			logger.error("Could not send actication mail",e);
			return FSXMLBuilder.getErrorMessage("500", "Internal Server Error", e.getMessage(), "");
		}
		return FSXMLBuilder.getStatusMessage("200","OK", "Forgot password mail has been send", "");
	}
	
	/**
	 * Sends an email with a confirmation request for resetting a user's password
	 * 
	 * Stores a UUID into the filesystem within the current user information.
	 * It sends an email with this information included. 
	 * This UUID should be used to reset a user's password.
	 * 
	 * @param user
	 * @param lang
	 * @throws CommandException 
	 */
	private void sendForgotPWMail(User user, String language) throws CommandException {
		// get mail template
		String mailTemplateUri = MAILTEMPLATE_URI_TEMPLATE.replace("{domain}", user.getDomain()).replace("{language}", language);
		Document mailTemplateDoc = FSXMLRequestHandler.instance().getNodeProperties(mailTemplateUri, false);
		if(mailTemplateDoc == null) {
			throw new CommandException("No mail template defined in filesystem");
		}
		
		// get template variables
		String from = null, subjectTemplate = null, contentTemplate = null;
		try {
			from = mailTemplateDoc.selectSingleNode("//properties/from").getText();
			subjectTemplate = mailTemplateDoc.selectSingleNode("//properties/subject").getText();
			contentTemplate = mailTemplateDoc.selectSingleNode("//properties/content").getText();
		} catch(Exception e) {
			throw new CommandException("Some variables where not define in the filesystem. (from, subject, content)");
		}
		
		// create random ID and store it in the filesystem
		UUID uuid = UUID.randomUUID();
		String userUri = user.getURI();
		FSXMLRequestHandler.instance().updateProperty(userUri+"/properties/forgotpw-uuid", "forgotpw-uuid", uuid.toString(), "PUT", true);
		
		// get variables
		String to = user.getEmail();
		String subject = subjectTemplate
				.replace("{userid}", user.getId())
				.replace("{firstname}", user.getFirstname())
				.replace("{lastname}", user.getLastname())
				.replace("{email}", user.getEmail())
				.replace("{domain}", user.getDomain())
				.replace("{uuid}", uuid.toString());
		String content = contentTemplate
				.replace("{userid}", user.getId())
				.replace("{firstname}", user.getFirstname())
				.replace("{lastname}", user.getLastname())
				.replace("{email}", user.getEmail())
				.replace("{domain}", user.getDomain())
				.replace("{uuid}", uuid.toString());
		
		// send mail
		try {
			MailHelper.send(from, to, null, subject, content);
		} catch (Exception e) {
			throw new CommandException("Could not send email -- "+e.getMessage());
		} 
	}

	public ManualEntry man() {
		ManualEntry entry = new ManualEntry();
		entry.setDescription("Sends the user a confirmation mail for resetting his/her password");
		entry.setSyntax("forgotpw [options ...] -uid LOGIN");
		entry.addOption("u","uid","The user's id, or LOGIN.");
		entry.addOption("lang","The language of the mail.");
		return entry;
	}
}
