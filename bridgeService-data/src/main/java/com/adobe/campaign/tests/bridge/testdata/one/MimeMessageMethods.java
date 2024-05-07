/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.testdata.one;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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


}

