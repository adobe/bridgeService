package com.adobe.campaign.tests.service;

import com.adobe.campaign.tests.service.exceptions.AmbiguousMethodException;
import com.adobe.campaign.tests.service.exceptions.NonExistantJavaObjectException;
import com.adobe.campaign.tests.service.exceptions.TargetJavaClassException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
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

    public List<Method> fetchMethodCandidates(Class in_class) {
        List<Method> lr_method = new ArrayList<>();

        //ourClass = iClassLoader.loadClass(getClassName());
        lr_method = Arrays.stream(in_class.getMethods())
                .filter(f -> f.getName().equals(this.getMethodName()))
                .filter(fp -> fp.getParameterCount() == this.getArgs().length).collect(
                        Collectors.toList());

        if (lr_method.size() == 0) {
            throw new NonExistantJavaObjectException(
                    "Method " + this.getClassName() + "." + this.getMethodName() + "   with " + this.getArgs().length
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

        Object lr_object = null;
        try {
            Class ourClass = Class.forName(getClassName(), true, iClassLoader);

            Method l_method = fetchMethod(ourClass);

            Object ourInstance = ourClass.getDeclaredConstructor().newInstance();
            lr_object = l_method.invoke(ourInstance, expandArgs(iClassLoader));
        } catch (IllegalArgumentException e) {
            throw new NonExistantJavaObjectException(
                    "The given method " + this.getFullName() + " could not accept the given arguments..");

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new TargetJavaClassException(
                    "We experienced an exception when calling the provided method " + this.getFullName() + ".", e);
        } catch (ClassNotFoundException e) {
            throw new NonExistantJavaObjectException("The given class " + this.getClassName() + "could not be found.");
        } catch (InstantiationException e) {
            throw new NonExistantJavaObjectException(
                    "Could not instantiate class. The given class " + this.getClassName() + "could not be found.");
        } catch (NoSuchMethodException e) {
            throw new NonExistantJavaObjectException(
                    "Could not find the method " + this.getFullName() +".");
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
     * @param in_currentClassLoader
     * @return an array with values enriched with the previous results
     */
    public Object[] expandArgs(IntegroBridgeClassLoader in_currentClassLoader) {
        return Arrays.stream(getArgs()).map(arg -> in_currentClassLoader.getCallResultCache()
                .containsKey(arg) ? in_currentClassLoader.getCallResultCache().get(
                arg) : arg).toArray();
    }
}
