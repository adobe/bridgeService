/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.integration;



/*
import com.adobe.campaign.tests.integro.tools.RandomManager;
import com.adobe.campaign.tests.service.CallContent;
import com.adobe.campaign.tests.service.ConfigValueHandlerIBS;
import com.adobe.campaign.tests.service.JavaCallResults;
import com.adobe.campaign.tests.service.JavaCalls;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
*/

import com.adobe.campaign.tests.bridge.service.ConfigValueHandlerIBS;
import org.hamcrest.Matchers;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;

public class IntegrationTests {
    String EndPointURL;

    @BeforeClass(alwaysRun = true)
    public void prepareEnvironment() {
        EndPointURL = ConfigValueHandlerIBS.PRODUCT_DEPLOYMENT_URL.fetchValue();
    }



    //Test to make sure that we can do two integro calls
/*
    @Test(groups = "Integration")
    public void testMainHelloWorldCall() throws JsonProcessingException {
        String l_recipientID1 = "";
        String l_recipientID2 = "";
        String randomEmailCall1 = RandomManager.getRandomEmail();
        String randomEmailCall2 = RandomManager.getRandomEmail();

        //CALL1 accintg-dev134
        String l_systemURL1 = "";
        String l_systemID1 = "";
        String l_systemPWD1 = "";

        //CALL2 accintg-dev90
        String l_systemURL2 = "";
        String l_systemID2 = "";
        String l_systemPWD2 = "";

        System.out.println("------------- Call 1");
        {
            JavaCalls l_call1 = new JavaCalls();
            CallContent call1Content1 = new CallContent();
            call1Content1.setClassName("utils.CampaignUtils");
            call1Content1.setMethodName("setCurrentAuthenticationToLocal");
            call1Content1.setReturnType("java.lang.String");

            call1Content1.setArgs(
                    new Object[] { l_systemURL1, l_systemID1, l_systemPWD1 });

            CallContent call1Content2 = new CallContent();
            call1Content2.setClassName("testhelper.NmsRecipientHelper");
            call1Content2.setMethodName("createRecipient");
            call1Content2.setReturnType("java.lang.String");
            call1Content2.setArgs(
                    new Object[] { RandomManager.getRandomPersonFirstName(), RandomManager.getRandomPersonLastName(),
                            randomEmailCall1 });

            l_call1.getCallContent().put("call1Cont1", call1Content1);
            l_call1.getCallContent().put("call1RecipientID", call1Content2);

            String x = given().body(l_call1).post(EndPointURL + "call").thenReturn().getBody().asPrettyString();
            System.out.println(x);
            ObjectMapper mapper = new ObjectMapper();
            JavaCallResults jcr1 = mapper.readValue(x, JavaCallResults.class);

            l_recipientID1 = jcr1.getReturnValues().get("call1RecipientID").toString();
        }

        System.out.println("------------- Call 2");
        //CALL2 accintg-dev90
        {

            JavaCalls l_call2 = new JavaCalls();
            CallContent call2Content1 = new CallContent();
            call2Content1.setClassName("utils.CampaignUtils");
            call2Content1.setMethodName("setCurrentAuthenticationToLocal");
            call2Content1.setReturnType("java.lang.String");
            call2Content1.setArgs(
                    new Object[] { l_systemURL2, l_systemID2, l_systemPWD2 });

            CallContent call2Content2 = new CallContent();
            call2Content2.setClassName("testhelper.NmsRecipientHelper");
            call2Content2.setMethodName("createRecipient");
            call2Content2.setReturnType("java.lang.String");

            call2Content2.setArgs(
                    new Object[] { RandomManager.getRandomPersonFirstName(), RandomManager.getRandomPersonLastName(),
                            randomEmailCall2 });

            CallContent call2Content3 = new CallContent();
            call2Content3.setClassName("testhelper.NmsRecipientHelper");
            call2Content3.setMethodName("getRecipientEmailById");
            call2Content3.setReturnType("java.lang.String");
            System.out.println("recipient used : "+l_recipientID1+ ", email would be: "+randomEmailCall1);
            call2Content3.setArgs(
                    new Object[] { l_recipientID1 });

            l_call2.getCallContent().put("call2Cont1", call2Content1);
            l_call2.getCallContent().put("call2RecipientID", call2Content2);
            l_call2.getCallContent().put("call2RecipientExists", call2Content3);

            String x = given().body(l_call2).post(EndPointURL + "call").thenReturn().getBody().asPrettyString();
            System.out.println(x);
            ObjectMapper mapper = new ObjectMapper();
            JavaCallResults jcr1 = mapper.readValue(x, JavaCallResults.class);

            l_recipientID2 = jcr1.getReturnValues().get("call2RecipientID").toString();
            assertThat(jcr1.getReturnValues().get("call2RecipientExists").toString(), Matchers.not(Matchers.equalTo(randomEmailCall1)));
        }

        System.out.println("------------- Call 3");
        //CALL3 accintg-dev134 randomEmailCall2 should not be here either
        {
            JavaCalls l_call3 = new JavaCalls();
            CallContent call3Content1 = new CallContent();
            call3Content1.setClassName("utils.CampaignUtils");
            call3Content1.setMethodName("setCurrentAuthenticationToLocal");
            call3Content1.setReturnType("java.lang.String");
            call3Content1.setArgs(
                    new Object[] { l_systemURL1, l_systemID1, l_systemPWD1 });

            CallContent call3Content2 = new CallContent();
            call3Content2.setClassName("testhelper.NmsRecipientHelper");
            call3Content2.setMethodName("getRecipientEmailById");
            call3Content2.setReturnType("java.lang.String");

            System.out.println("recipient used : "+l_recipientID2+ ", email would be: "+randomEmailCall2);
            call3Content2.setArgs(
                    new Object[] { l_recipientID2 });


            //call3Content2.setArgs(
            //        new Object[] { l_recipientID1 });

            l_call3.getCallContent().put("call3Cont1", call3Content1);
            l_call3.getCallContent().put("call3RecipientExists", call3Content2);

            String x = given().body(l_call3).post(EndPointURL + "call").thenReturn().getBody().asPrettyString();
            System.out.println(x);
            ObjectMapper mapper = new ObjectMapper();
            JavaCallResults jcr1 = mapper.readValue(x, JavaCallResults.class);
            assertThat(jcr1.getReturnValues().get("call2RecipientExists"), Matchers.not(Matchers.equalTo(randomEmailCall1)));
        }


    }
    */

    @Test(groups = "monitoring")
    public void testMainHelloWorld() {

        System.out.println("calling :  "+EndPointURL + "test");
        given().when().get(EndPointURL + "test").then().assertThat().body(Matchers.startsWith("All systems up  -")).body(Matchers.containsString("Bridge Service Version"));
    }

}
