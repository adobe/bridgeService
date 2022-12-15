package com.adobe.campaign.tests.service;

import java.util.Map;

public class GenericUnserialisableObject {
    String sourceClass;
    Map<String, Object> values;

    GenericUnserialisableObject(Object in_Object) {
        this.values = MetaUtils.extractValuesFromObject(in_Object);
    }

}
