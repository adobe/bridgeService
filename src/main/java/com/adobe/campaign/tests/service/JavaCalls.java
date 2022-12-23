package com.adobe.campaign.tests.service;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class JavaCalls {

    private Map<String, CallContent> callContent;

   // private Map<String,String> environmentVariables;
   private Map<String,Object> environmentVariables;

    IntegroBridgeClassLoader localClassLoader;



    public JavaCalls() {
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

        updateEnvironmentVariables();

        JavaCallResults lr_returnObject = new JavaCallResults();

        for (String lt_key : this.getCallContent().keySet()) {
            Object callResult = this.call(lt_key);

            lr_returnObject.addResult(lt_key, MetaUtils.extractValuesFromObject(callResult));
        }

        return lr_returnObject;
    }

    private void updateEnvironmentVariables()
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException,
            InstantiationException {
        CallContent l_setEnvironmetVars = new CallContent();
        l_setEnvironmetVars.setClassName("com.adobe.campaign.tests.integro.core.SystemValueHandler");
        l_setEnvironmetVars.setMethodName("setIntegroCache");

        //Fetch all environment variables
        Properties argumentProps = new Properties();
        environmentVariables.keySet().stream().forEach(k -> argumentProps.put(k, environmentVariables.get(k)));
        l_setEnvironmetVars.setArgs(new Object[] { argumentProps });
        l_setEnvironmetVars.call(this.localClassLoader);
    }

    public Map<String, Object> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(Map<String, Object> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JavaCalls javaCalls = (JavaCalls) o;

        return getCallContent().equals(javaCalls.getCallContent()) && getEnvironmentVariables().equals(
                javaCalls.getEnvironmentVariables());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCallContent(), getEnvironmentVariables());
    }
}
