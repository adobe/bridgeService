/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MimeMessageFactory {
    public static MimeMessage getMessage(String in_suffix) throws MessagingException {

        String from = "qa_sender@qamail.rd.campaign.adobe.com";
        MimeMessage message = new MimeMessage((Session) null);
        message.setFrom(new InternetAddress(from));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress("a@b.com"));
        message.setSubject("a subject by me " + in_suffix);
        message.setText("a content by yours truely " + in_suffix);
        return message;
    }

}

