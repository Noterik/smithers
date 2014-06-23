package com.noterik.bart.fs.action.dance4life;

import org.apache.log4j.Logger;
import org.dom4j.Document;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.bart.fs.tools.MailHelper;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * Sends an email
 *
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2008
 * @package com.noterik.bart.fs.action.dance4life
 * @access private
 * @version $Id: EmailAction.java,v 1.23 2011-07-01 11:38:56 derk Exp $
 *
 */
public class EmailAction extends ActionAdapter {
	/**
	 * logger
	 */
	private static Logger logger = Logger.getLogger(EmailAction.class);
	
	/**
	 * default sender
	 */
	private static final String SENDER = "noreply@dance4life.com";
	
	/**
	 * default subject
	 */
	private static final String SUBJECT = "Dance4life 29 November Live";
	
	/**
	 * default content
	 */
	/*
	private static final String CONTENT_ENGLISH = "Thanks! By posting this video, you too are supporting Dance4Life! \n\n" +
			"Watch on 29th November, when 50,000 young people in 19 countries are united live via satellite to  celebrate their achievements in pushing back the spread of HIV and AIDS.\n\n" +
			"Tune in at 1600 hrs GMT and witness for yourself this worldwide celebration live! Invite your friends and family to do the same! Watch these young people, support these young people and start Dancing, stop AIDS! \n\n" +   
			"Together we can!\n\n"+
			"The Dance4Life Team";
	
	private static final String CONTENT_SPANISH = "Gracias! Al publicar este vídeo, también está apoyando Dance4Life!\n\n" +
			"Mira el 29 de noviembre, cuando 50.000 jóvenes en 19 países están unidos en vivo vía satélite para celebrar sus logros en retroceder el VIH y el SIDA.\n\n" +
			"Vea al 1600 horas GMT y esta testigo tu mismo en esta celebración en todo el mundo en vivo!\n\n" +
			"Invita a tus amigos y familiares a hacer lo mismo!\n" +
			"Ver a estos jóvenes, apoya estos jóvenes y baila, detén el SIDA!\n\n" +
			"Juntos podemos!\n\n" +
			"El equipo de Dance4Life";
	
	private static final String CONTENT_RUSSIAN = "Спасибо! Копируя и рассылая этот ролик, ты тоже поддерживаешь \"Танцуй ради жизни\"! \n\n" +
			"Смотри 29-го ноября, как 50000 молодых людей в девятнадцати странах мира будут соединены спутниковой связью во время празднования своих успехов в борьбе с эпидемией ВИЧ/СПИДа. \n\n" +
			"Смотри нас в 16.00 по Гринвичу и стань свидетелем всемирного празднования жизни! Приглашай своих друзей и родственников! Смотри на этих молодых людей, поддерживай их, начни танцевать, останови СПИД. \n\n" +
			"Вместе мы сможем! \n\n" +
			"Команда \"Танцуй ради жизни\"";
	*/
	
	private static final String CONTENT_ENGLISH = "Thanks! By posting this video, you too are supporting Dance4Life! \n\n"+
			"If you would like to learn more about the youth's actions or other ways you can support Dance4Life, please go to www.dance4life.com. \n\n" +
			"Together we can! \n\n" +
			"The Dance4Life Team";

	private static final String CONTENT_SPANISH = CONTENT_ENGLISH;

	private static final String CONTENT_RUSSIAN = CONTENT_ENGLISH;
	
	/**
	 * default content containing the link to the player 
	 */
	private static final String CONTENT_LINK = "\n\n=================\n\n" +
			"View your player online.\n\n" +
			"English:\n" +
			"http://dance4life.noterik.com/webtv2/lsplayer.php?language=en&userid={userid} \n\n" +
			"Spanish:\n" +
			"http://dance4life.noterik.com/webtv2/lsplayer.php?language=es&userid={userid} \n\n" +
			"Russian:\n" +
			"http://dance4life.noterik.com/webtv2/lsplayer.php?language=ru&userid={userid}";
	
	@Override
	public String run() {	
		try {
			logger.debug("EmailAction: sending email");
			
			// get username and email
			String userid = URIParser.getUserFromUri(event.getUri());
			String username = "participant";
			String language = "en";
			try {
				String uri = "/domain/dance4life/user/"+userid+"/properties";
				Document doc = FSXMLRequestHandler.instance().getNodeProperties(uri, true);
				username = doc.valueOf("//name");
				language = doc.valueOf("//lang");
			} catch (Exception e) {
				logger.error(e);
			}
			
			// change content
			String content = CONTENT_ENGLISH.replace("{username}",username);
			if(language.toLowerCase().equals("es")) {
				content = CONTENT_SPANISH.replace("{username}",username);
			} 
			else if(language.toLowerCase().equals("ru")) {
				content = CONTENT_RUSSIAN.replace("{username}",username);
			}
			
			// add links
			content += CONTENT_LINK.replace("{userid}", userid);
				
			// username is email
			MailHelper.send(SENDER, userid, null, SUBJECT, content);
			
			logger.debug("EmailAction: email send");
		} catch (Exception e) {
			logger.debug("EmailAction: could not send email: " + e.getMessage());
			logger.error(e);
		}
		
		return null;
	}
}
