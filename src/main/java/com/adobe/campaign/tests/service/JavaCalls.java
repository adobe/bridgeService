package com.adobe.campaign.tests.service;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class JavaCalls {

    private Map<String, CallContent> callContent;

    public JavaCalls() {
        callContent = new HashMap<>();
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
            throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        if (!this.callContent.containsKey(in_key)) {
            throw new CallDefinitionNotFoundException("Could not find a call definition with the given key "+in_key);
        }
        return this.getCallContent().get(in_key).call();
    }

    /**
     * Calls all the call definitions in this class
     *
     * @return a map with the key whoch is the same as the call keys
     */
    public JavaCallResults submitCalls()
            throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {

        JavaCallResults lr_returnObject = new JavaCallResults();

        for (String lt_key : this.getCallContent().keySet()) {
            lr_returnObject.getReturnValues().put(lt_key, this.call(lt_key));
        }

        return lr_returnObject;
    }

}
