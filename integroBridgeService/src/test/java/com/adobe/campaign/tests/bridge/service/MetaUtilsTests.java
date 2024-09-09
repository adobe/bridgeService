/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import com.adobe.campaign.tests.bridge.testdata.nested.NestedExampleA_Level1;
import com.adobe.campaign.tests.bridge.testdata.nested.NestedExampleA_Level2;
import com.adobe.campaign.tests.bridge.testdata.nested.NestedExampleA_Level3;
import com.adobe.campaign.tests.bridge.testdata.nested.NestedExampleA_Level4;
import com.adobe.campaign.tests.bridge.testdata.one.ClassWithMethodThrowingException;
import com.adobe.campaign.tests.bridge.testdata.one.MimeMessageMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.hamcrest.Matchers;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;

public class MetaUtilsTests {
    @BeforeMethod
    @AfterClass
    public void setUp() {
        ConfigValueHandlerIBS.resetAllValues();
    }

    //Tests for extracting data from an object
    @Test
    public void testExtractable() throws MessagingException, NoSuchMethodException {

        Class<?> l_myClass = MimeMessage.class;

        assertThat("MimeMessage is serializable", Message.class instanceof Serializable);
        assertThat("MimeMessage is serializable", MimeMessage.class instanceof Serializable);


        assertThat("MimeMessage is not serializable", !MetaUtils.isExtractable(l_myClass));

        Class<?> l_String = String.class;

        assertThat("String is serializable", MetaUtils.isExtractable(l_String));

        Class<?> l_int = int.class;

        assertThat("int is extractable", MetaUtils.isExtractable(l_int));

        Class<?> l_list = ArrayList.class;

        assertThat("list is extractable", MetaUtils.isExtractable(l_list));

        MimeMessage l_message = MimeMessageMethods.createMessage("one");
        Object l_failingClass = l_message.getAllHeaders();

        Method l_failingMethod = l_failingClass.getClass().getMethod("hasMoreElements", null);
        assertThat("This method is not extractable", !MetaUtils.isExtractable(l_failingMethod));

    }

    @Test
    public void testExtractableMethod() throws NoSuchMethodException {
        Method l_simpleGetter = MimeMessage.class.getDeclaredMethod("getFileName");

        assertThat("getFileName is extractable", MetaUtils.isExtractable(l_simpleGetter));

        Method l_simpleSetter = MimeMessage.class.getDeclaredMethod("setFrom");

        assertThat("setSender is should not be extractable", !MetaUtils.isExtractable(l_simpleSetter));

        Method l_simpleIs = String.class.getDeclaredMethod("isEmpty");

        assertThat("isEmpty is extractable", MetaUtils.isExtractable(l_simpleIs));

        Method l_getSize = MimeMessage.class.getDeclaredMethod("getSize");
        assertThat("HashCode is NOT extractable", MetaUtils.isExtractable(l_getSize));



    }

    @Test
    public void testExtract() throws NoSuchMethodException {

        String l_simpleString = "testValue";

        assertThat("getFileName is extractable", MetaUtils.extractValuesFromObject(l_simpleString),
                Matchers.equalTo(l_simpleString));

        //List<String>
        Object l_simpleList = Collections.singletonList(l_simpleString);

        assertThat("sss", l_simpleList instanceof Collection);

        Method l_simpleSetter = MimeMessage.class.getDeclaredMethod("setFrom");

        assertThat("setSender is should not be extractable", !MetaUtils.isExtractable(l_simpleSetter));

        Method l_simpleIs = String.class.getDeclaredMethod("isEmpty");

        assertThat("isEmpty is extractable", MetaUtils.isExtractable(l_simpleIs));

        Method l_hashSet = MimeMessage.class.getDeclaredMethod("getSize");
        assertThat("HashCode is NOT extractable", MetaUtils.isExtractable(l_hashSet));


    }

    @Test
    public void prepareExtractMimeMessage()
            throws MessagingException, JsonProcessingException {
        Message m1 = MimeMessageMethods.createMessage("five");
        Message m2 = MimeMessageMethods.createMessage("six");
        List<Message> messages = Arrays.asList(m1, m2);
        Object x = messages;

        assertThat("The values should be extractable", MetaUtils.isExtractable(x.getClass()));
        JavaCallResults jcr = new JavaCallResults();
        jcr.getReturnValues().put("value", MetaUtils.extractValuesFromObject(x));

        String value = BridgeServiceFactory.transformJavaCallResultsToJSON(jcr, new HashSet<>());

    }

    public static MimeMessage createMultiPartMessage(String in_suffix) throws MessagingException {
        String from = "qa_sender@qamail.rd.campaign.adobe.com";
        MimeMessage message = new MimeMessage((Session) null);
        message.setFrom(new InternetAddress(from));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress("a@b.com"));
        message.setSubject("a subject by me " + in_suffix);

        // Create a multipart message
        MimeMultipart multipart = new MimeMultipart("example");

        // Part one is the text
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("Part 1 : a content by yours truely " + in_suffix,"utf-8");

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
        message.setSubject("This is the Subject Line!");

        // Step 4: Create a multipart/alternative content
        Multipart multipart = new MimeMultipart("alternative");

        // Step 5: Create two body parts
        BodyPart textBodyPart = new MimeBodyPart();
        textBodyPart.setText("This is the text version of the email.");

        BodyPart htmlBodyPart = new MimeBodyPart();
        htmlBodyPart.setContent("<h1>This is the HTML version of the email.</h1>", "text/html");

        // Step 6: Add the body parts to the multipart
        multipart.addBodyPart(textBodyPart);
        multipart.addBodyPart(htmlBodyPart);

        // Step 7: Set the multipart object to the message content
        message.setContent(multipart);
        return message;
    }

    Message createMPMHTML() throws MessagingException {
        /*
        Properties props = new Properties();

        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.host", "MYMAILSERVER");
        props.setProperty("mail.smtp.auth", "true");
        final PasswordAuthentication auth = new PasswordAuthentication(smtpUser, stmpPassword);
        Session mailSession = Session.getDefaultInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() { return auth; }
        });
        Session session = Session.getInstance(props,null);
        MimeMessage message = new MimeMessage(session);
        */
        /*
        Properties props = new Properties();
        props.put("mail.smtps.host", "smtp.gmail.com");
        props.put("mail.smtps.auth", "true");

        props.put("mail.smtps.port", 123);

        // Get the Session object.
        Session session = Session.getInstance(props, null);

        MimeMessage message = new MimeMessage(session);

         */
        MimeMessage message = new MimeMessage((Session) null);

        InternetAddress from = new InternetAddress("from@me.com");
        InternetAddress to = new InternetAddress("to@you.com");

        message.setSubject("I am a multipart text/html email" );
        message.setFrom(from);
        message.addRecipient(Message.RecipientType.TO, to);

        Multipart multipart = new MimeMultipart();

        // PLAIN TEXT
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText("Here is your plain text message");
        multipart.addBodyPart(messageBodyPart);

        // HTML TEXT
        BodyPart messageBodyPart2 = new MimeBodyPart();
        String htmlText = "<H1>I am the html part</H1>";
        messageBodyPart2.setContent(htmlText, "text/html");
        multipart.addBodyPart(messageBodyPart2);

        message.setContent(multipart);
        return message;
    }

    BodyPart createMimePart() throws MessagingException {

        Multipart multipart = new MimeMultipart();

        // PLAIN TEXT
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText("Here is your plain text message");
        multipart.addBodyPart(messageBodyPart);

        // HTML TEXT
        // HTML TEXT
        BodyPart messageBodyPart2 = new MimeBodyPart();
        String htmlText = "<H1>I am the html part</H1>";
        messageBodyPart2.setContent(htmlText, "text/html");
        multipart.addBodyPart(messageBodyPart2);

        return messageBodyPart2;
    }

    Multipart createMutliPart() throws MessagingException {

        Multipart multipart = new MimeMultipart();

        // PLAIN TEXT
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText("Here is your plain text message");
        multipart.addBodyPart(messageBodyPart);

        // HTML TEXT
        // HTML TEXT
        BodyPart messageBodyPart2 = new MimeBodyPart();
        String htmlText = "<H1>I am the html part</H1>";
        messageBodyPart2.setContent(htmlText, "text/html");
        multipart.addBodyPart(messageBodyPart2);

        return multipart;
    }

    @Test
    public void prepareExtractMultiPartMimeMessage()
            throws MessagingException, JsonProcessingException {
        //List<Message> m1 = Collections.singletonList(createMultiPartMessage("dddd"));

        Object x = createMutliPart();

        //assertThat("The values should be extractable", MetaUtils.isExtractable(x.getClass()));
        JavaCallResults jcr = new JavaCallResults();
        jcr.getReturnValues().put("value", MetaUtils.extractValuesFromObject(x));

        String value = BridgeServiceFactory.transformJavaCallResultsToJSON(jcr, new HashSet<>());
    }

    @Test
    public void prepareExtractMultiPartAlternativeMimeMessage()
            throws MessagingException, JsonProcessingException {
        //List<Message> m1 = Collections.singletonList(createMultiPartMessage("dddd"));

        Object x = MimeMessageMethods.createMultiPartAlternativeMessage("abc");

        //assertThat("The values should be extractable", MetaUtils.isExtractable(x.getClass()));
        JavaCallResults jcr = new JavaCallResults();
        jcr.getReturnValues().put("value", MetaUtils.extractValuesFromObject(x));

        String value = BridgeServiceFactory.transformJavaCallResultsToJSON(jcr, new HashSet<>());
    }

    @Test
    public void prepareExtractSimpleInt()
            throws JsonProcessingException {
        int m1 = 5;
        int m2 = 6;
        List messages = Arrays.asList(m1, m2);
        Object x = messages;

        assertThat("The values should be extractable", MetaUtils.isExtractable(x.getClass()));
        JavaCallResults jcr = new JavaCallResults();
        jcr.getReturnValues().put("value", MetaUtils.extractValuesFromObject(x));

        String value = BridgeServiceFactory.transformJavaCallResultsToJSON(jcr, new HashSet<>());
    }

    @Test
    public void prepareExtractSimpleString()
            throws JsonProcessingException {
        String m1 = "five";
        String m2 = "six";
        List<String> messages = Arrays.asList(m1, m2);
        Object x = messages;

        assertThat("The values should be extractable", MetaUtils.isExtractable(x.getClass()));
        JavaCallResults jcr = new JavaCallResults();
        jcr.getReturnValues().put("value", MetaUtils.extractValuesFromObject(x));

        String value = BridgeServiceFactory.transformJavaCallResultsToJSON(jcr, new HashSet<>());

    }

    @Test
    public void prepareExtractMultiPartAlternativeMimeMessage_lvl1()
            throws MessagingException, IOException {

        Message l_message = MimeMessageMethods.createMultiPartAlternativeMessage("abc");
        Object l_content = l_message.getContent();

        assertThat("The multi-part mime message content is not serializeable",l_message.getContent(), Matchers.not(Matchers.instanceOf(
                Serializable.class)));

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true);

        Map<String, Object> value = (Map<String, Object>) MetaUtils.extractValuesFromObject(l_message);

        for (Map.Entry<String, Object> lt_entry : value.entrySet()) {
            assertThat("The value for "+ lt_entry.getKey()+" should be serializable", lt_entry.getValue(), Matchers.instanceOf(Serializable.class));
        }

        String lr_resultPayload = mapper.writeValueAsString(mapper.writeValueAsString(value));

    }

    @Test
    public void testValueGenerator() {
        assertThat("We should get the expected field", MetaUtils.extractFieldName("getSubject"),
                Matchers.equalTo("subject"));

        assertThat("We should get the expected field", MetaUtils.extractFieldName("getSubjectMatter"),
                Matchers.equalTo("subjectMatter"));

        assertThat("We should get the expected field", MetaUtils.extractFieldName("fetchValue"),
                Matchers.equalTo("fetchValue"));

        assertThat("We should get the expected field", MetaUtils.extractFieldName("fetchValue"),
                Matchers.equalTo("fetchValue"));

        assertThat("We should get the expected field", MetaUtils.extractFieldName("get"),
                Matchers.nullValue());
    }

    @Test
    public void testDeserializer()
            throws MessagingException {
        String l_suffix = "one";
        Message l_message = MimeMessageMethods.createMessage(l_suffix);

        assertThat("This class should not be serializable", !(l_message instanceof Serializable));

        Map<String, Object> l_result = (Map<String, Object>) MetaUtils.extractValuesFromObject(l_message);

        assertThat(l_result.keySet(),
                Matchers.containsInAnyOrder("isExpunged","hashCode","contentType", "size", "content", "subject", "lineCount", "messageNumber"));

        assertThat(l_result.get("contentType"), Matchers.equalTo("text/plain"));
        assertThat(l_result.get("size"), Matchers.equalTo(-1));
        assertThat(l_result.get("subject"), Matchers.equalTo("a subject by me " + l_suffix));
        assertThat(l_result.get("content"), Matchers.equalTo("a content by yours truely " + l_suffix));
        assertThat(l_result.get("lineCount"), Matchers.equalTo(-1));

    }

    @Test
    public void testDeserializer_collection()
            throws MessagingException {
        String l_suffix = "two";
        List<Message> l_messages = Collections.singletonList(MimeMessageMethods.createMessage(l_suffix));

        List l_resultList = (List) MetaUtils.extractValuesFromList(l_messages);

        assertThat("We should have an array of one element", l_resultList.size(), Matchers.equalTo(1));

        Map<String, Object> l_result = (Map<String, Object>) l_resultList.get(0);

        assertThat(l_result.keySet(),
                Matchers.containsInAnyOrder("contentType", "size", "content", "subject", "lineCount", "messageNumber",
                        "hashCode", "isExpunged"));

        assertThat(l_result.get("contentType"), Matchers.equalTo("text/plain"));
        assertThat(l_result.get("size"), Matchers.equalTo(-1));
        assertThat(l_result.get("subject"), Matchers.equalTo("a subject by me " + l_suffix));
        assertThat(l_result.get("content"), Matchers.equalTo("a content by yours truely " + l_suffix));
        assertThat(l_result.get("lineCount"), Matchers.equalTo(-1));

    }

    @Test(description = "Related to issue #159 failing during multi-part mime message deserialization")
    public void testDeserializer_MimeMessageMultiPart() throws MessagingException {

        String l_suffix = "M&M";
        Message l_messages = MimeMessageMethods.createMultiPartMessage(l_suffix);

        Object l_resultList = MetaUtils.extractValuesFromObject(l_messages);


        Map<String, Object> l_result = (Map<String, Object>) l_resultList;

        assertThat(l_result.keySet(),
                Matchers.containsInAnyOrder("contentType", "size", "content", "subject", "lineCount", "messageNumber",
                        "hashCode", "isExpunged"));

        //assertThat(l_result.get("contentType").toString(), Matchers.startsWith("multipart"));


        assertThat(l_result.get("size"), Matchers.equalTo(-1));
        assertThat(l_result.get("subject"), Matchers.equalTo("a subject by me " + l_suffix));
        //assertThat(l_result.get("content"), Matchers.equalTo("a content by yours truely " + l_suffix));
        assertThat(l_result.get("lineCount"), Matchers.equalTo(-1));
    }

    @Test
    public void testDeserializingClassWithMethodThrowingException_negative() {
        ClassWithMethodThrowingException cwe = new ClassWithMethodThrowingException();
        Map<String, Object>  l_result = (Map) MetaUtils.extractValuesFromObject(cwe);
        assertThat(l_result.keySet(), Matchers.containsInAnyOrder("second","hashCode"));
        assertThat(l_result.get("second"), Matchers.equalTo("secondValue"));
    }

    @Test
    public void testDeserializerNull() {
        String l_returnObject = null;

        //List<Message> = ArrayList
        assertThat("This class should not be serializable", !(l_returnObject instanceof Serializable));

        Object extractedReturnObject = (Map<String, Object>) MetaUtils.extractValuesFromObject(l_returnObject);
        assertThat(extractedReturnObject, Matchers.instanceOf(Map.class));

        assertThat(((Map) extractedReturnObject).size(), Matchers.equalTo(0));
    }

    @Test
    public void testNestedControl() throws JsonProcessingException, NoSuchMethodException {
        NestedExampleA_Level1 l_nested = new NestedExampleA_Level1();
        l_nested.setLevel1Field1("test");
        NestedExampleA_Level2 l_nested2 = new NestedExampleA_Level2();
        l_nested2.setLevel2Field1("test2");
        l_nested.setLevel2(l_nested2);

        Method l_method = NestedExampleA_Level1.class.getDeclaredMethod("getLevel2");
        assertThat("getLevel2 should be extractable", MetaUtils.isExtractable(l_method));
        Object l_result = MetaUtils.extractValuesFromObject(l_nested);

        //Add level 2
        NestedExampleA_Level3 l_nested3 = new NestedExampleA_Level3();
        l_nested3.setLevel3Field1("test3");
        l_nested2.setLevel3(l_nested3);

        Object l_result2 = MetaUtils.extractValuesFromObject(l_nested);

        //Add level 3
        NestedExampleA_Level4 l_nested4 = new NestedExampleA_Level4();
        l_nested4.setLevel4Field1("test4");
        l_nested3.setLevel4(l_nested4);

        JavaCallResults jcr = new JavaCallResults();
        jcr.getReturnValues().put("value", l_result);

        Object l_result3 = MetaUtils.extractValuesFromObject(l_nested);
        jcr.getReturnValues().put("value", l_result3);
        String result = BridgeServiceFactory.transformJavaCallResultsToJSON(jcr, new HashSet<>());

        assertThat("We should see the effects of nesting", ((Map)((Map) l_result3).get("level2")).get("level3"), Matchers.equalTo("... of type "+NestedExampleA_Level3.class.getName()));
    }

    @Test
    public void testingIsBasic() {

        assertThat("String is basic", MetaUtils.isBasicReturnType(String.class));
        assertThat("String is basic", MetaUtils.isBasicReturnType(Object.class));
        assertThat("String is basic", MetaUtils.isBasicReturnType(int.class));
        assertThat("String is basic", MetaUtils.isBasicReturnType(float.class));
        assertThat("String is basic", !MetaUtils.isBasicReturnType(MimeMessage.class));
    }


}
