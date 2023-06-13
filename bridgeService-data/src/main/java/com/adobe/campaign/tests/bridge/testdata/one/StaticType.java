package com.adobe.campaign.tests.bridge.testdata.one;

public class StaticType {
    public static String fetchInstantiableStringValue(Instantiable in_instantiableObject) {
        return in_instantiableObject.getValueString();
    }
}
