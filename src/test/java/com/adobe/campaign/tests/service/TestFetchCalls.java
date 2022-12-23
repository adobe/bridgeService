package com.adobe.campaign.tests.service;

import com.adobe.campaign.tests.integro.tools.DateAndTimeTools;
import com.adobe.campaign.tests.integro.tools.RandomManager;
import com.adobe.campaign.tests.service.exceptions.AmbiguousMethodException;
import com.adobe.campaign.tests.service.exceptions.NonExistantJavaObjectException;
import com.adobe.campaign.tests.service.exceptions.TargetJavaClassException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;
import spark.Spark;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestFetchCalls {

    @Test
    public void testJSONCall()
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException,
            IOException, InstantiationException {
        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(RandomManager.class.getTypeName());
        l_cc.setMethodName("fetchRandomCountry");

        l_cc.setArgs(new Object[] { "A" });

        l_myJavaCalls.getCallContent().put("fetchEmail", l_cc);

        assertThat("We should access our calls correctly",
                l_myJavaCalls.getCallContent().get("fetchEmail").getClassName(),
                Matchers.equalTo(RandomManager.class.getTypeName()));
        assertThat("We should access our calls correctly",
                l_myJavaCalls.getCallContent().get("fetchEmail").getMethodName(),
                Matchers.equalTo("fetchRandomCountry"));
        assertThat("We should access our calls correctly", l_myJavaCalls.getCallContent().get("fetchEmail").getArgs(),
                Matchers.arrayContainingInAnyOrder("A"));

        l_myJavaCalls.getCallContent().get("fetchEmail").setArgs(new Object[] {});

        IntegroBridgeClassLoader iClassLoader = new IntegroBridgeClassLoader();
        Method l_definedMethod = l_myJavaCalls.getCallContent().get("fetchEmail").fetchMethod();
        assertThat("The method should not be null", l_definedMethod, Matchers.notNullValue());

        assertThat("We should have created the correct method", l_definedMethod.getName(),
                Matchers.equalTo(l_myJavaCalls.getCallContent().get("fetchEmail").getMethodName()));

        String returnedCountry = (String) l_myJavaCalls.getCallContent().get("fetchEmail").call(iClassLoader);
        assertThat("We should get a good answer back from the call",
                Arrays.asList("AT", "AU", "CA", "CH", "DE", "US", "FR", "CN", "IN", "JP", "RU", "BR", "ID", "GB", "MX")
                        .stream().anyMatch(f -> f.equals(returnedCountry)));

    }

    @Test
    public void testJSONCallWithAruments()
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException,
            IOException, InstantiationException {
        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(RandomManager.class.getTypeName());
        l_cc.setMethodName("getRandomNumber");

        l_cc.setArgs(new Object[] { 13 });

        l_myJavaCalls.getCallContent().put("fetchEmail", l_cc);

        assertThat("We should access our calls correctly", l_myJavaCalls.getCallContent().get("fetchEmail").getArgs(),
                Matchers.arrayContainingInAnyOrder(13));

        Method l_definedMethod = l_myJavaCalls.getCallContent().get("fetchEmail").fetchMethod();
        assertThat("The method should not be null", l_definedMethod, Matchers.notNullValue());

        assertThat("We should have created the correct method", l_definedMethod.getName(),
                Matchers.equalTo(l_myJavaCalls.getCallContent().get("fetchEmail").getMethodName()));

        IntegroBridgeClassLoader iClassLoader = new IntegroBridgeClassLoader();
        Object returnedValue = l_myJavaCalls.getCallContent().get("fetchEmail").call(iClassLoader);
        assertThat("We should get a good answer back from the call", (Integer) returnedValue, Matchers.lessThan(13));

    }

    @Test
    public void testMakeMultipleCalls()
            throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException,
            IllegalAccessException, InstantiationException {
        //Call 1
        JavaCalls l_myJavaCalls1 = new JavaCalls();
        CallContent l_cc1 = new CallContent();
        l_cc1.setClassName(RandomManager.class.getTypeName());
        l_cc1.setMethodName("fetchRandomCountry");
        l_cc1.setArgs(new Object[] {  });
        l_myJavaCalls1.getCallContent().put("fetchCountry", l_cc1);

        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName(RandomManager.class.getTypeName());
        l_cc2.setMethodName("getUniqueEmail");
        l_cc2.setArgs(new Object[] { "A", "B" });
        l_myJavaCalls1.getCallContent().put("fetchEmail", l_cc2);

        IntegroBridgeClassLoader iClassLoader = new IntegroBridgeClassLoader();
        JavaCallResults jcr = l_myJavaCalls1.submitCalls();
        assertThat("We should get a good answer back from the call", jcr.getReturnValues().get("fetchEmail").toString(),
                Matchers.startsWith("A+"));
        assertThat("We should get a good answer back from the call", jcr.getReturnValues().get("fetchEmail").toString(), Matchers.endsWith("@B"));

    }

    @Test
    public void testJSONCallWithTwoAruments()
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException,
            IOException, InstantiationException {

        CallContent l_cc = new CallContent();
        l_cc.setClassName(RandomManager.class.getTypeName());
        l_cc.setMethodName("getUniqueEmail");

        l_cc.setArgs(new Object[] { "A", "B" });

        Method l_definedMethod = l_cc.fetchMethod();
        assertThat("The method should not be null", l_definedMethod, Matchers.notNullValue());

        assertThat("We should have created the correct method", l_definedMethod.getName(),
                Matchers.equalTo(l_cc.getMethodName()));

        assertThat("We should have created the correct method", l_definedMethod.getParameterCount(),
                Matchers.equalTo(2));

        IntegroBridgeClassLoader iClassLoader = new IntegroBridgeClassLoader();
        Object returnedValue = l_cc.call(iClassLoader);
        assertThat("We should get a good answer back from the call", returnedValue.toString(),
                Matchers.startsWith("A+"));
        assertThat("We should get a good answer back from the call", returnedValue.toString(), Matchers.endsWith("@B"));

    }


    @Test
    public void testJSONCallWithBadArguments()
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException,
            IOException, InstantiationException, MessagingException {

        CallContent l_cc = new CallContent();
        l_cc.setClassName(RandomManager.class.getTypeName());
        l_cc.setMethodName("getRandomString");

        l_cc.setArgs(new Object[] { MimeMessageFactory.getMessage("ab") });

        IntegroBridgeClassLoader iClassLoader = new IntegroBridgeClassLoader();
        Assert.assertThrows(NonExistantJavaObjectException.class, () -> l_cc.call(iClassLoader));
    }

    @Test
    public void testJSONCallWithFailingTargetMethod() {

        CallContent l_cc = new CallContent();
        l_cc.setClassName(DateAndTimeTools.class.getTypeName());
        l_cc.setMethodName("convertStringToDate");

        l_cc.setArgs(new Object[] { "", "" });

        IntegroBridgeClassLoader iClassLoader = new IntegroBridgeClassLoader();
        Assert.assertThrows(TargetJavaClassException.class, ()->l_cc.call(iClassLoader));
    }

    @Test
    public void testMainHelloWorldCall_problem() throws IOException {

        JavaCalls l_call = new JavaCalls();
        CallContent myContent = new CallContent();
        myContent.setClassName("com.adobe.campaign.tests.integro.tools.RandomManager");
        myContent.setMethodName("getCountries");
        myContent.setReturnType("java.lang.String");
        l_call.getCallContent().put("call1PL", myContent);

        ObjectMapper mapper = new ObjectMapper();

        String mappedJSON = mapper.writeValueAsString(l_call);
        System.out.println(mappedJSON);

        ObjectMapper m2 = new ObjectMapper();
        JavaCalls j2 = JavaCallsFactory.createJavaCalls(mappedJSON);

        assertThat("Both calls should be equal" , j2.getCallContent().get("call1PL"), Matchers.equalTo(myContent));

        assertThat("Both calls should be equal" , j2, Matchers.equalTo(l_call));

        //Equal tests
        assertThat("Both calls should be equal" , j2.getCallContent().get("call1PL"), Matchers.not(Matchers.equalTo(null)));

        assertThat("Both calls should be equal" , j2, Matchers.equalTo(j2));

        assertThat("Both calls should be equal" , myContent, Matchers.equalTo(myContent));

        assertThat("Both calls should be equal" , j2, Matchers.not(Matchers.equalTo(null)));
    }

    @Test
    public void testJSONTransformation()
            throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException, InstantiationException {
        String l_jsonString =
                "{\"callContent\" :{\"call1\" :  {" + "    \"class\": \"" + RandomManager.class.getTypeName() + "\",\n"
                        + "    \"method\": \"getUniqueEmail\",\n" + "    \"returnType\": \"java.lang.String\",\n"
                        + "    \"args\": [\n" + "        \"A\",\n" + "        \"B\"\n" + "    ]\n" + "}\n" + "}}";

        ObjectMapper mapper = new ObjectMapper();
        JavaCalls fetchedFromJSON = mapper.readValue(l_jsonString, JavaCalls.class);
        CallContent l_cc = fetchedFromJSON.getCallContent().get("call1");
        assertThat(l_cc.getMethodName(), Matchers.equalTo("getUniqueEmail"));

        Method l_definedMethod = l_cc.fetchMethod();
        assertThat("The method should not be null", l_definedMethod, Matchers.notNullValue());

        assertThat("We should have created the correct method", l_definedMethod.getName(),
                Matchers.equalTo(l_cc.getMethodName()));

        assertThat("We should have created the correct method", l_definedMethod.getParameterCount(),
                Matchers.equalTo(2));

        IntegroBridgeClassLoader iClassLoader = new IntegroBridgeClassLoader();
        Object returnedValue = l_cc.call(iClassLoader);
        assertThat("We should get a good answer back from the call", returnedValue.toString(),
                Matchers.startsWith("A+"));
        assertThat("We should get a good answer back from the call", returnedValue.toString(), Matchers.endsWith("@B"));
    }

    @Test
    public void testJSONTransformation2()
            throws JsonProcessingException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException, InstantiationException {
        String l_jsonString =
                "{\"callContent\" :{\"call1\" :  {" + "    \"class\": \"" + RandomManager.class.getTypeName() + "\",\n"
                        + "    \"method\": \"getUniqueEmail\",\n" + "    \"returnType\": \"java.lang.String\",\n"
                        + "    \"args\": [\n" + "        \"A\",\n" + "        \"B\"\n" + "    ]\n" + "}\n" + "}}";

        ObjectMapper mapper = new ObjectMapper();
        JavaCalls fetchedFromJSON = mapper.readValue(l_jsonString, JavaCalls.class);

        Assert.assertThrows(CallDefinitionNotFoundException.class, () -> fetchedFromJSON.call("nonExistant"));
        Object returnedValue = fetchedFromJSON.call("call1");
        assertThat("We should get a good answer back from the call", returnedValue.toString(),
                Matchers.startsWith("A+"));
        assertThat("We should get a good answer back from the call", returnedValue.toString(), Matchers.endsWith("@B"));

        Map<String, Object> l_returnValue = fetchedFromJSON.submitCalls().getReturnValues();
        assertThat("We should have an entry with the key call1", l_returnValue.containsKey("call1"));
        Object returnedValue2 = l_returnValue.get("call1");
        assertThat("We should get a good answer back from the call", returnedValue2.toString(),
                Matchers.startsWith("A+"));

        assertThat("We should get a good answer back from the call", returnedValue2.toString(),
                Matchers.endsWith("@B"));
    }

    @Test
    public void testJSONTransformation_deserialize()
            throws JsonProcessingException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException, InstantiationException {
        String l_jsonString =
                "{\"callContent\" :{\"call1\" :  {" + "    \"class\": \"" + RandomManager.class.getTypeName() + "\",\n"
                        + "    \"method\": \"getUniqueEmail\",\n" + "    \"returnType\": \"java.lang.String\",\n"
                        + "    \"args\": [\n" + "        \"A\",\n" + "        \"B\"\n" + "    ]\n" + "}\n" + "}}";

        ObjectMapper mapper = new ObjectMapper();
        JavaCalls fetchedFromJSON = mapper.readValue(l_jsonString, JavaCalls.class);

        JavaCallResults l_returnValue = fetchedFromJSON.submitCalls();

        assertThat("The retun value should be of a correct format", l_returnValue.returnValues.containsKey("call1"));
        assertThat("The retun value should be of a correct format", l_returnValue.returnValues.get("call1").toString(),
                Matchers.startsWith("A"));
        assertThat("The retun value should be of a correct format", l_returnValue.returnValues.get("call1").toString(),
                Matchers.endsWith("B"));

    }

    @Test
    public void testValueGenerator() {
        assertThat("We should get the expected field", MetaUtils.extractFieldName("getSubject"),
                Matchers.equalTo("subject"));

        assertThat("We should get the expected field", MetaUtils.extractFieldName("getSubjectMatter"),
                Matchers.equalTo("subjectMatter"));

        assertThat("We should get the expected field", MetaUtils.extractFieldName("fetchValue"),
                Matchers.equalTo("fetchValue"));
    }

    @Test
    public void testDeserializer()
            throws MessagingException, UnsupportedEncodingException, InvocationTargetException, IllegalAccessException,
            JsonProcessingException {
        String l_suffix = "one";
        Message l_message = MimeMessageFactory.getMessage(l_suffix);

       // ObjectMapper mapper = new ObjectMapper();
       // mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
      //  System.out.println(mapper.writeValueAsString(l_message));

        //List<Message> = ArrayList
        assertThat("This class should not be serializable", !(l_message instanceof Serializable));

        Map<String, Object> l_result = (Map<String, Object>) MetaUtils.extractValuesFromObject(l_message);

        //assertThat("dddd", l_result instanceof Serializable);

        assertThat(l_result.keySet(),
                Matchers.containsInAnyOrder("contentType", "size", "content", "subject", "lineCount", "messageNumber",
                        "hashCode", "isExpunged"));

        assertThat(l_result.get("contentType"), Matchers.equalTo("text/plain"));
        assertThat(l_result.get("size"), Matchers.equalTo(-1));
        assertThat(l_result.get("subject"), Matchers.equalTo("a subject by me " + l_suffix));
        assertThat(l_result.get("content"), Matchers.equalTo("a content by yours truely " + l_suffix));
        assertThat(l_result.get("lineCount"), Matchers.equalTo(-1));

    }

    @Test
    public void testDeserializer_collection()
            throws MessagingException, UnsupportedEncodingException, InvocationTargetException, IllegalAccessException {
        String l_suffix = "two";
        List<Message> l_messages = Arrays.asList(MimeMessageFactory.getMessage(l_suffix));
        Object ananymized = l_messages;

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

    @Test
    public void testSeparationOfStaticFields()
            throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException,
            IOException, InstantiationException {

        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName("com.adobe.campaign.tests.integro.tools.RandomManager");
        l_cc.setMethodName("getRandomEmail");
        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName("com.adobe.campaign.tests.integro.core.SystemValueHandler");
        l_cc2.setMethodName("setIntegroCache");
        Properties l_authentication = new Properties();
        l_authentication.put("AC.UITEST.MAILING.PREFIX", "bada");
        l_authentication.put("AC.INTEGRO.MAILING.BASE", "boom.com");
        l_cc2.setArgs(new Object[] { l_authentication });

        l_myJavaCalls.getCallContent().put("aaa", l_cc2);

        l_myJavaCalls.getCallContent().put("getRandomEmail", l_cc);

        JavaCallResults returnedValue = l_myJavaCalls.submitCalls();

        System.out.println(JavaCallsFactory.transformJavaCallResultsToJSON(returnedValue));

        assertThat("We should get a good answer back from the call",
                returnedValue.getReturnValues().get("getRandomEmail").toString(),
                Matchers.startsWith("bada+"));
        assertThat("We should get a good answer back from the call",
                returnedValue.getReturnValues().get("getRandomEmail").toString(), Matchers.endsWith("@boom.com"));

        JavaCalls l_myJavaCallsB = new JavaCalls();
        CallContent l_ccB = new CallContent();
        l_ccB.setClassName("com.adobe.campaign.tests.integro.tools.RandomManager");
        l_ccB.setMethodName("getRandomEmail");
        l_myJavaCallsB.getCallContent().put("getRandomEmailB", l_ccB);

        JavaCallResults returnedValueB = l_myJavaCallsB.submitCalls();
        //Object b = l_ccB.call(new IntegroBridgeClassLoader());
        //System.out.println(b);
        assertThat("We should get a good answer back from the call",
                returnedValueB.getReturnValues().get("getRandomEmailB").toString(),
                Matchers.startsWith("testqa+"));
        assertThat("We should get a good answer back from the call",
                returnedValueB.getReturnValues().get("getRandomEmailB").toString(),
                Matchers.endsWith("@localhost.corp.adobe.com"));
    }

    @Test
    public void testSeparationOfStaticFields_json()
            throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException,
            IOException, InstantiationException {

        String l_jsonString =
                "{\n"
                        + "    \"callContent\": {\n"
                        + "        \"call1\": {\n"
                        + "            \"class\": \"com.adobe.campaign.tests.integro.tools.RandomManager\",\n"
                        + "            \"method\": \"getRandomEmail\",\n"
                        + "            \"returnType\": \"java.lang.String\",\n"
                        + "            \"args\": []\n"
                        + "        }\n"
                        + "    },\n"
                        + "    \"environmentVariables\": {\n"
                        + "        \"AC.UITEST.MAILING.PREFIX\": \"tyrone\",\n"
                        + "        \"AC.INTEGRO.MAILING.BASE\" : \"profane.com\"\n"
                        + "    }\n"
                        + "}";

        ObjectMapper mapper = new ObjectMapper();
        JavaCalls fetchedFromJSON = mapper.readValue(l_jsonString, JavaCalls.class);

        JavaCallResults returnedValue = fetchedFromJSON.submitCalls();

        System.out.println(JavaCallsFactory.transformJavaCallResultsToJSON(returnedValue));

        assertThat("We should get a good answer back from the call",
                returnedValue.getReturnValues().get("call1").toString(),
                Matchers.startsWith("tyrone+"));

        assertThat("We should get a good answer back from the call",
                returnedValue.getReturnValues().get("call1").toString(), Matchers.endsWith("@profane.com"));

    }

    @Test
    public void testIssueWithAmbiguousCallException() throws ClassNotFoundException {
        CallContent l_cc = new CallContent();
        l_cc.setClassName("com.adobe.campaign.tests.integro.tools.EmailClientTools");
        l_cc.setMethodName("fetchEmail");
        l_cc.setArgs(new Object[] { "testqa+krs3726@acc-simulators.email.corp.adobe.com", "rcdbxq",
                Boolean.TRUE.booleanValue() });
        IntegroBridgeClassLoader ibcl = new IntegroBridgeClassLoader();

        Assert.assertThrows(AmbiguousMethodException.class,
                () -> l_cc.fetchMethod(ibcl.loadClass(l_cc.getClassName())));

    }

    @Test(enabled = false)
    public void testIssueWithAmbiguousCall() throws ClassNotFoundException {
        CallContent l_cc = new CallContent();
        l_cc.setClassName("com.adobe.campaign.tests.integro.tools.EmailClientTools");
        l_cc.setMethodName("fetchEmail");
        l_cc.setArgs(new Object[] { "testqa+krs3726@acc-simulators.email.corp.adobe.com", "rcdbxq", Boolean.TRUE });
        IntegroBridgeClassLoader ibcl = new IntegroBridgeClassLoader();

        List<Method> l_methods = l_cc.fetchMethodCandidates(ibcl.loadClass(l_cc.getClassName()));

        assertThat("We should only find one method", l_methods.size(), Matchers.equalTo(1));
    }

    @Test
    public void testIssueWithNonExistantMethodException() throws ClassNotFoundException {
        CallContent l_cc = new CallContent();
        l_cc.setClassName("com.adobe.campaign.tests.integro.tools.EmailClientTools");
        l_cc.setMethodName("fetchEmailNonExistant");
        l_cc.setArgs(new Object[] { "testqa+krs3726@acc-simulators.email.corp.adobe.com", "rcdbxq", Boolean.TRUE });
        IntegroBridgeClassLoader ibcl = new IntegroBridgeClassLoader();

        Assert.assertThrows(NonExistantJavaObjectException.class,
                () -> l_cc.fetchMethod(ibcl.loadClass(l_cc.getClassName())));
    }

    @Test
    public void testIssueWithNonExistantClassException() throws ClassNotFoundException {
        CallContent l_cc = new CallContent();
        l_cc.setClassName("com.adobe.campaign.tests.integro.tools.EmailNonExistantTools");
        l_cc.setMethodName("fetchEmail");
        l_cc.setArgs(new Object[] { "testqa+krs3726@acc-simulators.email.corp.adobe.com", "rcdbxq", Boolean.TRUE });
        IntegroBridgeClassLoader ibcl = new IntegroBridgeClassLoader();

        Assert.assertThrows(NonExistantJavaObjectException.class,
                () -> l_cc.fetchMethod(ibcl.loadClass(l_cc.getClassName())));
    }

    @Test(enabled = false)
    public void testIssueWithAmbiguousCall_Apache() throws ClassNotFoundException {
        CallContent l_cc = new CallContent();
        l_cc.setClassName("com.adobe.campaign.tests.integro.tools.EmailClientTools");
        l_cc.setMethodName("fetchEmail");
        System.out.println(boolean.class.getTypeName());
        l_cc.setArgs(new Object[] { "testqa+krs3726@acc-simulators.email.corp.adobe.com", "rcdbxq", "boolean" });
        //l_cc.setArgTypes(new Object[] { "java.lang.String", "java.lang.String", "" });
        IntegroBridgeClassLoader ibcl = new IntegroBridgeClassLoader();
        List<Method> l_methods = l_cc.fetchMethodCandidates(ibcl.loadClass(l_cc.getClassName()));
        assertThat("We should only find one method", l_methods.size(), Matchers.equalTo(1));
    }

    @Test(enabled = false)
    public void testThatWeCanSendAndReceiveEmails()
            throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException,
            IllegalAccessException, InstantiationException {
        JavaCalls l_myJavaCalls = new JavaCalls();
        // testqa+qvm6747@acc-simulators.email.corp.adobe.com hzgiwz AC.UITEST.MAILING_PORT=143 (-1)
        CallContent l_cc = new CallContent();
        l_cc.setClassName("com.adobe.campaign.tests.integro.tools.EmailClientTools");
        l_cc.setMethodName("fetchEmail");
        l_cc.setArgs(new Object[] { "testqa+krs3726@acc-simulators.email.corp.adobe.com", "rcdbxq" });
        l_myJavaCalls.getCallContent().put("emailCheck", l_cc);

        l_myJavaCalls.getEnvironmentVariables().put("AC.UITEST.MAILING.HOST", "acc-simulators.email.corp.adobe.com");
        l_myJavaCalls.getEnvironmentVariables().put("AC.UITEST.MAILING_PORT", "143");
        l_myJavaCalls.getEnvironmentVariables().put("AC.UITEST.MAILING.PREFIX", "testqa");
        l_myJavaCalls.getEnvironmentVariables()
                .put("AC.UITEST.MAILING.ID", "testqa@acc-simulators.email.corp.adobe.com");
        l_myJavaCalls.getEnvironmentVariables().put("AC.UITEST.MAILING.PWD", "changeme");
        l_myJavaCalls.getEnvironmentVariables().put("AC.UITEST.MAILING.PROVIDER", "imap");
        l_myJavaCalls.getEnvironmentVariables().put("AC.UITEST.MAILING.FOLDER", "INBOX");

        JavaCallResults jcr = l_myJavaCalls.submitCalls();

//        Map x = (Map) MetaUtils.extractValuesFromObject(jcr.getReturnValues().get("emailCheck"));
        System.err.println(JavaCallsFactory.transformJavaCallResultsToJSON(jcr));

/*
        ObjectMapper mapper = new ObjectMapper();

        mapper.writeValueAsString(jcr);
        System.out.println(mapper.writeValueAsString(l_myJavaCalls));
*/

    }

    @Test
    public void testExtractable() {

        Class<?> l_myClass = MimeMessage.class;


        assertThat("MimeMessage is not serializable",Message.class instanceof Serializable);

        assertThat("MimeMessage is not serializable",!MetaUtils.isExtractable(l_myClass));

        Class<?> l_String = String.class;

        assertThat("String is serializable",MetaUtils.isExtractable(l_String));

        Class<?> l_int = int.class;

        assertThat("int is extractable",MetaUtils.isExtractable(l_int));

        Class<?> l_list = List.class;


        assertThat("list is extractable",MetaUtils.isExtractable(l_list));
    }

    @Test
    public void testExtractableMethod() throws NoSuchMethodException {
        Method l_simpleGetter = MimeMessage.class.getDeclaredMethod("getFileName");

        assertThat("getFileName is extractable",MetaUtils.isExtractable(l_simpleGetter));

        Method l_simpleSetter = MimeMessage.class.getDeclaredMethod("setFrom");

        assertThat("setSender is should not be extractable",!MetaUtils.isExtractable(l_simpleSetter));

        Method l_simpleIs = String.class.getDeclaredMethod("isEmpty");

        assertThat("isEmpty is extractable",MetaUtils.isExtractable(l_simpleIs));

        Method l_hashSet = MimeMessage.class.getDeclaredMethod("getSize");
        assertThat("HashCode is NOT extractable",MetaUtils.isExtractable(l_hashSet));


    }


    @Test
    public void testExtract() throws NoSuchMethodException {

        String l_simpleString = "testValue";

        assertThat("getFileName is extractable",MetaUtils.extractValuesFromObject(l_simpleString), Matchers.equalTo(l_simpleString));

        //List<String>
        Object l_simpleList = Arrays.asList(l_simpleString);
        Class t = l_simpleList.getClass();

        assertThat("sss", l_simpleList instanceof Collection);

        Method l_simpleSetter = MimeMessage.class.getDeclaredMethod("setFrom");

        assertThat("setSender is should not be extractable",!MetaUtils.isExtractable(l_simpleSetter));

        Method l_simpleIs = String.class.getDeclaredMethod("isEmpty");

        assertThat("isEmpty is extractable",MetaUtils.isExtractable(l_simpleIs));

        Method l_hashSet = MimeMessage.class.getDeclaredMethod("getSize");
        assertThat("HashCode is NOT extractable",MetaUtils.isExtractable(l_hashSet));


    }


    @Test
    public void prepareExtractMimeMessage()
            throws MessagingException, UnsupportedEncodingException, JsonProcessingException {
        Message m1 = MimeMessageFactory.getMessage("five");
        Message m2 = MimeMessageFactory.getMessage("six");
        List<Message> messages = Arrays.asList(m1,m2);
        Object x = messages;

        assertThat("The values should be extractable", MetaUtils.isExtractable(x.getClass()));
        JavaCallResults jcr = new JavaCallResults();
        jcr.returnValues.put("value",MetaUtils.extractValuesFromObject(x));



        String value = JavaCallsFactory.transformJavaCallResultsToJSON(jcr);

        System.out.println(value);

    }


    @Test
    public void prepareExtractSimpleString()
            throws MessagingException, UnsupportedEncodingException, JsonProcessingException {
        String m1 = "five";
        String m2 = "six";
        List<String> messages = Arrays.asList(m1,m2);
        Object x = messages;

        assertThat("The values should be extractable", MetaUtils.isExtractable(x.getClass()));
        JavaCallResults jcr = new JavaCallResults();
        jcr.returnValues.put("value",MetaUtils.extractValuesFromObject(x));



        String value = JavaCallsFactory.transformJavaCallResultsToJSON(jcr);

        System.out.println(value);

    }

    @Test
    public void prepareExtractSimpleInt()
            throws MessagingException, UnsupportedEncodingException, JsonProcessingException {
        int m1 = 5;
        int m2 = 6;
        List messages = Arrays.asList(m1,m2);
        Object x = messages;

        assertThat("The values should be extractable", MetaUtils.isExtractable(x.getClass()));
        JavaCallResults jcr = new JavaCallResults();
        jcr.returnValues.put("value",MetaUtils.extractValuesFromObject(x));



        String value = JavaCallsFactory.transformJavaCallResultsToJSON(jcr);

        System.out.println(value);

    }

    @BeforeGroups(groups = "E2E")
    public void startUpService() {
        IntegroAPI iapi = new IntegroAPI();
        iapi.startServices();
        Spark.awaitInitialization();
    }

    public static final String EndPointURL = "http://localhost:4567/";

    @Test(groups = "E2E")
    public void testMainHelloWorld() {
        given().when().get(EndPointURL + "hello").then().assertThat().equals("Hello Worlds");

    }

    @Test(groups = "E2E")
    public void testMainHelloWorldCall() throws IOException {

        JavaCalls l_call = new JavaCalls();
        CallContent myContent = new CallContent();
        myContent.setClassName("com.adobe.campaign.tests.integro.tools.RandomManager");
        myContent.setMethodName("getCountries");
        myContent.setReturnType("java.lang.String");
        l_call.getCallContent().put("call1PL", myContent);

        given().body(l_call).post(EndPointURL + "call").then().assertThat().body("returnValues.call1PL",
                Matchers.containsInAnyOrder("AT", "AU", "CA", "CH", "DE", "US", "FR", "CN", "IN", "JP", "RU", "BR",
                        "ID", "GB", "MX"));

    }

    @Test(groups = "E2E")
    public void testErrors() {
        JavaCallResults jcr = new JavaCallResults();

        given().body(jcr).post(EndPointURL + "call").then().statusCode(400).and().assertThat()
                .equals(IntegroAPI.ERROR_JSON_TRANSFORMATION);
    }

    @AfterGroups(groups = "E2E")
    public void tearDown() {
        Spark.stop();
    }

}
