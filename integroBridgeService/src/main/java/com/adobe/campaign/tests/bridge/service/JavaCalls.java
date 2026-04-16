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
                throw new IBSRunTimeException(e.getMessage(), e.getCause());
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


        //Expand header args
        Properties l_expandedProperties =  new Properties();
        getEnvironmentVariables().forEach((k, v) -> {
            l_expandedProperties.put(k, getLocalClassLoader().getCallResultCache().getOrDefault(v, v));
        });

        //Fetch all environment variables
        l_setEnvironmentVars.setArgs(new Object[] { l_expandedProperties });
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
     * Processes incoming request headers into three strictly disjoint namespaces:
     *
     * <ol>
     *   <li><b>Secret headers</b> ({@code IBS.SECRETS.FILTER.PREFIX}, default {@code ibs-secret-}) — stored in
     *       the classloader result cache and marked as secrets so their values are suppressed from the response.
     *       They can be referenced by key in {@code args} for call-chain dependency resolution.</li>
     *   <li><b>Env-var headers</b> ({@code IBS.ENV.HEADER.PREFIX}, default {@code ibs-env-}) — injected directly
     *       into {@code environmentVariables} as Java execution env vars. The prefix is stripped and the
     *       remainder uppercased to form the variable name (e.g. {@code ibs-env-AC.HOST} → {@code AC.HOST}).
     *       These headers are <em>not</em> added to the classloader cache and cannot be used as call-chain args.</li>
     *   <li><b>Regular headers</b> (matching {@code IBS.HEADERS.FILTER.PREFIX}, default {@code ""} = all) — stored
     *       in the classloader result cache and can be referenced by key in {@code args}. Secret and env-var
     *       headers are excluded from this group even when the filter prefix would otherwise match.</li>
     * </ol>
     *
     * An {@link com.adobe.campaign.tests.bridge.service.exceptions.IBSPayloadException} is thrown if any header
     * key collides with a {@code callContent} entry name.
     *
     * @param in_mapOHeaders A map containing header values coming from the request
     */
    public void addHeaders(Map<String, String> in_mapOHeaders) {
        LogManagement.logStep(LogManagement.STD_STEPS.STORE_HEADERS);

        String envPrefix = ConfigValueHandlerIBS.ENV_HEADER_PREFIX.fetchValue();
        boolean envPrefixActive = envPrefix != null && !envPrefix.isBlank();
        String lowerEnvPrefix = envPrefixActive ? envPrefix.toLowerCase(java.util.Locale.ROOT) : "";

        // Regular headers → classloader cache for call-chain dependency resolution.
        // Secrets and env-var headers are handled separately and excluded here.
        in_mapOHeaders.keySet().stream()
                .filter(i -> i.startsWith(ConfigValueHandlerIBS.HEADERS_FILTER_PREFIX.fetchValue())
                        && !i.startsWith(ConfigValueHandlerIBS.SECRETS_FILTER_PREFIX.fetchValue())
                        && !(envPrefixActive && i.toLowerCase(java.util.Locale.ROOT).startsWith(lowerEnvPrefix)))
                .forEach(fk -> {
                    this.getLocalClassLoader().getCallResultCache().put(fk, in_mapOHeaders.get(fk));
                    this.getLocalClassLoader().getHeaderSet().add(fk);
                });

        // Secret headers → classloader secret cache (suppressed from output, resolvable in args).
        in_mapOHeaders.keySet().stream()
                .filter(i -> i.startsWith(ConfigValueHandlerIBS.SECRETS_FILTER_PREFIX.fetchValue())).forEach(fk -> {
                    this.getLocalClassLoader().getCallResultCache().put(fk, in_mapOHeaders.get(fk));
                    this.getLocalClassLoader().getSecretSet().add(fk);
                });

        // Env-var headers → environment variables only (not added to the call-chain cache).
        if (envPrefixActive) {
            in_mapOHeaders.entrySet().stream()
                    .filter(e -> e.getKey().toLowerCase(java.util.Locale.ROOT).startsWith(lowerEnvPrefix))
                    .forEach(e -> this.environmentVariables.setProperty(
                            e.getKey().substring(envPrefix.length()).toUpperCase(java.util.Locale.ROOT),
                            e.getValue()));
        }

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
