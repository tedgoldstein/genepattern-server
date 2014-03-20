/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

/**
 *
 */
package org.genepattern.server.webapp.jsf;

import java.util.Date;
import java.util.Properties;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;

/**
 * This class is a JSF backing bean for the Contact Page, linked to from the 'Contact Us' link.
 * By default it will send an email to 'gp-help@broadinstitute.org' using the 'smtp.broadinstitute.org'
 * mail server.
 * These default values can be set via standard GP server configuration settings, e.g.
 * <pre>
 * contact.us.email=gp-help@broadinstitute.org
 * smtp.server=smtp.broadinstitute.org
 * </pre>
 * 
 * One way to change the default values is by adding or editing a custom property from the 'Server Settings > Custom' page.
 * 
 * @author pcarr
 *
 */
public class ContactUsBean {
    public static final String PROP_CONTACT_EMAIL="contact.us.email";
    public static final String DEFAULT_CONTACT_EMAIL="gp-help@broadinstitute.org";
    public static final String PROP_SMTP_SERVER="smtp.server";
    public static final String DEFAULT_SMTP_SERVER="smtp.broadinstitute.org"; 
    
    private String subject;

    private String replyTo;

    private String message;

    private boolean sent = false;

    private static Logger log = Logger.getLogger(ContactUsBean.class);

    public static final String EMAIL_REGEXP_PATTERN = "^[\\w-\\.]+@([\\w-]+\\.)([\\w-]+\\.[\\w-]+)*[\\w-]{2,4}$";

    public ContactUsBean() {
        User user = new UserDAO().findById(UIBeanHelper.getUserId());
        if (user != null) {
            replyTo = user.getEmail();
        }
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String send() {
        final GpContext gpContext=GpContext.getServerContext();
        final String sendToAddress = ServerConfigurationFactory.instance().getGPProperty(gpContext, PROP_CONTACT_EMAIL, DEFAULT_CONTACT_EMAIL);
        final String smtpServer = ServerConfigurationFactory.instance().getGPProperty(gpContext, PROP_SMTP_SERVER, DEFAULT_SMTP_SERVER);
        return send(sendToAddress, smtpServer);
    }
    
    private String send(final String sendToAddress, final String smtpServer) {
        Properties p = new Properties();
        p.put("mail.host", smtpServer);

        Session mailSession = Session.getDefaultInstance(p, null);
        mailSession.setDebug(false);
        MimeMessage msg = new MimeMessage(mailSession);

        try {
            msg.setSubject(subject);
            msg.setText(message);
            msg.setFrom(new InternetAddress(replyTo));
            msg.setSentDate(new Date());
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(sendToAddress));
            Transport.send(msg);
        } catch (MessagingException e) {
            log.error(e);
            UIBeanHelper.setErrorMessage("An error occurred while sending the email.");
            return "failure";
        }
        sent = true;
        return "success";
    }

    public boolean isSent() {
        return sent;
    }

    public String getInfoMessages() {
        if (!sent)
            return "An error occurred while sending the email.";
        return "Your request has been sent!";
    }

    public void validateEmail(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        String message = "The return address entered is not valid.";
        FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);

        if (value == null) {
            ((UIInput) component).setValid(false);
            throw new ValidatorException(facesMessage);
        }
        String address = (String) value;
        try {
            InternetAddress emailAddr = new InternetAddress(address);
            if (!address.matches(EMAIL_REGEXP_PATTERN)) {
                ((UIInput) component).setValid(false);
                throw new ValidatorException(facesMessage);
            }
        } catch (AddressException ex) {
            ((UIInput) component).setValid(false);
            throw new ValidatorException(facesMessage);
        }
    }
}
