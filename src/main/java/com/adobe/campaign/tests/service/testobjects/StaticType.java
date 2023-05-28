package com.adobe.campaign.tests.service.testobjects;

public class StaticType {
    public String fetchInstantiableStringValue(Instantiable in_instantiableObject) {
        return in_instantiableObject.getValueString();
    }
}
