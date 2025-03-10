package com.aogroup.za.adaptors;

import com.co.ke.main.EntryPoint;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.StartTLSOptions;
import log.Logging;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailAdaptor extends AbstractVerticle {
    EventBus eventBus;
    Logging logger;
    @Override
    public void start(Future<Void> startFuture) {
        //System.out.println("Deposit Verticle Started");
        eventBus = vertx.eventBus();
        logger = new Logging();
        eventBus.consumer("SEND_EMAIL", this::sendRegisterEmailToRider);

    }

    private void sendRegisterEmailToRider(io.vertx.core.eventbus.Message<JsonObject> message) {
        JsonObject data = message.body();
        System.out.println("EMAIL TO SEND \t" + data.toString());

        MultiMap headers = message.headers();
        if (headers.isEmpty()) {
            System.out.println("empty Header");
            message.fail(666, "Unauthenticated User");
            return;
        }

        // MUST PROVIDE recipient_Email,subject,body,
        String emailRecipient = data.getString("emailRecipient");
        String emailSubject = data.getString("emailSubject");
        String emailBody = data.getString("emailBody");

        MailConfig config = new MailConfig();
        config.setHostname("smtp.office365.com");
        config.setPort(587);
        config.setStarttls(StartTLSOptions.REQUIRED);
        config.setAuthMethods("LOGIN");
        config.setUsername(EntryPoint.SMTP_EMAIL);
        config.setPassword(EntryPoint.SMTP_PASSWORD);
        MailClient mailClient = MailClient.createShared(vertx, config);
        MailMessage email = new MailMessage();
        email.setFrom(EntryPoint.SMTP_EMAIL);
        email.setTo(emailRecipient);
        email.setSubject(emailSubject);
        email.setText(emailBody);
        mailClient.sendMail(email, result -> {
            if (result.succeeded()) {
                System.out.println(result.result());
                System.out.println("Mail sent");
            } else {
                System.out.println("got exception");
                result.cause().printStackTrace();
            }
        });

        // Get system properties
//        Properties properties = System.getProperties();
//        properties.setProperty("mail.transport.protocol", "smtp");
//        properties.setProperty("mail.host", EntryPoint.SMTP_HOST);
//        properties.put("mail.smtp.auth", "true");
//        properties.put("mail.smtp.port", EntryPoint.SMTP_PORT);
//        properties.put("mail.debug", "true");
//        properties.put("mail.smtp.socketFactory.port", EntryPoint.SMTP_PORT);
//        properties.put("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory");
//        properties.put("mail.smtp.socketFactory.fallback", "false");
//
//        // Get the default Session object.
//        Session session = Session.getInstance(properties, new Authenticator() {
//            @Override
//            protected PasswordAuthentication getPasswordAuthentication() {
//                return new PasswordAuthentication(EntryPoint.SMTP_EMAIL,EntryPoint.SMTP_PASSWORD);
//            }
//        });
//
//        try {
//            MimeMessage mimeMessage = new MimeMessage(session);
//            mimeMessage.setFrom(new InternetAddress(EntryPoint.SMTP_EMAIL));
//            mimeMessage.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(emailRecipient));
//            mimeMessage.setSubject(emailSubject);
//            mimeMessage.setContent(emailBody, "text/html; charset=utf-8");
//
//            Transport.send(mimeMessage);
////            System.out.println("Sent message successfully....");
//        } catch (Exception mex) {
//            mex.printStackTrace();
//            logger.applicationLog(logger.logPreString()+"\t"+mex.getLocalizedMessage()+"\t"+emailRecipient+"\t"+emailSubject,"",12);
//        }
        message.reply(data);
    }

//    public void sendEmail(){
//        // Recipient's email ID needs to be mentioned.
//        String recipient = "munawarali545@gmail.com";
//
//        // Sender's email ID needs to be mentioned
//        String from = "aotest21@gmail.com";
//
//        // Assuming you are sending email from localhost
//        String host = "smtp.gmail.com";
//
//        // Get system properties
//        Properties properties = System.getProperties();
//
//        // Setup mail server
////        properties.setProperty("mail.smtp.host", host);
//
//        properties.setProperty("mail.transport.protocol", "smtp");
//        properties.setProperty("mail.host", host);
//        properties.put("mail.smtp.auth", "true");
//        properties.put("mail.smtp.port", "465");
//        properties.put("mail.debug", "true");
//        properties.put("mail.smtp.socketFactory.port", "465");
//        properties.put("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory");
//        properties.put("mail.smtp.socketFactory.fallback", "false");
//
//        // Get the default Session object.
//        Session session = Session.getInstance(properties, new Authenticator() {
//            @Override
//            protected PasswordAuthentication getPasswordAuthentication() {
//                return new passwordauthentication("aotest21@gmail.com","AO2021*Tech");
//            }
//        });
//
//        try {
//            // Create a default MimeMessage object.
//            MimeMessage message = new MimeMessage(session);
//
//            // Set From: header field of the header.
//            message.setFrom(new InternetAddress(from));
//
//            // Set To: header field of the header.
//            message.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(recipient));
//
//            // Set Subject: header field
//            message.setSubject("AO Logistic Registration!");
//
//            // Now set the actual message
//            message.setText("This is a test email");
//
//            // Send message
//            Transport.send(message);
////            //System.out.println("Sent message successfully....");
//        } catch (MessagingException mex) {
////            mex.printStackTrace();
//        }
//
//    }

}