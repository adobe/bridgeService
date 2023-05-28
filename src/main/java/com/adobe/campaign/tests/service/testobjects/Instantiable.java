package com.adobe.campaign.tests.service.testobjects;

public class Instantiable {
    String valueString;

    public Instantiable(String in_valueString) {
        valueString = in_valueString;
    }

    public String getValueString() {
        return valueString;
    }

    public void setValueString(String valueString) {
        this.valueString = valueString;
    }
}
