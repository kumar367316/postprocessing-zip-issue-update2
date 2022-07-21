package com.custom.postprocessing.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.internet.MimeMultipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.custom.postprocessing.email.api.dto.MailRequest;
import com.custom.postprocessing.email.api.dto.MailResponse;
import com.custom.postprocessing.email.api.dto.MailResponseDTO;

/**
 * @author kumar.charanswain
 *
 */

@Component
public class EmailUtility {

	public static final Logger logger = LoggerFactory.getLogger(EmailUtility.class);

	@Value("${mail.from}")
	private String mailForm;

	@Value("${mail.to}")
	private String mailTo;

	@Value("${mail.subject}")
	private String subject;

	@Value("${mail.smtp.starttls.key}")
	private String starttlsKey;

	@Value("${mail.smtp.starttls.value}")
	private String starttlsValue;

	@Value("${mail.smtp.host.key}")
	private String hostKey;

	@Value("${mail.smtp.host.value}")
	private String hostValue;

	@Value("${mail.smtp.port.key}")
	private String portKey;

	@Value("${mail.smtp.port.value}")
	private String portValue;

	@Value("${mail.debug.key}")
	private String debugKey;

	@Value("${mail.debug.value}")
	private String debugValue;

	@Value("${mail.smtp.auth.key}")
	private String authKey;

	@Value("${mail.smtp.auth.value}")
	private String authValue;
	
	public MailResponse sendEmail(MailRequest request, Map<String, Object> model, String currentDate) {
		MailResponse response = new MailResponse();
		try {
			Properties props = new Properties();
			props.put(starttlsKey, starttlsValue);
			props.put(hostKey, hostValue);
			props.put(portKey, portValue);
			props.put(debugKey, debugValue);
			props.put(authKey, authValue);
			Session session = Session.getDefaultInstance(props);
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(mailForm));
			message.setRecipient(RecipientType.TO, new InternetAddress(mailTo));
			message.setSubject(subject);
			File documentTxtFile = addAttachment(currentDate, request.getFileNames());
			BodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setFileName(documentTxtFile.getName());
			
			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(messageBodyPart);
			message.setContent(multipart);
			message.setSentDate(new Date());
			response.setFile(documentTxtFile);
			response.setMessage("mail send successfully : " + request.getTo());
			response.setStatus(Boolean.TRUE);
			Transport.send(message);
		} catch (AddressException addressException) {
			logger.info("email address invalid sendEmail() " + addressException);
			response.setMessage("mail sending failure");
			response.setStatus(Boolean.FALSE);
		} catch (MessagingException messagingException) {
			logger.info("message invalid sendEmail() " + messagingException);
			response.setMessage("mail sending failure");
			response.setStatus(Boolean.FALSE);
		} catch (Exception exception) {
			logger.info("exception sendEmail() " + exception);
			response.setMessage("mail sending failure");
			response.setStatus(Boolean.FALSE);
		}
		return response;
	}

	public File addAttachment(String currentDate, List<String> fileNames) {
		File file = null;
		try {
			String documentFileName = "completed-" + currentDate + ".txt";
			file = new File(documentFileName);
			final FileOutputStream outputStream = new FileOutputStream(file);
			PrintWriter writer = new PrintWriter(outputStream);
			writer.println("process file type summary" + '\n');
			for (String fileName : fileNames) {
				writer.println(fileName);
			}
			outputStream.close();
			writer.close();
		} catch (Exception exception) {
			logger.info("Exception addAttachment() " , exception);
		}
		return file;
	}

	public void emailProcess(ConcurrentHashMap<String, List<String>> updatePostProcessMap, String currentDate) {
		String mailStatus = "mail sent successfully";
		try {
			List<String> updateFileNames = new LinkedList<String>();
			List<MailResponseDTO> mailResponseDTOList = new LinkedList<MailResponseDTO>();
			for (String fileType : updatePostProcessMap.keySet()) {
				List<String> fileNames = updatePostProcessMap.get(fileType);
				addFileNameList(fileNames, updateFileNames);

				MailResponseDTO mailResponseDTO = new MailResponseDTO();
				mailResponseDTO.setFileType(fileType);
				mailResponseDTO.setTotalSize(fileNames.size());
				mailResponseDTOList.add(mailResponseDTO);
			}

			MailRequest mailRequest = new MailRequest();
			mailRequest.setFrom(mailForm);
			mailRequest.setTo(mailTo);
			mailRequest.setSubject(subject);
			mailRequest.setFileNames(updateFileNames);

			Map<String, Object> model = new HashMap<>();
			model.put("mailResponseList", mailResponseDTOList);
			MailResponse mailResponse = sendEmail(mailRequest, model, currentDate);
			mailStatus = mailResponse.getMessage();
			mailResponse.getFile().delete();
		} catch (Exception exception) {
			mailStatus = exception.getMessage();
			logger.info("exception emailProcess()" , exception);
		}
		logger.info("mailStatus emailProcess()" , mailStatus);
	}

	public void addFileNameList(List<String> fileNames, List<String> updateFileNames) {
		for (String fileName : fileNames) {
			updateFileNames.add(fileName);
		}
	}

}
