package com.adobe.campaign.tests.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CallContent {

    @JsonProperty("class")
    private String className;

    @JsonProperty("method")
    private String methodName;
    private String returnType;
    private Object[] args;

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
    public Method fetchMethod()  {
        Class ourClass;
        List<Method> lr_method = new ArrayList<>();
        try {
            ourClass = Class.forName(getClassName());
            lr_method = Arrays.stream(ourClass.getMethods())
                    .filter(f -> f.getName().equals(this.getMethodName()))
                    .filter(fp -> fp.getParameterCount() == this.getArgs().length).collect(
                            Collectors.toList());

            if (lr_method.size()==0) {
                throw new RuntimeException("Method "+this.getClassName()+"."+this.getMethodName()+ "   with "+this.getArgs().length+" could not be found.");
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }


        return lr_method.get(0);
    }

    /**
     * Calls the java method defined in this class
     *
     * @return the value of this call
     */
    public Object call()
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        return fetchMethod().invoke(null, this.getArgs());
    }
}
