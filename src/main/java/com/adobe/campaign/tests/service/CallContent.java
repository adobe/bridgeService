package com.adobe.campaign.tests.service;

import com.adobe.campaign.tests.service.exceptions.AmbiguousMethodException;
import com.adobe.campaign.tests.service.exceptions.NonExistantJavaObjectException;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CallContent {

    private IntegroBridgeClassLoader iClassLoader;

    @JsonProperty("class")
    private String className;

    @JsonProperty("method")
    private String methodName;
    private String returnType;
    private Object[] args;

    CallContent() {
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
                    "We could not find a unique method for " + this.getClassName() + "." + this.getMethodName());
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
    public Object call(IntegroBridgeClassLoader iClassLoader) throws InstantiationException {

        Object lr_object = null;
        try {
            Class ourClass = Class.forName(getClassName(), true, iClassLoader);

            Method l_method = fetchMethod(ourClass);

            Object ourInstance = ourClass.newInstance();
            lr_object = l_method.invoke(ourInstance, this.getArgs());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new NonExistantJavaObjectException("The given class " + this.getClassName() + "could not be found.",
                    e);
        }

        return lr_object;
    }
}
