/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.testdata.one;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class MimeMessageMethods {
    public static MimeMessage createMessage(String in_suffix) throws MessagingException {

        String from = "qa_sender@qamail.rd.campaign.adobe.com";
        MimeMessage message = new MimeMessage((Session) null);
        message.setFrom(new InternetAddress(from));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress("a@b.com"));
        message.setSubject("a subject by me " + in_suffix);
        message.setText("a content by yours truely " + in_suffix);
        return message;
    }

    public static List<MimeMessage> fetchMessages(String in_suffix, int nrOfMessages) throws MessagingException {
        List<MimeMessage> lr_messages = new ArrayList<>();
        for (int i=0; i < nrOfMessages; i++ ){
            lr_messages.add(createMessage(in_suffix+"_"+i));
        }
        return lr_messages;
    }

    public static MimeMessage[] fetchMessagesArray(String in_suffix, int nrOfMessages) throws MessagingException {
        MimeMessage[] lr_messages = new MimeMessage[nrOfMessages];
        for (int i=0; i < nrOfMessages; i++ ){
            lr_messages[i]= createMessage(in_suffix+"_"+i);
        }
        return lr_messages;
    }

    public static List<String> fetchMessageSubjects(List<MimeMessage> messages) {

        return messages.stream().map(m -> {
            try {
                return m.getSubject();
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    public static List<String> fetchMessageArraySubjects(MimeMessage[] messages) {

        return Arrays.stream(messages).map(m -> {
            try {
                return m.getSubject();
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    /**
     * creates a multipart mime message
     * @param in_suffix a suffix for the data
     * @return a multi-part mime message
     * @throws MessagingException
     */
    public static MimeMessage createMultiPartMessage(String in_suffix) throws MessagingException {
        String from = "qa_sender@qamail.rd.campaign.adobe.com";
        MimeMessage message = new MimeMessage((Session) null);
        message.setFrom(new InternetAddress(from));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress("a@b.com"));
        message.setSubject("a subject by me " + in_suffix);

        // Create a multipart message
        MimeMultipart multipart = new MimeMultipart();

        // Part one is the text
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("Part 1 : a content by yours truely " + in_suffix);

        // Part two is an attachment
        //MimeBodyPart attachmentPart = new MimeBodyPart();
        //attachmentPart.setText("Part 2 : a content by yours truely " + in_suffix);

        MimeBodyPart attachmentPart = new MimeBodyPart();
        ByteArrayDataSource ds = new ByteArrayDataSource("attachment content".getBytes(), "text/plain");
        attachmentPart.setDataHandler(new DataHandler(ds));
        attachmentPart.setFileName("testaRosa.txt");


        // Add parts to the multipart
        multipart.addBodyPart(textPart);
        multipart.addBodyPart(attachmentPart);

        // Set the multipart as the message's content
        message.setContent(multipart);
        return message;
    }

    public static MimeMessage createMultiPartAlternativeMessage(String in_suffix) throws MessagingException {
        // Step 1: Establish a mail session
        Properties properties = System.getProperties();
        Session session = Session.getDefaultInstance(properties);

        // Step 2: Create a default MimeMessage object
        MimeMessage message = new MimeMessage(session);

        // Step 3: Set From, To, Subject
        message.setFrom(new InternetAddress("from@example.com"));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress("to@example.com"));
        message.setSubject("This is the Subject Line "+in_suffix+"");

        // Step 4: Create a multipart/alternative content
        Multipart multipart = new MimeMultipart("alternative");

        // Step 5: Create two body parts
        BodyPart textBodyPart = new MimeBodyPart();
        textBodyPart.setText("This is the text version of the email." + in_suffix);

        BodyPart htmlBodyPart = new MimeBodyPart();
        htmlBodyPart.setContent("<h1>This is the HTML version of the email."+in_suffix+"</h1>", "text/html");

        // Step 6: Add the body parts to the multipart
        multipart.addBodyPart(textBodyPart);
        multipart.addBodyPart(htmlBodyPart);

        // Step 7: Set the multipart object to the message content
        message.setContent(multipart);
        return message;
    }
}

