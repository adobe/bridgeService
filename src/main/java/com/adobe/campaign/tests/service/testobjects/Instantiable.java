package com.adobe.campaign.tests.service.testobjects;

public class Instantiable {
    String valueString;

    public Instantiable(String in_valueString) {
        valueString = in_valueString;
    }

    public Instantiable(String in_valueString1,String in_valueString2 ) {
    }

    public Instantiable(String in_valueString1,int in_valueint1 ) {
    }

    public String getValueString() {
        return valueString;
    }

    public void setValueString(String valueString) {
        this.valueString = valueString;
    }
}
