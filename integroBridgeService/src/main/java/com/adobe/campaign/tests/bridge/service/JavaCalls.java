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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class JavaCalls {

    private static final Logger log = LogManager.getLogger();
    private Map<String, Assertion> assertions;
    private Long timeout;
    private Map<String, CallContent> callContent;
    private Properties environmentVariables;
    @JsonIgnore
    private IntegroBridgeClassLoader localClassLoader;

    public JavaCalls() {
        callContent = new LinkedHashMap<>();
        assertions = new LinkedHashMap<>();
        environmentVariables = new Properties();
        setLocalClassLoader(new IntegroBridgeClassLoader());
        timeout = Long.parseLong(ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.fetchValue());
    }

    public Map<String, CallContent> getCallContent() {
        return callContent;
    }

    /**
     * Calls the underlying method call
     *
     * @param in_key the key for identifying the java call
     * @return A map of the results
     */
    public Object call(String in_key) {

        if (!this.callContent.containsKey(in_key)) {
            throw new CallDefinitionNotFoundException("Could not find a call definition with the given key " + in_key);
        }

        Object lr_object;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable caller = () -> {   // Lambda Expression

            return this.getCallContent().get(in_key).call(getLocalClassLoader());
        };
        Future<Object> future = executor.submit(caller);
        try {
            lr_object = getTimeout() > 0 ? future.get(getTimeout(), TimeUnit.MILLISECONDS) : future.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {

            if (e.getCause() instanceof NonExistentJavaObjectException) {
                throw new NonExistentJavaObjectException(e.getMessage());
            } else if (e.getCause() instanceof AmbiguousMethodException) {
                throw new AmbiguousMethodException(e.getMessage());
            } else if (e.getCause() instanceof TargetJavaMethodCallException) {
                throw new TargetJavaMethodCallException(e.getCause().getMessage(), e.getCause().getCause());
            } else if (e.getCause() instanceof ClassLoaderConflictException) {
                throw new IBSConfigurationException(e.getCause().getMessage(), e.getCause());
            } else if (e.getCause() instanceof JavaObjectInaccessibleException) {
                throw new JavaObjectInaccessibleException(e.getCause().getMessage(), e.getCause().getCause());

            } else {
                throw new IBSRunTimeException(e.getMessage());
            }

        } catch (TimeoutException e) {
            throw new IBSTimeOutException(
                    "The call for " + in_key + " took longer than the set time limit of " + getTimeout()
                            + "ms. Process was therefore interrupted by the Bridge Service.");
        }
        return lr_object;
    }

    /**
     * Calls all the call definitions in this class
     *
     * @return a map with the key which is the same as the call keys
     */
    public JavaCallResults submitCalls() {

        if (!getEnvironmentVariables().isEmpty()) {
            LogManagement.logStep(LogManagement.STD_STEPS.ENVVARS);
            updateEnvironmentVariables();
        }

        JavaCallResults lr_returnObject = new JavaCallResults();

        for (String lt_key : this.getCallContent().keySet()) {
            LogManagement.logStep(lt_key);

            long l_startOfCall = System.currentTimeMillis();
            Object callResult = this.call(lt_key);
            long l_endOfCall = System.currentTimeMillis();

            getLocalClassLoader().getCallResultCache().put(lt_key, callResult);
            lr_returnObject.addResult(lt_key, MetaUtils.extractValuesFromObject(callResult),
                    l_endOfCall - l_startOfCall);
        }

        getAssertions().forEach((k, v) -> {
            LogManagement.logStep("Asserting " + k);
            lr_returnObject.getAssertionResults().put(k, v.perform(getLocalClassLoader(), lr_returnObject));
        });

        LogManagement.logStep(LogManagement.STD_STEPS.SEND_RESULT);
        return lr_returnObject;
    }

    private void updateEnvironmentVariables() {
        CallContent l_setEnvironmentVars = new CallContent();
        l_setEnvironmentVars.setClassName(ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.fetchValue());
        l_setEnvironmentVars.setMethodName(ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_METHOD.fetchValue());

        List<Object> badVariables = getEnvironmentVariables().keySet().stream()
                .filter(k -> getEnvironmentVariables().getProperty((String) k) == null).collect(
                        Collectors.toList());

        if (badVariables.size() > 0) {
            throw new IBSRunTimeException(
                    "The given environment variables should only contain strings.\n" + badVariables);
        }

        //Fetch all environment variables
        l_setEnvironmentVars.setArgs(new Object[] { environmentVariables });
        try {
            l_setEnvironmentVars.call(this.getLocalClassLoader());
        } catch (NonExistentJavaObjectException nejoe) {
            throw new IBSConfigurationException("The given environment value handler "
                    + ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.fetchValue() + "."
                    + ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_METHOD.fetchValue() + " could not be found.",
                    nejoe);
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

    public Map<String, Assertion> getAssertions() {
        return assertions;
    }

    public void setAssertions(Map<String, Assertion> assertions) {
        this.assertions = assertions;
    }

    /**
     * Adds headers to the results cache of the ClassLoader. We throw an exception o the header corresponds to a
     * callContent
     *
     * @param in_mapOHeaders A map containing header values coming from the request
     */
    public void addHeaders(Map<String, String> in_mapOHeaders) {
        LogManagement.logStep(LogManagement.STD_STEPS.STORE_HEADERS);
        in_mapOHeaders.keySet().stream()
                .filter(i -> (i.startsWith(ConfigValueHandlerIBS.HEADERS_FILTER_PREFIX.fetchValue()) && !i.startsWith(
                        ConfigValueHandlerIBS.SECRETS_FILTER_PREFIX.fetchValue()))).forEach(fk -> {
                    this.getLocalClassLoader().getCallResultCache().put(fk, in_mapOHeaders.get(fk));
                    this.getLocalClassLoader().getHeaderSet().add(fk);
                });

        in_mapOHeaders.keySet().stream()
                .filter(i -> i.startsWith(ConfigValueHandlerIBS.SECRETS_FILTER_PREFIX.fetchValue())).forEach(fk -> {
                    this.getLocalClassLoader().getCallResultCache().put(fk, in_mapOHeaders.get(fk));
                    this.getLocalClassLoader().getSecretSet().add(fk);
                });

        //Check for duplicates between headers and call contents
        if (this.getLocalClassLoader().getHeaderSet().stream()
                .anyMatch(s -> this.getCallContent().keySet().contains(s))) {
            throw new IBSPayloadException("We found a header key that is also found among the callContent names");
        }

        //Check for duplicates between headers and call contents
        if (this.getLocalClassLoader().getSecretSet().stream()
                .anyMatch(s -> this.getCallContent().keySet().contains(s))) {
            throw new IBSPayloadException("We found a secret key that is also found among the callContent names");
        }

    }

    /**
     * Returns a set of secrets that are stored during the call
     *
     * @return a set of stored secret values
     */
    public Set<String> fetchSecrets() {
        return this.getLocalClassLoader().getSecretSet().stream()
                .map(k -> (String) getLocalClassLoader().getCallResultCache().get(k)).collect(
                        Collectors.toSet());
    }
}
