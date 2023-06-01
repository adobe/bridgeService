package com.adobe.campaign.tests.service.testobjects;

public class StaticType {
    public static String fetchInstantiableStringValue(Instantiable in_instantiableObject) {
        return in_instantiableObject.getValueString();
    }
}
