/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.plugins.deserializer;

import com.adobe.campaign.tests.bridge.plugins.IBSDeserializerPlugin;
import com.adobe.campaign.tests.bridge.service.MetaUtils;

import java.io.IOException;
import java.util.*;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class MimeExtractionPluginDeserializer implements IBSDeserializerPlugin {

    @Override
    public boolean appliesTo(Object in_object) {
        if (in_object instanceof MimeMessage) {
            return true;
        }

        if (in_object instanceof Message) {
            return true;
        }

        return false;
    }

    @Override
    public Map<String, Object> apply(Object in_object) {
        MimeMessage l_message = (MimeMessage) in_object;
        Map<String, Object> l_returnMap = new HashMap<>();
        try {
            l_returnMap.put("subject", l_message.getSubject());
            l_returnMap.put("from", l_message.getFrom());
            l_returnMap.put("recipients", l_message.getAllRecipients());

            if (l_message.getContent() instanceof MimeMultipart) {
                MimeMultipart l_multipart = (MimeMultipart) l_message.getContent();
                List<Map<String, Object>> l_parts = new ArrayList<>();
                for (int p = 0; p < l_multipart.getCount(); p++) {
                    BodyPart l_bodyPart = l_multipart.getBodyPart(p);
                    Map<String, Object> l_partMap = new HashMap<>();
                    l_partMap.put("contentType", l_bodyPart.getContentType());
                    l_partMap.put("description", l_bodyPart.getDescription());
                    l_partMap.put("content", l_bodyPart.getContent());
                    l_parts.add(l_partMap);
                }
                l_returnMap.put("content", l_parts);
            } else {
                l_returnMap.put("content", l_message.getContent());
            }
            l_returnMap.put("contentType", l_message.getContentType());
            l_returnMap.put("description", l_message.getDescription());
            l_returnMap.put("receivedDate", l_message.getReceivedDate());
            l_returnMap.put("sentDate", MetaUtils.formatObject(l_message.getSentDate()));
            l_returnMap.put("size", l_message.getSize());
            l_returnMap.put("flags", l_message.getFlags());
            l_returnMap.put("messageNumber", l_message.getMessageNumber());
            l_returnMap.put("lineCount", l_message.getLineCount());
        } catch (MessagingException | IOException e) {
            throw new RuntimeException(e);
        }
        return l_returnMap;
    }


}
