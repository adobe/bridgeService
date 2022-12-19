package com.adobe.campaign.tests.service;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class JavaCalls {

    private Map<String, CallContent> callContent;

   // private Map<String,String> environmentVariables;
   private Map<String,Object> environmentVariables;

    IntegroBridgeClassLoader localClassLoader;



    public JavaCalls() throws IOException {
        callContent = new LinkedHashMap<>();
        environmentVariables = new HashMap<>();
        localClassLoader = new IntegroBridgeClassLoader();
    }


    public Map<String, CallContent> getCallContent() {
        return callContent;
    }

    public void setCallContent(Map<String, CallContent> callContent) {
        this.callContent = callContent;
    }

    /**
     * Calls the underlying method call
     * @param in_key the key for identifying the java call
     * @return A map of the results
     */
    public Object call(String in_key)
            throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException,
            InstantiationException {
        if (!this.callContent.containsKey(in_key)) {
            throw new CallDefinitionNotFoundException("Could not find a call definition with the given key "+in_key);
        }
        return this.getCallContent().get(in_key).call(localClassLoader);
    }

    /**
     * Calls all the call definitions in this class
     *
     * @return a map with the key whoch is the same as the call keys
     */
    public JavaCallResults submitCalls()
            throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException,
            InstantiationException {

        CallContent l_setEnvironmetVars = new CallContent();
        l_setEnvironmetVars.setClassName("com.adobe.campaign.tests.integro.core.SystemValueHandler");
        l_setEnvironmetVars.setMethodName("setIntegroCache");

        //Fetch all environment variables
        Properties argumentProps = new Properties();
        environmentVariables.keySet().stream().forEach(k -> argumentProps.put(k, environmentVariables.get(k)));
        l_setEnvironmetVars.setArgs(new Object[] { argumentProps });
        l_setEnvironmetVars.call(this.localClassLoader);

        JavaCallResults lr_returnObject = new JavaCallResults();

        for (String lt_key : this.getCallContent().keySet()) {
            lr_returnObject.getReturnValues().put(lt_key, this.call(lt_key));
        }

        return lr_returnObject;
    }

    public Map<String, Object> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(Map<String, Object> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }
}
