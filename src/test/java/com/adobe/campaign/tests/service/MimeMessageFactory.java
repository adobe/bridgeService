package com.adobe.campaign.tests.service;

import com.adobe.campaign.tests.integro.tools.EmailClientTools;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;

public class MimeMessageFactory {
    public static MimeMessage getMessage(String in_suffix) throws UnsupportedEncodingException, MessagingException {


        return EmailClientTools.createMimeMessage("a@b.com", "a subject by me "+in_suffix,"a content by yours truely "+in_suffix);
    }
}
