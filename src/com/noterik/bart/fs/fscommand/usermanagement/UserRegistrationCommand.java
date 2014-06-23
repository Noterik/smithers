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
import com.noterik.bart.fs.usermanager.UserExistsException;
import com.noterik.bart.fs.usermanager.UserKey;
import com.noterik.springfield.tools.PropertiesHelper;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * Register a user
 * 
 * SYNTAX
 * 
 * userreg [options ... ] -uid LOGIN
 * 
 * OPTIONS
 * 
 * -p, --password PASSWORD
 * 		The users password.
 * 
 * -u, --uid LOGIN
 * 		The user's id, or LOGIN
 * 
 * -firstname FIRSTNAME
 * 		The user's first name
 * 
 * -lastname LASTNAME
 * 		The user's last name
 * 
 * -email EMAIL
 * 		The user's email address
 * 
 * -lang LANGUAGE
 * 		The language of the activation mail
 * 
 * 
 * This command is used in combination with the {@link UserActivationCommand}
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2011
 * @package com.noterik.bart.fs.fscommand.usermanagement
 * @access private
 *
 */
public class UserRegistrationCommand extends UserCommandAdapter {
	/** the RegisterUserCommand's log4j Logger */
	private static Logger logger = Logger.getLogger(UserRegistrationCommand.class);
	
	/** mail template uri */
	private static final String MAILTEMPLATE_URI_TEMPLATE = "/domain/{domain}/config/user/setting/activation-mail/template/{language}";

	/** default language */
	private static final String DEFAULT_LANGUAGE = "en";
	
	public String execute(String uri, String xml) {
		// get input parameters
		String domain = URIParser.getDomainFromUri(uri);
		Properties params = getInputParameters(xml);
		String uid = PropertiesHelper.getString(params, "u", null);
		uid = (uid==null) ? PropertiesHelper.getString(params, "uid", null) : uid;
		String firstname = PropertiesHelper.getString(params, "firstname", null);
		String lastname = PropertiesHelper.getString(params, "lastname", null);
		String email = PropertiesHelper.getString(params, "email", null);
		String password = PropertiesHelper.getString(params, "p", null);
		password = (password==null) ? PropertiesHelper.getString(params, "password", null) : password;
		String lang = PropertiesHelper.getString(params, "lang", null);
		lang = (lang==null) ? DEFAULT_LANGUAGE : lang;
		
		// parameter check
		if(domain==null || uid==null || firstname==null || lastname==null || email==null || password==null) {
			return FSXMLBuilder.getErrorMessage("400", "Bad Request", "Parameters missing", "");
		}
		
		// create a user
		User user = new User();
		user.setDomain(domain);
		user.setId(uid);
		user.setFirstname(firstname);
		user.setLastname(lastname);
		user.setEmail(email);
		user.setPassword(password);
		try {
			boolean created = udao.create(user);
			if(!created) {
				return FSXMLBuilder.getErrorMessage("500", "Internal Server Error", "Unknown error", "");
			}
		} catch (DAOException e) {
			if(e instanceof UserExistsException) {
				return FSXMLBuilder.getErrorMessage("409", "Conflict", e.getMessage(), "");
			}
			return FSXMLBuilder.getErrorMessage("500", "Internal Server Error", e.getMessage(), "");
		}
		
		// create activation email
		try {
			createActivationMail(user,lang);
		} catch (CommandException e) {
			logger.error("Could not send actication mail",e);
			// rollback user creation
			try {
				udao.delete(new UserKey(domain,uid));
			} catch (DAOException daoe) {
				logger.error("Could not rollback user creation",daoe);
			}
			return FSXMLBuilder.getErrorMessage("500", "Internal Server Error", e.getMessage(), "");
		}
		return FSXMLBuilder.getStatusMessage("200","OK", "User was registered successfully", "");
	}
	
	/**
	 * Creates an activation email
	 * 
	 * Stores a UUID into the filesystem within the current user information.
	 * It sends an activation email with this information included. 
	 * This UUID should be used to activate the user.
	 * 
	 * @param user
	 * @param language
	 * @throws CommandException 
	 */
	private void createActivationMail(User user, String language) throws CommandException {
		// get mail template
		String mailTemplateUri = MAILTEMPLATE_URI_TEMPLATE.replace("{domain}", user.getDomain()).replace("{language}", language);
		Document mailTemplateDoc = FSXMLRequestHandler.instance().getNodeProperties(mailTemplateUri, false);
		if(mailTemplateDoc == null) {
			throw new CommandException("No mail template defined in filesystem");
		}
		
		// get template variables
		String from = null, subjectTemplate = null, contentTemplate = null, bcc = null;
		try {
			from = mailTemplateDoc.selectSingleNode("//properties/from").getText();
			bcc = mailTemplateDoc.selectSingleNode("//properties/bcc") == null ? null : mailTemplateDoc.selectSingleNode("//properties/bcc").getText();
			subjectTemplate = mailTemplateDoc.selectSingleNode("//properties/subject").getText();
			contentTemplate = mailTemplateDoc.selectSingleNode("//properties/content").getText();
		} catch(Exception e) {
			throw new CommandException("Some variables where not define in the filesystem. (from, subject, content)");
		}
		
		// create random ID and store it in the filesystem
		UUID uuid = UUID.randomUUID();
		String userUri = user.getURI();
		FSXMLRequestHandler.instance().updateProperty(userUri+"/properties/activation-uuid", "activation-uuid", uuid.toString(), "PUT", true);
		
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
			MailHelper.send(from, to, bcc, subject, content);
		} catch (Exception e) {
			throw new CommandException("Could not send activation email -- "+e.getMessage());
		} 
	}

	/**
	 * Manual description
	 */
	public ManualEntry man() {
		ManualEntry entry = new ManualEntry();
		entry.setDescription("Register a user");
		entry.setSyntax("userreg [options ... ] -uid LOGIN");
		entry.addOption("p","password","The users password.");
		entry.addOption("u","uid","The user's id, or LOGIN.");
		entry.addOption("firstname","The user's first name.");
		entry.addOption("lastname","The user's last name.");
		entry.addOption("email","The user's email address.");
		entry.addOption("lang","The language of the activation mail.");
		return entry;
	}

}