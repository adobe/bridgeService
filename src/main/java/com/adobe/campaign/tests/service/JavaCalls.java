package com.adobe.campaign.tests.service;

import com.adobe.campaign.tests.service.exceptions.IBSConfigurationException;
import com.adobe.campaign.tests.service.exceptions.IBSRunTimeException;
import com.adobe.campaign.tests.service.exceptions.NonExistentJavaObjectException;

import java.util.*;
import java.util.stream.Collectors;

public class JavaCalls {

    private Map<String, CallContent> callContent;

    private Properties environmentVariables;

    private IntegroBridgeClassLoader localClassLoader;

    public JavaCalls() {
        callContent = new LinkedHashMap<>();
        environmentVariables = new Properties();
        setLocalClassLoader(new IntegroBridgeClassLoader());
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
    public Object call(String in_key) {
        if (!this.callContent.containsKey(in_key)) {
            throw new CallDefinitionNotFoundException("Could not find a call definition with the given key "+in_key);
        }
        return this.getCallContent().get(in_key).call(getLocalClassLoader());
    }

    /**
     * Calls all the call definitions in this class
     *
     * @return a map with the key whoch is the same as the call keys
     */
    public JavaCallResults submitCalls() {

        if (!getEnvironmentVariables().isEmpty()) {
            updateEnvironmentVariables();
        }

        JavaCallResults lr_returnObject = new JavaCallResults();

        //for (String lt_key : this.getCallContent().keySet().stream().sorted().collect(Collectors.toList())) {
        for (String lt_key : this.getCallContent().keySet()) {
            //Adapt CallContent to results
            //this.getCallContent().get(lt_key).expandArgs(lr_returnObject);
            Object callResult = this.call(lt_key);
            getLocalClassLoader().getCallResultCache().put(lt_key, callResult);
            lr_returnObject.addResult(lt_key, MetaUtils.extractValuesFromObject(callResult));
        }

        return lr_returnObject;
    }

    private void updateEnvironmentVariables() {
        CallContent l_setEnvironmentVars = new CallContent();
        l_setEnvironmentVars.setClassName(ConfigValueHandler.ENVIRONMENT_VARS_SETTER_CLASS.fetchValue());
        l_setEnvironmentVars.setMethodName(ConfigValueHandler.ENVIRONMENT_VARS_SETTER_METHOD.fetchValue());

        List<Object> badVariables = getEnvironmentVariables().keySet().stream().filter(k -> getEnvironmentVariables().getProperty((String) k)==null).collect(
                Collectors.toList());

        if (badVariables.size()>0) {
            throw new IBSRunTimeException("The given environment variables should only contain strings.\n"+badVariables);
        }

        //Fetch all environment variables
        l_setEnvironmentVars.setArgs(new Object[] { environmentVariables });
        try {
            l_setEnvironmentVars.call(this.getLocalClassLoader());
        } catch (NonExistentJavaObjectException nejoe) {
            throw new IBSConfigurationException("The given environment value handler "+ConfigValueHandler.ENVIRONMENT_VARS_SETTER_CLASS.fetchValue()+ "."+ConfigValueHandler.ENVIRONMENT_VARS_SETTER_METHOD.fetchValue()+ " could not be found.", nejoe);
        }
    }

    public Properties getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(Properties environmentVariables) {
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

    public IntegroBridgeClassLoader getLocalClassLoader() {
        return localClassLoader;
    }

    public void setLocalClassLoader(IntegroBridgeClassLoader localClassLoader) {
        this.localClassLoader = localClassLoader;
    }
}
