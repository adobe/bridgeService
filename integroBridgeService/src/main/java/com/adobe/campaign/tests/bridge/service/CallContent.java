/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import com.adobe.campaign.tests.bridge.service.exceptions.AmbiguousMethodException;
import com.adobe.campaign.tests.bridge.service.exceptions.ClassLoaderConflictException;
import com.adobe.campaign.tests.bridge.service.exceptions.NonExistentJavaObjectException;
import com.adobe.campaign.tests.bridge.service.exceptions.TargetJavaMethodCallException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CallContent {

    @JsonProperty("class")
    private String className;

    @JsonProperty("method")
    private String methodName;
    private String returnType;
    private Object[] args;

    public CallContent() {
        args = new Object[] {};
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    /**
     * Returns the method object that is defined by this class
     *
     * @return the method object
     */
    public Method fetchMethod(Class in_class) {

        List<Method> lr_method = fetchMethodCandidates(in_class);
        if (lr_method.size() > 1) {
            throw new AmbiguousMethodException(
                    "We could not find a unique method for " + this.getFullName());
        }
        return lr_method.get(0);
    }

    /**
     * Returns the method object that is defined by this class
     *
     * @return the method object
     */
    public Constructor fetchConstructor(Class in_class) {

        List<Constructor> lr_method = fetchConstructorCandidates(in_class);
        if (lr_method.size() > 1) {
            throw new AmbiguousMethodException(
                    "We could not find a unique method for " + this.getFullName());
        }
        return lr_method.get(0);
    }

    public List<Method> fetchMethodCandidates(Class in_class) {

        List<Method> lr_method = Arrays.stream(in_class.getMethods())
                .filter(f -> f.getName().equals(this.getMethodName()))
                .filter(fp -> fp.getParameterCount() == this.getArgs().length).collect(
                        Collectors.toList());

        if (lr_method.isEmpty()) {
            throw new NonExistentJavaObjectException(
                    "Method " + this.getClassName() + "." + this.getMethodName() + "   with " + this.getArgs().length
                            + " arguments could not be found.");
        }
        return lr_method;
    }

    public List<Constructor> fetchConstructorCandidates(Class in_class) {

        List<Constructor> lr_method = Arrays.stream(in_class.getConstructors())
                .filter(fp -> fp.getParameterCount() == this.getArgs().length).collect(
                        Collectors.toList());

        if (lr_method.isEmpty()) {
            throw new NonExistentJavaObjectException(
                    "Constructor " + this.getClassName() + "." + this.getMethodName() + "   with "
                            + this.getArgs().length
                            + " arguments could not be found.");
        }
        return lr_method;
    }

    public Method fetchMethod() throws ClassNotFoundException {

        return fetchMethod(Class.forName(getClassName(), true, new IntegroBridgeClassLoader()));
    }

    /**
     * Calls the java method defined in this class
     *
     * @return the value of this call
     */
    public Object call(IntegroBridgeClassLoader iClassLoader) {

        Object lr_object;
        try {
            //Add our package to the classLoader integrity paths
            if (ConfigValueHandlerIBS.AUTOMATIC_INTEGRITY_PACKAGE_INJECTION.is("true")) {

                iClassLoader.getPackagePaths()
                        .add(this.getClassName().contains(".") ? this.getClassName()
                                .substring(0, this.getClassName().lastIndexOf('.')) : this.getClassName());
            }

            Object l_instanceObject = iClassLoader.getCallResultCache().get(this.getClassName());

            String l_usedClassName = (l_instanceObject == null ? this.getClassName() :
                    iClassLoader.getCallResultCache()
                            .get(this.getClassName()).getClass().getTypeName());

            Class ourClass = Class.forName(l_usedClassName, true, iClassLoader);

            if (isConstructorCall()) {
                Constructor l_constructor = fetchConstructor(ourClass);
                lr_object = l_constructor.newInstance(expandArgs(iClassLoader));
            } else {
                Method l_method = fetchMethod(ourClass);

                Object ourInstance = (l_instanceObject == null) ? ourClass.getDeclaredConstructor().newInstance() : l_instanceObject;
                lr_object = l_method.invoke(ourInstance, expandArgs(iClassLoader));
            }

        } catch (IllegalArgumentException e) {
            throw new NonExistentJavaObjectException(
                    "The given method " + this.getFullName() + " could not accept the given arguments..");

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof LinkageError) {
                throw new ClassLoaderConflictException("Linkage Error detected. This can be corrected by enriching the "+ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.systemName+" property", e.getCause());
            }
            //Arrays.stream(e.getTargetException().getStackTrace()).sequential().forEach(t -> response.append(t).append("\n"));
            throw new TargetJavaMethodCallException(
                    "We experienced an exception when calling the provided method " + this.getFullName()
                            + ".\nProvided error message : " + e.getTargetException().toString(), e);
        } catch (ClassNotFoundException e) {
            throw new NonExistentJavaObjectException("The given class " + this.getClassName() + "could not be found.");
        } catch (InstantiationException e) {
            throw new NonExistentJavaObjectException(
                    "Could not instantiate class. The given class " + this.getClassName() + "could not be found.");
        } catch (NoSuchMethodException e) {
            throw new NonExistentJavaObjectException(
                    "Could not find the method " + this.getFullName() + ".");
        }

        return lr_object;
    }

    /**
     * Returns the full name of the method
     *
     * @return the full name of the method
     */
    @JsonIgnore
    public String getFullName() {
        return this.getClassName() + "." + this.getMethodName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CallContent that = (CallContent) o;
        return getClassName().equals(that.getClassName()) && getMethodName().equals(that.getMethodName())
                && Objects.equals(getReturnType(), that.getReturnType()) && Arrays.equals(getArgs(),
                that.getArgs());
    }

    /**
     * This method returns an enriched array of parameters based on the current args. I.e. if we have stored the value
     * in the classloader context we replace the value
     *
     * @param in_currentClassLoader The used class loader
     * @return an array with values enriched with the previous results
     */
    public Object[] expandArgs(IntegroBridgeClassLoader in_currentClassLoader) {
        return Arrays.stream(getArgs()).map(arg -> in_currentClassLoader.getCallResultCache().getOrDefault(
                arg, arg)).toArray();
    }

    /**
     * Lets us know if the giben Call Content is a Constructor call
     *
     * @return true if the methos is actually a constructor
     */
    @JsonIgnore
    public boolean isConstructorCall() {
        return getMethodName() == null || getClassName().substring(getClassName().lastIndexOf('.') + 1)
                .equals(getMethodName());
    }

    @Override
    public int hashCode() {
        int result = getClassName().hashCode();
        result = 31 * result + (getMethodName() != null ? getMethodName().hashCode() : 0);
        result = 31 * result + (getReturnType() != null ? getReturnType().hashCode() : 0);
        result = 31 * result + Arrays.hashCode(getArgs());
        return result;
    }
}
