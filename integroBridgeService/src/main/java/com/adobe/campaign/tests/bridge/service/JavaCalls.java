/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import com.adobe.campaign.tests.bridge.service.exceptions.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class JavaCalls {

    private Long timeout;

    private Map<String, CallContent> callContent;

    private Properties environmentVariables;

    @JsonIgnore
    private IntegroBridgeClassLoader localClassLoader;

    @JsonIgnore
    private ExecutorService executor;

    public JavaCalls() {
        callContent = new LinkedHashMap<>();
        environmentVariables = new Properties();
        setLocalClassLoader(new IntegroBridgeClassLoader());
        timeout = Long.parseLong(ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.fetchValue());

        executor = Executors.newSingleThreadExecutor();
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

        Object lr_object;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable caller = () -> {   // Lambda Expression

            return this.getCallContent().get(in_key).call(getLocalClassLoader());
        };
        Future<Object> future = executor.submit(caller);
        try {
            lr_object = getTimeout()>0 ? future.get(getTimeout(), TimeUnit.MILLISECONDS) : future.get() ;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {

            if (e.getCause() instanceof  NonExistentJavaObjectException) {
                throw new NonExistentJavaObjectException(e.getMessage());
            } else if (e.getCause() instanceof AmbiguousMethodException) {
                throw new AmbiguousMethodException(e.getMessage());
            } else if (e.getCause() instanceof TargetJavaMethodCallException) {
                throw new TargetJavaMethodCallException(e.getCause().getMessage(), e.getCause().getCause());
            } else {
                throw new IBSRunTimeException(e.getMessage());
            }

        } catch (TimeoutException e) {
            throw new IBSTimeOutException("The call for "+in_key+" took longer than the set time limit of "+getTimeout()+"ms. Process was therefore interrupted by the Bridge Service.");
        }
        return lr_object;
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

            long l_startOfCall = System.currentTimeMillis();
            Object callResult = this.call(lt_key);
            long l_endOfCall = System.currentTimeMillis();

            getLocalClassLoader().getCallResultCache().put(lt_key, callResult);
            lr_returnObject.addResult(lt_key, MetaUtils.extractValuesFromObject(callResult), l_endOfCall - l_startOfCall);
        }

        return lr_returnObject;
    }

    private void updateEnvironmentVariables() {
        CallContent l_setEnvironmentVars = new CallContent();
        l_setEnvironmentVars.setClassName(ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.fetchValue());
        l_setEnvironmentVars.setMethodName(ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_METHOD.fetchValue());

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
            throw new IBSConfigurationException("The given environment value handler "+ ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.fetchValue()+ "."+ ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_METHOD.fetchValue()+ " could not be found.", nejoe);
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

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

}
